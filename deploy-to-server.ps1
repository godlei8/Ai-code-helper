param(
    [string]$Server = "8.137.187.70",
    [string]$User = "root",
    [string]$PrivateKeyPath = "d:\JavaAI\work_helper\deploy_key",
    [string]$LocalProjectPath = "d:\JavaAI\work_helper"
)

$ErrorActionPreference = "Continue"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   Work Helper 部署到阿里云" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$SSH = "ssh"
$SCP = "scp"

Write-Host "[1/7] 检查服务器环境..." -ForegroundColor Yellow

$checkResult = & $SSH -i $PrivateKeyPath -o StrictHostKeyChecking=no -o ConnectTimeout=30 $User@$Server @"
java -version 2>&1 | head -1
echo "---"
nginx -v 2>&1
echo "---"
node -v 2>&1
echo "---"
cat /etc/os-release | grep "^NAME" | head -1
"@ 2>&1

Write-Host "服务器环境:" -ForegroundColor Cyan
Write-Host $checkResult
Write-Host ""

Write-Host "[2/7] 安装必要软件..." -ForegroundColor Yellow

$installScript = @"
if ! command -v java &> /dev/null; then
    echo '安装 Java 21...'
    yum install -y java-21-openjdk java-21-openjdk-devel
fi

if ! command -v nginx &> /dev/null; then
    echo '安装 Nginx...'
    yum install -y nginx
fi

if ! command -v node &> /dev/null; then
    echo '安装 Node.js...'
    curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
    yum install -y nodejs
fi

mkdir -p /opt/work_helper /var/www/work_helper /opt/work_helper/config
echo '软件安装完成'
java -version 2>&1 | head -1
nginx -v 2>&1
node -v 2>&1
"@

& $SSH -i $PrivateKeyPath -o StrictHostKeyChecking=no $User@$Server $installScript 2>&1
Write-Host ""

Write-Host "[3/7] 检查本地构建文件..." -ForegroundColor Yellow

$backendJar = "$LocalProjectPath\target\work_helper-0.0.1-SNAPSHOT.jar"
$frontendDist = "$LocalProjectPath\ai-code-helper-frontend\dist"

if (-not (Test-Path $backendJar)) {
    Write-Host "警告: 后端 JAR 不存在: $backendJar" -ForegroundColor Yellow
    Write-Host "需要先运行: mvn clean package -DskipTests" -ForegroundColor Yellow
}

if (-not (Test-Path $frontendDist)) {
    Write-Host "警告: 前端 dist 不存在: $frontendDist" -ForegroundColor Yellow
    Write-Host "需要先运行: cd ai-code-helper-frontend; npm install; npm run build" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[4/7] 上传后端 JAR 文件..." -ForegroundColor Yellow

if (Test-Path $backendJar) {
    & $SCP -i $PrivateKeyPath -o StrictHostKeyChecking=no $backendJar "${User}@${Server}:/opt/work_helper/" 2>&1
    Write-Host "后端上传完成" -ForegroundColor Green
} else {
    Write-Host "跳过后端上传（文件不存在）" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[5/7] 上传前端文件..." -ForegroundColor Yellow

if (Test-Path $frontendDist) {
    & $SCP -i $PrivateKeyPath -o StrictHostKeyChecking=no -r $frontendDist "${User}@${Server}:/var/www/work_helper/" 2>&1
    Write-Host "前端上传完成" -ForegroundColor Green
} else {
    Write-Host "跳过前端上传（文件不存在）" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[6/7] 配置 Nginx..." -ForegroundColor Yellow

$nginxConfig = @"
server {
    listen 80;
    server_name _;
    root /var/www/work_helper;
    index index.html;

    location / {
        try_files `$uri `$uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8081/api;
        proxy_set_header Host `$host;
        proxy_set_header X-Real-IP `$remote_addr;
        proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /health {
        access_log off;
        return 200 ""OK"";
    }
}
"@

$nginxConfigPath = "$env:TEMP\work_helper_nginx.conf"
$nginxConfig | Out-File -FilePath $nginxConfigPath -Encoding UTF8

& $SCP -i $PrivateKeyPath -o StrictHostKeyChecking=no $nginxConfigPath "${User}@${Server}:/tmp/work_helper.conf" 2>&1

$nginxSetupScript = @"
mv /tmp/work_helper.conf /etc/nginx/conf.d/work_helper.conf
nginx -t
systemctl enable nginx
systemctl restart nginx
echo 'Nginx 配置完成'
"@

& $SSH -i $PrivateKeyPath -o StrictHostKeyChecking=no $User@$Server $nginxSetupScript 2>&1
Write-Host ""

Write-Host "[7/7] 配置 Systemd 服务..." -ForegroundColor Yellow

$systemdService = @"
[Unit]
Description=Work Helper AI Application
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/work_helper
ExecStart=/usr/lib/jvm/java-21-openjdk/bin/java -jar work_helper-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
Restart=always
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
"@

$systemdPath = "$env:TEMP\work_helper.service"
$systemdService | Out-File -FilePath $systemdPath -Encoding UTF8

& $SCP -i $PrivateKeyPath -o StrictHostKeyChecking=no $systemdPath "${User}@${Server}:/tmp/work_helper.service" 2>&1

$systemdSetupScript = @"
mv /tmp/work_helper.service /etc/systemd/system/work_helper.service
systemctl daemon-reload
systemctl enable work_helper
systemctl stop work_helper 2>/dev/null || true
systemctl start work_helper
echo 'Systemd 服务配置完成'
"@

& $SSH -i $PrivateKeyPath -o StrictHostKeyChecking=no $User@$Server $systemdSetupScript 2>&1

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   验证部署" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

Start-Sleep -Seconds 5

$verifyScript = @"
echo '=== 后端健康检查 ==='
curl -s --connect-timeout 5 http://127.0.0.1:8081/api/health || echo '后端可能还在启动中，请稍后检查'

echo ''
echo '=== 服务状态 ==='
systemctl status work_helper --no-pager | head -15

echo ''
echo '=== 端口监听 ==='
netstat -tlnp 2>/dev/null | grep -E '8081|:80' || ss -tlnp | grep -E '8081|:80'

echo ''
echo '=== Nginx 状态 ==='
systemctl status nginx --no-pager | head -5
"@

& $SSH -i $PrivateKeyPath -o StrictHostKeyChecking=no $User@$Server $verifyScript 2>&1

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "   部署完成!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "访问地址: http://$Server" -ForegroundColor Cyan
Write-Host "API 地址: http://$Server/api" -ForegroundColor Cyan
Write-Host ""
Write-Host "维护命令:" -ForegroundColor Yellow
Write-Host "  查看日志:  journalctl -u work_helper -f" -ForegroundColor White
Write-Host "  重启后端:  systemctl restart work_helper" -ForegroundColor White
Write-Host "  重启Nginx: nginx -s reload" -ForegroundColor White
Write-Host ""

Remove-Item $nginxConfigPath -ErrorAction SilentlyContinue
Remove-Item $systemdPath -ErrorAction SilentlyContinue

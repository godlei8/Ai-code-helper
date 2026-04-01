@echo off
setlocal enabledelayedexpansion

set SERVER=8.137.187.70
set USER=root
set PASSWORD=Aa642353

echo ======================================
echo   Work Helper 阿里云部署脚本
echo ======================================
echo.

echo [步骤1] 接受服务器主机密钥...
echo y | ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "echo Connected" > nul 2>&1

if errorlevel 1 (
    echo 主机密钥接受失败，继续尝试...
)

echo.
echo [步骤2] 检查服务器环境...
echo --------------------------------------------------------
echo y | ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "java -version 2>&1 | head -1; echo '---'; nginx -v 2>&1; echo '---'; node -v 2>&1; uname -a"
echo --------------------------------------------------------
echo.

echo [步骤3] 安装必要软件（如果需要）...
(
    echo %PASSWORD%
) | ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "
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
echo '服务器环境准备完成'
java -version 2>&1 | head -1
nginx -v 2>&1
node -v 2>&1
"

echo.
echo [步骤4] 创建目录结构...
(
    echo %PASSWORD%
) | ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "
mkdir -p /opt/work_helper /var/www/work_helper /opt/work_helper/config
ls -la /opt/
"

echo.
echo [步骤5] 配置 Nginx...
(
    echo %PASSWORD%
) | ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "cat > /etc/nginx/conf.d/work_helper.conf << 'NGINXCONF'
server {
    listen 80;
    server_name _;
    root /var/www/work_helper;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8081/api;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /health {
        access_log off;
        return 200 \"OK\";
    }
}
NGINXCONF
nginx -t && echo 'Nginx 配置正确'
"

echo.
echo [步骤6] 配置 Systemd 服务...
(
    echo %PASSWORD%
) | ssh -o StrictHostKeyChecking=no %USER%@%SERVER% "cat > /etc/systemd/system/work_helper.service << 'SYSTEMD'
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
SYSTEMD
systemctl daemon-reload
systemctl enable work_helper
echo 'Systemd 服务配置完成'
"

echo.
echo ======================================
echo   服务器配置完成!
echo ======================================
echo.
echo 接下来请手动执行以下步骤:
echo 1. 上传后端 JAR 文件
echo 2. 上传前端 dist 文件
echo 3. 启动服务
echo.
echo 或者运行完整的部署脚本
echo.

pause

# 阿里云部署计划

## 项目概述

* **后端**: Spring Boot 3.5.7 + Java 21

* **前端**: Vue 3 + Vite + Element Plus

* **服务器**: 8.137.187.70 (root/Aa642353)

* **目标**: 前后端分离部署，后端8082端口，前端Nginx代理

***

## 部署阶段

### 第一阶段：服务器环境准备

#### 1.1 连接服务器并检查环境

```bash
ssh root@8.137.187.70
# 检查是否已安装Java 21
java -version
# 检查是否已安装Nginx
nginx -v
# 检查是否已安装Node.js (前端构建用)
node -v
```

#### 1.2 安装必要软件（如缺失）

```bash
# 安装Java 21
yum install -y java-21-openjdk java-21-openjdk-devel

# 安装Nginx
yum install -y nginx

# 安装Node.js 18+
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs
```

#### 1.3 创建应用目录

```bash
mkdir -p /opt/work_helper
mkdir -p /var/www/work_helper
```

***

### 第二阶段：本地构建项目

#### 2.1 构建后端JAR包

```bash
cd d:\JavaAI\work_helper
mvn clean package -DskipTests
# 输出: target/work_helper-0.0.1-SNAPSHOT.jar
```

#### 2.2 构建前端

```bash
cd d:\JavaAI\work_helper\ai-code-helper-frontend
npm install
npm run build
# 输出: dist/ 目录
```

***

### 第三阶段：上传文件到服务器

#### 3.1 创建部署包结构

本地创建 `deploy-package/` 目录，包含：

```
deploy-package/
├── backend/
│   └── work_helper-0.0.1-SNAPSHOT.jar
├── frontend/
│   └── dist/ (前端构建产物)
├── config/
│   └── application-prod.yml (生产配置)
├── nginx/
│   └── work_helper.conf (Nginx配置)
└── scripts/
    └── deploy.sh (部署脚本)
```

#### 3.2 使用SCP上传

```bash
# 上传后端
scp deploy-package/backend/*.jar root@8.137.187.70:/opt/work_helper/

# 上传前端
scp -r deploy-package/frontend/* root@8.137.187.70:/var/www/work_helper/

# 上传配置
scp deploy-package/config/* root@8.137.187.70:/opt/work_helper/config/
```

***

### 第四阶段：服务器配置

#### 4.1 配置生产环境配置文件

在 `/opt/work_helper/config/application-prod.yml`:

```yaml
minimax:
  api-key: <你的MiniMax API Key>
  base-url: https://api.minimaxi.com/v1
  api-host: https://api.minimaxi.com
  chat-model:
    model-name: MiniMax-M2.7
    timeout-seconds: 120
  streaming-chat-model:
    model-name: MiniMax-M2.7
    timeout-seconds: 120
  mcp:
    command: uvx
    base-path: target/minimax-mcp
    resource-mode: url
```

#### 4.2 配置Nginx反向代理

创建 `/etc/nginx/conf.d/work_helper.conf`:

```nginx
server {
    listen 8082;
    server_name _;

    # 前端静态文件
    root /var/www/work_helper;
    index index.html;

    # 前端路由
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理到后端
    location /api {
        proxy_pass http://127.0.0.1:8081/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # SSE支持
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 "OK";
    }
}
```

#### 4.3 创建Systemd服务

创建 `/etc/systemd/system/work_helper.service`:

```ini
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
```

***

### 第五阶段：启动服务

#### 5.1 启动后端服务

```bash
systemctl daemon-reload
systemctl enable work_helper
systemctl start work_helper
systemctl status work_helper
```

#### 5.2 启动Nginx

```bash
nginx -t  # 测试配置
systemctl enable nginx
systemctl restart nginx
systemctl status nginx
```

#### 5.3 验证部署

```bash
# 检查后端健康
curl http://127.0.0.1:8081/api/health

# 检查前端
curl http://127.0.0.1/

# 检查Nginx日志
tail -f /var/log/nginx/access.log
tail -f /var/log/work_helper/application.log
```

***

### 第六阶段：域名/防火墙配置（如需要）

#### 6.1 开放防火墙端口

```bash
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
```

#### 6.2 阿里云安全组配置

需要在阿里云控制台开放：

* 80端口 (HTTP)

* 8082

* 443端口 (HTTPS，如配置SSL)

***

## 部署验证清单

* [ ] 服务器SSH连接成功

* [ ] Java 21已安装

* [ ] Nginx已安装并运行

* [ ] 后端JAR包已上传到 `/opt/work_helper/`

* [ ] 前端文件已上传到 `/var/www/work_helper/`

* [ ] 生产配置文件已创建

* [ ] Nginx配置已生效

* [ ] Systemd服务已启动

* [ ] 端口检查：8081和8082

* [ ] 健康检查通过

* [ ] 浏览器访问测试

***

## 常用维护命令

```bash
# 查看后端日志
journalctl -u work_helper -f

# 重启后端
systemctl restart work_helper

# 重启Nginx
nginx -s reload

# 回滚版本（如需要）
systemctl stop work_helper
# 替换JAR包后
systemctl start work_helper
```

***

## 预计完成时间

* 环境准备：10分钟

* 本地构建：5分钟

* 文件上传：5分钟

* 服务器配置：10分钟

* 验证测试：5分钟

* **总计约35分钟**


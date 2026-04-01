# AI 编程小助手

基于 `Spring Boot + LangChain4j + MiniMax + Vue 3` 的编程学习与求职辅导项目。  
当前仓库同时包含后端服务和独立前端项目，核心场景是通过 SSE 实时聊天，围绕编程学习、求职准备、面试题解析和知识检索提供帮助。

## 项目结构

```text
work_helper/
├─ src/main/java/com/example/work_helper
│  ├─ controller/AiController.java
│  ├─ ai/AiCodeHelperService.java
│  ├─ ai/AiCodeHelperServiceFactory.java
│  ├─ ai/model/MiniMaxChatModelConfig.java
│  ├─ ai/listener/ChatModelListenerConfig.java
│  ├─ ai/rag/RagConfig.java
│  ├─ ai/mcp/McpConfig.java
│  ├─ ai/tools/InterviewQuestionTool.java
│  ├─ ai/guardrail/SafeInputGuardrail.java
│  └─ ai/config/CorsConfig.java
├─ src/main/resources
│  ├─ application.yml
│  ├─ application-local.yml
│  ├─ system-prompt.txt
│  └─ docs/
└─ ai-code-helper-frontend/
   ├─ src/App.vue
   ├─ src/services/http.js
   ├─ src/services/hotPrompts.js
   └─ src/utils/markdown.js
```

## 当前能力

- 普通对话：基于 MiniMax 聊天模型回答编程和求职问题
- 流式对话：后端返回 SSE，前端按 chunk 实时渲染
- 会话记忆：通过 `memoryId` 区分不同聊天会话
- RAG 检索增强：读取 `src/main/resources/docs` 下的本地资料，基于轻量分段与关键词匹配返回相关内容
- 工具调用：通过 `InterviewQuestionTool` 从面试鸭抓取相关面试题
- MCP 集成：通过 `McpConfig` 对接 MiniMax Coding Plan MCP 工具
- 输入护栏：`SafeInputGuardrail`
- Markdown 展示：前端支持标题、列表、表格、任务列表、代码块高亮
- 中文技术热榜：前端动态拉取掘金、V2EX、CNode 热门内容生成推荐提问

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.7
- LangChain4j 1.1.0 / 1.1.0-beta7
- LangChain4j OpenAI 兼容模型接入（MiniMax）
- LangChain4j MCP
- Reactor
- Jsoup

### 前端

- Vue 3
- Vite
- Element Plus
- Axios
- markdown-it
- highlight.js

## 核心接口

后端默认端口和上下文路径配置在 [application.yml](src/main/resources/application.yml)：

- 服务地址：`http://localhost:8081`
- 接口前缀：`/api`

当前聊天接口：

```http
GET /api/ai/chat?memoryId=10001&message=怎么准备 Java 面试
```

返回类型：

```text
text/event-stream
```

对应实现见 [AiController.java](src/main/java/com/example/work_helper/controller/AiController.java)。

## 本地运行

### 1. 准备环境

- JDK 21+
- Node.js 18+ 或更高版本
- Maven Wrapper 已包含在仓库中
- MiniMax API Key
- 可选：`uvx`，如果需要启用 MCP

### 2. 配置后端

建议检查并修改：

- [application.yml](src/main/resources/application.yml)
- [application-local.yml](src/main/resources/application-local.yml)

当前后端主要依赖以下配置：

```yaml
minimax:
  api-key: <your-minimax-api-key>
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

说明：

- `minimax.api-key` 用于聊天模型调用，也会传递给 MCP 进程
- 如果未配置 `minimax.api-key`，聊天请求无法正常访问模型，`McpConfig` 也会自动降级为禁用 MCP 工具
- 默认配置会从 `src/main/resources/application-local.yml`、仓库根目录 `application-local.yml` 或 `config/application-local.yml` 按顺序尝试覆盖

### 3. 启动后端

在仓库根目录执行：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

启动后默认地址：

```text
http://localhost:8081/api
```

### 4. 启动前端

```bash
cd ai-code-helper-frontend
npm install
npm run dev
```

开发环境默认地址：

```text
http://localhost:5110
```

## 前后端联调说明

前端开发环境通过 Vite 代理处理：

- `/api` -> `http://localhost:8081`
- `/hot-api/juejin` -> `https://api.juejin.cn`
- `/hot-api/v2ex` -> `https://www.v2ex.com`

对应配置见：

- [vite.config.js](ai-code-helper-frontend/vite.config.js)
- [.env.development](ai-code-helper-frontend/.env.development)

生产环境如果仍要启用掘金 / V2EX 热榜，部署侧也需要提供 `/hot-api` 反向代理；否则前端会退回到 `CNode` 兜底。

## 主要代码说明

### 后端

- [AiCodeHelperService.java](src/main/java/com/example/work_helper/ai/AiCodeHelperService.java)
  AI 服务接口，定义普通对话、RAG 对话和流式对话
- [AiCodeHelperServiceFactory.java](src/main/java/com/example/work_helper/ai/AiCodeHelperServiceFactory.java)
  组装聊天模型、流式模型、工具、MCP 和内容检索器
- [MiniMaxChatModelConfig.java](src/main/java/com/example/work_helper/ai/model/MiniMaxChatModelConfig.java)
  配置 MiniMax 普通聊天模型和流式聊天模型
- [RagConfig.java](src/main/java/com/example/work_helper/ai/rag/RagConfig.java)
  加载本地文档并构造 `ContentRetriever`
- [InterviewQuestionTool.java](src/main/java/com/example/work_helper/ai/tools/InterviewQuestionTool.java)
  搜索面试鸭问题
- [McpConfig.java](src/main/java/com/example/work_helper/ai/mcp/McpConfig.java)
  配置 MiniMax MCP 工具及缺省降级行为

### 前端

- [App.vue](ai-code-helper-frontend/src/App.vue)
  单页聊天室、会话管理、SSE 接入、热榜推荐
- [http.js](ai-code-helper-frontend/src/services/http.js)
  Axios 和 SSE URL 构造
- [hotPrompts.js](ai-code-helper-frontend/src/services/hotPrompts.js)
  中文技术热榜抓取、过滤和本地缓存
- [markdown.js](ai-code-helper-frontend/src/utils/markdown.js)
  Markdown 渲染、表格支持和代码高亮

## 构建

### 后端

```bash
./mvnw clean package
```

### 前端

```bash
cd ai-code-helper-frontend
npm run build
```

### 阿里云部署包

```powershell
.\deploy\aliyun\build-release.ps1
```

打包完成后会在 `release/aliyun/` 下生成版本化的阿里云部署包，内含：

- Spring Boot 可执行 jar
- 前端静态资源
- `config/application-prod.yml.example`
- Nginx 反向代理模板
- Linux 安装、回滚、状态脚本
- `systemd` 服务模板

如果服务器是从旧的 DashScope/BigModel 部署升级到当前版本，记得同步更新 `/opt/ai-code-helper/shared/config/application-prod.yml`。  
新的安装脚本会检测旧配置并直接报错；如需用当前 bundle 的 MiniMax 模板覆盖共享配置，可在服务器执行：

```bash
sudo APP_ROOT=/opt/ai-code-helper CONFIG_FORCE_REFRESH=1 bash ops/scripts/install.sh
```

## 注意事项

- 请不要把真实 API Key 提交到公开仓库
- `application-local.yml` 中的本地敏感配置建议只保留在个人环境
- MCP 服务是否能连通，受 MiniMax API Key、`uvx` 环境和远端服务状态影响
- RAG 会在启动时加载 `src/main/resources/docs` 下的文档内容

## 相关 README

- 前端说明见 [ai-code-helper-frontend/README.md](ai-code-helper-frontend/README.md)

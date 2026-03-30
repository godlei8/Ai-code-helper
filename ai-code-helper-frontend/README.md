# AI Code Helper Frontend

`ai-code-helper-frontend` 是《AI 编程小助手》的前端子项目，基于 `Vue 3 + Vite + Element Plus + Axios` 实现。

项目当前是单页聊天室：

- 上方展示聊天记录
- 用户消息在右侧
- AI 消息在左侧
- 首次进入页面自动生成 `memoryId`
- 通过 SSE 调用后端接口，实时显示回复内容
- AI 回复支持 Markdown、表格、任务列表、代码块高亮
- 推荐提问来自中文技术社区热榜

## 技术栈

- Vue 3
- Vite
- Element Plus
- Axios
- markdown-it
- markdown-it-multimd-table
- markdown-it-task-lists
- highlight.js

## 目录结构

```text
ai-code-helper-frontend/
├─ src/
│  ├─ App.vue
│  ├─ main.js
│  ├─ styles.css
│  ├─ services/
│  │  ├─ http.js
│  │  └─ hotPrompts.js
│  └─ utils/
│     └─ markdown.js
├─ .env.development
├─ .env.production
├─ package.json
└─ vite.config.js
```

## 启动

```bash
npm install
npm run dev
```

默认开发地址：

```text
http://localhost:5110
```

## 后端对接

聊天接口由后端提供：

```http
GET /api/ai/chat?memoryId=10001&message=怎么准备 Java 面试
```

前端开发环境默认把 `/api` 代理到：

```text
http://localhost:8081
```

因此本地联调时只需要确保后端在 `8081` 启动。

## 环境变量

### 开发环境

[.env.development](.env.development)

```env
VITE_API_BASE_URL=/api
VITE_PROXY_TARGET=http://localhost:8081
VITE_HOT_PROXY_BASE=/hot-api
```

### 生产环境

[.env.production](.env.production)

```env
VITE_API_BASE_URL=/api
```

说明：

- 开发环境依赖 Vite 代理访问后端和中文热榜来源
- 生产环境如果还想保留掘金 / V2EX 热榜，需要部署侧继续配置 `/hot-api` 代理

## 页面能力

### 1. 聊天与会话

- 页面初始化自动生成 `memoryId`
- 可手动新建会话
- 可清空当前聊天记录
- 支持停止流式回复

### 2. SSE 流式输出

前端使用 `EventSource` 建立连接，核心逻辑见：

- [App.vue](src/App.vue)
- [http.js](src/services/http.js)

### 3. Markdown 渲染

AI 回复通过 [markdown.js](src/utils/markdown.js) 渲染，支持：

- 标题
- 列表
- 表格
- 任务列表
- 行内代码
- fenced code block
- 语言高亮

### 4. 中文技术热榜

推荐提问逻辑见 [hotPrompts.js](src/services/hotPrompts.js)。

当前优先使用：

- 掘金
- V2EX
- CNode 兜底

并按“编程 / 面试 / 求职”关键词做过滤和排序。

## 构建

```bash
npm run build
```

构建产物输出到：

```text
dist/
```

## 说明

- 当前项目使用了完整的 `highlight.js`，因此打包体积会偏大
- 如果后续需要优化首屏体积，可以进一步改成语言按需加载

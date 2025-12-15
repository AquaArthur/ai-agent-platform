# AI Agent Platform Frontend

AI 智能体平台前端应用，基于 Vue 3 + TypeScript + Vite 构建的现代化 Web 应用。

## 📋 项目简介

这是一个功能完整的 AI 智能体管理平台前端项目，提供智能体管理、插件管理、知识库管理、工作流编排、对话测试等核心功能。项目采用现代化的前端技术栈，内置可视化工作流编辑器，支持拖拽式节点编排，支持 Mock 数据模式和真实 API 模式切换，便于开发和测试。

## ✨ 主要功能

- **智能体管理** - 创建、编辑、删除和查看智能体配置，支持关联工作流和知识库
- **插件管理** - 管理 AI 智能体可用的插件工具，支持 OpenAPI 规范导入和配置
- **知识库管理** - 创建和管理知识库，上传文档，配置向量化参数，支持文档预览和删除
- **工作流管理** - 可视化工作流编辑器，支持拖拽式节点编排、工作流验证和执行
  - 可视化编辑器：基于 Vue Flow 的拖拽式工作流设计器
  - 节点类型：支持开始、结束、LLM调用、HTTP请求、知识库检索、意图识别、字符串处理等节点
  - 工作流验证：自动验证工作流的完整性和正确性
  - 工作流执行：支持实时执行工作流并查看执行结果
  - 执行历史：查看工作流的执行记录和状态
  - 撤销/重做：支持操作历史记录管理
  - 自动布局：一键自动排列节点
- **对话测试** - 与智能体进行实时对话，测试智能体功能，支持会话历史管理
- **LLM 模型管理** - 管理 LLM 模型和提供商配置，支持多模型切换
- **系统测试** - 系统功能测试页面

## 🛠️ 技术栈

- **框架**: Vue 3.5+ (Composition API)
- **语言**: TypeScript 5.9+
- **构建工具**: Vite 7.2+
- **UI 框架**: Element Plus 2.11+
- **状态管理**: Pinia 3.0+
- **路由**: Vue Router 4.6+
- **HTTP 客户端**: Axios 1.13+
- **样式**: Tailwind CSS 4.1+
- **图标**: Element Plus Icons
- **代码编辑器**: Monaco Editor 0.55+
- **工作流可视化**: Vue Flow 1.48+ (基于 React Flow)
  - `@vue-flow/core` - 核心工作流组件
  - `@vue-flow/background` - 背景网格
  - `@vue-flow/controls` - 画布控制按钮
  - `@vue-flow/minimap` - 小地图导航

## 📁 项目结构

```
frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API 接口定义
│   │   ├── agent.ts       # 智能体相关 API
│   │   ├── chat.ts        # 对话相关 API
│   │   ├── plugin.ts      # 插件相关 API
│   │   ├── llm.ts         # LLM 模型相关 API
│   │   ├── knowledgeBase.ts # 知识库相关 API
│   │   ├── workflow.ts    # 工作流相关 API
│   │   └── index.ts       # API 统一导出
│   ├── assets/            # 资源文件
│   ├── components/        # 公共组件
│   │   ├── MainLayout.vue # 主布局组件
│   │   ├── MonacoEditor.vue # Monaco 代码编辑器组件
│   │   └── HelloWorld.vue
│   ├── mock/              # Mock 数据
│   │   └── data.ts        # Mock 数据定义
│   ├── router/            # 路由配置
│   │   └── index.ts
│   ├── stores/            # Pinia 状态管理
│   │   ├── index.ts
│   │   ├── useAgentStore.ts    # 智能体状态管理
│   │   ├── usePluginStore.ts   # 插件状态管理
│   │   └── useUserStore.ts     # 用户状态管理
│   ├── styles/            # 全局样式
│   │   ├── index.css
│   │   ├── tailwind.css
│   │   ├── common.css
│   │   └── node-common.css     # 工作流节点通用样式
│   ├── types/             # TypeScript 类型定义
│   │   └── entity.ts      # 实体类型定义
│   ├── utils/             # 工具函数
│   │   ├── http.ts        # HTTP 请求封装
│   │   ├── formatters.ts  # 格式化工具函数
│   │   └── store.ts       # Store 工具函数
│   ├── views/             # 页面视图
│   │   ├── agent/         # 智能体相关页面
│   │   │   ├── AgentList.vue
│   │   │   ├── AgentEditor.vue
│   │   │   └── components/
│   │   │       └── ChatPanel.vue
│   │   ├── chat/          # 对话相关页面
│   │   │   ├── ChatView.vue
│   │   │   └── components/
│   │   │       ├── ChatInput.vue
│   │   │       └── ChatMessage.vue
│   │   ├── plugin/        # 插件相关页面
│   │   │   ├── PluginList.vue
│   │   │   └── PluginDialog.vue
│   │   ├── knowledgeBase/ # 知识库相关页面
│   │   │   ├── KnowledgeBaseList.vue
│   │   │   └── KnowledgeBaseDetail.vue
│   │   ├── workflow/      # 工作流相关页面
│   │   │   ├── WorkflowList.vue
│   │   │   ├── WorkflowEditor.vue
│   │   │   ├── nodes/     # 工作流节点组件
│   │   │   │   ├── StartNode.vue
│   │   │   │   ├── EndNode.vue
│   │   │   │   ├── LLMNode.vue
│   │   │   │   ├── HttpNode.vue
│   │   │   │   ├── KnowledgeNode.vue
│   │   │   │   ├── IntentNode.vue
│   │   │   │   └── StringNode.vue
│   │   │   └── components/
│   │   │       ├── NodeConfigDialog.vue
│   │   │       ├── ExecutionPanel.vue
│   │   │       └── ExecutionStatus.vue
│   │   └── HomeView.vue   # 首页
│   ├── App.vue            # 根组件
│   └── main.ts            # 入口文件
├── Dockerfile             # Docker 构建文件
├── nginx.conf             # Nginx 配置
├── package.json           # 项目依赖
├── tsconfig.json          # TypeScript 配置
├── vite.config.ts         # Vite 配置
└── tailwind.config.ts     # Tailwind CSS 配置
```

## 🚀 快速开始

### 环境要求

- Node.js >= 20.x
- npm >= 9.x 或 yarn >= 1.22.x

### 安装依赖

```bash
npm install
```

### 开发模式

启动开发服务器（默认端口 5173）：

```bash
npm run dev
```

开发服务器会自动启动，支持热模块替换（HMR）。

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist/` 目录。

### 预览生产构建

```bash
npm run preview
```

## ⚙️ 配置说明

### 环境变量

项目支持通过环境变量进行配置，`.env.development`：

```env
# API 基础路径（默认: /api）
VITE_BASE_API=/api

# 是否启用 Mock 模式（默认: true）
# 设置为 false 时，将使用真实的后端 API
VITE_USE_MOCK=true
```

### API 代理配置

开发模式下，Vite 会将 `/api` 请求代理到后端服务器。在 `vite.config.ts` 中配置：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端服务地址
      changeOrigin: true,
    }
  }
}
```

### Mock 模式

- **启用 Mock**: 设置 `VITE_USE_MOCK=true` 或不设置（默认启用）
- **禁用 Mock**: 设置 `VITE_USE_MOCK=false`，将使用真实后端 API

## 🐳 Docker 部署

### 构建 Docker 镜像

```bash
docker build -t ai-agent-platform-frontend .
```

### 运行容器

```bash
docker run -d -p 80:80 ai-agent-platform-frontend
```

### Docker Compose

如果使用 Docker Compose，确保后端服务名为 `backend`，端口为 `8080`。Nginx 配置会自动代理 `/api/` 请求到后端服务。

**Dockerfile 说明**：
- 使用多阶段构建，第一阶段使用 Node.js 构建前端应用
- 第二阶段使用 Nginx Alpine 镜像作为生产环境
- 构建产物复制到 Nginx 静态文件目录
- Nginx 配置支持 SPA 路由和 API 代理

## 🗺️ 路由说明

应用主要路由如下：

| 路径 | 名称 | 说明 |
|------|------|------|
| `/` | - | 重定向到 `/agents` |
| `/home` | home | 系统测试页面 |
| `/agents` | agent-list | 智能体列表页 |
| `/agents/:id` | agent-editor | 智能体编辑器（创建/编辑） |
| `/plugins` | plugin-list | 插件管理列表页 |
| `/chat` | chat | 对话测试页面 |
| `/knowledge-bases` | knowledge-base-list | 知识库列表页 |
| `/knowledge-bases/:uuid` | knowledge-base-detail | 知识库详情页 |
| `/workflows` | workflow-list | 工作流列表页 |
| `/workflow-editor/:uuid?` | workflow-editor | 工作流编辑器（新建/编辑） |

## 📡 API 接口

### 智能体 API

- `GET /api/v1/agents` - 获取智能体列表
- `GET /api/v1/agents/:id` - 获取智能体详情
- `POST /api/v1/agents` - 创建智能体
- `PUT /api/v1/agents/:id` - 更新智能体
- `DELETE /api/v1/agents/:id` - 删除智能体

### 插件 API

- `GET /api/v1/plugins` - 获取插件列表
- `GET /api/v1/plugins/:id` - 获取插件详情
- `POST /api/v1/plugins` - 创建插件
- `PUT /api/v1/plugins/:id` - 更新插件
- `DELETE /api/v1/plugins/:id` - 删除插件

### 对话 API

- `POST /api/v1/chat/session` - 创建会话
- `GET /api/v1/chat/history/:sessionId` - 获取消息历史
- `POST /api/v1/chat/message` - 发送消息

### 知识库 API

- `GET /api/v1/knowledge-bases` - 获取知识库列表（支持分页、搜索、筛选）
- `GET /api/v1/knowledge-bases/:uuid` - 获取知识库详情
- `POST /api/v1/knowledge-bases` - 创建知识库
- `PATCH /api/v1/knowledge-bases/:uuid` - 更新知识库（部分更新）
- `DELETE /api/v1/knowledge-bases/:uuid` - 删除知识库
- `GET /api/v1/knowledge-bases/:kbUuid/documents` - 获取知识库的文档列表
- `POST /api/v1/knowledge-bases/:kbUuid/documents` - 上传文档到知识库
- `GET /api/v1/documents/:uuid` - 获取文档详情
- `DELETE /api/v1/documents/:uuid` - 删除文档

### LLM 模型 API

- `GET /api/v1/llm/models` - 获取 LLM 模型列表
- `GET /api/v1/llm/models/:id` - 获取 LLM 模型详情
- `POST /api/v1/llm/models` - 创建 LLM 模型
- `PUT /api/v1/llm/models/:id` - 更新 LLM 模型
- `DELETE /api/v1/llm/models/:id` - 删除 LLM 模型
- `GET /api/v1/llm/providers` - 获取 LLM 提供商列表

### 工作流 API

- `GET /api/v1/workflows` - 获取工作流列表（支持分页、搜索、筛选）
- `GET /api/v1/workflows/:uuid` - 获取工作流详情
- `POST /api/v1/workflows` - 创建工作流
- `PUT /api/v1/workflows/:uuid` - 更新工作流
- `DELETE /api/v1/workflows/:uuid` - 删除工作流
- `POST /api/v1/workflows/:uuid/validate` - 验证工作流
- `POST /api/v1/workflows/:uuid/execute` - 执行工作流
- `GET /api/v1/workflows/:uuid/executions` - 获取工作流的执行历史
- `GET /api/v1/workflows/executions` - 获取所有执行历史（支持筛选）
- `GET /api/v1/workflows/executions/:executionId` - 获取执行记录详情

### 响应格式

后端统一响应格式：

```typescript
{
  code: number,        // 状态码，0 表示成功
  message: string,     // 消息
  data: any,          // 数据
  timestamp: number    // 时间戳
}
```

## 🔄 工作流节点类型

工作流编辑器支持以下节点类型：

| 节点类型 | 标识 | 说明 | 颜色 |
|---------|------|------|------|
| 开始 | `start` | 工作流的起始节点，每个工作流必须有一个开始节点 | 绿色 (#67c23a) |
| 结束 | `end` | 工作流的终止节点，每个工作流必须有一个结束节点 | 红色 (#f56c6c) |
| LLM调用 | `llm` | 调用大语言模型进行文本生成或处理 | 蓝色 (#409eff) |
| HTTP请求 | `http` | 发送 HTTP 请求调用外部 API | 橙色 (#e6a23c) |
| 知识库检索 | `knowledge` | 从知识库中检索相关信息 | 灰色 (#909399) |
| 意图识别 | `intent` | 识别用户意图，进行意图分类 | 紫色 (#9c27b0) |
| 字符串处理 | `string` | 对字符串进行各种处理和转换 | 青色 (#00bcd4) |

### 工作流编辑器功能

- **拖拽添加节点**：从工具栏拖拽节点到画布
- **连接节点**：通过拖拽连接点建立节点之间的连接关系
- **节点配置**：双击节点打开配置对话框，设置节点参数
- **撤销/重做**：支持操作历史记录（快捷键：Ctrl+Z / Ctrl+Y）
- **自动布局**：一键自动排列所有节点
- **居中显示**：快速将画布居中显示所有节点
- **小地图**：提供画布导航小地图
- **工作流验证**：验证工作流的完整性和正确性
- **工作流执行**：实时执行工作流并查看执行结果和节点执行状态

## 🎨 开发规范

### 代码风格

- 使用 TypeScript 进行类型检查
- 遵循 Vue 3 Composition API 最佳实践
- 使用 ESLint 和 Prettier 保持代码风格一致（如已配置）

### 组件开发

- 使用 `<script setup>` 语法
- 组件命名使用 PascalCase
- Props 和 Emits 需要定义类型

### 状态管理

- 使用 Pinia 进行全局状态管理
- Store 文件命名格式：`useXxxStore.ts`
- 每个功能模块对应一个 Store

### API 调用

- 所有 API 调用统一通过 `src/api/` 目录下的文件
- 使用 `src/utils/http.ts` 封装的 axios 实例
- 支持 Mock 模式和真实 API 模式自动切换

## 🔧 常见问题

### 1. 端口被占用

修改 `vite.config.ts` 中的 `server.port` 配置：

```typescript
server: {
  port: 3000,  // 修改为其他端口
  // ...
}
```

### 2. API 请求失败

- 检查后端服务是否正常运行
- 确认 `VITE_BASE_API` 配置正确
- 检查浏览器控制台的网络请求错误信息
- 确认 CORS 配置正确

### 3. Mock 数据不生效

- 确认 `VITE_USE_MOCK` 环境变量设置为 `true`
- 检查 `src/mock/data.ts` 文件是否存在
- 重启开发服务器

## 📝 更新日志

### v0.0.0

- 初始版本

### v1.0.0

- 实现智能体管理功能（创建、编辑、删除、查看智能体配置）
- 实现插件管理功能（OpenAPI 规范导入和配置）
- 实现对话测试功能（实时对话、会话历史）
- 支持 LLM 模型管理（多模型和提供商配置）
- 支持 Mock 数据模式
- 支持 Docker 部署（多阶段构建，Nginx 服务）

### v2.0.0

- 实现知识库管理功能（创建、编辑、删除知识库，上传和管理文档）
- 集成 Monaco Editor 代码编辑器

### v3.0.0

- 实现工作流管理功能
  - 可视化工作流编辑器（基于 Vue Flow）
  - 支持 7 种节点类型（开始、结束、LLM、HTTP、知识库、意图、字符串）
  - 拖拽式节点编排
  - 工作流验证和执行
  - 执行历史记录查看
  - 撤销/重做功能
  - 自动布局和居中显示
- 集成 Vue Flow 工作流可视化组件

## 📄 许可证

本项目为私有项目。

## 👥 贡献

欢迎提交 Issue 和 Pull Request。

## 📮 联系方式

如有问题或建议，请联系项目维护者。


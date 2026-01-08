# AI Agent Platform Frontend

基于 Vue 3 + TypeScript + Vite 构建的 AI 智能体管理平台前端。

## 功能模块

### 用户认证

登录、注册，基于 Token 的身份验证与路由守卫。

### 智能体管理

- 卡片列表：名称/描述搜索、状态筛选、分页
- 创建/编辑：名称、描述、系统提示词、提示词模板
- 模型配置：选择 LLM 模型、温度参数
- 关联配置：绑定知识库、绑定插件（含优先级和启用状态）

### 知识库管理

- 卡片列表：名称/描述搜索、作用域筛选、分页
- 创建/编辑：名称、描述、作用域类型、访问级别
- 向量化参数：分块大小、分块重叠
- 文档管理：上传文档、查看文档数和文本块数
- 作用域类型：系统级、学校级、课程级、智能体级、个人级

### 插件管理

- 卡片列表：名称/标识符/描述搜索、状态筛选、分页
- 创建/编辑：名称、描述、基础 URL、鉴权方式
- 支持 OpenAPI 规范 JSON 导入
- 插件测试：选择操作、输入参数、执行调用

### 模型管理

- 卡片列表：名称/提供商/描述搜索、状态筛选、分页
- 创建/编辑（管理员）：名称、提供商、模型类型、API 配置
- 启用/禁用模型、设置默认模型

### 工作流编排

- 可视化编辑器（基于 Vue Flow）：拖拽添加节点、连线编排
- 节点类型：开始、结束、LLM 调用、HTTP 请求、知识库检索、意图识别、字符串处理
- 工作流验证与执行、执行历史记录
- 撤销/重做、自动布局、小地图导航

### 对话测试

- 选择智能体进行实时对话
- 支持从路由传入模型 ID 或智能体 ID
- 显示智能体配置信息（状态、描述、模型）

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.5 (Composition API) |
| 语言 | TypeScript 5.9 |
| 构建 | Vite 7.2 |
| UI | Element Plus 2.11 |
| 状态管理 | Pinia 3.0 |
| 路由 | Vue Router 4.6 |
| 样式 | Tailwind CSS 4.1 |
| 代码编辑器 | Monaco Editor 0.55 |
| 工作流可视化 | Vue Flow 1.48 |

## 项目结构

```
src/
├── api/                # API 接口
├── components/         # 公共组件
├── composables/        # 组合式函数
├── router/             # 路由配置
├── stores/             # Pinia 状态管理
├── styles/             # 全局样式
├── types/              # TypeScript 类型定义
├── utils/              # 工具函数
└── views/              # 页面视图
    ├── agent/          # 智能体
    ├── chat/           # 对话
    ├── knowledgeBase/  # 知识库
    ├── model/          # 模型
    ├── plugin/         # 插件
    └── workflow/       # 工作流
        └── nodes/      # 工作流节点组件
```

## 快速开始

### 环境要求

- Node.js >= 20.x
- npm >= 9.x

### 本地开发

```bash
npm install
npm run dev      # 启动开发服务器，默认端口 5173
npm run build    # 生产构建
```

本地开发时，Vite 将 `/api` 请求代理到后端（默认 `http://localhost:8081`），可通过环境变量 `VITE_PROXY_TARGET` 覆盖。

## 部署

项目提供开发环境和生产环境两套 Docker 配置，通过 `deploy/` 目录下的 Docker Compose 统一编排。

### 开发环境

使用 `Dockerfile.dev`，启动 Vite Dev Server，支持热重载。

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend | 3000 | Vite Dev Server |
| Backend | 8081 | Spring Boot |

```bash
cd deploy
make dev    # 或 bash deploy.dev.sh
```

### 生产环境

使用 `Dockerfile.prod`，采用多阶段构建（Node.js 构建 + Nginx 运行）。

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend | 80 | Nginx 静态服务 |
| Backend | 8080 | Spring Boot |

```bash
cd deploy
make prod   # 或 bash deploy.prod.sh
```

Nginx 配置支持 SPA 路由，并将 `/api/` 请求代理至后端服务 `http://backend:8080`。

## 路由

| 路径 | 说明 |
|------|------|
| `/login` | 登录 |
| `/register` | 注册 |
| `/main/agents` | 智能体列表 |
| `/main/agents/:id` | 智能体编辑 |
| `/main/plugins` | 插件管理 |
| `/main/models` | 模型管理 |
| `/main/knowledge-bases` | 知识库列表 |
| `/main/knowledge-bases/:uuid` | 知识库详情 |
| `/main/workflows` | 工作流列表 |
| `/main/workflow-editor/:uuid?` | 工作流编辑 |
| `/main/chat` | 对话测试 |

## 更新日志

### v4.0.0 (2026-01-01)

- 新增用户认证模块（登录、注册）
- 新增路由守卫与权限控制
- 重构路由结构，采用嵌套布局

### v3.0.0

- 实现可视化工作流编辑器
- 支持 7 种工作流节点类型
- 工作流验证、执行与历史记录
- 撤销/重做、自动布局功能

### v2.0.0

- 实现知识库管理功能
- 集成 Monaco Editor

### v1.0.0

- 智能体管理、插件管理、对话测试
- LLM 模型管理
- Docker 部署支持

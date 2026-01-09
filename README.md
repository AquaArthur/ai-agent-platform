# 🤖 AI Agent Platform

一个功能完备的智能体创作平台，支持可视化 AI 智能体编辑、知识库构建、插件系统、工作流编排、多模型接入等能力，提供从用户管理、工作流设计到智能体运行的完整工具链。

## 📋 目录

- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [部署指南](#部署指南)
- [API 文档](#api-文档)
- [开发规范](#开发规范)
- [许可证](#许可证)

## ✨ 功能特性

### 🧠 智能体管理
- 创建、编辑、发布智能体
- 配置系统提示词和提示词模板
- 绑定 LLM 模型、知识库、插件
- 智能体测试与对话

### 📚 知识库管理
- 创建多层级知识库（系统级、学校级、课程级、智能体级、个人级）
- 支持 TXT/Markdown 文档上传
- 自动文档分块与向量化
- 基于 RAG 的语义检索

### 🔧 工作流编排
- 可视化拖拽编辑器（基于 Vue Flow）
- 7 种节点类型：开始、结束、LLM 调用、HTTP 请求、知识库检索、意图识别、字符串处理
- DAG 校验与执行引擎
- 撤销/重做、自动布局、小地图导航

### 🔌 插件系统
- 基于 OpenAPI 3.0 规范的插件注册
- 支持多种鉴权方式（API Key、OAuth 等）
- 插件启用/禁用管理
- 工作流中调用外部 API

### 🎛️ 模型管理
- 多 LLM 提供商支持（DeepSeek、通义千问、豆包等）
- 模型参数配置（温度、最大 Token 等）
- 默认模型设置

### 👥 用户系统
- 用户注册与登录（JWT 认证）
- 双角色权限（普通用户 / 管理员）
- 个人资料管理

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (Vue 3)                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐ │
│  │  Agent  │ │Knowledge│ │Workflow │ │ Plugin  │ │    Chat     │ │
│  │ Manager │ │  Base   │ │ Editor  │ │ Manager │ │   Module    │ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                               │ HTTP/REST
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐ │
│  │  Agent  │ │Workflow │ │ Plugin  │ │   LLM   │ │    User     │ │
│  │ Service │ │ Engine  │ │ Service │ │ Service │ │   Service   │ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
        │                              │
        ▼                              ▼
┌───────────────┐              ┌───────────────┐
│     MySQL     │              │  KB Service   │
│  (主数据存储)  │              │  (Python)     │
└───────────────┘              └───────────────┘
                                       │
                               ┌───────┴───────┐
                               ▼               ▼
                        ┌───────────┐   ┌───────────┐
                        │   Redis   │   │  Celery   │
                        │  (缓存)    │   │ (异步任务) │
                        └───────────┘   └───────────┘
```

## 🛠️ 技术栈

### 前端

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | 3.5 |
| 语言 | TypeScript | 5.9 |
| 构建工具 | Vite | 7.2 |
| UI 组件库 | Element Plus | 2.11 |
| 状态管理 | Pinia | 3.0 |
| 路由 | Vue Router | 4.6 |
| 样式 | Tailwind CSS | 4.1 |
| 代码编辑器 | Monaco Editor | 0.55 |
| 工作流可视化 | Vue Flow | 1.48 |

### 后端

| 服务 | 技术 | 版本 |
|------|------|------|
| 核心服务 | Spring Boot | 3.5.8 |
| JDK | Java | 17 |
| ORM | MyBatis Plus | 3.5.14 |
| 安全认证 | Spring Security + JWT | 0.11.5 |
| API 文档 | SpringDoc OpenAPI | 2.8.13 |
| 知识库服务 | Python + FastAPI | - |
| 异步任务 | Celery + Redis | 5.4+ |

### 数据库与中间件

| 组件 | 用途 |
|------|------|
| MySQL | 结构化数据存储 |
| Redis | 缓存与消息队列 |

### 部署

| 工具 | 用途 |
|------|------|
| Docker | 容器化 |
| Docker Compose | 服务编排 |
| Nginx | 反向代理与静态资源服务 |

## 📁 项目结构

```
ai-agent-platform/
├── frontend/                    # 前端项目
│   ├── src/
│   │   ├── api/                 # API 接口封装
│   │   ├── components/          # 公共组件
│   │   ├── composables/         # 组合式函数
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── styles/              # 全局样式
│   │   ├── types/               # TypeScript 类型定义
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面视图
│   │       ├── agent/           # 智能体管理
│   │       ├── chat/            # 对话模块
│   │       ├── knowledgeBase/   # 知识库管理
│   │       ├── model/           # 模型管理
│   │       ├── plugin/          # 插件管理
│   │       └── workflow/        # 工作流编排
│   ├── Dockerfile.dev           # 开发环境 Dockerfile
│   ├── Dockerfile.prod          # 生产环境 Dockerfile
│   └── package.json
│
├── backend/
│   ├── core/                    # 核心后端服务 (Java/Spring Boot)
│   │   ├── src/main/java/org/demo/core/
│   │   │   ├── controller/      # REST 控制器
│   │   │   ├── service/         # 业务逻辑层
│   │   │   ├── mapper/          # 数据访问层
│   │   │   ├── model/           # 数据模型
│   │   │   │   ├── dto/         # 数据传输对象
│   │   │   │   ├── entity/      # 实体类
│   │   │   │   └── vo/          # 视图对象
│   │   │   ├── config/          # 配置类
│   │   │   ├── workflow/        # 工作流执行引擎
│   │   │   ├── plugin/          # 插件系统
│   │   │   └── util/            # 工具类
│   │   ├── Dockerfile.dev
│   │   ├── Dockerfile.prod
│   │   └── pom.xml
│   │
│   └── kb/                      # 知识库服务 (Python/FastAPI)
│       ├── main.py              # FastAPI 入口
│       ├── rag_service.py       # RAG 检索服务
│       ├── chunker.py           # 文档分块
│       ├── embed_client.py      # 向量化客户端
│       ├── tasks.py             # Celery 异步任务
│       ├── Dockerfile
│       └── requirements.txt
│
├── deploy/                      # 部署配置
│   ├── database/init/           # 数据库初始化脚本
│   ├── docker-compose.dev.yml   # 开发环境编排
│   ├── docker-compose.prod.yml  # 生产环境编排
│   ├── env.dev.example          # 开发环境变量模板
│   ├── env.prod.example         # 生产环境变量模板
│   ├── Makefile                 # 快捷命令
│   └── README.md                # 部署文档
│
├── docs/                        # 项目文档
│   ├── requirements/            # 需求文档
│   ├── database.md              # 数据库设计
│   └── workflow.md              # 工作流引擎文档
│
├── LICENSE                      # GPL-3.0 许可证
└── README.md
```

## 🚀 快速开始

### 环境要求

- Docker & Docker Compose
- Node.js >= 20.x（本地开发）
- JDK 17（本地开发）
- Python 3.10+（本地开发）

### 一键启动（推荐）

```bash
# 克隆项目
git clone <repository-url>
cd ai-agent-platform

# 配置环境变量
cd deploy
cp env.dev.example .env.dev

# 启动开发环境
make dev
```

启动后访问：

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 后端 API | http://localhost:8081 |
| Swagger 文档 | http://localhost:8081/swagger-ui.html |
| 知识库 API | http://localhost:9001/docs |
| phpMyAdmin | http://localhost:8082 |

### 本地开发

#### 前端

```bash
cd frontend
npm install
npm run dev      # 启动开发服务器 (http://localhost:5173)
npm run build    # 生产构建
```

#### 后端（Core）

```bash
cd backend/core
./mvnw spring-boot:run
```

#### 知识库服务

```bash
cd backend/kb
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 9000
```

## 📦 部署指南

### 开发环境

```bash
cd deploy
make dev           # 启动开发环境
make stop-dev      # 停止开发环境
```

默认端口：
- Frontend: 3000
- Backend: 8081
- MySQL: 3307
- KB Service: 9001
- Redis: 6379

### 生产环境

```bash
cd deploy

# 配置生产环境变量（务必修改敏感信息）
cp env.prod.example .env.prod
vim .env.prod

# 启动生产环境
make prod          # 启动生产环境
make stop-prod     # 停止生产环境
```

默认端口：
- Frontend (Nginx): 80
- Backend: 8080
- MySQL: 3306
- KB Service: 9000
- Redis: 6379

详细部署说明请参阅 [deploy/README.md](./deploy/README.md)。

## 📖 API 文档

### 核心 API 模块

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/v1/auth` | 注册、登录 |
| 用户 | `/api/v1/users` | 用户信息管理 |
| 智能体 | `/api/v1/agents` | 智能体 CRUD、发布、对话 |
| 知识库 | `/api/v1/knowledge-bases` | 知识库管理、文档上传、RAG 检索 |
| 工作流 | `/api/v1/workflows` | 工作流管理、执行、调试 |
| 插件 | `/api/v1/plugins` | 插件注册、状态管理 |
| 模型 | `/api/v1/llm` | LLM 模型管理 |
| 系统 | `/api/v1/system` | 系统配置、日志 |

开发环境下可通过 Swagger UI 查看完整 API 文档：
- http://localhost:8081/swagger-ui.html

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1634567890123
}
```

## 📐 开发规范

### Git 工作流

本项目采用 **Feature Branch Workflow**，请遵循以下规范：

#### 分支管理

1. **`main` 分支**：生产分支，始终保持可部署状态，**禁止直接推送**。

2. **功能分支**：
   - 命名格式：`<姓名拼音首字母>/feature/<功能描述>`
   - 示例：`zs/feature/add-user-auth`、`ls/feature/workflow-engine`

#### 开发流程

```bash
# 1. 确保本地 main 分支最新
git checkout main
git pull origin main

# 2. 创建功能分支
git checkout -b zs/feature/your-feature-name

# 3. 开发并提交
git add .
git commit -m "feat: 添加用户认证功能"

# 4. 推送到远程
git push origin zs/feature/your-feature-name

# 5. 创建 Pull Request → main
# 6. 审核通过后，使用 "Squash and merge" 合并
# 7. 合并后删除远程功能分支
```

#### 提交信息规范

推荐使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>(<scope>): <description>

[optional body]
```

类型说明：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

#### 注意事项

- 定期同步 `main` 分支，保持与远程一致
- 创建新分支前，确保本地 `main` 是最新的
- 合并时选择 **Squash and merge**，保持主分支提交记录整洁
- 合并后及时删除远程功能分支

### 代码规范

- **前端**：遵循 Vue 3 Composition API 风格，使用 TypeScript 严格模式
- **后端**：遵循阿里巴巴 Java 开发规范
- **API**：遵循 RESTful 设计原则

## 📄 许可证

本项目采用 [GNU General Public License v3.0](./LICENSE) 开源许可证。

---

## 🔗 相关链接

- [前端开发文档](./frontend/README.md)
- [后端开发文档](./backend/README.md)
- [部署指南](./deploy/README.md)
- [数据库设计文档](./docs/database.md)
- [工作流引擎文档](./docs/workflow.md)
- [需求分析文档](./docs/requirements/requirement.md)

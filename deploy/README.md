# AI Agent Platform 部署指南

## ⚙️ 首次使用前配置

在项目根目录下：

```bash
cd deploy

# 初始化环境变量文件（如已存在可跳过）
cp env.dev.example  .env.dev
cp env.prod.example .env.prod

# ⚠️ 请务必编辑 .env.prod，修改数据库密码等敏感配置
```

确保本机已安装并启动 Docker / Docker Compose（例如 Docker Desktop）。

---

## 🚀 一键启动

### 开发环境（推荐用于开发和联调）

```bash
cd deploy
make dev           # 或 bash deploy.dev.sh
```

启动后可访问：

- 前端（Vite Dev Server）：`http://localhost:3000`
- 后端 API：`http://localhost:8081`
  - Swagger UI：`http://localhost:8081/swagger-ui.html`（dev 默认开启）
- 数据库管理（phpMyAdmin）：`http://localhost:8082`
- 知识库服务（KB 文档）：`http://localhost:9001/docs`
- Redis（消息队列）：`localhost:6379`
- Celery Worker：后台异步任务处理

默认端口（可在 `.env.dev` 中修改）：

- MySQL：`localhost:3307`
- Backend：`localhost:8081`
- Frontend：`localhost:3000`
- KB：`localhost:9001`
- Redis：`localhost:6379`

---

### 生产环境（用于正式部署 / 本地模拟生产）

```bash
cd deploy
make prod          # 或 bash deploy.prod.sh
```

启动后可访问：

- 前端 Web：`http://localhost`  （或 `http://宿主机:WEB_ADMIN_PORT`）
- 后端 API：`http://localhost:8080`
  - 若在 `.env.prod` 中开启 `ENABLE_SWAGGER=true`：`http://localhost:8080/swagger-ui.html`
- 数据库管理（phpMyAdmin）：`http://localhost:8081`
- 知识库服务（KB 文档）：`http://localhost:9000/docs`
- Redis（消息队列）：`localhost:6379`
- Celery Worker：后台异步任务处理

默认端口（可在 `.env.prod` 中修改）：

- MySQL：`localhost:3306`
- Backend：`localhost:8080`
- Frontend：`localhost:80`
- KB：`localhost:9000`
- Redis：`localhost:6379`

---

## 📋 常用命令

在 `deploy` 目录下：

```bash
make help       # 查看可用命令

make dev        # 启动开发环境（等价于 bash deploy.dev.sh）
make prod       # 启动生产环境（等价于 bash deploy.prod.sh）

make stop-dev   # 停止开发环境容器
make stop-prod  # 停止生产环境容器
make stop       # 停止所有环境容器
```

---

## 🔧 简单故障排查

- 使用 `docker ps` 检查容器是否都处于 `Up` 状态。
- 使用 `docker logs <容器名>` 查看具体服务日志，例如：
  - `docker logs ai-agent-platform-dev-backend`
  - `docker logs ai-agent-platform-dev-mysql`
  - `docker logs ai-agent-platform-dev-frontend`
  - `docker logs ai-agent-platform-dev-kb`
  - `docker logs ai-agent-platform-dev-redis`
  - `docker logs ai-agent-platform-dev-celery`

如仍无法解决，可结合日志信息进行排查或提问。



# 🐳 Docker 化部署

## 部署目录
```
ai-agent-platform/
├── frontend/                 # Vue3 + Vite 前端
│   ├── Dockerfile
│   ├── .env.development
│   ├── .env.production
│   └── src/
│
├── backend/
│   └── core/
│       ├── Dockerfile
│       └── target/*.jar     # 构建后的 Spring Boot 可执行 jar
│
├── deploy/      
│   ├── docker-compose.prod.yml
│   ├── .env.prod
│   ├── database/
│   │   ├── schema.sql
│   │   └── data.sql
│   └── deploy.sh            # 一键启动脚本
│
└── README.md
```
---

## 🔧 环境要求
- 软件	版本
- Docker	20+
- Docker Compose	2.x
- Node（本地开发可选）	20+
- Maven（本地后端构建可选）	3.8+

## 🚀 一键部署

项目根目录运行：
```

## 💡 手动部署（如需）
后端打包：
```
cd backend/core
mvn clean package -DskipTests
```

前端打包：
```
cd frontend
npm install
npm run build
```

使用 Docker Compose 启动：
```
cd deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## 🌐 服务访问说明
模块	地址
前端 UI	http://localhost

后端 API	http://localhost:8080

Swagger 文档（Knife4j）	http://localhost:8080/doc.html

Nginx API 代理（前端访问后端）	http://localhost/api/


## 🧪 测试 Docker 状态

查看容器：
```
docker ps
```

查看日志：
```
docker logs aiagent-frontend
docker logs aiagent-backend
docker logs aiagent-mysql
```

## 🎉 部署成功标志
- 正常打开前端：http://localhost
- 后端 API 正常：http://localhost:8080/doc.html
- 后端健康检查：http://localhost/api/v1/hello
- MySQL 启动成功	✔
- 所有容器状态为 Up	✔

 # AI Agent Platform 部署 Makefile

.PHONY: help dev prod stop stop-dev stop-prod

# 默认目标：打印可用命令
help:
	@echo "AI Agent Platform 简化部署命令："
	@echo ""
	@echo "  make dev        - 启动开发环境 (deploy.dev.sh)"
	@echo "  make prod       - 启动生产环境 (deploy.prod.sh)"
	@echo "  make stop-dev   - 停止开发环境容器"
	@echo "  make stop-prod  - 停止生产环境容器"
	@echo "  make stop       - 停止所有容器"
	@echo ""

# 启动开发环境
dev:
	@bash deploy.dev.sh

# 启动生产环境
prod:
	@bash deploy.prod.sh

# 停止开发环境
stop-dev:
	@docker compose -f docker-compose.dev.yml --env-file .env.dev down
	@echo "开发环境已停止"

# 停止生产环境
stop-prod:
	@docker compose -f docker-compose.prod.yml --env-file .env.prod down
	@echo "生产环境已停止"

# 停止所有环境
stop:
	@docker compose -f docker-compose.dev.yml --env-file .env.dev down 2>/dev/null || true
	@docker compose -f docker-compose.prod.yml --env-file .env.prod down 2>/dev/null || true
	@echo "所有服务已停止"

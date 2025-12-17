#!/bin/bash

# 使用 docker-compose.prod.yml + .env.prod 启动所有服务

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "缺少文件: $COMPOSE_FILE"
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少环境变量文件: $ENV_FILE"
  echo "请先从 env.prod.example 复制一份，并修改敏感配置:"
  echo "  cp env.prod.example .env.prod"
  exit 1
fi

echo "[PROD] 使用 $COMPOSE_FILE + $ENV_FILE 启动服务..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build

echo "[PROD] 启动完成。当前相关容器："
docker ps --filter "name=ai-agent-platform-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"



#!/bin/bash

# 使用 docker-compose.dev.yml + .env.dev 启动所有服务

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.dev.yml"
ENV_FILE=".env.dev"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "缺少文件: $COMPOSE_FILE"
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少环境变量文件: $ENV_FILE"
  echo "请先从 env.dev.example 复制一份:"
  echo "  cp env.dev.example .env.dev"
  exit 1
fi

echo "[DEV] 使用 $COMPOSE_FILE + $ENV_FILE 启动服务..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build

echo "[DEV] 启动完成。当前相关容器："
docker ps --filter "name=ai-agent-platform-dev" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"



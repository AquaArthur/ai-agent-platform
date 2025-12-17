# 数据库初始化说明

## 🎯 设计原则

采用**统一初始化脚本 + 环境变量**的方案，实现开发和生产环境的灵活配置。

## 📁 文件结构

```
database/
├── init/
│   ├── 00_init.sql      # 初始化说明（可选）
│   ├── 01_schema.sql    # 表结构定义
│   └── 02_data.sql      # 基础数据
└── README.md            # 本文档
```

## 🔧 工作原理

### MySQL 容器自动初始化
MySQL 容器启动时会：
1. 自动创建 `MYSQL_DATABASE` 环境变量指定的数据库
2. 自动执行 `/docker-entrypoint-initdb.d/` 目录下的 `.sql` 文件
3. 按文件名字母顺序执行（所以用 `01_`, `02_` 前缀）

### 环境变量配置

| 环境 | 数据库名 | 配置文件 |
|------|----------|----------|
| **开发** | `ai_agent_platform_dev_db` | `.env.dev` |
| **生产** | `ai_agent_platform_db` | `.env.prod` |

## 🚀 使用方式

### 开发环境
```bash
# 启动开发环境（会自动创建 ai_agent_platform_dev_db）
make dev
```

### 生产环境
```bash
# 启动生产环境（会自动创建 ai_agent_platform_db）
make prod
```

## ✅ 优势

1. **统一脚本**：只需维护一套 SQL 脚本
2. **环境隔离**：不同环境使用不同数据库名
3. **自动化**：容器启动时自动初始化
4. **灵活性**：通过环境变量控制配置

## 🔍 验证方法

```bash
# 检查数据库是否创建成功
docker exec ai-agent-platform-dev-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "SHOW DATABASES;"

# 检查表是否创建成功
docker exec ai-agent-platform-dev-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} -e "SHOW TABLES;"
```

## 🛠️ 故障排除

如果初始化失败：
1. 检查 SQL 语法是否正确
2. 查看容器日志：`docker logs ai-agent-platform-dev-mysql`
3. 重新创建容器：`make clean-dev` 然后 `make dev`

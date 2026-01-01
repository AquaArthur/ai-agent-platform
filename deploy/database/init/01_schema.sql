-- ============================================================
-- 智能体创作平台数据库初始化脚本
-- 版本: 1.0
-- 创建日期: 2025-11-24
-- 描述: 根据数据库设计文档生成的完整表结构定义
-- ============================================================


-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 数据库已由 MySQL 容器自动创建
-- 容器启动时会根据 MYSQL_DATABASE 环境变量创建数据库
-- ============================================================

-- 注意：MySQL 容器会自动切换到 MYSQL_DATABASE 指定的数据库
-- 所以这里不需要 USE 语句

-- ============================================================
-- 1. 用户表 (user)
-- 功能: 存储用户账号信息及认证数据
-- 关联用户故事: US-019, US-020, US-021
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（必填,用于登录）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称（可选）',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色（user/admin）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '账户状态（active/locked/inactive）',
    `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 智能体表 (agent)
-- 功能: 存储智能体的基本信息、配置及状态
-- 关联用户故事: US-001, US-002, US-003, US-004, US-005, US-017, US-022
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent` (
    `id` VARCHAR(64) NOT NULL COMMENT '智能体唯一标识',
    `name` VARCHAR(100) NOT NULL COMMENT '智能体名称（必填）',
    `description` TEXT DEFAULT NULL COMMENT '智能体描述',
    `prompt` TEXT DEFAULT NULL COMMENT '智能体提示词',
    `prompt_template` TEXT DEFAULT NULL COMMENT '系统提示词模板（别名字段）',
    `model_config` JSON DEFAULT NULL COMMENT '模型配置（存储model、temperature、api_key_id等参数）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '智能体状态（draft/published）',
    `user_id` VARCHAR(64) NOT NULL COMMENT '创建者ID',
    `workflow_id` VARCHAR(64) DEFAULT NULL COMMENT '绑定的工作流ID',
    `workflows` JSON DEFAULT NULL COMMENT '关联的工作流ID列表（JSON数组格式）',
    `knowledge_base_id` VARCHAR(64) DEFAULT NULL COMMENT '绑定的知识库ID',
    `kb_ids` JSON DEFAULT NULL COMMENT '关联的知识库ID列表（JSON数组格式）',
    `tools_config` JSON DEFAULT NULL COMMENT '绑定的插件配置（JSON数组存储插件ID列表）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_workflow` (`workflow_id`),
    CONSTRAINT `fk_agent_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体表';

-- ============================================================
-- 3. 智能体对话历史表 (agent_conversation)
-- 功能: 存储智能体的对话历史记录
-- 关联用户故事: US-004, US-018
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent_conversation` (
    `id` VARCHAR(64) NOT NULL COMMENT '对话记录ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID（聚合多轮对话）',
    `agent_id` VARCHAR(64) NOT NULL COMMENT '智能体ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `query` TEXT NOT NULL COMMENT '用户提问',
    `answer` LONGTEXT NOT NULL COMMENT '智能体回答',
    `metadata` JSON DEFAULT NULL COMMENT '元数据（引用来源、Token消耗）',
    `conversation_type` VARCHAR(20) NOT NULL COMMENT '类型（chat/debug）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_time` (`session_id`, `create_time`),
    KEY `idx_agent_user_time` (`agent_id`, `user_id`, `create_time`),
    CONSTRAINT `fk_conversation_agent` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体对话历史表';

-- ============================================================
-- 4. 工作流表 (workflow)
-- 功能: 存储工作流的定义、配置及状态
-- 关联用户故事: US-011, US-012, US-013
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow` (
    `id` VARCHAR(64) NOT NULL COMMENT '工作流唯一标识',
    `uuid` VARCHAR(36) DEFAULT NULL COMMENT '工作流UUID（用于外部接口）',
    `agent_id` VARCHAR(64) DEFAULT NULL COMMENT '所属智能体ID',
    `name` VARCHAR(100) NOT NULL COMMENT '工作流名称',
    `description` TEXT DEFAULT NULL COMMENT '工作流描述',
    `nodes` JSON NOT NULL COMMENT '节点列表（存储节点配置信息）',
    `edges` JSON NOT NULL COMMENT '边列表（存储节点间连接关系）',
    `config` JSON DEFAULT NULL COMMENT '工作流配置（stop_on_error、timeout、retry_on_failure等）',
    `is_valid` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'DAG校验是否通过（0-否, 1-是）',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '工作流是否激活',
    `is_public` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '工作流是否公开',
    `execution_count` INT NOT NULL DEFAULT 0 COMMENT '执行次数统计',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功次数统计',
    `user_id` VARCHAR(64) NOT NULL COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`),
    KEY `idx_agent` (`agent_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_workflow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_workflow_agent` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表';

-- ============================================================
-- 5. 工作流执行历史表 (workflow_execution)
-- 功能: 记录工作流的每一次动态执行实例
-- 关联用户故事: US-012, US-013
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow_execution` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
    `execution_id` VARCHAR(36) NOT NULL COMMENT '执行UUID（用于外部查询）',
    `workflow_id` VARCHAR(64) NOT NULL COMMENT '工作流ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '执行者ID',
    `status` VARCHAR(20) NOT NULL COMMENT '状态（pending/running/completed/failed/terminated）',
    `input` JSON NOT NULL COMMENT '初始输入参数',
    `output` JSON DEFAULT NULL COMMENT '最终输出结果',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `node_executions` JSON DEFAULT NULL COMMENT '节点执行快照（各节点的执行记录）',
    `run_type` VARCHAR(20) NOT NULL COMMENT '类型（full-完整执行/debug-调试执行）',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `execution_time` INT DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_execution_id` (`execution_id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_execution_id` (`execution_id`),
    KEY `idx_status` (`status`),
    KEY `idx_workflow_time` (`workflow_id`, `create_time`),
    CONSTRAINT `fk_execution_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_execution_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行历史表';

-- ============================================================
-- 6. 知识库表 (knowledge_base)
-- 功能: 存储知识库的基本信息，支持分级设计
-- 关联用户故事: US-006, US-008, US-010
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` VARCHAR(64) NOT NULL COMMENT '知识库唯一标识',
    `uuid` VARCHAR(36) DEFAULT NULL COMMENT 'UUID标识',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称（必填）',
    `description` TEXT DEFAULT NULL COMMENT '知识库描述',
    `icon` VARCHAR(200) DEFAULT NULL COMMENT '知识库图标URL',
    
    -- 层级与归属
    `scope_type` VARCHAR(20) NOT NULL DEFAULT 'personal' COMMENT '作用域类型（system/school/course/agent/personal）',
    `scope_id` INT DEFAULT NULL COMMENT '作用域ID',
    `parent_kb_id` VARCHAR(64) DEFAULT NULL COMMENT '父知识库ID',
    
    -- 创建者与权限
    `owner_id` VARCHAR(64) NOT NULL COMMENT '创建者ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '创建者ID（别名字段）',
    `access_level` VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '访问级别（public/protected/private）',
    
    -- 统计信息
    `document_count` INT NOT NULL DEFAULT 0 COMMENT '文档数量',
    `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '分块数量',
    `total_size` BIGINT NOT NULL DEFAULT 0 COMMENT '总文件大小（字节）',
    
    -- 配置参数
    `chunk_size` INT NOT NULL DEFAULT 800 COMMENT '分块大小',
    `chunk_overlap` INT NOT NULL DEFAULT 50 COMMENT '分块重叠',
    `embedding_model` VARCHAR(50) DEFAULT NULL COMMENT '向量模型（如text-embedding-3）',
    `embedding_model_id` VARCHAR(64) DEFAULT NULL COMMENT '向量模型ID（外键）',
    `retrieval_config` JSON DEFAULT NULL COMMENT '检索配置',
    
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`),
    KEY `idx_scope` (`scope_type`, `scope_id`),
    KEY `idx_access_level` (`access_level`),
    KEY `idx_parent_kb` (`parent_kb_id`),
    CONSTRAINT `fk_kb_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kb_parent` FOREIGN KEY (`parent_kb_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';


-- ============================================================
-- 7. 智能体知识库关联表 (agent_knowledge_base)
-- 功能: 存储智能体与知识库的多对多关联关系
-- 关联用户故事: US-001, US-006
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent_knowledge_base` (
    `id` VARCHAR(64) NOT NULL COMMENT '关联记录唯一标识',
    `agent_id` VARCHAR(64) NOT NULL COMMENT '智能体ID',
    `knowledge_base_id` VARCHAR(64) NOT NULL COMMENT '知识库ID',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
    `is_enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_kb` (`agent_id`, `knowledge_base_id`),
    KEY `idx_agent` (`agent_id`),
    KEY `idx_kb` (`knowledge_base_id`),
    KEY `idx_priority` (`priority`),
    CONSTRAINT `fk_agent_kb_agent` FOREIGN KEY (`agent_id`) REFERENCES `agent` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_agent_kb_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体知识库关联表';

-- ============================================================
-- 8. 文档表 (document)
-- 功能: 存储知识库中上传的文档信息及处理状态
-- 关联用户故事: US-007, US-008
-- ============================================================
CREATE TABLE IF NOT EXISTS `document` (
    `id` VARCHAR(64) NOT NULL COMMENT '文档唯一标识',
    `uuid` VARCHAR(36) NOT NULL COMMENT '文档UUID（用于外部接口）',
    `name` VARCHAR(255) NOT NULL COMMENT '文档名称（显示名称）',
    `filename` VARCHAR(255) NOT NULL COMMENT '文档文件名（原始文件名）',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文档文件名（别名字段）',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件存储URL（对象存储地址）',
    `file_path` VARCHAR(512) DEFAULT NULL COMMENT '文档存储路径',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文档大小（字节）',
    `file_type` VARCHAR(50) NOT NULL COMMENT '文档类型（txt/md/markdown）',
    `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '切分片段数量',
    `status` VARCHAR(20) NOT NULL DEFAULT 'uploading' COMMENT '处理状态（uploading/processing/processed/failed）',
    `process_status` TINYINT DEFAULT NULL COMMENT '处理状态数值（0-上传中, 1-处理中, 2-已完成, 3-失败）',
    `error_message` TEXT DEFAULT NULL COMMENT '处理失败原因',
    `processed_at` DATETIME DEFAULT NULL COMMENT '处理完成时间',
    `knowledge_base_id` VARCHAR(64) NOT NULL COMMENT '所属知识库ID',
    `kb_id` VARCHAR(64) DEFAULT NULL COMMENT '所属知识库ID（别名字段）',
    `user_id` VARCHAR(64) NOT NULL COMMENT '上传者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（别名字段）',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（别名字段）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    KEY `idx_kb_status` (`knowledge_base_id`, `status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_doc_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_doc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ============================================================
-- 9. 插件表 (plugin)
-- 功能: 存储插件的注册信息、配置及状态
-- 关联用户故事: US-014, US-015, US-016
-- ============================================================
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` VARCHAR(64) NOT NULL COMMENT '插件唯一标识',
    `name` VARCHAR(100) NOT NULL COMMENT '插件名称',
    `identifier` VARCHAR(100) DEFAULT NULL COMMENT '插件唯一标识符（key）',
    `description` TEXT DEFAULT NULL COMMENT '插件描述',
    `type` VARCHAR(20) NOT NULL DEFAULT 'http' COMMENT '插件类型（http/mqtt/local等）',
    `base_url` VARCHAR(255) DEFAULT NULL COMMENT '基础请求地址（如 https://plugin.aiot.hello1023.com）',
    `openapi_spec` JSON NOT NULL COMMENT 'OpenAPI规范内容',
    `openapi_schema` JSON DEFAULT NULL COMMENT 'OpenAPI规范（别名字段）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'disabled' COMMENT '插件状态（enabled/disabled）',
    `is_enabled` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用',
    `auth_info` JSON DEFAULT NULL COMMENT '鉴权信息（存储API Key等）',
    `auth_type` VARCHAR(20) DEFAULT 'none' COMMENT '鉴权类型（none/api_key/oauth等）',
    `auth_config` JSON DEFAULT NULL COMMENT '鉴权配置（别名字段）',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '注册者ID（NULL代表系统插件）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_identifier` (`identifier`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`),
    KEY `idx_status` (`status`),
    KEY `idx_type` (`type`),
    CONSTRAINT `fk_plugin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';

-- ============================================================
-- 10. 插件操作表 (plugin_operation)
-- 功能: 存储每个插件的接口操作信息
-- 关联用户故事: US-014, US-015, US-016
-- ============================================================
CREATE TABLE IF NOT EXISTS `plugin_operation` (
    `id` VARCHAR(64) NOT NULL COMMENT '插件操作唯一标识',
    `plugin_id` VARCHAR(64) NOT NULL COMMENT '所属插件ID',
    `operation_id` VARCHAR(100) NOT NULL COMMENT 'OpenAPI中的operationId（如getSensorData）',
    `name` VARCHAR(100) NOT NULL COMMENT '操作名称，用于前端展示',
    `method` VARCHAR(10) NOT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE等）',
    `path` VARCHAR(255) NOT NULL COMMENT '请求路径（如/plugin/sensor-data）',
    `description` TEXT DEFAULT NULL COMMENT '操作描述',
    `input_schema` JSON DEFAULT NULL COMMENT '入参结构（从OpenAPI解析的参数信息）',
    `output_schema` JSON DEFAULT NULL COMMENT '出参结构（从OpenAPI解析的响应信息）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_plugin_id` (`plugin_id`),
    CONSTRAINT `fk_operation_plugin` FOREIGN KEY (`plugin_id`) REFERENCES `plugin` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件操作表';

-- ============================================================
-- 11. 系统日志表 (system_log)
-- 功能: 存储系统操作日志及审计信息
-- 关联用户故事: US-023
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_log` (
    `id` VARCHAR(64) NOT NULL COMMENT '日志唯一标识',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID（NULL表示系统操作）',
    `module` VARCHAR(50) NOT NULL COMMENT '操作模块（agent/workflow/plugin/knowledge_base等）',
    `action` VARCHAR(50) NOT NULL COMMENT '操作动作（create/update/delete/execute等）',
    `level` VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '日志级别（info/warn/error）',
    `content` TEXT DEFAULT NULL COMMENT '日志详情/错误堆栈',
    `request_params` JSON DEFAULT NULL COMMENT '请求参数快照（用于审计）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_module` (`module`),
    KEY `idx_user` (`user_id`),
    CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- ============================================================
-- 12. 系统配置表 (system_config)
-- 功能: 存储系统全局配置信息
-- 关联用户故事: US-022
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` VARCHAR(64) NOT NULL COMMENT '配置唯一标识',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键（如default_model、max_upload_size）',
    `config_value` TEXT NOT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================================
-- 13. LLM提供商表 (llm_providers)
-- 功能: 存储LLM提供商信息
-- 关联用户故事: US-022
-- ============================================================
CREATE TABLE IF NOT EXISTS `llm_providers` (
    `id` VARCHAR(64) NOT NULL COMMENT '提供商唯一标识',
    `code` VARCHAR(50) NOT NULL COMMENT '提供商代码',
    `name` VARCHAR(100) NOT NULL COMMENT '提供商名称',
    `title` VARCHAR(200) NOT NULL COMMENT '提供商完整标题',
    `description` TEXT DEFAULT NULL COMMENT '提供商描述',
    `apply_url` VARCHAR(500) DEFAULT NULL COMMENT 'API申请地址',
    `doc_url` VARCHAR(500) DEFAULT NULL COMMENT '文档地址',
    `default_api_base` VARCHAR(500) DEFAULT NULL COMMENT '默认API地址',
    `has_free_quota` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否提供免费额度',
    `icon` VARCHAR(200) DEFAULT NULL COMMENT '图标URL或图标名称',
    `tag_type` VARCHAR(20) DEFAULT NULL COMMENT '标签类型',
    `country` VARCHAR(20) DEFAULT NULL COMMENT '国家',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM提供商表';

-- ============================================================
-- 14. LLM模型表 (llm_models)
-- 功能: 存储LLM模型配置信息
-- 关联用户故事: US-022
-- ============================================================
CREATE TABLE IF NOT EXISTS `llm_models` (
    `id` VARCHAR(64) NOT NULL COMMENT '模型唯一标识',
    `name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `display_name` VARCHAR(100) NOT NULL COMMENT '模型显示名称',
    `provider` VARCHAR(50) NOT NULL COMMENT '提供商代码',
    `model_type` VARCHAR(50) DEFAULT NULL COMMENT '模型类型',
    `api_base` VARCHAR(500) DEFAULT NULL COMMENT 'API基础URL',
    `api_key` VARCHAR(500) DEFAULT NULL COMMENT 'API密钥',
    `api_version` VARCHAR(50) DEFAULT NULL COMMENT 'API版本',
    `max_tokens` INT DEFAULT NULL COMMENT '最大token数',
    `temperature` DECIMAL(3,2) DEFAULT NULL COMMENT '温度参数',
    `top_p` DECIMAL(3,2) DEFAULT NULL COMMENT 'top_p参数',
    `enable_deep_thinking` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用深度思考',
    `frequency_penalty` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '频率惩罚参数',
    `presence_penalty` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '存在惩罚参数',
    `config` JSON DEFAULT NULL COMMENT '其他配置参数',
    `description` TEXT DEFAULT NULL COMMENT '模型描述',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否激活',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模型',
    `is_system` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_provider` (`provider`),
    KEY `idx_is_active_is_default` (`is_active`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM模型表';

-- ============================================================
-- 15. 向量表 (vector)
-- 功能: 存储文档分片后的向量

-- ============================================================
create table if not exists `vector` (
    id          bigint auto_increment comment '向量唯一标识'
        primary key,
    document_id VARCHAR(64)                         not null comment '所属文档id',
    kb_id       VARCHAR(64)                         not null comment '所属知识库id',
    chunk_index int                                 not null comment '分片序号，从0开始',
    chunk_text  text                                not null comment 'chunk文本内容',
    embedding   json                                not null comment '向量，存储为JSON 数组',
    vector_dim  int                                 null comment '向量维度，便于一致性检查',
    create_time timestamp default CURRENT_TIMESTAMP null,
    constraint fk_vector_kb
        foreign key (kb_id) references `knowledge_base` (id)
            on delete cascade
)
ENGINE=InnoDB 
DEFAULT CHARSET=utf8mb4 
COLLATE=utf8mb4_unicode_ci  
comment '文档向量表';

create index idx_kb_document
    on `vector` (document_id, kb_id);

-- ============================================================
-- 初始化系统配置数据
-- ============================================================
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `description`) VALUES
('config_001', 'default_model', 'gpt-4', '默认大模型'),
('config_002', 'max_upload_size', '104857600', '最大上传文件大小（字节，默认100MB）'),
('config_003', 'enable_registration', 'true', '是否开放注册');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 脚本执行完成
-- ============================================================

-- ============================================================
-- 智能体创作平台初始数据
-- 版本: 1.0
-- 创建日期: 2025-11-24
-- 描述: 为 ai_agent_platform_db 数据库填充基础测试数据
-- ============================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `ai_agent_platform_db`;

-- ------------------------------------------------------------
-- 清空现有测试数据（可选，但推荐在测试环境中执行）
-- ------------------------------------------------------------
DELETE FROM `system_log`;
DELETE FROM `workflow_run`;
DELETE FROM `agent_conversation`;
DELETE FROM `agent`;
DELETE FROM `workflow`;
DELETE FROM `plugin_operation`;
DELETE FROM `plugin`;
DELETE FROM `document`;
DELETE FROM `knowledge_base`;
DELETE FROM `user`;
-- 注意：system_config 表数据通常在建表后就固定了，不在此处删除。


-- ============================================================
-- 1. 用户表 (user) - 增加普通用户
-- 密码字段为 BCrypt 加密后的 'password123'
-- ============================================================
INSERT INTO `user` (`id`, `username`, `email`, `password`, `nickname`, `role`, `status`, `email_verified`, `create_time`) VALUES
('user-001-admin', 'sysadmin', 'admin@homeagent.com', '$2a$10$Q.2K0u4q0h.6gP2xMv4f0O/l6uR9x.QvK4zYp.E2D7T9Zz8G6g/w.', '平台管理员', 'admin', 'active', TRUE, '2025-11-01 10:00:00'),
('user-002-home', 'home_creator', 'smart_home@homeagent.com', '$2a$10$Q.2K0u4q0h.6gP2xMv4f0O/l6uR9x.QvK4zYp.E2D7T9Zz8G6g/w.', '智能家居管家', 'user', 'active', TRUE, '2025-11-01 10:05:00'),
('user-003-tester', 'testuser', 'tester@homeagent.com', '$2a$10$Q.2K0u4q0h.6gP2xMv4f0O/l6uR9x.QvK4zYp.E2D7T9Zz8G6g/w.', '家庭用户A', 'user', 'active', TRUE, '2025-11-05 15:30:00'),
('user-004-dev', 'plugin_dev', 'dev@homeagent.com', '$2a$10$Q.2K0u4q0h.6gP2xMv4f0O/l6uR9x.QvK4zYp.E2D7T9Zz8G6g/w.', '插件开发者', 'user', 'active', TRUE, '2025-11-08 09:00:00');


-- ============================================================
-- 2. 智能体表 (agent) - 增加一个仅用插件的Agent
-- ------------------------------------------------------------
INSERT INTO `agent` (`id`, `name`, `description`, `prompt`, `model_config`, `status`, `user_id`, `workflow_id`, `kb_ids`, `tools_config`, `create_time`) VALUES
('agent-001-smarthome', '智能家居助理', '你可以控制家里的LED灯，查询室内温度，并能回答关于设备文档的问题。', '你是一个友好的智能家居助手，可以帮助用户控制IoT设备。

## 设备信息
设备UUID: ab3b34d1-fae0-489b-80e8-19a8a6c7543d

## 你的能力

### 1️⃣ 查询传感器数据
- 温度查询：当用户问"温度多少"、"几度"、"热不热"
- 湿度查询：当用户问"湿度多少"、"潮湿吗"、"干燥吗"

### 2️⃣ 控制LED灯
- 支持LED 1-4号
- 开灯：当用户说"打开灯"、"开灯"、"点亮LED"
- 关灯：当用户说"关灯"、"关闭灯"、"熄灯"

### 3️⃣ 执行预设指令

**可用预设列表：**

| 预设名称 | 触发词 | preset_key | 说明 |
|---------|--------|------------|------|
| 眨眼睛 | "眨眼睛"、"眨一下" | led_seq_mi71o69r | LED1点亮3秒后熄灭 |

**使用方法：**
当用户说出触发词时，使用对应的preset_key调用预设接口。

## 交互规则

### ✅ 应该做的
1. 当用户查询温度/湿度时，直接调用传感器接口
2. 当用户要控制LED时，先确认是哪个LED（1-4），然后调用控制接口
3. 当用户说出预设触发词（如"眨眼睛"）时，直接使用对应的preset_key调用预设接口
4. 用简洁友好的语言回复结果
5. 如果用户指令不明确，主动询问清楚

### ❌ 不要做的
1. 不要向用户索要设备UUID（已经通过变量传入）
2. 不要提供超出能力范围的功能（如继电器、舵机、PWM等）
3. 不要过度解释技术细节
4. 不要在用户没问的情况下重复查询数据

## 回复示例

### 传感器查询
用户："现在温度多少？"
你：[调用传感器接口]
你："当前温度是24.5°C 😊"

### LED控制
用户："帮我开灯"
你："好的，请问要打开哪个LED灯呢？我们有LED 1到4号"
用户："LED1"
你：[调用控制接口]
你："✨ LED1已打开"

### 预设指令
用户："眨眼睛"
你：[调用预设接口，preset_name="led_seq_mi71o69r"]
你："✅ LED1将点亮3秒后自动熄灭"

## 特别提示
- 所有操作都自动使用设备UUID变量，你无需管理
- 如果接口返回错误，友好地告知用户"暂时无法操作，请稍后重试"
- 保持对话自然流畅，像朋友一样交流', '{"model": "model-001-qwen-turbo", "temperature": 0.2}', 'published', 'user-002-home', 'wf-001-home-ctrl', '["kb-001-dev", "kb-002-faq"]', '["plugin_be2e083736e0"]', '2025-11-22 10:00:00'),
('agent-002-scheduler', '日程管理Agent', '专门用于处理家庭日程、提醒和日历查询。', '你是一个日程管理专家，请利用日历插件帮助用户安排生活。', '{"model": "gpt-3.5-turbo", "temperature": 0.5}', 'draft', 'user-002-home', NULL, '[]', '["plugin-003-calendar"]', '2025-11-23 09:30:00');

-- 关联 Agent 和 Workflow
UPDATE `workflow` SET `agent_id` = 'agent-001-smarthome' WHERE `id` = 'wf-001-home-ctrl';
-- 注意：工作流 wf-002-auto-off 是一个定时任务，可能不会绑定到任何用户可见的 Agent。


-- ============================================================
-- 3. 智能体对话历史表 (agent_conversation) - 增加对话轮次
-- ------------------------------------------------------------
INSERT INTO `agent_conversation` (`id`, `session_id`, `agent_id`, `user_id`, `query`, `answer`, `conversation_type`, `create_time`, `metadata`) VALUES
('conv-001', 'sess-home-001', 'agent-001-smarthome', 'user-003-tester', '帮我把客厅的灯打开。', '好的，已调用 [智能灯光控制] 插件，客厅灯已开启。', 'chat', '2025-11-23 11:00:00', '{"token_usage": 50}'),
('conv-002', 'sess-home-001', 'agent-001-smarthome', 'user-003-tester', '现在房间里温度是多少？', '正在查询... [室内温度查询] 插件返回：当前室内温度是26.5°C，湿度55%。', 'chat', '2025-11-23 11:01:30', '{"token_usage": 65, "plugin_call": "temperature_sensor"}'),
('conv-003', 'sess-home-002', 'agent-001-smarthome', 'user-003-tester', 'LED灯的手册在哪里？', '根据知识库[智能设备开发文档]，LED灯的手册是 “LED_Manual.pdf”，请查阅。', 'chat', '2025-11-24 10:00:00', '{"token_usage": 80, "kb_hit": "kb-001-dev"}'),
('conv-004', 'sess-home-003', 'agent-001-smarthome', 'user-003-tester', '怎么给设备排除故障？', '请参考知识库 [常见问题解答] 中的 “Troubleshooting.md”，里面提供了详细的故障排除步骤。', 'chat', '2025-11-24 11:00:00', '{"token_usage": 90, "kb_hit": "kb-002-faq"}'),
('conv-005', 'sess-home-003', 'agent-001-smarthome', 'user-003-tester', '把卧室灯调成红色。', '已调用 [智能灯光控制] 插件，卧室灯颜色已调整为红色。', 'chat', '2025-11-24 11:01:30', '{"token_usage": 70, "plugin_call": "led_controller"}');


-- ============================================================
-- 5. 工作流表 (workflow) - 增加一个定时任务工作流
-- ------------------------------------------------------------
INSERT INTO `workflow` (`id`, `name`, `description`, `definition`, `graph_data`, `is_valid`, `status`, `user_id`, `create_time`) VALUES
('wf-001-home-ctrl', '智能家居控制与反馈', '根据用户请求，执行灯光控制或温度查询，并提供知识库支持。', '{"nodes": ["start", "check_intent", "call_plugin", "llm_feedback"], "edges": []}', '{"nodes": [], "edges": []}', TRUE, 'active', 'user-002-home', '2025-11-20 09:00:00'),
('wf-002-auto-off', '定时关闭卧室灯', '每天晚上11点检查卧室灯状态,如果开启则自动关闭。', '{"nodes": ["cron_trigger", "check_status", "call_led_off"], "edges": []}', '{"nodes": [], "edges": []}', TRUE, 'active', 'user-002-home', '2025-11-21 15:00:00');


-- ============================================================
-- 6. 工作流执行历史表 (workflow_run) - 增加执行记录
-- ------------------------------------------------------------
INSERT INTO `workflow_run` (`id`, `workflow_id`, `user_id`, `status`, `inputs`, `outputs`, `run_type`, `start_time`, `end_time`, `create_time`) VALUES
('run-001', 'wf-001-home-ctrl', 'user-003-tester', 'completed', '{"intent": "turn_on_light", "location": "living_room"}', '{"result": "success", "plugin_used": "led_controller"}', 'full', '2025-11-23 11:00:00', '2025-11-23 11:00:03', '2025-11-23 11:00:00'),
('run-002', 'wf-001-home-ctrl', 'user-003-tester', 'completed', '{"intent": "get_temperature", "location": "room"}', '{"result": "success", "temperature": 26.5, "plugin_used": "temperature_sensor"}', 'full', '2025-11-23 11:01:30', '2025-11-23 11:01:32', '2025-11-23 11:01:30'),
('run-003', 'wf-002-auto-off', 'user-002-home', 'completed', '{"time": "23:00"}', '{"result": "success", "status": "light already off"}', 'full', '2025-11-23 23:00:00', '2025-11-23 23:00:01', '2025-11-23 23:00:00'),
('run-004', 'wf-001-home-ctrl', 'user-003-tester', 'failed', '{"intent": "turn_on_light", "location": "balcony"}', '{"error": "Device not found"}', 'full', '2025-11-24 12:00:00', '2025-11-24 12:00:05', '2025-11-24 12:00:00');


-- ============================================================
-- 7. 知识库表 (knowledge_base) - 增加第二个知识库
-- ------------------------------------------------------------
INSERT INTO `knowledge_base` (`id`, `uuid`, `name`, `description`, `icon`, `scope_type`, `scope_id`, `parent_kb_id`, `owner_id`, `user_id`, `access_level`, `document_count`, `chunk_count`, `total_size`, `chunk_size`, `chunk_overlap`, `embedding_model`, `embedding_model_id`, `retrieval_config`, `create_time`) VALUES
('kb-001-dev', 'kb-uuid-001', '智能设备开发文档', '包含LED灯、传感器等设备的API和故障排除文档。', NULL, 'personal', NULL, NULL, 'user-002-home', 'user-002-home', 'private', 2, 40, 1098576, 800, 50, 'text-embedding-3-small', NULL, '{"top_k": 5, "similarity_threshold": 0.4, "max_context_length": 2000}', '2025-11-10 09:10:00'),
('kb-002-faq', 'kb-uuid-002', '常见问题解答', '用户对智能家居系统的常见疑问及标准答案。', NULL, 'system', NULL, NULL, 'user-001-admin', 'user-001-admin', 'public', 2, 35, 780288, 800, 50, 'text-embedding-3-small', NULL, '{"top_k": 5, "similarity_threshold": 0.4, "max_context_length": 2000}', '2025-11-12 14:00:00'),
('kb-003-tt', 'kb-uuid-003', '测试知识库', '测试知识库', null, 'system', null, null, 'user-001-admin', 'user-001-admin', 'public', 1, 3, 1145, 800, 50, 'text-embedding-v4', null, '{"top_k": 5, "max_context_length": 2000, "similarity_threshold": 0.4}', '2025-12-08 21:33:26', '2025-12-08 21:33:29');


-- ============================================================
-- 8. 智能体知识库关联表 (agent_knowledge_base) - 增加关联记录
-- ------------------------------------------------------------
INSERT INTO `agent_knowledge_base` (`id`, `agent_id`, `knowledge_base_id`, `priority`, `is_enabled`, `create_time`) VALUES
('akb-001', 'agent-001-smarthome', 'kb-001-dev', 10, TRUE, '2025-11-22 10:10:00'),
('akb-002', 'agent-001-smarthome', 'kb-002-faq', 5, TRUE, '2025-11-22 10:15:00'),
('akb-003', 'agent-002-scheduler', 'kb-002-faq', 1, FALSE, '2025-11-23 09:35:00'),
('akb-004', 'agent-001-smarthome', 'kb-003-tt', 1, TRUE, '2025-11-23 09:35:00');

-- ============================================================
-- 9. 文档表 (document) - 增加文档数量
-- ------------------------------------------------------------
INSERT INTO `document` (`id`, `uuid`, `name`, `filename`, `file_name`, `file_url`, `file_path`, `file_size`, `file_type`, `chunk_count`, `status`, `process_status`, `error_message`, `processed_at`, `knowledge_base_id`, `kb_id`, `user_id`, `create_time`, `created_at`, `update_time`, `updated_at`) VALUES
('doc-001', 'doc-uuid-001', 'LED设备操作手册', 'LED_Manual.txt', 'LED_Manual.txt', 'http://storage.com/kb-001/led_manual.txt', '/files/kb-001/led_manual.txt', 1048576, 'txt', 30, 'processed', 2, NULL, '2025-11-10 10:35:00', 'kb-001-dev', 'kb-001-dev', 'user-002-home', '2025-11-10 10:30:00', '2025-11-10 10:30:00', '2025-11-10 10:35:00', '2025-11-10 10:35:00'),
('doc-002', 'doc-uuid-002', '温度传感器技术规格', 'Temp_Sensor_Spec.txt', 'Temp_Sensor_Spec.txt', 'http://storage.com/kb-001/temp_spec.txt', '/files/kb-001/temp_spec.txt', 50000, 'txt', 10, 'processed', 2, NULL, '2025-11-10 11:05:00', 'kb-001-dev', 'kb-001-dev', 'user-002-home', '2025-11-10 11:00:00', '2025-11-10 11:00:00', '2025-11-10 11:05:00', '2025-11-10 11:05:00'),
('doc-003', 'doc-uuid-003', '常见问题排查指南', 'Troubleshooting.md', 'Troubleshooting.md', 'http://storage.com/kb-002/trouble.md', '/files/kb-002/trouble.md', 256000, 'md', 20, 'processed', 2, NULL, '2025-11-13 09:10:00', 'kb-002-faq', 'kb-002-faq', 'user-001-admin', '2025-11-13 09:00:00', '2025-11-13 09:00:00', '2025-11-13 09:10:00', '2025-11-13 09:10:00'),
('doc-004', 'doc-uuid-004', '设备安装指南', 'Installation_Guide.md', 'Installation_Guide.md', 'http://storage.com/kb-002/install.md', '/files/kb-002/install.md', 524288, 'markdown', 15, 'processing', 1, NULL, NULL, 'kb-002-faq', 'kb-002-faq', 'user-001-admin', '2025-11-13 10:00:00', '2025-11-13 10:00:00', '2025-11-13 10:00:00', '2025-11-13 10:00:00'),
('doc-005', 'doc-uuid-005', '测试文档', '测试文档.md', '测试文档.md', 'http://storage.com/kb-002/测试文档.md', '/files/kb-003/test.md', 1111, 'markdown', 3, DEFAULT, null, null, null, 'kb-003-tt', 'kb-003-tt', 'user-001-admin', DEFAULT, DEFAULT, DEFAULT, DEFAULT);


-- ============================================================
-- 10. 插件表 (plugin) - 增加一个第三方插件
-- ------------------------------------------------------------
INSERT INTO `plugin` (`id`, `name`, `identifier`, `description`, `type`, `base_url`, `openapi_spec`, `status`, `is_enabled`, `auth_type`, `user_id`, `create_time`) VALUES
('plugin-001-led', '智能灯光控制', 'led_controller', '用于开启、关闭和调整智能LED灯的亮度或颜色。', 'http', 'https://plugin.smarthome.local', '{"openapi": "3.0.0", "info": {"title": "LED Control API"}, "paths": {"/light/on": {}, "/light/off": {}}}', 'enabled', TRUE, 'api_key', 'user-002-home', '2025-11-15 10:00:00'),
('plugin-002-temp', '室内温度查询', 'temperature_sensor', '获取当前房间的实时温度和湿度数据。', 'http', 'https://plugin.smarthome.local', '{"openapi": "3.0.0", "info": {"title": "Temperature API"}, "paths": {"/sensor/current_temp": {}}}', 'enabled', TRUE, 'none', 'user-002-home', '2025-11-15 11:30:00'),
('plugin-003-calendar', '家庭日程提醒', 'family_calendar', '用于查询和添加家庭共享日历事件。', 'http', 'https://calendar.api.local', '{"openapi": "3.0.0", "info": {"title": "Calendar API"}, "paths": {"/events": {}}}', 'disabled', FALSE, 'oauth', 'user-004-dev', '2025-11-18 14:00:00'),
('plugin_be2e083736e0','IoT设备控制','iot','传感器查询、设备控制（LED/继电器/舵机/PWM）、预设指令','http','https://plugin.aiot.hello1023.com','{"type": "openapi", "baseUrl": "https://plugin.aiot.hello1023.com", "originalSpec": {"info": {"title": "IoT设备控制", "version": "1.2.0", "description": "传感器查询、设备控制（LED/继电器/舵机/PWM）、预设指令"}, "paths": {"/plugin/preset": {"post": {"tags": ["预设指令"], "summary": "执行预设", "responses": {"200": {"content": {"application/json": {"schema": {"type": "object", "properties": {"msg": {"type": "string", "example": "成功"}, "code": {"type": "integer", "example": 200}, "data": {"type": "object", "properties": {"result": {"type": "string", "example": "success"}}}}}}}, "description": "成功"}}, "description": "通过preset_key执行用户自定义预设", "operationId": "executePreset", "requestBody": {"content": {"application/json": {"schema": {"type": "object", "required": ["device_uuid", "preset_name"], "properties": {"parameters": {"type": "object", "example": {}, "description": "可选参数（通常为空）", "additionalProperties": true}, "device_uuid": {"type": "string", "example": "test", "description": "设备UUID"}, "preset_name": {"type": "string", "example": "led_blink_k8x9y2", "description": "预设标识(preset_key)，如：led_blink_k8x9y2"}}}, "example": {"parameters": {}, "device_uuid": "test", "preset_name": "led_blink_k8x9y2"}}}, "required": true}}}, "/plugin/control": {"post": {"tags": ["设备控制"], "summary": "控制设备", "responses": {"200": {"content": {"application/json": {"schema": {"type": "object", "properties": {"msg": {"type": "string", "example": "成功"}, "code": {"type": "integer", "example": 200}, "data": {"type": "object", "properties": {"result": {"type": "string", "example": "success"}}}}}}}, "description": "成功"}}, "description": "控制LED、继电器、舵机、PWM等设备", "operationId": "controlDevice", "requestBody": {"content": {"application/json": {"schema": {"type": "object", "required": ["device_uuid", "port_type", "port_id", "action"], "properties": {"value": {"type": "integer", "example": 90, "maximum": 180, "minimum": 0, "description": "设置值：舵机角度(0-180)或PWM占空比(0-100)，仅当action为set时需要"}, "action": {"enum": ["on", "off", "set"], "type": "string", "example": "on", "description": "动作：on(打开)/off(关闭)/set(设置值，用于舵机角度或PWM占空比)"}, "port_id": {"type": "integer", "example": 1, "maximum": 4, "minimum": 1, "description": "端口ID：LED和继电器为1-4，舵机为1-4，PWM为1-2"}, "port_type": {"enum": ["led", "relay", "servo", "pwm"], "type": "string", "example": "led", "description": "设备类型：led(LED灯)、relay(继电器)、servo(舵机)、pwm(PWM输出)"}, "device_uuid": {"type": "string", "example": "test", "description": "设备UUID"}}}, "example": {"action": "on", "port_id": 1, "port_type": "led", "device_uuid": "test"}}}, "required": true}}}, "/plugin/sensor-data": {"get": {"tags": ["传感器"], "summary": "查询传感器", "responses": {"200": {"content": {"application/json": {"schema": {"type": "object", "properties": {"msg": {"type": "string", "example": "成功"}, "code": {"type": "integer", "example": 200}, "data": {"type": "object", "properties": {"unit": {"type": "string", "example": "°C"}, "value": {"type": "number", "example": 24.5}}}}}, "example": {"msg": "成功", "code": 200, "data": {"unit": "°C", "value": 24.5}}}}, "description": "成功"}}, "parameters": [{"in": "query", "name": "uuid", "schema": {"type": "string", "example": "test"}, "required": true, "description": "UUID"}, {"in": "query", "name": "sensor", "schema": {"enum": ["温度", "湿度", "雨水", "雨水级别", "DS18B20", "DS18B20温度", "temperature", "humidity", "rain", "rain_level"], "type": "string", "example": "温度"}, "required": true, "description": "传感器类型"}], "description": "获取各类传感器数据（温度、湿度、雨水、DS18B20等）", "operationId": "getSensorData"}}}, "openapi": "3.0.0", "servers": [{"url": "https://plugin.aiot.hello1023.com", "description": "生产服务器"}]}}','enabled',TRUE,'none','user-004-dev','2025-12-03 14:37:30');

-- ============================================================
-- 11. 插件操作表 (plugin_operation) - 插件接口操作数据
-- ------------------------------------------------------------
INSERT INTO `plugin_operation` (`id`, `plugin_id`, `operation_id`, `name`, `method`, `path`, `description`, `input_schema`, `output_schema`, `create_time`) VALUES
('op-001-led-on', 'plugin-001-led', 'turnOnLight', '开灯', 'POST', '/light/on', '打开指定位置的LED灯', '{"type": "object", "properties": {"location": {"type": "string", "description": "位置"}}}', '{"type": "object", "properties": {"success": {"type": "boolean"}}}', '2025-11-15 10:00:00'),
('op-002-led-off', 'plugin-001-led', 'turnOffLight', '关灯', 'POST', '/light/off', '关闭指定位置的LED灯', '{"type": "object", "properties": {"location": {"type": "string", "description": "位置"}}}', '{"type": "object", "properties": {"success": {"type": "boolean"}}}', '2025-11-15 10:00:00'),
('op-003-temp-get', 'plugin-002-temp', 'getCurrentTemp', '获取当前温度', 'GET', '/sensor/current_temp', '获取当前房间的实时温度和湿度', '{"type": "object", "properties": {"room": {"type": "string", "description": "房间名称"}}}', '{"type": "object", "properties": {"temperature": {"type": "number"}, "humidity": {"type": "number"}}}', '2025-11-15 11:30:00'),
('op-004-calendar-list', 'plugin-003-calendar', 'listEvents', '查询日程', 'GET', '/events', '查询家庭共享日历事件列表', '{"type": "object", "properties": {"date": {"type": "string", "format": "date"}}}', '{"type": "array", "items": {"type": "object", "properties": {"title": {"type": "string"}, "time": {"type": "string"}}}}', '2025-11-18 14:00:00'),
('op-005-calendar-add', 'plugin-003-calendar', 'addEvent', '添加日程', 'POST', '/events', '添加新的家庭日历事件', '{"type": "object", "properties": {"title": {"type": "string"}, "date": {"type": "string"}, "time": {"type": "string"}}}', '{"type": "object", "properties": {"id": {"type": "string"}, "success": {"type": "boolean"}}}', '2025-11-18 14:00:00'),
('02262dac8f89447e','plugin_be2e083736e0','getSensorData','查询传感器','GET','/plugin/sensor-data','获取各类传感器数据（温度、湿度、雨水、DS18B20等）',NULL,'{"type": "object", "properties": {"msg": {"type": "string", "example": "成功"}, "code": {"type": "integer", "example": 200}, "data": {"type": "object", "properties": {"unit": {"type": "string", "example": "°C"}, "value": {"type": "number", "example": 24.5}}}}}','2025-12-03 14:37:30'),
('9916f4ddca17488f','plugin_be2e083736e0','executePreset','执行预设','POST','/plugin/preset','通过preset_key执行用户自定义预设','{"type": "object", "required": ["device_uuid", "preset_name"], "properties": {"parameters": {"type": "object", "example": {}, "description": "可选参数（通常为空）", "additionalProperties": true}, "device_uuid": {"type": "string", "example": "test", "description": "设备UUID"}, "preset_name": {"type": "string", "example": "led_blink_k8x9y2", "description": "预设标识(preset_key)，如：led_blink_k8x9y2"}}}','{"type": "object", "properties": {"msg": {"type": "string", "example": "成功"}, "code": {"type": "integer", "example": 200}, "data": {"type": "object", "properties": {"result": {"type": "string", "example": "success"}}}}}','2025-12-03 14:37:30'),
('f00c229544d34a09','plugin_be2e083736e0','controlDevice','控制设备','POST','/plugin/control','控制LED、继电器、舵机、PWM等设备','{"type": "object", "required": ["device_uuid", "port_type", "port_id", "action"], "properties": {"value": {"type": "integer", "example": 90, "maximum": 180, "minimum": 0, "description": "设置值：舵机角度(0-180)或PWM占空比(0-100)，仅当action为set时需要"}, "action": {"enum": ["on", "off", "set"], "type": "string", "example": "on", "description": "动作：on(打开)/off(关闭)/set(设置值，用于舵机角度或PWM占空比)"}, "port_id": {"type": "integer", "example": 1, "maximum": 4, "minimum": 1, "description": "端口ID：LED和继电器为1-4，舵机为1-4，PWM为1-2"}, "port_type": {"enum": ["led", "relay", "servo", "pwm"], "type": "string", "example": "led", "description": "设备类型：led(LED灯)、relay(继电器)、servo(舵机)、pwm(PWM输出)"}, "device_uuid": {"type": "string", "example": "test", "description": "设备UUID"}}}','{"type": "object", "properties": {"msg": {"type": "string", "example": "成功"}, "code": {"type": "integer", "example": 200}, "data": {"type": "object", "properties": {"result": {"type": "string", "example": "success"}}}}}','2025-12-03 14:37:30');

-- ============================================================
-- 12. 系统日志表 (system_log) - 增加操作审计和错误日志
-- ------------------------------------------------------------
INSERT INTO `system_log` (`id`, `user_id`, `module`, `action`, `level`, `content`, `create_time`, `request_params`) VALUES
('log-001', 'user-002-home', 'agent', 'create', 'info', '用户 [home_creator] 创建了智能体 [智能家居助理]', '2025-11-22 10:00:00', '{"name": "智能家居助理"}'),
('log-002', 'user-002-home', 'plugin', 'enable', 'info', '用户 [home_creator] 启用了插件 [智能灯光控制]', '2025-11-15 10:05:00', '{"id": "plugin-001-led"}'),
('log-003', 'user-003-tester', 'workflow', 'execute', 'info', '工作流 [智能家居控制与反馈] (run-001) 执行成功', '2025-11-23 11:00:03', '{"run_id": "run-001"}'),
('log-004', 'user-001-admin', 'knowledge_base', 'upload_doc', 'info', '管理员上传了新文档到 [常见问题解答]', '2025-11-13 10:00:00', '{"doc_id": "doc-004"}'),
('log-005', 'user-003-tester', 'workflow', 'execute', 'error', '工作流执行失败: Device not found in location [balcony]', '2025-11-24 12:00:05', '{"run_id": "run-004"}');


-- ============================================================
-- 13. 系统配置表 (system_config) 
-- ------------------------------------------------------------
-- 这些数据已经存在，无需重复插入
/*
INSERT INTO `ai_agent_platform_db`.`system_config` (`id`, `config_key`, `config_value`, `description`) VALUES
('config_001', 'default_model', 'gpt-4', '默认大模型'),
('config_002', 'max_upload_size', '104857600', '最大上传文件大小（字节，默认100MB）'),
('config_003', 'enable_registration', 'true', '是否开放注册');
*/


-- ============================================================
-- 14. LLM提供商表 (llm_providers) - 初始数据
-- ------------------------------------------------------------
INSERT INTO `llm_providers` (`id`, `code`, `name`, `title`, `description`, `apply_url`, `doc_url`, `default_api_base`, `has_free_quota`, `tag_type`, `country`, `sort_order`, `is_active`, `created_at`, `updated_at`) VALUES
('provider-001-qwen', 'qwen', '通义千问', '阿里云通义千问（模型服务平台百炼）', '阿里云自研的大语言模型，支持中文对话、代码生成、Function Calling 等功能。提供 Turbo、Plus、Max 等多个版本，性能强劲，响应快速。', 'https://dashscope.console.aliyun.com/', 'https://help.aliyun.com/zh/model-studio/qwen-api-reference', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 1, 'primary', 'cn', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('provider-002-doubao', 'doubao', '豆包', '火山引擎豆包（字节跳动）', '字节跳动自研的大语言模型，推理能力强，响应快速。支持多种场景应用，包括对话、文本生成、Kimi长文本等。火山引擎方舟平台提供稳定的API服务。', 'https://console.volcengine.com/ark', 'https://www.volcengine.com/docs/82379/1330310', 'https://ark.cn-beijing.volces.com/api/v3', 1, 'success', 'cn', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('provider-003-openai', 'openai', 'OpenAI', 'OpenAI GPT系列', 'OpenAI 提供的 GPT 系列大语言模型，包括 GPT-3.5、GPT-4 等，业界领先的对话和生成能力。', 'https://platform.openai.com/', 'https://platform.openai.com/docs/api-reference', 'https://api.openai.com/v1', 0, 'info', 'us', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ============================================================
-- 15. LLM模型表 (llm_models) - 初始数据
-- 注意：API密钥需要自行配置
-- ------------------------------------------------------------
INSERT INTO `llm_models` (`id`, `name`, `display_name`, `provider`, `model_type`, `api_base`, `api_key`, `max_tokens`, `temperature`, `top_p`, `enable_deep_thinking`, `frequency_penalty`, `presence_penalty`, `config`, `description`, `is_active`, `is_default`, `is_system`, `sort_order`, `created_at`, `updated_at`) VALUES
('model-001-qwen-turbo', 'qwen-turbo', '通义千问-Turbo', 'qwen', 'chat', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'YOUR_API_KEY_HERE', 8192, 0.70, 0.90, 0, 0.00, 0.00, NULL, '阿里云通义千问大语言模型，性能强劲，响应快速，适合对话场景', 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('model-002-qwen-plus', 'qwen-plus', '通义千问-Plus', 'qwen', 'chat', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'YOUR_API_KEY_HERE', 32768, 0.70, 0.90, 0, 0.00, 0.00, NULL, '阿里云通义千问Plus版本，更强大的理解和生成能力', 1, 0, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('model-003-qwen-max', 'qwen-max', '通义千问-Max', 'qwen', 'chat', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'YOUR_API_KEY_HERE', 8192, 0.70, 0.90, 0, 0.00, 0.00, NULL, '阿里云通义千问Max版本，最强理解能力，适合复杂任务', 1, 0, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('model-004-doubao-pro', 'doubao-pro-32k', '豆包-Pro-32k', 'doubao', 'chat', 'https://ark.cn-beijing.volces.com/api/v3', 'YOUR_API_KEY_HERE', 32768, 0.70, 0.90, 0, 0.00, 0.00, NULL, '字节跳动豆包Pro版本，支持32k上下文，适合长文本处理', 1, 0, 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('model-005-gpt35-turbo', 'gpt-3.5-turbo', 'GPT-3.5 Turbo', 'openai', 'chat', 'https://api.openai.com/v1', 'YOUR_API_KEY_HERE', 4096, 0.70, 1.00, 0, 0.00, 0.00, NULL, 'OpenAI GPT-3.5 Turbo 模型，快速高效，性价比高', 1, 0, 1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('model-006-gpt4', 'gpt-4', 'GPT-4', 'openai', 'chat', 'https://api.openai.com/v1', 'YOUR_API_KEY_HERE', 8192, 0.70, 1.00, 0, 0.00, 0.00, NULL, 'OpenAI GPT-4 模型，更强大的推理和理解能力', 1, 0, 1, 21, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('model-007-qwen-embedding','text-embedding-v4','通义千问向量化','qwen','embedding','https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings','YOUR_API_KET_HERE',8192, 0.70, 0.90, 0, 0.00, 0.00, NULL,'通义文本向量化模型',1,1,1,0,CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SET FOREIGN_KEY_CHECKS = 1;


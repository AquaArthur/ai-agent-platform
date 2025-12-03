# 智能体知识库关联 API 文档

## 概述

本文档描述了智能体知识库关联（AgentKnowledgeBase）功能的所有 REST API 接口。该功能用于管理智能体与知识库之间的多对多关联关系。

## 基础信息

- **Base URL**: `/api/v1/agents/{agentId}/knowledge-bases`
- **认证方式**: Bearer Token (待实现)
- **响应格式**: JSON

## API 端点

### 1. 为智能体添加知识库

为指定智能体添加一个知识库关联。

**请求**
```http
POST /api/v1/agents/{agentId}/knowledge-bases
Content-Type: application/json

{
  "knowledgeBaseId": "kb-001-dev",
  "priority": 10
}
```

**路径参数**
- `agentId` (string, required): 智能体ID

**请求体参数**
- `knowledgeBaseId` (string, required): 知识库ID
- `priority` (integer, optional): 优先级，数值越大优先级越高，默认为0

**响应**
```json
{
  "code": 200,
  "message": "知识库添加成功",
  "data": {
    "id": "akb-001",
    "agentId": "agent-001-smarthome",
    "knowledgeBaseId": "kb-001-dev",
    "priority": 10,
    "isEnabled": true,
    "createTime": "2025-12-03T10:00:00",
    "updateTime": "2025-12-03T10:00:00"
  }
}
```

---

### 2. 批量添加知识库

为指定智能体批量添加多个知识库关联。

**请求**
```http
POST /api/v1/agents/{agentId}/knowledge-bases/batch
Content-Type: application/json

{
  "knowledgeBaseIds": ["kb-001-dev", "kb-002-faq"]
}
```

**路径参数**
- `agentId` (string, required): 智能体ID

**请求体参数**
- `knowledgeBaseIds` (array, required): 知识库ID列表

**响应**
```json
{
  "code": 200,
  "message": "批量添加成功",
  "data": [
    {
      "id": "akb-001",
      "agentId": "agent-001-smarthome",
      "knowledgeBaseId": "kb-001-dev",
      "priority": 2,
      "isEnabled": true,
      "createTime": "2025-12-03T10:00:00",
      "updateTime": "2025-12-03T10:00:00"
    },
    {
      "id": "akb-002",
      "agentId": "agent-001-smarthome",
      "knowledgeBaseId": "kb-002-faq",
      "priority": 1,
      "isEnabled": true,
      "createTime": "2025-12-03T10:00:01",
      "updateTime": "2025-12-03T10:00:01"
    }
  ]
}
```

---

### 3. 获取智能体关联的知识库列表

获取指定智能体关联的所有知识库列表，支持筛选只返回启用的知识库。

**请求**
```http
GET /api/v1/agents/{agentId}/knowledge-bases?enabledOnly=false
```

**路径参数**
- `agentId` (string, required): 智能体ID

**查询参数**
- `enabledOnly` (boolean, optional): 是否只返回启用的知识库，默认为false

**响应**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": "kb-001-dev",
      "uuid": "kb-uuid-001",
      "name": "智能设备开发文档",
      "description": "包含LED灯、传感器等设备的API和故障排除文档。",
      "userId": "user-002-home",
      "documentCount": 2,
      "chunkCount": 40,
      "createTime": "2025-11-10T09:10:00",
      "updateTime": "2025-11-10T09:10:00"
    },
    {
      "id": "kb-002-faq",
      "uuid": "kb-uuid-002",
      "name": "常见问题解答",
      "description": "用户对智能家居系统的常见疑问及标准答案。",
      "userId": "user-001-admin",
      "documentCount": 2,
      "chunkCount": 35,
      "createTime": "2025-11-12T14:00:00",
      "updateTime": "2025-11-12T14:00:00"
    }
  ]
}
```

---

### 4. 移除知识库关联

移除智能体与指定知识库的关联。

**请求**
```http
DELETE /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}
```

**路径参数**
- `agentId` (string, required): 智能体ID
- `knowledgeBaseId` (string, required): 知识库ID

**响应**
```json
{
  "code": 200,
  "message": "知识库关联已移除",
  "data": null
}
```

---

### 5. 批量移除知识库关联

批量移除智能体与多个知识库的关联。

**请求**
```http
DELETE /api/v1/agents/{agentId}/knowledge-bases/batch
Content-Type: application/json

{
  "knowledgeBaseIds": ["kb-001-dev", "kb-002-faq"]
}
```

**路径参数**
- `agentId` (string, required): 智能体ID

**请求体参数**
- `knowledgeBaseIds` (array, required): 知识库ID列表

**响应**
```json
{
  "code": 200,
  "message": "批量移除成功",
  "data": null
}
```

---

### 6. 更新知识库优先级

更新智能体关联的知识库的优先级。优先级数值越大，检索时优先级越高。

**请求**
```http
PATCH /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}/priority
Content-Type: application/json

{
  "priority": 20
}
```

**路径参数**
- `agentId` (string, required): 智能体ID
- `knowledgeBaseId` (string, required): 知识库ID

**请求体参数**
- `priority` (integer, required): 新的优先级值

**响应**
```json
{
  "code": 200,
  "message": "优先级更新成功",
  "data": {
    "id": "akb-001",
    "agentId": "agent-001-smarthome",
    "knowledgeBaseId": "kb-001-dev",
    "priority": 20,
    "isEnabled": true,
    "createTime": "2025-12-03T10:00:00",
    "updateTime": "2025-12-03T10:05:00"
  }
}
```

---

### 7. 切换知识库启用状态

启用或禁用智能体关联的知识库，禁用后不会物理删除关联记录。

**请求**
```http
PATCH /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}/toggle
Content-Type: application/json

{
  "enabled": false
}
```

**路径参数**
- `agentId` (string, required): 智能体ID
- `knowledgeBaseId` (string, required): 知识库ID

**请求体参数**
- `enabled` (boolean, required): 是否启用

**响应**
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": {
    "id": "akb-001",
    "agentId": "agent-001-smarthome",
    "knowledgeBaseId": "kb-001-dev",
    "priority": 20,
    "isEnabled": false,
    "createTime": "2025-12-03T10:00:00",
    "updateTime": "2025-12-03T10:06:00"
  }
}
```

---

### 8. 检查知识库关联

检查智能体是否关联了指定知识库。

**请求**
```http
GET /api/v1/agents/{agentId}/knowledge-bases/{knowledgeBaseId}/exists
```

**路径参数**
- `agentId` (string, required): 智能体ID
- `knowledgeBaseId` (string, required): 知识库ID

**响应**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": true
}
```

---

## 错误响应

所有接口在发生错误时返回统一格式的错误响应：

```json
{
  "code": 400,
  "message": "错误描述信息",
  "data": null
}
```

### 常见错误码

- `400` - 请求参数错误
- `404` - 资源不存在
- `500` - 服务器内部错误

### 常见错误场景

1. **智能体不存在**
```json
{
  "code": 400,
  "message": "智能体不存在: agent-xxx",
  "data": null
}
```

2. **知识库不存在**
```json
{
  "code": 400,
  "message": "知识库不存在: kb-xxx",
  "data": null
}
```

3. **关联已存在**
```json
{
  "code": 400,
  "message": "该智能体已关联此知识库",
  "data": null
}
```

4. **关联不存在**
```json
{
  "code": 400,
  "message": "未找到该关联记录",
  "data": null
}
```

---

## 数据库表结构

### agent_knowledge_base 表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | VARCHAR(64) | 关联记录唯一标识 |
| agent_id | VARCHAR(64) | 智能体ID |
| knowledge_base_id | VARCHAR(64) | 知识库ID |
| priority | INT | 优先级（数值越大优先级越高） |
| is_enabled | BOOLEAN | 是否启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 使用示例

### 示例1：创建智能体并关联知识库

```bash
# 1. 为智能体添加知识库
curl -X POST "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases" \
  -H "Content-Type: application/json" \
  -d '{
    "knowledgeBaseId": "kb-001-dev",
    "priority": 10
  }'

# 2. 添加第二个知识库
curl -X POST "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases" \
  -H "Content-Type: application/json" \
  -d '{
    "knowledgeBaseId": "kb-002-faq",
    "priority": 5
  }'

# 3. 查看所有关联的知识库
curl -X GET "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases"
```

### 示例2：批量管理知识库关联

```bash
# 批量添加
curl -X POST "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "knowledgeBaseIds": ["kb-001-dev", "kb-002-faq", "kb-003-docs"]
  }'

# 批量移除
curl -X DELETE "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "knowledgeBaseIds": ["kb-002-faq", "kb-003-docs"]
  }'
```

### 示例3：管理优先级和启用状态

```bash
# 更新优先级
curl -X PATCH "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases/kb-001-dev/priority" \
  -H "Content-Type: application/json" \
  -d '{
    "priority": 100
  }'

# 禁用知识库
curl -X PATCH "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases/kb-001-dev/toggle" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'

# 重新启用知识库
curl -X PATCH "http://localhost:8080/api/v1/agents/agent-001/knowledge-bases/kb-001-dev/toggle" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true
  }'
```

---

## 注意事项

1. **级联删除**: 当智能体或知识库被删除时，相关的关联记录会自动删除
2. **唯一性约束**: 同一智能体不能重复关联同一知识库
3. **优先级排序**: 获取知识库列表时，按优先级降序排列
4. **软删除支持**: 通过`isEnabled`字段支持临时禁用，无需物理删除
5. **事务保证**: 所有写操作都在事务中执行，确保数据一致性

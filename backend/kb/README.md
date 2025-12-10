# 知识库系统

## 统一响应格式规范

所有 API 均返回以下结构：

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1634567890123
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "timestamp": 1634567890123
}
```

## 向量化相关 API（RAG）

### 1.1 文档向量化（含分块）

POST /api/v1/rag/embed

将文档自动分块、调用 embedding 模型向量化，并写入数据库。

#### 请求参数（JSON）
```json
{
  "knowledge_base_id": "kb001",
  "document_id": "doc001",
  "title": "示例文档.md",
  "content": "文档内容……"
}
```

#### 请求参数说明
- knowledge_base_id: 知识库id
- document_id: 文档id
- title: 文档标题（文件名）
- content: 文档内容

#### 返回示例
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "knowledge_base_id": "kb001",
    "document_id": "doc001",
    "chunks": 12,
    "vector_dim": 1536
  },
  "timestamp": 1634567890123
}
```

#### 返回参数说明
- knowledge_base_id: 知识库id
- document_id: 文档id
- chunks: 分片总数
- vector_dim: 向量维度

### 1.2 RAG 检索接口

POST /api/v1/rag/query

对 query 进行向量化，对数据库中 chunks 执行相似度检索。

#### 请求参数
```json
{
  "knowledge_base_id": 1,
  "query": "如何安装系统？",
  "top_k": 5,
  "similarity_threshold": 0.2,
  "model_name": "bge-m3" 
}
```
#### 请求参数说明
- knowledge_base_id: 知识库id
- query: 查询的文本
- top_k: 最大返回前K项
- similarity_threshold: 相似度阈值，非必需参数，默认0.5
- model_name: 使用的向量化模型，非必须参数

#### 返回示例
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "result_num":2,
    "results": [
      {
        "vector_id": 101,
        "chunk_index": 0,
        "score": 0.86,
        "content": "系统安装步骤如下……"
      },
      {
        "vector_id": 102,
        "chunk_index": 1,
        "score": 0.82,
        "content": "准备启动盘……"
      }
    ]
  },
  "timestamp": 1634567890123
}
```
#### 返回参数说明
- result_num: 数量
- results: 查询结果列表
    - vector_id: 向量唯一标识
    - chunk_index: 分片序号（从0开始）
    - score: 相似度得分
    - content: 文本内容

# 数据库相关表
## 建表脚本
```sql
create table vector
(
    id          bigint auto_increment comment '向量唯一标识'
        primary key,
    document_id varchar(64)                         not null comment '所属文档id',
    kb_id       varchar(64)                         not null comment '所属知识库id',
    chunk_index int                                 not null comment '分片序号，从0开始',
    chunk_text  text                                not null comment 'chunk文本内容',
    embedding   json                                not null comment '向量，存储为JSON 数组',
    vector_dim  int                                 null comment '向量维度，便于一致性检查',
    create_time timestamp default CURRENT_TIMESTAMP null,
    constraint fk_vector_document
        foreign key (document_id) references document (id)
            on delete cascade,
    constraint fk_vector_kb
        foreign key (kb_id) references knowledge_base (id)
            on delete cascade
)
    comment '文档向量表';

create index idx_kb_document
    on vector (document_id, kb_id);


```


# 使用说明
```bash
# 启动服务 (--port 9000表示监听端口9000，也可修改为其他端口)
uvicorn main:app --reload --host 0.0.0.0 --port 9000
# 测试脚本
C:\repositories\ai-agent-platform>python .\backend\kb\test.py
```



# TODO

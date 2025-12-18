import json
import numpy as np
from kb.celery_app import celery
from kb.database import get_db
from kb.chunker import smart_chunk
from kb.embed_client import EmbedClient, EMBEDDING_MODEL_NAME
from kb.utils.response import success, error


# -----------------------
# 批量写入数据库
# -----------------------
def _save_chunks_to_db(db, cursor, kb_id, document_id, chunks, embeddings):
    sql = """
        INSERT INTO vector
        (kb_id, document_id, chunk_index, chunk_text, embedding, vector_dim)
        VALUES (%s, %s, %s, %s, %s, %s)
    """
    batch_rows = []
    for i, (chunk_text, emb_info) in enumerate(zip(chunks, embeddings)):
        embedding = json.dumps(emb_info["embedding"])
        vector_dim = emb_info["vector_dim"]
        batch_rows.append(
            (kb_id, document_id, i, chunk_text, embedding, vector_dim))

    batch_size = 100
    for i in range(0, len(batch_rows), batch_size):
        cursor.executemany(sql, batch_rows[i:i + batch_size])
    db.commit()


# -----------------------
# 更新文档信息
# -----------------------
def _save_document_info_to_db(db, cursor, document_id, chunk_count, status,
                              process_status):
    sql = """
        UPDATE document
        SET
            chunk_count = %s,
            status = %s,
            process_status = %s,
            update_time = NOW(),
            updated_at = NOW()
        WHERE
            id = %s;
    """
    cursor.execute(sql, (chunk_count, status, process_status, document_id))
    db.commit()


@celery.task(bind=True, name="vectorize_document_task")
def vectorize_document_task(self,
                            knowledge_base_id: str,
                            document_id: str,
                            title: str,
                            content: str,
                            model_name: str = EMBEDDING_MODEL_NAME):
    """
    Celery 任务：对单个文档进行分块、向量化并写入 DB。
    返回值会被序列化后存入 Celery backend。
    """
    try:
        db, cursor = get_db()
        embed_client = EmbedClient()

        # 1. 分块
        is_markdown = title.endswith('.md')
        chunks = smart_chunk(content, is_markdown)

        # 2. 对所有 chunk 做 embedding（这里仍是同步调用模型）
        embeddings_info = []
        for chunk in chunks:
            result = embed_client.embed_text(text=chunk, model_name=model_name)
            embeddings_info.append(result)

        # 3. 写入数据库
        _save_chunks_to_db(db, cursor, knowledge_base_id, document_id, chunks,
                           embeddings_info)

        _save_document_info_to_db(db, cursor, document_id, len(chunks),
                                  "processed", 2)

        return {
            "knowledge_base_id": knowledge_base_id,
            "document_id": document_id,
            "chunks": len(chunks),
            "vector_dim": embeddings_info[0]["vector_dim"],
            "message": "文档向量化成功"
        }
    except Exception as exc:
        # 记录异常并让 Celery 标记任务为失败
        self.retry(exc=exc, countdown=10, max_retries=3)
        # 若不想重试，可直接 raise
        raise

import json
import datetime
import numpy as np
from kb.database import get_db
from kb.chunker import smart_chunk
from kb.embed_client import EmbedClient, EMBEDDING_MODEL_NAME
from kb.utils.response import success, error
from kb.tasks import vectorize_document_task


# -----------------------
# 批量写入数据库
# -----------------------
def save_chunks_to_db(db, cursor, kb_id, document_id, chunks, embeddings):

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
# 文档向量化（含 chunk）
# -----------------------
# def vectorize_document(knowledge_base_id: str,
#                        document_id: str,
#                        title: str,
#                        content: str,
#                        model_name: str = EMBEDDING_MODEL_NAME):

#     db, cursor = get_db()
#     embed_client = EmbedClient()

#     # 1. 分块
#     # is_markdown = ("#" in content)
#     is_markdown = (title.endswith('.md'))
#     chunks = smart_chunk(content, is_markdown)

#     # 2. 对所有 chunk 做 embedding
#     embeddings_info = []
#     for chunk in chunks:
#         result = embed_client.embed_text(text=chunk, model_name=model_name)
#         # embedding = result["embedding"]
#         # vector_dim = result["vector_dim"]
#         embeddings_info.append(result)

#     # 3. 写入数据库
#     save_chunks_to_db(db, cursor, knowledge_base_id, document_id, chunks,
#                       embeddings_info)

#     return success(data={
#         "knowledge_base_id": knowledge_base_id,
#         "document_id": document_id,
#         "chunks": len(chunks),
#         "vector_dim": embeddings_info[0]["vector_dim"]
#     },
#                    message="文档向量化成功")


# -----------------------
# 文档向量化（异步）
# -----------------------
def vectorize_document(knowledge_base_id,
                       document_id,
                       title,
                       content,
                       model_name=EMBEDDING_MODEL_NAME):
    # 异步调用
    task = vectorize_document_task.delay(knowledge_base_id, document_id, title,
                                         content, model_name)
    return success(data={"task_id": task.id}, message="文档向量化任务已提交")


# -----------------------
# RAG 检索（Chunk 模式）
# -----------------------
def rag_search(knowledge_base_id: int,
               query: str,
               top_k: int,
               similarity_threshold: float,
               model_name: str = EMBEDDING_MODEL_NAME):

    db, cursor = get_db()
    embed_client = EmbedClient()

    # 1. 向量化
    res = embed_client.embed_text(text=query)
    query_vec = res["embedding"]
    vec_dim = res["vector_dim"]
    query_vec = np.array(query_vec, dtype=float)

    # 2. 从 MySQL 查询当前 kb_id 下的所有 vec
    cursor.execute(
        """
        SELECT id, chunk_index, chunk_text, embedding
        FROM vector
        WHERE kb_id = %s
        ORDER BY document_id, chunk_index
        """, (knowledge_base_id, ))
    rows = cursor.fetchall()

    # print("➡ 数据库返回向量条数 =", len(rows))

    if not rows:
        return {"results": []}

    # 3. 批量解析 JSON 向量（性能优化：尽量减少 Python 循环）
    chunk_ids = []
    chunk_indexes = []
    chunk_texts = []
    embeddings = []

    for row in rows:
        # vec_id, chunk_index, chunk_text, emb_json = row
        vec_id = row["id"]
        chunk_index = row["chunk_index"]
        chunk_text = row["chunk_text"]
        emb_json = row["embedding"]

        # 1. 打印 row 的 meta 信息
        # print(f"ROW: id={vec_id}, chunk_index={chunk_index}")

        # 2. 打印 embedding 内容前 100 字符
        # print("emb_json head:", str(emb_json)[:100])

        try:
            emb = json.loads(emb_json) if isinstance(emb_json,
                                                     str) else emb_json
        except Exception as e:
            print("⚠ JSON 解析失败:", e)
            continue
        print("stored_dim =", len(emb), " query_dim =", vec_dim)

        emb = np.array(emb, dtype=float)

        # 维度校验（可选）
        if emb.shape[0] != vec_dim:
            continue

        chunk_ids.append(vec_id)
        chunk_indexes.append(chunk_index)
        chunk_texts.append(chunk_text)
        embeddings.append(emb)

    if len(embeddings) == 0:
        return {"results": [], "message": "知识库中没有可用向量（可能未向量化或维度不一致）"}
    # 转为矩阵：N x D
    embedding_matrix = np.vstack(embeddings)  # shape = (N, D)
    # print("shape:", embedding_matrix.shape)

    # 4. 批量计算余弦相似度： (query · vectors) / (|q| |v|)
    query_norm = np.linalg.norm(query_vec)
    vec_norms = np.linalg.norm(embedding_matrix, axis=1)

    dot_products = embedding_matrix @ query_vec

    similarity_scores = dot_products / (vec_norms * query_norm)

    # 5. 过滤并排序 Top K
    scored_items = []
    for i, score in enumerate(similarity_scores):
        print(f"score={score},similarity_threshold={similarity_threshold}")
        if score >= similarity_threshold:
            scored_items.append(
                (chunk_ids[i], float(score), chunk_texts[i], chunk_indexes[i]))

    # 排序
    scored_items.sort(key=lambda x: x[1], reverse=True)
    scored_items = scored_items[:top_k]

    # 6. 返回结构
    return success(data={
        "result_num":
        len(scored_items),
        "results": [{
            "vector_id": cid,
            "chunk_index": cidx,
            "score": score,
            "content": text
        } for cid, score, text, cidx in scored_items]
    },
                   message=f"向量查询成功,共{len(scored_items)}条结果")

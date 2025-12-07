from fastapi import FastAPI
from models import EmbedDocInput, RagQueryInput
from rag_service import vectorize_document, rag_search

app = FastAPI(title="RAG Module API", version="1.0")


@app.post("/api/v1/rag/embed")
def embed_document(input_data: EmbedDocInput):
    return vectorize_document(knowledge_base_id=input_data.knowledge_base_id,
                              document_id=input_data.document_id,
                              title=input_data.title,
                              content=input_data.content,
                              model_name=input_data.model_name)


@app.post("/api/v1/rag/query")
def rag_query(input_data: RagQueryInput):
    return rag_search(knowledge_base_id=input_data.knowledge_base_id,
                      query=input_data.query,
                      top_k=input_data.top_k,
                      similarity_threshold=input_data.similarity_threshold,
                      model_name=input_data.model_name)

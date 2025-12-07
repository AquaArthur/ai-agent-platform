from pydantic import BaseModel, Field
from typing import Optional


class EmbedDocInput(BaseModel):
    knowledge_base_id: str
    document_id: str
    title: str = None
    content: str
    model_name: str = "text-embedding-v4"  # 默认模型


class RagQueryInput(BaseModel):
    knowledge_base_id: str
    query: str
    top_k: int = Field(default=5, description="返回的向量检索数量")
    similarity_threshold: Optional[float] = Field(default=0.5,
                                                  ge=0.0,
                                                  le=1.0,
                                                  description="向量相似度阈值（越高越严格）")

    model_name: str = "text-embedding-v4"  # 默认模型

import requests
from kb.database import get_api_key

EMBEDDING_MODEL_NAME = "text-embedding-v4"


class EmbedClient:

    def __init__(self, model_name: str = EMBEDDING_MODEL_NAME):
        model_info = get_api_key(model_name)
        if not model_info:
            raise ValueError(f"模型 {model_name} 未在 llm_model 表中配置 API Key")
        self.api_key = model_info["api_key"]
        self.api_base = model_info["api_base"]

    def embed_text(self, text: str, model_name: str = EMBEDDING_MODEL_NAME):

        api_base = self.api_base

        # 自动补全为 OpenAI embedding endpoint
        if api_base.endswith("/v1"):
            api_base = api_base + "/embeddings"
        elif api_base.endswith("/v1/"):
            api_base = api_base + "embeddings"

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

        payload = {"model": model_name, "input": text}

        response = requests.post(api_base,
                                 json=payload,
                                 headers=headers,
                                 timeout=15)

        if response.status_code != 200:
            raise RuntimeError(
                f"千问向量化失败: {response.status_code}, {response.text}")

        embedding = response.json()["data"][0]["embedding"]
        vector_dim = len(embedding)

        return {"embedding": embedding, "vector_dim": vector_dim}


if __name__ == "__main__":
    # client = EmbedClient()
    # print(client.embed_text("你好"))  # 从文本向量化
    pass

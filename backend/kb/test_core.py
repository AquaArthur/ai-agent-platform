import requests
import json
import time

# Java 后端对外提供的 URL
JAVA_QUERY_API = "http://localhost:8080/api/v1/knowledge-bases/kb-uuid-003/query"  # 若使用 docker，请改成 Java 服务实际地址


def test_rag_query():
    payload = {
        "knowledge_base_id": "kb-003-tt",
        "query": "智能家居",
        "top_k": 3,
        "similarity_threshold": 0.2
    }

    print(">>> 发送请求到 Java 后端:", JAVA_QUERY_API)
    print("请求内容：", json.dumps(payload, ensure_ascii=False, indent=2))
    print("------------------------------------------------------------")

    try:
        start = time.time()
        resp = requests.post(JAVA_QUERY_API, json=payload, timeout=10)
        duration = time.time() - start

        print(f"响应状态码: {resp.status_code}")
        print(f"耗时: {duration:.2f}s")

        try:
            data = resp.json()
            print("\n响应 JSON：")
            print(json.dumps(data, ensure_ascii=False, indent=2))
        except Exception:
            print("⚠ 无法解析 JSON，原始响应内容：")
            print(resp.text)

    except Exception as e:
        print("❌ 请求失败:", e)


if __name__ == "__main__":
    test_rag_query()

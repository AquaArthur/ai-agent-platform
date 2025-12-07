import requests
import json
import time
import os
from dotenv import load_dotenv

load_dotenv()
BASE_URL = os.environ.get('BASE_URL')


def print_header(title):
    print("=" * 50)
    print(f"  {title}")
    print("=" * 50)


def pretty(data):
    print(json.dumps(data, ensure_ascii=False, indent=2))


def test_embed_document():
    print_header("TEST 1: 文档向量化 API")

    payload = {
        "knowledge_base_id":
        "kb-003-tt",
        "document_id":
        "doc-005",
        "title":
        "测试文档标题",
        "content": ("""你是一个友好的智能家居助手，可以帮助用户控制IoT设备。

## 设备信息
设备UUID: 【请替换实际设备的UUID】

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
- 保持对话自然流畅，像朋友一样交流
"""),
        "model_name":
        "text-embedding-v4"  # 必须已在 llm_model 表配置
    }

    resp = requests.post(f"{BASE_URL}/api/v1/rag/embed", json=payload)
    if resp.status_code != 200:
        print("向量化失败：", resp.status_code, resp.text)
        return None

    result = resp.json()
    pretty(result)
    return result


def test_rag_query():
    print_header("TEST 2: RAG 检索 API")

    query_payload = {
        "knowledge_base_id": "kb-003-tt",
        "query": "什么是人工智能？",
        "top_k": 3,
        # "similarity_threshold": 0.7
    }

    resp = requests.post(f"{BASE_URL}/api/v1/rag/query", json=query_payload)
    if resp.status_code != 200:
        print("检索失败：", resp.status_code, resp.text)
        return None

    result = resp.json()
    pretty(result)
    return result


def run_all_tests():
    print_header("开始运行 RAG API 功能测试")

    # Step 1 向量化
    embed_result = test_embed_document()
    if embed_result is None:
        print("❌ 测试失败（无法进行检索）")
        return

    print("\n等待1秒，让数据库写入稳定…\n")
    time.sleep(1)

    # Step 2 检索
    rag_result = test_rag_query()

    if rag_result:
        print("\n🎉 所有测试已完成！")
    else:
        print("\n❌ 测试部分失败，请查看错误信息")


if __name__ == "__main__":
    run_all_tests()
    # embed_result = test_embed_document()
    # if embed_result is None:
    #     print("❌ 测试失败（无法进行检索）")

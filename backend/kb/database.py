'''
backend.kb.db.database 的 Docstring
提供数据库服务
'''

import pymysql


def get_db():
    db = pymysql.connect(host="localhost",
                         user="root",
                         password="123456",
                         database="ai_agent_platform_db",
                         charset='utf8mb4',
                         cursorclass=pymysql.cursors.DictCursor)
    cursor = db.cursor()
    return db, cursor


def get_api_key(model_name: str):
    """从 llm_models 表获取对应 embedding 模型的 API Key"""
    db, cursor = get_db()
    cursor.execute(
        """
        SELECT api_key, api_base FROM llm_models WHERE name = %s
    """, (model_name, ))
    row = cursor.fetchone()
    cursor.close()
    db.close()
    return row  # { "api_key": "...", "base_url": "..." }


if __name__ == "__main__":
    print(get_api_key("text-embedding-v4"))
    pass

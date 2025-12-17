from celery import Celery

# 根据实际部署修改 broker / backend 地址
celery = Celery(
    "ai_agent_platform",
    broker="redis://localhost:6379/0",  # 例：Redis 作为消息中间件
    include=['kb.tasks'])

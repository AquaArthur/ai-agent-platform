FROM python:3.10-slim-bullseye

WORKDIR /app

# 系统依赖
RUN apt-get update && apt-get install -y \
    build-essential default-libmysqlclient-dev \
    && rm -rf /var/lib/apt/lists/*

# 复制 kb 目录所有文件
COPY . /app/kb

COPY ./requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt
RUN pip install --no-cache-dir cryptography



# 暴露服务端口
EXPOSE 8000

# 启动 FastAPI
CMD ["uvicorn", "kb.main:app", "--host", "0.0.0.0", "--port", "8000"]

import time


def _current_timestamp():
    """返回毫秒级时间戳"""
    return int(time.time() * 1000)


def success(data=None, message="操作成功", code=200):
    """统一成功响应格式"""
    return {
        "code": code,
        "message": message,
        "data": data if data is not None else {},
        "timestamp": _current_timestamp()
    }


def error(message="请求参数错误", code=400, data=None):
    """统一错误响应格式"""
    return {
        "code": code,
        "message": message,
        "data": data,  # 错误响应 data 为 null
        "timestamp": _current_timestamp()
    }

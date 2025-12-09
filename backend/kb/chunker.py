import re


def determine_chunk_params(text: str):
    size = len(text.encode("utf-8"))

    if size < 5 * 1024:  # 小文档
        return 500, 50
    elif size < 50 * 1024:  # 中文档
        return 800, 80
    else:  # 大文档
        return 1000, 100


# Markdown 优化：按标题优先切分
def markdown_split(text: str):
    parts = re.split(r"(?m)^#.+", text)
    parts = [p.strip() for p in parts if p.strip()]
    return parts


# 通用文本分割
def chunk_text(text: str, chunk_size: int, overlap: int):
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunks.append(text[start:end])
        start += (chunk_size - overlap)
    return chunks


def smart_chunk(text: str, is_markdown: bool):
    chunk_size, overlap = determine_chunk_params(text)

    if is_markdown:
        sections = markdown_split(text)
        chunks = []
        for sec in sections:
            if len(sec) <= chunk_size:
                chunks.append(sec)
            else:
                chunks.extend(chunk_text(sec, chunk_size, overlap))
        return chunks

    return chunk_text(text, chunk_size, overlap)

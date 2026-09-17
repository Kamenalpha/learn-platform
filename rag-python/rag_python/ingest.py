"""集合重建:从 MySQL doc_chunk 全量分块 -> SiliconFlow bge-small-zh 嵌入 -> 写入 Chroma。

元数据与后端 IngestService 完全一致(docId/docTitle/courseId/courseName/page/vectorId,
全部字符串无 null);chunk_id 不入向量元数据,与生产语义一致(chunkId 由关键词路补全)。
向量 ID 复用 doc_chunk.vector_id(格式 "{resourceId}-{index}"),保证两路融合键对齐。

用法:python -m rag_python.ingest [--collection learn_platform_knowledge]
注意:同名集合会先删除再重建(幂等);换嵌入模型后必须重建(维度不匹配)。
"""

from __future__ import annotations

import argparse
import logging
import sys
from typing import Iterator

import pymysql

from . import config
from .chroma_source import get_client
from .mysql_kw import _connect

log = logging.getLogger(__name__)


def iter_chunks(course_ids: list[int] | None = None) -> Iterator[dict]:
    conn = _connect()
    try:
        sql = """
            SELECT c.resource_id AS resourceId, c.chunk_id AS chunkId, c.content AS content,
                   c.page_num AS pageNum, c.vector_id AS vectorId, r.title AS docTitle,
                   r.course_id AS courseId, cu.course_name AS courseName
            FROM doc_chunk c
            JOIN resource r ON r.resource_id = c.resource_id
            JOIN course cu ON cu.course_id = r.course_id
        """
        params: tuple = ()
        if course_ids:
            ph = ",".join(["%s"] * len(course_ids))
            sql += f" WHERE r.course_id IN ({ph})"
            params = tuple(course_ids)
        sql += " ORDER BY r.resource_id, c.chunk_id"
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(sql, params)
            for row in cur:
                yield row
    finally:
        conn.close()


def embed_batch(texts: list[str]) -> list[list[float]]:
    """调用 SiliconFlow /v1/embeddings(与后端同一嵌入模型)。"""
    from openai import OpenAI

    client = OpenAI(api_key=config.EMBED_API_KEY, base_url=f"{config.EMBED_BASE_URL}/v1")
    resp = client.embeddings.create(model=config.EMBED_MODEL, input=texts)
    # OpenAI SDK 返回按输入顺序排列
    return [d.embedding for d in resp.data]


def rebuild(collection_name: str = config.CHROMA_COLLECTION) -> int:
    """重建集合,返回写入的分块数。"""
    config.require_api_keys()
    client = get_client()
    # 幂等:先删后建(避免历史数据与新 schema 混在一起)
    for c in client.list_collections():
        if c.name == collection_name:
            log.info("删除旧集合 %s", collection_name)
            client.delete_collection(collection_name)
    coll = client.create_collection(collection_name, metadata={"hnsw:space": "cosine"})

    rows = list(iter_chunks())
    texts = [r["content"] for r in rows]
    total = 0
    for start in range(0, len(texts), config.EMBED_BATCH_SIZE):
        batch_rows = rows[start:start + config.EMBED_BATCH_SIZE]
        batch_texts = [r["content"] for r in batch_rows]
        vectors = embed_batch(batch_texts)
        ids = [r["vectorId"] for r in batch_rows]
        metadatas = [
            {
                "docId": str(r["resourceId"]),
                "docTitle": str(r["docTitle"] or ""),
                "courseId": str(r["courseId"]),
                "courseName": str(r["courseName"] or ""),
                "page": "" if r["pageNum"] is None else str(r["pageNum"]),
                "vectorId": str(r["vectorId"]),
            }
            for r in batch_rows
        ]
        coll.add(ids=ids, embeddings=vectors, documents=batch_texts, metadatas=metadatas)
        total += len(ids)
        log.info("已写入 %d/%d", total, len(texts))
    return total


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    parser = argparse.ArgumentParser(description="重建 Chroma 知识集合")
    parser.add_argument("--collection", default=config.CHROMA_COLLECTION)
    args = parser.parse_args()
    try:
        n = rebuild(args.collection)
    except Exception as e:
        print(f"[error] 重建失败: {e}", file=sys.stderr)
        return 1
    print(f"[ok] 集合 {args.collection} 重建完成,共 {n} 个分块")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

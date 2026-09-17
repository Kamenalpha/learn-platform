"""MySQL ngram 关键词路:与后端 DocChunkMapper.keywordSearch 完全同构的 SQL。

依赖 doc_chunk.content 的 ngram 全文索引(见仓库 sql/upgrade_fulltext_index.sql)。
索引缺失时 MATCH 报错,由调用方捕获降级为仅向量检索(与后端双降级兜底一致)。
"""

from __future__ import annotations

import logging
from typing import Optional

import pymysql

from . import config
from .models import Hit

log = logging.getLogger(__name__)

_SQL = """
SELECT c.resource_id AS resourceId, c.chunk_id AS chunkId, c.content AS content,
       c.page_num AS pageNum, c.vector_id AS vectorId, r.title AS docTitle,
       MATCH(c.content) AGAINST(%s IN NATURAL LANGUAGE MODE) AS kwScore
FROM doc_chunk c JOIN resource r ON r.resource_id = c.resource_id
WHERE MATCH(c.content) AGAINST(%s IN NATURAL LANGUAGE MODE) > 0
  AND r.course_id IN ({placeholders})
ORDER BY kwScore DESC
LIMIT %s
"""


def keyword_search(
    query: str,
    course_ids: list[int],
    limit: int = config.LEG_LIMIT,
    conn: Optional[pymysql.connections.Connection] = None,
) -> list[Hit]:
    """执行 ngram 关键词召回。course_ids 为空时返回空列表(与后端 fail-closed 一致)。"""
    if not course_ids:
        return []
    own_conn = conn is None
    try:
        conn = conn or _connect()
        placeholders = ",".join(["%s"] * len(course_ids))
        with conn.cursor() as cur:
            cur.execute(_SQL.format(placeholders=placeholders),
                        (query, query, *course_ids, limit))
            rows = cur.fetchall()
    finally:
        if own_conn and conn is not None:
            conn.close()

    hits: list[Hit] = []
    for r in rows:
        page = r["pageNum"]
        hits.append(Hit(
            doc_id=_to_int(r["resourceId"]),
            chunk_id=_to_int(r["chunkId"]),
            vector_id=r["vectorId"],
            doc_title=r["docTitle"] or "",
            page=_to_int(page),
            text=r["content"] or "",
            keyword_score=float(r["kwScore"]) if r["kwScore"] is not None else None,
        ))
    return hits


def list_course_ids() -> list[int]:
    """有分块资料的课程 id(评估脚本的作用域:与"知识在库"一致)。"""
    conn = _connect()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT DISTINCT r.course_id
                FROM resource r JOIN doc_chunk c ON c.resource_id = r.resource_id
                ORDER BY r.course_id
            """)
            return [int(row["course_id"]) for row in cur.fetchall()]
    finally:
        conn.close()


def fetch_corpus(course_ids: list[int], limit: int = 100000) -> list[Hit]:
    """导出课程范围内全部分块,供 BM25 建立语料(两框架 bm25 模式共用)。"""
    if not course_ids:
        return []
    conn = _connect()
    try:
        placeholders = ",".join(["%s"] * len(course_ids))
        sql = f"""
            SELECT c.resource_id AS resourceId, c.chunk_id AS chunkId, c.content AS content,
                   c.page_num AS pageNum, c.vector_id AS vectorId, r.title AS docTitle
            FROM doc_chunk c JOIN resource r ON r.resource_id = c.resource_id
            WHERE r.course_id IN ({placeholders})
            ORDER BY c.resource_id, c.chunk_id
            LIMIT %s
        """
        with conn.cursor() as cur:
            cur.execute(sql, (*course_ids, limit))
            rows = cur.fetchall()
    finally:
        conn.close()
    return [
        Hit(
            doc_id=_to_int(r["resourceId"]),
            chunk_id=_to_int(r["chunkId"]),
            vector_id=r["vectorId"],
            doc_title=r["docTitle"] or "",
            page=_to_int(r["pageNum"]),
            text=r["content"] or "",
        )
        for r in rows
    ]


def _connect() -> pymysql.connections.Connection:
    return pymysql.connect(
        host=config.DB_HOST,
        port=config.DB_PORT,
        user=config.DB_USER,
        password=config.DB_PASSWORD,
        database=config.DB_NAME,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=5,
        read_timeout=30,
    )


def _to_int(v) -> int | None:
    try:
        return None if v is None else int(v)
    except (TypeError, ValueError):
        return None

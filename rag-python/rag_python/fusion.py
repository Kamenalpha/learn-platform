"""RRF 倒数排名融合:与后端 RetrievalService 的 rankInto/RRF_K 完全同款。

只按名次算分(1/(k+rank)),避免向量分(0~1)与关键词分(0~30+)量纲不统一的问题。
融合键与后端一致:优先 docId:vectorId,vectorId 缺失时用 docId:chunkId 兜底。
"""

from __future__ import annotations

from .config import RRF_K
from .models import Hit


def key_of(doc_id: int | None, vector_id: str | None, chunk_id: int | None) -> str:
    """融合唯一键:同文档未嵌入块(无 vectorId)用 chunkId 兜底,避免互相覆盖。"""
    if doc_id is None:
        return f"doc-none:{vector_id or chunk_id}"
    return f"{doc_id}:{vector_id}" if vector_id else f"{doc_id}:chunk{chunk_id}"


def rrf_fuse(
    vector_hits: list[Hit],
    keyword_hits: list[Hit],
    rrf_k: int = RRF_K,
) -> list[Hit]:
    """按名次融合两路召回,返回按 RRF 分降序的列表(不修改入参,返回新对象)。"""
    merged: dict[str, Hit] = {}
    for rank, h in enumerate(vector_hits, start=1):
        key = key_of(h.doc_id, h.vector_id, h.chunk_id)
        cur = _clone(h)
        cur.rrf_score = 1.0 / (rrf_k + rank)
        merged[key] = cur
    for rank, h in enumerate(keyword_hits, start=1):
        key = key_of(h.doc_id, h.vector_id, h.chunk_id)
        if key in merged:
            cur = merged[key]
            cur.rrf_score += 1.0 / (rrf_k + rank)
            cur.keyword_score = h.keyword_score
            # 关键词路携带 chunkId/page,若向量路先建条目则补齐
            if cur.chunk_id is None:
                cur.chunk_id = h.chunk_id
            if cur.page is None:
                cur.page = h.page
            if not cur.doc_title:
                cur.doc_title = h.doc_title
            if not cur.text:
                cur.text = h.text
        else:
            cur = _clone(h)
            cur.rrf_score = 1.0 / (rrf_k + rank)
            merged[key] = cur
    return sorted(merged.values(), key=lambda x: x.rrf_score, reverse=True)


def filter_by_vector_threshold(hits: list[Hit], threshold: float) -> list[Hit]:
    """只保留向量分达到阈值的命中(与后端 SearchRequest.similarityThreshold 语义一致)。"""
    return [h for h in hits if h.vector_score is None or h.vector_score >= threshold]


def _clone(h: Hit) -> Hit:
    return Hit(
        doc_id=h.doc_id,
        chunk_id=h.chunk_id,
        vector_id=h.vector_id,
        doc_title=h.doc_title,
        page=h.page,
        text=h.text,
        vector_score=h.vector_score,
        keyword_score=h.keyword_score,
    )

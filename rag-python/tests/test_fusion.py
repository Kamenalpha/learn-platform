"""RRF 融合与阈值过滤单测:验证与后端 RetrievalService 同款数学(1/(k+rank))。"""

import pytest

from rag_python.fusion import filter_by_vector_threshold, key_of, rrf_fuse
from rag_python.models import Hit


def _hit(doc_id, vector_id=None, chunk_id=None, vec=None, kw=None, page=None, title="doc"):
    return Hit(doc_id=doc_id, vector_id=vector_id, chunk_id=chunk_id, page=page,
               doc_title=title, text="t", vector_score=vec, keyword_score=kw)


def test_key_of_vector_id_preferred():
    assert key_of(7, "7-0", 100) == "7:7-0"
    assert key_of(7, None, 100) == "7:chunk100"


def test_rrf_single_leg():
    hits = rrf_fuse([_hit(1, "1-0", vec=0.8), _hit(2, "2-0", vec=0.7)], [])
    assert [h.doc_id for h in hits] == [1, 2]
    assert hits[0].rrf_score == pytest.approx(1.0 / 61)


def test_rrf_double_leg_sums():
    vector = [_hit(1, "1-0", vec=0.8), _hit(2, "2-0", vec=0.7)]
    keyword = [_hit(2, "2-0", kw=3.0), _hit(3, "3-0", kw=2.0)]
    hits = rrf_fuse(vector, keyword)
    by_id = {h.doc_id: h for h in hits}
    # 各腿独立按名次计分:doc2 在向量路第 2、关键词路第 1
    assert by_id[2].rrf_score == pytest.approx(1.0 / 62 + 1.0 / 61)
    assert by_id[1].rrf_score == pytest.approx(1.0 / 61)
    assert by_id[3].rrf_score == pytest.approx(1.0 / 62)
    # 双路命中应排最前
    assert hits[0].doc_id == 2


def test_rrf_keyword_merges_chunk_metadata():
    """关键词路后到,应补齐向量路缺失的 chunkId/page。"""
    vector = [_hit(1, "1-0", vec=0.8, page=None)]
    keyword = [_hit(1, "1-0", chunk_id=99, kw=2.0, page=3)]
    hits = rrf_fuse(vector, keyword)
    assert hits[0].chunk_id == 99
    assert hits[0].page == 3
    assert hits[0].keyword_score == 2.0
    assert hits[0].vector_score == 0.8


def test_rrf_chunk_fallback_dedup():
    """vectorId 缺失时按 chunkId 兜底去重,避免同文档未嵌入块互相覆盖。"""
    vector = [_hit(1, None, chunk_id=5, vec=0.8)]
    keyword = [_hit(1, None, chunk_id=5, kw=2.0)]
    hits = rrf_fuse(vector, keyword)
    assert len(hits) == 1
    assert hits[0].rrf_score == pytest.approx(2.0 / 61)


def test_rrf_does_not_mutate_inputs():
    v = [_hit(1, "1-0", vec=0.8)]
    k = [_hit(2, "2-0", kw=1.0)]
    rrf_fuse(v, k)
    assert v[0].rrf_score == 0.0
    assert k[0].rrf_score == 0.0


def test_threshold_filter():
    hits = [_hit(1, vec=0.5), _hit(2, vec=0.3), _hit(3, vec=None)]
    kept = filter_by_vector_threshold(hits, 0.4)
    assert [h.doc_id for h in kept] == [1, 3]  # 无向量分的不拦(与后端语义一致)

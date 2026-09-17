"""MySQL 关键词路集成测试:依赖本机 MySQL 与 ngram 全文索引,不可用时跳过。"""

import pytest

from rag_python import mysql_kw


def _db_ok() -> bool:
    try:
        return bool(mysql_kw.list_course_ids())
    except Exception:
        return False


pytestmark = pytest.mark.skipif(not _db_ok(), reason="本机 MySQL 不可用")


def test_course_ids_nonempty():
    ids = mysql_kw.list_course_ids()
    assert ids and all(isinstance(i, int) for i in ids)


def test_keyword_search_shape():
    hits = mysql_kw.keyword_search("什么是进程", [2], limit=5)
    assert len(hits) <= 5
    for h in hits:
        assert h.doc_id == 7 or h.doc_title  # OSTEP-01-Process 或其它 OSTEP 块
        assert h.keyword_score is not None and h.keyword_score > 0
        assert h.text
    # 按分数降序
    scores = [h.keyword_score for h in hits]
    assert scores == sorted(scores, reverse=True)


def test_keyword_search_empty_course_scope():
    assert mysql_kw.keyword_search("进程", []) == []


def test_fetch_corpus():
    corpus = mysql_kw.fetch_corpus([2])
    assert len(corpus) == 221  # OSTEP 3 章分块数(2026-09-11 实测基线)
    assert all(h.vector_id for h in corpus)

"""重排客户端单测:协议与后端一致(/v1/rerank,失败降级)。"""

from unittest import mock

import pytest

import rag_python.rerank as rerank_mod
from rag_python.rerank import RerankError, rerank


def _ok_response(payload):
    r = mock.Mock()
    r.status_code = 200
    r.json.return_value = payload
    return r


def test_rerank_parses_and_sorts(monkeypatch):
    payload = {"results": [
        {"index": 2, "relevance_score": 0.9},
        {"index": 0, "relevance_score": 0.5},
        {"index": 1, "relevance_score": 0.7},
    ]}
    monkeypatch.setattr(rerank_mod.requests, "post", lambda *a, **k: _ok_response(payload))
    out = rerank("q", ["a", "b", "c"], top_n=3)
    assert out == [(2, 0.9), (1, 0.7), (0, 0.5)]


def test_rerank_payload_shape(monkeypatch):
    captured = {}

    def fake_post(url, json=None, headers=None, timeout=None):
        captured.update(url=url, json=json, headers=headers, timeout=timeout)
        return _ok_response({"results": [{"index": 0, "relevance_score": 1.0}]})

    monkeypatch.setattr(rerank_mod.requests, "post", fake_post)
    rerank("q", ["a"], top_n=1)
    assert captured["url"].endswith("/v1/rerank")
    assert captured["json"]["model"] == rerank_mod.config.RERANK_MODEL
    assert captured["json"]["query"] == "q"
    assert captured["json"]["documents"] == ["a"]
    assert captured["json"]["top_n"] == 1
    assert captured["headers"]["Authorization"].startswith("Bearer ")


def test_rerank_http_error_raises(monkeypatch):
    r = mock.Mock()
    r.status_code = 500
    r.text = "boom"
    monkeypatch.setattr(rerank_mod.requests, "post", lambda *a, **k: r)
    with pytest.raises(RerankError):
        rerank("q", ["a"])


def test_rerank_network_error_raises(monkeypatch):
    def boom(*a, **k):
        raise TimeoutError("conn refused")

    monkeypatch.setattr(rerank_mod.requests, "post", boom)
    with pytest.raises(RerankError):
        rerank("q", ["a"])


def test_rerank_malformed_payload_raises(monkeypatch):
    monkeypatch.setattr(rerank_mod.requests, "post",
                        lambda *a, **k: _ok_response(
                            {"results": [{"index": "x", "relevance_score": 1.0}]}))
    with pytest.raises(RerankError):
        rerank("q", ["a"])

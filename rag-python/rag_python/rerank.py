"""SiliconFlow BGE-Reranker 重排客户端:与后端 RetrievalService.rerank 同一 /v1/rerank 协议。

失败抛 RerankError,由调用方降级为融合顺序(不阻断问答)。
"""

from __future__ import annotations

import logging

import requests

from . import config

log = logging.getLogger(__name__)


class RerankError(Exception):
    """重排服务调用失败(网络/非 200/解析失败),调用方应降级。"""


def rerank(
    query: str,
    documents: list[str],
    top_n: int | None = None,
) -> list[tuple[int, float]]:
    """返回 [(原下标, relevance_score)] 按分数降序。top_n=None 表示全部。"""
    if not documents:
        return []
    payload = {
        "model": config.RERANK_MODEL,
        "query": query,
        "documents": documents,
        "top_n": min(top_n, len(documents)) if top_n else len(documents),
    }
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {config.RERANK_API_KEY}",
    }
    try:
        resp = requests.post(
            f"{config.RERANK_BASE_URL}/v1/rerank",
            json=payload,
            headers=headers,
            timeout=60,
        )
    except (requests.RequestException, OSError) as e:
        log.warning("重排调用失败(网络),降级为融合顺序: %s", e)
        raise RerankError(str(e)) from e
    if resp.status_code != 200:
        log.warning("重排服务返回 %s,降级为融合顺序: %s", resp.status_code, resp.text[:200])
        raise RerankError(f"http {resp.status_code}")

    try:
        results = resp.json().get("results", [])
        out = [(int(r["index"]), float(r["relevance_score"]))
               for r in results if "index" in r and "relevance_score" in r]
        # 服务端按分数降序返回,校验一下(防御性)
        out.sort(key=lambda x: x[1], reverse=True)
        return out
    except (ValueError, KeyError, TypeError) as e:
        log.warning("重排响应解析失败,降级为融合顺序: %s", e)
        raise RerankError(str(e)) from e

"""Chroma 客户端选择:优先连服务端(生产一致),连不上回退本地持久化目录。

数据契约与后端 IngestService 一致:集合 learn_platform_knowledge,余弦距离,
元数据 docId/docTitle/courseId/courseName/page/vectorId(均为字符串,无 null)。
"""

from __future__ import annotations

import logging
from functools import lru_cache

import chromadb
from chromadb import Collection

from . import config

log = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def get_client() -> chromadb.ClientAPI:
    """获取客户端;优先 HTTP 服务(默认 localhost:8000),回退 PersistentClient。"""
    http_client = chromadb.HttpClient(host=config.CHROMA_HOST, port=config.CHROMA_PORT)
    try:
        http_client.heartbeat()
        log.info("Chroma 数据源: http://%s:%s", config.CHROMA_HOST, config.CHROMA_PORT)
        return http_client
    except Exception:
        log.warning("Chroma HTTP 服务不可用,回退本地持久化目录 %s", config.CHROMA_PERSIST_DIR)
        return chromadb.PersistentClient(path=config.CHROMA_PERSIST_DIR)


def get_collection() -> Collection:
    client = get_client()
    try:
        return client.get_collection(config.CHROMA_COLLECTION)
    except Exception as e:
        raise RuntimeError(
            f"Chroma 集合 {config.CHROMA_COLLECTION} 不可用: {e}\n"
            f"请先启动 Chroma(scripts/start-infra.ps1 或 docker compose up -d chroma),"
            f"或执行 python -m rag_python.ingest 重建集合"
        ) from e

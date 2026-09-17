"""配置加载:读取仓库根 .env(已 gitignore,含真实 Key),未配置时回退与后端 application.yml 一致的默认值。

Key 约定与 Java 后端一致:LLM_* / EMBED_* / RERANK_* / DB_* / CHROMA_*。
唯一差异:RERANK_API_KEY 为空时自动复用 EMBED_API_KEY(README 推荐做法,后端未做自动回退)。
"""

import os
from pathlib import Path

from dotenv import load_dotenv

# rag-python/rag_python/config.py -> parents[1]=rag-python, parents[2]=仓库根
MODULE_DIR = Path(__file__).resolve().parents[1]
REPO_ROOT = Path(__file__).resolve().parents[2]

load_dotenv(REPO_ROOT / ".env")           # 仓库根 .env(compose/后端共用)
load_dotenv(MODULE_DIR / ".env")          # 模块级覆盖(可选)


def _non_placeholder(key: str) -> str:
    """本地占位符 Key(如 local-embeddings)视为未配置,不用于云 API。"""
    if not key:
        return ""
    if key.startswith(("sk-fill-your-key", "sk-your-key", "local-")):
        return ""
    return key

# ---- Chroma ----
CHROMA_HOST = os.getenv("CHROMA_HOST", "localhost")
CHROMA_PORT = int(os.getenv("CHROMA_PORT", "8000"))
CHROMA_COLLECTION = os.getenv("CHROMA_COLLECTION", "learn_platform_knowledge")
# 持久化回退路径:后端配置 CHROMA_PERSIST_DIR 前,项目本地 Chroma 数据目录
CHROMA_PERSIST_DIR = os.getenv("CHROMA_PERSIST_DIR", str(REPO_ROOT / "data" / "chroma"))

# ---- 嵌入(SiliconFlow OpenAI 兼容) ----
EMBED_BASE_URL = os.getenv("EMBED_BASE_URL", "https://api.siliconflow.cn").rstrip("/")
EMBED_API_KEY = os.getenv("EMBED_API_KEY", "")
EMBED_MODEL = os.getenv("EMBED_MODEL", "BAAI/bge-small-zh-v1.5")
EMBED_BATCH_SIZE = int(os.getenv("EMBED_BATCH_SIZE", "16"))

# ---- 重排(SiliconFlow 兼容 /v1/rerank) ----
RERANK_BASE_URL = os.getenv("RERANK_BASE_URL", "https://api.siliconflow.cn").rstrip("/")
RERANK_MODEL = os.getenv("RERANK_MODEL", "BAAI/bge-reranker-v2-m3")
RERANK_API_KEY = os.getenv("RERANK_API_KEY") or (_non_placeholder(EMBED_API_KEY) or "")

# ---- MySQL(与后端同库 learn_platform) ----
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "learn_platform")

# ---- 检索默认参数(与后端 RagProperties / application.yml 一致) ----
TOP_K = int(os.getenv("RAG_TOP_K", "5"))
THRESHOLD = float(os.getenv("RAG_THRESHOLD", "0.4"))
RRF_K = 60          # RRF 融合常数(与后端 RetrievalService.RRF_K 相同)
LEG_LIMIT = 20      # 单路粗召回上限(与后端 LEG_LIMIT 相同)


def require_api_keys() -> None:
    """嵌入 Key 缺失时给出可读报错(重排/嵌入都依赖它)。"""
    if not EMBED_API_KEY or EMBED_API_KEY.startswith("sk-fill-your-key"):
        raise RuntimeError(
            "未配置 EMBED_API_KEY:请把仓库根 .env 中 EMBED_API_KEY 填为有效的 SiliconFlow Key"
            "(或复制 .env.example 为 .env 后填入)"
        )

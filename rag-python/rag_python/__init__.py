"""rag-python:用 LangChain / LlamaIndex 重写的检索模块参考实现。

与后端 Spring AI 版 RetrievalService 保持同一数据契约与流水线:
向量召回(Chroma) + 关键词召回(MySQL ngram / BM25) -> 权限作用域过滤 -> RRF 融合 -> 可选重排 -> topK。
用户级权限校验仍在 Java 后端收口(fail-closed),本模块只负责"已授权课程范围内"的检索。
"""

__version__ = "0.1.0"

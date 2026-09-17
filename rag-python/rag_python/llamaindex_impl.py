"""LlamaIndex 实现:Chroma VectorStoreIndex + MySQL ngram/BM25 关键词路 + RRF 融合 + 重排后处理器。

框架原生组件用法:
- 向量路:llama-index-vector-stores-chroma ChromaVectorStore + VectorStoreIndex.as_retriever
  (MetadataFilters 对 courseId 做 IN 过滤)
- 关键词路 bm25 模式:llama-index-retrievers-bm25 BM25Retriever(jieba 中文分词)+
  QueryFusionRetriever(mode=RECIPROCAL_RANK, num_queries=1 关掉 LLM 改写)
- 重排:BaseNodePostprocessor 子类包 SiliconFlow /v1/rerank
阈值语义与后端一致:只作用于向量路;bm25 模式为框架端到端融合,阈值在融合后按向量分复算过滤。
"""

from __future__ import annotations

import logging

from llama_index.core import QueryBundle, Settings, VectorStoreIndex
from llama_index.core.llms import MockLLM
from llama_index.core.postprocessor.types import BaseNodePostprocessor
from llama_index.core.retrievers import QueryFusionRetriever
from llama_index.core.retrievers.fusion_retriever import FUSION_MODES
from llama_index.core.schema import NodeWithScore, TextNode
from llama_index.core.vector_stores.types import (FilterOperator, MetadataFilter,
                                                  MetadataFilters)
from llama_index.retrievers.bm25 import BM25Retriever
from llama_index.vector_stores.chroma import ChromaVectorStore

from . import config, mysql_kw
from .chroma_source import get_collection
from .embeddings import make_llamaindex_embedding
from .fusion import filter_by_vector_threshold, key_of, rrf_fuse
from .models import Hit, RetrievalResult
from .rerank import RerankError, rerank

log = logging.getLogger(__name__)


def _jieba_tokens(text: str) -> list[str]:
    """BM25 中文分词:默认英文词边界分词对中文整句退化为一个 token,必须换 jieba。"""
    import jieba

    return jieba.lcut(text)


class SiliconFlowRerankPostprocessor(BaseNodePostprocessor):
    """把 BGE-Reranker 包装成 LlamaIndex 节点后处理器(QueryEngine 可插拔组件)。"""

    top_n: int = 0  # 0 = 处理全部

    def _postprocess_nodes(self, nodes: list[NodeWithScore], query_bundle: QueryBundle):
        if not nodes:
            return nodes
        texts = [n.node.get_content() for n in nodes]
        top_n = self.top_n or len(nodes)
        pairs = rerank(query_bundle.query_str, texts, top_n=top_n)
        out = []
        for idx, score in pairs:
            n = nodes[idx]
            n.node.metadata["preRerankScore"] = n.score  # 调试面板保留重排前分数
            n.score = score
            out.append(n)
        return out


class LlamaIndexRetriever:
    """与后端 RetrievalService 同流水线的 LlamaIndex 参考实现。"""

    name = "llamaindex"

    def __init__(
        self,
        kw_engine: str = "mysql",
        rerank_enabled: bool = True,
        top_k: int = config.TOP_K,
        threshold: float = config.THRESHOLD,
    ):
        assert kw_engine in ("mysql", "bm25"), "kw_engine 仅支持 mysql / bm25"
        self.kw_engine = kw_engine
        # 重排 Key 为占位符/空时安静关闭(与后端 rerankActive 语义一致)
        self.rerank_enabled = rerank_enabled and bool(config.RERANK_API_KEY)
        self.top_k = top_k
        self.threshold = threshold
        self._corpus_nodes: list[TextNode] | None = None
        self._course_ids: list[int] = []

        Settings.embed_model = make_llamaindex_embedding()
        # QueryFusionRetriever 构造时会解析 Settings.llm;num_queries=1 不会真正调用,
        # 用内置 MockLLM 占位,避免未配置 LLM Key 时初始化失败
        Settings.llm = MockLLM()
        collection = get_collection()
        self.index = VectorStoreIndex.from_vector_store(
            ChromaVectorStore(chroma_collection=collection)
        )
        self.postprocessor = SiliconFlowRerankPostprocessor(top_n=self.top_k)

    # ---------- 检索入口 ----------

    def retrieve(self, query: str, course_ids: list[int]) -> RetrievalResult:
        if not course_ids:
            return RetrievalResult(query=query)
        self._course_ids = course_ids
        qb = QueryBundle(query_str=query)
        course_filter = MetadataFilters(filters=[
            MetadataFilter(key="courseId", value=[str(c) for c in course_ids],
                           operator=FilterOperator.IN)
        ])

        # 1. 向量路(语义;阈值作用于本路)
        vector_hits = self._vector_leg(qb, course_filter)
        vector_ok = filter_by_vector_threshold(vector_hits, self.threshold)

        # 2. 关键词路
        if self.kw_engine == "mysql":
            keyword_hits = self._keyword_mysql(query, course_ids)
            hits = rrf_fuse(vector_ok, keyword_hits)
        else:
            keyword_hits = self._keyword_bm25(query, course_ids)
            hits = self._fusion_retrieve(qb, course_filter, vector_hits, vector_ok, keyword_hits)

        # 3. 重排(失败降级为融合顺序)
        if self.rerank_enabled and hits:
            try:
                nodes = self.postprocessor._postprocess_nodes(
                    [_to_node(h) for h in hits], qb
                )
                hits = [_node_to_hit(n) for n in nodes]
            except RerankError:
                log.warning("重排失败,降级为融合顺序")

        # 4. 截断 topK 并编号
        final = hits[: self.top_k]
        for i, h in enumerate(final, start=1):
            h.rank = i
        return RetrievalResult(query=query, hits=final, stages=self._stages(query, vector_hits, keyword_hits))

    # ---------- 各阶段 ----------

    def _vector_leg(self, qb: QueryBundle, course_filter: MetadataFilters) -> list[Hit]:
        try:
            retriever = self.index.as_retriever(
                similarity_top_k=config.LEG_LIMIT, filters=course_filter
            )
            nodes = retriever.retrieve(qb)
        except Exception as e:
            log.error("限定课程向量检索失败,已阻止全库回退: %s", e)
            raise
        return [_node_to_hit(n, vector=True) for n in nodes]

    def _keyword_mysql(self, query: str, course_ids: list[int]) -> list[Hit]:
        try:
            return mysql_kw.keyword_search(query, course_ids)
        except Exception as e:
            log.warning("关键词检索不可用(未建全文索引?),降级为仅向量检索: %s", e)
            return []

    def _keyword_bm25(self, query: str, course_ids: list[int]) -> list[Hit]:
        bm25 = self._bm25_retriever(course_ids)
        return [_node_to_hit(n, vector=False) for n in bm25.retrieve(QueryBundle(query_str=query))]

    def _fusion_retrieve(self, qb: QueryBundle, course_filter: MetadataFilters,
                         vector_hits: list[Hit], vector_ok: list[Hit],
                         keyword_hits: list[Hit]) -> list[Hit]:
        """框架原生融合:QueryFusionRetriever RRF(num_queries=1 不做 LLM 改写)。"""
        if not keyword_hits:
            return vector_ok
        vector_retriever = self.index.as_retriever(
            similarity_top_k=config.LEG_LIMIT, filters=course_filter
        )
        fusion = QueryFusionRetriever(
            [vector_retriever, self._bm25_retriever(self._course_ids)],
            mode=FUSION_MODES.RECIPROCAL_RANK,
            num_queries=1,
            similarity_top_k=config.LEG_LIMIT,
            use_async=False,
        )
        nodes = fusion.retrieve(qb)
        # 阈值:来自向量路的块,向量分低于阈值即剔除(关键词路不受阈值约束)
        vector_scores = {key_of(h.doc_id, h.vector_id, None): h.vector_score for h in vector_hits}
        out: list[Hit] = []
        for n in nodes:
            h = _node_to_hit(n, vector=False)
            h.rrf_score = n.score if n.score is not None else h.rrf_score
            vscore = vector_scores.get(key_of(h.doc_id, h.vector_id, None))
            if vscore is not None and vscore < self.threshold:
                continue
            h.vector_score = vscore
            out.append(h)
        return out

    # ---------- 工具 ----------

    def _bm25_retriever(self, course_ids: list[int]) -> BM25Retriever:
        if self._corpus_nodes is None:
            self._corpus_nodes = [_to_node(h) for h in mysql_kw.fetch_corpus(course_ids)]
        return BM25Retriever.from_defaults(
            nodes=self._corpus_nodes,
            similarity_top_k=config.LEG_LIMIT,
            tokenizer=_jieba_tokens,
        )

    def _stages(self, query: str, vector_hits: list[Hit], keyword_hits: list[Hit]) -> dict:
        return {
            "vector": [h.to_dict() for h in vector_hits],
            "keyword": [h.to_dict() for h in keyword_hits],
        }


def _to_node(h: Hit) -> TextNode:
    metadata = {
        "docId": h.doc_id, "chunkId": h.chunk_id, "vectorId": h.vector_id,
        "docTitle": h.doc_title, "page": h.page,
        "vector_score": h.vector_score, "keyword_score": h.keyword_score,
        "rrf_score": h.rrf_score,
    }
    return TextNode(text=h.text, metadata={k: v for k, v in metadata.items() if v is not None})


def _node_to_hit(n: NodeWithScore, vector: bool = False) -> Hit:
    m = n.node.metadata
    h = Hit(
        doc_id=_to_int(m.get("docId")),
        chunk_id=_to_int(m.get("chunkId")),
        vector_id=m.get("vectorId"),
        doc_title=m.get("docTitle") or "",
        page=_to_int(m.get("page")),
        text=n.node.get_content(),
    )
    score = n.score
    if vector:
        h.vector_score = score
    else:
        h.rrf_score = float(score or 0)
    if m.get("rerank_score") is not None:
        h.rerank_score = _to_float(m.get("rerank_score"))
    return h


def _to_int(v) -> int | None:
    try:
        return None if v in (None, "") else int(v)
    except (TypeError, ValueError):
        return None


def _to_float(v) -> float | None:
    try:
        return None if v in (None, "") else float(v)
    except (TypeError, ValueError):
        return None

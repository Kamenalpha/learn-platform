"""LangChain 实现:Chroma 向量路 + MySQL ngram/BM25 关键词路 + RRF 融合 + 重排压缩器。

框架原生组件用法:
- 向量路:langchain_chroma.Chroma(OpenAIEmbeddings 指向 SiliconFlow)+ courseId 元数据过滤
- 关键词路 bm25 模式:langchain_community BM25Retriever(jieba 中文分词)+
  langchain_classic EnsembleRetriever(weights=[1,1], c=60, 与后端 RRF_K 同值)
- 重排:BaseDocumentCompressor 子类包 SiliconFlow /v1/rerank(ContextualCompression 范式)
阈值语义与后端一致:只作用于向量路;bm25 模式为框架端到端融合,阈值在融合后按向量分复算过滤。
"""

from __future__ import annotations

import logging

from langchain_classic.retrievers import EnsembleRetriever
from langchain_classic.retrievers.contextual_compression import BaseDocumentCompressor
from langchain_community.retrievers import BM25Retriever
from langchain_core.documents import Document

from . import config, mysql_kw
from .chroma_source import get_client
from .embeddings import OpenAICompatEmbeddings
from .fusion import filter_by_vector_threshold, key_of, rrf_fuse
from .models import Hit, RetrievalResult
from .rerank import RerankError, rerank

log = logging.getLogger(__name__)


def _jieba_tokens(text: str) -> list[str]:
    """BM25 中文分词:默认按空白切分对中文整句退化为一个 token,必须换 jieba。"""
    import jieba

    return jieba.lcut(text)


class SiliconFlowCompressor(BaseDocumentCompressor):
    """把 BGE-Reranker 包装成 LangChain 文档压缩器(ContextualCompression 范式)。"""

    top_n: int = 0  # 0 = 压缩全部

    def compress_documents(self, documents: list[Document], query: str, callbacks=None) -> list[Document]:
        if not documents:
            return documents
        top_n = self.top_n or len(documents)
        pairs = rerank(query, [d.page_content for d in documents], top_n=top_n)
        out = []
        for idx, score in pairs:
            d = documents[idx]
            out.append(Document(
                page_content=d.page_content,
                metadata={**d.metadata, "rerank_score": score},
            ))
        return out


class LangChainRetriever:
    """与后端 RetrievalService 同流水线的 LangChain 参考实现。"""

    name = "langchain"

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
        self._corpus_cache: list[Hit] | None = None

        embeddings = OpenAICompatEmbeddings()
        from .chroma_source import get_client

        self.vector_store = __import__("langchain_chroma").Chroma(
            client=get_client(),
            collection_name=config.CHROMA_COLLECTION,
            embedding_function=embeddings,
        )
        self.vector_retriever = self.vector_store.as_retriever(
            search_kwargs={"k": config.LEG_LIMIT}
        )
        self.compressor = SiliconFlowCompressor(top_n=self.top_k)

    # ---------- 检索入口 ----------

    def retrieve(self, query: str, course_ids: list[int]) -> RetrievalResult:
        if not course_ids:
            return RetrievalResult(query=query)
        course_filter = {"courseId": {"$in": [str(c) for c in course_ids]}}

        # 1. 向量路(语义;阈值作用于本路)
        vector_hits = self._vector_leg(query, course_filter)
        vector_ok = filter_by_vector_threshold(vector_hits, self.threshold)

        # 2. 关键词路
        if self.kw_engine == "mysql":
            keyword_hits = self._keyword_mysql(query, course_ids)
            hits = rrf_fuse(vector_ok, keyword_hits)
        else:
            keyword_hits = self._keyword_bm25(query, course_ids)
            hits = self._ensemble(query, course_filter, vector_hits, vector_ok, keyword_hits)

        # 3. 重排(失败降级为融合顺序)
        if self.rerank_enabled and hits:
            try:
                compressed = self.compressor.compress_documents(
                    self._hits_to_docs(hits), query
                )
                hits = self._docs_to_hits(compressed)
            except RerankError:
                log.warning("重排失败,降级为融合顺序")

        # 4. 截断 topK 并编号
        final = hits[: self.top_k]
        for i, h in enumerate(final, start=1):
            h.rank = i
        return RetrievalResult(query=query, hits=final, stages=self._stages(query, vector_hits, keyword_hits))

    # ---------- 各阶段 ----------

    def _vector_leg(self, query: str, course_filter: dict) -> list[Hit]:
        try:
            pairs = self.vector_store.similarity_search_with_relevance_scores(
                query, k=config.LEG_LIMIT, filter=course_filter
            )
        except Exception as e:
            log.error("限定课程向量检索失败,已阻止全库回退: %s", e)
            raise
        return [_doc_to_hit(d, score) for d, score in pairs]

    def _keyword_mysql(self, query: str, course_ids: list[int]) -> list[Hit]:
        try:
            return mysql_kw.keyword_search(query, course_ids)
        except Exception as e:
            log.warning("关键词检索不可用(未建全文索引?),降级为仅向量检索: %s", e)
            return []

    def _keyword_bm25(self, query: str, course_ids: list[int]) -> list[Hit]:
        corpus = self._corpus(course_ids)
        bm25 = BM25Retriever.from_documents(
            self._hits_to_docs(corpus), k=config.LEG_LIMIT,
            preprocess_func=_jieba_tokens,
        )
        return [_doc_to_hit(d) for d in bm25.invoke(query)]

    def _ensemble(self, query: str, course_filter: dict, vector_hits: list[Hit],
                  vector_ok: list[Hit], keyword_hits: list[Hit]) -> list[Hit]:
        """框架原生融合:EnsembleRetriever RRF(weights 相等 + c=60,与手动 RRF 同款数学)。"""
        if not keyword_hits:
            return vector_ok
        bm25 = BM25Retriever.from_documents(
            self._hits_to_docs(self._corpus(course_filter)), k=config.LEG_LIMIT,
            preprocess_func=_jieba_tokens,
        )
        ensemble = EnsembleRetriever(
            retrievers=[self.vector_retriever, bm25],
            weights=[1.0, 1.0],
            c=config.RRF_K,
            id_key="vectorId",
        )
        docs = ensemble.invoke(query)
        # 阈值:来自向量路的块,向量分低于阈值即剔除(关键词路不受阈值约束)
        vector_scores = {key_of(h.doc_id, h.vector_id, None): h.vector_score for h in vector_hits}
        keep: list[Hit] = []
        for d in docs:
            h = _doc_to_hit(d)
            vscore = vector_scores.get(key_of(h.doc_id, h.vector_id, None))
            if vscore is not None and vscore < self.threshold:
                continue
            h.vector_score = vscore
            keep.append(h)
        return keep

    # ---------- 工具 ----------

    def _corpus(self, course_ids_or_filter) -> list[Hit]:
        """课程范围内全部分块语料(懒加载;bm25 与 ensemble 共用,与关键词路同源)。"""
        if self._corpus_cache is None:
            cids = course_ids_or_filter if isinstance(course_ids_or_filter, list) else None
            if cids is None:
                # 从 courseId 过滤表达式反解课程 id($in 列表)
                cids = course_ids_or_filter["courseId"]["$in"]
                cids = [int(c) for c in cids]
            self._corpus_cache = mysql_kw.fetch_corpus(cids)
        return self._corpus_cache

    def _stages(self, query: str, vector_hits: list[Hit], keyword_hits: list[Hit]) -> dict:
        return {
            "vector": [h.to_dict() for h in vector_hits],
            "keyword": [h.to_dict() for h in keyword_hits],
        }

    def _hits_to_docs(self, hits: list[Hit]) -> list[Document]:
        return [
            Document(
                page_content=h.text,
                metadata={
                    "docId": h.doc_id, "chunkId": h.chunk_id, "vectorId": h.vector_id,
                    "docTitle": h.doc_title, "page": h.page,
                    "vector_score": h.vector_score, "keyword_score": h.keyword_score,
                    "rrf_score": h.rrf_score,
                },
            )
            for h in hits
        ]

    def _docs_to_hits(self, docs: list[Document]) -> list[Hit]:
        out = []
        for d in docs:
            m = d.metadata
            out.append(Hit(
                doc_id=_to_int(m.get("docId")),
                chunk_id=_to_int(m.get("chunkId")),
                vector_id=m.get("vectorId"),
                doc_title=m.get("docTitle") or "",
                page=_to_int(m.get("page")),
                text=d.page_content,
                vector_score=_to_float(m.get("vector_score")),
                keyword_score=_to_float(m.get("keyword_score")),
                rrf_score=float(m.get("rrf_score") or 0),
                rerank_score=_to_float(m.get("rerank_score")),
            ))
        return out


def _doc_to_hit(d: Document, score: float | None = None) -> Hit:
    m = d.metadata
    return Hit(
        doc_id=_to_int(m.get("docId")),
        chunk_id=_to_int(m.get("chunkId")),
        vector_id=m.get("vectorId"),
        doc_title=m.get("docTitle") or "",
        page=_to_int(m.get("page")),
        text=d.page_content,
        vector_score=score if score is not None else _to_float(m.get("vector_score")),
        keyword_score=_to_float(m.get("keyword_score")),
    )


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

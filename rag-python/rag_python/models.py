"""检索结果数据契约:与后端 RetrievalService 返回的引用字段一一对应。"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class Hit:
    """一个检索命中的分块。各阶段分数可空(该路未命中/未启用时)。"""

    rank: int = 0
    doc_id: int | None = None
    chunk_id: int | None = None
    vector_id: str | None = None
    doc_title: str = ""
    page: int | None = None
    text: str = ""
    vector_score: float | None = None
    keyword_score: float | None = None
    rrf_score: float = 0.0
    rerank_score: float | None = None

    def to_dict(self, snippet: int = 160) -> dict:
        d = {
            "rank": self.rank,
            "docId": self.doc_id,
            "chunkId": self.chunk_id,
            "vectorId": self.vector_id,
            "docTitle": self.doc_title,
            "page": self.page,
            "vectorScore": _round3(self.vector_score),
            "keywordScore": _round3(self.keyword_score),
            "rrfScore": round(self.rrf_score, 3),
            "rerankScore": _round3(self.rerank_score),
        }
        if snippet and self.text:
            d["snippet"] = self.text[:snippet] + ("..." if len(self.text) > snippet else "")
        return d


@dataclass
class RetrievalResult:
    """一次检索的完整结果:hits 为最终 topK,stages 记录每一阶段中间结果(调试面板)。"""

    query: str
    hits: list[Hit] = field(default_factory=list)
    stages: dict[str, list[dict]] = field(default_factory=dict)


def _round3(v: float | None) -> float | None:
    return None if v is None else round(v, 3)

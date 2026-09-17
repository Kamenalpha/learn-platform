"""CLI:单查询分阶段调试(LangChain / LlamaIndex 可对比)。

示例:
  python -m rag_python.cli "什么是进程？进程和程序有什么区别？" --course 2
  python -m rag_python.cli "How does limited direct execution work?" --framework both --kw bm25 --rerank
"""

from __future__ import annotations

import argparse
import json
import logging
import sys
import time

from . import config, mysql_kw
from .langchain_impl import LangChainRetriever
from .llamaindex_impl import LlamaIndexRetriever

log = logging.getLogger(__name__)


def build_retriever(framework: str, args) -> object:
    cls = LangChainRetriever if framework == "langchain" else LlamaIndexRetriever
    return cls(
        kw_engine=args.kw,
        rerank_enabled=args.rerank,
        top_k=args.top_k,
        threshold=args.threshold,
    )


def _get(item, key, default=None):
    """同时支持 Hit 对象(snake_case 字段)与 to_dict() 出的字典(camelCase 键)。"""
    if isinstance(item, dict):
        return item.get(key, default)
    # 属性名蛇形化:vectorScore -> vector_score
    snake = "".join("_" + c.lower() if c.isupper() else c for c in key)
    return getattr(item, snake, default)


def print_hits(hits, stage: str = "") -> None:
    header = f"=== {stage} ===" if stage else "=== 最终 topK ==="
    print(header)
    print(f"{'rank':<5}{'rrf':<8}{'vec':<7}{'kw':<8}{'rerank':<8}{'docTitle':<38}{'page'}")
    for h in hits:
        vec = _get(h, "vectorScore")
        kw = _get(h, "keywordScore")
        rr = _get(h, "rerankScore")
        print(f"{_get(h, 'rank', 0):<5}{_get(h, 'rrfScore', 0.0):<8.3f}"
              f"{'-' if vec is None else round(vec, 3):<7}"
              f"{'-' if kw is None else round(kw, 3):<8}"
              f"{'-' if rr is None else round(rr, 3):<8}"
              f"{str(_get(h, 'docTitle', ''))[:36]:<38}{_get(h, 'page')}")
    print()


def run_one(framework: str, query: str, args) -> dict:
    retriever = build_retriever(framework, args)
    t0 = time.perf_counter()
    result = retriever.retrieve(query, args.course)
    elapsed = (time.perf_counter() - t0) * 1000
    print(f"\n########## {framework} (kw={args.kw}, rerank={'on' if args.rerank else 'off'}) "
          f"{elapsed:.0f}ms ##########")
    print_hits(result.stages.get("vector", []), "向量路(阈值过滤前)")
    print_hits(result.stages.get("keyword", []), "关键词路")
    print_hits(result.hits, "最终 topK")
    return {
        "framework": framework,
        "kw": args.kw,
        "rerank": args.rerank,
        "elapsed_ms": round(elapsed),
        "hits": [h.to_dict() for h in result.hits],
        "stages": {k: [x["docTitle"] + f"#{x['page']}" for x in v] for k, v in result.stages.items()},
    }


def main() -> int:
    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(message)s")
    parser = argparse.ArgumentParser(description="RAG 检索参考实现(分阶段调试)")
    parser.add_argument("query", help="检索问题")
    parser.add_argument("--framework", choices=["langchain", "llamaindex", "both"], default="both")
    parser.add_argument("--kw", choices=["mysql", "bm25"], default="mysql",
                        help="关键词路:mysql=ngram 全文索引(与后端一致)/ bm25=框架原生 BM25")
    parser.add_argument("--course", type=int, action="append", default=None,
                        help="课程作用域(可多次);缺省为全部有分块的课程")
    parser.add_argument("--top-k", type=int, default=config.TOP_K)
    parser.add_argument("--threshold", type=float, default=config.THRESHOLD)
    parser.add_argument("--rerank", action="store_true", default=True)
    parser.add_argument("--no-rerank", dest="rerank", action="store_false")
    parser.add_argument("--json", action="store_true", help="输出 JSON")
    args = parser.parse_args()

    if args.course is None:
        args.course = mysql_kw.list_course_ids()
        print(f"[info] 课程作用域(自动): {args.course}")

    frameworks = ["langchain", "llamaindex"] if args.framework == "both" else [args.framework]
    try:
        out = [run_one(f, args.query, args) for f in frameworks]
    except Exception as e:
        print(f"[error] {e}", file=sys.stderr)
        return 1
    if args.json:
        print(json.dumps({"query": args.query, "course": args.course, "results": out},
                         ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

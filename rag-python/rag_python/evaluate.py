"""8 题基线评估:与 scripts/bench_rag.py 同一组问题,对比双框架 × 双关键词路的命中率。

输出 rag-python/reports/eval-report.md + eval-report.json。
基线对照(2026-09-12 复测,见 docs/RAG首轮实测报告-20260911.md):Java 生产链路
中文在库 5/5、英文在库 2/2、越界 0 引用。
"""

from __future__ import annotations

import argparse
import itertools
import json
import logging
import time
from pathlib import Path

from . import mysql_kw
from .langchain_impl import LangChainRetriever
from .llamaindex_impl import LlamaIndexRetriever

# 与 scripts/bench_rag.py 完全一致的问题集
QUESTIONS: list[tuple[str, str]] = [
    ("OSTEP-进程(中)", "什么是进程？进程和程序有什么区别？"),
    ("OSTEP-受限直接执行(中)", "受限直接执行（Limited Direct Execution）是如何工作的？"),
    ("OSTEP-地址空间(中)", "什么是地址空间？它包含哪些部分？"),
    ("MIT18.01-导数(中)", "导数的定义是什么？如何理解变化率？"),
    ("MIT18.01-极限(中)", "极限是什么意思？连续性的含义是什么？"),
    ("OSTEP-地址空间(英)", "What is an address space and what does it contain?"),
    ("OSTEP-LDE(英)", "How does limited direct execution work?"),
    ("越界-知识库外(中)", "请介绍量子计算的基本原理。"),
]

# 每题的期望命中文档(标题包含关系);越界题期望为无(判定越界召回是否干净)
EXPECTED: dict[str, str | None] = {
    "OSTEP-进程(中)": "OSTEP-01-Process",
    "OSTEP-受限直接执行(中)": "OSTEP-02-Limited-Direct-Execution",
    "OSTEP-地址空间(中)": "OSTEP-03-Address-Spaces",
    "MIT18.01-导数(中)": "MIT18.01-01-Rates-and-Derivatives",
    "MIT18.01-极限(中)": "MIT18.01-02-Slope-Limits-Continuity",
    "OSTEP-地址空间(英)": "OSTEP-03-Address-Spaces",
    "OSTEP-LDE(英)": "OSTEP-02-Limited-Direct-Execution",
    "越界-知识库外(中)": None,
}

REPORT_DIR = Path(__file__).resolve().parents[1] / "reports"


def main() -> int:
    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(message)s")
    parser = argparse.ArgumentParser(description="8 题基线评估")
    parser.add_argument("--framework", choices=["langchain", "llamaindex", "both"], default="both")
    parser.add_argument("--kw", choices=["mysql", "bm25", "both"], default="both")
    parser.add_argument("--rerank", action="store_true", default=True)
    parser.add_argument("--no-rerank", dest="rerank", action="store_false")
    parser.add_argument("--top-k", type=int, default=5)
    args = parser.parse_args()

    frameworks = ["langchain", "llamaindex"] if args.framework == "both" else [args.framework]
    kw_engines = ["mysql", "bm25"] if args.kw == "both" else [args.kw]
    course_ids = mysql_kw.list_course_ids()
    print(f"[info] 课程作用域: {course_ids}; 模式: {list(itertools.product(frameworks, kw_engines))}")

    results: dict[str, dict] = {}
    for framework, kw in itertools.product(frameworks, kw_engines):
        key = f"{framework}/{kw}"
        print(f"\n[run] {key} ...", flush=True)
        retriever = (LangChainRetriever if framework == "langchain" else LlamaIndexRetriever)(
            kw_engine=kw, rerank_enabled=args.rerank, top_k=args.top_k
        )
        t0 = time.perf_counter()
        per_question = []
        for tag, q in QUESTIONS:
            result = retriever.retrieve(q, course_ids)
            titles = [h.doc_title for h in result.hits]
            expect = EXPECTED[tag]
            hit = expect is not None and any(expect in t for t in titles)
            per_question.append({
                "tag": tag,
                "query": q,
                "hit": hit,
                "top_titles": titles,
                "top_scores": [round(h.rrf_score, 3) for h in result.hits],
                "rerank_scores": [round(h.rerank_score, 3) if h.rerank_score is not None else None
                                  for h in result.hits],
            })
        elapsed = time.perf_counter() - t0
        in_cn = [r for r in per_question if "中" in r["tag"] and "越界" not in r["tag"]]
        in_en = [r for r in per_question if "英" in r["tag"]]
        oob = next(r for r in per_question if "越界" in r["tag"])
        results[key] = {
            "elapsed_s": round(elapsed, 1),
            "zh_hit": sum(1 for r in in_cn if r["hit"]), "zh_total": len(in_cn),
            "en_hit": sum(1 for r in in_en if r["hit"]), "en_total": len(in_en),
            "oob_top5_titles": oob["top_titles"],
            "questions": per_question,
        }
        print(f"  中文 {results[key]['zh_hit']}/{results[key]['zh_total']}  "
              f"英文 {results[key]['en_hit']}/{results[key]['en_total']}  "
              f"越界 top5: {oob['top_titles']}  ({elapsed:.1f}s)")

    REPORT_DIR.mkdir(exist_ok=True)
    with open(REPORT_DIR / "eval-report.json", "w", encoding="utf-8") as f:
        json.dump({"course_ids": course_ids, "rerank": args.rerank, "results": results},
                  f, ensure_ascii=False, indent=2)
    (REPORT_DIR / "eval-report.md").write_text(_render_md(results, args), encoding="utf-8")
    print(f"\n[ok] 报告已写入 {REPORT_DIR}")
    return 0


def _render_md(results: dict, args) -> str:
    lines = [
        "# RAG 检索参考实现评估报告(LangChain / LlamaIndex)",
        "",
        f"- 生成时间:{time.strftime('%Y-%m-%d %H:%M:%S')}  topK={args.top_k}  rerank={'on' if args.rerank else 'off'}",
        "- 问题集与 scripts/bench_rag.py 一致(8 题);期望文档按 docs/RAG首轮实测报告-20260911.md 标注",
        "- Java 生产基线(2026-09-12 复测):中文在库 5/5、英文在库 2/2、越界 references 0",
        "",
        "## 命中率汇总",
        "",
        "| 模式 | 中文在库 | 英文在库 | 耗时 |",
        "|------|---------|---------|------|",
    ]
    for key, r in results.items():
        lines.append(f"| {key} | {r['zh_hit']}/{r['zh_total']} | {r['en_hit']}/{r['en_total']} | {r['elapsed_s']}s |")
    lines += ["", "## 逐题明细", ""]
    for key, r in results.items():
        lines.append(f"### {key}")
        lines.append("")
        lines.append("| 题 | 命中 | top5 文档 | rrf | rerank |")
        lines.append("|----|------|----------|-----|--------|")
        for q in r["questions"]:
            expect = EXPECTED[q["tag"]]
            mark = "✅" if q["hit"] else ("🛡" if expect is None and q["top_titles"] else "❌")
            titles = " / ".join(q["top_titles"]) or "(空)"
            scores = ", ".join(str(s) for s in q["top_scores"])
            rr = ", ".join(str(s) for s in q["rerank_scores"])
            lines.append(f"| {q['tag']} | {mark} | {titles} | {scores} | {rr} |")
        lines.append("")
    return "\n".join(lines)


if __name__ == "__main__":
    raise SystemExit(main())

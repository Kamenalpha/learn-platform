# RAG 检索参考实现评估报告(LangChain / LlamaIndex)

- 生成时间:2026-09-17 16:49:51  topK=5  rerank=off
- 问题集与 scripts/bench_rag.py 一致(8 题);期望文档按 docs/RAG首轮实测报告-20260911.md 标注
- Java 生产基线(2026-09-12 复测):中文在库 5/5、英文在库 2/2、越界 references 0

## 命中率汇总

| 模式 | 中文在库 | 英文在库 | 耗时 |
|------|---------|---------|------|
| langchain/mysql | 5/5 | 2/2 | 2.4s |
| langchain/bm25 | 5/5 | 2/2 | 8.2s |
| llamaindex/mysql | 5/5 | 2/2 | 0.9s |
| llamaindex/bm25 | 5/5 | 2/2 | 1.4s |

## 逐题明细

### langchain/mysql

| 题 | 命中 | top5 文档 | rrf | rerank |
|----|------|----------|-----|--------|
| OSTEP-进程(中) | ✅ | OSTEP-01-Process / OSTEP-03-Address-Spaces / OSTEP-01-Process / OSTEP-01-Process / OSTEP-01-Process | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| OSTEP-受限直接执行(中) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution | 0.032, 0.031, 0.029, 0.016, 0.016 | None, None, None, None, None |
| OSTEP-地址空间(中) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| MIT18.01-导数(中) | ✅ | MIT18.01-01-Rates-and-Derivatives / MIT18.01-01-Rates-and-Derivatives / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| MIT18.01-极限(中) | ✅ | MIT18.01-02-Slope-Limits-Continuity / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| OSTEP-地址空间(英) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces | 0.033, 0.033, 0.031, 0.031, 0.03 | None, None, None, None, None |
| OSTEP-LDE(英) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process | 0.029, 0.027, 0.026, 0.016, 0.016 | None, None, None, None, None |
| 越界-知识库外(中) | 🛡 | OSTEP-01-Process / MIT18.01-01-Rates-and-Derivatives / OSTEP-01-Process / MIT18.01-01-Rates-and-Derivatives / MIT18.01-01-Rates-and-Derivatives | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |

### langchain/bm25

| 题 | 命中 | top5 文档 | rrf | rerank |
|----|------|----------|-----|--------|
| OSTEP-进程(中) | ✅ | OSTEP-03-Address-Spaces / OSTEP-01-Process / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / OSTEP-01-Process | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| OSTEP-受限直接执行(中) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| OSTEP-地址空间(中) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / MIT18.01-02-Slope-Limits-Continuity / OSTEP-03-Address-Spaces / MIT18.01-02-Slope-Limits-Continuity | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| MIT18.01-导数(中) | ✅ | MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-01-Rates-and-Derivatives | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| MIT18.01-极限(中) | ✅ | MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-01-Rates-and-Derivatives / OSTEP-02-Limited-Direct-Execution | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| OSTEP-地址空间(英) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| OSTEP-LDE(英) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process / OSTEP-02-Limited-Direct-Execution | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |
| 越界-知识库外(中) | 🛡 | MIT18.01-02-Slope-Limits-Continuity / MIT18.01-01-Rates-and-Derivatives / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-01-Rates-and-Derivatives / MIT18.01-01-Rates-and-Derivatives | 0.0, 0.0, 0.0, 0.0, 0.0 | None, None, None, None, None |

### llamaindex/mysql

| 题 | 命中 | top5 文档 | rrf | rerank |
|----|------|----------|-----|--------|
| OSTEP-进程(中) | ✅ | OSTEP-01-Process / OSTEP-03-Address-Spaces / OSTEP-01-Process / OSTEP-01-Process / OSTEP-01-Process | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| OSTEP-受限直接执行(中) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution | 0.032, 0.031, 0.029, 0.016, 0.016 | None, None, None, None, None |
| OSTEP-地址空间(中) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| MIT18.01-导数(中) | ✅ | MIT18.01-01-Rates-and-Derivatives / MIT18.01-01-Rates-and-Derivatives / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| MIT18.01-极限(中) | ✅ | MIT18.01-02-Slope-Limits-Continuity / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |
| OSTEP-地址空间(英) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces | 0.033, 0.033, 0.031, 0.031, 0.03 | None, None, None, None, None |
| OSTEP-LDE(英) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process | 0.029, 0.027, 0.026, 0.016, 0.016 | None, None, None, None, None |
| 越界-知识库外(中) | 🛡 | OSTEP-01-Process / MIT18.01-01-Rates-and-Derivatives / OSTEP-01-Process / MIT18.01-01-Rates-and-Derivatives / MIT18.01-01-Rates-and-Derivatives | 0.016, 0.016, 0.016, 0.016, 0.015 | None, None, None, None, None |

### llamaindex/bm25

| 题 | 命中 | top5 文档 | rrf | rerank |
|----|------|----------|-----|--------|
| OSTEP-进程(中) | ✅ | OSTEP-01-Process / MIT18.01-02-Slope-Limits-Continuity / OSTEP-03-Address-Spaces / MIT18.01-02-Slope-Limits-Continuity / OSTEP-01-Process | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| OSTEP-受限直接执行(中) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| OSTEP-地址空间(中) | ✅ | OSTEP-03-Address-Spaces / MIT18.01-02-Slope-Limits-Continuity / OSTEP-03-Address-Spaces / MIT18.01-02-Slope-Limits-Continuity / OSTEP-03-Address-Spaces | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| MIT18.01-导数(中) | ✅ | MIT18.01-01-Rates-and-Derivatives / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-01-Rates-and-Derivatives / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| MIT18.01-极限(中) | ✅ | MIT18.01-02-Slope-Limits-Continuity / MIT18.01-02-Slope-Limits-Continuity / OSTEP-02-Limited-Direct-Execution / MIT18.01-02-Slope-Limits-Continuity / OSTEP-01-Process | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| OSTEP-地址空间(英) | ✅ | OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces / OSTEP-03-Address-Spaces | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| OSTEP-LDE(英) | ✅ | OSTEP-02-Limited-Direct-Execution / OSTEP-01-Process / OSTEP-01-Process / OSTEP-02-Limited-Direct-Execution / OSTEP-02-Limited-Direct-Execution | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |
| 越界-知识库外(中) | 🛡 | OSTEP-01-Process / MIT18.01-02-Slope-Limits-Continuity / MIT18.01-01-Rates-and-Derivatives / MIT18.01-02-Slope-Limits-Continuity / OSTEP-01-Process | 0.017, 0.017, 0.016, 0.016, 0.016 | None, None, None, None, None |

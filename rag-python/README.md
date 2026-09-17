# rag-python — 用 LangChain / LlamaIndex 重写的检索模块参考实现

把后端 Spring AI 版 [`RetrievalService`](../backend/src/main/java/com/rag/edu/service/rag/RetrievalService.java)
的检索流水线用 **LangChain** 与 **LlamaIndex** 各实现一遍，作为主流框架经验对照，
同时可对同一份数据做 8 题基线回归（与 `scripts/bench_rag.py` 同题）。

> 定位：**参考实现/实验模块**，不替换生产链路。生产问答仍走 Java 后端（权限收口、
> 会话、配额、SSE 都在那侧）。本模块只负责"已授权课程范围内"的检索，与 Java 侧
> fail-closed 权限模型一致：`courseIds` 由调用方给出，资源级 `canReadResource`
> 校验仍由 Java 后端执行。

## 流水线（与 Java 版逐级对应）

```
query
 ├─ 向量路: Chroma(learn_platform_knowledge) + courseId IN 过滤 + 相似度阈值
 ├─ 关键词路: MySQL ngram 全文索引(与后端同 SQL) 或 BM25(框架原生,jieba 分词)
 ├─ 权限作用域: 仅课程范围(用户级校验在 Java 侧)
 ├─ RRF 融合: 1/(k+rank), k=60, 融合键 docId:vectorId(chunkId 兜底)
 ├─ (可选) BGE-Reranker 重排: SiliconFlow /v1/rerank, 失败降级融合顺序
 └─ topK 截断
```

两路召回各自实现与降级策略与后端一致：向量路失败 **fail-closed**（阻止全库回退）；
关键词路失败（未建全文索引）**降级为仅向量**；重排失败**降级为融合顺序**。

## 目录结构

```
rag-python/
├── rag_python/
│   ├── config.py          # 读仓库根 .env;默认值与 application.yml 对齐
│   ├── models.py          # Hit/RetrievalResult 数据契约(与后端引用字段对应)
│   ├── fusion.py          # RRF 融合 + 阈值过滤(纯函数,与后端同款数学)
│   ├── mysql_kw.py        # MySQL ngram 关键词路(与 DocChunkMapper 同 SQL)
│   ├── rerank.py          # SiliconFlow /v1/rerank 客户端(失败抛 RerankError)
│   ├── chroma_source.py   # Chroma 客户端:优先 HTTP 服务,回退本地持久化目录
│   ├── embeddings.py      # 嵌入适配器(字符串输入;规避 langchain-openai token id 问题)
│   ├── ingest.py          # 从 MySQL 重建集合(python -m rag_python.ingest)
│   ├── langchain_impl.py  # LangChain 实现
│   ├── llamaindex_impl.py # LlamaIndex 实现
│   ├── cli.py             # 单查询分阶段调试
│   └── evaluate.py        # 8 题基线评估 → reports/eval-report.{md,json}
├── tests/                 # pytest:融合/重排/关键词路
├── requirements.txt
└── README.md
```

## 环境与安装

- Python 3.12;依赖见 `requirements.txt`(`pip install -r requirements.txt`)。
- **数据源**：MySQL(`learn_platform` 库,`doc_chunk`/`resource`/`course`)与
  Chroma 集合 `learn_platform_knowledge`(余弦距离,512 维 bge-small-zh-v1.5)。
  - 连不上 Chroma 时自动回退本地持久化目录 `data/chroma`;
  - 集合缺失/需重建时执行 `python -m rag_python.ingest`(从 MySQL 全量重建,
    与后端 IngestService 同元数据契约)。
- **密钥**：读仓库根 `.env`(已 gitignore)。`EMBED_API_KEY` 支持 SiliconFlow Key,
  也支持本地 ONNX 嵌入服务(`EMBED_BASE_URL=http://localhost:9100`,
  `scripts/embed_server.py`);`RERANK_API_KEY` 为空/占位符时重排安静关闭
  (与后端 `rerankActive` 语义一致)。
- **基础设施**：`scripts/start-infra.ps1` 一键起 MySQL + Chroma + Redis。

## 用法

```bash
# 单查询分阶段调试(默认双框架对比,关键词路=mysql 与生产一致)
python -m rag_python.cli "什么是地址空间？它包含哪些部分？" --course 2

# 框架原生 BM25 关键词路(LangChain EnsembleRetriever / LlamaIndex QueryFusionRetriever)
python -m rag_python.cli "How does limited direct execution work?" --kw bm25 --no-rerank

# 8 题基线评估(输出 reports/eval-report.md)
python -m rag_python.evaluate --no-rerank

# 单测
python -m pytest tests/
```

## 8 题基线结果（2026-09-17 实测）

环境：本机 MySQL + Chroma(重建集合) + 本地 ONNX 嵌入;重排未配置(与 09-12 Java 基线同条件)。
topK=5,阈值 0.4。

| 模式 | 中文在库 | 英文在库 | 越界 | 耗时(8 题) |
|------|---------|---------|------|-----------|
| **Java 生产基线**(2026-09-12 复测) | 5/5 | 2/2 | 拒答 0 引用 | — |
| langchain / mysql(ngram) | 5/5 | 2/2 | 检索出噪声块* | 2.4s |
| langchain / bm25 | 5/5 | 2/2 | 同上 | 8.2s |
| llamaindex / mysql | 5/5 | 2/2 | 同上 | 0.9s |
| llamaindex / bm25 | 5/5 | 2/2 | 同上 | 1.4s |

\* 越界题检索侧仍会带回少量过阈值噪声块——这与 Java 生产行为一致
(09-11 基线同款问题),生产由生成侧拒答过滤
(`ChatService.isNoContentAnswer`)收口,属于生成阶段职责,本检索模块不处理。

## 三框架对照（LangChain vs LlamaIndex vs Spring AI）

同一流水线在三个框架里的惯用组件映射：

| 环节 | Spring AI(生产) | LangChain | LlamaIndex |
|------|----------------|-----------|------------|
| 嵌入 | `EmbeddingModel`(OpenAI 兼容) | `OpenAIEmbeddings` → **需自定义适配器**① | `Settings.embed_model` → **需自定义 BaseEmbedding**② |
| 向量库 | `VectorStore`(Chroma) | `langchain_chroma.Chroma` + 元数据过滤 | `ChromaVectorStore` + `VectorStoreIndex` + `MetadataFilters` |
| 关键词路 | MyBatis ngram SQL | `BM25Retriever`(jieba 分词) | `BM25Retriever`(bm25s,jieba 分词) |
| 混合融合 | 手写 RRF | `EnsembleRetriever(weights=[1,1], c=60)` | `QueryFusionRetriever(mode=RECIPROCAL_RANK, num_queries=1)` |
| 重排 | 手写 HTTP `/v1/rerank` | `BaseDocumentCompressor`(压缩器范式) | `BaseNodePostprocessor`(后处理器范式) |
| 阈值过滤 | `SearchRequest.similarityThreshold` | 召回后按 relevance 过滤 | 召回后按 node.score 过滤 |

### 踩坑记录（2026-09-17 实测钉死）

1. **langchain-openai 1.3.x 把文本本地分词为 token id 再发送**（`input` 为整型数组），
   本地 ONNX 嵌入服务与部分 OpenAI 兼容服务返回 422。→ 自写 `OpenAICompatEmbeddings`
   适配器（字符串输入直连）。
2. **llama-index-embeddings-openai 0.7.x 对 model 名做 OpenAI 枚举校验**，
   `BAAI/bge-small-zh-v1.5` 直接 `ValueError`。→ 包装成 `BaseEmbedding` 子类
   （官方支持的第三方嵌入接入方式），注意需一并实现 `_aget_*` 异步抽象方法。
3. **LangChain 1.x 重构**：`EnsembleRetriever`/`ContextualCompressionRetriever`/
   `BaseDocumentCompressor` 移入 `langchain-classic`;`EnsembleRetriever` 无
   `fusion_mode` 参数，RRF 用 `weights=[1,1] + c=60` 等价实现。
4. **LlamaIndex 0.14 改名**：`QueryFusionMode` → `FUSION_MODES`
   （RRF 对应 `RECIPROCAL_RANK`）;`MetadataFilters` 在
   `llama_index.core.vector_stores.types` 而非 `schema`。
5. **`QueryFusionRetriever` 构造时会解析 `Settings.llm`**（即使 `num_queries=1`
   不调用 LLM），未配 LLM Key 直接初始化失败。→ 用内置 `MockLLM` 占位。
6. **BM25 中文分词**：默认按空白切分，中文整句退化为一个 token。
   LangChain 用 `preprocess_func=jieba.lcut`;LlamaIndex 用 `tokenizer=jieba.lcut`。
7. **BM25 融合模式不暴露分阶段分数**（框架只给顺序，`EnsembleRetriever` 连融合分都不回传），
   调试面板相应列为空——需要分阶段分数做归因时用 mysql 模式。
8. **chromadb 数据迁移**：`data/chroma` 由旧版 chromadb 创建，升级 1.5.9 后
   集合不可见（旧集合挂在已废弃的 database 下），且旧 hnsw 索引 1.5.9 加载挂起。
   → 备份后 `python -m rag_python.ingest` 重建（同模型 bge-small-zh-v1.5，
   分数与旧基线吻合：英文问 0.63–0.71）。

### 与生产 Java 实现的差异（有意为之，均已注释）

- `RERANK_API_KEY` 为空时**自动复用**非占位符的 `EMBED_API_KEY`
  （README 推荐做法；Java 端未实现自动回退，属已知差异）。
- 用户级资源权限（`canReadResource`）不在本模块——检索前调用方已把
  `courseIds` 限定为授权范围。
- 重排在 mysql 模式下作用于融合后结果（与 Java 相同）；bm25 模式下作用于
  框架融合结果（框架原生组合）。

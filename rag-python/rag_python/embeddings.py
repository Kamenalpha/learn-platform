"""嵌入客户端:OpenAI 兼容 /v1/embeddings 字符串输入直连(本地 ONNX 服务 / SiliconFlow 通用)。

背景:
- langchain-openai 1.3.x 会把文本本地分词为 token id 再发送(input 为整型数组),
  本地 ONNX 嵌入服务与部分 OpenAI 兼容服务不认 token id(返回 422),故 LangChain 侧用本适配器。
- llama-index-embeddings-openai 0.7.x 对 model 名做 OpenAI 枚举校验,第三方模型直接报错,
  故 LlamaIndex 侧以本适配器包装成 BaseEmbedding 子类。
"""

from __future__ import annotations

import logging

from . import config

log = logging.getLogger(__name__)


class OpenAICompatEmbeddings:
    """同时实现 LangChain embedding_function 与通用批嵌入的适配器。"""

    def __init__(self, model: str | None = None, base_url: str | None = None,
                 api_key: str | None = None, batch_size: int | None = None):
        from openai import OpenAI

        self.model = model or config.EMBED_MODEL
        self.batch_size = batch_size or config.EMBED_BATCH_SIZE
        self._client = OpenAI(
            api_key=api_key or config.EMBED_API_KEY,
            base_url=base_url or f"{config.EMBED_BASE_URL}/v1",
        )

    # ---- LangChain embedding_function 接口 ----
    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return self.embed_texts(texts)

    def embed_query(self, text: str) -> list[float]:
        return self.embed_texts([text])[0]

    # ---- 通用 ----
    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        out: list[list[float]] = []
        for start in range(0, len(texts), self.batch_size):
            batch = texts[start:start + self.batch_size]
            resp = self._client.embeddings.create(model=self.model, input=batch)
            out.extend(d.embedding for d in resp.data)
        return out


def make_llamaindex_embedding():
    """包装成 llama-index BaseEmbedding(LlamaIndex 官方支持的第三方嵌入接入方式)。"""
    from llama_index.core.embeddings import BaseEmbedding

    impl = OpenAICompatEmbeddings()

    class OpenAICompatBaseEmbedding(BaseEmbedding):
        def _get_query_embedding(self, query: str) -> list[float]:
            return impl.embed_query(query)

        def _get_text_embedding(self, text: str) -> list[float]:
            return impl.embed_texts([text])[0]

        def _get_text_embeddings(self, texts: list[str]) -> list[list[float]]:
            return impl.embed_texts(texts)

        async def _aget_query_embedding(self, query: str) -> list[float]:
            return self._get_query_embedding(query)

        async def _aget_text_embedding(self, text: str) -> list[float]:
            return self._get_text_embedding(text)

        async def _aget_text_embeddings(self, texts: list[str]) -> list[list[float]]:
            return self._get_text_embeddings(texts)

    return OpenAICompatBaseEmbedding()

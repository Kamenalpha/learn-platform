# 本地嵌入服务:OpenAI 兼容 /v1/embeddings 端点(onnxruntime 实现,无需 torch)
# 模型与项目配置一致:BAAI/bge-small-zh-v1.5(512 维,CLS 池化,输出已 L2 归一化)
# 启动:.venv/Scripts/python.exe scripts/embed_server.py(端口 9100)
# 用途:未配置 SiliconFlow Key 时的本地替代,EMBED_BASE_URL=http://localhost:9100
import os
import time

import numpy as np
import onnxruntime as ort
from fastapi import FastAPI
from pydantic import BaseModel
from tokenizers import Tokenizer
import uvicorn

MODEL_DIR = os.environ.get("EMBED_MODEL_DIR", r"E:\RAG\data\models\bge-small-zh-v1.5")
PORT = int(os.environ.get("EMBED_SERVER_PORT", "9100"))
MAX_LEN = 512

app = FastAPI(title="local-embeddings")
_session: ort.InferenceSession | None = None


@app.middleware("http")
async def log_requests(request, call_next):
    body = await request.body()
    print(
        f"[req] {request.method} {request.url.path} "
        f"ct={request.headers.get('content-type')} len={len(body)} "
        f"prefix={body[:200]!r}",
        flush=True,
    )
    return await call_next(request)


def get_session() -> ort.InferenceSession:
    global _session
    if _session is None:
        _session = ort.InferenceSession(
            os.path.join(MODEL_DIR, "onnx", "model.onnx"),
            providers=["CPUExecutionProvider"],
        )
    return _session


def get_tokenizer() -> Tokenizer:
    tok = Tokenizer.from_file(os.path.join(MODEL_DIR, "tokenizer.json"))
    tok.enable_truncation(max_length=MAX_LEN)
    tok.enable_padding()  # 动态 pad 到批内最长,attention_mask 随之生成
    return tok


@app.get("/health")
def health():
    ok = _session is not None
    return {"status": "ok", "model_loaded": ok}


def embed(texts: list[str]) -> np.ndarray:
    tok = get_tokenizer()
    enc = tok.encode_batch(texts)
    ids = np.array([e.ids for e in enc], dtype=np.int64)
    mask = np.array([e.attention_mask for e in enc], dtype=np.int64)
    feeds = {"input_ids": ids, "attention_mask": mask}
    names = {i.name for i in get_session().get_inputs()}
    if "token_type_ids" in names:
        feeds["token_type_ids"] = np.zeros_like(ids)
    out = get_session().run(None, feeds)[0]
    # 3 维 = last_hidden_state(batch,seq,hidden)→ CLS 池化;2 维 = 已池化的(batch,hidden)
    if out.ndim == 3:
        cls = out[:, 0, :]
    elif out.ndim == 2:
        cls = out
    else:
        raise ValueError(f"unexpected onnx output shape: {out.shape}")
    norm = np.linalg.norm(cls, axis=1, keepdims=True)
    norm[norm == 0] = 1.0
    return cls / norm


class EmbeddingRequest(BaseModel):
    input: str | list[str]
    model: str | None = None


@app.post("/v1/embeddings")
def embeddings(req: EmbeddingRequest):
    texts = [req.input] if isinstance(req.input, str) else list(req.input)
    if not texts:
        texts = [""]
    t0 = time.time()
    vectors = embed(texts)
    ms = int((time.time() - t0) * 1000)
    return {
        "object": "list",
        "model": req.model or "bge-small-zh-v1.5",
        "data": [
            {
                "object": "embedding",
                "index": i,
                "embedding": [round(float(x), 7) for x in vectors[i]],
            }
            for i in range(len(texts))
        ],
        "usage": {
            "prompt_tokens": sum(len(t) for t in texts),
            "total_tokens": sum(len(t) for t in texts),
        },
        "elapsed_ms": ms,
    }


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=PORT, log_level="warning")

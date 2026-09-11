# RAG 问答基准:延迟 + 引用命中
import json, re, time, urllib.request, uuid, sys

BASE = "http://localhost:8080"

def login(user, pwd):
    body = json.dumps({"username": user, "password": pwd}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=body, method="POST",
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())["data"]["token"]

TOKEN = login("student", "123456")

QUESTIONS = [
    ("OSTEP-进程(中)", "什么是进程？进程和程序有什么区别？"),
    ("OSTEP-受限直接执行(中)", "受限直接执行（Limited Direct Execution）是如何工作的？"),
    ("OSTEP-地址空间(中)", "什么是地址空间？它包含哪些部分？"),
    ("MIT18.01-导数(中)", "导数的定义是什么？如何理解变化率？"),
    ("MIT18.01-极限(中)", "极限是什么意思？连续性的含义是什么？"),
    ("OSTEP-地址空间(英)", "What is an address space and what does it contain?"),
    ("OSTEP-LDE(英)", "How does limited direct execution work?"),
    ("越界-知识库外(中)", "请介绍量子计算的基本原理。"),
]

def ask(tag, q):
    body = json.dumps({"sessionId": "bench-" + uuid.uuid4().hex[:8], "question": q}).encode()
    req = urllib.request.Request(BASE + "/api/chat/ask", data=body, method="POST", headers={
        "Content-Type": "application/json", "Authorization": "Bearer " + TOKEN})
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            client_ms = (time.perf_counter() - t0) * 1000
            resp = json.loads(r.read())
    except urllib.error.HTTPError as e:
        client_ms = (time.perf_counter() - t0) * 1000
        return {"tag": tag, "q": q, "client_ms": round(client_ms), "http": e.code, "error": e.read()[:200].decode(errors="replace")}
    if resp.get("code") != 0:
        return {"tag": tag, "q": q, "client_ms": round(client_ms), "http": 200, "error": json.dumps(resp, ensure_ascii=False)[:200]}
    d = resp["data"]
    refs = d.get("references") or []
    ans = d.get("answer") or ""
    markers = sorted(set(re.findall(r"\[(\d+)\]", ans)))
    return {
        "tag": tag, "q": q,
        "client_ms": round(client_ms),
        "server_ms": d.get("elapsedMs"),
        "ans_len": len(ans),
        "cite_markers": len(markers),
        "ref_count": len(refs),
        "docs": sorted(set((r.get("docTitle") or "?") for r in refs)),
        "pages": sorted(set(r.get("page") for r in refs if r.get("page") is not None)),
        "scores": [round(r.get("score"), 4) if r.get("score") is not None else None for r in refs],
        "answer_head": ans[:150].replace("\n", " "),
    }

results = []
for tag, q in QUESTIONS:
    print(f"[run] {tag} ...", flush=True)
    results.append(ask(tag, q))
    print(json.dumps(results[-1], ensure_ascii=False), flush=True)

with open(r"E:\RAG\data\rag_bench_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)
ok = [r for r in results if "client_ms" in r and "error" not in r]
if ok:
    lat = sorted(r["client_ms"] for r in ok)
    print(f"\n== 汇总 ==  成功 {len(ok)}/{len(results)}")
    print(f"客户端延迟 ms: min={lat[0]} p50={lat[len(lat)//2]} max={lat[-1]} avg={sum(lat)//len(lat)}")
    srv = [r["server_ms"] for r in ok if r.get("server_ms")]
    if srv:
        print(f"服务端 elapsedMs: min={min(srv)} max={max(srv)} avg={sum(srv)//len(srv)}")
    hit = [r for r in ok if r.get("ref_count")]
    print(f"引用命中(返回>=1条来源): {len(hit)}/{len(ok)}")

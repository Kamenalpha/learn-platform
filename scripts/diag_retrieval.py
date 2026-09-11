# 检索诊断:管理端 debug 接口查看各阶段召回明细
import json, urllib.request, urllib.parse

BASE = "http://localhost:8080"

def login(user, pwd):
    body = json.dumps({"username": user, "password": pwd}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=body, method="POST",
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())["data"]["token"]

def get(path, token):
    req = urllib.request.Request(BASE + path, headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read())

ADMIN = login("admin", "admin123")

print("== 当前 RAG 运行时配置 ==")
print(json.dumps(get("/api/admin/kb/config", ADMIN), ensure_ascii=False))

QS = ["受限直接执行（Limited Direct Execution）是如何工作的？",
      "什么是地址空间？它包含哪些部分？",
      "极限是什么意思？连续性的含义是什么？",
      "什么是进程？进程和程序有什么区别？"]

for q in QS:
    print(f"\n== debug: {q} ==")
    data = get("/api/admin/kb/debug?question=" + urllib.parse.quote(q), ADMIN)
    rows = data.get("data") or []
    if not rows:
        print("  (空召回:两路均无结果)")
    for r in rows[:6]:
        print(f"  rank={r['rank']} doc={r.get('docTitle')} p={r.get('page')} "
              f"vec={r.get('vectorScore')} kw={r.get('keywordScore')} rrf={r.get('rrfScore')} rerank={r.get('rerankScore')}")
        print(f"        {r.get('snippet','')[:90]}")

# 启动基础设施:MySQL + Chroma(项目 venv)+ Redis(项目便携版)
# 用法(管理员 PowerShell):
#   powershell -ExecutionPolicy Bypass -File scripts\start-infra.ps1
# 说明:
#   - MySQL 服务需管理员权限启动(本机服务名 mysql8046)。
#   - Chroma 用项目目录虚拟环境 .venv 启动(端口 8000,数据 data/chroma)。
#   - Redis 用项目便携版 tools/redis(端口 6379,配置 tools/redis/redis.conf)。
#   - LLM/Embed API Key 需另行提供:设置环境变量 LLM_API_KEY / EMBED_API_KEY。
$ErrorActionPreference = 'Stop'

# 1) MySQL 服务
$svc = Get-Service -Name mysql8046 -ErrorAction SilentlyContinue
if ($null -eq $svc) {
    Write-Host '[warn] 未发现 MySQL 服务 mysql8046,请确认已安装/改服务名。' -ForegroundColor Yellow
} elseif ($svc.Status -ne 'Running') {
    Write-Host '[ok] 启动 MySQL 服务 mysql8046 ...'
    net start mysql8046
} else {
    Write-Host '[ok] MySQL 服务已在运行。'
}

# 2) Chroma(venv)
$py = 'E:\RAG\.venv\Scripts\chroma.exe'
if (-not (Test-Path $py)) {
    Write-Host '[error] 未找到 venv chroma,请先创建虚拟环境并安装 chromadb。' -ForegroundColor Red
    exit 1
}
$portOpen = Test-NetConnection -ComputerName 127.0.0.1 -Port 8000 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $portOpen) {
    Write-Host '[ok] 启动 Chroma(venv,端口 8000) ...'
    $env:TEMP = 'E:\RAG\.venv\_tmp'
    $env:TMP = $env:TEMP
    New-Item -ItemType Directory -Force -Path 'E:\RAG\data\chroma' | Out-Null
    Start-Process -FilePath $py -ArgumentList 'run','--host','127.0.0.1','--port','8000','--path','E:\RAG\data\chroma' -WindowStyle Hidden
    Start-Sleep -Seconds 6
} else {
    Write-Host '[ok] Chroma 已在运行(端口 8000)。'
}

# 3) Redis(项目便携版,配置文件方式启动,避免命令行参数转义问题)
$redisExe = 'E:\RAG\tools\redis\redis-server.exe'
$redisConf = 'E:\RAG\tools\redis\redis.conf'
if (-not (Test-Path $redisExe)) {
    Write-Host '[warn] 未找到便携版 Redis(tools\redis\redis-server.exe),Redis 相关功能将降级。' -ForegroundColor Yellow
} elseif (Test-NetConnection -ComputerName 127.0.0.1 -Port 6379 -InformationLevel Quiet -WarningAction SilentlyContinue) {
    Write-Host '[ok] Redis 已在运行(端口 6379)。'
} else {
    Write-Host '[ok] 启动 Redis(便携版,端口 6379) ...'
    Start-Process -FilePath $redisExe -ArgumentList $redisConf -WorkingDirectory 'E:\RAG\tools\redis' -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

if (Test-NetConnection -ComputerName 127.0.0.1 -Port 8000 -InformationLevel Quiet -WarningAction SilentlyContinue) {
    Write-Host '[done] MySQL 就绪; Chroma: http://127.0.0.1:8000; Redis: 127.0.0.1:6379' -ForegroundColor Green
} else {
    Write-Host '[warn] Chroma 尚未就绪,请检查上方输出。' -ForegroundColor Yellow
}
Write-Host '提示: AI 功能还需设置环境变量 LLM_API_KEY / EMBED_API_KEY。'

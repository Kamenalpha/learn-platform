# 通用多学科智能学习平台 (Graduation Project)

面向**所有学习者**的**一站式智能学习平台**(不止计算机学科):任何人不登录也可浏览**公开课程**与**每日知识资讯**;登录后可上传自己的教材并自主分类,定制 AI 助手学习,制定学习计划,在社区交流,做模拟测试(自动出题 + AI 评分),并用 AI 辅助完成课程设计项目——形成"学-练-测-交流-做项目"的完整学习闭环。核心引擎为 **RAG + 大模型**,回答带**可溯源引用**。

> 技术架构:后端 Spring Boot + Spring AI,前端 Vue3 + Element Plus(响应式,PC/移动一套代码),数据库 MySQL + Chroma(向量) + Redis。

**版权说明**:平台公开内容来源于①用户上传并自主公开,②系统每日 9:00 自动抓取的公开资讯(RSS/Atom)。所有内容均标明来源与作者,版权归原作者所有,仅作学习交流;**如若侵权,可联系平台管理员删除**。

## 一、技术栈

| 层级 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.5.x (Java 17) |
| AI 编排 | Spring AI 1.1.8(OpenAI 兼容接 DeepSeek / 通义千问) |
| 数据库 | MySQL 8.0 + MyBatis-Plus(业务库 `learn_platform`) |
| 向量数据库 | Chroma(语义检索,集合 `learn_platform_knowledge`) |
| 缓存 | Redis 7(按用户+会话隔离的对话上下文、图谱缓存、RAG 运行时配置) |
| 文档解析 | Apache PDFBox + Apache POI + **PaddleOCR**(扫描件) |
| 生成大模型 | DeepSeek `deepseek-chat` 或通义 `qwen-plus` |
| 嵌入模型 | SiliconFlow 托管 `BAAI/bge-small-zh-v1.5`(可换 text-embedding-v3) |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |
| 约束规范 | 见 `PROJECT_CONVENTIONS.md`(修改/提交流程、提交规范) |

## 二、功能模块

**用户端**
- **公开内容(游客可用,无需登录)**:「公开资源」浏览用户上传并公开的课程/教材(含试读),**公开需管理员审核通过后展示**;「知识资讯」由系统**每天早上 9:00** 自动抓取公开资讯源(RSS/Atom,默认知乎日报/Solidot/少数派/36氪/阮一峰博客,可用 `NEWS_RSS_FEEDS` 覆盖)入库并**强制标注来源**,管理员可手动触发抓取。个人知识库、AI 问答等个性化功能登录后可用。
- **我的知识库**:学科→课程→章节→知识点 四级分类(用户自建)+ 资料上传(PDF/Word/PPT/TXT,扫描件自动 OCR)、解析、分块、向量入库、预览/重解析/删除;资源可见性(私有/公开/分享),设为公开即提交审核,可在列表查看审核状态(待审核/公开/未通过)。
- **AI 助手**:可创建多个助手,每个绑定课程库(知识隔离),可配置系统提示词、回答风格、上下文轮数和引用开关;自定义提示词只作为教学/表达偏好,不能覆盖知识库限定与防编造规则。生成模型由部署级 `LLM_*` 环境变量统一配置,暂不支持按助手切换。
- **学习计划**:目标→阶段→任务(可关联课程/章节/知识点),完成打卡、连续天数、提醒;完成任务自动打卡。
- **社区**:帖子/文章 + 问答(提问→回答→采纳最佳答案),评论/点赞,可挂课程/知识点;内容可举报/审核。
- **出题模拟**:依据 **教材块 / 用户重点 / 样卷** 三种出处自动出题;题型=单选/多选/判断/填空/简答,题量难度可调;正式考试由服务端记录真实起止时间并执行限时,开考/交卷前接口不返回答案,重复交卷由原子状态更新阻止;**客观题自动判分 + 主观题 AI 评分**(附评语),错题自动入**错题本**。
- **项目辅导**:AI 全流程陪跑——需求拆解→技术方案→任务清单→阶段计划→报告/文档框架,产出沉淀为项目档案。
- **学习画像**:学习时长、今日学习、连续打卡、提问/练习/错题/计划/资料统计,弱项知识点分析。

**管理端(管理员)**:用户管理、**内容审核**(公开课程/教材申请的通过与驳回)、RAG 检索参数(阈值/Top-K/分块)、数据看板与监控(含 **AI 用量/配额**)、资讯手动抓取。AI 助手模板、按助手模型/温度切换尚未实现。

## 三、目录结构

```
RAG/
├── backend/                # Spring Boot 后端
│   └── src/main/java/com/rag/edu/
│       ├── config/         # Web/JWT/MyBatis-Plus/RAG参数/初始数据
│       ├── common/         # 统一响应/异常/JWT/用户上下文
│       ├── controller/     # auth/subjects/courses/chapters/knowledge-points/docs/assistants/plans/community/exam/projects/analytics/chat/public/news/admin…
│       ├── service/        # 各模块业务 + rag/(解析/分块/入库/问答/OCR)
│       ├── entity/ mapper/ dto/
├── frontend/               # Vue3 前端(views/ 下为各模块页面)
├── sql/init_learning.sql   # 新库 learn_platform 建表(34 张表)
├── docs/                   # 前端原型设计、用例图等设计文档
├── PROJECT_CONVENTIONS.md  # 项目约束(工作流/提交规范)
├── backend/Dockerfile      # 后端镜像(maven 构建 → JRE 17 运行)
├── .env.example            # Docker 部署环境变量模板(复制为 .env)
└── docker-compose.yml      # 一键部署:MySQL + Redis + Chroma + 后端
```

## 四、快速开始

### 方式 A:Docker 一键部署(推荐体验,约 5 分钟)
> 前置:安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/) 并保持运行。

```bash
# 1) 准备配置(Windows 用 copy,macOS/Linux 用 cp)
cp .env.example .env
#    编辑 .env,填入 LLM_API_KEY 与 EMBED_API_KEY(服务商见下表)
# 2) 一键构建并启动 MySQL + Redis + Chroma + 后端
#    MySQL 数据卷为空时会自动按序执行 sql/*.sql 完成建库建表
docker compose up -d --build
# 3) 启动前端
cd frontend && npm install && npm run dev    # http://localhost:5173
```

- 后端地址 `http://localhost:8080`,首次启动自动创建账号 **admin/admin123(管理员)、student/123456(用户)**。
- 常用命令:`docker compose logs -f backend` 查看后端日志;`docker compose down` 停止(数据保留在命名卷,删卷需 `docker compose down -v`)。
- 说明:首次建库会执行全部 `sql/*.sql`,其中旧版 `init.sql` 会多建一个不使用的 `rag_edu` 库,可忽略。

### 方式 B:手动部署(本机开发调试)

### 0. 准备 API Key(必填)
| 用途 | 默认服务商 | 环境变量 |
|------|-----------|---------|
| 生成大模型 | DeepSeek | `LLM_API_KEY` |
| 中文嵌入模型 | SiliconFlow(bge-small-zh 免费) | `EMBED_API_KEY` |

### 1. 建库(MySQL)
```bash
mysql -u root -p < sql/init_learning.sql          # 新库 learn_platform(34 张表)
mysql -u root -p < sql/upgrade_guest_public_news.sql   # 老库升级:补 knowledge_news 表
```
> 本机 MySQL 密码若与默认不同,请设置 `DB_PASSWORD`。

### 2. 启动依赖
方式 A(推荐,已内置脚本):`powershell -ExecutionPolicy Bypass -File scripts\start-infra.ps1` 启动 MySQL 服务 + venv 版 Chroma。

方式 B(Docker):`docker compose up -d mysql redis chroma` 只启动依赖容器(需 Docker Desktop)。

> 依赖清单:
> - **MySQL**(3306):服务 `mysql8046`,库 `learn_platform`(需先建库)。
> - **Chroma**(8000):本项目用 Python 虚拟环境 `.venv` 安装 `chromadb` 并运行(见 `scripts/start-infra.ps1`);数据在 `data/chroma`。
> - **Redis**(6379):运行时依赖(对话上下文/图谱缓存/RAG 配置)。对话历史键按 `userId + sessionId` 隔离。本机已装**项目便携版** `tools/redis`(5.0.14,配置 `tools/redis/redis.conf`),`start-infra.ps1` 会自动启动;也可自装 Memurai 或 WSL redis。
> - **API Key**:设置 `LLM_API_KEY` / `EMBED_API_KEY`(AI 调用必需)。

### 3. 启动后端
```bash
cd backend
# 本机默认 JDK 为 8 时需指向 JDK 17:
$env:JAVA_HOME = "E:\JDK\jdk-17.0.12"
$env:DB_PASSWORD = "你的MySQL密码"     # 若与本机不同
$env:LLM_API_KEY = "sk-xxx"
$env:EMBED_API_KEY = "sk-xxx"
mvn spring-boot:run
```
启动后:`http://localhost:8080`,首次自动创建账号 **admin/admin123(管理员)、student/123456(用户)**。

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev    # http://localhost:5173,/api 自动代理到 8080
```
> 无后端时可用 `npm run dev:mock` 走内置 mock 数据预览页面(演示/截图)。

### 5. 验收流程
1. `admin` 或 `student` 登录 → 「我的知识库」确认学科示例(计算机类/数学类/语言类)。
2. 「我的知识库」新建课程 → 上传 PDF 教材,查看解析状态与分块预览。
3. 新建「AI 助手」绑定该课程 → 提问,查看带 [1][2] 引用的回答。
4. 「学习计划」建计划 + 任务,完成打卡。
5. 「出题模拟」选出处生成题目 → 模拟考试 → 查看 AI 评分与解析/错题本。
6. 「项目辅导」输入项目题目 → 查看 AI 拆解方案。
7. 管理端:数据看板、内容审核、RAG 检索参数。

## 五、常用环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `DB_HOST` / `DB_NAME` | localhost / learn_platform | 数据库 |
| `DB_USER` / `DB_PASSWORD` | root / root123456 | 数据库账号 |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 | Redis |
| `CHROMA_HOST` / `CHROMA_PORT` | http://localhost / 8000 | 向量库 |
| `LLM_BASE_URL` / `LLM_MODEL` | api.deepseek.com / deepseek-chat | 生成模型 |
| `EMBED_BASE_URL` / `EMBED_MODEL` | api.siliconflow.cn / BAAI/bge-small-zh-v1.5 | 嵌入模型 |
| `OCR_BASE_URL` | (空) | OCR 服务(PaddleOCR),扫描件解析用 |
| `NEWS_FETCH_CRON` | `0 0 9 * * ?` | 知识资讯定时抓取(默认每天早上 9:00) |
| `NEWS_RSS_FEEDS` | (见 application.yml) | 资讯源列表,每项 `来源名\|分类\|RSS地址`,逗号分隔 |

## 六、RAG 核心链路
1. 文档摄入解析(PDF/Word/PPT/TXT,扫描件走 OCR,保留页码)。2. 递归字符分块。3. 向量化嵌入。4. 写入 Chroma。5. **混合检索**(Chroma 向量 + MySQL ngram 关键词双路召回,**关键词路需先执行 `sql/upgrade_fulltext_index.sql`**,未建索引自动降级为仅向量)RRF 融合,可选 **BGE-Reranker 重排**(管理端开关,需显式配置 `RERANK_API_KEY`,可复用与嵌入相同的 Key;未配置时重排不生效,管理端会显示实际状态)。6. 大模型生成带引用回答。后续统一入 `doc_chunk`/`qa_record`;管理端「检索测试」页可查看各阶段召回明细。

> 该链路不仅用于问答,也支撑"AI 助手限定课程检索"与"出题模拟"(从教材/重点/样卷生成题目)。

## 七、常见问题
- **后端启动连接 Chroma 失败** → 先 `docker compose up -d`,确认 8000 可用。
- **换嵌入模型维度不匹配** → 删除旧 Collection 或对全部课程重建向量。
- **扫描件解析为空** → 需配置 `OCR_BASE_URL`(PaddleOCR 服务);不配则 OCR 关闭。
- **登录不上** → 先确认后端启动、`learn_platform` 已建库、`DB_PASSWORD` 正确。

## 八、后续可扩展(论文展望)
移动端 Vant 组件强化、多模态(图片/视频)检索、语音提问、引用点击跳转 PDF 原文高亮(PDF.js 二期)。

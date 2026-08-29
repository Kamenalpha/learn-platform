# 基于 RAG 的数媒专业知识库问答系统

面向数字媒体技术专业的知识库问答平台:上传课程课件(PDF / Word / PPT / TXT),系统解析、分块、向量化入库;学生用自然语言提问,系统从知识库中召回相关知识块,交由大模型生成**带引用来源(文档名/页码,可溯源)**的回答,并提供考点生成、知识图谱、学习历史等学习辅助功能。

> 毕业设计项目骨架,后端 Spring Boot + Spring AI,前端 Vue3,符合论文大纲"系统总体设计"中的技术选型。

## 一、技术栈

| 层级 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.5.16(Java 17) |
| AI 编排 | Spring AI 1.1.8(OpenAI 兼容协议接 DeepSeek / 通义千问) |
| 向量数据库 | Chroma 1.0(轻量易部署,localhost:8000) |
| 数据库 | MySQL 8.0 + MyBatis-Plus |
| 缓存 | Redis 7(对话上下文、检索参数、图谱缓存) |
| 文档解析 | Apache PDFBox + Apache POI(保留页码用于溯源) |
| 文本分块 | 递归字符分割(默认 500 字符 / 重叠 50,可后台调整) |
| 生成大模型 | 默认 DeepSeek `deepseek-chat`,可换通义千问 `qwen-plus` |
| 嵌入模型 | 默认 SiliconFlow 托管 `BAAI/bge-small-zh-v1.5`(512 维,可换 DashScope `text-embedding-v3`) |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |

> 说明:毕设文档中的 Spring Boot 3.2.x 已停止维护,且 Spring AI 正式版要求 Spring Boot 3.4+,故选用 3.5.x。本机默认 `java` 若为 JDK 8,请安装 JDK 17 并为后端构建配置 `JAVA_HOME`。

## 二、目录结构

```
RAG/
├── backend/                # 后端(Spring Boot 3.5 + Spring AI 1.1)
│   └── src/main/java/com/rag/edu/
│       ├── config/         # Web/JWT拦截器、MyBatis-Plus、RAG参数、初始数据
│       ├── common/         # 统一响应、全局异常、JWT、用户上下文
│       ├── controller/     # auth / chat / courses / docs / assist / admin(kb,stats,users)
│       ├── service/        # 认证、课程、文档、知识库配置、统计
│       ├── service/rag/    # TextExtractor 解析 / TextChunker 分块 / IngestService 入库
│       │                   # ChatService 检索问答 / ExamService 考点 / GraphService 图谱
│       ├── entity/ mapper/ dto/
│   └── src/main/resources/application.yml
├── frontend/               # 前端(Vue3 + Element Plus + ECharts)
│   └── src/
│       ├── views/          # Chat 问答 / Courses 课程 / Documents 文档 / ExamPoints 考点
│       │                   # KnowledgeGraph 图谱 / History 历史 / admin 后台三页
│       ├── api/ store/ router/ layout/ utils/
├── sql/init.sql            # 建库建表 + 初始课程数据
├── docker-compose.yml      # MySQL 8 + Redis 7 + Chroma 1.0
└── README.md
```

## 三、快速开始

### 0. 准备 API Key(必填)

| 用途 | 默认服务商 | 环境变量 |
|------|-----------|---------|
| 生成大模型 | [DeepSeek 开放平台](https://platform.deepseek.com) | `LLM_API_KEY` |
| 中文嵌入模型 | [SiliconFlow 硅基流动](https://siliconflow.cn)(bge-small-zh-v1.5 免费) | `EMBED_API_KEY` |

### 1. 启动基础设施(需要 Docker Desktop)

```bash
docker compose up -d
# MySQL 3306(首次启动自动执行 sql/init.sql 建表)/ Redis 6379 / Chroma 8000
```

没有 Docker 时也可本机安装 MySQL 8 与 Redis,手工导入 `sql/init.sql`;Chroma 可用 `pip install chromadb && chroma run` 启动。

### 2. 启动后端

```bash
cd backend
# Windows PowerShell 示例(本机默认 JDK 为 8 时,需指向 JDK 17):
#   $env:JAVA_HOME = "E:\JDK\jdk-17.0.12"
export LLM_API_KEY=sk-xxx        # Git Bash 写法
export EMBED_API_KEY=sk-xxx
mvn spring-boot:run
```

启动成功后:`http://localhost:8080`。首次启动会自动创建账号 **admin / admin123(管理员)、student / 123456(学生)**。

所有配置均可通过环境变量覆盖(DB_HOST、DB_PASSWORD、REDIS_HOST、CHROMA_HOST、LLM_BASE_URL、LLM_MODEL、EMBED_BASE_URL、EMBED_MODEL 等),详见 `application.yml` 注释。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
# 打开 http://localhost:5173,/api 自动代理到 8080
```

> 本机 npm 默认源若指向公司内部 artifactory(不通),临时换源安装:
> `npm install --registry=https://registry.npmmirror.com`

### 4. 验收流程(建议按此顺序演示)

1. 用 `admin` 登录 → 「知识库浏览」确认 3 门示例课程;
2. 「文档列表」上传 1-2 个 PDF 课件(如数字图像处理)→ 等待解析状态变为"已解析";
3. 「智能问答」提问课件中的知识点,检查回答中的 **[1][2] 引用来源**(文档名 + 页码 + 相似度);
4. 「考点生成」「知识图谱」分别体验章节考点输出与知识点关联图;
5. 管理端:「数据看板」看提问量趋势/问答日志;「知识库管理」调大 Top-K、修改分块大小后对课程"重建向量"再提问对比效果。

## 四、RAG 核心链路(对应论文第五章)

1. **文档摄入解析**:`TextExtractor` 按页抽取文本(PDFBox 按页 / PPT 按幻灯片 / Word、TXT 整篇),保留页码。
2. **递归字符分块**:`TextChunker` 按分隔符优先级(空行→换行→句号→…)递归拆分,目标 500 字符、相邻块重叠 50 字符,避免知识点被切断。
3. **向量嵌入入库**:`IngestService` 调用 bge-small-zh 嵌入,写入 Chroma(向量 ID 规则 `docId-chunkIndex`,与 `doc_chunk` 表一一对应,支持精准删除/重建)。
4. **检索增强问答**:`ChatService` 将问题向量化 → 相似度检索 Top-K(阈值可在后台调)→ 拼接"最近3轮对话 + 知识上下文 + 问题"→ 生成回答并要求标注 [1][2] 引用;引用来源(文档、页码、分块、相似度)落库 `qa_record.reference`。
5. **学习辅助**:`ExamService` 按章节检索后生成知识点梳理与练习题;`GraphService` 用大模型抽取知识块关键词,以共现关系构建知识图谱(Redis 缓存 2 小时)。

## 五、数据库设计

`sql/init.sql` 共 5 张表:用户 `sys_user`、课程 `course`、文档 `course_document`、分块 `doc_chunk`、问答记录 `qa_record`,与论文"核心数据库表设计"一致,另补充了 `course`、`doc_chunk`(支撑课程分类管理与分块可查/可重建)。

## 六、常见问题

- **启动报错连接 Chroma 失败** → 先 `docker compose up -d`,确认 8000 端口可访问;`initialize-schema: true` 会在首次启动自动建 Collection。
- **换嵌入模型后检索报维度不匹配** → 嵌入模型维度变化后需在 Chroma 中删除旧 Collection(或在管理后台对全部课程重建)。
- **上传解析失败** → 查看文档列表"失败原因"列;扫描版 PDF(图片型)无文本层,当前版本不支持 OCR,可列为论文"不足与展望"。
- **DeepSeek 报 embeddings 不支持** → 生成模型与嵌入模型是两个独立服务,DeepSeek 仅用于生成,嵌入走 SiliconFlow/通义,分别配置 `LLM_*` 与 `EMBED_*` 两组变量。
- **端口冲突** → MySQL 3306 / Redis 6379 / Chroma 8000 / 后端 8080 / 前端 5173,可在 docker-compose.yml 与 application.yml 中修改。

## 七、后续可扩展(论文"总结与展望"素材)

- 召回重排序(BGE-Reranker,文档中 RAG 流程第 6 步预留位置在 `ChatService` 检索之后);
- 流式输出(SSE)、语音提问多模态扩展;
- 扫描版 PDF OCR 解析、PPT 图片理解;
- 检索过程可视化(命中分块高亮,答辩演示亮点)。

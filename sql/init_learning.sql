-- ============================================================
-- 通用多学科智能学习平台 - 数据库初始化脚本(重构版)
-- MySQL 8.0 / utf8mb4
-- 说明:该脚本为重构后的学习平台新库,独立于旧版 rag_edu(RAG 问答系统)。
--      覆盖 用户/分类/资料/助手/计划/社区/出题/项目/画像/治理/配额 等模块。
-- ============================================================

CREATE DATABASE IF NOT EXISTS learn_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learn_platform;

-- ============================================================
-- 一、账号与分类体系
-- ============================================================

-- 1. 用户表(角色:0 用户/学习者, 1 管理员)
CREATE TABLE IF NOT EXISTS sys_user (
  user_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  nickname    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
  avatar      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  email       VARCHAR(100) NOT NULL DEFAULT '' COMMENT '邮箱',
  phone       VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
  role        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色:0用户 1管理员',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0禁用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

-- 2. 学科表(四级分类顶层)
CREATE TABLE IF NOT EXISTS subject (
  subject_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '学科ID',
  subject_name VARCHAR(100) NOT NULL COMMENT '学科名称',
  description  VARCHAR(500) NOT NULL DEFAULT '' COMMENT '学科简介',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (subject_id),
  UNIQUE KEY uk_subject_name (subject_name)
) ENGINE = InnoDB COMMENT = '学科表';

-- 3. 课程表(归属学科,可私有/公开/分享)
CREATE TABLE IF NOT EXISTS course (
  course_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  subject_id  BIGINT       NOT NULL COMMENT '所属学科ID',
  owner_id    BIGINT       NOT NULL COMMENT '创建人ID',
  course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '课程简介',
  visibility  TINYINT      NOT NULL DEFAULT 0 COMMENT '可见性:0私有 1公开(审核) 2分享',
  audit_status TINYINT     NOT NULL DEFAULT 0 COMMENT '审核状态:0待审 1通过 2驳回',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (course_id),
  KEY idx_subject_id (subject_id),
  KEY idx_owner_id (owner_id)
) ENGINE = InnoDB COMMENT = '课程表';

-- 4. 章节表(支持章->节两级,parent_id 为空表示章)
CREATE TABLE IF NOT EXISTS chapter (
  chapter_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  course_id    BIGINT       NOT NULL COMMENT '所属课程ID',
  parent_id    BIGINT       NULL DEFAULT NULL COMMENT '父章节ID(章下可再分节)',
  chapter_name VARCHAR(100) NOT NULL COMMENT '章节名称',
  order_num    INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (chapter_id),
  KEY idx_course_id (course_id)
) ENGINE = InnoDB COMMENT = '章节表';

-- 5. 知识点表(四级分类叶子)
CREATE TABLE IF NOT EXISTS knowledge_point (
  kp_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识点ID',
  chapter_id  BIGINT       NOT NULL COMMENT '所属章节ID',
  kp_name     VARCHAR(200) NOT NULL COMMENT '知识点名称',
  description TEXT         NULL COMMENT '知识点说明',
  order_num   INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (kp_id),
  KEY idx_chapter_id (chapter_id)
) ENGINE = InnoDB COMMENT = '知识点表';

-- 6. 标签表(用户自定义)
CREATE TABLE IF NOT EXISTS tag (
  tag_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  user_id     BIGINT       NOT NULL COMMENT '所属用户ID',
  tag_name    VARCHAR(50)  NOT NULL COMMENT '标签名',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (tag_id),
  UNIQUE KEY uk_user_tag (user_id, tag_name)
) ENGINE = InnoDB COMMENT = '标签表';

-- ============================================================
-- 二、资料与知识库
-- ============================================================

-- 7. 资料表(教材/课件/样卷等,含 OCR、审核、可见性)
CREATE TABLE IF NOT EXISTS resource (
  resource_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资料ID',
  user_id       BIGINT       NOT NULL COMMENT '上传人ID',
  course_id     BIGINT       NULL DEFAULT NULL COMMENT '归属课程ID',
  chapter_id    BIGINT       NULL DEFAULT NULL COMMENT '归属章节ID',
  title         VARCHAR(200) NOT NULL COMMENT '资料标题',
  file_type     VARCHAR(20)  NOT NULL COMMENT '文件类型:pdf/word/ppt/txt/image',
  file_url      VARCHAR(255) NOT NULL COMMENT '存储文件名(上传目录内)',
  file_size     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  chunk_count   INT          NOT NULL DEFAULT 0 COMMENT '分块数量',
  parse_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '解析状态:0未解析 1已解析 2失败',
  fail_reason   VARCHAR(500) NULL DEFAULT NULL COMMENT '解析失败原因',
  is_scanned    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否扫描件:0否 1是(需OCR)',
  visibility    TINYINT      NOT NULL DEFAULT 0 COMMENT '可见性:0私有 1公开 2分享',
  audit_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态:0待审 1通过 2驳回',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (resource_id),
  KEY idx_user_id (user_id),
  KEY idx_course_id (course_id)
) ENGINE = InnoDB COMMENT = '资料表';

-- 8. 资料-标签关联表
CREATE TABLE IF NOT EXISTS resource_tag (
  resource_id BIGINT NOT NULL COMMENT '资料ID',
  tag_id      BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (resource_id, tag_id)
) ENGINE = InnoDB COMMENT = '资料-标签关联表';

-- 9. 资料分块表(保留原文,支持手动调整分块与向量删除重建)
CREATE TABLE IF NOT EXISTS doc_chunk (
  chunk_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分块ID',
  resource_id BIGINT      NOT NULL COMMENT '资料ID',
  chunk_index INT         NOT NULL COMMENT '块序号(从0开始)',
  content     TEXT        NOT NULL COMMENT '分块文本内容',
  page_num    INT         NULL DEFAULT NULL COMMENT '所在页码(PDF页/幻灯片页)',
  vector_id   VARCHAR(64) NULL DEFAULT NULL COMMENT '向量库中对应ID(resourceId-chunkIndex)',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (chunk_id),
  KEY idx_resource_id (resource_id)
) ENGINE = InnoDB COMMENT = '资料分块表';

-- 10. 用户重点表(用户划定的重点范围,用于出题)
CREATE TABLE IF NOT EXISTS user_keypoint (
  keypoint_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '重点ID',
  user_id     BIGINT      NOT NULL COMMENT '用户ID',
  resource_id BIGINT      NULL DEFAULT NULL COMMENT '关联资料ID',
  kp_id       BIGINT      NULL DEFAULT NULL COMMENT '关联知识点ID',
  content     TEXT        NOT NULL COMMENT '重点内容(用户划定)',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (keypoint_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '用户重点表';

-- ============================================================
-- 三、AI 助手
-- ============================================================

-- 11. 助手配置表(可定制,适配不同课程)
CREATE TABLE IF NOT EXISTS assistant (
  assistant_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '助手ID',
  user_id        BIGINT       NOT NULL COMMENT '创建人ID',
  name           VARCHAR(100) NOT NULL COMMENT '助手名称',
  avatar         VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  system_prompt  TEXT         NULL COMMENT '系统提示词',
  model          VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '模型:deepseek-chat/qwen-plus等',
  temperature    DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '采样温度(0~1)',
  style          VARCHAR(20)  NOT NULL DEFAULT 'default' COMMENT '回答风格:concise/detailed/tutor',
  context_rounds INT          NOT NULL DEFAULT 3 COMMENT '多轮上下文轮数',
  with_reference TINYINT      NOT NULL DEFAULT 1 COMMENT '是否带引用:0否 1是',
  scope_type     TINYINT      NOT NULL DEFAULT 0 COMMENT '知识范围:0整库 1指定章节',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (assistant_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = 'AI助手配置表';

-- 12. 助手-课程绑定表(知识隔离的依据)
CREATE TABLE IF NOT EXISTS assistant_course (
  assistant_id BIGINT NOT NULL COMMENT '助手ID',
  course_id    BIGINT NOT NULL COMMENT '课程ID',
  PRIMARY KEY (assistant_id, course_id)
) ENGINE = InnoDB COMMENT = '助手-课程绑定表';

-- 13. 问答记录表(学习历史,支持收藏)
CREATE TABLE IF NOT EXISTS qa_record (
  record_id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id     BIGINT      NOT NULL COMMENT '提问用户ID',
  assistant_id BIGINT     NULL DEFAULT NULL COMMENT '使用的助手ID',
  session_id  VARCHAR(64) NOT NULL COMMENT '对话会话ID',
  question    TEXT        NOT NULL COMMENT '用户问题',
  answer      TEXT        NULL COMMENT '系统回答',
  reference   TEXT        NULL COMMENT '引用来源(JSON数组)',
  is_favorite TINYINT     NOT NULL DEFAULT 0 COMMENT '是否收藏:0否 1是',
  elapsed_ms  BIGINT      NOT NULL DEFAULT 0 COMMENT '回答耗时(毫秒)',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
  PRIMARY KEY (record_id),
  KEY idx_user_id (user_id),
  KEY idx_session_id (session_id)
) ENGINE = InnoDB COMMENT = '问答记录表';

-- ============================================================
-- 四、学习计划
-- ============================================================

-- 14. 学习计划表
CREATE TABLE IF NOT EXISTS study_plan (
  plan_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  user_id     BIGINT       NOT NULL COMMENT '所属用户ID',
  title       VARCHAR(100) NOT NULL COMMENT '计划标题',
  goal        VARCHAR(500) NOT NULL DEFAULT '' COMMENT '计划目标',
  start_date  DATE         NULL COMMENT '开始日期',
  end_date    DATE         NULL COMMENT '截止日期',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0进行中 1已完成 2已取消',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (plan_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '学习计划表';

-- 15. 计划任务表(关联课程/章节/知识点,支持打卡与提醒)
CREATE TABLE IF NOT EXISTS plan_task (
  task_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  plan_id     BIGINT       NOT NULL COMMENT '所属计划ID',
  course_id   BIGINT       NULL DEFAULT NULL COMMENT '关联课程ID',
  chapter_id  BIGINT       NULL DEFAULT NULL COMMENT '关联章节ID',
  kp_id       BIGINT       NULL DEFAULT NULL COMMENT '关联知识点ID',
  title       VARCHAR(200) NOT NULL COMMENT '任务标题',
  plan_date   DATE         NULL COMMENT '计划日期',
  done        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成:0否 1是',
  reminder    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否提醒:0否 1是',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (task_id),
  KEY idx_plan_id (plan_id)
) ENGINE = InnoDB COMMENT = '计划任务表';

-- ============================================================
-- 五、社区
-- ============================================================

-- 16. 帖子表(帖子/文章/问题)
CREATE TABLE IF NOT EXISTS post (
  post_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  user_id       BIGINT       NOT NULL COMMENT '发帖人ID',
  title         VARCHAR(200) NOT NULL COMMENT '标题',
  content       TEXT         NOT NULL COMMENT '内容',
  type          TINYINT      NOT NULL DEFAULT 0 COMMENT '类型:0帖子/文章 1问题',
  course_id     BIGINT       NULL DEFAULT NULL COMMENT '关联课程ID',
  kp_id         BIGINT       NULL DEFAULT NULL COMMENT '关联知识点ID',
  view_count    INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  like_count    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
  comment_count INT          NOT NULL DEFAULT 0 COMMENT '评论数',
  is_ai         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否AI生成:0否 1是',
  audit_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态:0待审 1通过 2驳回',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (post_id),
  KEY idx_user_id (user_id),
  KEY idx_course_id (course_id)
) ENGINE = InnoDB COMMENT = '帖子表';

-- 17. 回答表(问题的最佳答案被采纳)
CREATE TABLE IF NOT EXISTS answer (
  answer_id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '回答ID',
  post_id     BIGINT      NOT NULL COMMENT '所属问题ID(post.type=1)',
  user_id     BIGINT      NOT NULL COMMENT '回答人ID',
  content     TEXT        NOT NULL COMMENT '回答内容',
  is_accepted TINYINT     NOT NULL DEFAULT 0 COMMENT '是否被采纳:0否 1是',
  like_count  INT         NOT NULL DEFAULT 0 COMMENT '点赞数',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回答时间',
  PRIMARY KEY (answer_id),
  KEY idx_post_id (post_id)
) ENGINE = InnoDB COMMENT = '回答表';

-- 18. 评论表
CREATE TABLE IF NOT EXISTS comment (
  comment_id  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  post_id     BIGINT      NOT NULL COMMENT '帖子ID',
  answer_id   BIGINT      NULL DEFAULT NULL COMMENT '回答ID(对某回答评论则填)',
  user_id     BIGINT      NOT NULL COMMENT '评论人ID',
  content     TEXT        NOT NULL COMMENT '评论内容',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (comment_id),
  KEY idx_post_id (post_id)
) ENGINE = InnoDB COMMENT = '评论表';

-- 19. 点赞表
CREATE TABLE IF NOT EXISTS post_like (
  like_id     BIGINT   NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  post_id     BIGINT   NOT NULL COMMENT '帖子ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (like_id),
  UNIQUE KEY uk_user_post (user_id, post_id)
) ENGINE = InnoDB COMMENT = '点赞表';

-- 20. 关注表
CREATE TABLE IF NOT EXISTS follow (
  follow_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  follower_id BIGINT   NOT NULL COMMENT '关注者ID',
  followee_id BIGINT   NOT NULL COMMENT '被关注者ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (follow_id),
  UNIQUE KEY uk_follow (follower_id, followee_id)
) ENGINE = InnoDB COMMENT = '关注表';

-- ============================================================
-- 六、出题模拟
-- ============================================================

-- 21. 题目表(由教材块/用户重点/样卷自动生成)
CREATE TABLE IF NOT EXISTS question (
  question_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  owner_id    BIGINT      NOT NULL COMMENT '生成人ID',
  resource_id BIGINT      NULL DEFAULT NULL COMMENT '来源资料ID',
  kp_id       BIGINT      NULL DEFAULT NULL COMMENT '关联知识点ID',
  qtype       TINYINT     NOT NULL COMMENT '题型:0单选 1多选 2判断 3填空 4简答',
  stem        TEXT        NOT NULL COMMENT '题干',
  options     TEXT        NULL COMMENT '选项(JSON,选择题)',
  answer      TEXT        NULL COMMENT '参考答案',
  analysis    TEXT        NULL COMMENT '解析',
  difficulty  TINYINT     NOT NULL DEFAULT 1 COMMENT '难度:1简单 2中等 3难',
  source_type TINYINT     NOT NULL DEFAULT 0 COMMENT '出处:0教材块 1用户重点 2样卷',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (question_id),
  KEY idx_owner_id (owner_id),
  KEY idx_source_type (source_type)
) ENGINE = InnoDB COMMENT = '题目表';

-- 22. 试卷表
CREATE TABLE IF NOT EXISTS paper (
  paper_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
  user_id     BIGINT      NOT NULL COMMENT '创建人ID',
  title       VARCHAR(200) NOT NULL COMMENT '试卷标题',
  source_type TINYINT     NOT NULL DEFAULT 0 COMMENT '出处:0教材块 1用户重点 2样卷',
  difficulty  TINYINT     NOT NULL DEFAULT 1 COMMENT '总体难度',
  total_score INT         NOT NULL DEFAULT 0 COMMENT '总分',
  duration_min INT        NOT NULL DEFAULT 0 COMMENT '限时(分钟),0不限',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (paper_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '试卷表';

-- 23. 试卷-题目关联表
CREATE TABLE IF NOT EXISTS paper_question (
  paper_id    BIGINT NOT NULL COMMENT '试卷ID',
  question_id BIGINT NOT NULL COMMENT '题目ID',
  score       INT    NOT NULL DEFAULT 0 COMMENT '本题分值',
  order_num   INT    NOT NULL DEFAULT 0 COMMENT '题序',
  PRIMARY KEY (paper_id, question_id)
) ENGINE = InnoDB COMMENT = '试卷-题目关联表';

-- 24. 考试/答题记录表
CREATE TABLE IF NOT EXISTS exam_record (
  exam_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '考试ID',
  user_id     BIGINT      NOT NULL COMMENT '考生ID',
  paper_id    BIGINT      NOT NULL COMMENT '试卷ID',
  start_time  DATETIME    NULL COMMENT '开始时间',
  end_time    DATETIME    NULL COMMENT '交卷时间',
  score       DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '得分',
  status      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态:0进行中 1已交卷 2已截止',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (exam_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '考试记录表';

-- 25. 逐题作答表(含AI评分与评语)
CREATE TABLE IF NOT EXISTS exam_answer (
  exam_answer_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '作答ID',
  exam_id        BIGINT       NOT NULL COMMENT '考试ID',
  question_id    BIGINT       NOT NULL COMMENT '题目ID',
  user_answer    TEXT         NULL COMMENT '用户作答',
  is_correct     TINYINT      NULL DEFAULT NULL COMMENT '客观题对错:0错 1对 NULL待评',
  ai_score       DECIMAL(6,1) NULL DEFAULT NULL COMMENT '主观题AI评分',
  ai_comment     TEXT         NULL COMMENT 'AI评语',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (exam_answer_id),
  KEY idx_exam_id (exam_id)
) ENGINE = InnoDB COMMENT = '逐题作答表';

-- 26. 错题本表
CREATE TABLE IF NOT EXISTS mistake (
  mistake_id      BIGINT   NOT NULL AUTO_INCREMENT COMMENT '错题ID',
  user_id         BIGINT   NOT NULL COMMENT '用户ID',
  question_id     BIGINT   NOT NULL COMMENT '题目ID',
  wrong_count     INT      NOT NULL DEFAULT 1 COMMENT '答错次数',
  last_wrong_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近答错时间',
  mastered        TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已掌握:0否 1是',
  PRIMARY KEY (mistake_id),
  UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE = InnoDB COMMENT = '错题本表';

-- ============================================================
-- 七、课程设计项目辅导
-- ============================================================

-- 27. 项目辅导档案表
CREATE TABLE IF NOT EXISTS project_case (
  project_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  user_id       BIGINT       NOT NULL COMMENT '学生ID',
  project_title VARCHAR(200) NOT NULL COMMENT '项目标题',
  requirement   TEXT         NULL COMMENT '项目需求',
  tech_solution TEXT         NULL COMMENT '技术方案',
  task_list     TEXT         NULL COMMENT '任务清单(JSON)',
  stage_plan    TEXT         NULL COMMENT '阶段计划(JSON)',
  doc_content   TEXT         NULL COMMENT '文档/报告框架',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (project_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '项目辅导档案表';

-- ============================================================
-- 八、学习画像与统计
-- ============================================================

-- 28. 学习行为日志表(时长/掌握度/弱项数据来源)
CREATE TABLE IF NOT EXISTS study_log (
  log_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  user_id       BIGINT       NOT NULL COMMENT '用户ID',
  activity_type TINYINT      NOT NULL COMMENT '行为类型:0问答 1做题 2打卡 3浏览 4项目',
  course_id     BIGINT       NULL DEFAULT NULL COMMENT '关联课程ID',
  duration_sec  INT          NOT NULL DEFAULT 0 COMMENT '耗时(秒)',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (log_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '学习行为日志表';

-- 29. 打卡表(连续天数)
CREATE TABLE IF NOT EXISTS checkin (
  checkin_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '打卡ID',
  user_id      BIGINT   NOT NULL COMMENT '用户ID',
  plan_id      BIGINT   NULL DEFAULT NULL COMMENT '关联计划ID',
  checkin_date DATE     NOT NULL COMMENT '打卡日期',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  PRIMARY KEY (checkin_id),
  UNIQUE KEY uk_user_date (user_id, checkin_date)
) ENGINE = InnoDB COMMENT = '打卡表';

-- ============================================================
-- 九、治理、权限与配额
-- ============================================================

-- 30. 内容审核记录表(社区帖子、公开资源、回答等)
CREATE TABLE IF NOT EXISTS review_record (
  review_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
  target_type TINYINT      NOT NULL COMMENT '目标类型:0帖子 1资源 2回答',
  target_id   BIGINT       NOT NULL COMMENT '目标ID',
  reporter_id BIGINT       NULL DEFAULT NULL COMMENT '举报人ID(手动举报)',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0待审 1通过 2驳回',
  reason      VARCHAR(500) NULL DEFAULT NULL COMMENT '审核/驳回原因',
  reviewer_id BIGINT       NULL DEFAULT NULL COMMENT '审核人ID',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (review_id)
) ENGINE = InnoDB COMMENT = '内容审核记录表';

-- 31. 分享链接表(资源/助手/帖子,支持密码与有效期)
CREATE TABLE IF NOT EXISTS share_link (
  share_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  owner_id    BIGINT       NOT NULL COMMENT '分享人ID',
  target_type TINYINT      NOT NULL COMMENT '目标类型:0资源 1助手 2帖子',
  target_id   BIGINT       NOT NULL COMMENT '目标ID',
  token       VARCHAR(64)  NOT NULL COMMENT '分享令牌',
  password    VARCHAR(100) NOT NULL DEFAULT '' COMMENT '访问密码(可为空)',
  expire_time DATETIME     NULL DEFAULT NULL COMMENT '过期时间(空为永久)',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (share_id),
  UNIQUE KEY uk_token (token)
) ENGINE = InnoDB COMMENT = '分享链接表';

-- 32. 配额/用量记录表(单用户月度额度与限流)
CREATE TABLE IF NOT EXISTS quota_log (
  quota_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '配额日志ID',
  user_id     BIGINT      NOT NULL COMMENT '用户ID',
  action_type VARCHAR(30) NOT NULL COMMENT '动作:chat/generate/grade等',
  month       CHAR(6)     NOT NULL COMMENT '月份(YYYYMM)',
  use_count   INT         NOT NULL DEFAULT 0 COMMENT '当月累计次数',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (quota_id),
  UNIQUE KEY uk_user_action_month (user_id, action_type, month)
) ENGINE = InnoDB COMMENT = '配额/用量记录表';

-- 初始学科示例(演示用,可自行增删)
INSERT INTO subject (subject_name, description) VALUES
('计算机类', '计算机科学与技术、软件工程、人工智能等课程'),
('数学类', '高等数学、线性代数、概率论等课程'),
('语言类', '大学英语、文学等课程');

-- 初始用户由后端启动时自动创建(DataInitializer):
--   管理员 admin / admin123
--   用户   student / 123456

-- 33. 知识资讯表:系统每天早上 9:00 自动从公开资讯源(RSS/Atom)抓取,
--     入库强制标注来源(source_name/source_url),仅作学习导航,
--     版权:版权归原作者所有,如若侵权可联系删除。
CREATE TABLE IF NOT EXISTS knowledge_news (
  news_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
  title        VARCHAR(512) NOT NULL COMMENT '标题',
  summary      VARCHAR(1024)          DEFAULT NULL COMMENT '摘要(截断存储,仅作索引导航)',
  source_name  VARCHAR(128) NOT NULL COMMENT '来源名称(版权标注)',
  source_url   VARCHAR(768) NOT NULL COMMENT '原文链接(唯一,去重)',
  category     VARCHAR(32)  NOT NULL DEFAULT '综合' COMMENT '分类:综合/科技/商业科技/数字生活等',
  published_at DATETIME               DEFAULT NULL COMMENT '原文发布时间',
  fetched_at   DATETIME     NOT NULL COMMENT '本次抓取时间',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (news_id),
  UNIQUE KEY uk_source_url (source_url (255)),
  KEY idx_category (category),
  KEY idx_fetched_at (fetched_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每日知识资讯(自动抓取,标注来源,如若侵权可联系删除)';

-- 说明:公开课程复用 course.visibility=1 与 resource.visibility=1,
-- 游客通过 /api/public/** 只读访问,登录后才可用个人知识库等个性化功能。

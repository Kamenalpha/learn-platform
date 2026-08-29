-- ============================================================
-- 基于 RAG 的数媒专业知识库问答系统 - 数据库初始化脚本
-- MySQL 8.0 / utf8mb4
-- Docker 部署时由容器首次启动自动执行;手动部署时请自行导入
-- ============================================================

CREATE DATABASE IF NOT EXISTS rag_edu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rag_edu;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  user_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  nickname    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
  role        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色:0学生 1管理员',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

-- 2. 课程表(课程分类管理)
CREATE TABLE IF NOT EXISTS course (
  course_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '课程简介',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (course_id),
  UNIQUE KEY uk_course_name (course_name)
) ENGINE = InnoDB COMMENT = '课程表';

-- 3. 课程文档表
CREATE TABLE IF NOT EXISTS course_document (
  doc_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  course_id    BIGINT       NOT NULL COMMENT '所属课程ID',
  doc_title    VARCHAR(200) NOT NULL COMMENT '文档标题',
  file_type    VARCHAR(20)  NOT NULL COMMENT '文件类型:pdf/word/ppt/txt',
  file_url     VARCHAR(255) NOT NULL COMMENT '存储文件名(上传目录内)',
  file_size    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  chunk_count  INT          NOT NULL DEFAULT 0 COMMENT '分块数量',
  parse_status TINYINT      NOT NULL DEFAULT 0 COMMENT '解析状态:0未解析 1已解析 2失败',
  fail_reason  VARCHAR(500) NULL DEFAULT NULL COMMENT '解析失败原因',
  uploader_id  BIGINT       NULL DEFAULT NULL COMMENT '上传人ID',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (doc_id),
  KEY idx_course_id (course_id)
) ENGINE = InnoDB COMMENT = '课程文档表';

-- 4. 文档分块表(保留原文,支持手动查看/调整分块与向量删除重建)
CREATE TABLE IF NOT EXISTS doc_chunk (
  chunk_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分块ID',
  doc_id      BIGINT      NOT NULL COMMENT '文档ID',
  chunk_index INT         NOT NULL COMMENT '块序号(从0开始)',
  content     TEXT        NOT NULL COMMENT '分块文本内容',
  page_num    INT         NULL DEFAULT NULL COMMENT '所在页码(PDF页/幻灯片页),无页概念为NULL',
  vector_id   VARCHAR(64) NULL DEFAULT NULL COMMENT '向量库中对应ID(docId-chunkIndex)',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (chunk_id),
  KEY idx_doc_id (doc_id)
) ENGINE = InnoDB COMMENT = '文档分块表';

-- 5. 问答记录表(学习历史,支持收藏)
CREATE TABLE IF NOT EXISTS qa_record (
  record_id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id     BIGINT      NOT NULL COMMENT '提问用户ID',
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

-- 初始课程数据(对应数媒专业核心课程,可自行增删)
INSERT INTO course (course_name, description) VALUES
('数字图像处理', '数字媒体技术专业核心课程:图像变换、图像增强、图像分割、特征提取等'),
('计算机图形学', '图形绘制管线、几何造型、真实感渲染、交互技术等'),
('数字视频处理', '视频编码、运动估计、目标跟踪、视频结构化分析等');

-- 初始用户由后端启动时自动创建(DataInitializer):
--   管理员 admin / admin123
--   学生   student / 123456

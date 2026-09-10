-- ============================================================
-- 升级脚本:AI 学情观察表(2026-09-10)
-- 用途:AI 批改主观题产生的评语沉淀为"AI 诊断",学习画像页消费。
-- 老库直接执行本脚本;新库执行 init_learning.sql 已含此表。
-- ============================================================
USE learn_platform;

CREATE TABLE IF NOT EXISTS ai_diagnosis (
  diagnosis_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '观察ID',
  user_id      BIGINT      NOT NULL COMMENT '用户ID',
  kp_id        BIGINT      NULL DEFAULT NULL COMMENT '关联知识点ID(可空)',
  source_type  TINYINT     NOT NULL DEFAULT 0 COMMENT '来源:0 AI评分评语 1 问答误解(二期预留)',
  content      TEXT        NOT NULL COMMENT '观察内容(AI评语)',
  ref_id       BIGINT      NULL DEFAULT NULL COMMENT '来源记录ID(exam_answer_id),唯一键防重',
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (diagnosis_id),
  UNIQUE KEY uk_ref (ref_id),
  KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 学情观察表';

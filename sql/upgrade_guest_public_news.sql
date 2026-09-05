-- ============================================================
-- 升级脚本:面向公众开放(游客可学)+ 每日知识资讯
-- 适用于已按 sql/init_learning.sql 建库的环境,可重复执行。
-- 日期:2026-09-05
-- ============================================================
USE learn_platform;

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

-- 说明:公开课程复用 course.visibility = 1(公开)与 resource.visibility = 1,
-- 无需新增列;游客通过 /api/public/** 只读访问,登录后才可使用个人知识库等个性化功能。

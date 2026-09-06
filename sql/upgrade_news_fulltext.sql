-- 资讯正文入库升级(2026-09-06)
-- content 存抽取后的纯文本正文;可空,抓取失败时退化为"标题+摘要+原文链接"模式
-- 可重复执行(information_schema 守卫)
USE learn_platform;

SET @ddl = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'knowledge_news'
        AND COLUMN_NAME = 'content') = 0,
    'ALTER TABLE knowledge_news ADD COLUMN content LONGTEXT NULL COMMENT ''资讯正文(纯文本,已标注出处)'' AFTER summary',
    'SELECT 1')
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

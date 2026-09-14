-- 混合检索:为 doc_chunk.content 建立 MySQL ngram 全文索引(支持中文分词)。
-- 说明:检索时若无此索引,MATCH 会报错,系统已降级为仅向量检索;
--       建议执行本脚本后再启用混合检索。
-- 可重复执行(information_schema 守卫):新库 init_learning.sql 已建索引,本脚本自动跳过;
--       老库(Docker 首建或手动建库均可)执行本脚本正常补建。
-- 注意:全文索引会占用磁盘与写入开销,分块表为中等规模(课程教材),影响可接受。
USE learn_platform;

SET @ddl = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'doc_chunk'
        AND INDEX_NAME = 'ft_doc_chunk_content'
        AND INDEX_TYPE = 'FULLTEXT') = 0,
    'ALTER TABLE doc_chunk ADD FULLTEXT INDEX ft_doc_chunk_content (content) WITH PARSER ngram',
    'SELECT 1')
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

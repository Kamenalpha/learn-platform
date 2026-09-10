-- 混合检索:为 doc_chunk.content 建立 MySQL ngram 全文索引(支持中文分词)。
-- 说明:检索时若无此索引,MATCH 会报错,系统已降级为仅向量检索;
--       建议执行本脚本后再启用混合检索。老库与新库均可执行(幂等需手动确认不存在)。
-- 注意:全文索引会占用磁盘与写入开销,分块表为中等规模(课程教材),影响可接受。
USE learn_platform;
ALTER TABLE doc_chunk ADD FULLTEXT INDEX ft_doc_chunk_content (content) WITH PARSER ngram;

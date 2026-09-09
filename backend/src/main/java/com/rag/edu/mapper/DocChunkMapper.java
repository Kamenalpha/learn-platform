package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.DocChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface DocChunkMapper extends BaseMapper<DocChunk> {

    /** 按课程取分块(知识图谱构建用) */
    @Select("""
            SELECT ch.* FROM doc_chunk ch
            JOIN resource r ON r.resource_id = ch.resource_id
            WHERE r.course_id = #{courseId}
            ORDER BY ch.resource_id, ch.chunk_index
            LIMIT #{limit}
            """)
    List<DocChunk> listByCourse(@Param("courseId") Long courseId, @Param("limit") int limit);

    /** 混合检索关键词路:MySQL ngram 全文索引,按相关度召回(需先执行 upgrade_fulltext_index.sql) */
    @Select("""
            <script>
            SELECT c.resource_id AS resourceId, c.chunk_id AS chunkId, c.content AS content,
                   c.page_num AS pageNum, c.vector_id AS vectorId, r.title AS docTitle,
                   MATCH(c.content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS kwScore
            FROM doc_chunk c JOIN resource r ON r.resource_id = c.resource_id
            WHERE MATCH(c.content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) &gt; 0
            <if test="courseIds != null and courseIds.size() > 0">
              AND r.course_id IN
              <foreach collection="courseIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
            </if>
            ORDER BY kwScore DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> keywordSearch(@Param("query") String query,
                                            @Param("courseIds") List<Long> courseIds,
                                            @Param("limit") int limit);
}

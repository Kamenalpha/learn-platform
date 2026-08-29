package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.DocChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DocChunkMapper extends BaseMapper<DocChunk> {

    /** 按课程取分块(知识图谱构建用) */
    @Select("""
            SELECT ch.* FROM doc_chunk ch
            JOIN course_document d ON d.doc_id = ch.doc_id
            WHERE d.course_id = #{courseId}
            ORDER BY ch.doc_id, ch.chunk_index
            LIMIT #{limit}
            """)
    List<DocChunk> listByCourse(@Param("courseId") Long courseId, @Param("limit") int limit);
}

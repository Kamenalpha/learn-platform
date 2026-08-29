package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.CourseDocument;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CourseDocumentMapper extends BaseMapper<CourseDocument> {

    /** 各课程文档数量统计 */
    @Select("""
            SELECT c.course_name AS name, COUNT(d.doc_id) AS value
            FROM course c LEFT JOIN course_document d ON d.course_id = c.course_id
            GROUP BY c.course_id, c.course_name ORDER BY c.course_id
            """)
    List<Map<String, Object>> countByCourse();
}

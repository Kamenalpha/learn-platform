package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.Course;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CourseMapper extends BaseMapper<Course> {

    /** 课程列表 + 每门课程的文档数量 */
    @Select("""
            SELECT c.course_id, c.course_name, c.description, c.create_time,
                   (SELECT COUNT(*) FROM course_document d WHERE d.course_id = c.course_id) AS doc_count
            FROM course c ORDER BY c.course_id
            """)
    List<Map<String, Object>> listWithDocCount();
}

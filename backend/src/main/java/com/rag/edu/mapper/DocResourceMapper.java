package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.DocResource;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface DocResourceMapper extends BaseMapper<DocResource> {

    /** 各课程资料数量统计 */
    @Select("""
            SELECT c.course_name AS name, COUNT(r.resource_id) AS value
            FROM course c LEFT JOIN resource r ON r.course_id = c.course_id
            GROUP BY c.course_id, c.course_name ORDER BY c.course_id
            """)
    List<Map<String, Object>> countByCourse();
}

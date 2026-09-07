package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.Course;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CourseMapper extends BaseMapper<Course> {

    /** 课程列表 + 每门课程的资料数量(归属用户 + 公开可见) */
    @Select("""
            SELECT c.course_id, c.course_name, c.subject_id, c.owner_id, c.description, c.visibility,
                   c.audit_status, c.create_time,
                   (SELECT COUNT(*) FROM resource r WHERE r.course_id = c.course_id
                     AND (c.owner_id = #{userId} OR r.visibility = 1 AND r.audit_status = 1)) AS doc_count,
                   (SELECT s.subject_name FROM subject s WHERE s.subject_id = c.subject_id) AS subject_name
            FROM course c
            WHERE c.owner_id = #{userId}
               OR c.visibility = 1 AND c.audit_status = 1
            ORDER BY c.create_time DESC
            """)
    List<Map<String, Object>> listVisible(@Param("userId") Long userId);

    /** 统计某课程下的资料数量(用于删除校验) */
    @Select("SELECT COUNT(*) FROM resource WHERE course_id = #{courseId}")
    Long countResources(@Param("courseId") Long courseId);
}

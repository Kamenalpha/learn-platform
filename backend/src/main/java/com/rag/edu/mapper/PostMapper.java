package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PostMapper extends BaseMapper<Post> {

    /** 帖子列表(已审核通过,含发帖人昵称),可按课程/类型过滤 */
    @Select("""
            <script>
            SELECT p.post_id, p.title, p.content, p.type, p.course_id, p.view_count, p.like_count,
                   p.comment_count, p.is_ai, p.create_time, u.nickname AS author_name
            FROM post p LEFT JOIN sys_user u ON u.user_id = p.user_id
            WHERE p.audit_status = 1
            <if test="courseId != null"> AND p.course_id = #{courseId} </if>
            <if test="type != null"> AND p.type = #{type} </if>
            ORDER BY p.create_time DESC
            LIMIT 100
            </script>
            """)
    List<Map<String, Object>> listFeed(@Param("courseId") Long courseId, @Param("type") Integer type);

    /** 浏览 +1 */
    @Select("UPDATE post SET view_count = view_count + 1 WHERE post_id = #{postId}")
    int addView(@Param("postId") Long postId);
}

package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CommentMapper extends BaseMapper<Comment> {

    /** 某帖子下的评论(含昵称) */
    @Select("""
            SELECT co.comment_id, co.post_id, co.answer_id, co.user_id, co.content, co.create_time,
                   u.nickname AS author_name
            FROM comment co LEFT JOIN sys_user u ON u.user_id = co.user_id
            WHERE co.post_id = #{postId}
            ORDER BY co.create_time ASC
            """)
    List<Map<String, Object>> listByPost(@Param("postId") Long postId);
}

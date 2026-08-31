package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.Answer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface AnswerMapper extends BaseMapper<Answer> {

    /** 某帖子下的回答(含昵称),采纳优先 */
    @Select("""
            SELECT a.answer_id, a.post_id, a.user_id, a.content, a.is_accepted, a.like_count, a.create_time,
                   u.nickname AS author_name
            FROM answer a LEFT JOIN sys_user u ON u.user_id = a.user_id
            WHERE a.post_id = #{postId}
            ORDER BY a.is_accepted DESC, a.create_time ASC
            """)
    List<Map<String, Object>> listByPost(@Param("postId") Long postId);
}

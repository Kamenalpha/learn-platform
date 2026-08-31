package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论表
 */
@Data
@TableName("comment")
public class Comment {

    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;

    private Long postId;

    private Long answerId;

    private Long userId;

    private String content;

    private LocalDateTime createTime;
}

package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞表
 */
@Data
@TableName("post_like")
public class PostLike {

    @TableId(value = "like_id", type = IdType.AUTO)
    private Long likeId;

    private Long userId;

    private Long postId;

    private LocalDateTime createTime;
}

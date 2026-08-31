package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回答表(问题的最佳答案被采纳)
 */
@Data
@TableName("answer")
public class Answer {

    @TableId(value = "answer_id", type = IdType.AUTO)
    private Long answerId;

    private Long postId;

    private Long userId;

    private String content;

    /** 是否被采纳:0否 1是 */
    private Integer isAccepted;

    private Integer likeCount;

    private LocalDateTime createTime;
}

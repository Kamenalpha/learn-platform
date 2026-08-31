package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子表(帖子/文章/问题)
 */
@Data
@TableName("post")
public class Post {

    @TableId(value = "post_id", type = IdType.AUTO)
    private Long postId;

    /** 发帖人ID */
    private Long userId;

    private String title;

    private String content;

    /** 0帖子/文章 1问题 */
    private Integer type;

    private Long courseId;

    private Long kpId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    /** 是否AI生成:0否 1是 */
    private Integer isAi;

    /** 审核:0待审 1通过 2驳回 */
    private Integer auditStatus;

    private LocalDateTime createTime;
}

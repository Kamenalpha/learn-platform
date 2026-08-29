package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答记录表
 */
@Data
@TableName("qa_record")
public class QaRecord {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    private Long userId;

    private String sessionId;

    private String question;

    private String answer;

    /** 引用来源(JSON数组字符串) */
    private String reference;

    /** 0否 1收藏 */
    private Integer isFavorite;

    private Long elapsedMs;

    private LocalDateTime createTime;
}

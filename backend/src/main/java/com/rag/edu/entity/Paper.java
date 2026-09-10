package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷表
 */
@Data
@TableName("paper")
public class Paper {

    @TableId(value = "paper_id", type = IdType.AUTO)
    private Long paperId;

    private Long userId;

    private String title;

    /** 0教材块 1用户重点 2样卷 3变式训练 */
    private Integer sourceType;

    private Integer difficulty;

    private Integer totalScore;

    private Integer durationMin;

    private LocalDateTime createTime;
}

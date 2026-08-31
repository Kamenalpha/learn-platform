package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试/答题记录表
 */
@Data
@TableName("exam_record")
public class ExamRecord {

    @TableId(value = "exam_id", type = IdType.AUTO)
    private Long examId;

    private Long userId;

    private Long paperId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double score;

    /** 0进行中 1已交卷 2已截止 */
    private Integer status;

    private LocalDateTime createTime;
}

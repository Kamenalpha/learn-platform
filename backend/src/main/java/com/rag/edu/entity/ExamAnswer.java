package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 逐题作答表(含AI评分与评语)
 */
@Data
@TableName("exam_answer")
public class ExamAnswer {

    @TableId(value = "exam_answer_id", type = IdType.AUTO)
    private Long examAnswerId;

    private Long examId;

    private Long questionId;

    private String userAnswer;

    /** 客观题对错:0错 1对 NULL待评 */
    private Integer isCorrect;

    private Double aiScore;

    private String aiComment;

    private LocalDateTime createTime;
}

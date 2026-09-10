package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 学情观察表
 */
@Data
@TableName("ai_diagnosis")
public class AiDiagnosis {

    @TableId(value = "diagnosis_id", type = IdType.AUTO)
    private Long diagnosisId;

    private Long userId;

    private Long kpId;

    /** 来源:0 AI评分评语 1 问答误解(二期预留) */
    private Integer sourceType;

    private String content;

    /** 来源记录ID(exam_answer_id),唯一键防重 */
    private Long refId;

    private LocalDateTime createTime;
}

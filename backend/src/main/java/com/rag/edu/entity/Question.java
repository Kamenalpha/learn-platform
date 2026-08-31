package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目表
 */
@Data
@TableName("question")
public class Question {

    @TableId(value = "question_id", type = IdType.AUTO)
    private Long questionId;

    private Long ownerId;

    private Long resourceId;

    private Long kpId;

    /** 0单选 1多选 2判断 3填空 4简答 */
    private Integer qtype;

    private String stem;

    /** 选项(JSON) */
    private String options;

    private String answer;

    private String analysis;

    /** 1简单 2中等 3难 */
    private Integer difficulty;

    /** 0教材块 1用户重点 2样卷 */
    private Integer sourceType;

    private LocalDateTime createTime;
}

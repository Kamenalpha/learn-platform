package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 试卷-题目关联表
 */
@Data
@TableName("paper_question")
public class PaperQuestion {

    private Long paperId;

    private Long questionId;

    private Integer score;

    private Integer orderNum;
}

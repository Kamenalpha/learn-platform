package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学科表(四级分类顶层)
 */
@Data
@TableName("subject")
public class Subject {

    @TableId(value = "subject_id", type = IdType.AUTO)
    private Long subjectId;

    private String subjectName;

    private String description;

    private LocalDateTime createTime;
}

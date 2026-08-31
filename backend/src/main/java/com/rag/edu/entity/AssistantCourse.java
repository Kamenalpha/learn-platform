package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 助手-课程绑定表(知识隔离的依据)
 */
@Data
@TableName("assistant_course")
public class AssistantCourse {

    private Long assistantId;

    private Long courseId;
}

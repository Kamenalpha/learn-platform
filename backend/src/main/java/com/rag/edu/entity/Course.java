package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程表
 */
@Data
@TableName("course")
public class Course {

    @TableId(value = "course_id", type = IdType.AUTO)
    private Long courseId;

    private String courseName;

    private String description;

    private LocalDateTime createTime;
}

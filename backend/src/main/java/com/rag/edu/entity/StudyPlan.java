package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划表
 */
@Data
@TableName("study_plan")
public class StudyPlan {

    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;

    /** 所属用户ID */
    private Long userId;

    private String title;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 状态:0进行中 1已完成 2已取消 */
    private Integer status;

    private LocalDateTime createTime;
}

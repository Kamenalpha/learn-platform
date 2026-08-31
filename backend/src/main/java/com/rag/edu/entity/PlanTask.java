package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划任务表(可关联课程/章节/知识点)
 */
@Data
@TableName("plan_task")
public class PlanTask {

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    private Long planId;

    private Long courseId;

    private Long chapterId;

    private Long kpId;

    private String title;

    private LocalDate planDate;

    /** 是否完成:0否 1是 */
    private Integer done;

    /** 是否提醒:0否 1是 */
    private Integer reminder;

    private LocalDateTime createTime;
}

package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目辅导档案表
 */
@Data
@TableName("project_case")
public class ProjectCase {

    @TableId(value = "project_id", type = IdType.AUTO)
    private Long projectId;

    private Long userId;

    private String projectTitle;

    private String requirement;

    private String techSolution;

    /** 任务清单(JSON) */
    private String taskList;

    /** 阶段计划(JSON) */
    private String stagePlan;

    /** 文档/报告框架 */
    private String docContent;

    private LocalDateTime createTime;
}

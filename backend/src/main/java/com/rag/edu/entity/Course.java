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

    /** 所属学科ID */
    private Long subjectId;

    /** 创建人ID */
    private Long ownerId;

    private String courseName;

    private String description;

    /** 可见性:0私有 1公开(审核) 2分享 */
    private Integer visibility;

    /** 审核状态:0待审 1通过 2驳回 */
    private Integer auditStatus;

    private LocalDateTime createTime;
}

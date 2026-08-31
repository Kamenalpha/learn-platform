package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习行为日志表
 */
@Data
@TableName("study_log")
public class StudyLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    private Long userId;

    /** 0问答 1做题 2打卡 3浏览 4项目 */
    private Integer activityType;

    private Long courseId;

    private Integer durationSec;

    private LocalDateTime createTime;
}

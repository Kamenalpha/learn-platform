package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 用量月度记账表
 */
@Data
@TableName("quota_log")
public class QuotaLog {

    @TableId(value = "quota_id", type = IdType.AUTO)
    private Long quotaId;

    private Long userId;

    /** 动作:chat问答 generate出题 grade评分 project项目辅导 graph知识图谱 */
    private String actionType;

    /** 月份(yyyyMM) */
    private String month;

    private Integer useCount;

    private LocalDateTime createTime;
}

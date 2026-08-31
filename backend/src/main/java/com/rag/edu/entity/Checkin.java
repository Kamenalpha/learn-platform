package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡表
 */
@Data
@TableName("checkin")
public class Checkin {

    @TableId(value = "checkin_id", type = IdType.AUTO)
    private Long checkinId;

    private Long userId;

    private Long planId;

    private LocalDate checkinDate;

    private LocalDateTime createTime;
}

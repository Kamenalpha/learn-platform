package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本表
 */
@Data
@TableName("mistake")
public class Mistake {

    @TableId(value = "mistake_id", type = IdType.AUTO)
    private Long mistakeId;

    private Long userId;

    private Long questionId;

    private Integer wrongCount;

    private LocalDateTime lastWrongTime;

    /** 0未掌握 1已掌握 */
    private Integer mastered;
}

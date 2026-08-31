package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点表(四级分类叶子)
 */
@Data
@TableName("knowledge_point")
public class KnowledgePoint {

    @TableId(value = "kp_id", type = IdType.AUTO)
    private Long kpId;

    private Long chapterId;

    private String kpName;

    private String description;

    private Integer orderNum;

    private LocalDateTime createTime;
}

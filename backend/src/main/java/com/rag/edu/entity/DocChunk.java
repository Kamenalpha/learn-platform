package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分块表(向量库中同名原文的落库镜像,便于溯源与重建)
 */
@Data
@TableName("doc_chunk")
public class DocChunk {

    @TableId(value = "chunk_id", type = IdType.AUTO)
    private Long chunkId;

    private Long resourceId;

    private Integer chunkIndex;

    private String content;

    /** PDF页码/幻灯片页号,无页概念为 null */
    private Integer pageNum;

    /** 向量库ID: resourceId-chunkIndex */
    private String vectorId;

    private LocalDateTime createTime;
}

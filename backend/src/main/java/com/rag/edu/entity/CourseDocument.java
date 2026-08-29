package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程文档表
 */
@Data
@TableName("course_document")
public class CourseDocument {

    @TableId(value = "doc_id", type = IdType.AUTO)
    private Long docId;

    private Long courseId;

    private String docTitle;

    /** pdf / word / ppt / txt */
    private String fileType;

    /** 存储文件名(位于 rag.upload-dir 目录内) */
    private String fileUrl;

    private Long fileSize;

    private Integer chunkCount;

    /** 0未解析 1已解析 2失败 */
    private Integer parseStatus;

    private String failReason;

    private Long uploaderId;

    private LocalDateTime createTime;
}

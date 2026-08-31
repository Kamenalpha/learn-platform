package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料表(教材/课件/样卷等),映射 resource 表.
 */
@Data
@TableName("resource")
public class DocResource {

    @TableId(value = "resource_id", type = IdType.AUTO)
    private Long resourceId;

    /** 上传人ID */
    private Long userId;

    /** 归属课程ID */
    private Long courseId;

    /** 归属章节ID */
    private Long chapterId;

    /** 资料标题 */
    private String title;

    /** pdf / word / ppt / txt / image */
    private String fileType;

    /** 存储文件名(位于 rag.upload-dir 目录内) */
    private String fileUrl;

    private Long fileSize;

    private Integer chunkCount;

    /** 0未解析 1已解析 2失败 */
    private Integer parseStatus;

    private String failReason;

    /** 是否扫描件(需OCR):0否 1是 */
    private Integer isScanned;

    /** 可见性:0私有 1公开(审核) 2分享 */
    private Integer visibility;

    /** 审核状态:0待审 1通过 2驳回 */
    private Integer auditStatus;

    private LocalDateTime createTime;
}

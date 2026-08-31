package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节表(支持章->节两级)
 */
@Data
@TableName("chapter")
public class Chapter {

    @TableId(value = "chapter_id", type = IdType.AUTO)
    private Long chapterId;

    private Long courseId;

    /** 父章节ID(章下可再分节),顶层为NULL */
    private Long parentId;

    private String chapterName;

    private Integer orderNum;

    private LocalDateTime createTime;
}

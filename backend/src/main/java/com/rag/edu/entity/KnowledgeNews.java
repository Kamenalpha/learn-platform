package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识资讯表:系统每天早上 9:00 从公开资讯源(RSS)自动抓取,
 * 入库时强制标注来源(source_name/source_url),仅供学习导航,如若侵权可联系删除。
 */
@Data
@TableName("knowledge_news")
public class KnowledgeNews {

    @TableId(value = "news_id", type = IdType.AUTO)
    private Long newsId;

    private String title;

    /** 摘要(截断存储,仅作索引导航) */
    private String summary;

    /** 来源名称(版权标注,如 Solidot / 少数派) */
    private String sourceName;

    /** 原文链接(唯一,用于去重) */
    private String sourceUrl;

    /** 分类:综合/科技/商业科技/数字生活等(随资讯源配置) */
    private String category;

    /** 原文发布时间(解析失败为 null) */
    private LocalDateTime publishedAt;

    /** 本次抓取时间 */
    private LocalDateTime fetchedAt;

    private LocalDateTime createTime;
}

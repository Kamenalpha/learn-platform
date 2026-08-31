package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 助手配置表
 */
@Data
@TableName("assistant")
public class Assistant {

    @TableId(value = "assistant_id", type = IdType.AUTO)
    private Long assistantId;

    /** 创建人ID */
    private Long userId;

    private String name;

    private String avatar;

    /** 系统提示词 */
    private String systemPrompt;

    /** 模型: deepseek-chat / qwen-plus */
    private String model;

    /** 采样温度(0~1) */
    private Double temperature;

    /** 回答风格: concise/detailed/tutor */
    private String style;

    /** 多轮上下文轮数 */
    private Integer contextRounds;

    /** 是否带引用:0否 1是 */
    private Integer withReference;

    /** 知识范围:0整库 1指定章节 */
    private Integer scopeType;

    /** 状态:1启用 0停用 */
    private Integer status;

    private LocalDateTime createTime;
}

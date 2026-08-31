package com.rag.edu.dto;

import java.util.List;

/**
 * AI 助手模块 DTO
 */
public class AssistantDtos {

    /** 保存/更新助手(含绑定课程) */
    public record AssistantSaveReq(
            Long assistantId,
            String name,
            String avatar,
            String systemPrompt,
            String model,
            Double temperature,
            String style,
            Integer contextRounds,
            Integer withReference,
            Integer scopeType,
            List<Long> courseIds) {
    }

    /** 助手视图(含绑定课程id) */
    public record AssistantVO(
            Long assistantId,
            String name,
            String avatar,
            String systemPrompt,
            String model,
            Double temperature,
            String style,
            Integer contextRounds,
            Integer withReference,
            Integer scopeType,
            List<Long> courseIds) {
    }
}

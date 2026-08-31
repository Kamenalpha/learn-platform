package com.rag.edu.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 智能问答模块 DTO
 */
public class ChatDtos {

    public record AskReq(
            @NotBlank(message = "会话ID不能为空") String sessionId,
            @NotBlank(message = "问题不能为空") String question,
            Long assistantId) {
    }

    /** 引用来源:可溯源到文档与页码 */
    public record Source(Long docId, String docTitle, Integer page, Long chunkId, Double score, String snippet) {
    }

    public record AskResp(Long recordId, String answer, List<Source> references, long elapsedMs) {
    }

    public record SessionVO(String sessionId, String lastQuestion, String lastTime, Long msgCount) {
    }
}

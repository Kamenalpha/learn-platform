package com.rag.edu.dto;

/**
 * 社区模块 DTO
 */
public class CommunityDtos {

    /** 发帖/提问 */
    public record CreatReq(String title, String content, Integer type, Long courseId, Long kpId) {
    }

    /** 回答/评论内容 */
    public record ReplyReq(String content, Long answerId) {
    }
}

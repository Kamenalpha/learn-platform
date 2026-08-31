package com.rag.edu.dto;

/**
 * 项目辅导模块 DTO
 */
public class ProjectDtos {

    /** 新建项目:题目 + 需求 */
    public record CreateReq(String projectTitle, String requirement) {
    }
}

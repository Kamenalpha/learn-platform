package com.rag.edu.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 知识库管理 / 学习辅助模块 DTO
 */
public class KbDtos {

    /** 检索参数配置(管理后台可调,存于 Redis) */
    public record KbConfig(Integer topK, Double similarityThreshold, Integer chunkSize,
                           Integer chunkOverlap, String promptSuffix) {
    }

    /** 知识图谱数据 */
    public record GraphData(List<GraphNode> nodes, List<GraphEdge> edges) {
    }

    public record GraphNode(String id, String name, int value) {
    }

    public record GraphEdge(String source, String target, int weight) {
    }

    /** 考点生成请求 */
    public record ExamReq(@NotNull(message = "课程不能为空") Long courseId, String chapter) {
    }
}

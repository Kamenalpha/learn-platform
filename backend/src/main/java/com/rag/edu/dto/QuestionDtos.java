package com.rag.edu.dto;

import java.util.List;

/**
 * 出题模拟模块 DTO
 */
public class QuestionDtos {

    /** 出题请求:出处/题型/题量/难度 */
    public record GenerateReq(
            Long courseId,
            Long resourceId,
            Integer sourceType,   // 0教材块 1用户重点 2样卷
            Integer count,
            Integer difficulty,
            List<Integer> qtypes) {
    }

    public record QuestionVO(
            Long questionId, Integer qtype, String stem, List<String> options,
            Integer difficulty, Integer sourceType) {
    }

    /** 出题结构化输出:由框架的 JSON Schema 约束生成,替代手写解析 */
    public record GeneratedQuestion(
            Integer qtype,         // 0单选 1多选 2判断 3填空 4简答
            String stem,
            List<String> options,  // 选择题选项;非选择题为 null
            String answer,
            String analysis,
            Integer difficulty) {
    }

    /** 主观题 AI 评分结构化输出 */
    public record AiGradeResult(double score, String comment) {
    }

    /** 某道题作答 */
    public record GradeItem(Long questionId, String userAnswer) {
    }

    /** 交卷请求 */
    public record GradeReq(Long examId, List<GradeItem> answers) {
    }
}

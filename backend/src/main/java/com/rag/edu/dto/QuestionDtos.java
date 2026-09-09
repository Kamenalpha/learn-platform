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

    /** 某道题作答 */
    public record GradeItem(Long questionId, String userAnswer) {
    }

    /** 交卷请求 */
    public record GradeReq(Long examId, List<GradeItem> answers) {
    }
}

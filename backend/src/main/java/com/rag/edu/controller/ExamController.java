package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.dto.QuestionDtos.GenerateReq;
import com.rag.edu.dto.QuestionDtos.GradeReq;
import com.rag.edu.service.ExamPracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 出题模拟接口:自动出题 / 试卷 / 交卷判分 / 错题本.
 */
@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamPracticeService examPracticeService;

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody GenerateReq req) {
        return Result.ok(examPracticeService.generate(UserContext.userId(), req));
    }

    @GetMapping("/paper/{paperId}")
    public Result<Map<String, Object>> paperDetail(@PathVariable Long paperId) {
        return Result.ok(examPracticeService.paperDetail(paperId, UserContext.userId()));
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody GradeReq req) {
        return Result.ok(examPracticeService.grade(UserContext.userId(), req));
    }

    @GetMapping("/exams")
    public Result<List<Map<String, Object>>> exams() {
        return Result.ok(examPracticeService.listExams(UserContext.userId()));
    }

    @GetMapping("/mistakes")
    public Result<List<Map<String, Object>>> mistakes() {
        return Result.ok(examPracticeService.mistakes(UserContext.userId()));
    }

    @PostMapping("/mistakes/{mistakeId}/master")
    public Result<Void> master(@PathVariable Long mistakeId) {
        examPracticeService.masterMistake(UserContext.userId(), mistakeId);
        return Result.ok();
    }
}

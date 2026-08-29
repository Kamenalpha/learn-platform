package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.dto.KbDtos.ExamReq;
import com.rag.edu.dto.KbDtos.GraphData;
import com.rag.edu.service.rag.ExamService;
import com.rag.edu.service.rag.GraphService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学习辅助接口:考点生成 / 知识图谱
 */
@RestController
@RequestMapping("/api/assist")
@RequiredArgsConstructor
public class AssistController {

    private final ExamService examService;
    private final GraphService graphService;

    /** 知识图谱(courseId 必填;refresh=true 强制重建缓存) */
    @GetMapping("/graph")
    public Result<GraphData> graph(@RequestParam Long courseId,
                                   @RequestParam(defaultValue = "false") boolean refresh) {
        return Result.ok(graphService.build(courseId, refresh));
    }

    /** 考点自动生成:知识点梳理 + 练习题 */
    @PostMapping("/exam")
    public Result<String> exam(@Valid @RequestBody ExamReq req) {
        return Result.ok(examService.generate(req.courseId(), req.chapter()));
    }
}

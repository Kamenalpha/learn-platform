package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 学习画像接口.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(analyticsService.overview(UserContext.userId()));
    }

    /** AI 诊断:最近的 AI 学情观察列表 */
    @GetMapping("/diagnoses")
    public Result<List<Map<String, Object>>> diagnoses() {
        return Result.ok(analyticsService.diagnoses(UserContext.userId()));
    }
}

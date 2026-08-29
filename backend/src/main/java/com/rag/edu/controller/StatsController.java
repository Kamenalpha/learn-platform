package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统运行监控(管理员)
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    /** 近N天每日提问量 */
    @GetMapping("/qa-trend")
    public Result<List<Map<String, Object>>> qaTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.qaTrend(days));
    }

    @GetMapping("/docs-by-course")
    public Result<List<Map<String, Object>>> docsByCourse() {
        return Result.ok(statsService.docsByCourse());
    }

    /** 问答日志(分页) */
    @GetMapping("/qa-logs")
    public Result<Map<String, Object>> qaLogs(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(statsService.qaLogs(page, size));
    }
}

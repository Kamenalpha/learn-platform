package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端资讯接口:/api/admin/** 由 AuthInterceptor 强制管理员角色。
 */
@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;

    /** 手动触发一次资讯抓取(默认每天 9:00 自动执行) */
    @PostMapping("/fetch")
    public Result<Map<String, Object>> fetch() {
        return Result.ok(Map.of("added", newsService.fetchAndSave()));
    }
}

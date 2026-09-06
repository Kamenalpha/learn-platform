package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.entity.KnowledgeNews;
import com.rag.edu.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 知识资讯接口:/news/list 与 /news/detail 均在 WebConfig 白名单中,游客(未登录)可访问。
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String category) {
        return Result.ok(newsService.page(category, page, size));
    }

    /** 资讯详情:含抽取入库的正文(标明出处);游客可读 */
    @GetMapping("/detail/{newsId}")
    public Result<KnowledgeNews> detail(@PathVariable Long newsId) {
        return Result.ok(newsService.detail(newsId));
    }
}

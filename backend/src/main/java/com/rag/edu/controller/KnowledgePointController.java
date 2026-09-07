package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.KnowledgePoint;
import com.rag.edu.service.KnowledgePointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识点接口:按章节查询,增删改校验章节归属.
 */
@RestController
@RequestMapping("/api/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @GetMapping("/chapter/{chapterId}")
    public Result<List<KnowledgePoint>> listByChapter(@PathVariable Long chapterId) {
        return Result.ok(knowledgePointService.listByChapter(chapterId, UserContext.userId()));
    }

    @PostMapping
    public Result<Void> add(@RequestBody KnowledgePoint knowledgePoint) {
        knowledgePointService.save(knowledgePoint, UserContext.userId());
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody KnowledgePoint knowledgePoint) {
        knowledgePointService.save(knowledgePoint, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/{kpId}")
    public Result<Void> delete(@PathVariable Long kpId) {
        knowledgePointService.delete(kpId, UserContext.userId());
        return Result.ok();
    }
}

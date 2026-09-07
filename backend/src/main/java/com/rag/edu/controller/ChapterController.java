package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.Chapter;
import com.rag.edu.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 章节接口:按课程查询,增删改校验课程归属.
 */
@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/course/{courseId}")
    public Result<List<Chapter>> listByCourse(@PathVariable Long courseId) {
        return Result.ok(chapterService.listByCourse(courseId, UserContext.userId()));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Chapter chapter) {
        chapterService.save(chapter, UserContext.userId());
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Chapter chapter) {
        chapterService.save(chapter, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/{chapterId}")
    public Result<Void> delete(@PathVariable Long chapterId) {
        chapterService.delete(chapterId, UserContext.userId());
        return Result.ok();
    }
}

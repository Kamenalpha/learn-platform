package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.Subject;
import com.rag.edu.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学科(分类顶层)接口:列表登录可见,增删改仅管理员.
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public Result<List<Subject>> list() {
        return Result.ok(subjectService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Subject subject) {
        UserContext.requireAdmin();
        subjectService.save(subject);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Subject subject) {
        UserContext.requireAdmin();
        subjectService.save(subject);
        return Result.ok();
    }

    @DeleteMapping("/{subjectId}")
    public Result<Void> delete(@PathVariable Long subjectId) {
        UserContext.requireAdmin();
        subjectService.delete(subjectId);
        return Result.ok();
    }
}

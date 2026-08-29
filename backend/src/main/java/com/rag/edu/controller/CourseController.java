package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.Course;
import com.rag.edu.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程(知识库分类)接口:浏览开放给所有用户,写操作仅管理员
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(courseService.listWithDocCount());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Course course) {
        UserContext.requireAdmin();
        courseService.save(course);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Course course) {
        UserContext.requireAdmin();
        courseService.save(course);
        return Result.ok();
    }

    @DeleteMapping("/{courseId}")
    public Result<Void> delete(@PathVariable Long courseId) {
        UserContext.requireAdmin();
        courseService.delete(courseId);
        return Result.ok();
    }
}

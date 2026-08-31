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
 * 课程(分类体系第二层)接口:列表对"我的 + 公开审核通过"开放,写操作按当前用户归属.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(courseService.listVisible(UserContext.userId()));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Course course) {
        courseService.save(course, UserContext.userId());
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Course course) {
        courseService.save(course, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/{courseId}")
    public Result<Void> delete(@PathVariable Long courseId) {
        courseService.delete(courseId, UserContext.userId());
        return Result.ok();
    }
}

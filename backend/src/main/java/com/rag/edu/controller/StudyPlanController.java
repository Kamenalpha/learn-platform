package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.PlanTask;
import com.rag.edu.entity.StudyPlan;
import com.rag.edu.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习计划接口:计划 CRUD + 任务维护(归属当前用户).
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping
    public Result<List<StudyPlan>> list() {
        return Result.ok(studyPlanService.listMine(UserContext.userId()));
    }

    @GetMapping("/{planId}")
    public Result<java.util.Map<String, Object>> detail(@PathVariable Long planId) {
        return Result.ok(studyPlanService.detail(planId));
    }

    @PostMapping
    public Result<StudyPlan> add(@RequestBody StudyPlan plan) {
        return Result.ok(studyPlanService.save(plan, UserContext.userId()));
    }

    @PutMapping
    public Result<StudyPlan> update(@RequestBody StudyPlan plan) {
        return Result.ok(studyPlanService.save(plan, UserContext.userId()));
    }

    @DeleteMapping("/{planId}")
    public Result<Void> delete(@PathVariable Long planId) {
        studyPlanService.delete(planId, UserContext.userId());
        return Result.ok();
    }

    @PostMapping("/{planId}/tasks")
    public Result<PlanTask> addTask(@PathVariable Long planId, @RequestBody PlanTask task) {
        task.setPlanId(planId);
        return Result.ok(studyPlanService.addTask(task, UserContext.userId()));
    }

    @PutMapping("/tasks")
    public Result<Void> updateTask(@RequestBody PlanTask task) {
        studyPlanService.updateTask(task, UserContext.userId());
        return Result.ok();
    }

    @PostMapping("/tasks/{taskId}/toggle")
    public Result<PlanTask> toggleTask(@PathVariable Long taskId) {
        return Result.ok(studyPlanService.toggleTask(taskId, UserContext.userId()));
    }

    @DeleteMapping("/tasks/{taskId}")
    public Result<Void> deleteTask(@PathVariable Long taskId) {
        studyPlanService.deleteTask(taskId, UserContext.userId());
        return Result.ok();
    }
}

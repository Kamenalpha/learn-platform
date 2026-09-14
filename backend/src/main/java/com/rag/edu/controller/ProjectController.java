package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.dto.ProjectDtos.CreateReq;
import com.rag.edu.entity.ProjectCase;
import com.rag.edu.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目辅导接口.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public Result<List<ProjectCase>> list() {
        return Result.ok(projectService.listMine(UserContext.userId()));
    }

    @GetMapping("/{projectId}")
    public Result<ProjectCase> detail(@PathVariable Long projectId) {
        return Result.ok(projectService.detail(projectId, UserContext.userId()));
    }

    @PostMapping
    public Result<ProjectCase> create(@RequestBody CreateReq req) {
        return Result.ok(projectService.create(UserContext.userId(), req));
    }

    @DeleteMapping("/{projectId}")
    public Result<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId, UserContext.userId());
        return Result.ok();
    }
}

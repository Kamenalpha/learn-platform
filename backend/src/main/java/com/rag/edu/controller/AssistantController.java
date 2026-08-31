package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.dto.AssistantDtos.AssistantSaveReq;
import com.rag.edu.dto.AssistantDtos.AssistantVO;
import com.rag.edu.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 助手接口:定制 / 管理(归属当前用户).
 */
@RestController
@RequestMapping("/api/assistants")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @GetMapping
    public Result<List<AssistantVO>> list() {
        return Result.ok(assistantService.listMine(UserContext.userId()));
    }

    @GetMapping("/{assistantId}")
    public Result<AssistantVO> detail(@PathVariable Long assistantId) {
        return Result.ok(assistantService.getDetail(assistantId));
    }

    @PostMapping
    public Result<AssistantVO> add(@RequestBody AssistantSaveReq req) {
        return Result.ok(assistantService.save(req, UserContext.userId()));
    }

    @PutMapping
    public Result<AssistantVO> update(@RequestBody AssistantSaveReq req) {
        return Result.ok(assistantService.save(req, UserContext.userId()));
    }

    @DeleteMapping("/{assistantId}")
    public Result<Void> delete(@PathVariable Long assistantId) {
        assistantService.delete(assistantId, UserContext.userId());
        return Result.ok();
    }
}

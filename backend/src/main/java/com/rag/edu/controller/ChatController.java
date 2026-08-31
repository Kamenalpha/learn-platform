package com.rag.edu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.dto.AssistantDtos.AssistantVO;
import com.rag.edu.dto.ChatDtos.AskReq;
import com.rag.edu.dto.ChatDtos.AskResp;
import com.rag.edu.entity.QaRecord;
import com.rag.edu.mapper.QaRecordMapper;
import com.rag.edu.service.AssistantService;
import com.rag.edu.service.rag.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能问答接口:提问 / 会话列表 / 历史记录 / 收藏
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final QaRecordMapper qaRecordMapper;
    private final AssistantService assistantService;

    /** 提问(RAG 检索 + 大模型生成;可选指定助手,限定其绑定课程) */
    @PostMapping("/ask")
    public Result<AskResp> ask(@Valid @RequestBody AskReq req) {
        List<Long> courseIds = null;
        String prompt = null;
        if (req.assistantId() != null) {
            AssistantVO a = assistantService.getDetail(req.assistantId());
            courseIds = a.courseIds();
            prompt = a.systemPrompt();
        }
        return Result.ok(chatService.ask(UserContext.userId(), req.sessionId(), req.question(), courseIds, prompt));
    }

    /** 当前用户的会话列表 */
    @GetMapping("/sessions")
    public Result<List> sessions() {
        return Result.ok(qaRecordMapper.listSessions(UserContext.userId()));
    }

    /** 当前用户的全部问答记录(学习历史,最新在前) */
    @GetMapping("/records")
    public Result<List<QaRecord>> records() {
        return Result.ok(qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, UserContext.userId())
                .orderByDesc(QaRecord::getCreateTime)
                .last("LIMIT 200")));
    }

    /** 某会话的问答记录 */
    @GetMapping("/history")
    public Result<List<QaRecord>> history(@RequestParam String sessionId) {
        return Result.ok(qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, UserContext.userId())
                .eq(QaRecord::getSessionId, sessionId)
                .orderByAsc(QaRecord::getCreateTime)));
    }

    /** 收藏列表 */
    @GetMapping("/favorites")
    public Result<List<QaRecord>> favorites() {
        return Result.ok(qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, UserContext.userId())
                .eq(QaRecord::getIsFavorite, 1)
                .orderByDesc(QaRecord::getCreateTime)));
    }

    /** 切换收藏状态 */
    @PostMapping("/{recordId}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Long recordId) {
        QaRecord record = qaRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(UserContext.userId())) {
            return Result.error(404, "记录不存在");
        }
        int next = record.getIsFavorite() != null && record.getIsFavorite() == 1 ? 0 : 1;
        record.setIsFavorite(next);
        qaRecordMapper.updateById(record);
        return Result.ok(next == 1);
    }
}

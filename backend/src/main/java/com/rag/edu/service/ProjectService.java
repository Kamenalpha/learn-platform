package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.ProjectDtos.CreateReq;
import com.rag.edu.entity.ProjectCase;
import com.rag.edu.mapper.ProjectCaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目辅导:AI 全流程陪跑——需求拆解 -> 技术方案 -> 任务清单 -> 阶段计划 -> 报告框架.
 */
@Slf4j
@Service
public class ProjectService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ProjectCaseMapper projectCaseMapper;

    public ProjectService(ChatModel chatModel, ObjectMapper objectMapper, ProjectCaseMapper projectCaseMapper) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
        this.projectCaseMapper = projectCaseMapper;
    }

    public ProjectCase create(Long userId, CreateReq req) {
        String prompt = """
                你是课程设计项目导师。请根据下面的项目题目与需求,给出完整辅导方案,严格返回 JSON(不要其它文字):
                {"techSolution":"技术方案说明","taskList":["任务1","任务2",...],"stagePlan":["阶段1:...","阶段2:...",...],"docContent":"项目报告/文档大纲(含背景/需求/设计/实现/测试/总结)"}
                项目题目:%s
                需求:%s
                """.formatted(req.projectTitle(), req.requirement() == null ? "" : req.requirement());

        ProjectCase pc = new ProjectCase();
        pc.setUserId(userId);
        pc.setProjectTitle(req.projectTitle());
        pc.setRequirement(req.requirement());
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(extractJson(raw));
            pc.setTechSolution(node.path("techSolution").asText(""));
            pc.setTaskList(node.path("taskList").toString());
            pc.setStagePlan(node.path("stagePlan").toString());
            pc.setDocContent(node.path("docContent").asText(""));
        } catch (Exception e) {
            log.warn("项目AI拆解失败,降级为仅保存需求: {}", e.getMessage());
            pc.setTechSolution("AI 生成失败,可根据需求自行补充");
            pc.setTaskList("[]");
            pc.setStagePlan("[]");
            pc.setDocContent("");
        }
        projectCaseMapper.insert(pc);
        return pc;
    }

    public List<ProjectCase> listMine(Long userId) {
        return projectCaseMapper.selectList(new LambdaQueryWrapper<ProjectCase>()
                .eq(ProjectCase::getUserId, userId).orderByDesc(ProjectCase::getCreateTime));
    }

    public ProjectCase detail(Long projectId) {
        ProjectCase pc = projectCaseMapper.selectById(projectId);
        if (pc == null) {
            throw new BizException(404, "项目不存在");
        }
        return pc;
    }

    public void delete(Long projectId, Long userId) {
        ProjectCase pc = projectCaseMapper.selectById(projectId);
        if (pc == null) {
            return;
        }
        if (!pc.getUserId().equals(userId)) {
            throw new BizException(403, "无权删除");
        }
        projectCaseMapper.deleteById(projectId);
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start < 0 || end < start ? "{}" : raw.substring(start, end + 1);
    }
}

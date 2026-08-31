package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.AssistantDtos.AssistantSaveReq;
import com.rag.edu.dto.AssistantDtos.AssistantVO;
import com.rag.edu.entity.Assistant;
import com.rag.edu.entity.AssistantCourse;
import com.rag.edu.mapper.AssistantCourseMapper;
import com.rag.edu.mapper.AssistantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 助手配置管理:支持为不同课程定制助手,并以绑定课程实现知识隔离.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AssistantMapper assistantMapper;
    private final AssistantCourseMapper assistantCourseMapper;

    public List<AssistantVO> listMine(Long userId) {
        List<Assistant> list = assistantMapper.selectList(new LambdaQueryWrapper<Assistant>()
                .eq(Assistant::getUserId, userId)
                .orderByDesc(Assistant::getCreateTime));
        return list.stream().map(this::toVO).toList();
    }

    public AssistantVO getDetail(Long assistantId) {
        Assistant a = assistantMapper.selectById(assistantId);
        if (a == null) {
            throw new BizException(404, "助手不存在");
        }
        return toVO(a);
    }

    @Transactional
    public AssistantVO save(AssistantSaveReq req, Long userId) {
        Assistant a = new Assistant();
        a.setAssistantId(req.assistantId());
        a.setUserId(userId);
        a.setName(req.name());
        a.setAvatar(req.avatar() == null ? "" : req.avatar());
        a.setSystemPrompt(req.systemPrompt());
        a.setModel(req.model());
        a.setTemperature(req.temperature());
        a.setStyle(req.style() == null ? "default" : req.style());
        a.setContextRounds(req.contextRounds() == null ? 3 : req.contextRounds());
        a.setWithReference(req.withReference() == null ? 1 : req.withReference());
        a.setScopeType(req.scopeType() == null ? 0 : req.scopeType());
        a.setStatus(1);
        if (a.getAssistantId() == null) {
            assistantMapper.insert(a);
        } else {
            Assistant db = assistantMapper.selectById(a.getAssistantId());
            if (db == null || !db.getUserId().equals(userId)) {
                throw new BizException(403, "无权修改该助手");
            }
            assistantMapper.updateById(a);
        }
        // 同步绑定课程(删除旧绑定 + 重新插入)
        assistantCourseMapper.delete(new LambdaQueryWrapper<AssistantCourse>()
                .eq(AssistantCourse::getAssistantId, a.getAssistantId()));
        if (req.courseIds() != null) {
            for (Long courseId : req.courseIds()) {
                AssistantCourse ac = new AssistantCourse();
                ac.setAssistantId(a.getAssistantId());
                ac.setCourseId(courseId);
                assistantCourseMapper.insert(ac);
            }
        }
        return toVO(assistantMapper.selectById(a.getAssistantId()));
    }

    public void delete(Long assistantId, Long userId) {
        Assistant db = assistantMapper.selectById(assistantId);
        if (db == null || !db.getUserId().equals(userId)) {
            throw new BizException(403, "无权删除该助手");
        }
        assistantCourseMapper.delete(new LambdaQueryWrapper<AssistantCourse>()
                .eq(AssistantCourse::getAssistantId, assistantId));
        assistantMapper.deleteById(assistantId);
    }

    private AssistantVO toVO(Assistant a) {
        List<Long> courseIds = assistantCourseMapper.selectList(new LambdaQueryWrapper<AssistantCourse>()
                        .eq(AssistantCourse::getAssistantId, a.getAssistantId()))
                .stream().map(AssistantCourse::getCourseId).toList();
        return new AssistantVO(a.getAssistantId(), a.getName(), a.getAvatar(), a.getSystemPrompt(),
                a.getModel(), a.getTemperature(), a.getStyle(), a.getContextRounds(), a.getWithReference(),
                a.getScopeType(), courseIds);
    }
}

package com.rag.edu.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.ProjectCase;
import com.rag.edu.mapper.ProjectCaseMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目辅导服务的归属校验测试:detail 必须校验归属,防止越权读取他人项目.
 */
class ProjectServiceTest {

    private final ProjectCaseMapper projectCaseMapper = mock(ProjectCaseMapper.class);
    private final ProjectService service = new ProjectService(
            mock(ChatModel.class), new ObjectMapper(), projectCaseMapper,
            mock(QuotaService.class));

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectCase.class);
    }

    @Test
    void detailOwnerReadable() {
        ProjectCase pc = caseOf(10L, 1L);
        when(projectCaseMapper.selectById(10L)).thenReturn(pc);

        assertEquals(pc, service.detail(10L, 1L));
    }

    @Test
    void detailNonOwnerDenied() {
        when(projectCaseMapper.selectById(10L)).thenReturn(caseOf(10L, 1L));

        BizException denied = assertThrows(BizException.class, () -> service.detail(10L, 2L));
        assertEquals(403, denied.getCode());
        assertEquals("无权查看该项目", denied.getMessage());
    }

    @Test
    void detailMissingThrows404() {
        when(projectCaseMapper.selectById(404L)).thenReturn(null);

        BizException denied = assertThrows(BizException.class, () -> service.detail(404L, 1L));
        assertEquals(404, denied.getCode());
    }

    @Test
    void listMineScopedToCaller() {
        service.listMine(1L);

        org.mockito.ArgumentCaptor<LambdaQueryWrapper<ProjectCase>> captor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(projectCaseMapper).selectList(captor.capture());
        LambdaQueryWrapper<ProjectCase> wrapper = captor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(1L));
    }

    @Test
    void deleteNonOwnerDenied() {
        when(projectCaseMapper.selectById(10L)).thenReturn(caseOf(10L, 1L));

        BizException denied = assertThrows(BizException.class, () -> service.delete(10L, 2L));
        assertEquals(403, denied.getCode());
        verify(projectCaseMapper, never()).deleteById(anyLong());
        verify(projectCaseMapper, never()).deleteById(eq(10L));
    }

    private static ProjectCase caseOf(Long id, Long userId) {
        ProjectCase pc = new ProjectCase();
        pc.setProjectId(id);
        pc.setUserId(userId);
        return pc;
    }
}

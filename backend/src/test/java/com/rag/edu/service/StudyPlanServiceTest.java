package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.PlanTask;
import com.rag.edu.entity.StudyPlan;
import com.rag.edu.mapper.PlanTaskMapper;
import com.rag.edu.mapper.StudyPlanMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学习计划服务的归属校验测试:detail/addTask/delete 必须校验归属,防止越权.
 */
class StudyPlanServiceTest {

    private final StudyPlanMapper planMapper = mock(StudyPlanMapper.class);
    private final PlanTaskMapper taskMapper = mock(PlanTaskMapper.class);
    private final StudyPlanService service =
            new StudyPlanService(planMapper, taskMapper, mock(StudyLogService.class));

    @Test
    void detailOwnerReadable() {
        StudyPlan plan = planOf(10L, 1L);
        when(planMapper.selectById(10L)).thenReturn(plan);
        when(taskMapper.selectList(anyWrapper())).thenReturn(List.of(new PlanTask()));

        Map<String, Object> detail = service.detail(10L, 1L);

        assertSame(plan, detail.get("plan"));
        assertEquals(1, ((List<?>) detail.get("tasks")).size());
    }

    @Test
    void detailNonOwnerDenied() {
        when(planMapper.selectById(10L)).thenReturn(planOf(10L, 1L));

        BizException denied = assertThrows(BizException.class, () -> service.detail(10L, 2L));
        assertEquals(403, denied.getCode());
        verify(taskMapper, never()).selectList(anyWrapper());
    }

    @Test
    void addTaskNonOwnerDenied() {
        when(planMapper.selectById(10L)).thenReturn(planOf(10L, 1L));
        PlanTask task = new PlanTask();
        task.setPlanId(10L);

        BizException denied = assertThrows(BizException.class, () -> service.addTask(task, 2L));
        assertEquals(403, denied.getCode());
        verify(taskMapper, never()).insert(task);
    }

    @Test
    void deleteNonOwnerDenied() {
        when(planMapper.selectById(10L)).thenReturn(planOf(10L, 1L));

        BizException denied = assertThrows(BizException.class, () -> service.delete(10L, 2L));
        assertEquals(403, denied.getCode());
        verify(taskMapper, never()).delete(anyWrapper());
        verify(planMapper, never()).deleteById(10L);
    }

    @SuppressWarnings("unchecked")
    private static LambdaQueryWrapper<PlanTask> anyWrapper() {
        return org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class);
    }

    private static StudyPlan planOf(Long id, Long userId) {
        StudyPlan plan = new StudyPlan();
        plan.setPlanId(id);
        plan.setUserId(userId);
        return plan;
    }
}

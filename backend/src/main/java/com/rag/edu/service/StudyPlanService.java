package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.PlanTask;
import com.rag.edu.entity.StudyPlan;
import com.rag.edu.mapper.PlanTaskMapper;
import com.rag.edu.mapper.StudyPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学习计划管理:计划 + 任务(可关联课程/章节/知识点,支持打卡与提醒).
 */
@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanMapper planMapper;
    private final PlanTaskMapper taskMapper;
    private final StudyLogService studyLogService;

    public List<StudyPlan> listMine(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .orderByDesc(StudyPlan::getCreateTime));
    }

    public java.util.Map<String, Object> detail(Long planId, Long userId) {
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(404, "计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BizException(403, "无权查看该计划");
        }
        List<PlanTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<PlanTask>()
                .eq(PlanTask::getPlanId, planId).orderByAsc(PlanTask::getPlanDate));
        return java.util.Map.of("plan", plan, "tasks", tasks);
    }

    public StudyPlan save(StudyPlan plan, Long userId) {
        plan.setUserId(userId);
        if (plan.getStatus() == null) {
            plan.setStatus(0);
        }
        if (plan.getPlanId() == null) {
            planMapper.insert(plan);
        } else {
            StudyPlan db = planMapper.selectById(plan.getPlanId());
            if (db == null || !db.getUserId().equals(userId)) {
                throw new BizException(403, "无权修改该计划");
            }
            planMapper.updateById(plan);
        }
        return plan;
    }

    public void delete(Long planId, Long userId) {
        StudyPlan db = planMapper.selectById(planId);
        if (db == null || !db.getUserId().equals(userId)) {
            throw new BizException(403, "无权删除该计划");
        }
        taskMapper.delete(new LambdaQueryWrapper<PlanTask>().eq(PlanTask::getPlanId, planId));
        planMapper.deleteById(planId);
    }

    public PlanTask addTask(PlanTask task, Long userId) {
        verifyPlanOwner(task.getPlanId(), userId);
        if (task.getDone() == null) {
            task.setDone(0);
        }
        taskMapper.insert(task);
        return task;
    }

    public void updateTask(PlanTask task, Long userId) {
        PlanTask db = taskMapper.selectById(task.getTaskId());
        if (db == null) {
            throw new BizException(404, "任务不存在");
        }
        verifyPlanOwner(db.getPlanId(), userId);
        taskMapper.updateById(task);
    }

    public PlanTask toggleTask(Long taskId, Long userId) {
        PlanTask db = taskMapper.selectById(taskId);
        if (db == null) {
            throw new BizException(404, "任务不存在");
        }
        verifyPlanOwner(db.getPlanId(), userId);
        db.setDone(db.getDone() != null && db.getDone() == 1 ? 0 : 1);
        taskMapper.updateById(db);
        if (db.getDone() == 1) {
            studyLogService.checkin(userId, db.getPlanId());
            studyLogService.logStudy(userId, 2, db.getCourseId(), 0);
        }
        return db;
    }

    public void deleteTask(Long taskId, Long userId) {
        PlanTask db = taskMapper.selectById(taskId);
        if (db == null) {
            return;
        }
        verifyPlanOwner(db.getPlanId(), userId);
        taskMapper.deleteById(taskId);
    }

    private void verifyPlanOwner(Long planId, Long userId) {
        StudyPlan db = planMapper.selectById(planId);
        if (db == null || !db.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作该计划的任务");
        }
    }
}

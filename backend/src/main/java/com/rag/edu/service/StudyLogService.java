package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.entity.Checkin;
import com.rag.edu.entity.StudyLog;
import com.rag.edu.mapper.CheckinMapper;
import com.rag.edu.mapper.StudyLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 学习行为埋点:记录学习日志与每日打卡(供学习画像统计).
 */
@Service
@RequiredArgsConstructor
public class StudyLogService {

    private final StudyLogMapper studyLogMapper;
    private final CheckinMapper checkinMapper;

    /** 记录学习行为(0问答 1做题 2打卡 3浏览 4项目) */
    public void logStudy(Long userId, Integer activityType, Long courseId, Integer durationSec) {
        StudyLog log = new StudyLog();
        log.setUserId(userId);
        log.setActivityType(activityType);
        log.setCourseId(courseId);
        log.setDurationSec(durationSec == null ? 0 : durationSec);
        studyLogMapper.insert(log);
    }

    /** 每日打卡(同一天只记一次) */
    public void checkin(Long userId, Long planId) {
        LocalDate today = LocalDate.now();
        Long exists = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId).eq(Checkin::getCheckinDate, today));
        if (exists > 0) {
            return;
        }
        Checkin c = new Checkin();
        c.setUserId(userId);
        c.setPlanId(planId);
        c.setCheckinDate(today);
        checkinMapper.insert(c);
    }
}

package com.rag.edu.service;

import com.rag.edu.mapper.AnalyticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习画像:学习时长/连续打卡/掌握度/弱项等汇总.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsMapper analyticsMapper;

    public Map<String, Object> overview(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalSeconds", analyticsMapper.totalSeconds(userId));
        data.put("todaySeconds", analyticsMapper.todaySeconds(userId));
        data.put("chatCount", analyticsMapper.chatCount(userId));
        data.put("examCount", analyticsMapper.examCount(userId));
        data.put("mistakeCount", analyticsMapper.mistakeCount(userId));
        data.put("planCount", analyticsMapper.planCount(userId));
        data.put("resourceCount", analyticsMapper.resourceCount(userId));
        data.put("checkinStreak", computeStreak(analyticsMapper.checkinDates(userId)));
        data.put("weakPoints", analyticsMapper.weakPoints(userId));
        return data;
    }

    /** 连续打卡天数(从今天或昨天往前数) */
    private int computeStreak(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        if (!dates.contains(cursor)) {
            return 0;
        }
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}

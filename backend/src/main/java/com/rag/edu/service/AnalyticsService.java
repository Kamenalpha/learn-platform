package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.entity.AiDiagnosis;
import com.rag.edu.entity.KnowledgePoint;
import com.rag.edu.mapper.AiDiagnosisMapper;
import com.rag.edu.mapper.AnalyticsMapper;
import com.rag.edu.mapper.KnowledgePointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习画像:学习时长/连续打卡/掌握度/弱项等汇总 + AI 诊断.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsMapper analyticsMapper;
    private final AiDiagnosisMapper aiDiagnosisMapper;
    private final KnowledgePointMapper knowledgePointMapper;

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

    /** AI 诊断:最近的 AI 学情观察(来源:主观题 AI 评分评语),供画像页展示 */
    public List<Map<String, Object>> diagnoses(Long userId) {
        List<AiDiagnosis> list = aiDiagnosisMapper.selectList(new LambdaQueryWrapper<AiDiagnosis>()
                .eq(AiDiagnosis::getUserId, userId)
                .orderByDesc(AiDiagnosis::getCreateTime)
                .last("LIMIT 20"));
        return list.stream().map(d -> {
            String kpName = null;
            if (d.getKpId() != null) {
                KnowledgePoint kp = knowledgePointMapper.selectById(d.getKpId());
                kpName = kp == null ? null : kp.getKpName();
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("diagnosisId", d.getDiagnosisId());
            m.put("kpName", kpName);
            m.put("sourceType", d.getSourceType());
            m.put("content", d.getContent());
            m.put("createTime", d.getCreateTime());
            return m;
        }).toList();
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

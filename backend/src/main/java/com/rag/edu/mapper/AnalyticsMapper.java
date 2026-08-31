package com.rag.edu.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学习画像统计(纯查询,无需实体).
 */
public interface AnalyticsMapper {

    @Select("SELECT COALESCE(SUM(duration_sec), 0) FROM study_log WHERE user_id = #{userId}")
    Long totalSeconds(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(duration_sec), 0) FROM study_log WHERE user_id = #{userId} AND DATE(create_time) = CURDATE()")
    Long todaySeconds(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM qa_record WHERE user_id = #{userId}")
    Long chatCount(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM exam_record WHERE user_id = #{userId}")
    Long examCount(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM mistake WHERE user_id = #{userId}")
    Long mistakeCount(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM study_plan WHERE user_id = #{userId}")
    Long planCount(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM resource r WHERE r.user_id = #{userId}")
    Long resourceCount(@Param("userId") Long userId);

    @Select("SELECT checkin_date FROM checkin WHERE user_id = #{userId} ORDER BY checkin_date DESC")
    List<LocalDate> checkinDates(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(NULLIF(kp.kp_name, ''), '未归类') AS name, COUNT(*) AS cnt
            FROM mistake m JOIN question q ON q.question_id = m.question_id
            LEFT JOIN knowledge_point kp ON kp.kp_id = q.kp_id
            WHERE m.user_id = #{userId}
            GROUP BY kp.kp_id, kp.kp_name ORDER BY cnt DESC LIMIT 5
            """)
    List<Map<String, Object>> weakPoints(@Param("userId") Long userId);
}

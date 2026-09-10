package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.QuotaLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface QuotaLogMapper extends BaseMapper<QuotaLog> {

    /** 当月原子累加(唯一键 uk_user_action_month 冲突时累加) */
    @Insert("INSERT INTO quota_log (user_id, action_type, month, use_count, create_time) "
            + "VALUES (#{userId}, #{action}, #{month}, 1, NOW()) "
            + "ON DUPLICATE KEY UPDATE use_count = use_count + 1")
    int upsertMonth(@Param("userId") Long userId, @Param("action") String action, @Param("month") String month);

    /** 某月各动作总用量 */
    @Select("SELECT action_type AS actionType, IFNULL(SUM(use_count), 0) AS total "
            + "FROM quota_log WHERE month = #{month} GROUP BY action_type")
    List<Map<String, Object>> sumByMonth(@Param("month") String month);

    /** 某月用量 Top 用户 */
    @Select("SELECT user_id AS userId, IFNULL(SUM(use_count), 0) AS total "
            + "FROM quota_log WHERE month = #{month} GROUP BY user_id ORDER BY total DESC LIMIT 10")
    List<Map<String, Object>> topUsersByMonth(@Param("month") String month);

    /** 最近记账明细 */
    @Select("SELECT quota_id AS quotaId, user_id AS userId, action_type AS actionType, "
            + "month, use_count AS useCount, create_time AS createTime "
            + "FROM quota_log ORDER BY quota_id DESC LIMIT #{limit}")
    List<Map<String, Object>> recent(@Param("limit") int limit);
}

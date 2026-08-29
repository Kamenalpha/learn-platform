package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.QaRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface QaRecordMapper extends BaseMapper<QaRecord> {

    /** 用户会话列表(按会话聚合) */
    @Select("""
            SELECT session_id              AS sessionId,
                   MAX(question)           AS lastQuestion,
                   MAX(create_time)        AS lastTime,
                   COUNT(*)                AS msgCount
            FROM qa_record WHERE user_id = #{userId}
            GROUP BY session_id ORDER BY lastTime DESC
            """)
    List<Map<String, Object>> listSessions(@Param("userId") Long userId);

    /** 近N天每日提问量(运行监控) */
    @Select("""
            SELECT DATE(create_time) AS date, COUNT(*) AS count
            FROM qa_record WHERE create_time >= #{since}
            GROUP BY DATE(create_time) ORDER BY date
            """)
    List<Map<String, Object>> countDaily(@Param("since") LocalDateTime since);
}

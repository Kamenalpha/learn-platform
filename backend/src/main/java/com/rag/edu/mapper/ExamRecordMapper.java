package com.rag.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.edu.entity.ExamRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Update("""
            UPDATE exam_record SET status = #{status}, end_time = #{endTime}
            WHERE exam_id = #{examId} AND user_id = #{userId} AND status = 0
            """)
    int claim(@Param("examId") Long examId, @Param("userId") Long userId,
              @Param("status") int status, @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE exam_record SET score = #{score} WHERE exam_id = #{examId}")
    int updateScore(@Param("examId") Long examId, @Param("score") double score);
}

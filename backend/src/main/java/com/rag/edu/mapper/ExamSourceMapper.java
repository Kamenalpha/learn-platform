package com.rag.edu.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 出题素材查询(无需实体)
 */
public interface ExamSourceMapper {

    /** 某课程(可选限定到某资料)的已解析分块内容 */
    @Select("""
            <script>
            SELECT ch.content, ch.resource_id, ch.page_num
            FROM doc_chunk ch JOIN resource r ON r.resource_id = ch.resource_id
            WHERE r.course_id = #{courseId}
            <if test="resourceId != null"> AND ch.resource_id = #{resourceId} </if>
            ORDER BY ch.resource_id, ch.chunk_index
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> courseChunks(@Param("courseId") Long courseId,
                                           @Param("resourceId") Long resourceId,
                                           @Param("limit") int limit);

    /** 用户划定的重点内容 */
    @Select("SELECT content FROM user_keypoint WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<String> userKeypoints(@Param("userId") Long userId, @Param("limit") int limit);
}

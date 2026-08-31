package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.Chapter;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.KnowledgePoint;
import com.rag.edu.mapper.ChapterMapper;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.KnowledgePointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识点(分类叶子)管理,归属校验到课程创建人.
 */
@Service
@RequiredArgsConstructor
public class KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    public List<KnowledgePoint> listByChapter(Long chapterId) {
        return knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getChapterId, chapterId)
                .orderByAsc(KnowledgePoint::getOrderNum));
    }

    public void save(KnowledgePoint kp, Long userId) {
        verifyChapterOwner(kp.getChapterId(), userId);
        if (kp.getOrderNum() == null) {
            kp.setOrderNum(0);
        }
        if (kp.getKpId() == null) {
            knowledgePointMapper.insert(kp);
        } else {
            KnowledgePoint db = knowledgePointMapper.selectById(kp.getKpId());
            if (db == null || !db.getChapterId().equals(kp.getChapterId())) {
                throw new BizException(403, "无权修改该知识点");
            }
            knowledgePointMapper.updateById(kp);
        }
    }

    public void delete(Long kpId, Long userId) {
        KnowledgePoint db = knowledgePointMapper.selectById(kpId);
        if (db == null) {
            return;
        }
        verifyChapterOwner(db.getChapterId(), userId);
        knowledgePointMapper.deleteById(kpId);
    }

    private void verifyChapterOwner(Long chapterId, Long userId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BizException("章节不存在");
        }
        Course course = courseMapper.selectById(chapter.getCourseId());
        if (course == null || !course.getOwnerId().equals(userId)) {
            throw new BizException(403, "无权操作该知识点");
        }
    }
}

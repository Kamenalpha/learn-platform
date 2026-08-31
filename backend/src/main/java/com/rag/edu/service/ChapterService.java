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
 * 章节(分类第三层)管理,归属校验到课程创建人.
 */
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public List<Chapter> listByCourse(Long courseId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, courseId)
                .orderByAsc(Chapter::getOrderNum));
    }

    public void save(Chapter chapter, Long userId) {
        verifyCourseOwner(chapter.getCourseId(), userId);
        if (chapter.getOrderNum() == null) {
            chapter.setOrderNum(0);
        }
        if (chapter.getChapterId() == null) {
            chapterMapper.insert(chapter);
        } else {
            Chapter db = chapterMapper.selectById(chapter.getChapterId());
            if (db == null || !db.getCourseId().equals(chapter.getCourseId())) {
                throw new BizException(403, "无权修改该章节");
            }
            chapterMapper.updateById(chapter);
        }
    }

    public void delete(Long chapterId, Long userId) {
        Chapter db = chapterMapper.selectById(chapterId);
        if (db == null) {
            return;
        }
        verifyCourseOwner(db.getCourseId(), userId);
        Long kps = knowledgePointMapper.selectCount(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getChapterId, chapterId));
        Long children = chapterMapper.selectCount(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getParentId, chapterId));
        if (kps > 0 || children > 0) {
            throw new BizException("该章节下还有知识点/子章节,请先删除");
        }
        chapterMapper.deleteById(chapterId);
    }

    private void verifyCourseOwner(Long courseId, Long userId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getOwnerId().equals(userId)) {
            throw new BizException(403, "无权操作该课程下的章节");
        }
    }
}

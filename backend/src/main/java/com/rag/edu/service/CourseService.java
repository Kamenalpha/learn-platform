package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.Course;
import com.rag.edu.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 课程(四级分类第二层)管理 —— 归属当前用户,列表展示"我的 + 公开审核通过".
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;

    public List<Map<String, Object>> listVisible(Long userId) {
        return courseMapper.listVisible(userId);
    }

    public void save(Course course, Long userId) {
        // 归属当前用户
        course.setOwnerId(userId);
        if (course.getVisibility() == null) {
            course.setVisibility(0);
        }
        if (course.getAuditStatus() == null) {
            course.setAuditStatus(0);
        }
        if (course.getCourseId() == null) {
            // 同一用户下课程名不得重复
            Long exists = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getOwnerId, userId)
                    .eq(Course::getCourseName, course.getCourseName()));
            if (exists > 0) {
                throw new BizException("同名课程已存在");
            }
            courseMapper.insert(course);
        } else {
            Course db = courseMapper.selectById(course.getCourseId());
            if (db == null || !db.getOwnerId().equals(userId)) {
                throw new BizException(403, "无权修改该课程");
            }
            courseMapper.updateById(course);
        }
    }

    public void delete(Long courseId, Long userId) {
        Course db = courseMapper.selectById(courseId);
        if (db == null || !db.getOwnerId().equals(userId)) {
            throw new BizException(403, "无权删除该课程");
        }
        Long docs = courseMapper.countResources(courseId);
        if (docs > 0) {
            throw new BizException("该课程下还有 " + docs + " 个资料,请先删除资料");
        }
        courseMapper.deleteById(courseId);
    }
}

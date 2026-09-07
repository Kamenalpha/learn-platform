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
    private final CourseAccessService courseAccessService;

    public List<Map<String, Object>> listVisible(Long userId) {
        return courseMapper.listVisible(userId);
    }

    public void save(Course course, Long userId) {
        // 归属当前用户
        course.setOwnerId(userId);
        Course db = course.getCourseId() == null ? null
                : courseAccessService.requireManagedCourse(course.getCourseId(), userId);
        // 可见性缺省时:新建视为私有,编辑保持原值
        if (course.getVisibility() == null) {
            course.setVisibility(db == null ? 0 : db.getVisibility());
        }
        // 审核状态由服务端裁定,不信任前端传值:公开 = (重新)提交审核(0);
        // 已通过审核且公开状态未改动的课程维持通过(1)
        boolean keepApproved = db != null
                && Integer.valueOf(1).equals(db.getVisibility())
                && Integer.valueOf(1).equals(db.getAuditStatus())
                && Integer.valueOf(1).equals(course.getVisibility());
        course.setAuditStatus(keepApproved ? 1 : 0);
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
            courseMapper.updateById(course);
        }
    }

    public void delete(Long courseId, Long userId) {
        courseAccessService.requireManagedCourse(courseId, userId);
        Long docs = courseMapper.countResources(courseId);
        if (docs > 0) {
            throw new BizException("该课程下还有 " + docs + " 个资料,请先删除资料");
        }
        courseMapper.deleteById(courseId);
    }
}

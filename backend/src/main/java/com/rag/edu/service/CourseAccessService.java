package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.DocResource;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/** 课程及其资料的统一访问边界。 */
@Service
@RequiredArgsConstructor
public class CourseAccessService {

    private final CourseMapper courseMapper;
    private final DocResourceMapper resourceMapper;

    public Course requireReadableCourse(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        if (!isAdmin() && !isOwner(course, userId) && !isPublic(course)) {
            throw new BizException(403, "无权访问该课程");
        }
        return course;
    }

    public Course requireManagedCourse(Long courseId, Long userId) {
        Course course = requireCourse(courseId);
        if (!isAdmin() && !isOwner(course, userId)) {
            throw new BizException(403, "无权操作该课程");
        }
        return course;
    }

    public void requireManagedCourses(Collection<Long> courseIds, Long userId) {
        if (courseIds == null) {
            return;
        }
        courseIds.stream().distinct().forEach(id -> requireManagedCourse(id, userId));
    }

    public List<Long> listManagedCourseIds(Long userId) {
        // ponytail:普通问答暂只检索自有课程;开放全站公开课程时应先增加显式选课,避免召回范围无限增长。
        if (isAdmin()) {
            return courseMapper.selectList(null).stream().map(Course::getCourseId).toList();
        }
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getOwnerId, userId))
                .stream().map(Course::getCourseId).toList();
    }

    public DocResource requireReadableResource(Long resourceId, Long userId) {
        DocResource resource = requireResource(resourceId);
        if (isAdmin() || userId != null && userId.equals(resource.getUserId())) {
            return resource;
        }
        Course course = courseMapper.selectById(resource.getCourseId());
        if (!isPublic(resource) || course == null || !isPublic(course)) {
            throw new BizException(403, "无权访问该资料");
        }
        return resource;
    }

    public DocResource requireManagedResource(Long resourceId, Long userId) {
        DocResource resource = requireResource(resourceId);
        if (!isAdmin() && (userId == null || !userId.equals(resource.getUserId()))) {
            throw new BizException(403, "无权操作该资料");
        }
        return resource;
    }

    public boolean canReadResource(Long resourceId, Long userId) {
        try {
            requireReadableResource(resourceId, userId);
            return true;
        } catch (BizException e) {
            return false;
        }
    }

    private Course requireCourse(Long courseId) {
        Course course = courseId == null ? null : courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        return course;
    }

    private DocResource requireResource(Long resourceId) {
        DocResource resource = resourceId == null ? null : resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BizException(404, "资料不存在");
        }
        return resource;
    }

    private static boolean isOwner(Course course, Long userId) {
        return userId != null && userId.equals(course.getOwnerId());
    }

    private static boolean isPublic(Course course) {
        return Integer.valueOf(1).equals(course.getVisibility())
                && Integer.valueOf(1).equals(course.getAuditStatus());
    }

    private static boolean isPublic(DocResource resource) {
        return Integer.valueOf(1).equals(resource.getVisibility())
                && Integer.valueOf(1).equals(resource.getAuditStatus());
    }

    private static boolean isAdmin() {
        LoginUser user = UserContext.get();
        return user != null && Integer.valueOf(1).equals(user.getRole());
    }
}

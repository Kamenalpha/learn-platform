package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.CourseDocument;
import com.rag.edu.mapper.CourseDocumentMapper;
import com.rag.edu.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 课程(知识库分类)管理
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseDocumentMapper documentMapper;

    public List<Map<String, Object>> listWithDocCount() {
        return courseMapper.listWithDocCount();
    }

    public void save(Course course) {
        if (course.getCourseId() == null) {
            Long exists = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getCourseName, course.getCourseName()));
            if (exists > 0) {
                throw new BizException("课程名称已存在");
            }
            courseMapper.insert(course);
        } else {
            courseMapper.updateById(course);
        }
    }

    public void delete(Long courseId) {
        Long docs = documentMapper.selectCount(new LambdaQueryWrapper<CourseDocument>()
                .eq(CourseDocument::getCourseId, courseId));
        if (docs > 0) {
            throw new BizException("该课程下还有 " + docs + " 个文档,请先删除文档");
        }
        courseMapper.deleteById(courseId);
    }
}

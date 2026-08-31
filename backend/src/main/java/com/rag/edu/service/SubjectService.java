package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.Subject;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学科(分类顶层)管理:列表对已登录用户开放,增删改仅管理员.
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;

    public List<Subject> list() {
        return subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                .orderByAsc(Subject::getSubjectId));
    }

    public void save(Subject subject) {
        if (subject.getSubjectId() == null) {
            Long exists = subjectMapper.selectCount(new LambdaQueryWrapper<Subject>()
                    .eq(Subject::getSubjectName, subject.getSubjectName()));
            if (exists > 0) {
                throw new BizException("学科名称已存在");
            }
            subjectMapper.insert(subject);
        } else {
            subjectMapper.updateById(subject);
        }
    }

    public void delete(Long subjectId) {
        Long courses = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getSubjectId, subjectId));
        if (courses > 0) {
            throw new BizException("该学科下还有 " + courses + " 个课程,请先删除课程");
        }
        subjectMapper.deleteById(subjectId);
    }
}

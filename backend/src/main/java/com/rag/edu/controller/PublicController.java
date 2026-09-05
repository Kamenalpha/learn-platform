package com.rag.edu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.common.Result;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.entity.DocResource;
import com.rag.edu.entity.Subject;
import com.rag.edu.entity.SysUser;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.mapper.DocResourceMapper;
import com.rag.edu.mapper.SubjectMapper;
import com.rag.edu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公开内容接口(游客可访问,/api/public/** 在 WebConfig 白名单中):
 * 公开课程来源于用户上传并自主公开(visibility=1),
 * 前端展示时须标注上传者与版权声明("如若侵权,可联系删除")。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    /** 试读返回的最大分块数 */
    private static final int PREVIEW_CHUNKS = 8;

    private final CourseMapper courseMapper;
    private final DocResourceMapper docResourceMapper;
    private final DocChunkMapper docChunkMapper;
    private final SubjectMapper subjectMapper;
    private final SysUserMapper userMapper;

    /** 公开课程列表(含学科名/公开人/公开教材数) */
    @GetMapping("/courses")
    public Result<List<Map<String, Object>>> courses() {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getVisibility, 1)
                .orderByDesc(Course::getCreateTime)
                .last("LIMIT 60"));

        Map<Long, String> subjectNames = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getSubjectId, Subject::getSubjectName, (a, b) -> a));
        Map<Long, String> nicknames = nicknamesOf(
                courses.stream().map(Course::getOwnerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, Long> docCounts = docResourceMapper.selectList(new LambdaQueryWrapper<DocResource>()
                        .eq(DocResource::getVisibility, 1)).stream()
                .filter(d -> d.getCourseId() != null)
                .collect(Collectors.groupingBy(DocResource::getCourseId, Collectors.counting()));

        return Result.ok(courses.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", c.getCourseId());
            m.put("courseName", c.getCourseName());
            m.put("description", c.getDescription());
            m.put("subjectName", subjectNames.getOrDefault(c.getSubjectId(), "综合"));
            m.put("ownerName", nicknames.getOrDefault(c.getOwnerId(), "平台用户"));
            m.put("docCount", docCounts.getOrDefault(c.getCourseId(), 0L));
            m.put("createTime", c.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 某公开课程的公开教材列表 */
    @GetMapping("/courses/{courseId}/docs")
    public Result<List<Map<String, Object>>> courseDocs(@PathVariable Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getVisibility() == null || course.getVisibility() != 1) {
            throw new BizException(404, "课程不存在或未公开");
        }
        List<DocResource> docs = docResourceMapper.selectList(new LambdaQueryWrapper<DocResource>()
                .eq(DocResource::getCourseId, courseId)
                .eq(DocResource::getVisibility, 1)
                .orderByDesc(DocResource::getCreateTime));
        Map<Long, String> nicknames = nicknamesOf(
                docs.stream().map(DocResource::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));

        return Result.ok(docs.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("resourceId", d.getResourceId());
            m.put("title", d.getTitle());
            m.put("fileType", d.getFileType());
            m.put("chunkCount", d.getChunkCount());
            m.put("ownerName", nicknames.getOrDefault(d.getUserId(), "平台用户"));
            m.put("createTime", d.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 公开教材试读:拼接前若干分块正文(不含原文件下载) */
    @GetMapping("/docs/{resourceId}/preview")
    public Result<Map<String, Object>> docPreview(@PathVariable Long resourceId) {
        DocResource doc = docResourceMapper.selectById(resourceId);
        if (doc == null || doc.getVisibility() == null || doc.getVisibility() != 1) {
            throw new BizException(404, "教材不存在或未公开");
        }
        List<DocChunk> chunks = docChunkMapper.selectList(new LambdaQueryWrapper<DocChunk>()
                .eq(DocChunk::getResourceId, resourceId)
                .orderByAsc(DocChunk::getChunkIndex)
                .last("LIMIT " + PREVIEW_CHUNKS));

        Map<Long, String> nicknames = nicknamesOf(
                doc.getUserId() == null ? Set.of() : Set.of(doc.getUserId()));
        Map<String, Object> data = new HashMap<>();
        data.put("title", doc.getTitle());
        data.put("fileType", doc.getFileType());
        data.put("ownerName", nicknames.getOrDefault(doc.getUserId(), "平台用户"));
        data.put("content", chunks.stream().map(DocChunk::getContent).collect(Collectors.joining("\n\n")));
        return Result.ok(data);
    }

    private Map<Long, String> nicknamesOf(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId,
                        u -> u.getNickname() == null || u.getNickname().isBlank()
                                ? u.getUsername() : u.getNickname(), (a, b) -> a));
    }
}

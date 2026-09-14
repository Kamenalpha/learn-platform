package com.rag.edu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.common.Result;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.DocResource;
import com.rag.edu.entity.Post;
import com.rag.edu.entity.Subject;
import com.rag.edu.entity.SysUser;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocResourceMapper;
import com.rag.edu.mapper.PostMapper;
import com.rag.edu.mapper.SubjectMapper;
import com.rag.edu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内容审核接口(仅管理员,/api/admin/** 由 AuthInterceptor 强制角色):
 * 用户将课程/资料设为公开(visibility=1)后进入待审核(audit_status=0),
 * 管理员通过(1)后游客才可在公开页看到;驳回(2)则不公开。落实版权合规要求。
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final CourseMapper courseMapper;
    private final DocResourceMapper docResourceMapper;
    private final SubjectMapper subjectMapper;
    private final SysUserMapper userMapper;
    private final PostMapper postMapper;

    /** 待审核 + 已驳回的公开申请(课程/教材/社区帖三类) */
    @GetMapping("/pending")
    public Result<Map<String, Object>> pending() {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getVisibility, 1)
                .in(Course::getAuditStatus, 0, 2)
                .orderByDesc(Course::getCreateTime));
        List<DocResource> resources = docResourceMapper.selectList(new LambdaQueryWrapper<DocResource>()
                .eq(DocResource::getVisibility, 1)
                .in(DocResource::getAuditStatus, 0, 2)
                .orderByDesc(DocResource::getCreateTime));
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .in(Post::getAuditStatus, 0, 2)
                .orderByDesc(Post::getCreateTime));

        Map<Long, String> subjectNames = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getSubjectId, Subject::getSubjectName, (a, b) -> a));
        Set<Long> userIds = courses.stream().map(Course::getOwnerId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        resources.stream().map(DocResource::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        posts.stream().map(Post::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        Map<Long, String> nicknames = nicknamesOf(userIds);
        Map<Long, String> courseNames = courseNamesOf(resources);

        Map<String, Object> data = new HashMap<>();
        data.put("courses", courses.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", c.getCourseId());
            m.put("courseName", c.getCourseName());
            m.put("description", c.getDescription());
            m.put("subjectName", subjectNames.getOrDefault(c.getSubjectId(), "综合"));
            m.put("ownerName", nicknames.getOrDefault(c.getOwnerId(), "未知用户"));
            m.put("auditStatus", c.getAuditStatus());
            m.put("createTime", c.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
        data.put("resources", resources.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("resourceId", r.getResourceId());
            m.put("title", r.getTitle());
            m.put("fileType", r.getFileType());
            m.put("courseName", courseNames.getOrDefault(r.getCourseId(), "未归属课程"));
            m.put("ownerName", nicknames.getOrDefault(r.getUserId(), "未知用户"));
            m.put("auditStatus", r.getAuditStatus());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
        data.put("posts", posts.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("postId", p.getPostId());
            m.put("title", p.getTitle());
            m.put("type", p.getType());
            m.put("authorName", nicknames.getOrDefault(p.getUserId(), "未知用户"));
            m.put("auditStatus", p.getAuditStatus());
            m.put("createTime", p.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
        return Result.ok(data);
    }

    /** 课程审核决定:action=approve 通过 / reject 驳回 */
    @PostMapping("/course/{courseId}")
    public Result<Void> decideCourse(@PathVariable Long courseId, @RequestParam String action) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getVisibility() == null || course.getVisibility() != 1) {
            throw new BizException(404, "课程不存在或未申请公开");
        }
        course.setAuditStatus(decide(action));
        courseMapper.updateById(course);
        return Result.ok();
    }

    /** 教材审核决定:action=approve 通过 / reject 驳回 */
    @PostMapping("/resource/{resourceId}")
    public Result<Void> decideResource(@PathVariable Long resourceId, @RequestParam String action) {
        DocResource resource = docResourceMapper.selectById(resourceId);
        if (resource == null || resource.getVisibility() == null || resource.getVisibility() != 1) {
            throw new BizException(404, "教材不存在或未申请公开");
        }
        resource.setAuditStatus(decide(action));
        docResourceMapper.updateById(resource);
        return Result.ok();
    }

    /** 社区帖审核决定:action=approve 通过后对他人在 feed 可见 / reject 驳回 */
    @PostMapping("/post/{postId}")
    public Result<Void> decidePost(@PathVariable Long postId, @RequestParam String action) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(404, "帖子不存在");
        }
        post.setAuditStatus(decide(action));
        postMapper.updateById(post);
        return Result.ok();
    }

    private int decide(String action) {
        if ("approve".equals(action)) {
            return 1;
        }
        if ("reject".equals(action)) {
            return 2;
        }
        throw new BizException("非法的审核动作,仅支持 approve/reject");
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

    private Map<Long, String> courseNamesOf(List<DocResource> resources) {
        Set<Long> courseIds = resources.stream().map(DocResource::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, Course::getCourseName, (a, b) -> a));
    }
}

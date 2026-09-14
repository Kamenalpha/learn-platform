package com.rag.edu.controller;

import com.rag.edu.common.BizException;
import com.rag.edu.common.Result;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.DocResource;
import com.rag.edu.entity.Post;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocResourceMapper;
import com.rag.edu.mapper.PostMapper;
import com.rag.edu.mapper.SubjectMapper;
import com.rag.edu.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审核模块测试:课程/教材/社区帖三类审核决定,含非法动作拒绝与待审列表.
 */
class AdminAuditControllerTest {

    private final CourseMapper courseMapper = mock(CourseMapper.class);
    private final DocResourceMapper resourceMapper = mock(DocResourceMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final AdminAuditController controller = new AdminAuditController(
            courseMapper, resourceMapper, mock(SubjectMapper.class), mock(SysUserMapper.class), postMapper);

    @Test
    void decideCourseApproveSetsStatus1() {
        Course course = course(10L, 1);
        when(courseMapper.selectById(10L)).thenReturn(course);

        controller.decideCourse(10L, "approve");

        assertEquals(1, course.getAuditStatus());
        verify(courseMapper).updateById(course);
    }

    @Test
    void decideCourseRejectSetsStatus2() {
        Course course = course(10L, 1);
        when(courseMapper.selectById(10L)).thenReturn(course);

        controller.decideCourse(10L, "reject");

        assertEquals(2, course.getAuditStatus());
        verify(courseMapper).updateById(course);
    }

    @Test
    void decideCourseNotPublicThrows404() {
        when(courseMapper.selectById(10L)).thenReturn(course(10L, 0));

        BizException denied = assertThrows(BizException.class, () -> controller.decideCourse(10L, "approve"));
        assertEquals(404, denied.getCode());
        verify(courseMapper, never()).updateById(any(Course.class));
    }

    @Test
    void decideResourceInvalidActionThrows() {
        DocResource resource = new DocResource();
        resource.setResourceId(20L);
        resource.setVisibility(1);
        when(resourceMapper.selectById(20L)).thenReturn(resource);

        assertThrows(BizException.class, () -> controller.decideResource(20L, "delete"));
        verify(resourceMapper, never()).updateById(any(DocResource.class));
    }

    @Test
    void decidePostApproveMakesFeedVisible() {
        Post post = post(30L, 1L, 0);
        when(postMapper.selectById(30L)).thenReturn(post);

        controller.decidePost(30L, "approve");

        assertEquals(1, post.getAuditStatus());
        verify(postMapper).updateById(post);
    }

    @Test
    void decidePostRejectKeepsInvisible() {
        Post post = post(30L, 1L, 0);
        when(postMapper.selectById(30L)).thenReturn(post);

        controller.decidePost(30L, "reject");

        assertEquals(2, post.getAuditStatus());
        verify(postMapper).updateById(post);
    }

    @Test
    void decidePostMissingThrows404() {
        when(postMapper.selectById(404L)).thenReturn(null);

        BizException denied = assertThrows(BizException.class, () -> controller.decidePost(404L, "approve"));
        assertEquals(404, denied.getCode());
    }

    @Test
    void pendingIncludesPostsSection() {
        Post pendingPost = post(30L, 1L, 0);
        pendingPost.setTitle("求助:递归分块怎么理解");
        when(postMapper.selectList(any())).thenReturn(List.of(pendingPost));

        Result<Map<String, Object>> result = controller.pending();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> posts = (List<Map<String, Object>>) result.getData().get("posts");
        assertEquals(1, posts.size());
        assertEquals("求助:递归分块怎么理解", posts.get(0).get("title"));
        assertEquals(0, posts.get(0).get("auditStatus"));
        assertTrue(posts.get(0).containsKey("authorName"));
    }

    private static Course course(Long id, int visibility) {
        Course course = new Course();
        course.setCourseId(id);
        course.setVisibility(visibility);
        course.setAuditStatus(0);
        return course;
    }

    private static Post post(Long id, Long userId, int auditStatus) {
        Post post = new Post();
        post.setPostId(id);
        post.setUserId(userId);
        post.setAuditStatus(auditStatus);
        return post;
    }
}

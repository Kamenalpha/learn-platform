package com.rag.edu.service;

import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.Post;
import com.rag.edu.mapper.AnswerMapper;
import com.rag.edu.mapper.CommentMapper;
import com.rag.edu.mapper.PostLikeMapper;
import com.rag.edu.mapper.PostMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 社区帖审核流测试:新帖进入待审(0),feed 携带当前用户(本人视角可见自己的待审帖).
 */
class CommunityServiceTest {

    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommunityService service = new CommunityService(
            postMapper, mock(AnswerMapper.class), mock(CommentMapper.class), mock(PostLikeMapper.class));

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void createPostEntersPendingAudit() {
        Post post = new Post();
        post.setTitle("测试帖");
        post.setContent("内容");

        Post created = service.createPost(1L, post);

        assertEquals(0, created.getAuditStatus());
        assertEquals(1L, created.getUserId());
        verify(postMapper).insert(created);
    }

    @Test
    void feedPassesCurrentUserId() {
        UserContext.set(new LoginUser(1L, "student", 0));

        service.feed(null, null);

        verify(postMapper).listFeed(any(), any(), eq(1L));
    }

    @Test
    void feedWithoutLoginPassesNullUser() {
        UserContext.clear();

        service.feed(null, null);

        verify(postMapper).listFeed(any(), any(), eq((Long) null));
    }
}

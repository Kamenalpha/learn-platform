package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.Answer;
import com.rag.edu.entity.Comment;
import com.rag.edu.entity.Post;
import com.rag.edu.entity.PostLike;
import com.rag.edu.mapper.AnswerMapper;
import com.rag.edu.mapper.CommentMapper;
import com.rag.edu.mapper.PostLikeMapper;
import com.rag.edu.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 社区:帖子/文章/问答,支持回答采纳、评论、点赞.
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostMapper postMapper;
    private final AnswerMapper answerMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper likeMapper;

    public List<Map<String, Object>> feed(Long courseId, Integer type) {
        return postMapper.listFeed(courseId, type, UserContext.userId());
    }

    public Map<String, Object> detail(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(404, "帖子不存在");
        }
        postMapper.addView(postId);
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        return Map.of("post", post,
                "answers", answerMapper.listByPost(postId),
                "comments", commentMapper.listByPost(postId));
    }

    public Post createPost(Long userId, Post post) {
        post.setUserId(userId);
        post.setType(post.getType() == null ? 0 : post.getType());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsAi(post.getIsAi() == null ? 0 : post.getIsAi());
        post.setAuditStatus(0); // 待审核:通过前对他人在 feed 中不可见(本人始终可见自己的帖子)
        postMapper.insert(post);
        return post;
    }

    public Answer addAnswer(Long postId, Long userId, String content) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(404, "帖子不存在");
        }
        Answer a = new Answer();
        a.setPostId(postId);
        a.setUserId(userId);
        a.setContent(content);
        a.setIsAccepted(0);
        a.setLikeCount(0);
        answerMapper.insert(a);
        return a;
    }

    /** 采纳某回答(仅帖主可操作) */
    public void acceptAnswer(Long answerId, Long userId) {
        Answer a = answerMapper.selectById(answerId);
        if (a == null) {
            throw new BizException(404, "回答不存在");
        }
        Post post = postMapper.selectById(a.getPostId());
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BizException(403, "仅帖主可采纳回答");
        }
        // 同帖其它回答取消采纳
        List<Answer> list = answerMapper.selectList(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getPostId, a.getPostId()));
        for (Answer x : list) {
            x.setIsAccepted(x.getAnswerId().equals(answerId) ? 1 : 0);
            answerMapper.updateById(x);
        }
    }

    public Comment addComment(Long postId, Long answerId, Long userId, String content) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(404, "帖子不存在");
        }
        Comment c = new Comment();
        c.setPostId(postId);
        c.setAnswerId(answerId);
        c.setUserId(userId);
        c.setContent(content);
        commentMapper.insert(c);
        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        postMapper.updateById(post);
        return c;
    }

    /** 点赞/取消点赞 */
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(404, "帖子不存在");
        }
        PostLike existing = likeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, userId).eq(PostLike::getPostId, postId).last("LIMIT 1"));
        if (existing != null) {
            likeMapper.deleteById(existing.getLikeId());
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) - 1));
            postMapper.updateById(post);
            return false;
        }
        PostLike like = new PostLike();
        like.setUserId(userId);
        like.setPostId(postId);
        likeMapper.insert(like);
        post.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + 1);
        postMapper.updateById(post);
        return true;
    }

    public void deletePost(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return;
        }
        if (!isOwnerOrAdmin(post.getUserId(), userId)) {
            throw new BizException(403, "无权删除");
        }
        answerMapper.delete(new LambdaQueryWrapper<Answer>().eq(Answer::getPostId, postId));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        likeMapper.delete(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, postId));
        postMapper.deleteById(postId);
    }

    public void deleteAnswer(Long answerId, Long userId) {
        Answer a = answerMapper.selectById(answerId);
        if (a == null) {
            return;
        }
        if (!isOwnerOrAdmin(a.getUserId(), userId)) {
            throw new BizException(403, "无权删除");
        }
        answerMapper.deleteById(answerId);
    }

    private boolean isOwnerOrAdmin(Long ownerId, Long userId) {
        LoginUser u = UserContext.get();
        boolean admin = u != null && u.getRole() != null && u.getRole() == 1;
        return admin || (ownerId != null && ownerId.equals(userId));
    }
}

package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.dto.CommunityDtos.CreatReq;
import com.rag.edu.dto.CommunityDtos.ReplyReq;
import com.rag.edu.entity.Answer;
import com.rag.edu.entity.Comment;
import com.rag.edu.entity.Post;
import com.rag.edu.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社区接口:帖子/问答/评论/点赞.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public Result<List<Map<String, Object>>> feed(@RequestParam(required = false) Long courseId,
                                                  @RequestParam(required = false) Integer type) {
        return Result.ok(communityService.feed(courseId, type));
    }

    @GetMapping("/posts/{postId}")
    public Result<Map<String, Object>> detail(@PathVariable Long postId) {
        return Result.ok(communityService.detail(postId));
    }

    @PostMapping("/posts")
    public Result<Post> create(@RequestBody CreatReq req) {
        Post post = new Post();
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setType(req.type() == null ? 0 : req.type());
        post.setCourseId(req.courseId());
        post.setKpId(req.kpId());
        return Result.ok(communityService.createPost(UserContext.userId(), post));
    }

    @DeleteMapping("/posts/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        communityService.deletePost(postId, UserContext.userId());
        return Result.ok();
    }

    @PostMapping("/posts/{postId}/answers")
    public Result<Answer> addAnswer(@PathVariable Long postId, @RequestBody ReplyReq req) {
        return Result.ok(communityService.addAnswer(postId, UserContext.userId(), req.content()));
    }

    @PostMapping("/answers/{answerId}/accept")
    public Result<Void> acceptAnswer(@PathVariable Long answerId) {
        communityService.acceptAnswer(answerId, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/answers/{answerId}")
    public Result<Void> deleteAnswer(@PathVariable Long answerId) {
        communityService.deleteAnswer(answerId, UserContext.userId());
        return Result.ok();
    }

    @PostMapping("/posts/{postId}/comments")
    public Result<Comment> addComment(@PathVariable Long postId, @RequestBody ReplyReq req) {
        return Result.ok(communityService.addComment(postId, req.answerId(), UserContext.userId(), req.content()));
    }

    @PostMapping("/posts/{postId}/like")
    public Result<Boolean> toggleLike(@PathVariable Long postId) {
        return Result.ok(communityService.toggleLike(postId, UserContext.userId()));
    }
}

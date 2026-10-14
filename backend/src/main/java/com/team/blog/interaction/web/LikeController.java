package com.team.blog.interaction.web;

import com.team.blog.interaction.application.LikeService;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

/** 좋아요 API(30 §3)와 스크립트 없는 폼(FR-020). */
@Controller
public class LikeController {

    private final LikeService likeService;
    private final CurrentUserProvider currentUserProvider;

    public LikeController(LikeService likeService, CurrentUserProvider currentUserProvider) {
        this.likeService = likeService;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/api/posts/{postId}/like")
    @ResponseBody
    public LikeService.LikeResult like(@PathVariable long postId) {
        return likeService.set(currentUserProvider.current(), postId, true);
    }

    @DeleteMapping("/api/posts/{postId}/like")
    @ResponseBody
    public LikeService.LikeResult unlike(@PathVariable long postId) {
        return likeService.set(currentUserProvider.current(), postId, false);
    }

    /** 088 후속(강성찬 개인 확장): 카드 하트를 칠할 글 번호. 비회원은 빈 목록. */
    @GetMapping("/api/likes/mine")
    @ResponseBody
    public java.util.List<Long> mine(@RequestParam(value = "ids", required = false) java.util.List<Long> ids) {
        return currentUserProvider.current()
                .map(u -> likeService.likedAmong(u.memberId(), ids == null ? java.util.List.of() : ids))
                .orElse(java.util.List.of());
    }

    @PostMapping("/@{handle}/posts/{postId}/like")
    public RedirectView form(@PathVariable("handle") String handle, @PathVariable("postId") long postId,
                             @RequestParam(value = "liked", defaultValue = "true") boolean liked) {
        likeService.set(currentUserProvider.current(), postId, liked);
        RedirectView view = new RedirectView(com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + postId);
        view.setStatusCode(HttpStatus.SEE_OTHER);
        return view;
    }
}

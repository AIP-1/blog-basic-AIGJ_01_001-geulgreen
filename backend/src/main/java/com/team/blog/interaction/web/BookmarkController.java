package com.team.blog.interaction.web;

import com.team.blog.interaction.application.BookmarkService;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

/** 10-23 chan: 글 저장 API와 스크립트 없는 폼. 좋아요(LikeController)와 같은 모양. */
@Controller
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final CurrentUserProvider currentUserProvider;

    public BookmarkController(BookmarkService bookmarkService, CurrentUserProvider currentUserProvider) {
        this.bookmarkService = bookmarkService;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/api/posts/{postId}/save")
    @ResponseBody
    public Map<String, Boolean> save(@PathVariable long postId) {
        return Map.of("saved", bookmarkService.set(currentUserProvider.current(), postId, true));
    }

    @DeleteMapping("/api/posts/{postId}/save")
    @ResponseBody
    public Map<String, Boolean> unsave(@PathVariable long postId) {
        return Map.of("saved", bookmarkService.set(currentUserProvider.current(), postId, false));
    }

    @PostMapping("/@{handle}/posts/{postId}/save")
    public RedirectView form(@PathVariable("handle") String handle, @PathVariable("postId") long postId,
                             @RequestParam(value = "saved", defaultValue = "true") boolean saved) {
        bookmarkService.set(currentUserProvider.current(), postId, saved);
        RedirectView view = new RedirectView(com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + postId);
        view.setStatusCode(HttpStatus.SEE_OTHER);
        return view;
    }
}

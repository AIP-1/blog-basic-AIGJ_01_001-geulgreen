package com.team.blog.writing.web;

import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.writing.application.WritingAiService;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** AI로 글 다듬기 API(059): {@code {text, action, ask, visibility}} → {@code {text}}(제안만). */
@RestController
public class WritingAiApiController {

    private final WritingAiService service;
    private final CurrentUserProvider currentUserProvider;

    public WritingAiApiController(WritingAiService service, CurrentUserProvider currentUserProvider) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/api/posts/{postId}/writing-ai")
    public Map<String, String> rewrite(@PathVariable long postId, @RequestBody WritingAiService.Request request) {
        return Map.of("text", service.rewrite(currentUserProvider.current(), postId, request));
    }
}

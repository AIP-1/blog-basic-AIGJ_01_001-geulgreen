package com.team.blog.cover.web;

import com.team.blog.cover.application.CoverService;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** 055 카드 대표 사진 API(강성찬 개인 확장): 글쓰기 발행 화면이 쓴다. 대표 사진이 없으면 204(자동). */
@RestController
@ConditionalOnProperty(name = "blog.cover.enabled", havingValue = "true", matchIfMissing = true)
public class CoverApiController {

    private final CoverService covers;
    private final CurrentUserProvider currentUserProvider;

    public CoverApiController(CoverService covers, CurrentUserProvider currentUserProvider) {
        this.covers = covers;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/api/posts/{postId}/cover")
    public ResponseEntity<CoverService.Cover> get(@PathVariable long postId) {
        return covers.get(currentUserProvider.current(), postId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/api/posts/{postId}/cover")
    public CoverService.Cover set(@PathVariable long postId, @RequestBody(required = false) JsonNode body) {
        JsonNode url = body == null ? null : body.get("imageUrl");
        return covers.set(currentUserProvider.current(), postId, url == null || !url.isString() ? null : url.stringValue());
    }

    @DeleteMapping("/api/posts/{postId}/cover")
    public ResponseEntity<Void> clear(@PathVariable long postId) {
        covers.clear(currentUserProvider.current(), postId);
        return ResponseEntity.noContent().build();
    }
}

package com.team.blog.post.web;

import com.team.blog.post.application.DraftBulkPublishService;
import com.team.blog.post.application.ManagePage;
import com.team.blog.post.application.ManageTab;
import com.team.blog.post.application.PostManageQuery;
import com.team.blog.post.application.PostTrashService;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 내 글 관리 API와 삭제·복구·영구 삭제(011 contracts). */
@RestController
public class PostTrashApiController {

    private final PostTrashService trashService;
    private final PostManageQuery manageQuery;
    private final CurrentUserProvider currentUserProvider;
    private final DraftBulkPublishService bulkPublish;

    public PostTrashApiController(PostTrashService trashService, PostManageQuery manageQuery,
                                  CurrentUserProvider currentUserProvider, DraftBulkPublishService bulkPublish) {
        this.bulkPublish = bulkPublish;
        this.trashService = trashService;
        this.manageQuery = manageQuery;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/api/me/posts")
    public ManagePage myPosts(@RequestParam(value = "tab", required = false) String tab,
                              @RequestParam(value = "visibility", required = false) String visibility,
                              @RequestParam(value = "cursor", required = false) String cursor) {
        return manageQuery.page(currentUserProvider.current(), ManageTab.of(tab), visibility, cursor);
    }

    @GetMapping("/api/me/posts/counts")
    public Map<String, Long> myPostCounts() {
        return manageQuery.counts(currentUserProvider.current());
    }

    @DeleteMapping("/api/posts/{postId}")
    public PostTrashService.TrashResult trash(@PathVariable long postId) {
        return trashService.trash(currentUserProvider.current(), postId);
    }

    @PostMapping("/api/posts/{postId}/restore")
    public Map<String, Boolean> restore(@PathVariable long postId) {
        trashService.restore(currentUserProvider.current(), postId);
        return Map.of("restored", true);
    }

    @DeleteMapping("/api/posts/{postId}/permanent")
    public ResponseEntity<Void> purge(@PathVariable long postId) {
        trashService.purge(currentUserProvider.current(), postId);
        return ResponseEntity.noContent().build();
    }

    /** 113 후속 강성찬 개인 확장: 내 글 &gt; 임시글에서 고른 글을 저장된 설정으로 한 번에 발행(빠진 게 있는 글은 건너뛰고 알려 줌). */
    public record BulkPublishRequest(List<Long> postIds) {
    }

    @PostMapping("/api/me/posts/publish-drafts")
    public DraftBulkPublishService.Result publishDrafts(@RequestBody BulkPublishRequest request) {
        return bulkPublish.publish(currentUserProvider.current(), request == null ? null : request.postIds());
    }
}

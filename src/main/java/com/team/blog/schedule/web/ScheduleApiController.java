package com.team.blog.schedule.web;

import com.team.blog.schedule.application.ScheduleService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** 042 예약 발행 API(강성찬 개인 확장): 글쓰기 발행 화면이 쓴다. 본인 임시글만. */
@RestController
@ConditionalOnProperty(name = "blog.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleApiController {

    private final ScheduleService schedules;
    private final CurrentUserProvider currentUserProvider;
    private final AccountGuard accountGuard;
    private final JdbcTemplate jdbc;

    public ScheduleApiController(ScheduleService schedules, CurrentUserProvider currentUserProvider, AccountGuard accountGuard,
                                 JdbcTemplate jdbc) {
        this.schedules = schedules;
        this.currentUserProvider = currentUserProvider;
        this.accountGuard = accountGuard;
        this.jdbc = jdbc;
    }

    @GetMapping("/api/posts/{postId}/schedule")
    public ResponseEntity<ScheduleService.Schedule> get(@PathVariable long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(currentUserProvider.current());
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
        return schedules.ofPost(postId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/api/posts/{postId}/schedule")
    public ScheduleService.Schedule set(@PathVariable long postId, @RequestBody(required = false) JsonNode body) {
        List<String> tags = new ArrayList<>();
        JsonNode t = body == null ? null : body.get("tags");
        if (t != null && t.isArray()) {
            t.forEach(x -> tags.add(x.isString() ? x.stringValue() : x.toString()));
        }
        return schedules.schedule(currentUserProvider.current(), postId, text(body, "publishAt"), text(body, "visibility"), tags);
    }

    @DeleteMapping("/api/posts/{postId}/schedule")
    public ResponseEntity<Void> cancel(@PathVariable long postId) {
        schedules.cancel(currentUserProvider.current(), postId);
        return ResponseEntity.noContent().build();
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body == null ? null : body.get(field);
        return node == null || node.isNull() ? null : node.isString() ? node.stringValue() : node.toString();
    }
}

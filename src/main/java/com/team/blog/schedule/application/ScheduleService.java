package com.team.blog.schedule.application;

import com.team.blog.post.application.EditingContent;
import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.PostDraftService;
import com.team.blog.post.application.PostPublishService;
import com.team.blog.post.application.PublishCommand;
import com.team.blog.post.domain.PostStatus;
import com.team.blog.post.application.PostEditRow;
import com.team.blog.post.infra.PostEditStore;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 042 예약 발행(강성찬 개인 확장): 임시글에 시각·공개 범위·태그를 맡겨 두면, 그 시각에 발행 서비스(005)로 발행한다.
 * 그때 글의 최신 저장 내용으로 발행하고, 발행 규칙에 걸리면 예약만 지운다(글은 임시글로 남는다).
 */
@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    public record Schedule(long postId, Instant publishAt, String visibility, List<String> tags) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final PostEditStore store;
    private final PostDraftService draftService;
    private final PostPublishService publishService;
    private final PostAccessPolicy accessPolicy;
    private final ScheduleProperties properties;
    private final Clock clock;

    public ScheduleService(JdbcTemplate jdbc, AccountGuard accountGuard, PostEditStore store, PostDraftService draftService,
                           PostPublishService publishService, PostAccessPolicy accessPolicy, ScheduleProperties properties,
                           Clock clock) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.store = store;
        this.draftService = draftService;
        this.publishService = publishService;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 예약을 걸거나 바꾼다. 내 임시글만, 시각은 지금+최소 간격 ~ 최대 기간 안. */
    public Schedule schedule(Optional<CurrentUser> current, long postId, String publishAtIso, String visibility,
                             List<String> tags) {
        CurrentUser me = accountGuard.requireWritable(current);
        PostEditRow row = store.findActive(postId).filter(r -> r.authorId() == me.memberId()).orElseThrow(NotFoundException::new);
        if (row.status() != PostStatus.DRAFT) {
            throw new PostContentException("SCHEDULE_NOT_DRAFT");
        }
        Instant at;
        try {
            at = Instant.parse(publishAtIso == null ? "" : publishAtIso);
        } catch (DateTimeParseException e) {
            throw new PostContentException("SCHEDULE_TIME_INVALID");
        }
        Instant now = clock.instant();
        if (at.isBefore(now.plus(properties.minLead())) || at.isAfter(now.plus(properties.maxAhead()))) {
            throw new PostContentException("SCHEDULE_TIME_INVALID");
        }
        String v = visibility == null || !accessPolicy.supportedVisibilities().contains(visibility) ? "PUBLIC" : visibility;
        List<String> t = tags == null ? List.of() : tags.stream().filter(x -> x != null && !x.isBlank()).toList();
        jdbc.update("""
                INSERT INTO post_schedule (post_id, publish_at, visibility, tags) VALUES (?, ?, ?, ?)
                ON CONFLICT (post_id) DO UPDATE SET publish_at = EXCLUDED.publish_at, visibility = EXCLUDED.visibility,
                    tags = EXCLUDED.tags, created_at = CURRENT_TIMESTAMP
                """, postId, Timestamp.from(at), v, String.join("\n", t));
        return new Schedule(postId, at, v, t);
    }

    public void cancel(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        store.findActive(postId).filter(r -> r.authorId() == me.memberId()).orElseThrow(NotFoundException::new);
        jdbc.update("DELETE FROM post_schedule WHERE post_id = ?", postId);
    }

    public Optional<Schedule> ofPost(long postId) {
        return jdbc.query("SELECT post_id, publish_at, visibility, tags FROM post_schedule WHERE post_id = ?",
                (rs, i) -> new Schedule(rs.getLong("post_id"), rs.getTimestamp("publish_at").toInstant(),
                        rs.getString("visibility"), tags(rs.getString("tags"))), postId).stream().findFirst();
    }

    /** 시각이 된 예약을 발행한다(작업이 1분마다 부른다). 발행한 수. */
    public int runDue() {
        List<Schedule> due = jdbc.query("SELECT post_id, publish_at, visibility, tags FROM post_schedule WHERE publish_at <= ?"
                        + " ORDER BY publish_at LIMIT 100",
                (rs, i) -> new Schedule(rs.getLong("post_id"), rs.getTimestamp("publish_at").toInstant(),
                        rs.getString("visibility"), tags(rs.getString("tags"))), Timestamp.from(clock.instant()));
        int published = 0;
        for (Schedule s : due) {
            // 먼저 지운 쪽만 발행한다(서버가 여럿이어도 한 번)
            if (jdbc.update("DELETE FROM post_schedule WHERE post_id = ? AND publish_at = ?", s.postId(),
                    Timestamp.from(s.publishAt())) == 0) {
                continue;
            }
            try {
                Optional<PostEditRow> row = store.findActive(s.postId()).filter(r -> r.status() == PostStatus.DRAFT);
                if (row.isEmpty()) {
                    continue;
                }
                String role = jdbc.queryForObject("SELECT role FROM member WHERE id = ?", String.class, row.get().authorId());
                EditingContent content = draftService.current(row.get());
                publishService.publish(Optional.of(new CurrentUser(row.get().authorId(), role)), s.postId(),
                        "schedule-" + s.postId() + "-" + s.publishAt().getEpochSecond(),
                        new PublishCommand(content.title(), content.contentMd(), s.tags(), s.visibility(), content.version()));
                published++;
            } catch (RuntimeException e) {
                log.warn("예약 발행 실패 post={}: {}", s.postId(), e.getClass().getSimpleName());
            }
        }
        return published;
    }

    private static List<String> tags(String joined) {
        return joined == null || joined.isEmpty() ? List.of() : Arrays.asList(joined.split("\n"));
    }
}

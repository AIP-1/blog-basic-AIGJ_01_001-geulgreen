package com.team.blog.guestbook.application;

import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.error.RateLimitedException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 040 방명록(강성찬 개인 확장): 블로그마다 300자 글자 인사. 비밀글은 블로그 주인과 쓴 사람만 본다.
 * 쓰기는 이메일 인증한 회원만, 한 시간 20개. 쓴 사람과 블로그 주인이 지울 수 있다.
 */
@Service
public class GuestbookService {

    public record Entry(long id, String content, boolean secret, boolean visible, Instant createdAt, String handle,
                        String nickname, boolean deletable) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final RedisRateLimiter rateLimiter;
    private final GuestbookProperties properties;

    public GuestbookService(JdbcTemplate jdbc, AccountGuard accountGuard, RedisRateLimiter rateLimiter,
                            GuestbookProperties properties) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    public long write(Optional<CurrentUser> current, long ownerId, String rawContent, boolean secret) {
        CurrentUser me = accountGuard.requireWritable(current);
        String content = rawContent == null ? "" : rawContent.strip().replace("\r\n", "\n");
        if (content.isEmpty()) {
            throw new PostContentException("GUESTBOOK_EMPTY");
        }
        if (content.codePointCount(0, content.length()) > properties.maxLength()) {
            throw new PostContentException("GUESTBOOK_TOO_LONG");
        }
        RedisRateLimiter.Result r = rateLimiter.tryAcquire(
                RedisRateLimiter.key("guestbook:write", String.valueOf(me.memberId())), properties.perHour(), Duration.ofHours(1));
        if (!r.allowed()) {
            throw new RateLimitedException(r.retryAfterSeconds());
        }
        return jdbc.queryForObject("INSERT INTO guestbook_entry (owner_id, author_id, content, secret) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, ownerId, me.memberId(), content, secret);
    }

    public void delete(Optional<CurrentUser> current, long entryId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        if (jdbc.update("DELETE FROM guestbook_entry WHERE id = ? AND (author_id = ? OR owner_id = ?)", entryId,
                me.memberId(), me.memberId()) == 0) {
            throw new NotFoundException();
        }
    }

    /** 최신순. 비밀글은 볼 수 없는 사람에게 내용을 비워서 준다. */
    public List<Entry> entries(long ownerId, Optional<CurrentUser> viewer, Long beforeId, int limit) {
        long me = viewer.map(CurrentUser::memberId).orElse(-1L);
        return jdbc.query("SELECT g.id, g.content, g.secret, g.created_at, g.author_id, m.handle, m.nickname FROM guestbook_entry g"
                        + " JOIN member m ON m.id = g.author_id WHERE g.owner_id = ? AND m.withdrawn_at IS NULL"
                        + (beforeId == null ? "" : " AND g.id < " + beforeId) + " ORDER BY g.id DESC LIMIT " + limit,
                (rs, i) -> {
                    long author = rs.getLong("author_id");
                    boolean mine = author == me || ownerId == me;
                    boolean visible = !rs.getBoolean("secret") || mine;
                    return new Entry(rs.getLong("id"), visible ? rs.getString("content") : "", rs.getBoolean("secret"), visible,
                            rs.getTimestamp("created_at").toInstant(), rs.getString("handle"), rs.getString("nickname"), mine);
                }, ownerId);
    }

    public int pageSize() {
        return properties.pageSize();
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 탈퇴 정리: 쓴 것과 그 블로그의 방명록. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM guestbook_entry WHERE author_id = ? OR owner_id = ?", memberId, memberId);
    }
}

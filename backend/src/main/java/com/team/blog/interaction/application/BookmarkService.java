package com.team.blog.interaction.application;

import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.post.application.PostReadAccess;
import com.team.blog.shared.error.RateLimitedException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 10-23 chan: 글 저장(북마크). 좋아요와 달리 나만 보고, 글쓴이에게 알리지 않으며 수도 세지 않는다. 상태 지정 방식(토글 아님).
 * 판정 순서는 좋아요와 같다: 로그인 → 인증 → 글 볼 수 있음(404) → 요청 횟수(429). 자기 글도 저장할 수 있다(나중에 다시 보기).
 */
@Service
public class BookmarkService {

    private final AccountGuard accountGuard;
    private final PostReadAccess postReadAccess;
    private final RedisRateLimiter rateLimiter;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public BookmarkService(AccountGuard accountGuard, PostReadAccess postReadAccess, RedisRateLimiter rateLimiter,
                           JdbcTemplate jdbc, Clock clock) {
        this.accountGuard = accountGuard;
        this.postReadAccess = postReadAccess;
        this.rateLimiter = rateLimiter;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** @return 저장된 상태인지(요청한 값 그대로) */
    public boolean set(Optional<CurrentUser> currentUser, long postId, boolean save) {
        CurrentUser user = accountGuard.requireWritable(currentUser);
        if (save) {
            postReadAccess.requireWritableTarget(currentUser, postId);
        }
        RedisRateLimiter.Result limit = rateLimiter.tryAcquire(
                RedisRateLimiter.key("bookmark:member", String.valueOf(user.memberId())), 60, Duration.ofMinutes(1));
        if (!limit.allowed()) {
            throw new RateLimitedException(limit.retryAfterSeconds());
        }
        if (save) {
            jdbc.update("INSERT INTO post_bookmark (post_id, member_id, created_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    postId, user.memberId(), Timestamp.from(clock.instant()));
        } else {
            // 볼 수 없게 된 글도 목록에서 뺄 수는 있게(읽기 검사 없이 내 행만 지운다)
            jdbc.update("DELETE FROM post_bookmark WHERE post_id = ? AND member_id = ?", postId, user.memberId());
        }
        return save;
    }

    /** 글 상세 처음 화면: 내가 저장했는지(PK 조회 1번). */
    public boolean savedBy(long postId, long memberId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM post_bookmark WHERE post_id = ? AND member_id = ?",
                Integer.class, postId, memberId);
        return n != null && n > 0;
    }
}

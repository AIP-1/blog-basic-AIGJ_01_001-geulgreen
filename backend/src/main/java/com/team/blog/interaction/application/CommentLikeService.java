package com.team.blog.interaction.application;

import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.post.application.PostReadAccess;
import com.team.blog.shared.error.CannotLikeOwnPostException;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.RateLimitedException;
import com.team.blog.shared.event.CommentLiked;
import com.team.blog.shared.event.CommentUnliked;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 089 강성찬 개인 확장: 댓글·답글 좋아요. 글 좋아요(015)와 같은 규칙: 상태 지정 방식, 로그인 → 인증 → 글 볼 수 있음 → 댓글이 보임(404)
 * → 자기 댓글(400) → 요청 횟수(429, 글 좋아요와 같은 한도를 함께 쓴다).
 */
@Service
public class CommentLikeService {

    private final AccountGuard accountGuard;
    private final PostReadAccess postReadAccess;
    private final CommentQuery query;
    private final CommentProperties properties;
    private final RedisRateLimiter rateLimiter;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CommentLikeService(AccountGuard accountGuard, PostReadAccess postReadAccess, CommentQuery query,
                              CommentProperties properties, RedisRateLimiter rateLimiter, JdbcTemplate jdbc,
                              TransactionTemplate transactionTemplate, ApplicationEventPublisher events, Clock clock) {
        this.accountGuard = accountGuard;
        this.postReadAccess = postReadAccess;
        this.query = query;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.events = events;
        this.clock = clock;
    }

    public LikeService.LikeResult set(Optional<CurrentUser> currentUser, long commentId, boolean like) {
        if (!properties.likeEnabled()) {
            throw new NotFoundException();
        }
        CurrentUser user = accountGuard.requireWritable(currentUser);
        record Target(long postId, long authorId) {
        }
        Target t = jdbc.query("SELECT post_id, author_id FROM comment WHERE id = ?",
                (rs, n) -> new Target(rs.getLong("post_id"), rs.getLong("author_id")), commentId)
                .stream().findFirst().orElseThrow(NotFoundException::new);
        postReadAccess.requireWritableTarget(currentUser, t.postId());
        // 삭제·숨김·볼 수 없는 비밀 댓글은 없는 것처럼
        if (!"NORMAL".equals(query.single(currentUser, commentId).state())) {
            throw new NotFoundException();
        }
        if (t.authorId() == user.memberId()) {
            throw new CannotLikeOwnPostException();
        }
        RedisRateLimiter.Result limit = rateLimiter.tryAcquire(
                RedisRateLimiter.key("like:member", String.valueOf(user.memberId())), 60, Duration.ofMinutes(1));
        if (!limit.allowed()) {
            throw new RateLimitedException(limit.retryAfterSeconds());
        }
        return transactionTemplate.execute(status -> {
            int changed = like
                    ? jdbc.update("INSERT INTO comment_like (comment_id, member_id, created_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                            commentId, user.memberId(), Timestamp.from(clock.instant()))
                    : jdbc.update("DELETE FROM comment_like WHERE comment_id = ? AND member_id = ?", commentId, user.memberId());
            if (changed == 1) {
                events.publishEvent(like ? new CommentLiked(commentId, t.postId(), user.memberId(), t.authorId())
                        : new CommentUnliked(commentId, user.memberId(), t.authorId()));
            }
            Integer count = jdbc.queryForObject("SELECT count(*) FROM comment_like WHERE comment_id = ?", Integer.class, commentId);
            return new LikeService.LikeResult(like, count == null ? 0 : count);
        });
    }
}

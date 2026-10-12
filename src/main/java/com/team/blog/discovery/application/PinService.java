package com.team.blog.discovery.application;

import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 054 글 고정(강성찬 개인 확장): 블로그당 한 글을 맨 위에. 내 발행 글만, 다른 글을 고정하면 바뀐다. 목록에는 공개 글일 때만 보인다. */
@Service
public class PinService {

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;

    public PinService(JdbcTemplate jdbc, AccountGuard accountGuard) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
    }

    public void pin(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireWritable(current);
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND status = 'PUBLISHED'"
                + " AND deleted_at IS NULL", Integer.class, postId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
        jdbc.update("""
                INSERT INTO post_pin (member_id, post_id) VALUES (?, ?)
                ON CONFLICT (member_id) DO UPDATE SET post_id = EXCLUDED.post_id, pinned_at = CURRENT_TIMESTAMP
                """, me.memberId(), postId);
    }

    public void unpin(Optional<CurrentUser> current) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        jdbc.update("DELETE FROM post_pin WHERE member_id = ?", me.memberId());
    }

    public Optional<Long> pinnedPostId(long memberId) {
        return jdbc.queryForList("SELECT post_id FROM post_pin WHERE member_id = ?", Long.class, memberId).stream().findFirst();
    }
}

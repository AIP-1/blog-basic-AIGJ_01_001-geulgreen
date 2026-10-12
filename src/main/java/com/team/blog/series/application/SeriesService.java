package com.team.blog.series.application;

import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 041 시리즈(강성찬 개인 확장): 내 글을 이름 붙인 묶음에 넣은 순서대로. 글은 시리즈 0~1개, 비면 시리즈도 지운다.
 * 목록·편 번호는 공용 목록 조건(012) 글만 센다.
 */
@Service
public class SeriesService {

    public record Series(long id, String title, long postCount) {
    }

    public record Item(long id, String title, String handle) {

        public String url() {
            return "/@" + handle + "/posts/" + id;
        }
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final PostAccessPolicy accessPolicy;
    private final SeriesProperties properties;

    public SeriesService(JdbcTemplate jdbc, AccountGuard accountGuard, PostAccessPolicy accessPolicy,
                         SeriesProperties properties) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 블로그의 시리즈(공개 글 수 포함, 최근에 만든 순). {@code includeEmpty}: 주인에게는 공개 글이 없는 것도. */
    public List<Series> list(long memberId, boolean includeEmpty) {
        List<Series> all = jdbc.query("SELECT s.id, s.title, (SELECT count(*) FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " JOIN member m ON m.id = p.author_id WHERE sp.series_id = s.id AND "
                        + accessPolicy.publicListingCondition("p", "m") + ") AS cnt FROM series s WHERE s.member_id = ?"
                        + " ORDER BY s.id DESC",
                (rs, i) -> new Series(rs.getLong("id"), rs.getString("title"), rs.getLong("cnt")), memberId);
        return includeEmpty ? all : all.stream().filter(s -> s.postCount() > 0).toList();
    }

    public Optional<Series> find(long memberId, long seriesId) {
        return list(memberId, true).stream().filter(s -> s.id() == seriesId).findFirst();
    }

    /** 시리즈의 공개 글(넣은 순서). */
    public List<Item> items(long seriesId) {
        return jdbc.query("SELECT p.id, p.title, m.handle FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " JOIN member m ON m.id = p.author_id WHERE sp.series_id = ? AND "
                        + accessPolicy.publicListingCondition("p", "m") + " ORDER BY sp.position, p.id",
                (rs, i) -> new Item(rs.getLong("id"), rs.getString("title"), rs.getString("handle")), seriesId);
    }

    /** 글이 든 시리즈(없으면 빈 값). */
    public Optional<Series> ofPost(long postId) {
        return jdbc.query("SELECT s.id, s.title FROM series_post sp JOIN series s ON s.id = sp.series_id WHERE sp.post_id = ?",
                (rs, i) -> new Series(rs.getLong("id"), rs.getString("title"), 0), postId).stream().findFirst();
    }

    /**
     * 내 글을 시리즈에 넣는다. {@code seriesId}가 있으면 그 시리즈, 없으면 {@code newTitle}로 만들거나 같은 이름 시리즈를 쓴다.
     * 다른 시리즈에 있던 글이면 옮긴다(맨 끝).
     */
    @Transactional
    public long add(Optional<CurrentUser> current, long postId, Long seriesId, String newTitle) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMyPost(me, postId);
        long target;
        if (seriesId != null) {
            Integer mine = jdbc.queryForObject("SELECT count(*) FROM series WHERE id = ? AND member_id = ?", Integer.class,
                    seriesId, me.memberId());
            if (mine == null || mine == 0) {
                throw new NotFoundException();
            }
            target = seriesId;
        } else {
            String title = newTitle == null ? "" : newTitle.strip();
            if (title.isEmpty() || title.codePointCount(0, title.length()) > 60) {
                throw new PostContentException("SERIES_TITLE_INVALID");
            }
            jdbc.update("INSERT INTO series (member_id, title) VALUES (?, ?) ON CONFLICT (member_id, title) DO NOTHING",
                    me.memberId(), title);
            target = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ? AND title = ?", Long.class,
                    me.memberId(), title);
        }
        Optional<Series> before = ofPost(postId);
        if (before.isPresent() && before.get().id() == target) {
            return target;
        }
        jdbc.update("DELETE FROM series_post WHERE post_id = ?", postId);
        jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, "
                + "(SELECT COALESCE(max(position), 0) + 1 FROM series_post WHERE series_id = ?))", postId, target, target);
        before.ifPresent(s -> dropIfEmpty(s.id()));
        return target;
    }

    @Transactional
    public void remove(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        requireMyPost(me, postId);
        Optional<Series> before = ofPost(postId);
        jdbc.update("DELETE FROM series_post WHERE post_id = ?", postId);
        before.ifPresent(s -> dropIfEmpty(s.id()));
    }

    /** 탈퇴 정리. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM series WHERE member_id = ?", memberId);
    }

    private void dropIfEmpty(long seriesId) {
        jdbc.update("DELETE FROM series s WHERE s.id = ? AND NOT EXISTS (SELECT 1 FROM series_post sp WHERE sp.series_id = s.id)",
                seriesId);
    }

    private void requireMyPost(CurrentUser me, long postId) {
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
    }
}

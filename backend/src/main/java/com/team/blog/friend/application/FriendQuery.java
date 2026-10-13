package com.team.blog.friend.application;

import com.team.blog.account.application.ProfileAvatar;
import com.team.blog.discovery.application.SearchTerms;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 친한 친구 읽기(069). 한쪽 관계다: owner가 friend를 고르면 friend가 owner의 친구 공개 글을 읽는다. */
@Service
public class FriendQuery {

    /** 보는 사람 기준 상대: 고를 수 없음 / 내가 팔로우해서 고를 수 있음 / 이미 내 친구. */
    public enum Status { NONE, FOLLOWING, FRIENDS }

    public record Person(long memberId, String handle, String nickname, String profileImageUrl) {

        public ProfileAvatar avatar() {
            return new ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    private final JdbcTemplate jdbc;

    public FriendQuery(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code owner}가 {@code friend}를 친구로 골랐나(= friend가 owner의 친구 공개 글을 읽나). */
    public boolean hasFriend(long owner, long friend) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM close_friend WHERE owner_id = ? AND friend_id = ?)", Boolean.class, owner, friend));
    }

    public Status status(long viewer, long other) {
        if (viewer == other) {
            return Status.NONE;
        }
        if (hasFriend(viewer, other)) {
            return Status.FRIENDS;
        }
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM follow WHERE follower_id = ? AND followee_id = ?)", Boolean.class, viewer, other))
                ? Status.FOLLOWING : Status.NONE;
    }

    /** 내가 고른 친구(최근에 넣은 순). 탈퇴 유예·익명 회원은 뺀다. */
    public List<Person> friends(long me) {
        return people("""
                FROM close_friend cf JOIN member m ON m.id = cf.friend_id
                WHERE cf.owner_id = ? AND m.withdrawn_at IS NULL ORDER BY cf.created_at DESC, m.id DESC
                """, me);
    }

    /** 내가 팔로우하지만 아직 친구로 넣지 않은 사람(최근에 팔로우한 순). {@code q}가 있으면 주소·별명 부분 일치로 찾는다. */
    public List<Person> candidates(long me, String q) {
        String term = q == null ? "" : q.strip();
        return people("""
                FROM follow f JOIN member m ON m.id = f.followee_id
                WHERE f.follower_id = ? AND m.withdrawn_at IS NULL
                  AND NOT EXISTS (SELECT 1 FROM close_friend cf WHERE cf.owner_id = f.follower_id AND cf.friend_id = f.followee_id)
                  AND (? = '' OR m.handle ILIKE ? ESCAPE '\\' OR m.nickname ILIKE ? ESCAPE '\\')
                ORDER BY f.created_at DESC, m.id DESC
                """, me, term, SearchTerms.likePattern(term), SearchTerms.likePattern(term));
    }

    // ponytail: 한 화면에 1000명까지만 그린다(친구 수 제한은 없음). 넘는 사람이 생기면 쪽 나누기.
    private List<Person> people(String from, Object... args) {
        return jdbc.query("SELECT m.id, m.handle, m.nickname, m.profile_image_url " + from + " LIMIT 1000",
                (rs, n) -> new Person(rs.getLong("id"), rs.getString("handle"), rs.getString("nickname"),
                        rs.getString("profile_image_url")), args);
    }
}

package com.team.blog.discovery.application;

import com.team.blog.account.application.ProfileAvatar;
import com.team.blog.post.application.PostAccessPolicy;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 피드 "추천 블로그"(068, 강성찬 개인 확장): 내가 아직 팔로우하지 않은 작가 중 최근 30일에 공개 글을 쓴 사람을 먼저,
 * 그다음 공개 글 좋아요 합이 많은 순으로 보여 준다. 공용 목록 조건을 거친 글만 센다.
 */
@Service
public class RecommendedBlogQuery {

    static final int LIMIT = 6;
    static final Duration ACTIVE_WINDOW = Duration.ofDays(30);

    public record Blog(String handle, String nickname, String profileImageUrl, String bio) {

        public ProfileAvatar avatar() {
            return new ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;
    private final Clock clock;

    public RecommendedBlogQuery(JdbcTemplate jdbc, PostAccessPolicy accessPolicy, Clock clock) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    public List<Blog> forMember(long memberId) {
        Timestamp since = Timestamp.from(clock.instant().minus(ACTIVE_WINDOW));
        return jdbc.query("""
                SELECT m.handle, m.nickname, m.profile_image_url, m.bio
                FROM post p JOIN member m ON m.id = p.author_id
                WHERE """ + " " + accessPolicy.publicListingCondition("p", "m") + """
                  AND p.author_id <> ?
                  AND p.author_id NOT IN (SELECT f.followee_id FROM follow f WHERE f.follower_id = ?)
                GROUP BY m.id, m.handle, m.nickname, m.profile_image_url, m.bio
                ORDER BY max(p.first_public_at) >= ? DESC, sum(p.like_count) DESC, max(p.first_public_at) DESC, m.id
                LIMIT """ + " " + LIMIT,
                (rs, n) -> new Blog(rs.getString("handle"), rs.getString("nickname"),
                        rs.getString("profile_image_url"), rs.getString("bio")),
                memberId, memberId, since);
    }
}

package com.team.blog.discovery.application;

import com.team.blog.shared.security.CurrentUser;
import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 051 방문자 수(강성찬 개인 확장, 티스토리 참고): 블로그(첫 화면·글 화면)를 본 사람을 하루 한 번 센다.
 * 같은 사람 판정은 조회수(016)와 같은 방문자 키, 블로그 주인 자신은 세지 않는다. Redis가 멈추면 세지 않고 지나간다.
 */
@Service
public class BlogVisitService {

    @ConfigurationProperties("blog.visits")
    public record Properties(@DefaultValue("true") boolean enabled) {
    }

    public record Counts(long today, long total) {
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final VisitorKeys visitorKeys;
    private final Properties properties;
    private final Clock clock;

    public BlogVisitService(JdbcTemplate jdbc, StringRedisTemplate redis, VisitorKeys visitorKeys, Properties properties,
                            Clock clock) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.visitorKeys = visitorKeys;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public void recordVisit(long ownerId, Optional<CurrentUser> viewer, Optional<String> cookie, String ip, String userAgent) {
        if (!properties.enabled() || viewer.map(v -> v.memberId() == ownerId).orElse(false)) {
            return;
        }
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        String key = "blogvisit:" + ownerId + ":" + today;
        try {
            Long added = redis.opsForSet().add(key, visitorKeys.key(viewer, cookie, ip, userAgent));
            redis.expire(key, Duration.ofDays(2));
            if (added == null || added == 0) {
                return;
            }
        } catch (DataAccessException e) {
            return;
        }
        jdbc.update("""
                INSERT INTO blog_visit_daily (member_id, day, visitors) VALUES (?, ?, 1)
                ON CONFLICT (member_id, day) DO UPDATE SET visitors = blog_visit_daily.visitors + 1
                """, ownerId, Date.valueOf(today));
    }

    public Counts counts(long ownerId) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        return jdbc.queryForObject("""
                SELECT COALESCE(sum(visitors) FILTER (WHERE day = ?), 0) AS today, COALESCE(sum(visitors), 0) AS total
                FROM blog_visit_daily WHERE member_id = ?
                """, (rs, i) -> new Counts(rs.getLong("today"), rs.getLong("total")), Date.valueOf(today), ownerId);
    }
}

package com.team.blog.support;

import java.util.List;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/** 테스트마다 V1의 20개 테이블을 비우고 Redis {@code FLUSHDB}. */
@TestComponent
public class DatabaseCleaner {

    static final List<String> TABLES = List.of(
            "member", "image", "auth_identity", "member_agreement", "tag", "post", "post_draft", "post_like",
            "post_view_daily", "post_tag", "post_image", "comment", "follow", "friendship", "close_friend", "private_account", "follow_request", "report_case", "report",
            "member_suspension", "notification", "notification_actor", "notification_mute",
            // 강성찬 개인 확장(029~035)
            "post_link_share", "friend_group", "group_member", "post_group_visibility", "short_note", "mission",
            "mission_participant", "post_teaser",
            // 강성찬 개인 확장(037~043)
            "category", "post_category", "guestbook_entry", "series", "series_post", "post_schedule",
            // 강성찬 개인 확장(049~050)
            "site_notice", "admin_log",
            // 강성찬 개인 확장(051·054)
            "blog_visit_daily", "post_pin",
            // 강성찬 개인 확장(055)
            "post_cover",
            // 강성찬 개인 확장(056)
            "tag_icon",
            // 강성찬 개인 확장(057)
            "daily_topic", "mission_post");

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public DatabaseCleaner(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    public void clean() {
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", TABLES) + " RESTART IDENTITY CASCADE");
        redis.execute((RedisConnection connection) -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }
}

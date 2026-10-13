package com.team.blog.friend;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.support.IntegrationTestBase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** 069 V15·V16: 맞친구(025)는 양쪽 친한 친구로 옮기고, 대기 중 요청과 그 알림은 지운다. V16은 내가 팔로우하지 않는 친구를 뺀다. 앱이 쓰는 스키마와 따로 빈 스키마에서 V14 → V15. */
class CloseFriendMigrationIT extends IntegrationTestBase {

    @Test
    void acceptedPairsBecomeBothWaysAndPendingRequestsAreDropped() throws Exception {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS v15_check CASCADE");
            s.execute("CREATE SCHEMA v15_check");
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("v15_check").target("14").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(
                POSTGRES.getJdbcUrl() + "&currentSchema=v15_check,public", POSTGRES.getUsername(), POSTGRES.getPassword(), true));
        long[] ids = new long[3];
        for (int i = 0; i < 3; i++) {
            ids[i] = members.localMember("mig" + i, "mig" + i, "mig" + i + "@example.com", "Blog#2026ok", true);
            jdbc.update("INSERT INTO member OVERRIDING SYSTEM VALUE SELECT * FROM public.member WHERE id = ?", ids[i]);
        }
        jdbc.update("INSERT INTO friendship (member_a_id, member_b_id, requested_by, status, created_at, accepted_at) "
                + "VALUES (?, ?, ?, 'ACCEPTED', now(), now())", ids[0], ids[1], ids[0]);
        jdbc.update("INSERT INTO friendship (member_a_id, member_b_id, requested_by, status, created_at) "
                + "VALUES (?, ?, ?, 'PENDING', now())", ids[0], ids[2], ids[2]);
        jdbc.update("INSERT INTO notification (receiver_id, type, group_key, last_actor_id, actor_count, created_at, updated_at) "
                + "VALUES (?, 'FRIEND_REQUEST', 'FRIEND_REQUEST', ?, 1, now(), now())", ids[0], ids[2]);

        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("v15_check").target("15").load().migrate();

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT owner_id, friend_id FROM close_friend ORDER BY owner_id");
        assertThat(rows).containsExactly(Map.of("owner_id", ids[0], "friend_id", ids[1]), Map.of("owner_id", ids[1], "friend_id", ids[0]));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM friendship", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE type = 'FRIEND_REQUEST'", Integer.class)).isZero();

        // V16: 내가 팔로우하는 사람만 남는다(0은 1을 팔로우, 1은 0을 팔로우하지 않음)
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", ids[0], ids[1]);
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("v15_check").target("16").load().migrate();
        assertThat(jdbc.queryForList("SELECT owner_id, friend_id FROM close_friend"))
                .containsExactly(Map.of("owner_id", ids[0], "friend_id", ids[1]));
        jdbc.execute("DROP SCHEMA v15_check CASCADE");
    }
}

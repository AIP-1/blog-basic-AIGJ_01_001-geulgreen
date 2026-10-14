package com.team.blog.connect.application;

import com.team.blog.account.infra.KeyHashing;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 097 AI 연결 키. 원문은 발급할 때 한 번만 돌려주고 DB에는 SHA-256만 둔다. 키는 그 회원으로 MCP 도구를 부른다.
 */
@Service
public class AiKeyService {

    /** 원문 앞머리. 비밀 검색 도구가 알아보기 쉽게. */
    public static final String PREFIX = "ggk_";
    public static final int NAME_MAX = 40;

    public record Key(long id, String name, Instant createdAt, Instant lastUsedAt) {
    }

    /** 인증된 키: 키 번호(요청 제한용)와 주인. */
    public record Owner(long keyId, long memberId) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final AiConnectProperties properties;
    private final Clock clock;

    public AiKeyService(JdbcTemplate jdbc, AccountGuard accountGuard, AiConnectProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public int maxKeys() {
        return properties.maxKeys();
    }

    public List<Key> list(long memberId) {
        return jdbc.query("SELECT id, name, created_at, last_used_at FROM ai_key WHERE member_id = ? ORDER BY id",
                (rs, i) -> new Key(rs.getLong("id"), rs.getString("name"), rs.getTimestamp("created_at").toInstant(),
                        Optional.ofNullable(rs.getTimestamp("last_used_at")).map(Timestamp::toInstant).orElse(null)),
                memberId);
    }

    /** 새 키를 만들고 원문을 돌려준다. 키가 이미 {@code maxKeys}개면 빈 값. 글을 쓸 수 있는 회원만(인증 전·탈퇴 유예는 403). */
    @Transactional
    public Optional<String> issue(Optional<CurrentUser> current, String rawName) {
        long me = accountGuard.requireWritable(current).memberId();
        jdbc.queryForObject("SELECT id FROM member WHERE id = ? FOR UPDATE", Long.class, me); // 동시 발급이 개수 제한을 넘지 않게
        Integer count = jdbc.queryForObject("SELECT count(*) FROM ai_key WHERE member_id = ?", Integer.class, me);
        if (count != null && count >= properties.maxKeys()) {
            return Optional.empty();
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO ai_key (member_id, name, key_hash, created_at) VALUES (?, ?, ?, ?)",
                me, name(rawName), KeyHashing.sha256(raw), Timestamp.from(clock.instant()));
        return Optional.of(raw);
    }

    /** 키 끊기. 남의 키·없는 키는 404. */
    public void revoke(Optional<CurrentUser> current, long keyId) {
        long me = accountGuard.requireLoggedIn(current).memberId();
        if (jdbc.update("DELETE FROM ai_key WHERE id = ? AND member_id = ?", keyId, me) == 0) {
            throw new NotFoundException();
        }
    }

    /** 원문으로 주인을 찾는다. 찾으면 마지막 사용 시각을 고친다(같은 분 안에서는 쓰지 않음). */
    public Optional<Owner> authenticate(String raw) {
        if (raw == null || !raw.startsWith(PREFIX) || raw.length() > 100) {
            return Optional.empty();
        }
        Optional<Owner> owner = jdbc.query("SELECT id, member_id FROM ai_key WHERE key_hash = ?",
                (rs, i) -> new Owner(rs.getLong("id"), rs.getLong("member_id")), KeyHashing.sha256(raw)).stream().findFirst();
        owner.ifPresent(o -> {
            Instant now = clock.instant();
            jdbc.update("UPDATE ai_key SET last_used_at = ? WHERE id = ? AND (last_used_at IS NULL OR last_used_at < ?)",
                    Timestamp.from(now), o.keyId(), Timestamp.from(now.minusSeconds(60)));
        });
        return owner;
    }

    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM ai_key WHERE member_id = ?", memberId);
    }

    static String name(String raw) {
        String n = raw == null ? "" : raw.strip();
        if (n.isEmpty()) {
            return "AI 연결";
        }
        return n.codePointCount(0, n.length()) > NAME_MAX ? n.substring(0, n.offsetByCodePoints(0, NAME_MAX)) : n;
    }
}

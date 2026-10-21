package com.team.blog.account.application;

import com.team.blog.account.infra.KeyHashing;
import com.team.blog.shared.event.GuardianConsentRequested;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 10-21 chan: 14세 미만 가입의 보호자(법정대리인) 동의(개인정보 보호법 제22조의2).
 * <ul>
 *   <li>가입할 때 보호자 이름·이메일만 더 받는다(동의를 받기 위한 최소 정보, 시행령 제17조의2 제2항).</li>
 *   <li>보호자에게 동의 내용을 적은 메일을 보내고, 보호자가 메일 속 링크에서 [동의해요]를 눌러야 계정이 열린다.</li>
 *   <li>5일 안에 동의하지 않거나 [동의하지 않아요]를 누르면 계정과 보호자 정보를 지운다(탈퇴 정리 단계를 그대로 쓴다).</li>
 * </ul>
 */
@Service
public class GuardianConsentService {

    private static final Logger log = LoggerFactory.getLogger(GuardianConsentService.class);
    /** 동의 링크가 살아 있는 기간. 지나면 받은 정보를 지운다. */
    public static final Duration TTL = Duration.ofDays(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 동의 화면에 보여 줄 것. */
    public record Pending(long memberId, String childNickname, String childHandle, String guardianName, Instant expiresAt) {
    }

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    // 탈퇴 정리 단계 중 몇은 블로그 주인 찾기(이 서비스를 씀)를 거쳐서, 순환을 피하려고 지울 때 꺼낸다
    private final org.springframework.beans.factory.ObjectProvider<WithdrawalPurgeStep> purgeSteps;
    private final TransactionTemplate tx;

    public GuardianConsentService(JdbcTemplate jdbc, ApplicationEventPublisher events, Clock clock,
                                  org.springframework.beans.factory.ObjectProvider<WithdrawalPurgeStep> purgeSteps,
                                  TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.events = events;
        this.clock = clock;
        this.purgeSteps = purgeSteps;
        this.tx = tx;
    }

    /** 보호자 정보 검사. 문제가 있으면 화면에 보여 줄 문장. */
    public static Optional<String> problem(String guardianName, String guardianEmail, String childEmail) {
        String name = guardianName == null ? "" : guardianName.strip();
        if (name.isEmpty() || name.length() > 30) {
            return Optional.of("보호자 이름을 적어 주세요");
        }
        String email;
        try {
            email = EmailRules.normalizeAndValidate(guardianEmail);
        } catch (RuntimeException e) {
            return Optional.of("보호자 이메일을 바르게 적어 주세요");
        }
        if (childEmail != null && email.equals(EmailRules.normalize(childEmail))) {
            return Optional.of("보호자 이메일은 내 이메일과 달라야 해요");
        }
        return Optional.empty();
    }

    /** 가입 트랜잭션 안에서: 동의 요청을 남기고, 커밋 뒤 보호자에게 메일이 나간다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void request(long memberId, String guardianName, String guardianEmail, String childNickname) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expires = now.plus(TTL);
        String email = EmailRules.normalizeAndValidate(guardianEmail);
        jdbc.update("""
                INSERT INTO guardian_consent (member_id, guardian_name, guardian_email, token_hash, requested_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, memberId, guardianName.strip(), email, KeyHashing.sha256(raw), Timestamp.from(now), Timestamp.from(expires));
        events.publishEvent(new GuardianConsentRequested(memberId, email, guardianName.strip(), childNickname, raw, expires));
    }

    /** 보호자 동의를 기다리는 계정인지(로그인을 막는다). */
    public boolean pending(long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM guardian_consent WHERE member_id = ? AND consented_at IS NULL)", Boolean.class, memberId));
    }

    /** 링크로 연 동의 요청(아직 동의 전이고 기한 안). */
    public Optional<Pending> find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return jdbc.query("""
                SELECT g.member_id, m.nickname, m.handle, g.guardian_name, g.expires_at FROM guardian_consent g
                JOIN member m ON m.id = g.member_id
                WHERE g.token_hash = ? AND g.consented_at IS NULL AND g.expires_at > ?
                """, (rs, i) -> new Pending(rs.getLong("member_id"), rs.getString("nickname"), rs.getString("handle"),
                rs.getString("guardian_name"), rs.getTimestamp("expires_at").toInstant()),
                KeyHashing.sha256(rawToken), Timestamp.from(clock.instant())).stream().findFirst();
    }

    /** 보호자가 [동의해요]: 계정이 열리고 동의 기록(GUARDIAN)이 남는다. */
    @Transactional
    public boolean consent(String rawToken) {
        Optional<Pending> p = find(rawToken);
        if (p.isEmpty()) {
            return false;
        }
        Timestamp now = Timestamp.from(clock.instant());
        int n = jdbc.update("UPDATE guardian_consent SET consented_at = ? WHERE member_id = ? AND consented_at IS NULL",
                now, p.get().memberId());
        if (n > 0) {
            jdbc.update("INSERT INTO member_agreement (member_id, type, agreed_at) VALUES (?, 'GUARDIAN', ?)", p.get().memberId(), now);
        }
        return n > 0;
    }

    /** 보호자가 [동의하지 않아요]: 계정과 보호자 정보를 지운다. */
    public boolean decline(String rawToken) {
        Optional<Pending> p = find(rawToken);
        p.ifPresent(x -> erase(x.memberId()));
        return p.isPresent();
    }

    /** 기한이 지난 동의 요청을 지운다(매시 정각). */
    @Scheduled(cron = "${blog.guardian.purge-cron:0 0 * * * *}", zone = "Asia/Seoul")
    public int purgeExpired() {
        List<Long> due = jdbc.queryForList("SELECT member_id FROM guardian_consent WHERE consented_at IS NULL AND expires_at <= ?",
                Long.class, Timestamp.from(clock.instant()));
        due.forEach(this::erase);
        return due.size();
    }

    private void erase(long memberId) {
        try {
            tx.executeWithoutResult(s -> {
                List<Long> locked = jdbc.queryForList(
                        "SELECT member_id FROM guardian_consent WHERE member_id = ? AND consented_at IS NULL FOR UPDATE", Long.class, memberId);
                if (!locked.isEmpty()) {
                    // 탈퇴와 같은 길로 지운다(블로그 주소·로그인이 사라짐)
                    Timestamp now = Timestamp.from(clock.instant());
                    jdbc.update("UPDATE member SET status = 'WITHDRAWN', withdrawn_at = COALESCE(withdrawn_at, ?) WHERE id = ?", now, memberId);
                    purgeSteps.stream().sorted(Comparator.comparingInt(WithdrawalPurgeStep::order)).forEach(step -> step.purge(memberId));
                    jdbc.update("DELETE FROM guardian_consent WHERE member_id = ?", memberId);
                }
            });
        } catch (RuntimeException e) {
            log.warn("보호자 동의 정리 실패(다음 실행에서 다시): memberId={} cause={}", memberId, e.getClass().getSimpleName());
        }
    }
}

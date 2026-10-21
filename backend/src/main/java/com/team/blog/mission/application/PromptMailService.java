package com.team.blog.mission.application;

import com.team.blog.account.application.AuthProperties;
import com.team.blog.account.application.MailSender;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 강성찬 개인 확장(10-21): 미션 대신 "오늘의 글감". 글쓰기 화면에 글감 한 줄, 켠 사람에게만 그날 글을 안 쓴 날 저녁 메일 한 통(보낸 사람 글그린).
 * 휴대폰 푸시·리그는 없다.
 */
@Service
public class PromptMailService {

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final MissionService missions;
    private final MailSender mailSender;
    private final AuthProperties auth;

    public PromptMailService(JdbcTemplate jdbc, AccountGuard accountGuard, MissionService missions, MailSender mailSender,
                             AuthProperties auth) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.missions = missions;
        this.mailSender = mailSender;
        this.auth = auth;
    }

    /** 오늘 이 사람에게 보여 줄 글감 하나(사람마다 다른 칸, 날마다 바뀜). */
    public Optional<MissionService.Topic> todayFor(long memberId) {
        if (!missions.enabled()) {
            return Optional.empty();
        }
        List<MissionService.Topic> topics = missions.topics(missions.today());
        return topics.isEmpty() ? Optional.empty() : Optional.of(topics.get((int) Math.floorMod(memberId, topics.size())));
    }

    public boolean subscribed(long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM prompt_mail WHERE member_id = ?)", Boolean.class, memberId));
    }

    public void set(Optional<CurrentUser> current, boolean on) {
        long me = accountGuard.requireLoggedIn(current).memberId();
        if (on) {
            jdbc.update("INSERT INTO prompt_mail (member_id) VALUES (?) ON CONFLICT DO NOTHING", me);
        } else {
            jdbc.update("DELETE FROM prompt_mail WHERE member_id = ?", me);
        }
    }

    /**
     * 저녁 작업: 켠 사람 중 오늘(한국 시간) 처음 발행한 글이 없고 오늘 아직 안 받은 사람에게 한 통. 보내기 전에 오늘 보냄으로 적어서 두 번 가지 않게.
     *
     * @return 보낸 수
     */
    public int sendEvening() {
        LocalDate today = missions.today();
        List<Map<String, Object>> targets = jdbc.queryForList("""
                SELECT DISTINCT ON (m.id) m.id, a.email FROM prompt_mail pm
                JOIN member m ON m.id = pm.member_id AND m.status = 'ACTIVE'
                JOIN auth_identity a ON a.member_id = m.id AND a.email IS NOT NULL
                WHERE pm.last_sent_on IS DISTINCT FROM ?
                  AND NOT EXISTS (SELECT 1 FROM post p WHERE p.author_id = m.id AND p.published_at IS NOT NULL AND p.deleted_at IS NULL
                                  AND (p.published_at AT TIME ZONE 'Asia/Seoul')::date = ?)
                ORDER BY m.id, a.email_verified_at DESC NULLS LAST
                """, Date.valueOf(today), Date.valueOf(today));
        int sent = 0;
        for (Map<String, Object> t : targets) {
            long id = ((Number) t.get("id")).longValue();
            Optional<MissionService.Topic> topic = todayFor(id);
            if (topic.isEmpty() || jdbc.update("UPDATE prompt_mail SET last_sent_on = ? WHERE member_id = ? AND last_sent_on IS DISTINCT FROM ?",
                    Date.valueOf(today), id, Date.valueOf(today)) == 0) {
                continue;
            }
            String base = auth.mail().linkBaseUrl();
            mailSender.send((String) t.get("email"), "[글그린] 오늘의 글감: " + topic.get().title(), "mail/prompt",
                    Map.of("title", topic.get().title(), "hint", topic.get().hint(), "writeLink", base + "/missions",
                            "settingsLink", base + "/settings#prompt-mail-heading"));
            sent++;
        }
        return sent;
    }

    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM prompt_mail WHERE member_id = ?", memberId);
    }
}

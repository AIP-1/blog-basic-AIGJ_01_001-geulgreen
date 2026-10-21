package com.team.blog.account.application;

import com.team.blog.shared.event.GuardianConsentRequested;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 10-21 chan: 보호자 동의 메일(커밋 후). 무엇을·왜·얼마나 보관하는지 적고, 동의·거절 링크를 담는다. */
@Component
public class GuardianConsentMailListener {

    private static final Logger log = LoggerFactory.getLogger(GuardianConsentMailListener.class);
    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final MailSender mailSender;
    private final AuthProperties properties;

    public GuardianConsentMailListener(MailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(GuardianConsentRequested event) {
        try {
            String link = properties.mail().linkBaseUrl() + "/auth/guardian/consent?token=" + event.rawToken();
            mailSender.send(event.guardianEmail(), "[글그린] " + event.childNickname() + "님의 가입에 보호자 동의가 필요해요", "mail/guardian-consent",
                    Map.of("link", link, "guardianName", event.guardianName(), "childNickname", event.childNickname(),
                            "deadline", DEADLINE.format(event.expiresAt())));
        } catch (RuntimeException e) {
            log.warn("보호자 동의 메일 실패: memberId={} cause={}", event.memberId(), e.getClass().getSimpleName());
        }
    }
}

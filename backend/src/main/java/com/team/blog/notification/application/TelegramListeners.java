package com.team.blog.notification.application;

import com.team.blog.shared.event.InquiryReceived;
import com.team.blog.shared.event.MemberSignedUp;
import com.team.blog.shared.event.ReportReceived;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 096 강성찬 개인 확장: 운영 사건 → 관리자 텔레그램. 사이트 알림과 따로 돌고(커밋 뒤, 알림 대기열), 보내기가 실패해도 기록만 한다.
 * 문의 내용·이메일은 보내지 않는다(제목과 관리 화면 링크만).
 */
@Component
public class TelegramListeners {

    private final TelegramService telegram;
    private final NotificationDispatcher dispatcher;
    private final JdbcTemplate jdbc;

    public TelegramListeners(TelegramService telegram, NotificationDispatcher dispatcher, JdbcTemplate jdbc) {
        this.telegram = telegram;
        this.dispatcher = dispatcher;
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(InquiryReceived e) {
        if (telegram.active()) {
            dispatcher.submit("telegram inquiry " + e.inquiryId(), () -> telegram.toAdmins("📮 새 문의: "
                    + jdbc.queryForObject("SELECT title FROM inquiry WHERE id = ?", String.class, e.inquiryId())
                    + "\n" + telegram.siteOrigin() + "/admin/inquiries/" + e.inquiryId()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MemberSignedUp e) {
        if (telegram.active()) {
            dispatcher.submit("telegram signup " + e.memberId(), () -> telegram.toAdmins("🙋 새 회원 가입: "
                    + jdbc.queryForObject("SELECT handle FROM member WHERE id = ?", String.class, e.memberId())
                    + " (" + e.provider() + ")"));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ReportReceived e) {
        if (telegram.active()) {
            dispatcher.submit("telegram report", () -> telegram.toAdmins("🚨 새 신고: "
                    + ("POST".equals(e.targetType()) ? "글" : "댓글") + ", " + e.reasonLabel()
                    + "\n" + telegram.siteOrigin() + "/admin/reports"));
        }
    }
}

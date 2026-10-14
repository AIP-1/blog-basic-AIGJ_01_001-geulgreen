package com.team.blog.notification.web;

import com.team.blog.notification.application.TelegramService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUserProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.view.RedirectView;
import tools.jackson.databind.JsonNode;

/** 096 강성찬 개인 확장: 봇 웹훅(텔레그램 → 사이트)과 설정 화면의 [텔레그램 연결]·[연결 끊기] 폼. 꺼져 있으면 404. */
@Controller
public class TelegramController {

    private final TelegramService telegram;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;

    public TelegramController(TelegramService telegram, AccountGuard accountGuard, CurrentUserProvider currentUserProvider) {
        this.telegram = telegram;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
    }

    /** 텔레그램만 아는 확인값 헤더가 맞을 때만 처리한다. 처리 실패에도 200을 줘서 텔레그램이 같은 메시지를 다시 보내지 않게 한다. */
    @PostMapping("/api/telegram/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader(name = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
                                        @RequestBody JsonNode update) {
        if (!telegram.active()) {
            throw new NotFoundException();
        }
        if (secret == null || !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),
                telegram.webhookSecret().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        telegram.handle(update);
        return ResponseEntity.ok().build();
    }

    /** 일회용 코드를 만들어 봇 링크로 보낸다(봇 앱이 열리고 [시작]을 누르면 연결). */
    @PostMapping("/settings/telegram/link")
    public RedirectView link() {
        long memberId = accountGuard.requireLoggedIn(currentUserProvider.current()).memberId();
        if (!telegram.active()) {
            throw new NotFoundException();
        }
        return seeOther(telegram.linkUrl(memberId).orElse("/settings?telegram=fail#settings-notifications"));
    }

    @PostMapping("/settings/telegram/unlink")
    public RedirectView unlink() {
        telegram.unlink(accountGuard.requireLoggedIn(currentUserProvider.current()).memberId());
        return seeOther("/settings#settings-notifications");
    }

    private static RedirectView seeOther(String url) {
        RedirectView view = new RedirectView(url);
        view.setStatusCode(HttpStatus.SEE_OTHER);
        view.setExposeModelAttributes(false);
        return view;
    }
}

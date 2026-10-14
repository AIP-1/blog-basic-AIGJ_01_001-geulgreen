package com.team.blog.notification.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * 096 강성찬 개인 확장: 텔레그램 연결·보내기. 회원마다 채팅 번호를 하나 저장하고(지금은 관리자에게만 운영 알림),
 * 연결은 사이트가 준 일회용 코드가 든 t.me 링크 → 봇에 {@code /start 코드} → 웹훅이 받아 저장한다.
 * 웹훅은 텔레그램이 사이트 주소로 보내므로 앱 두 대 중 어느 쪽이 받아도 되고, 폴링·잠금이 필요 없다.
 */
@Service
public class TelegramService {

    private static final String CODE_KEY = "telegram:link:";
    private static final String ADMINS = """
            SELECT t.chat_id FROM telegram_link t JOIN member m ON m.id = t.member_id
            WHERE m.role = 'ADMIN' AND m.withdrawn_at IS NULL
            """;

    private final TelegramProperties properties;
    private final TelegramClient client;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final NotificationDispatcher dispatcher;
    private final SecureRandom random = new SecureRandom();
    private volatile String botUsername;

    public TelegramService(TelegramProperties properties, TelegramClient client, JdbcTemplate jdbc, StringRedisTemplate redis,
                           NotificationDispatcher dispatcher) {
        this.properties = properties;
        this.client = client;
        this.jdbc = jdbc;
        this.redis = redis;
        this.dispatcher = dispatcher;
    }

    public boolean active() {
        return properties.active();
    }

    public String siteOrigin() {
        return properties.siteOrigin();
    }

    /** 웹훅 요청 확인값. 봇 토큰에서 만들어 두 대가 같은 값을 쓰고, 따로 비밀값을 두지 않는다. */
    public String webhookSecret() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("telegram-webhook:" + properties.botToken()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 켜질 때 웹훅을 등록한다(두 대가 같은 값으로 불러도 같음). 사이트가 https가 아니면(로컬) 건너뛴다. */
    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        if (active() && properties.siteOrigin().startsWith("https://")) {
            dispatcher.submit("telegram webhook", () -> client.setWebhook(properties.siteOrigin() + "/api/telegram/webhook",
                    webhookSecret()));
        }
    }

    public boolean linked(long memberId) {
        return !jdbc.queryForList("SELECT 1 FROM telegram_link WHERE member_id = ?", Integer.class, memberId).isEmpty();
    }

    /** 일회용 코드를 만들어 봇 링크를 돌려준다. 봇 아이디를 못 얻으면 empty. */
    public Optional<String> linkUrl(long memberId) {
        String username = botUsername;
        if (username == null) {
            username = client.username();
            botUsername = username;
        }
        if (username == null) {
            return Optional.empty();
        }
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(CODE_KEY + code, String.valueOf(memberId), properties.linkTtl());
        return Optional.of("https://t.me/" + username + "?start=" + code);
    }

    public void unlink(long memberId) {
        jdbc.update("DELETE FROM telegram_link WHERE member_id = ?", memberId);
    }

    /** 봇이 받은 메시지. {@code /start 코드}면 연결, {@code /stop}이면 끊기. 나머지는 무시. */
    public void handle(JsonNode update) {
        JsonNode message = update.path("message");
        if (!"private".equals(message.path("chat").path("type").asString(""))) {
            return;
        }
        long chatId = message.path("chat").path("id").asLong();
        String text = message.path("text").asString("").strip();
        if (text.equals("/stop")) {
            jdbc.update("DELETE FROM telegram_link WHERE chat_id = ?", chatId);
            client.send(chatId, "연결을 끊었어요. 이제 블로그 알림이 오지 않아요.");
            return;
        }
        if (!text.startsWith("/start")) {
            return;
        }
        String code = text.substring("/start".length()).strip();
        String member = code.matches("[A-Za-z0-9_-]{1,64}") ? redis.opsForValue().getAndDelete(CODE_KEY + code) : null;
        if (member == null) {
            client.send(chatId, "연결 링크가 없거나 만료됐어요. 블로그 설정 > 알림에서 [텔레그램 연결]을 다시 눌러 주세요.");
            return;
        }
        jdbc.update("""
                INSERT INTO telegram_link (member_id, chat_id) VALUES (?, ?)
                ON CONFLICT (member_id) DO UPDATE SET chat_id = EXCLUDED.chat_id, linked_at = CURRENT_TIMESTAMP
                """, Long.parseLong(member), chatId);
        client.send(chatId, "블로그와 연결됐어요. 이제 알림이 여기로 와요. 끊으려면 /stop 을 보내요.\n"
                + "배포 결과 알림용 채팅 번호: " + chatId);
    }

    /** 관리자(연결한 사람만)에게 보낸다. 요청 스레드가 아니라 알림 대기열에서 부른다. */
    public void toAdmins(String text) {
        List<Long> chats = jdbc.queryForList(ADMINS, Long.class);
        chats.forEach(chat -> client.send(chat, text));
    }
}

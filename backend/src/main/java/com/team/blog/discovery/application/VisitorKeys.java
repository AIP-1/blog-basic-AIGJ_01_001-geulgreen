package com.team.blog.discovery.application;

import com.team.blog.shared.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * 방문자 구분(31 §2-1): {@code m:회원} → {@code v:쿠키 해시} → {@code h:IP|UA + 그날(한국 시간) 무작위 비밀값 해시}.
 * 원래 IP·쿠키 값은 키에 남지 않는다. 조회수(016)와 검색 요청 제한(020)이 같은 키를 쓴다.
 */
@Component
public class VisitorKeys {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final Clock clock;
    private final AtomicReference<DailySecret> secret = new AtomicReference<>();

    private record DailySecret(LocalDate day, byte[] value) {

        @Override
        public boolean equals(Object o) {
            return o instanceof DailySecret(LocalDate otherDay, byte[] otherValue) && day.equals(otherDay) && Arrays.equals(value, otherValue);
        }

        @Override
        public int hashCode() {
            return 31 * day.hashCode() + Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return "DailySecret[day=" + day + "]"; // 비밀값은 로그에 남기지 않는다
        }
    }

    public VisitorKeys(Clock clock) {
        this.clock = clock;
    }

    public String key(Optional<CurrentUser> viewer, Optional<String> visitorCookie, String ip, String userAgent) {
        if (viewer.isPresent()) {
            return "m:" + viewer.get().memberId();
        }
        if (visitorCookie.isPresent()) {
            return "v:" + sha256(visitorCookie.get().getBytes(StandardCharsets.UTF_8), new byte[0]);
        }
        String raw = (ip == null ? "" : ip) + "|" + (userAgent == null ? "" : userAgent);
        return "h:" + sha256(raw.getBytes(StandardCharsets.UTF_8), todaySecret());
    }

    private byte[] todaySecret() {
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        DailySecret current = secret.get();
        if (current == null || !current.day().equals(today)) {
            byte[] value = new byte[32];
            new SecureRandom().nextBytes(value);
            current = new DailySecret(today, value);
            secret.set(current);
        }
        return current.value();
    }

    private static String sha256(byte[] value, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return HexFormat.of().formatHex(digest.digest(value)).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

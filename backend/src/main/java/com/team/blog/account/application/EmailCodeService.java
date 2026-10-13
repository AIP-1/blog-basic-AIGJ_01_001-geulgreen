package com.team.blog.account.application;

import com.team.blog.account.infra.KeyHashing;
import com.team.blog.account.infra.RedisRateLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 074 메일 6자리 코드(가입 이메일 확인·비밀번호 찾기). Redis {@code auth:code:{용도}:{이메일 해시}}에 코드 해시·값·틀린 횟수를
 * 두고, 새 코드를 보내면 이전 코드는 덮인다. 틀린 횟수가 한도를 넘거나 맞히면 지운다. 키·값에 원문 이메일·코드를 두지 않는다.
 */
@Service
public class EmailCodeService {

    public enum Purpose { SIGNUP, RESET }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redis;
    private final RedisRateLimiter rateLimiter;
    private final InstaAccountProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmailCodeService(StringRedisTemplate redis, RedisRateLimiter rateLimiter,
                            InstaAccountProperties properties, Clock clock) {
        this.redis = redis;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.clock = clock;
    }

    /** 발송 한도: IP 1시간, 이메일 해시 1분·하루. 하나라도 넘으면 false. */
    public boolean allowSend(Purpose purpose, String email, String ip) {
        String base = "auth:code-send:" + purpose.name().toLowerCase();
        if (!rateLimiter.tryAcquire(RedisRateLimiter.key(base + ":ip", ip), properties.codeIpPerHour(),
                Duration.ofHours(1)).allowed()) {
            return false;
        }
        String emailKey = base + ":email:" + KeyHashing.emailHash(email);
        if (!rateLimiter.tryAcquire(emailKey + ":min", properties.codePerMinute(), Duration.ofMinutes(1)).allowed()) {
            return false;
        }
        String day = DAY.format(clock.instant().atZone(clock.getZone()));
        return rateLimiter.tryAcquire(emailKey + ":" + day, properties.codePerDay(), Duration.ofDays(1)).allowed();
    }

    /** 새 코드를 만들어 {@code value}와 함께 저장한다. @return 메일에 넣을 6자리 코드 */
    public String issue(Purpose purpose, String email, String value) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        String key = key(purpose, email);
        redis.delete(key);
        redis.opsForHash().putAll(key, Map.of("hash", KeyHashing.sha256(code), "value", value, "tries", "0"));
        redis.expire(key, properties.codeTtl());
        return code;
    }

    /** 맞으면 코드를 지우고 저장한 값을 돌려준다. 틀리면 횟수를 올리고, 한도를 넘으면 코드를 지운다. */
    public Optional<String> consume(Purpose purpose, String email, String code) {
        if (code == null || !code.strip().matches("\\d{6}")) {
            return Optional.empty();
        }
        String key = key(purpose, email);
        Object hash = redis.opsForHash().get(key, "hash");
        if (hash == null) {
            return Optional.empty();
        }
        if (redis.opsForHash().increment(key, "tries", 1) > properties.codeMaxAttempts()) {
            redis.delete(key);
            return Optional.empty();
        }
        if (!MessageDigest.isEqual(hash.toString().getBytes(StandardCharsets.UTF_8),
                KeyHashing.sha256(code.strip()).getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        Object value = redis.opsForHash().get(key, "value");
        redis.delete(key);
        return Optional.ofNullable(value).map(Object::toString);
    }

    public Duration ttl() {
        return properties.codeTtl();
    }

    private static String key(Purpose purpose, String email) {
        return "auth:code:" + purpose.name().toLowerCase() + ":" + KeyHashing.emailHash(email);
    }
}

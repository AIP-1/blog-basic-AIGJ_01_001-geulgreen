package com.team.blog.admin.application;

import com.team.blog.moderation.application.AdminGuard;
import com.team.blog.shared.security.CurrentUser;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 098 관리자 시스템 화면(강성찬 개인 확장): 기능 스위치(blog.*.enabled), 외부 연동 설정 여부, 앱 서버별 상태와 배포 버전.
 * 비밀값은 있는지만 보고 값은 화면에 내지 않는다. 서버 상태는 앱마다 30초마다 Redis에 남기는 신호로 본다(90초 넘게 없으면 꺼짐).
 */
@Service
public class SystemStatusService {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusService.class);
    static final String INSTANCES_KEY = "admin:instances";
    private static final Duration ALIVE = Duration.ofSeconds(90);
    private static final Duration FORGET = Duration.ofDays(1);

    public record Feature(String name, boolean on) {
    }

    public record Integration(String name, boolean configured, String note) {
    }

    public record Instance(String host, Instant startedAt, String version, Instant lastSeen, boolean alive, boolean self) {
    }

    private final ConfigurableEnvironment env;
    private final StringRedisTemplate redis;
    private final AdminGuard adminGuard;
    private final Clock clock;
    private final String host;
    private final String version;
    private final Instant startedAt = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());

    public SystemStatusService(ConfigurableEnvironment env, StringRedisTemplate redis, AdminGuard adminGuard, Clock clock,
                               @Value("${APP_VERSION:dev}") String version) {
        this.env = env;
        this.redis = redis;
        this.adminGuard = adminGuard;
        this.clock = clock;
        this.version = version.length() > 7 && version.matches("[0-9a-f]+") ? version.substring(0, 7) : version;
        this.host = hostName();
    }

    public String version() {
        return version;
    }

    @Scheduled(fixedDelayString = "30s", initialDelayString = "2s")
    public void heartbeat() {
        try {
            redis.opsForHash().put(INSTANCES_KEY, host, clock.millis() + "|" + startedAt.toEpochMilli() + "|" + version);
        } catch (DataAccessException e) {
            log.warn("instance heartbeat skipped: {}", e.getClass().getSimpleName());
        }
    }

    /** 이름순 기능 스위치. application.yml 등 설정 파일에 적힌 {@code blog.….enabled}만. */
    public List<Feature> features(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        TreeSet<String> keys = new TreeSet<>();
        for (var source : env.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> e) {
                for (String k : e.getPropertyNames()) {
                    if (k.startsWith("blog.") && k.endsWith(".enabled")) {
                        keys.add(k);
                    }
                }
            }
        }
        return keys.stream().map(k -> new Feature(k.substring(5, k.length() - 8), env.getProperty(k, Boolean.class, false))).toList();
    }

    public List<Integration> integrations(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        return List.of(
                new Integration("메일 발송", set("spring.mail.username"), set("spring.mail.username") ? null : "보내는 계정이 비어 있어요(개발은 Mailpit)"),
                new Integration("Google 로그인", set("spring.security.oauth2.client.registration.google.client-id"), null),
                new Integration("GitHub 로그인", set("spring.security.oauth2.client.registration.github.client-id"), null),
                new Integration("Gemini 태그 추천", set("blog.ai.tag-suggest.gemini.api-key"), "비어 있으면 자체 추천만 써요"),
                new Integration("텔레그램 알림", set("blog.telegram.bot-token") && on("blog.telegram.enabled"), null),
                new Integration("카카오톡 공유", set("blog.reading.kakao-js-key"), null),
                new Integration("AI 연결(MCP)", on("blog.ai-connect.enabled"), null));
    }

    /** 최근 하루 안에 신호를 남긴 앱 서버들(켜진 순). */
    public List<Instance> instances(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        Instant now = clock.instant();
        List<Instance> list = new ArrayList<>();
        try {
            for (Map.Entry<Object, Object> e : redis.opsForHash().entries(INSTANCES_KEY).entrySet()) {
                String[] v = String.valueOf(e.getValue()).split("\\|", 3);
                Instant seen = Instant.ofEpochMilli(Long.parseLong(v[0]));
                if (seen.isBefore(now.minus(FORGET))) {
                    redis.opsForHash().delete(INSTANCES_KEY, e.getKey());
                    continue;
                }
                String name = String.valueOf(e.getKey());
                list.add(new Instance(name, Instant.ofEpochMilli(Long.parseLong(v[1])), v.length > 2 ? v[2] : "?", seen,
                        !seen.isBefore(now.minus(ALIVE)), name.equals(host)));
            }
        } catch (RuntimeException e) { // Redis 실패·예전 형식 값
            log.warn("instance list failed: {}", e.getClass().getSimpleName());
        }
        if (list.stream().noneMatch(Instance::self)) { // Redis가 안 되거나 첫 신호 전이어도 지금 답한 서버는 보인다
            list.add(new Instance(host, startedAt, version, now, true, true));
        }
        list.sort((a, b) -> a.startedAt().compareTo(b.startedAt()));
        return list;
    }

    private boolean set(String key) {
        String v = env.getProperty(key, "");
        return !v.isBlank() && !v.endsWith("-not-set");
    }

    private boolean on(String key) {
        return env.getProperty(key, Boolean.class, false);
    }

    private static String hostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.io.IOException e) {
            return "unknown";
        }
    }
}

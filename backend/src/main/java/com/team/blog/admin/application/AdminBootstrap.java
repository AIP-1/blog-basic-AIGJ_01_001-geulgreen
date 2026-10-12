package com.team.blog.admin.application;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 관리자 지정(강성찬 개인 확장): 설정한 이메일의 계정을 1분마다 관리자로 올린다. 이메일 가입은 인증을 마친 뒤에만,
 * 소셜 로그인은 그 서비스가 알려 준 이메일로. 처음 운영자를 DB를 손대지 않고 만들기 위한 것.
 */
@Component
@ConditionalOnProperty(name = "blog.admin.bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final JdbcTemplate jdbc;
    private final AdminProperties properties;

    public AdminBootstrap(JdbcTemplate jdbc, AdminProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "60s", initialDelayString = "5s")
    public void run() {
        promote();
    }

    /** 올린 계정 수. */
    public int promote() {
        List<String> emails = properties.emails().stream().map(e -> e.strip().toLowerCase()).filter(e -> !e.isEmpty()).toList();
        if (emails.isEmpty()) {
            return 0;
        }
        int n = jdbc.update("""
                UPDATE member m SET role = 'ADMIN' WHERE m.role <> 'ADMIN' AND m.withdrawn_at IS NULL AND m.deleted_at IS NULL
                  AND EXISTS (SELECT 1 FROM auth_identity a WHERE a.member_id = m.id AND lower(a.email) = ANY (?)
                              AND (a.provider <> 'LOCAL' OR a.email_verified_at IS NOT NULL))
                """, (Object) emails.toArray(new String[0]));
        if (n > 0) {
            log.info("관리자로 지정한 계정 {}개", n);
        }
        return n;
    }
}

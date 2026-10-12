package com.team.blog.admin.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 관리자 지정(강성찬 개인 확장): 이 이메일로 가입·인증한 계정은 관리자가 된다. 서버에서는 {@code ADMIN_EMAILS}(쉼표로 여러 개).
 */
@ConfigurationProperties("blog.admin")
public record AdminProperties(@DefaultValue List<String> emails, @DefaultValue("true") boolean bootstrapEnabled) {
}

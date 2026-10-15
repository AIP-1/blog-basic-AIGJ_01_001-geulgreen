package com.team.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.account.application.ProfileProperties;
import com.team.blog.discovery.application.ReadingProperties;
import com.team.blog.media.application.StorageProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContentSecurityPolicyTest {

    /** PC 카카오톡 공유는 SDK가 sharer.kakao.com으로 폼을 보내므로 form-action에도 열어야 한다. */
    @Test
    void kakaoKeyOpensSharerForFormSubmission() {
        StorageProperties storage = new StorageProperties("http://localhost:9000", null, "b", "r", null, null, null,
                false, "s3", null, null, null);
        ProfileProperties profile = new ProfileProperties(new ProfileProperties.Bio(200, 4),
                new ProfileProperties.SocialPicture(256, Map.of()));
        String withKakao = new ContentSecurityPolicy(storage, profile, new ReadingProperties(true, true, true, true, "k")).value();
        String without = new ContentSecurityPolicy(storage, profile, new ReadingProperties(true, true, true, true, "")).value();

        assertThat(withKakao).endsWith("form-action 'self' https://t.me https://sharer.kakao.com");
        assertThat(without).endsWith("form-action 'self' https://t.me");
    }
}

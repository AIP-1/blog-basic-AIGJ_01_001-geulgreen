package com.team.blog.shared.security;

import com.team.blog.account.application.ProfileProperties;
import com.team.blog.discovery.application.ReadingProperties;
import com.team.blog.media.application.StorageProperties;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * CSP 문자열(헌법 IV, 12 §8). 003: 사진 저장소 출처와 소셜 사진 호스트를 {@code img-src}·{@code connect-src}에 더한다
 * (브라우저 직접 업로드·소셜 사진 복사, 003 research R-12). {@code blob:}은 자르기 미리보기용. 나머지 지시어는 001 그대로다.
 * 092 후속: 카카오톡 공유 키가 있을 때만 카카오 SDK(t1.kakaocdn.net)와 공유 서버(폼 전송 포함)를 연다.
 */
@Component
public class ContentSecurityPolicy {

    static final String KAKAO_SDK_ORIGIN = "https://t1.kakaocdn.net";

    private final String value;

    public ContentSecurityPolicy(StorageProperties storage, ProfileProperties profile, ReadingProperties reading) {
        Set<String> storageOrigins = new LinkedHashSet<>();
        addIfPresent(storageOrigins, StorageProperties.origin(storage.effectivePublicBaseUrl()));
        Set<String> connectOrigins = new LinkedHashSet<>(storageOrigins);
        addIfPresent(connectOrigins, StorageProperties.origin(storage.endpoint()));
        Set<String> socialHosts = new LinkedHashSet<>();
        profile.socialPicture().allowedHosts().values().stream().sorted().forEach(host -> socialHosts.add("https://" + host));

        boolean kakao = reading.kakaoEnabled();
        this.value = "default-src 'self'; script-src 'self'" + (kakao ? " " + KAKAO_SDK_ORIGIN : "") + "; "
                + "img-src 'self' data: blob:" + join(storageOrigins) + join(socialHosts) + "; "
                + "connect-src 'self'" + join(connectOrigins) + join(socialHosts) + (kakao ? " https://sharer.kakao.com https://kapi.kakao.com" : "") + "; "
                + "style-src 'self' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self' https://t.me" // 096: [텔레그램 연결] 폼이 봇 링크로 303
                + (kakao ? " https://sharer.kakao.com" : ""); // PC 카카오톡 공유는 SDK가 공유 창에 폼을 보낸다
    }

    public String value() {
        return value;
    }

    private static void addIfPresent(Set<String> target, String origin) {
        if (origin != null) {
            target.add(origin);
        }
    }

    private static String join(Set<String> values) {
        StringBuilder out = new StringBuilder();
        values.forEach(v -> out.append(' ').append(v));
        return out.toString();
    }
}

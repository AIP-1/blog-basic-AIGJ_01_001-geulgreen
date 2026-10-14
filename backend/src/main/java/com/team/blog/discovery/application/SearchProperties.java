package com.team.blog.discovery.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 검색(020, 33 §4·§5). suggest-*는 094 강성찬 개인 확장(오타 교정·비슷한 글): 첫 쪽 결과가 suggest-below개보다 적을 때만. */
@ConfigurationProperties("blog.search")
public record SearchProperties(@DefaultValue("3000") int recentWindow,
                               @DefaultValue("9") int pageSize,
                               @DefaultValue("20") int peopleLimit,
                               @DefaultValue("30") int perMinute,
                               @DefaultValue("true") boolean suggestEnabled,
                               @DefaultValue("3") int suggestBelow,
                               @DefaultValue("4") int similarLimit,
                               @DefaultValue("5m") Duration suggestCacheTtl) {
}

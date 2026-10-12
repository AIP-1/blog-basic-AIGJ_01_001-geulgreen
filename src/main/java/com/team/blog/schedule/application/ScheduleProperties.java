package com.team.blog.schedule.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 042 예약 발행: 지금부터 최소 간격 ~ 최대 기간 안. */
@ConfigurationProperties("blog.schedule")
public record ScheduleProperties(@DefaultValue("true") boolean enabled, @DefaultValue("5m") Duration minLead,
                                 @DefaultValue("90d") Duration maxAhead, @DefaultValue("true") boolean jobEnabled) {
}

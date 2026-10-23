package com.team.blog.post.application;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 카드 날짜(10 L-5, 009 FR-013): 1시간 이내 "N분 전"(1분 미만 "방금 전"), 24시간 이내 "N시간 전", 그 뒤 {@code yyyy.MM.dd}(한국 시간). */
public final class CardDates {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("Asia/Seoul"));

    private CardDates() {
    }

    public static String label(Instant at, Instant now) {
        if (at == null) {
            return "";
        }
        Duration ago = Duration.between(at, now);
        if (ago.isNegative()) {
            ago = Duration.ZERO;
        }
        if (ago.toMinutes() < 1) {
            return "방금 전";
        }
        if (ago.toHours() < 1) {
            return ago.toMinutes() + "분 전";
        }
        if (ago.toHours() < 24) {
            return ago.toHours() + "시간 전";
        }
        return DATE.format(at);
    }

    /** 10-23 강성찬 개인 확장: 피드 묶음 이름(한국 날짜 기준) "오늘"·"어제"·"이번 주"(7일 안)·"지난 글". */
    public static String day(Instant at, Instant now) {
        if (at == null) {
            return "";
        }
        java.time.LocalDate today = now.atZone(SEOUL).toLocalDate();
        java.time.LocalDate d = at.atZone(SEOUL).toLocalDate();
        if (!d.isBefore(today)) {
            return "오늘";
        }
        if (d.equals(today.minusDays(1))) {
            return "어제";
        }
        return d.isAfter(today.minusDays(7)) ? "이번 주" : "지난 글";
    }
}

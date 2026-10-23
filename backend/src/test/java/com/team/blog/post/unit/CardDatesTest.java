package com.team.blog.post.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.post.application.CardDates;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 009 T902: 카드 날짜(FR-013). */
class CardDatesTest {

    private static final Instant NOW = Instant.parse("2026-10-08T03:00:00Z");

    @Test
    void relativeWithinDayThenKoreanDate() {
        assertThat(CardDates.label(NOW.minusSeconds(30), NOW)).isEqualTo("방금 전");
        assertThat(CardDates.label(NOW.minus(Duration.ofMinutes(59)), NOW)).isEqualTo("59분 전");
        assertThat(CardDates.label(NOW.minus(Duration.ofHours(1)), NOW)).isEqualTo("1시간 전");
        assertThat(CardDates.label(NOW.minus(Duration.ofHours(23).plusMinutes(59)), NOW)).isEqualTo("23시간 전");
        assertThat(CardDates.label(Instant.parse("2026-10-01T15:30:00Z"), NOW)).isEqualTo("2026.10.02");
        assertThat(CardDates.label(NOW.plusSeconds(10), NOW)).isEqualTo("방금 전");
    }

    @Test
    void feedDayGroupsByKoreanDate() {
        // NOW = 한국 10-08 12:00. 한국 날짜가 바뀌는 때는 UTC 15:00
        assertThat(CardDates.day(Instant.parse("2026-10-07T15:00:00Z"), NOW)).isEqualTo("오늘");
        assertThat(CardDates.day(Instant.parse("2026-10-07T14:59:00Z"), NOW)).isEqualTo("어제");
        assertThat(CardDates.day(Instant.parse("2026-10-06T15:00:00Z"), NOW)).isEqualTo("어제");
        assertThat(CardDates.day(Instant.parse("2026-10-06T14:59:00Z"), NOW)).isEqualTo("이번 주");
        assertThat(CardDates.day(Instant.parse("2026-10-01T15:00:00Z"), NOW)).isEqualTo("이번 주");
        assertThat(CardDates.day(Instant.parse("2026-10-01T14:59:00Z"), NOW)).isEqualTo("지난 글");
    }
}

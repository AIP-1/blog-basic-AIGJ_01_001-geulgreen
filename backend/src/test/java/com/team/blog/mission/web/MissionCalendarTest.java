package com.team.blog.mission.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MissionCalendarTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @Test
    void octoberStartsOnThursdayAndCountsDoneDays() {
        MissionController.Calendar c = MissionController.Calendar.of(YearMonth.of(2026, 10),
                Set.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 30)), TODAY, TODAY);
        // 2026-10-01은 목요일 → 앞 빈칸 4개(일~수)
        assertThat(c.cells().subList(0, 4)).allMatch(d -> d.day() == 0);
        assertThat(c.cells().get(4).day()).isEqualTo(1);
        assertThat(c.cells()).hasSize(4 + 31);
        assertThat(c.doneCount()).isEqualTo(2);
        assertThat(c.cells().get(4 + 13).future()).isTrue();
        assertThat(c.cells().get(4 + 12).selected()).isTrue();
        assertThat(c.nextMonth()).isNull();
        assertThat(c.prevMonth()).isEqualTo("2026-09");
    }

    @Test
    void monthParamFallsBackToThisMonth() {
        assertThat(MissionController.month(null, TODAY)).isEqualTo(YearMonth.of(2026, 10));
        assertThat(MissionController.month("2026-09", TODAY)).isEqualTo(YearMonth.of(2026, 9));
        assertThat(MissionController.month("2027-01", TODAY)).isEqualTo(YearMonth.of(2026, 10));
        assertThat(MissionController.month("nope", TODAY)).isEqualTo(YearMonth.of(2026, 10));
        assertThat(MissionController.Calendar.of(YearMonth.of(2026, 9), Set.of(), TODAY, TODAY).nextMonth()).isEqualTo("2026-10");
    }
}

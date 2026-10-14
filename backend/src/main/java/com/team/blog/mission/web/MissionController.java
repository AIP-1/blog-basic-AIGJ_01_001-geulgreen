package com.team.blog.mission.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.mission.application.MissionService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 057 오늘의 미션 화면(강성찬 개인 확장): 오늘(또는 지난날) 주제 3개, 내 기록·066 배지, 친구들의 글·모두의 글, 지난 7일.
 * [이 주제로 쓰기]는 그 주제의 임시글을 열어 글쓰기 화면으로 보낸다.
 */
@Controller
@ConditionalOnProperty(name = "blog.missions.enabled", havingValue = "true", matchIfMissing = true)
public class MissionController {

    private final MissionService missions;
    private final CurrentUserProvider currentUserProvider;
    private final com.team.blog.post.application.PostListQuery listQuery;
    private final java.time.Clock clock;

    public MissionController(MissionService missions, CurrentUserProvider currentUserProvider,
                             com.team.blog.post.application.PostListQuery listQuery, java.time.Clock clock) {
        this.missions = missions;
        this.currentUserProvider = currentUserProvider;
        this.listQuery = listQuery;
        this.clock = clock;
    }

    @GetMapping("/missions")
    public String page(@RequestParam(value = "date", required = false) String date,
                       @RequestParam(value = "month", required = false) String month, Model model) {
        LocalDate today = missions.today();
        LocalDate day = today;
        if (date != null && !date.isBlank()) {
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new NotFoundException();
            }
            if (day.isAfter(today)) {
                throw new NotFoundException();
            }
        }
        Optional<CurrentUser> viewer = currentUserProvider.current();
        List<MissionService.Topic> topics = missions.topics(day);
        List<MissionService.FeedPost> feed = missions.feed(day, viewer);
        model.addAttribute("day", day);
        model.addAttribute("isToday", day.equals(today));
        model.addAttribute("dayLabel", label(day));
        model.addAttribute("topics", topics);
        model.addAttribute("friendPosts", feed.stream().filter(p -> p.friend() || p.mine()).toList());
        model.addAttribute("otherPosts", feed.stream().filter(p -> !p.friend() && !p.mine()).toList());
        // 086 강성찬 개인 확장: 공용 글 카드(사진·아이디·날짜). 공용 목록 카드가 없는 글(비공개 계정 등)은 예전 미션 카드로
        java.util.Map<Long, com.team.blog.post.application.PostCard> cards = listQuery.cardsByIds(
                feed.stream().map(MissionService.FeedPost::postId).toList());
        java.util.Map<Long, String> cardDates = new java.util.HashMap<>();
        cards.values().forEach(c -> cardDates.put(c.id(), com.team.blog.post.application.CardDates.label(c.firstPublicAt(), clock.instant())));
        model.addAttribute("cards", cards);
        model.addAttribute("cardDates", cardDates);
        model.addAttribute("prevDay", day.minusDays(1).toString());
        model.addAttribute("prevLabel", label(day.minusDays(1)));
        model.addAttribute("nextDay", day.isBefore(today) ? day.plusDays(1).toString() : null);
        YearMonth shownMonth = month(month, today);
        LocalDate shownDay = day;
        viewer.ifPresent(v -> {
            TreeSet<LocalDate> done = missions.doneDays(v.memberId());
            MissionService.Stats stats = MissionService.stats(done, today);
            model.addAttribute("calendar", Calendar.of(shownMonth, done, today, shownDay));
            List<MissionService.Badge> badges = MissionService.badges(stats);
            model.addAttribute("myStats", stats);
            model.addAttribute("myBadges", badges);
            model.addAttribute("myBadgeCount", badges.stream().filter(MissionService.Badge::earned).count());
            missions.myToday(v.memberId()).ifPresent(p -> model.addAttribute("myTodayPost", p));
        });
        model.addAttribute("anyAi", topics.stream().anyMatch(MissionService.Topic::ai));
        return "mission/today";
    }

    /** 달력 칸 하나. 앞쪽 빈칸은 day 0. */
    public record CalendarDay(int day, String date, boolean done, boolean future, boolean selected) {
    }

    /** 나의 성장 기록 달력: 일요일부터 한 주씩, 이 달 완료한 날 수, 이전·다음 달(다음 달은 이번 달까지만). */
    public record Calendar(String label, List<CalendarDay> cells, int doneCount, int length, String prevMonth, String nextMonth) {

        static Calendar of(YearMonth m, Set<LocalDate> done, LocalDate today, LocalDate selected) {
            List<CalendarDay> cells = new ArrayList<>();
            for (int i = 0; i < m.atDay(1).getDayOfWeek().getValue() % 7; i++) {
                cells.add(new CalendarDay(0, null, false, false, false));
            }
            int doneCount = 0;
            for (int d = 1; d <= m.lengthOfMonth(); d++) {
                LocalDate date = m.atDay(d);
                boolean isDone = done.contains(date);
                doneCount += isDone ? 1 : 0;
                cells.add(new CalendarDay(d, date.toString(), isDone, date.isAfter(today), date.equals(selected)));
            }
            YearMonth next = m.plusMonths(1);
            return new Calendar(m.getMonthValue() + "월", cells, doneCount, m.lengthOfMonth(), m.minusMonths(1).toString(),
                    next.isAfter(YearMonth.from(today)) ? null : next.toString());
        }
    }

    /** ?month=2026-10, 없거나 잘못되면 이번 달. 앞날은 이번 달로. */
    static YearMonth month(String month, LocalDate today) {
        YearMonth now = YearMonth.from(today);
        if (month == null || month.isBlank()) {
            return now;
        }
        try {
            YearMonth m = YearMonth.parse(month);
            return m.isAfter(now) ? now : m;
        } catch (DateTimeParseException e) {
            return now;
        }
    }

    @PostMapping("/missions/topics/{id}/write")
    public RedirectView write(@PathVariable("id") long topicId) {
        long postId = missions.start(currentUserProvider.current(), topicId);
        return seeOther("/write/" + postId);
    }

    static String label(LocalDate d) {
        return d.getMonthValue() + "월 " + d.getDayOfMonth() + "일 (" + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
    }
}

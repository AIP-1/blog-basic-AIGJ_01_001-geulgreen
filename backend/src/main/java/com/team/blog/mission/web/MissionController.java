package com.team.blog.mission.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.mission.application.MissionService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

    public MissionController(MissionService missions, CurrentUserProvider currentUserProvider) {
        this.missions = missions;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/missions")
    public String page(@RequestParam(value = "date", required = false) String date, Model model) {
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
        model.addAttribute("pastDays", missions.pastDays().stream().map(d -> new DayLink(d.toString(), label(d))).toList());
        model.addAttribute("prevDay", day.minusDays(1).toString());
        model.addAttribute("nextDay", day.isBefore(today) ? day.plusDays(1).toString() : null);
        viewer.ifPresent(v -> {
            MissionService.Stats stats = missions.stats(v.memberId());
            List<MissionService.Badge> badges = MissionService.badges(stats);
            model.addAttribute("myStats", stats);
            model.addAttribute("myBadges", badges);
            model.addAttribute("myBadgeCount", badges.stream().filter(MissionService.Badge::earned).count());
            missions.myToday(v.memberId()).ifPresent(p -> model.addAttribute("myTodayPost", p));
        });
        model.addAttribute("anyAi", topics.stream().anyMatch(MissionService.Topic::ai));
        return "mission/today";
    }

    public record DayLink(String date, String label) {
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

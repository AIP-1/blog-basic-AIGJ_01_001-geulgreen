package com.team.blog.mission.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.mission.application.ClubService;
import com.team.blog.shared.error.LoginRequiredException;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 111 후보(강성찬 개인 확장): 미션 화면의 [모임] 탭. 내 모임·만들기, 모임 화면(멤버만), 초대 링크로 들어오기,
 * 나가기·내보내기·삭제. 채팅·공개 모임 목록·랭킹은 없다.
 */
@Controller
@ConditionalOnProperty(name = "blog.missions.enabled", havingValue = "true", matchIfMissing = true)
public class ClubController {

    private final ClubService clubs;
    private final CurrentUserProvider currentUserProvider;
    private final com.team.blog.post.application.PostListQuery listQuery;
    private final java.time.Clock clock;

    public ClubController(ClubService clubs, CurrentUserProvider currentUserProvider,
                          com.team.blog.post.application.PostListQuery listQuery, java.time.Clock clock) {
        this.clubs = clubs;
        this.currentUserProvider = currentUserProvider;
        this.listQuery = listQuery;
        this.clock = clock;
    }

    @GetMapping("/missions/clubs")
    public String list(Model model) {
        CurrentUser me = currentUserProvider.current().orElseThrow(LoginRequiredException::new);
        model.addAttribute("clubs", clubs.mine(me.memberId()));
        model.addAttribute("today", clubs.today());
        if (!model.containsAttribute("form")) {
            LocalDate today = clubs.today();
            model.addAttribute("form", new ClubService.Form("", "", 7, today, today.plusWeeks(4).minusDays(1)));
        }
        return "mission/clubs";
    }

    @PostMapping("/missions/clubs")
    public RedirectView create(@RequestParam("name") String name, @RequestParam(value = "intro", required = false) String intro,
                               @RequestParam("everyDays") int everyDays, @RequestParam("startDate") String startDate,
                               @RequestParam("endDate") String endDate, RedirectAttributes flash) {
        ClubService.Form form = new ClubService.Form(name, intro, everyDays, date(startDate), date(endDate));
        Optional<String> problem = ClubService.problem(form);
        if (problem.isPresent()) {
            flash.addFlashAttribute("clubError", problem.get());
            flash.addFlashAttribute("form", form);
            return seeOther("/missions/clubs");
        }
        long id = clubs.create(currentUserProvider.current(), form);
        flash.addFlashAttribute("clubNotice", "함께 쓰기를 만들었어요. 초대 링크를 친구들에게 보내 주세요");
        return seeOther("/missions/clubs/" + id);
    }

    @GetMapping("/missions/clubs/{id}")
    public String board(@PathVariable("id") long id, Model model) {
        ClubService.Board board = clubs.board(currentUserProvider.current(), id);
        java.util.Map<Long, com.team.blog.post.application.PostCard> cards = listQuery.cardsByIds(board.postIds());
        java.util.Map<Long, String> cardDates = new java.util.HashMap<>();
        cards.values().forEach(c -> cardDates.put(c.id(), com.team.blog.post.application.CardDates.label(c.firstPublicAt(), clock.instant())));
        model.addAttribute("board", board);
        model.addAttribute("wroteCount", board.members().stream().filter(ClubService.Member::wrote).count());
        model.addAttribute("cards", board.postIds().stream().filter(cards::containsKey).map(cards::get).toList());
        model.addAttribute("cardDates", cardDates);
        return "mission/club";
    }

    /** 초대 링크: 로그인한 사람에게 모임 이름과 [참여하기]. 이미 멤버면 바로 모임으로. */
    @GetMapping("/missions/clubs/join/{code}")
    public Object invite(@PathVariable("code") String code, Model model) {
        CurrentUser me = currentUserProvider.current().orElseThrow(LoginRequiredException::new);
        ClubService.Club club = clubs.byInvite(code).orElseThrow(NotFoundException::new);
        if (clubs.isMember(club.id(), me.memberId())) {
            return seeOther("/missions/clubs/" + club.id());
        }
        model.addAttribute("club", club);
        return "mission/club-join";
    }

    @PostMapping("/missions/clubs/join/{code}")
    public RedirectView join(@PathVariable("code") String code, RedirectAttributes flash) {
        long id = clubs.join(currentUserProvider.current(), code);
        flash.addFlashAttribute("clubNotice", "함께 쓰기에 들어왔어요");
        return seeOther("/missions/clubs/" + id);
    }

    @PostMapping("/missions/clubs/{id}/leave")
    public RedirectView leave(@PathVariable("id") long id, RedirectAttributes flash) {
        clubs.leave(currentUserProvider.current(), id);
        flash.addFlashAttribute("clubNotice", "함께 쓰기에서 나왔어요");
        return seeOther("/missions/clubs");
    }

    @PostMapping("/missions/clubs/{id}/members/{memberId}/remove")
    public RedirectView remove(@PathVariable("id") long id, @PathVariable("memberId") long memberId, RedirectAttributes flash) {
        clubs.remove(currentUserProvider.current(), id, memberId);
        flash.addFlashAttribute("clubNotice", "멤버를 내보냈어요");
        return seeOther("/missions/clubs/" + id);
    }

    @PostMapping("/missions/clubs/{id}/delete")
    public RedirectView delete(@PathVariable("id") long id, RedirectAttributes flash) {
        clubs.delete(currentUserProvider.current(), id);
        flash.addFlashAttribute("clubNotice", "함께 쓰기를 지웠어요");
        return seeOther("/missions/clubs");
    }

    private static LocalDate date(String s) {
        try {
            return s == null || s.isBlank() ? null : LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

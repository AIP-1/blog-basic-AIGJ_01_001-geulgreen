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
    public String list(@RequestParam(value = "q", required = false) String q, Model model) {
        CurrentUser me = currentUserProvider.current().orElseThrow(LoginRequiredException::new);
        java.util.List<ClubService.Club> mine = clubs.mine(me.memberId());
        java.util.Set<Long> mineIds = mine.stream().map(ClubService.Club::id).collect(java.util.stream.Collectors.toSet());
        model.addAttribute("clubs", mine);
        model.addAttribute("wrote", clubs.wroteThisPeriod(me.memberId(), mine));
        model.addAttribute("today", clubs.today());
        // 111 후속: 받은 초대, 공개 목록(내가 든 것은 빼고)·검색
        model.addAttribute("invites", clubs.myInvites(me.memberId()));
        model.addAttribute("q", q == null ? "" : q.strip());
        model.addAttribute("listed", clubs.listed(q).stream().filter(c -> !mineIds.contains(c.id())).toList());
        model.addAttribute("modes", ClubService.MODES);
        if (!model.containsAttribute("form")) {
            LocalDate today = clubs.today();
            model.addAttribute("form", new ClubService.Form("", "", 7, today, today.plusWeeks(4).minusDays(1),
                    ClubService.APPROVAL, ClubService.DEFAULT_CAP));
        }
        return "mission/clubs";
    }

    @PostMapping("/missions/clubs")
    public RedirectView create(@RequestParam("name") String name, @RequestParam(value = "intro", required = false) String intro,
                               @RequestParam("everyDays") int everyDays, @RequestParam("startDate") String startDate,
                               @RequestParam("endDate") String endDate,
                               @RequestParam(value = "joinMode", defaultValue = ClubService.APPROVAL) String joinMode,
                               @RequestParam(value = "memberCap", defaultValue = "30") int memberCap, RedirectAttributes flash) {
        ClubService.Form form = new ClubService.Form(name, intro, everyDays, date(startDate), date(endDate), joinMode, memberCap);
        Optional<String> problem = ClubService.problem(form);
        if (problem.isPresent()) {
            flash.addFlashAttribute("clubError", problem.get());
            flash.addFlashAttribute("form", form);
            return seeOther("/missions/clubs");
        }
        long id = clubs.create(currentUserProvider.current(), form);
        flash.addFlashAttribute("clubNotice", "함께 쓰기를 만들었어요. 친구들을 초대해 보세요");
        return seeOther("/missions/clubs/" + id);
    }

    @GetMapping("/missions/clubs/{id}")
    public String board(@PathVariable("id") long id, Model model) {
        CurrentUser me = currentUserProvider.current().orElseThrow(LoginRequiredException::new);
        if (!clubs.isMember(id, me.memberId())) {
            // 111 후속: 공개 함께 쓰기(또는 초대받은 것)는 멤버가 아니어도 소개와 [참여하기]·[참여 요청]
            model.addAttribute("preview", clubs.preview(currentUserProvider.current(), id));
            return "mission/club-preview";
        }
        ClubService.Board board = clubs.board(currentUserProvider.current(), id);
        if (board.isAdmin()) {
            model.addAttribute("requests", clubs.requests(id));
            model.addAttribute("modes", ClubService.MODES);
        }
        if (!board.ended()) {
            model.addAttribute("candidates", clubs.inviteCandidates(me.memberId(), id));
            model.addAttribute("invitesLeft", Math.max(0, ClubService.DAILY_INVITES - clubs.invitesToday(me.memberId())));
        }
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
        try {
            long id = clubs.join(currentUserProvider.current(), code);
            boolean in = currentUserProvider.current().map(u -> clubs.isMember(id, u.memberId())).orElse(false);
            flash.addFlashAttribute("clubNotice", in ? "함께 쓰기에 들어왔어요" : "참여를 요청했어요. 관리자가 승인하면 들어가요");
            return seeOther("/missions/clubs/" + id);
        } catch (ClubService.ClubException e) {
            flash.addFlashAttribute("clubError", e.getMessage());
            return seeOther("/missions/clubs/join/" + code);
        }
    }

    // ----- 111 후속: 공개 함께 쓰기 참여·요청, 승인, 방 설정, 초대 -----

    @PostMapping("/missions/clubs/{id}/join")
    public RedirectView joinListed(@PathVariable("id") long id, RedirectAttributes flash) {
        return act(id, flash, () -> clubs.joinListed(currentUserProvider.current(), id) ? "함께 쓰기에 들어왔어요"
                : "참여를 요청했어요. 관리자가 승인하면 들어가요");
    }

    @PostMapping("/missions/clubs/{id}/request/cancel")
    public RedirectView cancelRequest(@PathVariable("id") long id, RedirectAttributes flash) {
        return act(id, flash, () -> {
            clubs.cancelRequest(currentUserProvider.current(), id);
            return "참여 요청을 거뒀어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/requests/{memberId}/{decision:approve|reject}")
    public RedirectView decide(@PathVariable("id") long id, @PathVariable("memberId") long memberId,
                               @PathVariable("decision") String decision, RedirectAttributes flash) {
        boolean approve = "approve".equals(decision);
        return act(id, flash, () -> {
            clubs.decide(currentUserProvider.current(), id, memberId, approve);
            return approve ? "참여를 승인했어요" : "참여 요청을 거절했어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/members/{memberId}/{role:admin|member}")
    public RedirectView setRole(@PathVariable("id") long id, @PathVariable("memberId") long memberId,
                                @PathVariable("role") String role, RedirectAttributes flash) {
        boolean admin = "admin".equals(role);
        return act(id, flash, () -> {
            clubs.setAdmin(currentUserProvider.current(), id, memberId, admin);
            return admin ? "관리자로 정했어요" : "관리자에서 뺐어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/settings")
    public RedirectView settings(@PathVariable("id") long id, @RequestParam("joinMode") String joinMode,
                                 @RequestParam("memberCap") int memberCap, RedirectAttributes flash) {
        return act(id, flash, () -> {
            clubs.settings(currentUserProvider.current(), id, joinMode, memberCap);
            return "참여 방식을 저장했어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/invite")
    public RedirectView invite(@PathVariable("id") long id,
                               @RequestParam(value = "memberIds", required = false) java.util.List<Long> memberIds,
                               RedirectAttributes flash) {
        return act(id, flash, () -> {
            int n = clubs.invite(currentUserProvider.current(), id, memberIds == null ? java.util.List.of() : memberIds);
            return n == 0 ? "초대할 사람을 골라 주세요" : n + "명에게 초대를 보냈어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/invite/{answer:accept|decline}")
    public RedirectView answerInvite(@PathVariable("id") long id, @PathVariable("answer") String answer, RedirectAttributes flash) {
        boolean accept = "accept".equals(answer);
        try {
            boolean in = clubs.answerInvite(currentUserProvider.current(), id, accept);
            flash.addFlashAttribute("clubNotice", !accept ? "초대를 거절했어요"
                    : in ? "함께 쓰기에 들어왔어요" : "초대를 수락했어요. 관리자가 승인하면 들어가요");
            return seeOther(accept ? "/missions/clubs/" + id : "/missions/clubs");
        } catch (ClubService.ClubException e) {
            flash.addFlashAttribute("listError", e.getMessage());
            return seeOther("/missions/clubs");
        }
    }

    /** 실패하면 그 문장을, 되면 알림 문장을 보여 주고 그 함께 쓰기로 돌아간다. */
    private RedirectView act(long id, RedirectAttributes flash, java.util.function.Supplier<String> action) {
        try {
            flash.addFlashAttribute("clubNotice", action.get());
        } catch (ClubService.ClubException e) {
            flash.addFlashAttribute("clubError", e.getMessage());
        }
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

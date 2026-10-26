package com.team.blog.mission.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.mission.application.ClubService;
import com.team.blog.shared.error.LoginRequiredException;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.LocalDate;
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
 * 111 후보(강성찬 개인 확장): 함께 쓰기. 내 방 목록·만들기, 방 화면(멤버만), 초대 링크로 들어오기, 나가기·내보내기·삭제.
 * 10-25 chan: 기간 없이 오픈채팅방처럼 — 방 화면은 멤버들의 글과 방 소식이 시간 순으로, 위에 방 글감. 채팅·랭킹은 없다.
 */
@Controller
@ConditionalOnProperty(name = "blog.missions.enabled", havingValue = "true", matchIfMissing = true)
public class ClubController {

    private static final java.time.ZoneId SEOUL = java.time.ZoneId.of("Asia/Seoul");

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
        java.util.List<ClubService.Room> rooms = clubs.rooms(me.memberId());
        model.addAttribute("rooms", rooms);
        java.time.Instant now = clock.instant();
        java.util.Map<Long, String> roomTimes = new java.util.HashMap<>();
        rooms.forEach(r -> roomTimes.put(r.club().id(), r.lastAt() == null ? "" : com.team.blog.post.application.CardDates.label(r.lastAt(), now)));
        model.addAttribute("roomTimes", roomTimes);
        // 111 후속: 받은 초대, 공개 목록(내가 든 것은 빼고)·검색
        model.addAttribute("invites", clubs.myInvites(me.memberId()));
        model.addAttribute("modes", ClubService.MODES);
        // 예전 ?q= 주소는 방 찾기 탭으로
        if (q != null && !q.isBlank()) {
            return "redirect:/missions/clubs/find?q=" + org.springframework.web.util.UriUtils.encodeQueryParam(q.strip(), java.nio.charset.StandardCharsets.UTF_8);
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ClubService.Form("", "", ClubService.APPROVAL, ClubService.DEFAULT_CAP));
        }
        return "mission/clubs";
    }

    /** 10-21 chan: 방 찾기 탭(공개 방·검색). 내가 든 방은 빼고 보여 준다. */
    @GetMapping("/missions/clubs/find")
    public String find(@RequestParam(value = "q", required = false) String q, Model model) {
        CurrentUser me = currentUserProvider.current().orElseThrow(LoginRequiredException::new);
        java.util.Set<Long> mineIds = clubs.mine(me.memberId()).stream().map(ClubService.Club::id)
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("q", q == null ? "" : q.strip());
        model.addAttribute("listed", clubs.listed(q).stream().filter(c -> !mineIds.contains(c.id())).toList());
        return "mission/clubs-find";
    }

    @PostMapping("/missions/clubs")
    public RedirectView create(@RequestParam("name") String name, @RequestParam(value = "intro", required = false) String intro,
                               @RequestParam(value = "joinMode", defaultValue = ClubService.APPROVAL) String joinMode,
                               @RequestParam(value = "memberCap", defaultValue = "30") int memberCap, RedirectAttributes flash) {
        ClubService.Form form = new ClubService.Form(name, intro, joinMode, memberCap);
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
        model.addAttribute("candidates", clubs.inviteCandidates(me.memberId(), id));
        model.addAttribute("invitesLeft", Math.max(0, ClubService.DAILY_INVITES - clubs.invitesToday(me.memberId())));
        java.util.Map<Long, com.team.blog.post.application.PostCard> cards = listQuery.cardsByIds(board.postIds());
        java.util.List<com.team.blog.post.application.PostCard> ordered = board.postIds().stream().filter(cards::containsKey).map(cards::get).toList();
        model.addAttribute("board", board);
        model.addAttribute("self", board.members().stream().filter(m -> m.memberId() == me.memberId()).findFirst().orElse(null));
        model.addAttribute("timeline", timeline(ordered, board.notices(), board.topicPostIds(), ordered.size() >= ClubService.POSTS));
        return "mission/club";
    }

    /** 방 화면 한 줄: 글(card) 또는 방 소식(text). 날짜가 바뀌는 첫 줄에 {@code day}("오늘"·"어제"·"10월 24일 (금)"). */
    public record Line(com.team.blog.post.application.PostCard card, String text, String time, String day, boolean topic) {
    }

    /** 글과 방 소식을 최근 순으로 섞는다. 글이 꽉 찼으면(더 옛날 글이 있음) 가장 옛 글보다 전 소식은 뺀다. */
    java.util.List<Line> timeline(java.util.List<com.team.blog.post.application.PostCard> cards, java.util.List<ClubService.Notice> notices,
                                  java.util.Set<Long> topicPostIds, boolean full) {
        java.time.Instant now = clock.instant();
        java.time.Instant oldest = full && !cards.isEmpty() ? cards.get(cards.size() - 1).firstPublicAt() : java.time.Instant.MIN;
        record Item(java.time.Instant at, com.team.blog.post.application.PostCard card, String text) {
        }
        java.util.List<Item> items = new java.util.ArrayList<>();
        cards.forEach(c -> items.add(new Item(c.firstPublicAt(), c, null)));
        notices.stream().filter(n -> !n.at().isBefore(oldest)).forEach(n -> items.add(new Item(n.at(), null, n.text())));
        items.sort(java.util.Comparator.comparing(Item::at).reversed());
        LocalDate today = LocalDate.ofInstant(now, SEOUL);
        java.util.List<Line> lines = new java.util.ArrayList<>();
        LocalDate prev = null;
        for (Item it : items) {
            LocalDate d = LocalDate.ofInstant(it.at(), SEOUL);
            String day = d.equals(prev) ? null : d.equals(today) ? "오늘" : d.equals(today.minusDays(1)) ? "어제" : MissionController.label(d);
            prev = d;
            lines.add(new Line(it.card(), it.text(), com.team.blog.post.application.CardDates.label(it.at(), now), day,
                    it.card() != null && topicPostIds.contains(it.card().id())));
        }
        return lines;
    }

    // ----- 10-25 방 글감 -----

    @PostMapping("/missions/clubs/{id}/topics")
    public RedirectView addTopic(@PathVariable("id") long id, @RequestParam("title") String title,
                                 @RequestParam(value = "hint", required = false) String hint, RedirectAttributes flash) {
        return act(id, flash, () -> {
            clubs.addTopic(currentUserProvider.current(), id, title, hint);
            return "방 글감을 올렸어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/topics/{topicId}/delete")
    public RedirectView deleteTopic(@PathVariable("id") long id, @PathVariable("topicId") long topicId, RedirectAttributes flash) {
        return act(id, flash, () -> {
            clubs.deleteTopic(currentUserProvider.current(), id, topicId);
            return "방 글감을 내렸어요";
        });
    }

    @PostMapping("/missions/clubs/{id}/topics/{topicId}/write")
    public RedirectView writeTopic(@PathVariable("id") long id, @PathVariable("topicId") long topicId) {
        return seeOther("/write/" + clubs.startTopic(currentUserProvider.current(), id, topicId));
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
}

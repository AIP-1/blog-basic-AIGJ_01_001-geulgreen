package com.team.blog.invite.application;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.account.domain.Member;
import com.team.blog.account.infra.MemberRepository;
import com.team.blog.interaction.application.FollowService;
import com.team.blog.mission.application.ClubService;
import com.team.blog.shared.error.AccountStatusException;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.RateLimitedException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.web.BlogPaths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 10-25 chan: 친구 초대 링크. 초대한 사람(아이디)과 선택한 함께 쓰기 방(초대 코드)을 한 링크에 담는다. 새 테이블은 없다 —
 * 아이디는 공개 정보이고 방 코드는 이미 {@code writing_club.invite_code}에 있다. 받아들이면 초대한 사람을 팔로우하고 방에 들어간다.
 */
@Service
public class InviteService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 초대 한 건. {@code club}이 비어 있으면 블로그로만 초대. */
    public record Invite(BlogOwner inviter, Optional<ClubService.Club> club) {

        public String path() {
            return "/i/" + inviter.handle() + club.map(c -> "/" + c.inviteCode()).orElse("");
        }

        /** 받아들인 뒤 갈 곳: 방이 있으면 방, 없으면 초대한 사람의 블로그. */
        public String home() {
            return club.map(c -> "/missions/clubs/" + c.id()).orElse(BlogPaths.of(inviter.handle()));
        }
    }

    /** 받아들인 결과: 안내 문구와 이동할 경로. */
    public record Outcome(String notice, String target) {
    }

    /** 내 초대 링크 한 줄(/me/invite). {@code club}이 없으면 블로그 초대. */
    public record Link(String label, String path, Optional<ClubService.Club> club) {
    }

    private final BlogOwnerResolver owners;
    private final ClubService clubs;
    private final FollowService follows;
    private final AccountGuard accountGuard;
    private final MemberRepository members;

    public InviteService(BlogOwnerResolver owners, ClubService clubs, FollowService follows, AccountGuard accountGuard,
                         MemberRepository members) {
        this.owners = owners;
        this.clubs = clubs;
        this.follows = follows;
        this.accountGuard = accountGuard;
        this.members = members;
    }

    /** 초대한 사람이 없거나(탈퇴·없는 아이디), 코드가 틀리거나, 초대한 사람이 그 방 멤버가 아니면 empty. */
    public Optional<Invite> find(String handle, String code) {
        Optional<BlogOwner> inviter = owners.resolve(handle);
        if (inviter.isEmpty()) {
            return Optional.empty();
        }
        if (code == null) {
            return Optional.of(new Invite(inviter.get(), Optional.empty()));
        }
        // 아이디와 코드를 따로 조합해 남의 방 링크에 내 아이디를 붙이지 못하게: 초대한 사람이 그 방 멤버일 때만
        return clubs.byInvite(code)
                .filter(club -> clubs.isMember(club.id(), inviter.get().memberId()))
                .map(club -> new Invite(inviter.get(), Optional.of(club)));
    }

    /**
     * 초대한 사람을 팔로우하고(이미 하고 있으면 그대로, 자기 자신이면 건너뜀) 방에 들어간다. 메일 인증 전이면 팔로우만 되고
     * 방은 인증 뒤에 들어갈 수 있다고 알린다. 승인 후 참여 방은 참여 요청이 된다.
     */
    public Outcome accept(Optional<CurrentUser> current, Invite invite, String clientIp) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        String name = invite.inviter().displayName();
        boolean followed = false;
        if (invite.inviter().memberId() != me.memberId()) {
            try {
                followed = follows.set(current, invite.inviter().handle(), true, clientIp).following();
            } catch (RateLimitedException e) {
                // 팔로우 제한에 걸려도 초대 자체는 받아들인다
            }
        }
        if (invite.club().isEmpty()) {
            return new Outcome(followed ? name + "님을 팔로우했어요. 새 글이 피드에 보여요" : name + "님의 블로그예요",
                    invite.home());
        }
        ClubService.Club club = invite.club().get();
        if (clubs.isMember(club.id(), me.memberId())) {
            return new Outcome("이미 함께 쓰기에 들어와 있어요", invite.home());
        }
        try {
            clubs.join(current, club.inviteCode());
        } catch (AccountStatusException e) {
            if (e.getReason() != AccountStatusException.Reason.EMAIL_NOT_VERIFIED) {
                throw e;
            }
            return new Outcome("메일 인증을 마치면 함께 쓰기 '" + club.name() + "'에 들어갈 수 있어요. 초대 링크를 다시 열면 돼요",
                    BlogPaths.of(invite.inviter().handle()));
        } catch (ClubService.ClubException e) {
            return new Outcome(e.getMessage(), BlogPaths.of(invite.inviter().handle()));
        }
        boolean in = clubs.isMember(club.id(), me.memberId());
        String notice = in ? (followed ? name + "님을 팔로우하고 함께 쓰기에 들어왔어요" : "함께 쓰기에 들어왔어요")
                : "참여를 요청했어요. 관리자가 승인하면 들어가요";
        return new Outcome(notice, in ? invite.home() : BlogPaths.of(invite.inviter().handle()));
    }

    /** 내 초대 링크들: 블로그 초대 하나와 아직 끝나지 않은 내 방마다 하나. */
    public List<Link> myLinks(long memberId) {
        String myHandle = members.findById(memberId).map(Member::getHandle).orElseThrow(NotFoundException::new);
        LocalDate today = LocalDate.now(SEOUL);
        List<Link> links = new java.util.ArrayList<>();
        links.add(new Link("내 블로그로 초대", "/i/" + myHandle, Optional.empty()));
        clubs.mine(memberId).stream()
                .filter(c -> !today.isAfter(c.endDate()))
                .forEach(c -> links.add(new Link("함께 쓰기 '" + c.name() + "'로 초대", "/i/" + myHandle + "/" + c.inviteCode(), Optional.of(c))));
        return links;
    }
}

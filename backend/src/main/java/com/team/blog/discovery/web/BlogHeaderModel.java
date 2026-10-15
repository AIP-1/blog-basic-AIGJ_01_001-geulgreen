package com.team.blog.discovery.web;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.discovery.application.BlogVisitService;
import com.team.blog.friend.application.FriendQuery;
import com.team.blog.interaction.application.FollowQuery;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.privacy.application.PrivateAccountService;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.web.BlogPaths;
import com.team.blog.shared.web.ClientIpResolver;
import com.team.blog.shared.web.VisitorCookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 블로그 머리말(프로필·숫자 줄·팔로우)과 탭(홈·글·시리즈, 100 강성찬 개인 확장)에 쓰는 값. 탭 화면이 모두 같이 쓴다.
 * 비공개 계정(073)을 볼 수 없는 사람이면 프로필 윗부분 값만 넣고 {@code false} — 화면은 {@code blog/private}.
 */
@Component
public class BlogHeaderModel {

    private final CurrentUserProvider currentUserProvider;
    private final PostListQuery listQuery;
    private final FollowQuery followQuery;
    private final FriendQuery friendQuery;
    private final BlogVisitService blogVisits;
    private final ClientIpResolver ipResolver;
    private final PrivateAccountService privacy;

    public BlogHeaderModel(CurrentUserProvider currentUserProvider, PostListQuery listQuery, FollowQuery followQuery,
                           FriendQuery friendQuery, BlogVisitService blogVisits, ClientIpResolver ipResolver,
                           PrivateAccountService privacy) {
        this.currentUserProvider = currentUserProvider;
        this.listQuery = listQuery;
        this.followQuery = followQuery;
        this.friendQuery = friendQuery;
        this.blogVisits = blogVisits;
        this.ipResolver = ipResolver;
        this.privacy = privacy;
    }

    /** {@code tab}: home·posts·series(지금 탭 표시). */
    public boolean fill(BlogOwner owner, String tab, HttpServletRequest request, Model model) {
        Optional<CurrentUser> viewer = currentUserProvider.current();
        model.addAttribute("owner", owner);
        model.addAttribute("author", owner.toAuthorDisplay());
        model.addAttribute("followCounts", followQuery.counts(owner.memberId()));
        model.addAttribute("loginRedirect", "/login?redirect=" + BlogPaths.of(owner.handle()));
        // 073 강성찬 개인 확장: 수락받지 않은 사람에게 비공개 계정은 프로필 윗부분만
        if (!privacy.canSee(viewer, owner.memberId())) {
            model.addAttribute("followRequested", viewer.map(v -> privacy.requested(v.memberId(), owner.memberId())).orElse(false));
            return false;
        }
        model.addAttribute("blogTab", tab);
        // 051 강성찬 개인 확장: 방문자 수(세고 나서 보여 준다)
        if (blogVisits.enabled()) {
            blogVisits.recordVisit(owner.memberId(), viewer, VisitorCookie.read(request), ipResolver.resolve(request),
                    request.getHeader("User-Agent"));
            model.addAttribute("blogVisits", blogVisits.counts(owner.memberId()));
        }
        model.addAttribute("publicCount", listQuery.publicCount(owner.memberId()));
        boolean isOwner = viewer.map(CurrentUser::memberId).map(id -> id == owner.memberId()).orElse(false);
        model.addAttribute("isOwner", isOwner);
        // 087 후속(강성찬 개인 확장): 내 블로그면 숫자 줄에서 친구 관리로(인스타의 친한 친구처럼)
        if (isOwner) {
            model.addAttribute("friendCount", friendQuery.count(owner.memberId()));
        }
        // 018: 보는 사람 기준 팔로우 상태, 025·069: 친구 버튼
        model.addAttribute("followingOwner", viewer.map(CurrentUser::memberId)
                .filter(id -> id != owner.memberId()).map(id -> followQuery.isFollowing(id, owner.memberId())).orElse(false));
        model.addAttribute("friendStatus", viewer.map(v -> friendQuery.status(v.memberId(), owner.memberId()))
                .orElse(FriendQuery.Status.NONE).name());
        return true;
    }

    /** 025·030: 주인이 보는 사람을 친구로 골랐으면 그 사람 번호(친구·그룹 공개 글도 보인다), 아니면 null. */
    public Long friendViewer(BlogOwner owner) {
        return currentUserProvider.current().map(CurrentUser::memberId)
                .filter(v -> friendQuery.hasFriend(owner.memberId(), v)).orElse(null);
    }
}

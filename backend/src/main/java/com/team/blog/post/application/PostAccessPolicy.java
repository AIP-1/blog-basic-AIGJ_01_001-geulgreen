package com.team.blog.post.application;

import com.team.blog.post.application.visibility.AuthorGate;
import com.team.blog.post.application.visibility.PostFacts;
import com.team.blog.post.application.visibility.VisibilityRule;
import com.team.blog.post.domain.PostStatus;
import com.team.blog.shared.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 글 읽기 판정을 한곳에(헌법 III, 42 §3·§5-1, 006 R-1).
 * 휴지통(조회에서 제외) → 작성자가 탈퇴 유예·익명이면 아무도 못 봄 → 작성자 본인은 봄 → 관리자 숨김이면 아무도 못 봄(022)
 * → 임시글은 작성자만 → 작성자 단위 막기(073) → 공개 범위 규칙. 관리자 예외는 없다. 목록은 {@link #publicListingCondition}·{@link #blogListingCondition}.
 */
@Component
public class PostAccessPolicy {

    private final Map<String, VisibilityRule> rules;
    private final List<AuthorGate> gates;

    public PostAccessPolicy(List<VisibilityRule> rules, ObjectProvider<AuthorGate> gates) {
        Map<String, VisibilityRule> byValue = new LinkedHashMap<>();
        rules.forEach(rule -> byValue.put(rule.visibility(), rule));
        this.rules = Map.copyOf(byValue);
        this.gates = gates.orderedStream().toList();
    }

    public boolean canRead(Optional<CurrentUser> viewer, PostFacts post) {
        if (post.authorWithdrawn()) {
            return false;
        }
        boolean author = viewer.map(v -> v.memberId() == post.authorId()).orElse(false);
        if (author) {
            return true;
        }
        if (post.hidden()) {
            return false;
        }
        if (post.status() != PostStatus.PUBLISHED) {
            return false;
        }
        if (!canSeeAuthor(viewer, post.authorId())) {
            return false;
        }
        VisibilityRule rule = rules.get(post.visibility());
        return rule != null && rule.canRead(viewer, post);
    }

    /** 작성자 단위 막기(073 비공개 계정): 본인이 아니면 모든 {@link AuthorGate}를 통과해야 그 작성자의 블로그·글을 본다. */
    public boolean canSeeAuthor(Optional<CurrentUser> viewer, long authorId) {
        if (viewer.map(v -> v.memberId() == authorId).orElse(false)) {
            return true;
        }
        return gates.stream().allMatch(g -> g.canSee(viewer, authorId));
    }

    /** 지원하는 공개 범위 값(검사용). */
    public Set<String> supportedVisibilities() {
        return rules.keySet();
    }

    /**
     * 작성자가 아닌 사람에게 보이는 공용 목록 조건(006 R-2). 여러 작성자가 섞이는 목록(홈·태그·검색·트렌딩·RSS·sitemap)은 이것을 쓴다:
     * 비공개 계정(073)의 글은 팔로워에게도 여기서는 빠진다.
     *
     * @param postAlias   {@code post} 별칭
     * @param memberAlias 작성자 {@code member} 별칭(조인 필요)
     */
    public String publicListingCondition(String postAlias, String memberAlias) {
        StringBuilder sql = new StringBuilder(blogListingCondition(postAlias, memberAlias));
        gates.forEach(g -> sql.append(" AND ").append(g.listCondition(memberAlias)));
        return sql.toString();
    }

    /**
     * 한 작성자의 블로그 안 목록(블로그 글·카테고리·시리즈·이전/다음 글, 팔로잉 피드) 조건: 공용 조건에서 작성자 단위 막기만 뺀 것.
     * 부르는 쪽이 {@link #canSeeAuthor}로 먼저 막거나(블로그 화면), 이미 팔로우한 작성자만 고른다(팔로잉 피드).
     */
    public String blogListingCondition(String postAlias, String memberAlias) {
        String visibility = rules.values().stream()
                .map(rule -> rule.listCondition(postAlias))
                .filter(Objects::nonNull)
                .map(c -> "(" + c + ")")
                .collect(Collectors.joining(" OR "));
        if (visibility.isEmpty()) {
            visibility = "FALSE";
        }
        return base(postAlias, memberAlias) + " AND (" + visibility + ")";
    }

    /**
     * 친구가 보는 개인 블로그 목록 조건(025, 강성찬 개인 확장, 06 §6-3): 공용 조건에 친구 공개 글을 더한다. 다른 목록에는 쓰지 않는다.
     */
    public String friendBlogCondition(String postAlias, String memberAlias) {
        return base(postAlias, memberAlias) + " AND " + postAlias + ".visibility IN ('PUBLIC', 'FRIENDS')";
    }

    /**
     * 친구가 보는 블로그에 그룹 공개 글(030)도 더한 조건. 보는 사람 번호가 바인딩 값 하나({@code ?})로 들어간다.
     */
    public String friendAndGroupBlogCondition(String postAlias, String memberAlias) {
        return base(postAlias, memberAlias) + " AND (" + postAlias + ".visibility IN ('PUBLIC', 'FRIENDS') OR (" + postAlias
                + ".visibility = 'GROUP' AND EXISTS (SELECT 1 FROM post_group_visibility pg JOIN group_member gm"
                + " ON gm.group_id = pg.group_id WHERE pg.post_id = " + postAlias + ".id AND gm.member_id = ?)))";
    }

    private static String base(String postAlias, String memberAlias) {
        return postAlias + ".status = 'PUBLISHED' AND " + postAlias + ".deleted_at IS NULL AND " + postAlias
                + ".hidden_at IS NULL AND " + memberAlias + ".withdrawn_at IS NULL";
    }
}

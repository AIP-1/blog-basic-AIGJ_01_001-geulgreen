package com.team.blog.interaction.web;

import com.team.blog.interaction.application.FollowQuery;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 글 상세 작성자 카드의 팔로우 버튼 상태(018 FR-007). 작성자 본인·비회원은 조회하지 않는다.
 * 097 강성찬 개인 확장: 카드의 작은 숫자 줄(공개 글 수·팔로워 수, 블로그 머리말과 같은 값).
 */
@Component
public class FollowDetailSection implements PostDetailSection {

    private final FollowQuery followQuery;
    private final PostListQuery listQuery;

    public FollowDetailSection(FollowQuery followQuery, PostListQuery listQuery) {
        this.followQuery = followQuery;
        this.listQuery = listQuery;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        model.addAttribute("followingAuthor", viewer.filter(v -> v.memberId() != post.authorId())
                .map(v -> followQuery.isFollowing(v.memberId(), post.authorId())).orElse(false));
        model.addAttribute("authorPostCount", listQuery.publicCount(post.authorId()));
        model.addAttribute("authorFollowers", followQuery.counts(post.authorId()).followers());
    }
}

package com.team.blog.post.application.visibility;

import com.team.blog.shared.security.CurrentUser;
import java.util.Optional;

/**
 * 공개 범위와 따로, 작성자 단위로 글을 막는 규칙(073 비공개 계정, 강성찬 개인 확장). 공개 범위 규칙처럼 Bean을 더해 붙인다.
 */
public interface AuthorGate {

    /** 보는 사람이 이 작성자의 글을 볼 수 있는지(작성자 본인 판정은 호출자가 먼저 한다). */
    boolean canSee(Optional<CurrentUser> viewer, long authorId);

    /** 여러 작성자가 섞이는 공용 목록에서 빼야 할 작성자를 거르는 SQL 조건. 막을 것이 없으면 {@code TRUE}. */
    String listCondition(String memberAlias);
}

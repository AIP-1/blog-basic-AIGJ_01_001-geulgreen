package com.team.blog.friend.application;

import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.friend.application.FriendQuery.Status;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 친한 친구 넣기·빼기(069, 인스타 "친한 친구"처럼). 나를 팔로우하는 사람만 넣을 수 있고, 상대 수락도 알림도 없다.
 * 판정 순서: 로그인(401) → 대상(없거나 탈퇴 유예 404). 나를 팔로우하지 않는 사람·나 자신은 아무것도 바꾸지 않는다.
 */
@Service
public class FriendService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final AccountGuard accountGuard;
    private final BlogOwnerResolver ownerResolver;
    private final FriendQuery query;
    private final GroupService groups;

    public FriendService(JdbcTemplate jdbc, TransactionTemplate tx, AccountGuard accountGuard, BlogOwnerResolver ownerResolver,
                         FriendQuery query, GroupService groups) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.accountGuard = accountGuard;
        this.ownerResolver = ownerResolver;
        this.query = query;
        this.groups = groups;
    }

    /** 나를 팔로우하는 사람을 친구로 넣는다. 이미 친구면 그대로. */
    public Status add(Optional<CurrentUser> current, String handle) {
        long me = accountGuard.requireLoggedIn(current).memberId();
        long target = target(handle);
        jdbc.update("""
                INSERT INTO close_friend (owner_id, friend_id)
                SELECT followee_id, follower_id FROM follow WHERE followee_id = ? AND follower_id = ?
                ON CONFLICT DO NOTHING
                """, me, target);
        return query.status(me, target);
    }

    /** 친구에서 뺀다. 내 그룹에서도 빠진다(030). 상대에게 알리지 않는다. */
    public Status remove(Optional<CurrentUser> current, String handle) {
        long me = accountGuard.requireLoggedIn(current).memberId();
        long target = target(handle);
        tx.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM close_friend WHERE owner_id = ? AND friend_id = ?", me, target);
            groups.removeFromOwnGroups(me, target);
        });
        return query.status(me, target);
    }

    /** 익명 처리 단계(13 §3-3): 그 회원이 고른 친구와, 남이 그 회원을 고른 관계를 모두 지운다. */
    public int purgeWithdrawn(long memberId) {
        return jdbc.update("DELETE FROM close_friend WHERE owner_id = ? OR friend_id = ?", memberId, memberId);
    }

    private long target(String handle) {
        return ownerResolver.resolve(handle).orElseThrow(NotFoundException::new).memberId();
    }
}

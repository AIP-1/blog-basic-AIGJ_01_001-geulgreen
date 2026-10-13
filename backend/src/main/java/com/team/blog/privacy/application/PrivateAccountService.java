package com.team.blog.privacy.application;

import com.team.blog.account.application.ProfileAvatar;
import com.team.blog.post.application.visibility.AuthorGate;
import com.team.blog.shared.event.FollowRequestClosed;
import com.team.blog.shared.event.FollowRequested;
import com.team.blog.shared.event.MemberFollowed;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 비공개 계정(073, 강성찬 개인 확장). 비공개 계정은 본인과 수락한 팔로워({@code follow} 행)만 블로그·글을 본다.
 * 팔로우 요청은 {@code follow_request}에 두었다가 수락하면 {@code follow}로 옮긴다. 비공개를 끄면 대기 중 요청을 모두 수락한다.
 */
@Service
public class PrivateAccountService implements AuthorGate {

    public record Requester(long memberId, String handle, String nickname, String profileImageUrl) {

        public ProfileAvatar avatar() {
            return new ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PrivacyProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PrivateAccountService(JdbcTemplate jdbc, TransactionTemplate tx, PrivacyProperties properties,
                                 ApplicationEventPublisher events, Clock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public boolean isPrivate(long memberId) {
        return properties.enabled() && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM private_account WHERE member_id = ?)", Boolean.class, memberId));
    }

    @Override
    public boolean canSee(Optional<CurrentUser> viewer, long authorId) {
        if (!isPrivate(authorId)) {
            return true;
        }
        return viewer.map(v -> v.memberId() == authorId || Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM follow WHERE follower_id = ? AND followee_id = ?)", Boolean.class,
                v.memberId(), authorId))).orElse(false);
    }

    @Override
    public String listCondition(String memberAlias) {
        return properties.enabled()
                ? "NOT EXISTS (SELECT 1 FROM private_account pa WHERE pa.member_id = " + memberAlias + ".id)" : "TRUE";
    }

    /** 보는 사람({@code viewer}, 없으면 -1)이 볼 수 있는 작성자만 남기는 조건: 본인·공개 계정·내가 팔로우하는 비공개 계정. */
    public String visibleToCondition(String memberAlias, long viewer) {
        if (!properties.enabled()) {
            return "TRUE";
        }
        return "(" + memberAlias + ".id = " + viewer + " OR " + listCondition(memberAlias)
                + " OR EXISTS (SELECT 1 FROM follow pf WHERE pf.follower_id = " + viewer + " AND pf.followee_id = " + memberAlias + ".id))";
    }

    /** 팔로우 버튼에서 비공개 계정에 팔로우를 누르면: 요청을 남기고 알린다. 같은 요청을 다시 보내도 한 줄. */
    public void request(long requester, long target) {
        boolean created = !jdbc.queryForList("""
                INSERT INTO follow_request (requester_id, target_id, created_at) VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING RETURNING requester_id
                """, Long.class, requester, target, Timestamp.from(clock.instant())).isEmpty();
        if (created) {
            events.publishEvent(new FollowRequested(requester, target, clock.instant()));
        }
    }

    public boolean requested(long requester, long target) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM follow_request WHERE requester_id = ? AND target_id = ?)", Boolean.class,
                requester, target));
    }

    /** 요청 취소(요청한 사람이 다시 누름)·거절. 상대에게 알리지 않는다. */
    public boolean cancel(long requester, long target) {
        boolean removed = jdbc.update("DELETE FROM follow_request WHERE requester_id = ? AND target_id = ?", requester, target) > 0;
        if (removed) {
            events.publishEvent(new FollowRequestClosed(requester, target));
        }
        return removed;
    }

    /** 받은 요청 수락: 팔로우가 된다. 요청이 없으면 아무것도 하지 않는다. */
    public void accept(long owner, long requester) {
        tx.executeWithoutResult(status -> {
            if (jdbc.update("DELETE FROM follow_request WHERE requester_id = ? AND target_id = ?", requester, owner) == 0) {
                return;
            }
            Instant now = clock.instant();
            jdbc.update("INSERT INTO follow (follower_id, followee_id, created_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    requester, owner, Timestamp.from(now));
            events.publishEvent(new FollowRequestClosed(requester, owner));
            events.publishEvent(new MemberFollowed(requester, owner, now));
        });
    }

    /** 받은 요청(최근 순). */
    public List<Requester> requests(long owner) {
        return jdbc.query("""
                SELECT m.id, m.handle, m.nickname, m.profile_image_url FROM follow_request fr JOIN member m ON m.id = fr.requester_id
                WHERE fr.target_id = ? AND m.withdrawn_at IS NULL ORDER BY fr.created_at DESC LIMIT 1000
                """, (rs, n) -> new Requester(rs.getLong("id"), rs.getString("handle"), rs.getString("nickname"),
                rs.getString("profile_image_url")), owner);
    }

    /** 비공개 켜기·끄기. 끄면 대기 중 요청을 모두 수락한다. */
    public void setPrivate(long owner, boolean on) {
        if (on) {
            jdbc.update("INSERT INTO private_account (member_id) VALUES (?) ON CONFLICT DO NOTHING", owner);
            return;
        }
        List<Long> pending = jdbc.queryForList("SELECT requester_id FROM follow_request WHERE target_id = ?", Long.class, owner);
        tx.executeWithoutResult(status -> jdbc.update("DELETE FROM private_account WHERE member_id = ?", owner));
        pending.forEach(requester -> accept(owner, requester));
    }

    /** 탈퇴 정리: 비공개 설정과 보낸·받은 요청. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM follow_request WHERE requester_id = ? OR target_id = ?", memberId, memberId);
        jdbc.update("DELETE FROM private_account WHERE member_id = ?", memberId);
    }
}

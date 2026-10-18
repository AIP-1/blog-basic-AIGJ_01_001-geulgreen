package com.team.blog.mission.application;

import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 111 후보(강성찬 개인 확장): 모임. 누구나 만들고 초대 링크로만 들어온다(탐색에 안 보임, 멤버만 봄).
 * 규칙 "N일에 1편": 시작일부터 N일씩 끊은 기간마다 그 기간 안에 처음 전체 공개한 글이 있으면 "썼어요".
 */
@Service
public class ClubService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 모임 글 목록에 보여 줄 수. */
    static final int POSTS = 30;

    public record Club(long id, long ownerId, String name, String intro, int everyDays, LocalDate startDate, LocalDate endDate,
                       String inviteCode, int memberCount, String joinMode, int memberCap) {

        public String modeLabel() {
            return ClubService.modeLabel(joinMode);
        }

        public boolean full() {
            return memberCount >= memberCap;
        }

        /**
         * 진행 표시: "N/M명 썼어요"(비율) 또는 "이번 기간 N명이 썼어요"(수만). 누구나 들어오는 방은 커지면 비율이 낮아 보여 수만 보인다.
         * 모두 비율이면 {@code true}, 모두 수만이면 {@code false}로 바꾸면 된다.
         */
        public boolean showsRatio() {
            return !OPEN.equals(joinMode);
        }
    }

    public static final String LINK = "LINK";
    public static final String APPROVAL = "APPROVAL";
    public static final String OPEN = "OPEN";
    public static final List<String> MODES = List.of(APPROVAL, OPEN, LINK);
    /** 새 함께 쓰기의 멤버 상한 기본값과 범위(만든 사람 포함). */
    public static final int DEFAULT_CAP = 30;
    public static final int MAX_CAP = 100;
    /** 한 사람이 하루에 보낼 수 있는 초대 수(한국 날짜 기준). */
    public static final int DAILY_INVITES = 20;
    /** 거절된 참여 요청을 다시 보낼 수 있을 때까지. */
    static final int REJECT_COOLDOWN_DAYS = 7;

    public static String modeLabel(String mode) {
        return switch (mode) {
            case OPEN -> "누구나 바로";
            case APPROVAL -> "승인 후 참여";
            default -> "링크로만";
        };
    }

    /** 화면에 그대로 보여 줄 문장으로 실패(정원이 참, 한도 넘음 등). */
    public static class ClubException extends RuntimeException {
        public ClubException(String message) {
            super(message);
        }
    }

    /** 참여 요청·초대 한 줄(사람 정보 포함). */
    public record Pending(long clubId, String clubName, long memberId, String handle, String nickname, String profileImageUrl,
                          String inviterName) {

        public String name() {
            return nickname == null ? handle : nickname;
        }

        public com.team.blog.account.application.ProfileAvatar avatar() {
            return new com.team.blog.account.application.ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    /** 초대 고르기 후보: 나를 팔로우하는 사람·친한 친구. {@code mutual}은 서로 팔로우. */
    public record Candidate(long memberId, String handle, String nickname, String profileImageUrl, boolean mutual) {

        public String name() {
            return nickname == null ? handle : nickname;
        }

        public com.team.blog.account.application.ProfileAvatar avatar() {
            return new com.team.blog.account.application.ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    /** 멤버가 아닌 사람이 보는 공개 함께 쓰기: 내 요청 상태(NONE·REQUESTED·REJECTED)와 받은 초대. */
    public record Preview(Club club, String requestState, boolean invited, boolean ended) {
    }

    /** 지금(또는 가장 가까운) 기간: 몇 번째, 시작·끝 날짜. */
    public record Period(int index, int total, LocalDate start, LocalDate end) {
    }

    public record Member(long memberId, String handle, String nickname, String profileImageUrl, boolean wrote, boolean owner,
                         boolean admin) {

        public String name() {
            return nickname == null ? handle : nickname;
        }

        public com.team.blog.account.application.ProfileAvatar avatar() {
            return new com.team.blog.account.application.ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    public record Board(Club club, Period period, List<Member> members, List<Long> postIds, boolean isOwner, boolean ended,
                        boolean notStarted, boolean isAdmin) {
    }

    /** 화면 값. 잘못 낸 값을 flash(세션)로 돌려보내서 Serializable. */
    public record Form(String name, String intro, int everyDays, LocalDate startDate, LocalDate endDate, String joinMode, int memberCap)
            implements java.io.Serializable {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;

    public ClubService(JdbcTemplate jdbc, AccountGuard accountGuard, Clock clock,
                       org.springframework.context.ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.clock = clock;
        this.events = events;
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), SEOUL);
    }

    /** 잘못된 값이면 화면에 보여 줄 문장. 맞으면 빈 값. */
    public static Optional<String> problem(Form f) {
        if (f.name() == null || f.name().isBlank() || f.name().strip().length() > 30) {
            return Optional.of("이름은 1~30자로 써 주세요");
        }
        if (f.intro() != null && f.intro().strip().length() > 100) {
            return Optional.of("한 줄 소개는 100자까지예요");
        }
        if (f.everyDays() < 1 || f.everyDays() > 30) {
            return Optional.of("규칙은 1~30일에 1편으로 정해 주세요");
        }
        if (f.startDate() == null || f.endDate() == null || f.endDate().isBefore(f.startDate())) {
            return Optional.of("끝 날짜는 시작 날짜와 같거나 뒤여야 해요");
        }
        if (ChronoUnit.DAYS.between(f.startDate(), f.endDate()) > 366) {
            return Optional.of("기간은 1년까지예요");
        }
        if (!MODES.contains(f.joinMode())) {
            return Optional.of("참여 방식을 골라 주세요");
        }
        if (f.memberCap() < 2 || f.memberCap() > MAX_CAP) {
            return Optional.of("멤버 수는 2~" + MAX_CAP + "명으로 정해 주세요");
        }
        return Optional.empty();
    }

    @Transactional
    public long create(Optional<CurrentUser> current, Form f) {
        CurrentUser me = accountGuard.requireWritable(current);
        if (problem(f).isPresent()) {
            throw new IllegalArgumentException(problem(f).get());
        }
        String code = HexFormat.of().formatHex(bytes());
        Long id = jdbc.queryForObject("""
                INSERT INTO writing_club (owner_id, name, intro, every_days, start_date, end_date, invite_code, join_mode, member_cap)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, me.memberId(), f.name().strip(), f.intro() == null ? "" : f.intro().strip(), f.everyDays(),
                Date.valueOf(f.startDate()), Date.valueOf(f.endDate()), code, f.joinMode(), f.memberCap());
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?)", id, me.memberId());
        return id;
    }

    private static byte[] bytes() {
        byte[] b = new byte[12];
        RANDOM.nextBytes(b);
        return b;
    }

    public List<Club> mine(long memberId) {
        return jdbc.query(SELECT + " WHERE c.id IN (SELECT club_id FROM writing_club_member WHERE member_id = ?) ORDER BY c.end_date DESC, c.id DESC",
                (rs, i) -> club(rs), memberId);
    }

    /** 목록 카드의 "썼어요/아직이에요": 이번 기간에 내가 처음 전체 공개한 글이 있는 함께 쓰기 id. */
    public Set<Long> wroteThisPeriod(long memberId, List<Club> clubs) {
        LocalDate today = today();
        return clubs.stream().filter(c -> {
            Period p = period(c, today);
            return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM post p WHERE p.author_id = ? AND " + PUBLIC
                    + " AND (p.first_public_at AT TIME ZONE 'Asia/Seoul')::date BETWEEN ? AND ?)", Boolean.class,
                    memberId, Date.valueOf(p.start()), Date.valueOf(p.end())));
        }).map(Club::id).collect(Collectors.toSet());
    }

    public Optional<Club> byInvite(String code) {
        return jdbc.query(SELECT + " WHERE c.invite_code = ?", (rs, i) -> club(rs), code).stream().findFirst();
    }

    public boolean isMember(long clubId, long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM writing_club_member WHERE club_id = ? AND member_id = ?)", Boolean.class, clubId, memberId));
    }

    /** 초대 링크로 들어오기. 이미 멤버면 그대로. 승인 후 참여 방은 링크로 와도 참여 요청이 된다(관리자 승인 없이는 못 들어옴). */
    @Transactional
    public long join(Optional<CurrentUser> current, String code) {
        CurrentUser me = accountGuard.requireWritable(current);
        Club club = byInvite(code).orElseThrow(NotFoundException::new);
        if (APPROVAL.equals(club.joinMode()) && !isMember(club.id(), me.memberId())) {
            request(club, me.memberId());
            return club.id();
        }
        addMember(club, me.memberId());
        return club.id();
    }

    /** 만든 사람이거나 관리자(부방장). */
    public boolean isAdmin(long clubId, long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM writing_club c WHERE c.id = ? AND c.owner_id = ?)
                    OR EXISTS (SELECT 1 FROM writing_club_member m WHERE m.club_id = ? AND m.member_id = ? AND m.role = 'ADMIN')
                """, Boolean.class, clubId, memberId, clubId, memberId));
    }

    /** 만든 사람만: 멤버를 관리자로 정하거나 해제. */
    public void setAdmin(Optional<CurrentUser> current, long clubId, long memberId, boolean admin) {
        requireOwner(current, clubId);
        jdbc.update("UPDATE writing_club_member SET role = ? WHERE club_id = ? AND member_id = ?", admin ? "ADMIN" : "MEMBER", clubId, memberId);
    }

    /** 멤버만 본다. 아니면 없는 모임처럼 404. */
    public Board board(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        Club club = jdbc.query(SELECT + " WHERE c.id = ?", (rs, i) -> club(rs), clubId).stream().findFirst()
                .orElseThrow(NotFoundException::new);
        if (!isMember(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        LocalDate today = today();
        Period period = period(club, today);
        Set<Long> wrote = jdbc.queryForList("""
                SELECT DISTINCT p.author_id FROM post p JOIN writing_club_member m ON m.member_id = p.author_id AND m.club_id = ?
                WHERE """ + " " + PUBLIC + " AND (p.first_public_at AT TIME ZONE 'Asia/Seoul')::date BETWEEN ? AND ?",
                Long.class, clubId, Date.valueOf(period.start()), Date.valueOf(period.end())).stream().collect(Collectors.toSet());
        List<Member> members = jdbc.query("""
                SELECT mb.id, mb.handle, mb.nickname, mb.profile_image_url, m.role FROM writing_club_member m JOIN member mb ON mb.id = m.member_id
                WHERE m.club_id = ? ORDER BY m.joined_at, mb.id
                """, (rs, i) -> new Member(rs.getLong("id"), rs.getString("handle"), rs.getString("nickname"), rs.getString("profile_image_url"),
                wrote.contains(rs.getLong("id")), rs.getLong("id") == club.ownerId(),
                rs.getLong("id") == club.ownerId() || "ADMIN".equals(rs.getString("role"))), clubId);
        List<Long> postIds = jdbc.queryForList("""
                SELECT p.id FROM post p JOIN writing_club_member m ON m.member_id = p.author_id AND m.club_id = ?
                WHERE """ + " " + PUBLIC + " AND (p.first_public_at AT TIME ZONE 'Asia/Seoul')::date BETWEEN ? AND ?"
                + " ORDER BY p.first_public_at DESC LIMIT " + POSTS,
                Long.class, clubId, Date.valueOf(club.startDate()), Date.valueOf(club.endDate()));
        return new Board(club, period, members, postIds, club.ownerId() == me.memberId(), today.isAfter(club.endDate()),
                today.isBefore(club.startDate()), isAdmin(clubId, me.memberId()));
    }

    /** 오늘이 든 기간. 시작 전이면 첫 기간, 끝난 뒤면 마지막 기간. 마지막 기간은 끝 날짜에서 잘린다. */
    static Period period(Club c, LocalDate today) {
        long days = ChronoUnit.DAYS.between(c.startDate(), c.endDate()) + 1;
        int total = (int) ((days + c.everyDays() - 1) / c.everyDays());
        LocalDate d = today.isBefore(c.startDate()) ? c.startDate() : today.isAfter(c.endDate()) ? c.endDate() : today;
        int index = (int) (ChronoUnit.DAYS.between(c.startDate(), d) / c.everyDays());
        LocalDate start = c.startDate().plusDays((long) index * c.everyDays());
        LocalDate end = start.plusDays(c.everyDays() - 1L);
        return new Period(index + 1, total, start, end.isAfter(c.endDate()) ? c.endDate() : end);
    }

    /** 멤버가 나간다. 만든 사람은 나갈 수 없다(모임 삭제를 쓴다). */
    public void leave(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        jdbc.update("DELETE FROM writing_club_member WHERE club_id = ? AND member_id = ? AND member_id <> (SELECT owner_id FROM writing_club WHERE id = ?)",
                clubId, me.memberId(), clubId);
    }

    /** 만든 사람만: 멤버 내보내기. */
    public void remove(Optional<CurrentUser> current, long clubId, long memberId) {
        CurrentUser me = requireOwner(current, clubId);
        if (memberId != me.memberId()) {
            jdbc.update("DELETE FROM writing_club_member WHERE club_id = ? AND member_id = ?", clubId, memberId);
        }
    }

    /** 만든 사람만: 모임 삭제(멤버 줄도 함께). 글은 그대로. */
    public void delete(Optional<CurrentUser> current, long clubId) {
        requireOwner(current, clubId);
        jdbc.update("DELETE FROM writing_club WHERE id = ?", clubId);
    }

    private CurrentUser requireOwner(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        Integer n = jdbc.queryForObject("SELECT count(*) FROM writing_club WHERE id = ? AND owner_id = ?", Integer.class, clubId, me.memberId());
        if (n == null || n == 0) {
            throw new NotFoundException();
        }
        return me;
    }

    // ----- 111 후속: 공개 목록, 참여 요청, 초대, 상한 -----

    /** 공개 목록: 승인 후 참여·누구나 바로인 아직 안 끝난 함께 쓰기. {@code q}가 있으면 이름·소개 부분 일치. */
    public List<Club> listed(String q) {
        String term = q == null ? "" : q.strip();
        String like = com.team.blog.discovery.application.SearchTerms.likePattern(term);
        return jdbc.query(SELECT + """
                 WHERE c.join_mode <> 'LINK' AND c.end_date >= ?
                   AND (? = '' OR c.name ILIKE ? ESCAPE '\\' OR c.intro ILIKE ? ESCAPE '\\')
                 ORDER BY c.created_at DESC, c.id DESC LIMIT 30
                """, (rs, i) -> club(rs), Date.valueOf(today()), term, like, like);
    }

    public Optional<Club> byId(long clubId) {
        return jdbc.query(SELECT + " WHERE c.id = ?", (rs, i) -> club(rs), clubId).stream().findFirst();
    }

    /** 멤버로 넣는다(이미 멤버면 그대로). 정원이 차면 실패. */
    private void addMember(Club club, long memberId) {
        if (isMember(club.id(), memberId)) {
            return;
        }
        // 정원 확인과 넣기를 한 문장으로(동시에 들어와도 넘지 않게)
        int n = jdbc.update("""
                INSERT INTO writing_club_member (club_id, member_id)
                SELECT c.id, ? FROM writing_club c WHERE c.id = ?
                  AND (SELECT count(*) FROM writing_club_member m WHERE m.club_id = c.id) < c.member_cap
                ON CONFLICT DO NOTHING
                """, memberId, club.id());
        if (n == 0 && !isMember(club.id(), memberId)) {
            throw new ClubException("정원(" + club.memberCap() + "명)이 다 찼어요");
        }
        jdbc.update("DELETE FROM writing_club_join WHERE club_id = ? AND member_id = ?", club.id(), memberId);
    }

    /** 멤버가 아닌 사람이 공개 함께 쓰기(또는 초대받은 것)를 볼 때. 링크만인데 초대도 없으면 없는 것처럼. */
    public Preview preview(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        Optional<java.util.Map<String, Object>> row = jdbc.queryForList(
                "SELECT kind, rejected_at FROM writing_club_join WHERE club_id = ? AND member_id = ?", clubId, me.memberId()).stream().findFirst();
        boolean invited = row.map(r -> "INVITE".equals(r.get("kind"))).orElse(false);
        if (LINK.equals(club.joinMode()) && !invited) {
            throw new NotFoundException();
        }
        String state = row.filter(r -> "REQUEST".equals(r.get("kind")))
                .map(r -> r.get("rejected_at") == null ? "REQUESTED" : "REJECTED").orElse("NONE");
        return new Preview(club, state, invited, today().isAfter(club.endDate()));
    }

    /** 공개 함께 쓰기에서 [참여하기](누구나 바로) 또는 [참여 요청](승인 후 참여). 들어갔으면 {@code true}. */
    @Transactional
    public boolean joinListed(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireWritable(current);
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        if (LINK.equals(club.joinMode())) {
            throw new NotFoundException();
        }
        if (today().isAfter(club.endDate())) {
            throw new ClubException("끝난 함께 쓰기예요");
        }
        if (OPEN.equals(club.joinMode())) {
            addMember(club, me.memberId());
            return true;
        }
        if (isMember(clubId, me.memberId())) {
            return true;
        }
        request(club, me.memberId());
        return false;
    }

    /** 참여 요청을 남기고 관리자들에게 알린다(정원이 찼거나 거절된 지 7일이 안 됐으면 실패). */
    private void request(Club club, long memberId) {
        long clubId = club.id();
        if (club.full()) {
            throw new ClubException("정원(" + club.memberCap() + "명)이 다 찼어요");
        }
        Optional<java.sql.Timestamp> rejected = jdbc.queryForList(
                "SELECT rejected_at FROM writing_club_join WHERE club_id = ? AND member_id = ? AND kind = 'REQUEST'",
                java.sql.Timestamp.class, clubId, memberId).stream().filter(java.util.Objects::nonNull).findFirst();
        if (rejected.isPresent() && rejected.get().toInstant().isAfter(clock.instant().minus(java.time.Duration.ofDays(REJECT_COOLDOWN_DAYS)))) {
            throw new ClubException("거절된 요청은 " + REJECT_COOLDOWN_DAYS + "일 뒤에 다시 보낼 수 있어요");
        }
        int n = jdbc.update("""
                INSERT INTO writing_club_join (club_id, member_id, kind) VALUES (?, ?, 'REQUEST')
                ON CONFLICT (club_id, member_id) DO UPDATE SET kind = 'REQUEST', invited_by = NULL, rejected_at = NULL,
                  created_at = CURRENT_TIMESTAMP WHERE writing_club_join.kind = 'REQUEST'
                """, clubId, memberId);
        if (n > 0) {
            for (Long admin : admins(clubId)) {
                events.publishEvent(new com.team.blog.shared.event.ClubNotice(clubId, admin, memberId, false));
            }
        }
    }

    private List<Long> admins(long clubId) {
        return jdbc.queryForList("""
                SELECT owner_id FROM writing_club WHERE id = ?
                UNION SELECT member_id FROM writing_club_member WHERE club_id = ? AND role = 'ADMIN'
                """, Long.class, clubId, clubId);
    }

    /** 내 참여 요청 거두기. */
    public void cancelRequest(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        jdbc.update("DELETE FROM writing_club_join WHERE club_id = ? AND member_id = ? AND kind = 'REQUEST' AND rejected_at IS NULL",
                clubId, me.memberId());
    }

    /** 만든 사람이 볼 기다리는 참여 요청. */
    public List<Pending> requests(long clubId) {
        return jdbc.query("""
                SELECT j.club_id, '' AS club_name, m.id, m.handle, m.nickname, m.profile_image_url, NULL AS inviter
                FROM writing_club_join j JOIN member m ON m.id = j.member_id
                WHERE j.club_id = ? AND j.kind = 'REQUEST' AND j.rejected_at IS NULL AND m.withdrawn_at IS NULL
                ORDER BY j.created_at, m.id
                """, (rs, i) -> pending(rs), clubId);
    }

    /** 만든 사람·관리자만: 요청 승인(정원 확인) 또는 거절(7일 동안 다시 못 보냄). */
    @Transactional
    public void decide(Optional<CurrentUser> current, long clubId, long memberId, boolean approve) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        if (!isAdmin(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        Integer pending = jdbc.queryForObject("SELECT count(*) FROM writing_club_join WHERE club_id = ? AND member_id = ? AND kind = 'REQUEST' AND rejected_at IS NULL",
                Integer.class, clubId, memberId);
        if (pending == null || pending == 0) {
            return;
        }
        if (approve) {
            addMember(byId(clubId).orElseThrow(NotFoundException::new), memberId);
        } else {
            jdbc.update("UPDATE writing_club_join SET rejected_at = ? WHERE club_id = ? AND member_id = ?",
                    java.sql.Timestamp.from(clock.instant()), clubId, memberId);
        }
    }

    /** 만든 사람만: 참여 방식과 정원(지금 멤버 수보다 적게는 못 함). */
    public void settings(Optional<CurrentUser> current, long clubId, String mode, int cap) {
        requireOwner(current, clubId);
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        if (!MODES.contains(mode)) {
            throw new ClubException("참여 방식을 골라 주세요");
        }
        if (cap < Math.max(2, club.memberCount()) || cap > MAX_CAP) {
            throw new ClubException("정원은 지금 멤버 수(" + club.memberCount() + "명)부터 " + MAX_CAP + "명까지 정할 수 있어요");
        }
        jdbc.update("UPDATE writing_club SET join_mode = ?, member_cap = ? WHERE id = ?", mode, cap, clubId);
    }

    /** 초대 고르기 후보: 나를 팔로우하는 사람과 내 친한 친구 중 아직 멤버·초대 대기가 아닌 사람(서로 팔로우가 먼저). */
    public List<Candidate> inviteCandidates(long me, long clubId) {
        return jdbc.query("""
                SELECT m.id, m.handle, m.nickname, m.profile_image_url,
                       EXISTS (SELECT 1 FROM follow f1 WHERE f1.follower_id = m.id AND f1.followee_id = ?)
                       AND EXISTS (SELECT 1 FROM follow f2 WHERE f2.follower_id = ? AND f2.followee_id = m.id) AS mutual
                FROM member m
                WHERE m.id <> ? AND m.withdrawn_at IS NULL AND m.status = 'ACTIVE'
                  AND (EXISTS (SELECT 1 FROM follow f WHERE f.follower_id = m.id AND f.followee_id = ?)
                       OR EXISTS (SELECT 1 FROM close_friend cf WHERE cf.owner_id = ? AND cf.friend_id = m.id))
                  AND NOT EXISTS (SELECT 1 FROM writing_club_member cm WHERE cm.club_id = ? AND cm.member_id = m.id)
                  AND NOT EXISTS (SELECT 1 FROM writing_club_join j WHERE j.club_id = ? AND j.member_id = m.id AND j.kind = 'INVITE')
                ORDER BY mutual DESC, m.handle LIMIT 200
                """, (rs, i) -> new Candidate(rs.getLong("id"), rs.getString("handle"), rs.getString("nickname"),
                rs.getString("profile_image_url"), rs.getBoolean("mutual")), me, me, me, me, me, clubId, clubId);
    }

    /** 오늘(한국 날짜) 보낸 초대 수. */
    public int invitesToday(long me) {
        java.time.Instant dayStart = today().atStartOfDay(SEOUL).toInstant();
        Integer n = jdbc.queryForObject("SELECT count(*) FROM writing_club_join WHERE invited_by = ? AND created_at >= ?",
                Integer.class, me, java.sql.Timestamp.from(dayStart));
        return n == null ? 0 : n;
    }

    /** 멤버가 팔로워·친한 친구를 초대(후보에 있는 사람만, 하루 한도·정원 안에서). 보낸 수. */
    @Transactional
    public int invite(Optional<CurrentUser> current, long clubId, List<Long> memberIds) {
        CurrentUser me = accountGuard.requireWritable(current);
        if (!isMember(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        if (today().isAfter(club.endDate())) {
            throw new ClubException("끝난 함께 쓰기예요");
        }
        Set<Long> allowed = inviteCandidates(me.memberId(), clubId).stream().map(Candidate::memberId).collect(Collectors.toSet());
        List<Long> targets = memberIds.stream().distinct().filter(allowed::contains).toList();
        int left = DAILY_INVITES - invitesToday(me.memberId());
        if (targets.size() > left) {
            throw new ClubException("초대는 하루 " + DAILY_INVITES + "명까지 보낼 수 있어요(오늘 " + Math.max(0, left) + "명 남음)");
        }
        if (club.memberCount() + targets.size() > club.memberCap()) {
            throw new ClubException("정원(" + club.memberCap() + "명)을 넘어서 초대할 수 없어요");
        }
        java.sql.Timestamp now = java.sql.Timestamp.from(clock.instant());
        for (Long id : targets) {
            // 이미 참여를 요청한 사람이면 초대로 바꿔 받는 사람이 수락만 하면 되게
            jdbc.update("""
                    INSERT INTO writing_club_join (club_id, member_id, kind, invited_by, created_at) VALUES (?, ?, 'INVITE', ?, ?)
                    ON CONFLICT (club_id, member_id) DO UPDATE SET kind = 'INVITE', invited_by = EXCLUDED.invited_by,
                      rejected_at = NULL, created_at = EXCLUDED.created_at
                    """, clubId, id, me.memberId(), now);
            events.publishEvent(new com.team.blog.shared.event.ClubNotice(clubId, id, me.memberId(), true));
        }
        return targets.size();
    }

    /** 내가 받은 초대(아직 안 끝난 것). */
    public List<Pending> myInvites(long me) {
        return jdbc.query("""
                SELECT c.id AS club_id, c.name AS club_name, i.id, i.handle, i.nickname, i.profile_image_url,
                       COALESCE(i.nickname, i.handle) AS inviter
                FROM writing_club_join j JOIN writing_club c ON c.id = j.club_id JOIN member i ON i.id = j.invited_by
                WHERE j.member_id = ? AND j.kind = 'INVITE' AND c.end_date >= ?
                ORDER BY j.created_at DESC
                """, (rs, i) -> pending(rs), me, Date.valueOf(today()));
    }

    /** 받은 초대 수락(정원 확인) 또는 거절. 수락했는데 승인을 기다리게 되면 {@code false}. */
    @Transactional
    public boolean answerInvite(Optional<CurrentUser> current, long clubId, boolean accept) {
        CurrentUser me = accountGuard.requireWritable(current);
        Integer n = jdbc.queryForObject("SELECT count(*) FROM writing_club_join WHERE club_id = ? AND member_id = ? AND kind = 'INVITE'",
                Integer.class, clubId, me.memberId());
        if (n == null || n == 0) {
            throw new NotFoundException();
        }
        if (!accept) {
            jdbc.update("DELETE FROM writing_club_join WHERE club_id = ? AND member_id = ?", clubId, me.memberId());
            return true;
        }
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        Long inviter = jdbc.queryForObject("SELECT invited_by FROM writing_club_join WHERE club_id = ? AND member_id = ?",
                Long.class, clubId, me.memberId());
        // 승인 후 참여 방: 관리자가 보낸 초대만 승인으로 친다. 일반 멤버의 초대는 수락하면 참여 요청이 된다
        if (APPROVAL.equals(club.joinMode()) && (inviter == null || !isAdmin(clubId, inviter))) {
            jdbc.update("DELETE FROM writing_club_join WHERE club_id = ? AND member_id = ?", clubId, me.memberId());
            request(club, me.memberId());
            return false;
        }
        addMember(club, me.memberId());
        return true;
    }

    private static Pending pending(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Pending(rs.getLong("club_id"), rs.getString("club_name"), rs.getLong("id"), rs.getString("handle"),
                rs.getString("nickname"), rs.getString("profile_image_url"), rs.getString("inviter"));
    }

    /** 탈퇴 정리: 멤버 줄, 요청·초대, 만든 함께 쓰기. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM writing_club_join WHERE member_id = ? OR invited_by = ?", memberId, memberId);
        jdbc.update("DELETE FROM writing_club_member WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM writing_club WHERE owner_id = ?", memberId);
    }

    private static final String PUBLIC = "p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND p.deleted_at IS NULL AND p.hidden_at IS NULL"
            + " AND p.first_public_at IS NOT NULL";

    private static final String SELECT = """
            SELECT c.id, c.owner_id, c.name, c.intro, c.every_days, c.start_date, c.end_date, c.invite_code, c.join_mode, c.member_cap,
                   (SELECT count(*) FROM writing_club_member m WHERE m.club_id = c.id) AS member_count
            FROM writing_club c""";

    private static Club club(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Club(rs.getLong("id"), rs.getLong("owner_id"), rs.getString("name"), rs.getString("intro"), rs.getInt("every_days"),
                rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(), rs.getString("invite_code"),
                rs.getInt("member_count"), rs.getString("join_mode"), rs.getInt("member_cap"));
    }
}

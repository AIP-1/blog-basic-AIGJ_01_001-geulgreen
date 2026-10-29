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
 * 111 후보(강성찬 개인 확장): 함께 쓰기 방. 10-25 chan: 기간을 없애고 오픈채팅방처럼 — 방이 열린 날부터 멤버들의 전체 공개 글이
 * 시간 순으로 모이고, "오늘 쓴 사람"과 방 글감(만든 사람·관리자가 올림)을 보여 준다. 기간 칸(every_days·end_date)은 DB에 남기고 쓰지 않는다.
 */
@Service
public class ClubService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 방 화면에 보여 줄 글 수. */
    public static final int POSTS = 30;
    /** 새 방의 끝 날짜(기간 없음). 예전 칸의 NOT NULL·CHECK를 지키려고 넣는다. */
    private static final LocalDate NO_END = LocalDate.of(9999, 12, 31);
    /** 지난 글감은 최근 것부터 이만큼. */
    static final int PAST_TOPICS = 10;
    /** 먼저 쓰고 보기: 글감을 올리고 이만큼 지나면 안 쓴 사람에게도 방 글감 글이 열린다. */
    static final java.time.Duration WRITE_FIRST_WINDOW = java.time.Duration.ofHours(24);
    /** 방 글감이 겹치지 않아야 하는 기간. */
    static final java.time.Duration YEAR = java.time.Duration.ofDays(365);

    public record Club(long id, long ownerId, String name, String intro, LocalDate startDate, String inviteCode, int memberCount,
                       String joinMode, Integer memberCap, boolean writeFirst, String subject) {

        public String modeLabel() {
            return ClubService.modeLabel(joinMode);
        }

        /** 10-28: 인원 제한(memberCap)이 없으면 언제나 들어올 수 있다. */
        public boolean full() {
            return memberCap != null && memberCount >= memberCap;
        }

        /** 목록에 보일 멤버 수: 제한이 있으면 "멤버 3/30명", 없으면 "멤버 3명". */
        public String memberLabel() {
            return memberCap == null ? "멤버 " + memberCount + "명" : "멤버 " + memberCount + "/" + memberCap + "명";
        }
    }

    public static final String LINK = "LINK";
    public static final String APPROVAL = "APPROVAL";
    public static final String OPEN = "OPEN";
    public static final List<String> MODES = List.of(APPROVAL, OPEN, LINK);
    /** 10-28 chan: 방은 기본으로 인원 제한이 없고, 방장이 켜면 이 범위에서 정한다(만든 사람 포함). 켤 때 처음 보이는 값은 SUGGESTED_CAP. */
    public static final int SUGGESTED_CAP = 30;
    public static final int MAX_CAP = 1000;
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
    public record Preview(Club club, String requestState, boolean invited) {
    }

    /** 멤버 한 줄. {@code lastWrote}는 마지막으로 전체 공개한 때(없으면 null), {@code wroteToday}는 한국 날짜로 오늘 썼는지. */
    public record Member(long memberId, String handle, String nickname, String profileImageUrl, java.time.Instant lastWrote,
                         boolean wroteToday, String lastLabel, boolean owner, boolean admin) {

        public String name() {
            return nickname == null ? handle : nickname;
        }

        public com.team.blog.account.application.ProfileAvatar avatar() {
            return new com.team.blog.account.application.ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    /** 방 글감. {@code writers}는 이 글감으로 전체 공개한 사람 수. */
    public record Topic(long id, String title, String hint, String byName, java.time.Instant createdAt, int writers) {
    }

    /** 지난 1년 안에 올린 방 글감(겹침 확인·AI 질문용). */
    public record YearTopic(String title, java.time.Instant createdAt) {
    }

    /** 방 소식 줄(글이 아닌 것): 들어온 사람, 올라온 글감. */
    public record Notice(java.time.Instant at, String text) {
    }

    /**
     * {@code lockedPostIds}: 먼저 쓰고 보기로 나에게 흐리게 보이는 글(남의 방 글감 글 중 내가 그 글감으로 아직 안 썼고 하루가 안 지난 것).
     * {@code wroteTopic}: 지금 방 글감으로 내가 전체 공개했는지. {@code topicLocked}: 지금 방 글감 글이 나에게 잠겨 있는지(글감 줄 문구·[쓰기] 강조).
     */
    public record Board(Club club, List<Member> members, List<Long> postIds, boolean isOwner, boolean isAdmin, Topic topic,
                        List<Topic> pastTopics, Set<Long> topicPostIds, List<Notice> notices, Set<Long> lockedPostIds,
                        boolean wroteTopic, boolean topicLocked) {

        public long wroteToday() {
            return members.stream().filter(Member::wroteToday).count();
        }
    }

    /** 방 목록 한 줄: 마지막 글(없으면 빈 값), 내가 마지막으로 본 뒤 올라온 남의 글 수, 지금 방 글감 제목. */
    public record Room(Club club, String lastAuthor, String lastTitle, java.time.Instant lastAt, int newCount, String topicTitle) {
    }

    /** 방 글감으로 연 글(글쓰기 화면 안내용). */
    public record PostTopic(long clubId, String clubName, String title, String hint) {
    }

    /** 화면 값. 잘못 낸 값을 flash(세션)로 돌려보내서 Serializable. */
    public record Form(String name, String intro, String joinMode, Integer memberCap, String subject) implements java.io.Serializable {
    }

    /** 10-28 발행 설정의 "올릴 방" 한 칸: 내가 든 방과, 이 글이 그 방에 올라가 있는지. */
    public record PostRoom(long clubId, String name, String subject, boolean picked) {
    }

    /** 방 주제 최대 글자 수(DB varchar(40)). */
    public static final int SUBJECT_MAX = 40;

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final com.team.blog.post.application.PostDraftService drafts;

    private final com.team.blog.post.application.PostAccessPolicy accessPolicy;

    public ClubService(JdbcTemplate jdbc, AccountGuard accountGuard, Clock clock,
                       org.springframework.context.ApplicationEventPublisher events,
                       com.team.blog.post.application.PostDraftService drafts,
                       com.team.blog.post.application.PostAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.clock = clock;
        this.events = events;
        this.drafts = drafts;
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), SEOUL);
    }

    /** 잘못된 값이면 화면에 보여 줄 문장. 맞으면 빈 값. */
    public static Optional<String> problem(Form f) {
        if (f.name() == null || f.name().isBlank() || f.name().strip().length() > 30) {
            return Optional.of("이름은 1~30자로 써 주세요");
        }
        if (f.subject() == null || f.subject().isBlank() || f.subject().strip().length() > SUBJECT_MAX) {
            return Optional.of("방 주제는 1~" + SUBJECT_MAX + "자로 써 주세요");
        }
        if (f.intro() != null && f.intro().strip().length() > 100) {
            return Optional.of("한 줄 소개는 100자까지예요");
        }
        if (!MODES.contains(f.joinMode())) {
            return Optional.of("참여 방식을 골라 주세요");
        }
        if (f.memberCap() != null && (f.memberCap() < 2 || f.memberCap() > MAX_CAP)) {
            return Optional.of("최대 인원은 2~" + MAX_CAP + "명으로 정해 주세요");
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
        java.sql.Timestamp now = java.sql.Timestamp.from(clock.instant());
        // every_days·end_date는 더 쓰지 않지만 NOT NULL·CHECK가 있어 자리값을 넣는다
        Long id = jdbc.queryForObject("""
                INSERT INTO writing_club (owner_id, name, intro, every_days, start_date, end_date, invite_code, join_mode, member_cap, created_at, subject)
                VALUES (?, ?, ?, 7, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, me.memberId(), f.name().strip(), f.intro() == null ? "" : f.intro().strip(),
                Date.valueOf(today()), Date.valueOf(NO_END), code, f.joinMode(), f.memberCap(), now, f.subject().strip());
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id, joined_at, last_seen_at) VALUES (?, ?, ?, ?)", id, me.memberId(), now, now);
        return id;
    }

    private static byte[] bytes() {
        byte[] b = new byte[12];
        RANDOM.nextBytes(b);
        return b;
    }

    public List<Club> mine(long memberId) {
        return jdbc.query(SELECT + " WHERE c.id IN (SELECT club_id FROM writing_club_member WHERE member_id = ?) ORDER BY c.id DESC",
                (rs, i) -> club(rs), memberId);
    }

    /** 10-25 방 목록(카톡 대화방 목록처럼): 마지막 글이 최근인 방부터, 새 글 수와 지금 글감. */
    public List<Room> rooms(long memberId) {
        return jdbc.query("""
                SELECT c.id, c.owner_id, c.name, c.intro, c.start_date, c.invite_code, c.join_mode, c.member_cap, c.write_first, c.subject,
                       (SELECT count(*) FROM writing_club_member m2 WHERE m2.club_id = c.id) AS member_count,
                       last.title AS last_title, last.first_public_at AS last_at, COALESCE(la.nickname, la.handle) AS last_author,
                       (SELECT count(*) FROM """ + " " + ROOM_POSTS + """
                         WHERE cp.club_id = c.id AND """ + " " + PUBLIC + """
                           AND p.author_id <> ? AND p.first_public_at > COALESCE(me.last_seen_at, me.joined_at)) AS new_count,
                       (SELECT t.title FROM writing_club_topic t WHERE t.club_id = c.id ORDER BY t.created_at DESC, t.id DESC LIMIT 1) AS topic_title
                FROM writing_club_member me JOIN writing_club c ON c.id = me.club_id
                LEFT JOIN LATERAL (
                    SELECT p.title, p.first_public_at, p.author_id FROM """ + " " + ROOM_POSTS + """
                    WHERE cp.club_id = c.id AND """ + " " + PUBLIC + """
                    ORDER BY p.first_public_at DESC LIMIT 1) last ON true
                LEFT JOIN member la ON la.id = last.author_id
                WHERE me.member_id = ?
                ORDER BY GREATEST(last.first_public_at, c.created_at) DESC, c.id DESC
                """, (rs, i) -> {
            java.sql.Timestamp at = rs.getTimestamp("last_at");
            return new Room(club(rs), rs.getString("last_author"), rs.getString("last_title"), at == null ? null : at.toInstant(),
                    rs.getInt("new_count"), rs.getString("topic_title"));
        }, memberId, memberId);
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

    /** 멤버만 본다. 아니면 없는 모임처럼 404. 보면 "마지막으로 본 때"를 지금으로(방 목록의 새 글 수). */
    @Transactional
    public Board board(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        if (!isMember(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        java.time.Instant now = clock.instant();
        LocalDate today = today();
        List<Member> members = jdbc.query("""
                SELECT mb.id, mb.handle, mb.nickname, mb.profile_image_url, m.role,
                       (SELECT max(p.first_public_at) FROM writing_club_post cp JOIN post p ON p.id = cp.post_id
                         WHERE cp.club_id = m.club_id AND p.author_id = mb.id AND """ + " " + PUBLIC + """
                       ) AS last_wrote
                FROM writing_club_member m JOIN member mb ON mb.id = m.member_id
                WHERE m.club_id = ? ORDER BY m.joined_at, mb.id
                """, (rs, i) -> {
            java.sql.Timestamp t = rs.getTimestamp("last_wrote");
            java.time.Instant last = t == null ? null : t.toInstant();
            boolean wroteToday = last != null && !LocalDate.ofInstant(last, SEOUL).isBefore(today);
            long id = rs.getLong("id");
            return new Member(id, rs.getString("handle"), rs.getString("nickname"), rs.getString("profile_image_url"), last, wroteToday,
                    lastLabel(last, today), id == club.ownerId(), id == club.ownerId() || "ADMIN".equals(rs.getString("role")));
        }, clubId);
        // 10-28 방에 올린 글만(글쓴이가 고른 방). 나간 멤버의 글은 빠진다
        List<Long> postIds = jdbc.queryForList("SELECT p.id FROM " + ROOM_POSTS + " WHERE cp.club_id = ? AND " + PUBLIC
                + " ORDER BY p.first_public_at DESC LIMIT " + POSTS, Long.class, clubId);
        List<Topic> topics = topics(clubId, PAST_TOPICS + 1);
        Set<Long> topicPostIds = postIds.isEmpty() ? Set.of() : new java.util.HashSet<>(jdbc.queryForList("""
                SELECT tp.post_id FROM writing_club_topic_post tp JOIN writing_club_topic t ON t.id = tp.topic_id
                WHERE t.club_id = ? AND tp.post_id = ANY (?)
                """, Long.class, clubId, postIds.toArray(Long[]::new)));
        // 10-26 먼저 쓰고 보기: 남이 방 글감으로 쓴 글 중 내가 그 글감으로 아직 전체 공개하지 않았고 글감이 하루 안 된 것
        Set<Long> locked = !club.writeFirst() || topicPostIds.isEmpty() ? Set.of() : new java.util.HashSet<>(jdbc.queryForList("""
                SELECT tp.post_id FROM writing_club_topic_post tp JOIN writing_club_topic t ON t.id = tp.topic_id
                WHERE t.club_id = ? AND tp.post_id = ANY (?) AND tp.member_id <> ? AND t.created_at > ?
                  AND NOT EXISTS (SELECT 1 FROM writing_club_topic_post mine JOIN post p ON p.id = mine.post_id
                                   WHERE mine.topic_id = t.id AND mine.member_id = ? AND """ + " " + PUBLIC + """
                                  )
                """, Long.class, clubId, topicPostIds.toArray(Long[]::new), me.memberId(),
                java.sql.Timestamp.from(now.minus(WRITE_FIRST_WINDOW)), me.memberId()));
        Topic topic = topics.isEmpty() ? null : topics.get(0);
        boolean wroteTopic = topic != null && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM writing_club_topic_post tp JOIN post p ON p.id = tp.post_id
                               WHERE tp.topic_id = ? AND tp.member_id = ? AND """ + " " + PUBLIC + """
                              )
                """, Boolean.class, topic.id(), me.memberId()));
        jdbc.update("UPDATE writing_club_member SET last_seen_at = ? WHERE club_id = ? AND member_id = ?",
                java.sql.Timestamp.from(now), clubId, me.memberId());
        return new Board(club, members, postIds, club.ownerId() == me.memberId(), isAdmin(clubId, me.memberId()),
                topic, topics.size() > 1 ? topics.subList(1, topics.size()) : List.of(),
                topicPostIds, notices(clubId), locked, wroteTopic,
                topic != null && club.writeFirst() && !wroteTopic && now.isBefore(topic.createdAt().plus(WRITE_FIRST_WINDOW)));
    }

    /** 멤버 줄의 "마지막으로 쓴 때": 오늘 씀, 어제, N일 전(한 달 안), 그보다 전이면 날짜, 없으면 "아직 없어요". */
    static String lastLabel(java.time.Instant last, LocalDate today) {
        if (last == null) {
            return "아직 없어요";
        }
        LocalDate d = LocalDate.ofInstant(last, SEOUL);
        long days = ChronoUnit.DAYS.between(d, today);
        if (days <= 0) {
            return "오늘 씀";
        }
        if (days == 1) {
            return "어제";
        }
        return days < 31 ? days + "일 전" : d.getMonthValue() + "월 " + d.getDayOfMonth() + "일";
    }

    /** 방 소식 줄: 최근에 들어온 사람(만든 사람 빼고)과 올라온 글감. 화면이 글 사이에 시간 순으로 끼운다. */
    private List<Notice> notices(long clubId) {
        List<Notice> list = new java.util.ArrayList<>(jdbc.query("""
                SELECT m.joined_at AS at, COALESCE(mb.nickname, mb.handle) || '님이 들어왔어요' AS text
                FROM writing_club_member m JOIN member mb ON mb.id = m.member_id JOIN writing_club c ON c.id = m.club_id
                WHERE m.club_id = ? AND m.member_id <> c.owner_id ORDER BY m.joined_at DESC LIMIT 20
                """, (rs, i) -> new Notice(rs.getTimestamp("at").toInstant(), rs.getString("text")), clubId));
        list.addAll(jdbc.query("""
                SELECT t.created_at AS at, COALESCE(mb.nickname, mb.handle) || '님이 방 글감 「' || t.title || '」을 올렸어요' AS text
                FROM writing_club_topic t JOIN member mb ON mb.id = t.created_by
                WHERE t.club_id = ? ORDER BY t.created_at DESC LIMIT 20
                """, (rs, i) -> new Notice(rs.getTimestamp("at").toInstant(), rs.getString("text")), clubId));
        list.sort(java.util.Comparator.comparing(Notice::at).reversed());
        return list;
    }

    // ----- 10-25 방 글감 -----

    /** 최근 것부터 {@code limit}개. */
    public List<Topic> topics(long clubId, int limit) {
        return jdbc.query("""
                SELECT t.id, t.title, t.hint, t.created_at, COALESCE(mb.nickname, mb.handle) AS by_name,
                       (SELECT count(DISTINCT tp.member_id) FROM writing_club_topic_post tp JOIN post p ON p.id = tp.post_id
                         WHERE tp.topic_id = t.id AND """ + " " + PUBLIC + """
                       ) AS writers
                FROM writing_club_topic t JOIN member mb ON mb.id = t.created_by
                WHERE t.club_id = ? ORDER BY t.created_at DESC, t.id DESC LIMIT ?
                """, (rs, i) -> new Topic(rs.getLong("id"), rs.getString("title"), rs.getString("hint"), rs.getString("by_name"),
                rs.getTimestamp("created_at").toInstant(), rs.getInt("writers")), clubId, limit);
    }

    /** 10-29 chan "1년 동안은 주제가 겹치지 않게": 최근 1년 글감, 최근 것부터. */
    public List<YearTopic> yearTopics(long clubId) {
        return jdbc.query("SELECT title, created_at FROM writing_club_topic WHERE club_id = ? AND created_at > ? ORDER BY created_at DESC, id DESC",
                (rs, i) -> new YearTopic(rs.getString("title"), rs.getTimestamp("created_at").toInstant()),
                clubId, java.sql.Timestamp.from(clock.instant().minus(YEAR)));
    }

    /** 만든 사람·관리자만: 새 글감을 올린다(이전 글감은 지난 글감으로). 1년 안에 같은 글감(띄어쓰기·문장부호 무시)은 막는다. */
    public void addTopic(Optional<CurrentUser> current, long clubId, String title, String hint) {
        CurrentUser me = accountGuard.requireWritable(current);
        if (!isAdmin(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        String t = title == null ? "" : title.strip();
        String h = hint == null ? "" : hint.strip();
        if (t.isEmpty() || t.length() > 60) {
            throw new ClubException("글감은 1~60자로 써 주세요");
        }
        if (h.length() > 200) {
            throw new ClubException("글감 설명은 200자까지예요");
        }
        String key = ClubTopicPrompt.key(t);
        Optional<YearTopic> same = yearTopics(clubId).stream().filter(y -> ClubTopicPrompt.key(y.title()).equals(key)).findFirst();
        if (same.isPresent()) {
            LocalDate d = LocalDate.ofInstant(same.get().createdAt(), SEOUL);
            throw new ClubException("「" + same.get().title() + "」은 " + d.getMonthValue() + "월 " + d.getDayOfMonth()
                    + "일에 올린 글감이에요. 1년 안에 올린 글감은 다시 올릴 수 없어요");
        }
        jdbc.update("INSERT INTO writing_club_topic (club_id, created_by, title, hint, created_at) VALUES (?, ?, ?, ?, ?)",
                clubId, me.memberId(), t, h, java.sql.Timestamp.from(clock.instant()));
    }

    /** 만든 사람·관리자만: 글감 내리기(그 글감으로 쓴 글은 그대로, "방 글감" 표시만 사라짐). */
    public void deleteTopic(Optional<CurrentUser> current, long clubId, long topicId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        if (!isAdmin(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        jdbc.update("DELETE FROM writing_club_topic WHERE id = ? AND club_id = ?", topicId, clubId);
    }

    /** 멤버가 [이 글감으로 쓰기]: 그 글감으로 쓰던 임시글이 있으면 그것, 없으면 글감 제목으로 새 임시글. 글 번호. */
    @Transactional
    public long startTopic(Optional<CurrentUser> current, long clubId, long topicId) {
        CurrentUser me = accountGuard.requireWritable(current);
        if (!isMember(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        String title = jdbc.queryForList("SELECT title FROM writing_club_topic WHERE id = ? AND club_id = ?", String.class, topicId, clubId)
                .stream().findFirst().orElseThrow(NotFoundException::new);
        Optional<Long> draft = jdbc.queryForList("""
                SELECT tp.post_id FROM writing_club_topic_post tp JOIN post p ON p.id = tp.post_id
                WHERE tp.topic_id = ? AND tp.member_id = ? AND p.status = 'DRAFT' AND p.deleted_at IS NULL ORDER BY tp.created_at DESC LIMIT 1
                """, Long.class, topicId, me.memberId()).stream().findFirst();
        if (draft.isPresent()) { // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
            return draft.get();
        }
        long postId = drafts.create(current, title, "");
        jdbc.update("INSERT INTO writing_club_topic_post (post_id, topic_id, member_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                postId, topicId, me.memberId());
        sendTo(clubId, postId, me.memberId());
        return postId;
    }

    // ----- 10-28 방에 올리기: 글쓴이가 고른 방에만 글이 올라간다 -----

    private void sendTo(long clubId, long postId, long memberId) {
        jdbc.update("INSERT INTO writing_club_post (club_id, post_id, member_id, created_at) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING",
                clubId, postId, memberId, java.sql.Timestamp.from(clock.instant()));
    }

    /** 방 화면 [글 쓰기]: 이 방에 올라갈 새 임시글. 글 번호. */
    @Transactional
    public long startPost(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireWritable(current);
        if (!isMember(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        long postId = drafts.create(current, "", "");
        sendTo(clubId, postId, me.memberId());
        return postId;
    }

    /** 발행 설정 "올릴 방": 내가 든 방(최근에 든 것부터)과 이 글이 올라가 있는지. */
    public List<PostRoom> postRooms(long memberId, long postId) {
        return jdbc.query("""
                SELECT c.id, c.name, c.subject,
                       EXISTS (SELECT 1 FROM writing_club_post cp WHERE cp.club_id = c.id AND cp.post_id = ?) AS picked
                FROM writing_club_member m JOIN writing_club c ON c.id = m.club_id
                WHERE m.member_id = ? ORDER BY m.joined_at DESC, c.id DESC
                """, (rs, i) -> new PostRoom(rs.getLong("id"), rs.getString("name"), rs.getString("subject"), rs.getBoolean("picked")),
                postId, memberId);
    }

    /** 글쓴이가 자기 글을 방에 올리거나 뺀다. 그 방 멤버이고 내 글일 때만. */
    @Transactional
    public void pick(Optional<CurrentUser> current, long postId, long clubId, boolean on) {
        CurrentUser me = accountGuard.requireWritable(current);
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, me.memberId());
        if (mine == null || mine == 0 || !isMember(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        if (on) {
            sendTo(clubId, postId, me.memberId());
        } else {
            jdbc.update("DELETE FROM writing_club_post WHERE club_id = ? AND post_id = ?", clubId, postId);
        }
    }

    /** 방장·관리자: 주제에 맞지 않는 글을 방에서 내린다(글은 블로그에 그대로). */
    public void takeDown(Optional<CurrentUser> current, long clubId, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        if (!isAdmin(clubId, me.memberId())) {
            throw new NotFoundException();
        }
        jdbc.update("DELETE FROM writing_club_post WHERE club_id = ? AND post_id = ?", clubId, postId);
    }

    /** 방장만: 방 주제 바꾸기. */
    public void subject(Optional<CurrentUser> current, long clubId, String subject) {
        requireOwner(current, clubId);
        String s = subject == null ? "" : subject.strip();
        if (s.isEmpty() || s.length() > SUBJECT_MAX) {
            throw new ClubException("방 주제는 1~" + SUBJECT_MAX + "자로 써 주세요");
        }
        jdbc.update("UPDATE writing_club SET subject = ? WHERE id = ?", s, clubId);
    }

    /** 글쓰기 화면 안내: 이 글이 방 글감으로 연 글이면 방과 글감. */
    public Optional<PostTopic> topicOfPost(long postId) {
        return jdbc.query("""
                SELECT c.id, c.name, t.title, t.hint FROM writing_club_topic_post tp
                JOIN writing_club_topic t ON t.id = tp.topic_id JOIN writing_club c ON c.id = t.club_id
                WHERE tp.post_id = ?
                """, (rs, i) -> new PostTopic(rs.getLong("id"), rs.getString("name"), rs.getString("title"), rs.getString("hint")), postId)
                .stream().findFirst();
    }

    /** 글 화면 띠: 이 글을 쓴 방과 글감, 같은 글감으로 쓴 다른 멤버들의 전체 공개 글(최근 것부터 {@link #BAND_POSTS}편). */
    /** {@code topicTitle}: 방 글감으로 쓴 글이면 그 글감, 아니면(방에만 올린 글) null. */
    public record Band(long clubId, String clubName, String topicTitle, String subject, List<BandPost> others) {
    }

    public record BandPost(long postId, String title, String handle, String nickname, String profileImageUrl, java.time.Instant at) {

        public String name() {
            return nickname == null ? handle : nickname;
        }

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + postId;
        }

        public com.team.blog.account.application.ProfileAvatar avatar() {
            return new com.team.blog.account.application.ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    static final int BAND_POSTS = 5;

    /**
     * 10-26 글 화면 "○○에서 함께 쓴 글" 띠. 글쓴이가 아직 그 방 멤버일 때만. 비공개방(링크로만)은 방 멤버에게만 보이고,
     * 공개방은 누구에게나 보인다([방 보기]는 멤버가 아니면 방 소개로).
     */
    public Optional<Band> band(long postId, long authorId, Optional<Long> viewerId) {
        record Row(long clubId, String name, String joinMode, String subject, long topicId, String title) {
        }
        // 방에서 내린 글은 띠도 없다(writing_club_post 줄이 있어야 함)
        Optional<Row> topicRow = jdbc.query("""
                SELECT c.id, c.name, c.join_mode, c.subject, t.id AS topic_id, t.title FROM writing_club_topic_post tp
                JOIN writing_club_topic t ON t.id = tp.topic_id JOIN writing_club c ON c.id = t.club_id
                JOIN writing_club_post cp ON cp.club_id = c.id AND cp.post_id = tp.post_id
                JOIN writing_club_member m ON m.club_id = c.id AND m.member_id = tp.member_id
                WHERE tp.post_id = ? AND tp.member_id = ? LIMIT 1
                """, (rs, i) -> new Row(rs.getLong("id"), rs.getString("name"), rs.getString("join_mode"), rs.getString("subject"),
                rs.getLong("topic_id"), rs.getString("title")), postId, authorId).stream().findFirst();
        // 10-28 방 글감이 아닌 글도 방에 올렸으면 "○○에 올린 글"과 방 주제(같은 글감 친구 글은 없음). 공개방을 먼저 고른다
        Optional<Row> row = topicRow.isPresent() ? topicRow : jdbc.query("""
                    SELECT c.id, c.name, c.join_mode, c.subject FROM writing_club_post cp JOIN writing_club c ON c.id = cp.club_id
                    JOIN writing_club_member m ON m.club_id = c.id AND m.member_id = cp.member_id
                    WHERE cp.post_id = ? AND cp.member_id = ? ORDER BY (c.join_mode = 'LINK'), cp.created_at LIMIT 1
                    """, (rs, i) -> new Row(rs.getLong("id"), rs.getString("name"), rs.getString("join_mode"), rs.getString("subject"), 0, null),
                    postId, authorId).stream().findFirst();
        if (row.isEmpty() || (LINK.equals(row.get().joinMode()) && viewerId.map(v -> !isMember(row.get().clubId(), v)).orElse(true))) {
            return Optional.empty();
        }
        Row r = row.get();
        if (r.title() == null) {
            return Optional.of(new Band(r.clubId(), r.name(), null, r.subject(), List.of()));
        }
        List<BandPost> others = jdbc.query("""
                SELECT p.id, p.title, p.first_public_at, mb.handle, mb.nickname, mb.profile_image_url
                FROM writing_club_topic_post tp JOIN post p ON p.id = tp.post_id JOIN member mb ON mb.id = p.author_id
                JOIN writing_club_member m ON m.club_id = ? AND m.member_id = p.author_id
                JOIN writing_club_post cp ON cp.club_id = m.club_id AND cp.post_id = p.id
                WHERE tp.topic_id = ? AND p.id <> ? AND p.first_public_at IS NOT NULL AND """ + " "
                + accessPolicy.publicListingCondition("p", "mb") + """
                 ORDER BY p.first_public_at DESC LIMIT
                """ + " " + BAND_POSTS,
                (rs, i) -> new BandPost(rs.getLong("id"), rs.getString("title"), rs.getString("handle"), rs.getString("nickname"),
                        rs.getString("profile_image_url"), rs.getTimestamp("first_public_at").toInstant()), r.clubId(), r.topicId(), postId);
        return Optional.of(new Band(r.clubId(), r.name(), r.title(), r.subject(), others));
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

    /** 공개 목록: 승인 후 참여·누구나 바로인 함께 쓰기. {@code q}가 있으면 이름·소개 부분 일치. */
    public List<Club> listed(String q) {
        String term = q == null ? "" : q.strip();
        String like = com.team.blog.discovery.application.SearchTerms.likePattern(term);
        return jdbc.query(SELECT + """
                 WHERE c.join_mode <> 'LINK'
                   AND (? = '' OR c.name ILIKE ? ESCAPE '\\' OR c.intro ILIKE ? ESCAPE '\\' OR c.subject ILIKE ? ESCAPE '\\')
                 ORDER BY c.created_at DESC, c.id DESC LIMIT 30
                """, (rs, i) -> club(rs), term, like, like, like);
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
                INSERT INTO writing_club_member (club_id, member_id, joined_at, last_seen_at)
                SELECT c.id, ?, ?, ? FROM writing_club c WHERE c.id = ?
                  AND (c.member_cap IS NULL OR (SELECT count(*) FROM writing_club_member m WHERE m.club_id = c.id) < c.member_cap)
                ON CONFLICT DO NOTHING
                """, memberId, java.sql.Timestamp.from(clock.instant()), java.sql.Timestamp.from(clock.instant()), club.id());
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
        return new Preview(club, state, invited);
    }

    /**
     * 10-26 글그린 공식 방: 운영자(관리자 계정)가 만든 공개방 중 가장 먼저 만든 것. 방이 없는 사람의 첫 화면 맨 위에 보이고,
     * 승인 방식이어도 [들어가기] 한 번에 바로 들어온다(몰래 넣지는 않는다: 14세 미만 회원·방 알림·멤버 목록 노출).
     */
    public Optional<Club> official() {
        return jdbc.query(SELECT + """
                 JOIN member o ON o.id = c.owner_id
                 WHERE c.join_mode <> 'LINK' AND o.role = 'ADMIN' AND o.status = 'ACTIVE' AND o.withdrawn_at IS NULL
                 ORDER BY c.created_at, c.id LIMIT 1
                """, (rs, i) -> club(rs)).stream().findFirst();
    }

    /** 방이 없는 사람의 첫 화면: 정원이 남은 공개방 최근 것부터 {@code limit}개({@code excludeId} 방은 빼고). */
    public List<Club> openToJoin(long excludeId, int limit) {
        return jdbc.query(SELECT + """
                 WHERE c.join_mode <> 'LINK' AND c.id <> ?
                   AND (c.member_cap IS NULL OR (SELECT count(*) FROM writing_club_member m3 WHERE m3.club_id = c.id) < c.member_cap)
                 ORDER BY c.created_at DESC, c.id DESC LIMIT ?
                """, (rs, i) -> club(rs), excludeId, limit);
    }

    /** 방 소개 카드 한 줄: 오늘(한국 날짜) 이 방 멤버 중 전체 공개 글을 쓴 사람 수. */
    public int wroteToday(long clubId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(DISTINCT p.author_id) FROM """ + " " + ROOM_POSTS + """
                WHERE cp.club_id = ? AND """ + " " + PUBLIC + " AND (p.first_public_at AT TIME ZONE 'Asia/Seoul')::date = ?",
                Integer.class, clubId, Date.valueOf(today()));
        return n == null ? 0 : n;
    }

    /** 공개 함께 쓰기에서 [참여하기](누구나 바로) 또는 [참여 요청](승인 후 참여). 들어갔으면 {@code true}. 공식 방은 언제나 바로. */
    @Transactional
    public boolean joinListed(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireWritable(current);
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        if (LINK.equals(club.joinMode())) {
            throw new NotFoundException();
        }
        if (OPEN.equals(club.joinMode()) || official().map(o -> o.id() == clubId).orElse(false)) {
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

    /**
     * 만든 사람만: 참여 방식과 인원 제한({@code cap} null = 제한 없음). 10-28: 지금 멤버 수보다 적게 정해도 되고,
     * 그러면 있는 멤버는 그대로 두고 새로 들어오는 것만 막는다.
     */
    public void settings(Optional<CurrentUser> current, long clubId, String mode, Integer cap, boolean writeFirst) {
        requireOwner(current, clubId);
        Club club = byId(clubId).orElseThrow(NotFoundException::new);
        if (!MODES.contains(mode)) {
            throw new ClubException("참여 방식을 골라 주세요");
        }
        if (cap != null && (cap < 2 || cap > MAX_CAP)) {
            throw new ClubException("최대 인원은 2~" + MAX_CAP + "명으로 정해 주세요");
        }
        jdbc.update("UPDATE writing_club SET join_mode = ?, member_cap = ?, write_first = ? WHERE id = ?", mode, cap, writeFirst, clubId);
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
        Set<Long> allowed = inviteCandidates(me.memberId(), clubId).stream().map(Candidate::memberId).collect(Collectors.toSet());
        List<Long> targets = memberIds.stream().distinct().filter(allowed::contains).toList();
        int left = DAILY_INVITES - invitesToday(me.memberId());
        if (targets.size() > left) {
            throw new ClubException("초대는 하루 " + DAILY_INVITES + "명까지 보낼 수 있어요(오늘 " + Math.max(0, left) + "명 남음)");
        }
        if (club.memberCap() != null && club.memberCount() + targets.size() > club.memberCap()) {
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

    /** 내가 받은 초대. */
    public List<Pending> myInvites(long me) {
        return jdbc.query("""
                SELECT c.id AS club_id, c.name AS club_name, i.id, i.handle, i.nickname, i.profile_image_url,
                       COALESCE(i.nickname, i.handle) AS inviter
                FROM writing_club_join j JOIN writing_club c ON c.id = j.club_id JOIN member i ON i.id = j.invited_by
                WHERE j.member_id = ? AND j.kind = 'INVITE'
                ORDER BY j.created_at DESC
                """, (rs, i) -> pending(rs), me);
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

    /** 10-28 방에 올린 글(p). 글쓴이가 지금 그 방 멤버인 것만(나간 사람의 글은 빠짐). 방은 {@code cp.club_id}로 고른다. */
    private static final String ROOM_POSTS = "writing_club_post cp JOIN post p ON p.id = cp.post_id"
            + " JOIN writing_club_member pm ON pm.member_id = p.author_id AND pm.club_id = cp.club_id ";

    private static final String SELECT = """
            SELECT c.id, c.owner_id, c.name, c.intro, c.start_date, c.invite_code, c.join_mode, c.member_cap, c.write_first, c.subject,
                   (SELECT count(*) FROM writing_club_member m WHERE m.club_id = c.id) AS member_count
            FROM writing_club c""";

    private static Club club(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Club(rs.getLong("id"), rs.getLong("owner_id"), rs.getString("name"), rs.getString("intro"),
                rs.getDate("start_date").toLocalDate(), rs.getString("invite_code"), rs.getInt("member_count"), rs.getString("join_mode"),
                (Integer) rs.getObject("member_cap"), rs.getBoolean("write_first"), rs.getString("subject"));
    }
}

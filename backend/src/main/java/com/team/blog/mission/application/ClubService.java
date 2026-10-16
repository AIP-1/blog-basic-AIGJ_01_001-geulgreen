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
                       String inviteCode, int memberCount) {
    }

    /** 지금(또는 가장 가까운) 기간: 몇 번째, 시작·끝 날짜. */
    public record Period(int index, int total, LocalDate start, LocalDate end) {
    }

    public record Member(long memberId, String handle, String nickname, boolean wrote, boolean owner) {

        public String name() {
            return nickname == null ? handle : nickname;
        }
    }

    public record Board(Club club, Period period, List<Member> members, List<Long> postIds, boolean isOwner, boolean ended,
                        boolean notStarted) {
    }

    /** 화면 값. 잘못 낸 값을 flash(세션)로 돌려보내서 Serializable. */
    public record Form(String name, String intro, int everyDays, LocalDate startDate, LocalDate endDate) implements java.io.Serializable {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final Clock clock;

    public ClubService(JdbcTemplate jdbc, AccountGuard accountGuard, Clock clock) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), SEOUL);
    }

    /** 잘못된 값이면 화면에 보여 줄 문장. 맞으면 빈 값. */
    public static Optional<String> problem(Form f) {
        if (f.name() == null || f.name().isBlank() || f.name().strip().length() > 30) {
            return Optional.of("모임 이름은 1~30자로 써 주세요");
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
            return Optional.of("모임 기간은 1년까지예요");
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
                INSERT INTO writing_club (owner_id, name, intro, every_days, start_date, end_date, invite_code)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, me.memberId(), f.name().strip(), f.intro() == null ? "" : f.intro().strip(), f.everyDays(),
                Date.valueOf(f.startDate()), Date.valueOf(f.endDate()), code);
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

    public Optional<Club> byInvite(String code) {
        return jdbc.query(SELECT + " WHERE c.invite_code = ?", (rs, i) -> club(rs), code).stream().findFirst();
    }

    public boolean isMember(long clubId, long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM writing_club_member WHERE club_id = ? AND member_id = ?)", Boolean.class, clubId, memberId));
    }

    /** 초대 링크로 들어오기. 이미 멤버면 그대로. */
    public long join(Optional<CurrentUser> current, String code) {
        CurrentUser me = accountGuard.requireWritable(current);
        Club club = byInvite(code).orElseThrow(NotFoundException::new);
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?) ON CONFLICT DO NOTHING", club.id(), me.memberId());
        return club.id();
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
                SELECT mb.id, mb.handle, mb.nickname FROM writing_club_member m JOIN member mb ON mb.id = m.member_id
                WHERE m.club_id = ? ORDER BY m.joined_at, mb.id
                """, (rs, i) -> new Member(rs.getLong("id"), rs.getString("handle"), rs.getString("nickname"),
                wrote.contains(rs.getLong("id")), rs.getLong("id") == club.ownerId()), clubId);
        List<Long> postIds = jdbc.queryForList("""
                SELECT p.id FROM post p JOIN writing_club_member m ON m.member_id = p.author_id AND m.club_id = ?
                WHERE """ + " " + PUBLIC + " AND (p.first_public_at AT TIME ZONE 'Asia/Seoul')::date BETWEEN ? AND ?"
                + " ORDER BY p.first_public_at DESC LIMIT " + POSTS,
                Long.class, clubId, Date.valueOf(club.startDate()), Date.valueOf(club.endDate()));
        return new Board(club, period, members, postIds, club.ownerId() == me.memberId(), today.isAfter(club.endDate()),
                today.isBefore(club.startDate()));
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

    /** 탈퇴 정리: 멤버 줄과 만든 모임. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM writing_club_member WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM writing_club WHERE owner_id = ?", memberId);
    }

    private static final String PUBLIC = "p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND p.deleted_at IS NULL AND p.hidden_at IS NULL"
            + " AND p.first_public_at IS NOT NULL";

    private static final String SELECT = """
            SELECT c.id, c.owner_id, c.name, c.intro, c.every_days, c.start_date, c.end_date, c.invite_code,
                   (SELECT count(*) FROM writing_club_member m WHERE m.club_id = c.id) AS member_count
            FROM writing_club c""";

    private static Club club(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Club(rs.getLong("id"), rs.getLong("owner_id"), rs.getString("name"), rs.getString("intro"), rs.getInt("every_days"),
                rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(), rs.getString("invite_code"),
                rs.getInt("member_count"));
    }
}

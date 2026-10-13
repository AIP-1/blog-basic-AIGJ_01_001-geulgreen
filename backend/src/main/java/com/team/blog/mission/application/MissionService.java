package com.team.blog.mission.application;

import com.team.blog.post.application.PostDraftService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.event.MissionCompleted;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import com.team.blog.privacy.application.PrivateAccountService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 057 오늘의 미션(강성찬 개인 확장, 034를 바꿈): 날마다 주제 3개(AI 또는 내장 주제), 주제를 고르면 그 주제의 임시글을 열고,
 * 그 글을 주제가 나온 날(한국 시간) 안에 처음 발행하면 완료. 완료 횟수·연속 일수를 세고, 066 배지는 그 기록으로 정한다.
 */
@Service
public class MissionService {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public record Topic(long id, LocalDate day, int slot, String title, String hint, boolean ai, int participants) {
    }

    /** 미션 글 한 줄. {@code friend}: 보는 사람이 친구로 고른 사람의 글(069). */
    public record FeedPost(long postId, String title, String excerpt, String handle, String nickname, String profileImageUrl,
                           String topicTitle, int commentCount, int likeCount, Instant completedAt, boolean friend, boolean mine) {

        public String url() {
            return "/@" + handle + "/posts/" + postId;
        }
    }

    /** 내 기록: 완료한 날 수, 지금 연속, 최고 연속, 오늘 했는지. */
    public record Stats(int total, int streak, int best, boolean doneToday) {
    }

    /** 066 미션 배지(강성찬 개인 확장): {@code rule}은 아직 못 받았을 때 보이는 받는 법. */
    public record Badge(String emoji, String name, String rule, boolean earned) {
    }

    /** 글이 미션 글이면 그 주제와 완료 여부. */
    public record PostMission(long topicId, LocalDate day, String title, String hint, boolean completed, boolean countedOnDay) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final PostDraftService drafts;
    private final TopicAi topicAi;
    private final MissionProperties properties;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final PrivateAccountService privacy;

    public MissionService(JdbcTemplate jdbc, AccountGuard accountGuard, PostDraftService drafts, TopicAi topicAi,
                          MissionProperties properties, ApplicationEventPublisher events, TransactionTemplate tx, Clock clock,
                          PrivateAccountService privacy) {
        this.privacy = privacy;
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.drafts = drafts;
        this.topicAi = topicAi;
        this.properties = properties;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), SEOUL);
    }

    // ----- 주제 -----

    /** 그날의 주제. 오늘인데 아직 없으면 내장 주제로 바로 채운다(지난날은 있던 것만). */
    public List<Topic> topics(LocalDate day) {
        List<Topic> list = load(day);
        if (list.isEmpty() && day.equals(today())) {
            insert(day, TopicPool.forDay(day, properties.topicsPerDay()), "POOL");
            list = load(day);
        }
        return list;
    }

    /**
     * 0시 1분 작업: AI로 오늘 주제를 만든다. 이미 내장 주제로 채워졌어도 아직 아무도 그 주제로 글을 시작하지 않았으면 AI 주제로 바꾼다.
     *
     * @return AI 주제로 정했는지
     */
    public boolean prepareWithAi(LocalDate day) {
        Optional<List<TopicPool.Topic>> ai = topicAi.topics(day, properties.topicsPerDay());
        if (ai.isEmpty()) {
            topics(day);
            return false;
        }
        Boolean replaced = tx.execute(status -> {
            List<Topic> existing = load(day);
            if (!existing.isEmpty()) {
                boolean untouched = existing.stream().noneMatch(t -> t.ai()) && jdbc.queryForObject(
                        "SELECT NOT EXISTS (SELECT 1 FROM mission_post mp JOIN daily_topic dt ON dt.id = mp.topic_id WHERE dt.day = ?)",
                        Boolean.class, Date.valueOf(day));
                if (!untouched) {
                    return false;
                }
                jdbc.update("DELETE FROM daily_topic WHERE day = ?", Date.valueOf(day));
            }
            insert(day, ai.get(), "AI");
            return true;
        });
        return Boolean.TRUE.equals(replaced);
    }

    private void insert(LocalDate day, List<TopicPool.Topic> topics, String source) {
        for (int i = 0; i < topics.size(); i++) {
            jdbc.update("INSERT INTO daily_topic (day, slot, title, hint, source) VALUES (?, ?, ?, ?, ?) ON CONFLICT (day, slot) DO NOTHING",
                    Date.valueOf(day), i + 1, topics.get(i).title(), topics.get(i).hint(), source);
        }
    }

    private List<Topic> load(LocalDate day) {
        return jdbc.query("""
                SELECT dt.id, dt.day, dt.slot, dt.title, dt.hint, dt.source,
                       (SELECT count(DISTINCT mp.member_id) FROM mission_post mp JOIN post p ON p.id = mp.post_id
                        WHERE mp.topic_id = dt.id AND mp.completed_at IS NOT NULL AND p.status = 'PUBLISHED' AND p.deleted_at IS NULL) AS n
                FROM daily_topic dt WHERE dt.day = ? ORDER BY dt.slot
                """, (rs, i) -> new Topic(rs.getLong("id"), rs.getDate("day").toLocalDate(), rs.getInt("slot"), rs.getString("title"),
                rs.getString("hint"), "AI".equals(rs.getString("source")), rs.getInt("n")), Date.valueOf(day));
    }

    /** 주제가 있었던 지난날들(오늘 빼고, 최근 순). */
    public List<LocalDate> pastDays() {
        return jdbc.queryForList("SELECT DISTINCT day FROM daily_topic WHERE day < ? AND day >= ? ORDER BY day DESC",
                Date.class, Date.valueOf(today()), Date.valueOf(today().minusDays(properties.historyDays()))).stream()
                .map(Date::toLocalDate).toList();
    }

    // ----- 참여 -----

    /**
     * 이 주제로 쓰기: 오늘 주제만. 같은 주제로 시작해 둔 임시글이 있으면 그 글, 없으면 제목에 주제를 넣은 새 임시글.
     *
     * @return 글 번호(글쓰기 화면으로 보낸다)
     */
    public long start(Optional<CurrentUser> current, long topicId) {
        CurrentUser me = accountGuard.requireWritable(current);
        Topic topic = jdbc.query("SELECT id, day, slot, title, hint, source FROM daily_topic WHERE id = ?",
                (rs, i) -> new Topic(rs.getLong("id"), rs.getDate("day").toLocalDate(), rs.getInt("slot"), rs.getString("title"),
                        rs.getString("hint"), "AI".equals(rs.getString("source")), 0), topicId).stream().findFirst()
                .orElseThrow(NotFoundException::new);
        if (!topic.day().equals(today())) {
            throw new PostContentException("MISSION_TOPIC_CLOSED");
        }
        Optional<Long> draft = jdbc.queryForList("""
                SELECT mp.post_id FROM mission_post mp JOIN post p ON p.id = mp.post_id
                WHERE mp.topic_id = ? AND mp.member_id = ? AND p.status = 'DRAFT' AND p.deleted_at IS NULL ORDER BY mp.created_at DESC LIMIT 1
                """, Long.class, topicId, me.memberId()).stream().findFirst();
        if (draft.isPresent()) {
            return draft.get();
        }
        long postId = drafts.create(current, topic.title(), "");
        try {
            jdbc.update("INSERT INTO mission_post (post_id, topic_id, member_id) VALUES (?, ?, ?)", postId, topicId, me.memberId());
        } catch (DuplicateKeyException e) {
            // 같은 글이 이미 연결됨(두 번 누름) — 그대로 쓴다
        }
        return postId;
    }

    /** 발행 사건에서: 미션 글이면 처음 발행한 때를 완료로 적고, 친구 알림 사건을 낸다. */
    public void onPublished(long postId, long authorId, String visibility) {
        Instant now = clock.instant();
        int n = jdbc.update("UPDATE mission_post SET completed_at = ? WHERE post_id = ? AND member_id = ? AND completed_at IS NULL",
                Timestamp.from(now), postId, authorId);
        if (n > 0 && ("PUBLIC".equals(visibility) || "FRIENDS".equals(visibility))) {
            events.publishEvent(new MissionCompleted(postId, authorId));
        }
    }

    /** 글의 미션(배지·글쓰기 안내용). */
    public Optional<PostMission> ofPost(long postId) {
        return jdbc.query("""
                SELECT dt.id, dt.day, dt.title, dt.hint, mp.completed_at FROM mission_post mp JOIN daily_topic dt ON dt.id = mp.topic_id
                WHERE mp.post_id = ?
                """, (rs, i) -> {
            LocalDate day = rs.getDate("day").toLocalDate();
            Timestamp done = rs.getTimestamp("completed_at");
            boolean onDay = done != null && LocalDate.ofInstant(done.toInstant(), SEOUL).equals(day);
            return new PostMission(rs.getLong("id"), day, rs.getString("title"), rs.getString("hint"), done != null, onDay);
        }, postId).stream().findFirst();
    }

    // ----- 목록 -----

    /** 그날 주제로 완료한 글 중 보는 사람이 읽을 수 있는 것(전체 공개, 친구 공개는 글쓴이가 나를 친구로 고른 경우, 본인 글은 무엇이든), 최근 순. */
    public List<FeedPost> feed(LocalDate day, Optional<CurrentUser> viewer) {
        long me = viewer.map(CurrentUser::memberId).orElse(-1L);
        // 069: 친구 칸은 내가 고른 친구의 글, 친구 공개 글은 글쓴이가 나를 고른 경우만
        String myFriend = "EXISTS (SELECT 1 FROM close_friend cf WHERE cf.owner_id = ? AND cf.friend_id = p.author_id)";
        String pickedMe = "EXISTS (SELECT 1 FROM close_friend cf WHERE cf.owner_id = p.author_id AND cf.friend_id = ?)";
        return jdbc.query("""
                SELECT p.id, p.title, COALESCE(tz.teaser, p.excerpt) AS excerpt, p.comment_count, p.like_count, p.author_id,
                       m.handle, m.nickname, m.profile_image_url, dt.title AS topic, mp.completed_at, """ + myFriend + """
                 AS friend
                FROM mission_post mp JOIN daily_topic dt ON dt.id = mp.topic_id JOIN post p ON p.id = mp.post_id
                     JOIN member m ON m.id = p.author_id LEFT JOIN post_teaser tz ON tz.post_id = p.id
                WHERE dt.day = ? AND mp.completed_at IS NOT NULL AND p.status = 'PUBLISHED' AND p.deleted_at IS NULL
                  AND p.hidden_at IS NULL AND m.withdrawn_at IS NULL AND """ + privacy.visibleToCondition("m", me) + """

                  AND (p.author_id = ? OR p.visibility = 'PUBLIC' OR (p.visibility = 'FRIENDS' AND""" + " " + pickedMe + """
                ))
                ORDER BY mp.completed_at DESC, p.id DESC LIMIT ?
                """, (rs, i) -> new FeedPost(rs.getLong("id"), rs.getString("title"),
                rs.getString("excerpt") == null ? "" : rs.getString("excerpt"), rs.getString("handle"), rs.getString("nickname"),
                rs.getString("profile_image_url"), rs.getString("topic"), rs.getInt("comment_count"), rs.getInt("like_count"),
                rs.getTimestamp("completed_at").toInstant(), rs.getBoolean("friend"), rs.getLong("author_id") == me),
                me, Date.valueOf(day), me, me, properties.feedSize());
    }

    // ----- 내 기록 -----

    /** 완료한 날: 주제가 나온 날 안에 발행했고 지금도 발행·삭제 안 됨. */
    public Stats stats(long memberId) {
        List<LocalDate> days = jdbc.queryForList("""
                SELECT DISTINCT dt.day FROM mission_post mp JOIN daily_topic dt ON dt.id = mp.topic_id JOIN post p ON p.id = mp.post_id
                WHERE mp.member_id = ? AND mp.completed_at IS NOT NULL AND p.status = 'PUBLISHED' AND p.deleted_at IS NULL
                  AND (mp.completed_at AT TIME ZONE 'Asia/Seoul')::date = dt.day
                """, Date.class, memberId).stream().map(Date::toLocalDate).toList();
        return stats(new TreeSet<>(days), today());
    }

    /** 날짜 모음으로 기록 계산(테스트도 쓴다). */
    public static Stats stats(TreeSet<LocalDate> days, LocalDate today) {
        boolean doneToday = days.contains(today);
        int streak = 0;
        LocalDate d = doneToday ? today : today.minusDays(1);
        while (days.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        int best = 0;
        int run = 0;
        LocalDate prev = null;
        for (LocalDate day : days) {
            run = prev != null && prev.plusDays(1).equals(day) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = day;
        }
        return new Stats(days.size(), streak, best, doneToday);
    }

    /** 066 배지: 따로 저장하지 않고 지금 기록(완료한 날 수·최고 연속)으로 정한다. 미션 글을 지우면 빠질 수 있다. */
    public static List<Badge> badges(Stats s) {
        return List.of(
                new Badge("🌱", "첫 미션", "미션을 처음 완료하면", s.total() >= 1),
                new Badge("🔥", "3일 연속", "3일 연속 완료하면", s.best() >= 3),
                new Badge("🌿", "미션 10번", "10번 완료하면", s.total() >= 10),
                new Badge("⭐", "일주일 연속", "7일 연속 완료하면", s.best() >= 7),
                new Badge("🌳", "미션 30번", "30번 완료하면", s.total() >= 30),
                new Badge("👑", "한 달 연속", "30일 연속 완료하면", s.best() >= 30),
                new Badge("🏆", "미션 100번", "100번 완료하면", s.total() >= 100));
    }

    /** 오늘 내가 완료한 글(있으면 첫 글). */
    public Optional<FeedPost> myToday(long memberId) {
        List<FeedPost> mine = new ArrayList<>();
        feed(today(), Optional.of(new CurrentUser(memberId, "USER"))).stream().filter(FeedPost::mine).forEach(mine::add);
        return mine.isEmpty() ? Optional.empty() : Optional.of(mine.get(mine.size() - 1));
    }

    /** 탈퇴 정리: 미션 글 연결(글은 글 정리가 지운다)과 034 때의 표. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM mission_post WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM mission_participant WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM mission_participant WHERE mission_id IN (SELECT id FROM mission WHERE creator_id = ?)", memberId);
        jdbc.update("DELETE FROM mission WHERE creator_id = ?", memberId);
    }
}

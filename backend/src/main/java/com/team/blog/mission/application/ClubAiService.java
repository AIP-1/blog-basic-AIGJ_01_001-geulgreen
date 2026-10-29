package com.team.blog.mission.application;

import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 10-29 chan: 방장이 ChatGPT·Gemini·Claude 중 하나를 방에 연결해 방 글감을 추천받는다. 방 주제를 보고 멤버들이 흥미를 가질 글감을,
 * 지난 1년 글감과 겹치지 않게. 받은 후보는 방장 화면에만 보이고, 방장이 고쳐서 [올리기]를 눌러야 방 글감이 된다(저절로 올라가지 않음).
 * 방장만(돈이 방장 키에서 나가므로 관리자도 안 됨), 14세 미만 방장은 연결할 수 없다. 방마다 하루 {@code dailyLimit}번.
 */
@Service
public class ClubAiService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    public static final List<String> PROVIDERS = List.of("GEMINI", "OPENAI", "CLAUDE");

    /** 연결 상태(키 자체는 담지 않는다). */
    public record Connection(String provider, String tail, java.time.Instant connectedAt, int usedToday, int dailyLimit) {

        public String name() {
            return ClubAiService.name(provider);
        }

        public int left() {
            return Math.max(0, dailyLimit - usedToday);
        }
    }

    /** 추천 후보. {@code similar}는 지난 1년 글감 중 비슷한 것(없으면 빈 값), {@code similarDay}는 그 날짜("3월 2일"). */
    public record Candidate(String title, String hint, String similar, String similarDay) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final ClubService clubs;
    private final ClubAiKeys keys;
    private final ClubAiClient client;
    private final ClubAiProperties properties;
    private final Clock clock;

    public ClubAiService(JdbcTemplate jdbc, AccountGuard accountGuard, ClubService clubs, ClubAiKeys keys, ClubAiClient client,
                         ClubAiProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.clubs = clubs;
        this.keys = keys;
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    public static String name(String provider) {
        return switch (provider) {
            case "OPENAI" -> "ChatGPT";
            case "CLAUDE" -> "Claude";
            default -> "Gemini";
        };
    }

    public boolean enabled() {
        return properties.enabled();
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), SEOUL);
    }

    public Optional<Connection> connection(long clubId) {
        LocalDate today = today();
        return jdbc.query("SELECT provider, key_tail, connected_at, used_on, used_count FROM writing_club_ai WHERE club_id = ?",
                (rs, i) -> {
                    Date on = rs.getDate("used_on");
                    int used = on != null && on.toLocalDate().equals(today) ? rs.getInt("used_count") : 0;
                    return new Connection(rs.getString("provider"), rs.getString("key_tail"), rs.getTimestamp("connected_at").toInstant(),
                            used, properties.dailyLimit());
                }, clubId).stream().findFirst();
    }

    private CurrentUser requireOwner(Optional<CurrentUser> current, long clubId) {
        CurrentUser me = accountGuard.requireWritable(current);
        if (clubs.byId(clubId).filter(c -> c.ownerId() == me.memberId()).isEmpty()) {
            throw new NotFoundException();
        }
        return me;
    }

    /** 방장: 키를 확인한 뒤(모델 목록, 돈 안 듦) 암호화해서 저장. 이미 있으면 바꾼다. */
    public void connect(Optional<CurrentUser> current, long clubId, String provider, String apiKey) {
        CurrentUser me = requireOwner(current, clubId);
        if (!enabled()) {
            throw new ClubService.ClubException("지금은 AI 연동을 쓸 수 없어요");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM member_agreement WHERE member_id = ? AND type = 'GUARDIAN')", Boolean.class, me.memberId()))) {
            throw new ClubService.ClubException("14세 미만 회원은 AI 키를 연결할 수 없어요");
        }
        if (!PROVIDERS.contains(provider)) {
            throw new ClubService.ClubException("ChatGPT, Gemini, Claude 중에서 골라 주세요");
        }
        String key = apiKey == null ? "" : apiKey.strip();
        if (key.length() < 20 || key.length() > 300 || key.chars().anyMatch(Character::isWhitespace)) {
            throw new ClubService.ClubException("API 키를 통째로 붙여 넣어 주세요");
        }
        try {
            client.check(provider, key);
        } catch (ClubAiClient.AiException e) {
            throw new ClubService.ClubException(e.getMessage());
        }
        jdbc.update("""
                INSERT INTO writing_club_ai (club_id, provider, key_cipher, key_tail, connected_by, connected_at) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (club_id) DO UPDATE SET provider = EXCLUDED.provider, key_cipher = EXCLUDED.key_cipher, key_tail = EXCLUDED.key_tail,
                    connected_by = EXCLUDED.connected_by, connected_at = EXCLUDED.connected_at
                """, clubId, provider, keys.encrypt(key), key.substring(key.length() - 4), me.memberId(), java.sql.Timestamp.from(clock.instant()));
    }

    /** 방장: 연결 끊기(암호문까지 지운다). */
    public void disconnect(Optional<CurrentUser> current, long clubId) {
        requireOwner(current, clubId);
        jdbc.update("DELETE FROM writing_club_ai WHERE club_id = ?", clubId);
    }

    /** 방장: 글감 후보 받기. 지난 1년과 같은 글감은 빼고, 비슷한 것은 표시만. */
    public List<Candidate> suggest(Optional<CurrentUser> current, long clubId) {
        requireOwner(current, clubId);
        ClubService.Club club = clubs.byId(clubId).orElseThrow(NotFoundException::new);
        var row = jdbc.query("SELECT provider, key_cipher FROM writing_club_ai WHERE club_id = ?",
                (rs, i) -> new String[] {rs.getString("provider"), rs.getString("key_cipher")}, clubId).stream().findFirst()
                .orElseThrow(() -> new ClubService.ClubException("먼저 AI를 연결해 주세요"));
        String key = keys.decrypt(row[1]).orElseThrow(() -> new ClubService.ClubException("연결이 풀렸어요. AI 키를 다시 연결해 주세요"));
        // 하루 한도: 날이 바뀌었으면 0부터. 실패해도 한 번으로 센다(키 돈을 지키려고 연타를 막음)
        int updated = jdbc.update("""
                UPDATE writing_club_ai SET used_count = CASE WHEN used_on = ? THEN used_count + 1 ELSE 1 END, used_on = ?
                WHERE club_id = ? AND (used_on IS DISTINCT FROM ? OR used_count < ?)
                """, Date.valueOf(today()), Date.valueOf(today()), clubId, Date.valueOf(today()), properties.dailyLimit());
        if (updated == 0) {
            throw new ClubService.ClubException("오늘은 " + properties.dailyLimit() + "번 다 받았어요. 내일 다시 받을 수 있어요");
        }
        List<ClubService.YearTopic> year = clubs.yearTopics(clubId);
        String prompt = ClubTopicPrompt.of(club.name(), club.subject(), club.intro(), year.stream().map(ClubService.YearTopic::title).toList());
        String answer;
        try {
            answer = client.ask(row[0], key, prompt);
        } catch (ClubAiClient.AiException e) {
            throw new ClubService.ClubException(e.getMessage());
        }
        List<Candidate> out = candidates(TopicAi.parse(answer, ClubTopicPrompt.COUNT), year);
        if (out.isEmpty()) {
            throw new ClubService.ClubException("AI 답에서 글감을 찾지 못했어요. 한 번 더 받아 보세요");
        }
        return out;
    }

    /**
     * 비슷한 글감: 한쪽이 다른 쪽을 품거나(3자 이상), 두 글자씩 끊은 조각이 짧은 쪽의 60% 이상 겹친다.
     * 예: "내가 처음 지은 집"과 "처음 지은 집 다시 짓기".
     */
    static boolean similar(String a, String b) {
        if (Math.min(a.length(), b.length()) < 3) {
            return false;
        }
        if (a.contains(b) || b.contains(a)) {
            return true;
        }
        java.util.Set<String> x = bigrams(a);
        java.util.Set<String> y = bigrams(b);
        long common = x.stream().filter(y::contains).count();
        return common >= 0.6 * Math.min(x.size(), y.size());
    }

    private static java.util.Set<String> bigrams(String s) {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (int i = 0; i + 2 <= s.length(); i++) {
            out.add(s.substring(i, i + 2));
        }
        return out;
    }

    /** 같은 글감(띄어쓰기·문장부호 무시)은 빼고, 비슷한 글감은 표시만. */
    static List<Candidate> candidates(List<TopicPool.Topic> topics, List<ClubService.YearTopic> year) {
        List<Candidate> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (TopicPool.Topic t : topics) {
            String k = ClubTopicPrompt.key(t.title());
            if (k.isEmpty() || seen.contains(k) || year.stream().anyMatch(y -> ClubTopicPrompt.key(y.title()).equals(k))) {
                continue;
            }
            seen.add(k);
            Optional<ClubService.YearTopic> near = year.stream().filter(y -> similar(ClubTopicPrompt.key(y.title()), k)).findFirst();
            String day = near.map(y -> {
                LocalDate d = LocalDate.ofInstant(y.createdAt(), SEOUL);
                return d.getMonthValue() + "월 " + d.getDayOfMonth() + "일";
            }).orElse("");
            out.add(new Candidate(t.title(), t.hint(), near.map(ClubService.YearTopic::title).orElse(""), day));
        }
        return out;
    }
}

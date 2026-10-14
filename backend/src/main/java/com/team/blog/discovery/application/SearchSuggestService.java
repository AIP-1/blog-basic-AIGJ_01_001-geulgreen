package com.team.blog.discovery.application;

import com.team.blog.post.application.PostAccessPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 094 강성찬 개인 확장: 검색 결과가 적을 때 "혹시 ○○을 찾으셨나요?"(자모 편집 거리로 제목 단어·태그·닉네임·아이디와 비교, 띄어쓰기·대소문자
 * 무시)와 "비슷한 글"(제목·태그·본문 단어 겹침 점수). 외부 AI는 쓰지 않는다. 후보는 검색과 같은 공용 목록 조건(전체 공개)의 글만 모아
 * {@code suggest-cache-ttl} 동안 앱 메모리에 둔다. 비공개·친구 공개 글은 처음부터 후보에 없다.
 */
@Service
public class SearchSuggestService {

    /** 글 한 편의 비교용 단어(자모 키). */
    private record Doc(long id, long authorId, Set<String> title, Set<String> tags, Set<String> body,
                       List<String> titleWords, List<String> tagNames) {
    }

    /** 고칠 후보 단어. key = 띄어쓰기·대소문자 지운 글자, jamo = key의 자모. */
    private record Term(String display, String jamo, int count) {
    }

    private record Snapshot(long loadedAt, List<Doc> docs, Map<String, Term> vocab) {
    }

    // ponytail: 글 5000편·본문 앞 5000자까지만 후보로 둔다. 사이트가 커지면 pg_trgm(권한 있는 DB)이나 검색 엔진으로 옮긴다.
    private static final int MAX_DOCS = 5000;
    private static final int BODY_CHARS = 5000;

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;
    private final SearchProperties properties;
    private volatile Snapshot snapshot;

    public SearchSuggestService(JdbcTemplate jdbc, PostAccessPolicy accessPolicy, SearchProperties properties) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
    }

    /**
     * 고친 검색어. 고칠 게 없으면 비어 있다.
     *
     * @param authorId 블로그 안 검색이면 그 주인(그 사람 글의 단어로만 고친다), 아니면 null(닉네임·아이디도 후보)
     */
    public Optional<String> correct(SearchTerms terms, Long authorId) {
        if (terms.empty()) {
            return Optional.empty();
        }
        Snapshot s = snapshot();
        Map<String, Term> vocab = authorId == null ? s.vocab() : vocab(s.docs().stream().filter(d -> d.authorId() == authorId).toList(), List.of());
        String text = terms.text();
        String whole = FuzzyText.key(text);
        Term exact = vocab.get(whole);
        String fixed;
        if (exact != null) {
            fixed = exact.display(); // 띄어쓰기만 다른 경우("스프링부트" → "스프링 부트")
        } else {
            fixed = nearest(vocab, whole).map(Term::display).orElseGet(() -> perWord(vocab, terms));
        }
        return fixed.equalsIgnoreCase(text) ? Optional.empty() : Optional.of(fixed);
    }

    private static String perWord(Map<String, Term> vocab, SearchTerms terms) {
        List<String> out = new ArrayList<>();
        for (String w : terms.words()) {
            String k = FuzzyText.key(w);
            boolean known = vocab.containsKey(k) || vocab.keySet().stream().anyMatch(v -> v.contains(k));
            out.add(known ? w : nearest(vocab, k).map(Term::display).orElse(w));
        }
        return String.join(" ", out);
    }

    private static Optional<Term> nearest(Map<String, Term> vocab, String key) {
        if (key.codePointCount(0, key.length()) < 2) {
            return Optional.empty();
        }
        String j = FuzzyText.jamo(key);
        int allow = FuzzyText.allowance(j);
        Term best = null;
        int bestD = allow + 1;
        for (Term t : vocab.values()) {
            int d = FuzzyText.distance(j, t.jamo(), allow);
            if (d > 0 && (d < bestD || d == bestD && best != null && t.count() > best.count())) {
                best = t;
                bestD = d;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * 비슷한 글 id(점수 높은 순, 같으면 최근 글). 검색어 단어마다 제목 3점·태그 2점·본문 1점, 단어는 포함 관계나 자모 편집 거리로 맞춘다.
     */
    public List<Long> similar(Collection<String> words, Long authorId, Set<Long> exclude, int limit) {
        List<String> qs = words.stream().map(w -> FuzzyText.jamo(FuzzyText.key(w))).filter(w -> w.length() >= 2).distinct().toList();
        if (qs.isEmpty()) {
            return List.of();
        }
        record Scored(long id, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Doc d : snapshot().docs()) {
            if (exclude.contains(d.id()) || authorId != null && d.authorId() != authorId) {
                continue;
            }
            int score = 0;
            for (String q : qs) {
                score += (matches(d.title(), q) ? 3 : 0) + (matches(d.tags(), q) ? 2 : 0) + (matches(d.body(), q) ? 1 : 0);
            }
            if (score > 0) {
                scored.add(new Scored(d.id(), score));
            }
        }
        // docs는 최근 글부터라 안정 정렬이면 같은 점수 안에서 최근 글이 앞
        return scored.stream().sorted(Comparator.comparingInt(Scored::score).reversed()).limit(limit).map(Scored::id).toList();
    }

    private static boolean matches(Set<String> tokens, String q) {
        int allow = FuzzyText.allowance(q);
        for (String t : tokens) {
            if (t.contains(q) || q.contains(t) && t.length() * 2 >= q.length() || FuzzyText.distance(t, q, allow) <= allow) {
                return true;
            }
        }
        return false;
    }

    private Snapshot snapshot() {
        Snapshot s = snapshot;
        long ttl = properties.suggestCacheTtl().toNanos();
        if (s != null && System.nanoTime() - s.loadedAt() < ttl) {
            return s;
        }
        synchronized (this) {
            s = snapshot;
            if (s == null || System.nanoTime() - s.loadedAt() >= ttl) {
                s = load();
                snapshot = s;
            }
            return s;
        }
    }

    private Snapshot load() {
        List<Doc> docs = jdbc.query("""
                SELECT p.id, p.author_id, p.title, left(p.content_md, ?) AS body,
                       ARRAY(SELECT g.name FROM post_tag pt JOIN tag g ON g.id = pt.tag_id WHERE pt.post_id = p.id) AS tags
                FROM post p JOIN member m ON m.id = p.author_id
                WHERE """ + " " + accessPolicy.publicListingCondition("p", "m") + """
                 AND p.hidden_at IS NULL
                ORDER BY p.first_public_at DESC, p.id DESC LIMIT ?
                """, (rs, n) -> {
            List<String> titleWords = FuzzyText.words(rs.getString("title"));
            List<String> tags = Arrays.asList((String[]) rs.getArray("tags").getArray());
            return new Doc(rs.getLong("id"), rs.getLong("author_id"), jamoSet(titleWords), jamoSet(tags),
                    jamoSet(FuzzyText.words(rs.getString("body"))), titleWords, tags);
        }, BODY_CHARS, MAX_DOCS);
        List<String> people = new ArrayList<>();
        jdbc.query("SELECT handle, nickname FROM member WHERE withdrawn_at IS NULL", rs -> {
            people.add(rs.getString("handle"));
            people.add(rs.getString("nickname"));
        });
        return new Snapshot(System.nanoTime(), docs, vocab(docs, people));
    }

    private static Set<String> jamoSet(List<String> words) {
        Set<String> out = new HashSet<>();
        words.forEach(w -> out.add(FuzzyText.jamo(FuzzyText.key(w))));
        return out;
    }

    /** 제목 단어, 이웃한 제목 두 단어(띄어쓰기 차이용), 태그, 닉네임·아이디. */
    private static Map<String, Term> vocab(List<Doc> docs, List<String> people) {
        Map<String, Term> vocab = new HashMap<>();
        for (Doc d : docs) {
            List<String> tw = d.titleWords();
            for (int i = 0; i < tw.size(); i++) {
                add(vocab, tw.get(i));
                if (i + 1 < tw.size()) {
                    add(vocab, tw.get(i) + " " + tw.get(i + 1));
                }
            }
            d.tagNames().forEach(t -> add(vocab, t));
        }
        people.stream().filter(p -> p != null && !p.isBlank()).forEach(p -> add(vocab, p));
        return vocab;
    }

    private static void add(Map<String, Term> vocab, String display) {
        String k = FuzzyText.key(display);
        vocab.merge(k, new Term(display, FuzzyText.jamo(k), 1),
                (a, b) -> new Term(a.display(), a.jamo(), a.count() + 1));
    }
}

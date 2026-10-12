package com.team.blog.tag.application.icon;

import com.team.blog.tag.application.icon.TagIconCatalog.Style;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 056 태그 아이콘(강성찬 개인 확장): 화면에 낼 때 아이콘이 없는 태그는 내장 규칙으로 바로 정해 저장하고,
 * AI가 연결돼 있으면 작업이 규칙으로 정한 태그를 하나씩 AI에게 다시 물어 다듬는다(AI가 고른 것은 다시 묻지 않음).
 */
@Service
public class TagIconService {

    private final JdbcTemplate jdbc;
    private final TagIconAi ai;
    private final TagIconProperties properties;
    private final Clock clock;

    public TagIconService(JdbcTemplate jdbc, TagIconAi ai, TagIconProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.ai = ai;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 태그 이름 → 아이콘·색. 없는 태그 이름은 규칙대로(저장은 하지 않음). */
    public Map<String, Style> styles(Collection<String> names) {
        Map<String, Style> out = new HashMap<>();
        if (!properties.enabled() || names.isEmpty()) {
            return out;
        }
        Map<Long, String> missing = new HashMap<>();
        jdbc.query("""
                SELECT t.id, t.name, ti.icon, ti.tone FROM tag t LEFT JOIN tag_icon ti ON ti.tag_id = t.id WHERE t.name = ANY (?)
                """, rs -> {
            String icon = rs.getString("icon");
            if (icon != null) {
                out.put(rs.getString("name"), new Style(icon, rs.getString("tone")));
            } else {
                missing.put(rs.getLong("id"), rs.getString("name"));
            }
        }, (Object) names.toArray(new String[0]));
        missing.forEach((id, name) -> {
            Style s = TagIconCatalog.byRule(name);
            jdbc.update("INSERT INTO tag_icon (tag_id, icon, tone, source) VALUES (?, ?, ?, 'RULE') ON CONFLICT (tag_id) DO NOTHING",
                    id, s.icon(), s.tone());
            out.put(name, s);
        });
        for (String name : names) {
            out.computeIfAbsent(name, TagIconCatalog::byRule);
        }
        return out;
    }

    /** AI로 다듬기: 규칙으로 정한 태그를 최대 {@code batch}개. AI 연결이 끊기면 그 자리에서 멈춘다. 다듬은 수. */
    public int refineWithAi() {
        if (!properties.enabled() || !ai.available()) {
            return 0;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT t.id, t.name FROM tag_icon ti JOIN tag t ON t.id = ti.tag_id
                WHERE ti.source = 'RULE' AND ti.ai_checked_at IS NULL ORDER BY t.id LIMIT ?
                """, properties.batch());
        int changed = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            Optional<String> answer = ai.ask((String) row.get("name"));
            if (answer.isEmpty()) {
                break;
            }
            Timestamp now = Timestamp.from(clock.instant());
            Optional<Style> picked = TagIconCatalog.parseAiAnswer(answer.get());
            if (picked.isPresent()) {
                jdbc.update("UPDATE tag_icon SET icon = ?, tone = ?, source = 'AI', ai_checked_at = ?, updated_at = ? WHERE tag_id = ?",
                        picked.get().icon(), picked.get().tone(), now, now, id);
                changed++;
            } else {
                jdbc.update("UPDATE tag_icon SET ai_checked_at = ? WHERE tag_id = ?", now, id);
            }
        }
        return changed;
    }
}

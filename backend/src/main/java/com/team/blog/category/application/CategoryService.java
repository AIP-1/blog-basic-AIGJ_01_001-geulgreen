package com.team.blog.category.application;

import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 037 카테고리(강성찬 개인 확장): 블로그 주인만 만들고 고친다. 글은 카테고리 0~1개, 카테고리를 지우면 글은 분류 없음.
 * 글 수·목록은 공용 목록 조건(012) 글만 센다.
 */
@Service
public class CategoryService {

    public record Category(long id, String name, int position, long postCount) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final PostAccessPolicy accessPolicy;
    private final CategoryProperties properties;

    public CategoryService(JdbcTemplate jdbc, AccountGuard accountGuard, PostAccessPolicy accessPolicy,
                           CategoryProperties properties) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 블로그의 카테고리와 공개 글 수(순서대로). */
    public List<Category> list(long memberId) {
        return jdbc.query("SELECT c.id, c.name, c.position, (SELECT count(*) FROM post_category pc JOIN post p ON p.id = pc.post_id"
                        + " JOIN member m ON m.id = p.author_id WHERE pc.category_id = c.id AND "
                        + accessPolicy.publicListingCondition("p", "m") + ") AS cnt FROM category c WHERE c.member_id = ?"
                        + " ORDER BY c.position, c.id",
                (rs, i) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getInt("position"), rs.getLong("cnt")),
                memberId);
    }

    public Optional<Category> find(long memberId, long categoryId) {
        return list(memberId).stream().filter(c -> c.id() == categoryId).findFirst();
    }

    @Transactional
    public long create(Optional<CurrentUser> current, String rawName) {
        CurrentUser me = accountGuard.requireWritable(current);
        String name = name(rawName);
        Integer count = jdbc.queryForObject("SELECT count(*) FROM category WHERE member_id = ?", Integer.class, me.memberId());
        if (count != null && count >= properties.maxPerBlog()) {
            throw new PostContentException("CATEGORY_LIMIT");
        }
        try {
            return jdbc.queryForObject("INSERT INTO category (member_id, name, position) VALUES (?, ?, "
                    + "(SELECT COALESCE(max(position), 0) + 1 FROM category WHERE member_id = ?)) RETURNING id",
                    Long.class, me.memberId(), name, me.memberId());
        } catch (DuplicateKeyException e) {
            throw new PostContentException("CATEGORY_NAME_TAKEN");
        }
    }

    public void rename(Optional<CurrentUser> current, long id, String rawName) {
        CurrentUser me = accountGuard.requireWritable(current);
        String name = name(rawName);
        try {
            if (jdbc.update("UPDATE category SET name = ? WHERE id = ? AND member_id = ?", name, id, me.memberId()) == 0) {
                throw new NotFoundException();
            }
        } catch (DuplicateKeyException e) {
            throw new PostContentException("CATEGORY_NAME_TAKEN");
        }
    }

    public void delete(Optional<CurrentUser> current, long id) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        if (jdbc.update("DELETE FROM category WHERE id = ? AND member_id = ?", id, me.memberId()) == 0) {
            throw new NotFoundException();
        }
    }

    /** 바로 위(up) 또는 아래 카테고리와 자리를 바꾼다. 끝이면 그대로. */
    @Transactional
    public void move(Optional<CurrentUser> current, long id, boolean up) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        List<Category> all = list(me.memberId());
        int i = -1;
        for (int k = 0; k < all.size(); k++) {
            if (all.get(k).id() == id) {
                i = k;
            }
        }
        if (i < 0) {
            throw new NotFoundException();
        }
        int j = up ? i - 1 : i + 1;
        if (j < 0 || j >= all.size()) {
            return;
        }
        // 순서 값이 같을 수도 있어 둘 다 목록 순서 번호로 다시 매긴다
        for (int k = 0; k < all.size(); k++) {
            int pos = k == i ? j : k == j ? i : k;
            jdbc.update("UPDATE category SET position = ? WHERE id = ?", pos, all.get(k).id());
        }
    }

    /** 내 글에 카테고리를 단다(null이면 뗀다). 남의 글·남의 카테고리는 404. */
    @Transactional
    public void assign(Optional<CurrentUser> current, long postId, Long categoryId) {
        CurrentUser me = accountGuard.requireWritable(current);
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
        jdbc.update("DELETE FROM post_category WHERE post_id = ?", postId);
        if (categoryId == null) {
            return;
        }
        if (jdbc.update("INSERT INTO post_category (post_id, category_id) SELECT ?, id FROM category WHERE id = ? AND member_id = ?",
                postId, categoryId, me.memberId()) == 0) {
            throw new NotFoundException();
        }
    }

    /** 글의 카테고리(없으면 빈 값). */
    public Optional<Category> ofPost(long postId) {
        return jdbc.query("SELECT c.id, c.name, c.position FROM post_category pc JOIN category c ON c.id = pc.category_id"
                        + " WHERE pc.post_id = ?",
                (rs, i) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getInt("position"), 0), postId)
                .stream().findFirst();
    }

    /** 카테고리의 공개 글 번호(최신순, 앞쪽 N개). */
    public List<Long> postIds(long categoryId) {
        // ponytail: 이어 보기 없이 최신 N개(list-size)만. 글이 많아지면 커서 이어 보기로 바꾼다.
        return jdbc.queryForList("SELECT p.id FROM post_category pc JOIN post p ON p.id = pc.post_id JOIN member m ON m.id = p.author_id"
                        + " WHERE pc.category_id = ? AND " + accessPolicy.publicListingCondition("p", "m")
                        + " ORDER BY p.first_public_at DESC, p.id DESC LIMIT " + properties.listSize(),
                Long.class, categoryId);
    }

    /** 탈퇴 정리. 글과의 연결은 함께 지워진다. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM category WHERE member_id = ?", memberId);
    }

    private static String name(String raw) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > 30) {
            throw new PostContentException("CATEGORY_NAME_INVALID");
        }
        return name;
    }
}

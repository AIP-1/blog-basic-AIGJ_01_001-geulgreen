package com.team.blog.cover.application;

import com.team.blog.media.application.ImageReferenceLookup;
import com.team.blog.media.application.PostImageService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 055 카드 대표 사진(강성찬 개인 확장): 내 글에 내가 올린 글 사진 하나를 대표로 정한다(본문에 있든 따로 올렸든).
 * 정하지 않으면 공통 규칙대로 본문 첫 사진(005·008)이 카드에 쓰인다. 사진 정리 작업은 대표 사진을 지우지 않는다.
 */
@Service
public class CoverService implements ImageReferenceLookup {

    @ConfigurationProperties("blog.cover")
    public record Properties(@DefaultValue("true") boolean enabled) {
    }

    public record Cover(long imageId, String imageUrl, String thumbUrl) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final PostImageService postImages;
    private final Properties properties;

    public CoverService(JdbcTemplate jdbc, AccountGuard accountGuard, PostImageService postImages, Properties properties) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.postImages = postImages;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 글쓴이만(남의 글·지운 글은 404). */
    public Optional<Cover> get(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        requireMyPost(me.memberId(), postId);
        return of(postId);
    }

    public Optional<Cover> of(long postId) {
        return jdbc.query("SELECT image_id, image_url, thumb_url FROM post_cover WHERE post_id = ?",
                (rs, i) -> new Cover(rs.getLong("image_id"), rs.getString("image_url"), rs.getString("thumb_url")), postId)
                .stream().findFirst();
    }

    /** 대표 사진 정하기: 우리 저장소에 내가 올린(올리기를 마친) 글 사진 주소만. */
    public Cover set(Optional<CurrentUser> current, long postId, String imageUrl) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMyPost(me.memberId(), postId);
        String url = imageUrl == null ? "" : imageUrl.strip();
        long imageId = postImages.ownedCompletedImageId(me.memberId(), url)
                .orElseThrow(() -> new PostContentException("COVER_IMAGE_INVALID"));
        String thumb = postImages.cardThumbnailOf(url);
        jdbc.update("""
                INSERT INTO post_cover (post_id, image_id, image_url, thumb_url) VALUES (?, ?, ?, ?)
                ON CONFLICT (post_id) DO UPDATE SET image_id = EXCLUDED.image_id, image_url = EXCLUDED.image_url,
                    thumb_url = EXCLUDED.thumb_url, updated_at = CURRENT_TIMESTAMP
                """, postId, imageId, url, thumb);
        return new Cover(imageId, url, thumb);
    }

    /** 자동(본문 첫 사진)으로 되돌리기. */
    public void clear(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        requireMyPost(me.memberId(), postId);
        jdbc.update("DELETE FROM post_cover WHERE post_id = ?", postId);
    }

    @Override
    public Set<Long> referencedIds(Collection<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT image_id FROM post_cover WHERE image_id = ANY (?)", Long.class,
                (Object) imageIds.toArray(new Long[0])));
    }

    private void requireMyPost(long memberId, long postId) {
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, memberId);
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
    }
}

package com.team.blog.cover.application;

import com.team.blog.media.application.ImageReferenceLookup;
import com.team.blog.media.application.ImageStorage;
import com.team.blog.media.application.ImageUploadService;
import com.team.blog.media.application.PostImageService;
import com.team.blog.media.application.PresignCommand;
import com.team.blog.media.application.PresignResult;
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
    private final ThumbnailRenderer renderer;
    private final ImageUploadService uploads;
    private final ImageStorage storage;
    private final java.time.Clock clock;

    public CoverService(JdbcTemplate jdbc, AccountGuard accountGuard, PostImageService postImages, Properties properties,
                        ThumbnailRenderer renderer, ImageUploadService uploads, ImageStorage storage, java.time.Clock clock) {
        this.renderer = renderer;
        this.uploads = uploads;
        this.storage = storage;
        this.clock = clock;
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

    private static final java.time.format.DateTimeFormatter DATE =
            java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(java.time.ZoneId.of("Asia/Seoul"));

    /**
     * 110 강성찬 개인 확장: 썸네일을 서버에서 그려(제목·시리즈·아이디·오늘 날짜) 내 사진으로 올리고 대표 사진으로 정한다.
     * 올리기는 화면과 같은 승인·완료 검사({@link ImageUploadService})를 거치고, 파일만 서버가 직접 넣는다(083 테스트 글과 같은 길).
     * {@code title}이 비면 글 제목. 글쓰기 화면은 브라우저에서 같은 배치로 그린다(thumbnail.js).
     */
    public Cover generate(Optional<CurrentUser> current, long postId, String title, String theme) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMyPost(me.memberId(), postId);
        var row = jdbc.queryForMap("""
                SELECT p.title, m.handle, (SELECT s.title FROM series_post sp JOIN series s ON s.id = sp.series_id
                                           WHERE sp.post_id = p.id) AS series_title
                FROM post p JOIN member m ON m.id = p.author_id WHERE p.id = ?""", postId);
        String text = title == null || title.isBlank() ? (String) row.get("title") : title.strip();
        byte[] png = renderer.render(text, (String) row.get("series_title"), "@" + row.get("handle"),
                DATE.format(clock.instant()), theme);
        PresignResult presigned = uploads.presign(current, new PresignCommand("POST", "image/png", (long) png.length, "thumbnail.png"));
        String key = jdbc.queryForObject("SELECT storage_key FROM image WHERE id = ?", String.class, presigned.imageId());
        storage.put(key, "image/png", png);
        return set(current, postId, uploads.complete(current, presigned.imageId()).url());
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

-- 055 카드 대표 사진(강성찬 개인 확장): 글쓴이가 고르거나 따로 올린 사진을 카드·링크 미리보기에 쓴다. 없으면 본문 첫 사진(공통).
CREATE TABLE post_cover (
    post_id                bigint NOT NULL,
    image_id               bigint NOT NULL,
    image_url              varchar(500) NOT NULL,
    thumb_url              varchar(500) NOT NULL,
    updated_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_post_cover_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_cover_image FOREIGN KEY (image_id) REFERENCES image (id) ON DELETE CASCADE
);
CREATE INDEX ix_post_cover_image ON post_cover (image_id);

COMMENT ON TABLE post_cover IS '카드 대표 사진(055), 사진 정리 작업은 여기 있는 사진을 지우지 않는다';

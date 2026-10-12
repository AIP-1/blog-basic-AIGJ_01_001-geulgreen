-- 037~043 강성찬 개인 확장(티스토리 참고): 카테고리, 방명록, 시리즈, 예약 발행, 비밀 댓글.
CREATE TABLE category (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    member_id              bigint NOT NULL,
    name                   varchar(30) NOT NULL,
    position               integer NOT NULL DEFAULT 0,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_category_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT uq_category_name UNIQUE (member_id, name),
    CONSTRAINT ck_category_name CHECK (length(btrim(name)) BETWEEN 1 AND 30)
);

CREATE TABLE post_category (
    post_id                bigint NOT NULL,
    category_id            bigint NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_post_category_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_category_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE
);
CREATE INDEX ix_post_category_category ON post_category (category_id);

CREATE TABLE guestbook_entry (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    owner_id               bigint NOT NULL,
    author_id              bigint NOT NULL,
    content                varchar(300) NOT NULL,
    secret                 boolean NOT NULL DEFAULT false,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_guestbook_owner FOREIGN KEY (owner_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_guestbook_author FOREIGN KEY (author_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_guestbook_content CHECK (length(btrim(content)) > 0)
);
CREATE INDEX ix_guestbook_owner ON guestbook_entry (owner_id, id DESC);

CREATE TABLE series (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    member_id              bigint NOT NULL,
    title                  varchar(60) NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_series_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT uq_series_title UNIQUE (member_id, title),
    CONSTRAINT ck_series_title CHECK (length(btrim(title)) BETWEEN 1 AND 60)
);

CREATE TABLE series_post (
    post_id                bigint NOT NULL,
    series_id              bigint NOT NULL,
    position               integer NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_series_post_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_series_post_series FOREIGN KEY (series_id) REFERENCES series (id) ON DELETE CASCADE
);
CREATE INDEX ix_series_post_series ON series_post (series_id, position);

-- 042: 예약 발행. 그 시각에 고른 공개 범위·태그로 발행 서비스를 부른다(글은 그때까지 임시글).
CREATE TABLE post_schedule (
    post_id                bigint NOT NULL,
    publish_at             timestamptz NOT NULL,
    visibility             varchar(20) NOT NULL,
    tags                   text NOT NULL DEFAULT '',
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_post_schedule_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);
CREATE INDEX ix_post_schedule_at ON post_schedule (publish_at);

-- 043: 비밀 댓글
ALTER TABLE comment ADD COLUMN secret boolean NOT NULL DEFAULT false;

COMMENT ON TABLE category IS '블로그 카테고리(037)';
COMMENT ON TABLE post_category IS '글의 카테고리, 글당 하나(037)';
COMMENT ON TABLE guestbook_entry IS '방명록(040)';
COMMENT ON TABLE series IS '시리즈(041)';
COMMENT ON TABLE series_post IS '시리즈에 든 글과 순서(041)';
COMMENT ON TABLE post_schedule IS '예약 발행(042)';

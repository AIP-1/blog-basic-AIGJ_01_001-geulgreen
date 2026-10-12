-- 051 방문자 수, 054 글 고정(강성찬 개인 확장).
CREATE TABLE blog_visit_daily (
    member_id              bigint NOT NULL,
    day                    date NOT NULL,
    visitors               integer NOT NULL DEFAULT 0,
    PRIMARY KEY (member_id, day),
    CONSTRAINT fk_blog_visit_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_blog_visit_count CHECK (visitors >= 0)
);

CREATE TABLE post_pin (
    member_id              bigint NOT NULL,
    post_id                bigint NOT NULL,
    pinned_at              timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_post_pin_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_pin_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

COMMENT ON TABLE blog_visit_daily IS '블로그 하루 방문자 수(051), 같은 방문자는 하루 한 번';
COMMENT ON TABLE post_pin IS '블로그 맨 위 고정 글, 블로그당 하나(054)';

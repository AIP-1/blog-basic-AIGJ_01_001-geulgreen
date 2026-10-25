-- 10-23 chan: 글 저장(북마크). 나만 보는 "나중에 다시 읽기" 목록. 글쓴이에게 알리지 않고 수도 세지 않는다.
-- 더하기만. 글이 지워지면 같이, 회원은 탈퇴 익명 처리 단계(InteractionPurgeSteps.Likes)에서 지운다.
CREATE TABLE post_bookmark (
    post_id                bigint NOT NULL,
    member_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, member_id),
    CONSTRAINT fk_post_bookmark_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_bookmark_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX ix_post_bookmark_member ON post_bookmark (member_id, created_at DESC);

COMMENT ON TABLE post_bookmark IS '글 저장(북마크, 저장한 사람만 봄)';

COMMENT ON COLUMN post_bookmark.post_id IS '글 번호';

COMMENT ON COLUMN post_bookmark.member_id IS '저장한 회원 번호';

COMMENT ON COLUMN post_bookmark.created_at IS '저장한 일시';

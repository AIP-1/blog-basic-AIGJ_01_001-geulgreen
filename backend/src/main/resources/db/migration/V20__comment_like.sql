-- 089 강성찬 개인 확장: 댓글·답글 좋아요(더하기만). 댓글이 지워지면 좋아요도 같이 지운다.
CREATE TABLE comment_like (
    comment_id             bigint NOT NULL,
    member_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, member_id),
    CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES comment (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_like_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

COMMENT ON TABLE comment_like IS '댓글 좋아요';

COMMENT ON COLUMN comment_like.comment_id IS '댓글 번호';

COMMENT ON COLUMN comment_like.member_id IS '누른 회원 번호';

COMMENT ON COLUMN comment_like.created_at IS '누른 일시';

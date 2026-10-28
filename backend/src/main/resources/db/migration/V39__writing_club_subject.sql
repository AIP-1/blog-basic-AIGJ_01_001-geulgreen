-- 10-28 강성찬 개인 확장: 함께 쓰기 방마다 주제. 방을 만들 때 주제를 정하고, 글쓴이가 발행 설정에서 고른 방에만 글이 올라간다
-- (멤버의 전체 공개 글이 모든 방에 저절로 올라가던 것을 바꿈). 더하기만, 지우는 칸·줄은 없다.
ALTER TABLE writing_club ADD COLUMN subject varchar(40) NOT NULL DEFAULT '';
COMMENT ON COLUMN writing_club.subject IS '방 주제(10-28): 이 방에 어떤 글을 올리는지 한 줄. 예전 방은 빈 값(방장이 방 설정에서 정함)';

-- 글쓴이가 고른 방. 임시글일 때도 줄이 있고, 방 화면에는 전체 공개된 글만 보인다. 방장·관리자가 내리면 줄이 사라진다(글은 남음).
CREATE TABLE writing_club_post (
    club_id                bigint NOT NULL,
    post_id                bigint NOT NULL,
    member_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (club_id, post_id),
    CONSTRAINT fk_writing_club_post_club FOREIGN KEY (club_id) REFERENCES writing_club (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_post_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_post_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_writing_club_post_post ON writing_club_post (post_id);
CREATE INDEX ix_writing_club_post_member ON writing_club_post (member_id);
COMMENT ON TABLE writing_club_post IS '방에 올린 글(10-28): 글쓴이가 발행 설정에서 고른 방. 방 화면은 이 줄의 전체 공개 글만 보여 준다';

-- 이미 방 글감으로 쓴 글은 그 방에 그대로 남긴다. 다른 글은 방에서 빠진다(글은 블로그에 그대로, 발행 설정에서 다시 올릴 수 있음).
INSERT INTO writing_club_post (club_id, post_id, member_id, created_at)
SELECT t.club_id, tp.post_id, tp.member_id, tp.created_at
FROM writing_club_topic_post tp JOIN writing_club_topic t ON t.id = tp.topic_id
ON CONFLICT DO NOTHING;

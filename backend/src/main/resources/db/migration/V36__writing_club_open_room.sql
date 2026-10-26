-- 10-25 강성찬 개인 확장: 함께 쓰기를 오픈채팅방처럼(기간 없앰, 방 글감, 새 글 수). 더하기만, 지우는 칸·줄은 없다.
-- 기간 칸(every_days·end_date)은 남겨 두고 쓰지 않는다. start_date는 "방이 열린 날"로 이날부터 멤버들의 전체 공개 글이 방에 보인다.
COMMENT ON COLUMN writing_club.every_days IS '더 쓰지 않음(10-25 기간 없앰). 예전 값은 그대로 둔다';
COMMENT ON COLUMN writing_club.end_date IS '더 쓰지 않음(10-25 기간 없앰, 끝난 방도 다시 열림). 새 방은 9999-12-31';
COMMENT ON COLUMN writing_club.start_date IS '방이 열린 날: 이날(한국 날짜)부터 멤버들의 전체 공개 글이 방에 보인다';
COMMENT ON TABLE writing_club IS '함께 쓰기 방: 멤버들의 전체 공개 글이 시간 순으로 모이는 곳(기간 없음)';

-- 방 목록의 "새 글 N": 내가 방을 마지막으로 본 때. 비어 있으면 들어온 때부터 센다.
ALTER TABLE writing_club_member ADD COLUMN last_seen_at timestamptz NULL;
COMMENT ON COLUMN writing_club_member.last_seen_at IS '이 멤버가 방 화면을 마지막으로 본 때(새 글 수 계산용)';

-- 방 글감: 만든 사람·관리자가 올린다. 가장 최근 것이 방 위에 고정되고, 이전 것은 지난 글감.
CREATE TABLE writing_club_topic (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    club_id                bigint NOT NULL,
    created_by             bigint NOT NULL,
    title                  varchar(60) NOT NULL,
    hint                   varchar(200) NOT NULL DEFAULT '',
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_writing_club_topic_club FOREIGN KEY (club_id) REFERENCES writing_club (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_topic_creator FOREIGN KEY (created_by) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_writing_club_topic_title CHECK (length(btrim(title)) BETWEEN 1 AND 60)
);
CREATE INDEX ix_writing_club_topic_club ON writing_club_topic (club_id, created_at DESC);

-- 방 글감으로 연 글. 글을 지우거나 글감을 지우면 줄도 사라진다(글은 남음).
CREATE TABLE writing_club_topic_post (
    post_id                bigint NOT NULL,
    topic_id               bigint NOT NULL,
    member_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_writing_club_topic_post_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_topic_post_topic FOREIGN KEY (topic_id) REFERENCES writing_club_topic (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_topic_post_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_writing_club_topic_post_topic ON writing_club_topic_post (topic_id);
CREATE INDEX ix_writing_club_topic_post_member ON writing_club_topic_post (member_id, topic_id);

COMMENT ON TABLE writing_club_topic IS '방 글감(10-25): 최근 것이 방 위에 고정, 이전 것은 지난 글감';
COMMENT ON TABLE writing_club_topic_post IS '방 글감으로 연 글(10-25). 방 화면에서 "방 글감" 표시';

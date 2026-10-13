-- 069 친한 친구(강성찬 개인 확장): 서로 요청·수락하던 친구(025)를, 나를 팔로우하는 사람 중에서 내가 골라 넣는 한쪽 관계로 바꾼다.
-- 공통 테이블 friendship은 그대로 두고 비우기만 한다(03 E-10). 이미 친구였던 쌍은 양쪽 모두의 친구로 옮기고, 대기 중 요청과 그 알림은 지운다.
CREATE TABLE close_friend (
    owner_id               bigint NOT NULL,
    friend_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner_id, friend_id),
    CONSTRAINT fk_close_friend_owner FOREIGN KEY (owner_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_close_friend_friend FOREIGN KEY (friend_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_close_friend_self CHECK (owner_id <> friend_id)
);
CREATE INDEX ix_close_friend_friend ON close_friend (friend_id);

COMMENT ON TABLE close_friend IS '친한 친구: owner가 고른 friend는 owner의 친구 공개 글을 읽는다(069)';
COMMENT ON COLUMN close_friend.owner_id IS '고른 회원 번호';
COMMENT ON COLUMN close_friend.friend_id IS '친구로 넣은 회원 번호';
COMMENT ON COLUMN close_friend.created_at IS '넣은 일자';

INSERT INTO close_friend (owner_id, friend_id, created_at)
SELECT member_a_id, member_b_id, accepted_at FROM friendship WHERE status = 'ACCEPTED'
UNION ALL
SELECT member_b_id, member_a_id, accepted_at FROM friendship WHERE status = 'ACCEPTED';

DELETE FROM friendship;
DELETE FROM notification WHERE type = 'FRIEND_REQUEST';

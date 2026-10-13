-- 073 비공개 계정(강성찬 개인 확장, 인스타처럼): 켜면 팔로우는 요청이 되고, 주인이 수락한 사람만 글을 본다. 공통 테이블은 CHECK 교체만.
CREATE TABLE private_account (
    member_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_private_account_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE TABLE follow_request (
    requester_id           bigint NOT NULL,
    target_id              bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (requester_id, target_id),
    CONSTRAINT fk_follow_request_requester FOREIGN KEY (requester_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_request_target FOREIGN KEY (target_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_follow_request_self CHECK (requester_id <> target_id)
);
CREATE INDEX ix_follow_request_target ON follow_request (target_id, created_at DESC);

COMMENT ON TABLE private_account IS '비공개 계정(073): 수락한 팔로워만 글을 본다';
COMMENT ON COLUMN private_account.member_id IS '회원 번호';
COMMENT ON COLUMN private_account.created_at IS '비공개로 바꾼 일자';
COMMENT ON TABLE follow_request IS '비공개 계정에 보낸 팔로우 요청(073), 수락하면 follow로 옮긴다';
COMMENT ON COLUMN follow_request.requester_id IS '요청한 회원 번호';
COMMENT ON COLUMN follow_request.target_id IS '비공개 계정 회원 번호';
COMMENT ON COLUMN follow_request.created_at IS '요청 일자';

-- 팔로우 요청 알림(팔로우처럼 묶음)
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'FOLLOW', 'NEW_POST',
    'REPORT_RESOLVED', 'CONTENT_HIDDEN', 'FRIEND_REQUEST', 'FIRST_PUBLIC', 'MISSION_DONE', 'FOLLOW_REQUEST'));
ALTER TABLE notification DROP CONSTRAINT ck_notification_group;
ALTER TABLE notification ADD CONSTRAINT ck_notification_group CHECK ((type IN ('LIKE', 'FOLLOW', 'FRIEND_REQUEST', 'FOLLOW_REQUEST')) = (group_key IS NOT NULL));

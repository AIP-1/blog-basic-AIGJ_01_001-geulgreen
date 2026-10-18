-- 111 후속(강성찬 개인 확장): 함께 쓰기 참여 경로 넓히기(더하기만, 있던 함께 쓰기는 '링크만' 그대로).
-- 참여 방식: LINK(초대 링크만) / APPROVAL(공개 목록에 보이고, 만든 사람이 승인) / OPEN(공개 목록에서 누구나 바로)
ALTER TABLE writing_club ADD COLUMN join_mode varchar(10) NOT NULL DEFAULT 'LINK';
ALTER TABLE writing_club ADD COLUMN member_cap int NOT NULL DEFAULT 30;
ALTER TABLE writing_club ADD CONSTRAINT ck_writing_club_join_mode CHECK (join_mode IN ('LINK', 'APPROVAL', 'OPEN'));
ALTER TABLE writing_club ADD CONSTRAINT ck_writing_club_cap CHECK (member_cap BETWEEN 2 AND 100);
CREATE INDEX ix_writing_club_listed ON writing_club (join_mode, end_date);

-- 참여 요청(본인이 보냄, 만든 사람이 승인)과 초대(멤버가 팔로워·친구에게 보냄, 받은 사람이 수락). 한 사람당 한 줄.
CREATE TABLE writing_club_join (
    club_id                bigint NOT NULL,
    member_id              bigint NOT NULL,
    kind                   varchar(10) NOT NULL,
    invited_by             bigint NULL,
    rejected_at            timestamptz NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (club_id, member_id),
    CONSTRAINT fk_writing_club_join_club FOREIGN KEY (club_id) REFERENCES writing_club (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_join_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_join_inviter FOREIGN KEY (invited_by) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_writing_club_join_kind CHECK (kind IN ('REQUEST', 'INVITE')),
    CONSTRAINT ck_writing_club_join_inviter CHECK ((kind = 'INVITE') = (invited_by IS NOT NULL))
);
CREATE INDEX ix_writing_club_join_member ON writing_club_join (member_id);
CREATE INDEX ix_writing_club_join_inviter ON writing_club_join (invited_by, created_at);

-- 알림: 초대받음(CLUB_INVITE), 참여 요청이 옴(CLUB_REQUEST). 알림이 그 함께 쓰기로 가게 번호를 담는다.
ALTER TABLE notification ADD COLUMN club_id bigint NULL;
ALTER TABLE notification ADD CONSTRAINT fk_notification_club FOREIGN KEY (club_id) REFERENCES writing_club (id) ON DELETE CASCADE;
CREATE INDEX ix_notification_club ON notification (club_id);
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'FOLLOW', 'NEW_POST',
    'REPORT_RESOLVED', 'CONTENT_HIDDEN', 'FRIEND_REQUEST', 'FIRST_PUBLIC', 'MISSION_DONE', 'FOLLOW_REQUEST', 'INQUIRY',
    'INQUIRY_REPLIED', 'CLUB_INVITE', 'CLUB_REQUEST'));

COMMENT ON COLUMN writing_club.join_mode IS '참여 방식: LINK 링크만, APPROVAL 승인 후 참여, OPEN 누구나 바로';
COMMENT ON COLUMN writing_club.member_cap IS '멤버 상한(만든 사람 포함)';
COMMENT ON TABLE writing_club_join IS '함께 쓰기 참여 요청(REQUEST)과 초대(INVITE). 거절된 요청은 rejected_at으로 7일 동안 다시 못 보냄';
COMMENT ON COLUMN notification.club_id IS '함께 쓰기 초대·참여 요청 알림이 가리키는 함께 쓰기';

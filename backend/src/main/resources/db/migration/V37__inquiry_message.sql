-- 10-26 강성찬 개인 확장: 문의를 대화로. 관리자와 보낸 회원이 사이트 안에서 여러 번 주고받는다(더하기만).
-- inquiry.reply·replied_at은 남겨 두고 "가장 최근 관리자 답장"으로 계속 채운다. 예전 답장은 대화의 첫 답장으로 옮긴다.
CREATE TABLE inquiry_message (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    inquiry_id             bigint NOT NULL,
    author_id              bigint NULL,
    from_admin             boolean NOT NULL,
    body                   varchar(3000) NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiry_message_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry (id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_message_author FOREIGN KEY (author_id) REFERENCES member (id) ON DELETE SET NULL
);
CREATE INDEX ix_inquiry_message_inquiry ON inquiry_message (inquiry_id, id);

COMMENT ON TABLE inquiry_message IS '문의 대화: 처음 문의 뒤에 오간 답장(관리자·보낸 회원)';

COMMENT ON COLUMN inquiry_message.author_id IS '쓴 회원 번호(탈퇴하면 비움)';

COMMENT ON COLUMN inquiry_message.from_admin IS '관리자 답장이면 참, 보낸 회원의 다시 답하기면 거짓';

INSERT INTO inquiry_message (inquiry_id, author_id, from_admin, body, created_at)
SELECT id, replied_by, true, reply, coalesce(replied_at, created_at) FROM inquiry WHERE reply IS NOT NULL ORDER BY id;

COMMENT ON COLUMN inquiry.reply IS '가장 최근 관리자 답장(전체 대화는 inquiry_message)';

COMMENT ON COLUMN inquiry.status IS '관리자 차례(NEW)·보낸 사람 차례 또는 처리 완료(DONE)';

-- 보낸 회원이 다시 답하면 관리자에게 그 문의로 가는 알림
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'FOLLOW', 'NEW_POST',
    'REPORT_RESOLVED', 'CONTENT_HIDDEN', 'FRIEND_REQUEST', 'FIRST_PUBLIC', 'MISSION_DONE', 'FOLLOW_REQUEST', 'INQUIRY',
    'INQUIRY_REPLIED', 'CLUB_INVITE', 'CLUB_REQUEST', 'HANDLE_CHANGED', 'INQUIRY_FOLLOWUP'));

-- 090 강성찬 개인 확장: 문의하기. 사이트 안 문의 양식으로 받아 관리자 문의함에서 답장한다(더하기만).
CREATE TABLE inquiry (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    member_id              bigint NULL,
    kind                   varchar(20) NOT NULL,
    title                  varchar(100) NOT NULL,
    content                varchar(3000) NOT NULL,
    email                  varchar(254) NOT NULL,
    status                 varchar(10) NOT NULL DEFAULT 'NEW',
    reply                  varchar(3000) NULL,
    replied_by             bigint NULL,
    replied_at             timestamptz NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiry_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_replied_by FOREIGN KEY (replied_by) REFERENCES member (id) ON DELETE SET NULL,
    CONSTRAINT ck_inquiry_kind CHECK (kind IN ('ACCOUNT', 'BUG', 'IDEA', 'OTHER')),
    CONSTRAINT ck_inquiry_status CHECK (status IN ('NEW', 'DONE'))
);
CREATE INDEX ix_inquiry_status ON inquiry (status, id DESC);
CREATE INDEX ix_inquiry_member ON inquiry (member_id);

COMMENT ON TABLE inquiry IS '문의(090)';

COMMENT ON COLUMN inquiry.member_id IS '보낸 회원 번호(비회원이면 비움)';

COMMENT ON COLUMN inquiry.kind IS '문의 종류';

COMMENT ON COLUMN inquiry.email IS '답장 받을 이메일';

COMMENT ON COLUMN inquiry.status IS '새 문의(NEW)·처리 완료(DONE)';

COMMENT ON COLUMN inquiry.reply IS '보낸 답장';

-- 새 문의 알림(관리자에게, 운영 알림처럼 끌 수 없음)
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'FOLLOW', 'NEW_POST',
    'REPORT_RESOLVED', 'CONTENT_HIDDEN', 'FRIEND_REQUEST', 'FIRST_PUBLIC', 'MISSION_DONE', 'FOLLOW_REQUEST', 'INQUIRY'));

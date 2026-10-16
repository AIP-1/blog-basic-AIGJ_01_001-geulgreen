-- 099 강성찬 개인 확장: 내 문의. 관리자가 답장하면 보낸 회원에게 알림을 주고, 알림이 그 문의로 바로 가게 문의 번호를 담는다(더하기만).
ALTER TABLE notification ADD COLUMN inquiry_id bigint NULL;
ALTER TABLE notification ADD CONSTRAINT fk_notification_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry (id) ON DELETE CASCADE;
CREATE INDEX ix_notification_inquiry ON notification (inquiry_id);

COMMENT ON COLUMN notification.inquiry_id IS '답장이 온 문의 번호(문의 답장 알림)';

ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'FOLLOW', 'NEW_POST',
    'REPORT_RESOLVED', 'CONTENT_HIDDEN', 'FRIEND_REQUEST', 'FIRST_PUBLIC', 'MISSION_DONE', 'FOLLOW_REQUEST', 'INQUIRY',
    'INQUIRY_REPLIED'));

-- 096 강성찬 개인 확장: 텔레그램 알림. 회원별로 봇과 연결한 채팅 번호(더하기만). 사이트의 [텔레그램 연결] → 봇에 /start로 저장된다.
CREATE TABLE telegram_link (
    member_id  bigint NOT NULL,
    chat_id    bigint NOT NULL,
    linked_at  timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_telegram_link_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

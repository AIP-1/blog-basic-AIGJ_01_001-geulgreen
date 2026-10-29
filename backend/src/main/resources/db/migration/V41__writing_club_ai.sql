-- 10-29 강성찬 개인 확장: 함께 쓰기 방장이 자기 AI(ChatGPT·Gemini·Claude) API 키를 방에 연결해 방 글감을 추천받는다.
-- 방마다 하나. 키는 서버 비밀값(CLUB_AI_SECRET)으로 AES-GCM 암호화해 두고, 화면에는 끝 4자리만 보여 준다. 더하기만.
CREATE TABLE writing_club_ai (
    club_id                bigint NOT NULL,
    provider               varchar(10) NOT NULL,
    key_cipher             text NOT NULL,
    key_tail               varchar(4) NOT NULL,
    connected_by           bigint NOT NULL,
    connected_at           timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_on                date NULL,
    used_count             integer NOT NULL DEFAULT 0,
    PRIMARY KEY (club_id),
    CONSTRAINT ck_writing_club_ai_provider CHECK (provider IN ('OPENAI', 'GEMINI', 'CLAUDE')),
    CONSTRAINT fk_writing_club_ai_club FOREIGN KEY (club_id) REFERENCES writing_club (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_ai_member FOREIGN KEY (connected_by) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_writing_club_ai_member ON writing_club_ai (connected_by);
COMMENT ON TABLE writing_club_ai IS '방장이 연결한 AI(10-29): 방 글감 추천에만 쓴다. 키는 암호문만, 방장이 탈퇴하거나 방이 없어지면 함께 지운다';
COMMENT ON COLUMN writing_club_ai.used_on IS '추천을 부른 날(한국 날짜). 날이 바뀌면 used_count를 0부터 센다(방마다 하루 한도)';

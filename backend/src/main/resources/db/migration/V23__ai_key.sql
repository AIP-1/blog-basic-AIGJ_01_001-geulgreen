-- 097 강성찬 개인 확장: AI 연결(MCP). 회원이 발급한 개인 연결 키. 원문은 저장하지 않고 SHA-256 해시만 둔다(더하기만).
CREATE TABLE ai_key (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    member_id     bigint NOT NULL,
    name          varchar(40) NOT NULL,
    key_hash      char(64) NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at  timestamptz NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_ai_key_hash UNIQUE (key_hash),
    CONSTRAINT fk_ai_key_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_ai_key_member ON ai_key (member_id);

COMMENT ON TABLE ai_key IS 'AI 연결 키(097)';

COMMENT ON COLUMN ai_key.name IS '키 이름(어느 도구에 붙였는지)';

COMMENT ON COLUMN ai_key.key_hash IS '키 원문의 SHA-256(소문자 16진수)';

COMMENT ON COLUMN ai_key.last_used_at IS '마지막 사용 시각(1분 단위로 갱신)';

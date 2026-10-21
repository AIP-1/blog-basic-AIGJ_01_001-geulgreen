-- 10-21 chan: 가입할 때 나이 확인. 만 14세 이상은 AGE14 동의, 14세 미만은 보호자(법정대리인) 동의(GUARDIAN)를 받아야 계정이 열린다.
-- 더하기만, 이미 가입한 계정은 그대로.
ALTER TABLE member_agreement DROP CONSTRAINT ck_member_agreement_type;
ALTER TABLE member_agreement ADD CONSTRAINT ck_member_agreement_type CHECK (type IN ('TERMS', 'PRIVACY', 'AI', 'AGE14', 'GUARDIAN'));

-- 14세 미만 가입: 보호자 동의를 받기 위한 최소 정보(보호자 이름·이메일)와 동의 링크. 동의 전에는 로그인할 수 없다.
-- 기한(5일) 안에 동의하지 않거나 거절하면 계정과 이 행을 지운다(개인정보 보호법 시행령 제17조의2).
CREATE TABLE guardian_consent (
    member_id              bigint NOT NULL,
    guardian_name          varchar(30) NOT NULL,
    guardian_email         varchar(254) NOT NULL,
    token_hash             varchar(64) NOT NULL,
    requested_at           timestamptz NOT NULL,
    expires_at             timestamptz NOT NULL,
    consented_at           timestamptz NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT uq_guardian_consent_token UNIQUE (token_hash),
    CONSTRAINT fk_guardian_consent_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_guardian_consent_pending ON guardian_consent (expires_at) WHERE consented_at IS NULL;

COMMENT ON TABLE guardian_consent IS '14세 미만 회원의 보호자 동의. consented_at이 비어 있으면 아직 동의 전(로그인 불가)';

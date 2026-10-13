-- 074 인스타식 계정(강성찬 개인 확장): 블로그 주소(아이디) 변경 기록과 닉네임 "14일에 두 번" 판정용 직전 변경 시각.
CREATE TABLE handle_history (
    old_handle             varchar(39) NOT NULL,
    member_id              bigint NOT NULL,
    changed_at             timestamptz NOT NULL,
    hold_until             timestamptz NOT NULL,
    PRIMARY KEY (old_handle),
    CONSTRAINT fk_handle_history_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_handle_history_member ON handle_history (member_id, changed_at DESC);

COMMENT ON TABLE handle_history IS '바꾸기 전 블로그 주소(074): 옛 주소로 오면 새 주소로 넘기고, 잡아 두는 동안은 본인만 되돌릴 수 있다';
COMMENT ON COLUMN handle_history.old_handle IS '예전 블로그 주소';
COMMENT ON COLUMN handle_history.member_id IS '회원 번호';
COMMENT ON COLUMN handle_history.changed_at IS '바꾼 일자';
COMMENT ON COLUMN handle_history.hold_until IS '이 시각까지는 다른 사람이 가져갈 수 없음';

ALTER TABLE member ADD COLUMN nickname_prev_changed_at timestamptz NULL;
COMMENT ON COLUMN member.nickname_prev_changed_at IS '그 전 닉네임 변경 일자(074: 14일에 두 번)';

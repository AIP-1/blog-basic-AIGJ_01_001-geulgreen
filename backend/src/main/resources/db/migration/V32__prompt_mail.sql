-- 강성찬 개인 확장(10-21): 저녁 글감 메일. 켠 사람만 행이 있고, 그날 글을 안 쓴 날에만 저녁에 한 통.
CREATE TABLE prompt_mail (
    member_id              bigint NOT NULL,
    last_sent_on           date NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_prompt_mail_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

COMMENT ON TABLE prompt_mail IS '저녁 글감 메일을 켠 회원, last_sent_on = 마지막으로 보낸 날(한국 시간)';

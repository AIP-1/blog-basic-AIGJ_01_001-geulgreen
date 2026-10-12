-- 056 태그 아이콘(강성찬 개인 확장): 태그마다 어울리는 아이콘·색. 처음엔 내장 규칙(RULE)으로 정하고, AI가 연결돼 있으면 다시 골라 다듬는다(AI).
CREATE TABLE tag_icon (
    tag_id                 bigint NOT NULL,
    icon                   varchar(30) NOT NULL,
    tone                   varchar(20) NOT NULL,
    source                 varchar(10) NOT NULL DEFAULT 'RULE',
    ai_checked_at          timestamptz NULL,
    updated_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tag_id),
    CONSTRAINT fk_tag_icon_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
    CONSTRAINT ck_tag_icon_source CHECK (source IN ('RULE', 'AI'))
);

COMMENT ON TABLE tag_icon IS '태그 아이콘·색(056), RULE=내장 규칙, AI=AI가 고름';

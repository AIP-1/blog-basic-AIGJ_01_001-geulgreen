-- 045~050 관리자 화면(강성찬 개인 확장): 사이트 공지, 관리 기록.
CREATE TABLE site_notice (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    message                varchar(200) NOT NULL,
    link_url               varchar(300) NULL,
    active                 boolean NOT NULL DEFAULT true,
    created_by             bigint NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_site_notice_creator FOREIGN KEY (created_by) REFERENCES member (id) ON DELETE RESTRICT,
    CONSTRAINT ck_site_notice_message CHECK (length(btrim(message)) BETWEEN 1 AND 200)
);

CREATE TABLE admin_log (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    admin_id               bigint NOT NULL,
    action                 varchar(40) NOT NULL,
    target                 varchar(100) NOT NULL,
    detail                 varchar(300) NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_log_admin FOREIGN KEY (admin_id) REFERENCES member (id) ON DELETE RESTRICT
);
CREATE INDEX ix_admin_log_created ON admin_log (created_at DESC, id DESC);

COMMENT ON TABLE site_notice IS '홈 위 띠 공지(049)';
COMMENT ON TABLE admin_log IS '관리자 화면에서 한 일(050)';

-- 111 후보(강성찬 개인 확장): 모임. 초대 링크로만 들어오는 비공개 글쓰기 모임, 규칙은 "N일에 1편".
CREATE TABLE writing_club (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    owner_id               bigint NOT NULL,
    name                   varchar(30) NOT NULL,
    intro                  varchar(100) NOT NULL DEFAULT '',
    every_days             int NOT NULL,
    start_date             date NOT NULL,
    end_date               date NOT NULL,
    invite_code            varchar(32) NOT NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_writing_club_invite UNIQUE (invite_code),
    CONSTRAINT fk_writing_club_owner FOREIGN KEY (owner_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_writing_club_name CHECK (length(btrim(name)) BETWEEN 1 AND 30),
    CONSTRAINT ck_writing_club_every CHECK (every_days BETWEEN 1 AND 30),
    CONSTRAINT ck_writing_club_period CHECK (end_date >= start_date)
);

CREATE TABLE writing_club_member (
    club_id                bigint NOT NULL,
    member_id              bigint NOT NULL,
    joined_at              timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (club_id, member_id),
    CONSTRAINT fk_writing_club_member_club FOREIGN KEY (club_id) REFERENCES writing_club (id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_club_member_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_writing_club_member_member ON writing_club_member (member_id);

COMMENT ON TABLE writing_club IS '모임(111 후보): 초대 링크로만 참여, 기간 안 전체 공개 글을 N일마다 1편으로 센다';
COMMENT ON TABLE writing_club_member IS '모임 멤버(만든 사람 포함)';

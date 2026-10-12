-- 057 오늘의 미션(강성찬 개인 확장): 날마다 주제 3개, 그 주제로 쓴 글. 034의 mission·mission_participant는 그대로 둔다(더 쓰지 않음).
CREATE TABLE daily_topic (
    id                     bigint GENERATED ALWAYS AS IDENTITY,
    day                    date NOT NULL,
    slot                   smallint NOT NULL,
    title                  varchar(60) NOT NULL,
    hint                   varchar(200) NOT NULL DEFAULT '',
    source                 varchar(10) NOT NULL DEFAULT 'POOL',
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_daily_topic UNIQUE (day, slot),
    CONSTRAINT ck_daily_topic_slot CHECK (slot BETWEEN 1 AND 5),
    CONSTRAINT ck_daily_topic_source CHECK (source IN ('AI', 'POOL')),
    CONSTRAINT ck_daily_topic_title CHECK (length(btrim(title)) BETWEEN 1 AND 60)
);

CREATE TABLE mission_post (
    post_id                bigint NOT NULL,
    topic_id               bigint NOT NULL,
    member_id              bigint NOT NULL,
    completed_at           timestamptz NULL,
    created_at             timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_mission_post_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_mission_post_topic FOREIGN KEY (topic_id) REFERENCES daily_topic (id) ON DELETE CASCADE,
    CONSTRAINT fk_mission_post_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
CREATE INDEX ix_mission_post_topic ON mission_post (topic_id, completed_at);
CREATE INDEX ix_mission_post_member ON mission_post (member_id, completed_at);

-- 친구에게 "오늘의 미션 글" 알림
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'FOLLOW', 'NEW_POST',
    'REPORT_RESOLVED', 'CONTENT_HIDDEN', 'FRIEND_REQUEST', 'FIRST_PUBLIC', 'MISSION_DONE'));

COMMENT ON TABLE daily_topic IS '오늘의 미션 주제(057), 날마다 3개, AI 또는 내장 주제';
COMMENT ON TABLE mission_post IS '미션 주제로 쓴 글(057), completed_at = 처음 발행한 때';

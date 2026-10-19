-- 113 강성찬 개인 확장: 오늘의 미션 글이 발행되면 들어가는 글쓴이별 '미션 시리즈'(이름은 바꿔도 이 표시로 찾는다). 한 사람에 하나.
ALTER TABLE series ADD COLUMN mission BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_series_mission_member ON series (member_id) WHERE mission;

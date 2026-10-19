-- 112 강성찬 개인 확장: 편 번호를 0편부터 세는 시리즈(00 강의처럼). 기본은 1편부터라 지금 시리즈는 그대로.
ALTER TABLE series ADD COLUMN start_zero BOOLEAN NOT NULL DEFAULT FALSE;

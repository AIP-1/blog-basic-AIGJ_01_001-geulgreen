-- 강성찬 개인 확장: 닉네임 최대 길이 10 → 30(영문 닉네임이 칸이 모자람, 인스타 이름 칸처럼). 넓히기만 해서 기존 값은 그대로.
ALTER TABLE member DROP CONSTRAINT ck_member_nickname;
ALTER TABLE member ALTER COLUMN nickname TYPE varchar(30);
ALTER TABLE member ADD CONSTRAINT ck_member_nickname CHECK (nickname ~ '^[가-힣a-zA-Z0-9]{2,30}$' AND nickname ~ '[가-힣a-zA-Z]');

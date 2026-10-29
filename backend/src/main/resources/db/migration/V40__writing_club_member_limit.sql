-- 10-28 개인 확장: 함께 쓰기 방은 기본으로 인원 제한이 없다. 방장이 켤 때만 member_cap에 숫자가 들어간다(NULL = 제한 없음).
-- 지금 방들의 정원은 그대로 둔다(방장이 방 설정에서 끌 수 있음). 지우는 칸·줄은 없다.
ALTER TABLE writing_club ALTER COLUMN member_cap DROP NOT NULL;
ALTER TABLE writing_club ALTER COLUMN member_cap DROP DEFAULT;
ALTER TABLE writing_club DROP CONSTRAINT ck_writing_club_cap;
ALTER TABLE writing_club ADD CONSTRAINT ck_writing_club_cap CHECK (member_cap IS NULL OR member_cap BETWEEN 2 AND 1000);
COMMENT ON COLUMN writing_club.member_cap IS '멤버 상한(만든 사람 포함). NULL이면 제한 없음(10-28부터 기본). 지금 멤버보다 적게 정하면 있는 멤버는 그대로, 새로 못 들어옴';

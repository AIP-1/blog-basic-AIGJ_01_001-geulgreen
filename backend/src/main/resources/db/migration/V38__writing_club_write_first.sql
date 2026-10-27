-- 10-26 강성찬 개인 확장: 함께 쓰기 "먼저 쓰고 보기". 방 글감으로 쓴 친구 글은 내가 그 글감으로 쓰기 전까지 방 화면에서 흐리게 보인다
-- (글감을 올리고 하루가 지나면 모두 열림, 글 화면은 언제나 열림). 만든 사람이 방 설정에서 끌 수 있다. 더하기만, 지우는 칸은 없다.
ALTER TABLE writing_club ADD COLUMN write_first boolean NOT NULL DEFAULT true;
COMMENT ON COLUMN writing_club.write_first IS '먼저 쓰고 보기: 켜면 방 글감 글을 내가 그 글감으로 써야(또는 하루 지나야) 방 화면에서 열림';

-- 103 강성찬 개인 확장: 목록 카드 요약을 600자까지(사진 카드 옆 글 카드의 빈 칸을 요약으로 채움). 넓히기만 한다.
-- 기존 글은 render_version 4로 올라간 다시 렌더링(RerenderJob)이 새 요약으로 채운다.
ALTER TABLE post ALTER COLUMN excerpt TYPE varchar(600);

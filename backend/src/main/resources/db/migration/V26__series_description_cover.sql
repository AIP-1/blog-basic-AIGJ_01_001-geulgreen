-- 107 강성찬 개인 확장: 시리즈 설명 한 줄과 대표 사진(비면 화면이 1편부터 첫 사진을 쓴다)
ALTER TABLE series ADD COLUMN description VARCHAR(100);
ALTER TABLE series ADD COLUMN cover_url VARCHAR(500);

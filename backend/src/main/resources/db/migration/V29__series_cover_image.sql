-- 111 강성찬 개인 확장: 시리즈 대표 사진을 따로 올린 사진(또는 썸네일 만들기)으로. 사진 정리 작업이 지우지 않게 사진 번호를 둔다.
ALTER TABLE series ADD COLUMN cover_image_id BIGINT;

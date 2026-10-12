# Feature Specification: 카테고리

**Feature Branch**: `037-categories` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 내 글 분류 (P1)
글쓴이는 설정에서 카테고리를 만들고(이름 1~30자, 20개까지) 순서·이름을 바꾸고 지운다. 글 화면에서 내 글 하나에 카테고리 하나를 고른다.
### User Story 2 - 카테고리로 보기 (P1)
블로그 첫 화면 위에 카테고리 목록(글 수 포함)이 보이고, 누르면 `/@주소?category=번호`로 그 카테고리 공개 글만 보인다.

## Requirements
- **FR-001** 카테고리는 블로그 주인만 만든다. 같은 이름은 한 블로그에 하나.
- **FR-002** 글은 카테고리 0~1개. 카테고리를 지우면 글은 "분류 없음"이 된다(글은 그대로).
- **FR-003** 카테고리 글 수·목록은 공용 목록 조건(012)으로 센다.
- **FR-004** 설정 `blog.categories.enabled`, 개수 `max-per-blog`(20).

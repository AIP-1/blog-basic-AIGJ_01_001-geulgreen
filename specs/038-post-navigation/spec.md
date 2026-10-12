# Feature Specification: 이전 글·다음 글 + 관련 글

**Feature Branch**: `038-post-navigation` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 이어 읽기 (P1)
공개 글 화면 아래에 같은 블로그의 이전 글·다음 글(공개 시각 기준)과, 같은 태그를 가진 다른 공개 글 3개("관련 글")가 보인다.

## Requirements
- **FR-001** 이전·다음은 같은 글쓴이의 공용 목록 조건 글 중 최초 공개 시각 바로 앞·뒤.
- **FR-002** 관련 글은 이 글 태그와 겹치는 태그 수가 많은 순, 같으면 최신순, 이 글 제외, 3개.
- **FR-003** 비공개·친구·링크·그룹 글 화면에는 보이지 않는다(목록 노출 규칙 그대로).
- **FR-004** 설정 `blog.post-navigation.enabled`, `related-count`(3).

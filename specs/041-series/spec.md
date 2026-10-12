# Feature Specification: 시리즈

**Feature Branch**: `041-series` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 글 묶기 (P1)
글쓴이는 글 화면에서 내 글을 시리즈(이름 1~60자)에 넣는다. 새 시리즈 이름을 쓰면 만들어진다. 시리즈 안 순서는 넣은 순서.
### User Story 2 - 시리즈 따라 읽기 (P1)
시리즈에 든 공개 글 화면 위에 "시리즈 이름 (n/전체)"와 목록 펼치기, 이전편·다음편이 보인다. `/@주소/series/번호`에서 전체 목록.

## Requirements
- **FR-001** 글은 시리즈 0~1개. 빼면 목록에서 빠지고, 남은 글이 없으면 시리즈도 지워진다.
- **FR-002** 목록·편 번호는 공용 목록 조건 글만 센다.
- **FR-003** 설정 `blog.series.enabled`.

# Feature Specification: 목차 자동 생성

**Feature Branch**: `039-toc` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 긴 글 훑기 (P1)
본문에 소제목(##·###)이 2개 이상이면 글 위에 "목차"가 생기고, 누르면 그 소제목으로 이동한다. 넓은 화면에서는 오른쪽에 붙어 따라온다.

## Requirements
- **FR-001** 정화된 본문(007)은 바꾸지 않는다. 화면 스크립트가 h2·h3에 id를 붙이고 목차를 만든다. 스크립트가 없으면 목차도 없다.
- **FR-002** 소제목이 2개 미만이면 목차 없음.
- **FR-003** 설정 `blog.toc.enabled`.

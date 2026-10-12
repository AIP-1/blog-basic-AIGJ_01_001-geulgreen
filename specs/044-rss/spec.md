# Feature Specification: RSS 구독

**Feature Branch**: `044-rss` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 구독기로 받기 (P1)
`/@주소/rss`는 그 블로그 공개 글 최신 20개를 RSS 2.0으로, `/rss`는 전체 공개 글 최신 20개를 낸다. 블로그 머리에 RSS 링크.

## Requirements
- **FR-001** 공용 목록 조건 글만. 본문 대신 요약(excerpt)·링크·공개 시각.
- **FR-002** 글자는 XML로 이스케이프한다. 없는 블로그는 404.
- **FR-003** 설정 `blog.rss.enabled`, `size`(20).

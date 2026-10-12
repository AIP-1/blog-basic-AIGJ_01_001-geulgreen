# Feature Specification: 관리 대시보드

**Feature Branch**: `045-admin-dashboard` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (10-12 chan 요청 "관리자 화면 추가"). 공통 022 신고·정지 화면 위에 더한다. 관리자가 아니면 404(42 P-10).

## Requirements
- **FR-001** `/admin`: 전체 회원·발행 글·댓글 수와 오늘 늘어난 수, 처리할 신고 수(있으면 강조), 최근 7일 가입·글·댓글 막대와 같은 내용의 표. 머리말 [관리]는 여기로.
- **FR-002** 바꾸는 동작은 관리 기록에 남긴다(050).

# Feature Specification: 방명록 관리

**Feature Branch**: `048-admin-guestbook` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (10-12 chan 요청 "관리자 화면 추가"). 공통 022 신고·정지 화면 위에 더한다. 관리자가 아니면 404(42 P-10).

## Requirements
- **FR-001** `/admin/guestbook`: 모든 블로그의 최근 방명록 100개(비밀글 포함)를 보고 지운다.
- **FR-002** 바꾸는 동작은 관리 기록에 남긴다(050).

# Feature Specification: 글·댓글 관리

**Feature Branch**: `047-admin-content` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (10-12 chan 요청 "관리자 화면 추가"). 공통 022 신고·정지 화면 위에 더한다. 관리자가 아니면 404(42 P-10).

## Requirements
- **FR-001** `/admin/content`: 발행 글·댓글을 제목·내용·닉네임·주소로 찾고, 신고 없이도 사유(022 사유 목록)를 골라 숨기기·숨김 해제. 댓글 숨김은 댓글 수에서 빼고 해제하면 되돌린다. 숨기면 022와 같은 알림 사건.
- **FR-002** 바꾸는 동작은 관리 기록에 남긴다(050).

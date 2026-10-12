# Feature Specification: 사이트 공지

**Feature Branch**: `049-site-notice` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (10-12 chan 요청 "관리자 화면 추가"). 공통 022 신고·정지 화면 위에 더한다. 관리자가 아니면 404(42 P-10).

## Requirements
- **FR-001** `/admin/notices`: 200자 공지와 선택 링크(/로 시작하거나 https만). 켜진 공지는 하나, 모든 화면 머리말 아래 띠로. 끄기.
- **FR-002** 바꾸는 동작은 관리 기록에 남긴다(050).

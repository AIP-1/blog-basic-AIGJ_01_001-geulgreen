# Feature Specification: 관리 기록과 관리자 지정

**Feature Branch**: `050-admin-log` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (10-12 chan 요청 "관리자 화면 추가"). 공통 022 신고·정지 화면 위에 더한다. 관리자가 아니면 404(42 P-10).

## Requirements
- **FR-001** `/admin/logs`: 이 화면에서 한 일(admin_log) + 신고 처리 + 회원 정지를 시각·관리자·한 일·대상·내용으로 최신 100개. 관리자 지정: 설정 `blog.admin.emails`(서버 `ADMIN_EMAILS`)의 이메일로 가입·인증한 계정을 1분마다 관리자로 올린다(소셜은 서비스가 알려 준 이메일). 지정된 뒤 다시 로그인해야 권한이 반영된다.
- **FR-002** 바꾸는 동작은 관리 기록에 남긴다(050).

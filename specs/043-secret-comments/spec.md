# Feature Specification: 비밀 댓글

**Feature Branch**: `043-secret-comments` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 글쓴이에게만 (P1)
댓글 칸의 "비밀 댓글"을 고르면 그 댓글은 쓴 사람과 글쓴이만 본다. 다른 사람에게는 "비밀 댓글이에요"로 보인다. 비밀 댓글에 단 답글도 비밀이다.

## Requirements
- **FR-001** 내용은 서버에서 가린다(화면·API 모두). 답글 대상 사람도 볼 수 있다.
- **FR-002** 댓글 수에는 그대로 센다.
- **FR-003** 설정 `blog.comments.secret-enabled`.

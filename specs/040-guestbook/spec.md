# Feature Specification: 방명록

**Feature Branch**: `040-guestbook` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (티스토리 참고, 10-12 chan 요청 "다 해보자"). 공통 완료 기준(001~024)은 그대로 지킨다. 끄는 설정 있음.

## User Scenarios & Testing
### User Story 1 - 블로그에 인사 남기기 (P1)
`/@주소/guestbook`에서 로그인한 회원이 300자 글자로 인사를 남긴다. "비밀글"을 고르면 블로그 주인과 쓴 사람만 본다.
### User Story 2 - 정리 (P2)
쓴 사람과 블로그 주인은 지울 수 있다.

## Requirements
- **FR-001** 이메일 인증한 회원만 쓴다(공통 쓰기 규칙). 한 시간 20개.
- **FR-002** 글자로만 보인다(서식·링크 없음). 비어 있거나 300자 넘으면 400.
- **FR-003** 탈퇴하면 그 사람이 쓴 것과 그 블로그 방명록이 지워진다.
- **FR-004** 설정 `blog.guestbook.enabled`.

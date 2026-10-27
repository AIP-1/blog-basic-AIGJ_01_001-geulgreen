# 릴리스 노트

이 저장소는 두 단계로 나뉜다.

- **v1.0.0 — 팀 공통 최소 요구사항**: 팀 3명이 함께 정한 공통 기능(docs/01 Tier A·B·C)을 명세 001~024로 옮겨 모두 구현한 버전.
- **v1.0.0 이후 — 개인 확장**: 팀은 공통 최소 요구사항만 맞추고, 그 위에 각자 **추가만** 하기로 했다(문서 저장소 docs/01 §2-4, 원칙 2: 개인 확장을 켜도 공통 완료 기준은 그대로 지킨다). 명세 025부터는 개인 확장이다(1.1.0: 025~036, 1.2.0: 037~044).

---

## [1.79.6] — 2026-10-27 · 개인 확장 (인스타그램 링크)

- 사이트 맨 아래·휴대폰 메뉴에 글그린 인스타그램 링크
- DB 바뀜 없음

#### 전/후 사진

**사이트 맨 아래**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.6/v1796-foot-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.6/v1796-foot-after.png) |

*휴대폰 메뉴*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.6/v1796-menu-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.6/v1796-menu-m-after.png) |

---

## [1.79.5] — 2026-10-26 · 개인 확장 (버튼 높이)

- 버튼·알약 버튼 높이를 사이트 전체에서 두 단계(보통 38px, 작게 32px)로 맞춤
- DB 바뀜 없음

#### 전/후 사진

**함께 쓰기 빈 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.5/v1795-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.5/v1795-club-after.png) |

**블로그 홈**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.5/v1795-blog-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.5/v1795-blog-after.png) |

---

## [1.79.4] — 2026-10-26 · 개인 확장 (피드 얼굴 줄 · 연속 글 접기)

- 피드 얼굴 줄: 최근 글쓴이 20명, 더 있으면 +N·전체 N명
- 한 사람이 세 편 넘게 이어 쓴 글은 접기(펼친 묶음 뒤 연속 글도 접기), 키보드 초점 테두리
- DB 바뀜 없음

---

## [1.79.3] — 2026-10-26 · 개인 확장 (탐색 시리즈 목록 · 문의 대화 · 예약 발행 · 링크 모양)

- 탐색 시리즈 탭: 한 줄 목록, 앞 세 편 제목
- 문의 대화: 관리자·보낸 회원이 사이트 안에서 여러 번 주고받기, 다시 답하면 관리자 알림
- 예약 발행: 스위치·카드, 빠른 고르기 버튼, 예약하면 창 닫힘
- 링크 모양: 떨어진 링크는 알약 버튼, 안내 상자는 글자만, 문장 속 링크는 굵은 글자
- DB: V37 문의 대화 표 추가, 예전 답장은 대화의 첫 답장으로 옮김(더하기만)

#### 전/후 사진

**탐색 시리즈는 목록으로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-sr-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-sr-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-sr-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-sr-m-after.png) |

**문의를 사이트 안에서 주고받아요**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-mine-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-mine-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-mine-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-mine-m-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-admin-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-admin-after.png) |

**예약 발행 다듬기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-sched-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-sched-after.png) |

**밑줄 링크 대신 알약 버튼**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-save-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-save-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-login-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-login-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-stats-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.3/v1793-stats-after.png) |

---

## [1.79.2] — 2026-10-26 · 개인 확장 (함께 쓰기 목록 · 홈 사진)

- 함께 쓰기 목록: 공개방·비공개방 카드, [방 만들기]는 창으로, 링크 모양 정리
- 휴대폰 홈 맨 위 사진 둥근 모서리(PC 그대로)
- DB 바뀜 없음

#### 전/후 사진

**함께 쓰기 목록**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-clubs-new-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-clubs-new-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-clubs-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-clubs-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-clubs-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-clubs-m-after.png) |

**휴대폰 홈 사진 둥근 모서리**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.2/v1792-home-m-after.png) |

---

## [1.79.1] — 2026-10-26 · 개인 확장 (친구 초대 링크 · 간단 가입)

- 초대 링크 `/i/주소[/방 코드]`: 가입하면 바로 팔로우·방 참여, 휴대폰 공유
- 간단 가입: 가입 칸 줄이기, 약관 모두 동의 시 한 줄로 접기
- README 기능 표: `blog.invite.enabled`, `blog.account.signup.simple`
- DB 바뀜 없음

#### 전/후 사진

**함께 쓰기 방 초대 칸**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.1/v1791-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.1/v1791-club-after.png) |

**초대 링크 화면(새 화면)**

![](https://github.com/practicech/blog/releases/download/v1.79.1/v1791-invite.png)

---

## [1.79.0] — 2026-10-26 · 개인 확장 (함께 쓰기 오픈채팅방 · 탐색 오른쪽 칸 · 설정 하위 화면)

- 함께 쓰기 방: 기간 없이 오픈채팅방처럼, 멤버 글이 대화처럼, 방 글감 고정, 방 목록에 새 글 수
- 탐색 추천 오른쪽 칸이 스크롤을 따라옴(PC)
- 설정 하위 화면(비공개 계정·친한 친구·카테고리·연결·탈퇴) 카드형
- DB: V36 방 글감 표·멤버 마지막 본 때 칸 추가(더하기만, 기간 칸은 남기고 안 씀)

#### 전/후 사진

**함께 쓰기 방이 오픈채팅방처럼**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-club-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-club-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-club-m-after.png) |

**탐색 오른쪽 칸이 따라와요**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-ex-after.png) |

**설정 하위 화면도 카드형**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-scat-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-scat-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-spriv-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-spriv-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-sfr-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-sfr-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-sconn-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-sconn-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-swd-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-swd-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-scat-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.79.0/v1790-scat-m-after.png) |

---

## [1.78.2] — 2026-10-25 · 개인 확장 (친한 친구)

- "친구"를 "친한 친구"로, 설정에서 팔로우하는 사람 중에 고르기
- 남의 블로그 머리에는 팔로우만([친구로 넣기] 뺌)
- DB 바뀜 없음

#### 전/후 사진

**남의 블로그에는 팔로우만**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.2/v1782-blog-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.2/v1782-blog-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.2/v1782-blog-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.2/v1782-blog-m-after.png) |

**친구는 "친한 친구"로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.2/v1782-fr-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.2/v1782-fr-after.png) |

---

## [1.78.1] — 2026-10-25 · 개인 확장 (알림 바로 저장 고침)

- 설정 알림 바로 저장: 로그인이 풀려 로그인 화면으로 돌아가면 저장 실패로 보고 [저장] 다시 보이기
- 화면 바뀜 없음, DB 바뀜 없음

---

## [1.78.0] — 2026-10-25 · 개인 확장 (설정 새 디자인 · 피드·탐색 다듬기)

- 설정: 기능별 탭과 카드형 화면, 알림 스위치는 누르면 바로 저장
- 피드: 글 읽기 쉽게, 썸네일 잘리지 않게
- 탐색 추천: 많이 읽은 글·사람·태그를 오른쪽 칸에
- 탐색 시리즈: 큰 표지 한 줄 3개
- DB 바뀜 없음

#### 전/후 사진

**설정 새 디자인**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-set-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-set-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-noti-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-noti-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-set-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-set-m-after.png) |

**피드**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-feed-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-feed-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-feed-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-feed-m-after.png) |

**탐색 추천**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-ex-after.png) |

**탐색 시리즈**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-sr-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.78.0/v1780-sr-after.png) |

---

## [1.77.3] — 2026-10-25 · 개인 확장 (함께 쓰기 첫 화면)

- 함께 쓰기 첫 화면: 세 단계 가운데 정렬, 안내 문구 짧게
- DB 바뀜 없음

#### 전/후 사진

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.3/v1773-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.3/v1773-club-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.77.3/v1773-club-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.3/v1773-club-m-after.png) |

---

## [1.77.2] — 2026-10-25 · 개인 확장 (설정 AI·연결 탭 합치기)

- 설정 [연결] 탭을 [AI] 탭 안으로(옛 #settings-connections 주소도 같은 곳)
- DB 바뀜 없음

#### 전/후 사진

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.2/v1772-ai-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.2/v1772-ai-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.77.2/v1772-set-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.2/v1772-set-m-after.png) |

---

## [1.77.1] — 2026-10-25 · 개인 확장 (위 메뉴 줄이기)

- 위 메뉴 5칸(홈·탐색·피드·글감·함께 쓰기)
- 태그는 탐색·검색에서, 관리는 프로필 메뉴에서
- DB 바뀜 없음

#### 전/후 사진

**위 메뉴**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.1/v1771-nav-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.1/v1771-nav-after.png) |

**탐색**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.1/v1771-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.1/v1771-ex-after.png) |

**휴대폰 메뉴**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.1/v1771-menu-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.1/v1771-menu-m-after.png) |

---

## [1.77.0] — 2026-10-25 · 개인 확장 (글 저장·모아보기 · 주소 변경 알림)

- 글 저장(북마크): 글 화면 [저장], 나만 보고 글쓴이에게 알리지 않음
- 모아보기 /me/liked·/me/saved: 좋아요한 글·저장한 글 탭
- 블로그 주소를 바꾸면 팔로워에게 HANDLE_CHANGED 알림(바꾸기 전 주소 표시)
- DB: V34 notification.old_handle 열·알림 종류 추가, V35 post_bookmark 표(더하기만)

#### 전/후 사진

**글 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.0/v1770-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.0/v1770-post-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.77.0/v1770-post-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.0/v1770-post-m-after.png) |

**새 모아보기 화면**

| PC | 휴대폰 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.77.0/v1770-me.png) | ![](https://github.com/practicech/blog/releases/download/v1.77.0/v1770-me-m.png) |

---

## [1.76.0] — 2026-10-25 · 개인 확장 (피드·탐색 나누기 · 글감별 글)

- 피드: 팔로우한 사람 새 글을 한 줄 목록으로, 위에 팔로우한 사람
- 탐색 추천 탭: 많이 읽은 글 1~5위·사람·태그 구역으로 나눠 홈과 다르게
- 오늘의 글감: 글감을 골라 그 글감으로 쓴 글만 보기
- DB 바뀜 없음

#### 전/후 사진

**피드는 친구 소식 한 줄 목록**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-feed-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-feed-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-feed-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-feed-m-after.png) |

**탐색 추천 탭은 홈과 다르게**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-ex-after.png) |

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-ex-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-ex-m-after.png) |

**글감별 글 모아보기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-gg-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.76.0/v1760-gg-after.png) |

---

## [1.75.1] — 2026-10-23 · 개인 확장 (UX 다듬기 · 기록 한눈에)

- 휴대폰 기록(잔디): 500px 아래 최근 26주, 640px 아래 39주만 보여 한 화면에
- ☰ 테두리 제거·머리줄 아이콘 정렬, 메뉴·꼬리말 약관 줄
- 홈·목록 한 줄 행, 글감 카드 단추, 탐색 카드를 홈 카드와 같게
- 글 화면 좋아요 선 하트, 설정 탭 한 줄, 비공개 계정 스위치, 휴대폰 알림 토스트
- 홈은 뒤로 가기로 돌아올 때만 스크롤 위치 복원
- 검색 결과 요약 다듬기
- DB 바뀜 없음

#### 전/후 사진(휴대폰 390)

**기록(잔디) 한눈에**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-rec-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-rec-after.png) |

**머리줄과 메뉴**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-menu-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-menu-after.png) |

**홈**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-home-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-home-after.png) |

**글감**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-gg-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-gg-after.png) |

**탐색**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-ex-after.png) |

**글 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-post-after.png) |

**설정**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-set-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-set-after.png) |

**비공개 계정**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-priv-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.1/v1751-priv-after.png) |

---

## [1.75.0] — 2026-10-22 · 개인 확장 (홈 화면 설치)

- 휴대폰 홈 화면에 글그린 설치(PWA): /manifest.webmanifest, 발바닥 아이콘, theme-color
- 서비스 워커 /sw.js: 페이지는 늘 네트워크, 끊기면 /offline.html 안내와 [다시 시도]
- DB 바뀜 없음

#### 새 화면 사진(전에는 없던 기능)

| 설치한 앱 | 연결 끊김 | 연결 끊김(어두운 테마) |
|---|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.75.0/v1750-standalone.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.0/v1750-offline.png) | ![](https://github.com/practicech/blog/releases/download/v1.75.0/v1750-offline-dark.png) |

---

## [1.74.2] — 2026-10-22 · 개인 확장 (화면 정렬)

- 스크롤바가 있든 없든 화면이 옆으로 밀리지 않음(scrollbar-gutter: stable)
- 목록 화면 제목 시작 높이 통일, 글감 제목 32px
- 함께 쓰기에 화면 제목(탭은 그 아래)
- DB 바뀜 없음

#### 전/후 사진

**함께 쓰기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-club-after.png) |

**탐색**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-ex-after.png) |

**태그**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-tags-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-tags-after.png) |

**피드**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-feed-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-feed-after.png) |

**글감**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-gg-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.2/v1742-gg-after.png) |

---

## [1.74.1] — 2026-10-22 · 개인 확장 (휴대폰 가독성)

- 태그 화면 머리가 좁은 화면 밖으로 넘치던 것 고침
- 탐색 카드(휴대폰): 태그 칩 빼고 글쓴이·시리즈 글자 키움
- 시리즈 화면(휴대폰): 표지가 없을 때 빈 칸 없앰
- 휴대폰 누르는 자리: ☰·글쓰기 사진/표 버튼 44px, 바닥 링크 14px
- DB 바뀜 없음

#### 전/후 사진(휴대폰 390)

**탐색 카드(휴대폰)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-ex-after.png) |

**시리즈 화면(휴대폰)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-sr-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-sr-after.png) |

**태그 화면 머리와 ☰ 버튼**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-tags-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-tags-after.png) |

**글쓰기 사진·표 버튼**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-ed-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-ed-after.png) |

**바닥 링크**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-ft-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.1/v1741-ft-after.png) |

---

## [1.74.0] — 2026-10-22 · 개인 확장 (가입 버튼 · 글 구조화 데이터)

- 로그아웃 홈 소개 줄에 [가입하고 첫 글 쓰기]
- 휴대폰 머리줄에 로그아웃일 때 [가입하기]
- 글 화면 JSON-LD(BlogPosting + BreadcrumbList: 글그린 > 블로그 > 시리즈 > 글)
- DB 바뀜 없음

#### 전/후 사진

**홈(PC)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.0/v1740-home-1440-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.0/v1740-home-1440-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.74.0/v1740-home-390-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.74.0/v1740-home-390-after.png) |

---

## [1.73.0] — 2026-10-22 · 개인 확장 (UX 개선 7건)

- 머리줄 해/달 테마 버튼 없앰(테마는 설정에서), 휴대폰 머리줄에 펜 [새 글] 버튼
- 글 화면: [신고] 흐린 글자로 오른쪽 끝, 조회 수는 작성자 줄 날짜 뒤
- 문구: 미션·주제 → 글감, 공지 띠 이모지 → 확성기 아이콘
- 글 카드: 태그 한 색, 작성자·태그 누르는 영역 44px, 좋아요 하트 아이콘(누른 글은 채움)
- 글쓰기: [변경 취소]를 [← 나가기] 옆 흐린 글자 버튼으로
- 1200px 이상 글 화면: 좋아요·공유를 본문 왼쪽 떠 있는 기둥으로
- 홈: 자동 불러오기 2번 뒤 [더 보기], 꼬리말 다시 보임
- DB 바뀜 없음

#### 전/후 사진

**휴대폰 머리줄에 [새 글] 펜**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-home-m-after.png) |

**글 화면 반응 줄**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-post-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-post-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-post-m-after.png) |

**넓은 화면 좋아요·공유 기둥**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-post-w-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-post-w-after.png) |

**글 카드**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-home-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-home-after.png) |

**홈 끝과 꼬리말**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-end-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-end-after.png) |

**글쓰기 [변경 취소]**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-edit-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.73.0/v1730-edit-after.png) |

---

## [1.72.1] — 2026-10-21 · 개인 확장 (옛 블로그 주소 넘김)

- 블로그 주소를 바꾼 뒤 옛 주소로 들어오면 301(영구 이동)로 새 주소에 넘김(전에는 302)
- 설정 `permanent-holds`에 적은 옛 주소는 잡아 두는 기간이 끝나지 않아 다른 회원이 가져갈 수 없음(관리자 블로그 주소). 지금 쓰는 동안은 영향 없음
- 화면 바뀜 없음, DB 바뀜 없음

---

## [1.72.0] — 2026-10-21 · 개인 확장 (탐색 카드)

- 탐색 추천·트렌딩: PC 한 줄 4장(좁으면 3장, 오른쪽 칸이 비어도 6장으로 늘지 않음)·폰 2장, 제목 크게 두 줄까지·글쓴이 줄 작게
- 시리즈에 든 글 카드에 "시리즈 · 시리즈 이름"(GET /api/series/titles, 공개 글만)
- DB 바뀜 없음

#### 전/후 사진

**PC(1440)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.72.0/v1720-ex-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.72.0/v1720-ex-after.png) |

**넓은 화면(1920)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.72.0/v1720-ex-w-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.72.0/v1720-ex-w-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.72.0/v1720-ex-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.72.0/v1720-ex-m-after.png) |

---

## [1.71.0] — 2026-10-21 · 개인 확장 (글감 화면 디자인 · 알림 모두 읽음)

- 글감 화면 새 디자인: 제목·숫자 한 번씩, 선 아이콘 카드, 배지 이름 "첫 글감"·"글감 10번/30번/100번"
- 알림 종: 펼치기만 해서는 읽음 처리하지 않고, 머리줄의 [모두 읽음] 버튼으로 한 번에 읽음
- DB 바뀜 없음

#### 전/후 사진

**글감 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.71.0/v1710-gg-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.71.0/v1710-gg-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.71.0/v1710-gg-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.71.0/v1710-gg-m-after.png) |

**알림 종**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.71.0/v1710-bell-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.71.0/v1710-bell-after.png) |

---

## [1.70.1] — 2026-10-21 · 개인 확장 (설정 > 연결 키 보기 · 만 14세 미만 안내)

- 설정 > 연결 칸에 키 이름·마지막 사용, 키가 있으면 버튼 "키 관리"
- 연결 화면 Claude Code 명령에 --scope user(한 번 실행하면 모든 폴더에서)
- 개인정보 처리방침에 만 14세 미만 칸(보호자 동의·권리), 보관 기간에 동의 대기 가입, 이용약관 2. 계정에 보호자 동의
- DB 바뀜 없음

#### 전/후 사진

**설정 > 연결**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-set-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-set-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-set-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-set-m-after.png) |

**연결 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-conn-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-conn-after.png) |

**개인정보 처리방침**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-privacy-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-privacy-after.png) |

**이용약관**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-terms-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.1/v1701-terms-after.png) |

---

## [1.70.0] — 2026-10-21 · 개인 확장 (오늘의 글감, 함께 쓰기 개편, 14세 미만 보호자 동의)

- 머리글 "미션"을 "글감"·"함께 쓰기"로 나눔
- 오늘의 글감: 글감 셋 중 골라 쓰기, 글쓰기 화면에 오늘의 글감, 저녁 글감 메일(켠 사람만, 안 쓴 날만)
- 함께 쓰기: "참여 중인 방"·"방 찾기" 탭, 처음 안내, 방 만들기·글 쓰러 가기 403 고침(csrf)
- 가입 때 나이 확인, 만 14세 미만은 보호자 메일 동의 후 계정 열림(5일 안에 없으면 지움)
- DB: V32 prompt_mail, V33 동의 종류 AGE14·GUARDIAN과 guardian_consent(더하기만)

#### 전/후 사진

**머리글 메뉴**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-head-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-head-after.png) |

**오늘의 글감**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-missions-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-missions-after.png) |

**함께 쓰기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-clubs-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-clubs-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-clubs-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-clubs-m-after.png) |

**가입 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-signup-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.70.0/v1700-signup-after.png) |

---

## [1.69.7] — 2026-10-21 · 개인 확장 (휴대폰 홈 카드 구분선)

- 휴대폰 홈 한 줄 카드: 사진 아래와 글 설명 아래에 얇은 구분선(탐색·PC는 그대로)
- DB 바뀜 없음

#### 전/후 사진

**휴대폰 홈(밝은 화면)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.7/v1697-home-m-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.7/v1697-home-m-light-after.png) |

**휴대폰 홈(어두운 화면)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.7/v1697-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.7/v1697-home-m-after.png) |

---

## [1.69.6] — 2026-10-21 · 개인 확장 (확인 팝업을 사이트 창으로)

- 브라우저 기본 confirm 13군데(발행, 휴지통, 영구 삭제, 전체 공개, 댓글 삭제, 시리즈 지우기 등)를 사이트 확인 창으로, 하는 일이 적힌 버튼만 진행, [아니요]·Esc·바깥은 취소
- DB 바뀜 없음

#### 전/후 사진

*바뀐 뒤(전은 브라우저 기본 팝업이라 페이지 사진에 안 찍힘)*

![](https://github.com/practicech/blog/releases/download/v1.69.6/v1696-ask.png)

![](https://github.com/practicech/blog/releases/download/v1.69.6/v1696-ask-m.png)

---

## [1.69.5] — 2026-10-21 · 개인 확장 (시리즈 추가 버튼, 탭 줄 스크롤바)

- 내 블로그 시리즈 탭 줄 끝에 [시리즈 추가](주인에게만, 내 글 > 시리즈의 새 시리즈 만들기 칸을 연 채로)
- 블로그 홈·글·시리즈 탭 줄의 세로 스크롤바 없앰
- DB 바뀜 없음

#### 전/후 사진

**블로그 시리즈 탭(주인)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.5/v1695-stab-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.5/v1695-stab-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.5/v1695-stab-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.5/v1695-stab-m-after.png) |

---

## [1.69.4] — 2026-10-20 · 개인 확장 (휴대폰 홈 카드 상자 빼기)

- 휴대폰 홈 한 줄 카드: 테두리·그림자·배경 상자 없이 사진은 화면 끝까지, 카드 사이는 여백으로(탐색 카드와 PC는 그대로)
- DB 바뀜 없음

#### 전/후 사진

**휴대폰 홈(어두운 화면)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.4/v1694-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.4/v1694-home-m-after.png) |

**휴대폰 홈(밝은 화면)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.4/v1694-home-m-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.4/v1694-home-m-light-after.png) |

---

## [1.69.3] — 2026-10-19 · 개인 확장 (탐색 카드 작게, 첫 댓글 칸 빼기)

- 탐색 [추천]·[트렌딩] 카드 작게: PC 한 줄 5개, 휴대폰 2개, 미리보기 글 없음, 15개씩 불러오기
- "첫 댓글을 기다리는 글" 칸과 그 글을 추천에서 빼던 규칙, 1.69.2의 칸 글로 채우던 단계를 뺌
- DB 바뀜 없음

#### 전/후 사진

**탐색 [추천]**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.3/v1693-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.3/v1693-explore-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.3/v1693-explore-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.3/v1693-explore-m-after.png) |

**탐색 [트렌딩]**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.3/v1693-trend-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.3/v1693-trend-after.png) |

---

## [1.69.2] — 2026-10-19 · 개인 확장 (탐색 추천이 비지 않게)

- 탐색 [추천]: 일반 추천 뒤에 팔로우한 사람 글(최신순), 그래도 비면 "첫 댓글을 기다리는 글" 칸 글도 목록에(내 글은 제외)
- DB 바뀜 없음

#### 전/후 사진

**탐색 [추천](모두 팔로우한 계정)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.2/v1692-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.2/v1692-explore-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.2/v1692-explore-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.2/v1692-explore-m-after.png) |

---

## [1.69.1] — 2026-10-19 · 개인 확장 (시리즈 관리 창)

- 내 글 > 시리즈: [글 넣기]·[설정]·[시리즈 화면] 버튼, 글 넣기 창(찾기·여러 개 고르기·고른 수, 휴대폰은 아래에서 올라오는 창), 설정 창(이름·설명·0편부터·대표 사진·삭제)
- DB 바뀜 없음

#### 전/후 사진

**펼친 시리즈**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.1/v1691-open-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.1/v1691-open-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.1/v1691-open-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.1/v1691-open-m-after.png) |

*글 넣기 창*

![](https://github.com/practicech/blog/releases/download/v1.69.1/v1691-add.png)

*설정 창*

![](https://github.com/practicech/blog/releases/download/v1.69.1/v1691-set.png)

---

## [1.69.0] — 2026-10-19 · 개인 확장 (시리즈 상자 글 끝으로, 미션 시리즈, 표지 16:10)

- 글 화면 시리즈 상자는 본문 끝으로, 위에는 한 줄(이름·몇 번째), 시리즈 글은 이전·다음 편만
- 오늘의 미션 글은 발행할 때 글쓴이의 "오늘의 미션" 시리즈에 저절로(한 번에 발행 포함)
- 시리즈 카드·관리 표지 칸 16:10
- DB: V31 series.mission과 한 사람에 하나 색인(더하기만)

#### 전/후 사진

**글 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-post-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-post-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-post-m-after.png) |

**내 글 > 시리즈(미션 글 발행 뒤)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-mission-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-mission-after.png) |

**블로그 시리즈 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-blogseries-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.69.0/v1690-blogseries-after.png) |

---

## [1.68.2] — 2026-10-19 · 개인 확장 (임시글 한 번에 발행, 시리즈 0편부터)

- **109 후속**: 내 글 > 임시글에서 골라 [선택한 글 발행], 여러 편이어도 팔로워 알림은 한 번
- **106 후속**: 시리즈 "0편부터 시작"(첫 글이 0편, 기본 꺼짐)
- DB: V30 series.start_zero(기본 false, 더하기만)

#### 전/후 사진

**임시글 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-drafts-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-drafts-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-drafts-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-drafts-m-after.png) |

**시리즈 설정**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-form-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-form-after.png) |

**시리즈 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-page-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-page-after.png) |

**글 화면 시리즈 상자(휴대폰)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-box-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.2/v1682-box-m-after.png) |

---

## [1.68.1] — 2026-10-19 · 개인 확장 (관리자 사진 하루 한도 없음)

- 관리자 계정은 하루 사진 200장 한도 없음(다른 회원은 200장, 1GB 저장 한도는 그대로), 스위치 IMAGE_ADMIN_DAILY_LIMIT_EXEMPT 기본 켜짐
- 화면 바뀜 없음(사진 없음), DB 바뀜 없음

---

## [1.68.0] — 2026-10-18 · 개인 확장 (시리즈 대표 사진 올리기)

- **107 후속**: 시리즈 대표 사진을 [사진 올리기]·[썸네일 만들기]로(없으면 1편부터 첫 사진), og:image도 대표 사진, 시리즈 화면 머리 링크 "닉네임님의 시리즈 모두 보기"와 16:10 사진
- DB: V29 series.cover_image_id(더하기만)

#### 전/후 사진

*대표 사진 고치기(새로 생김)*

![](https://github.com/practicech/blog/releases/download/v1.68.0/v1680-edit-after.png)

**시리즈 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.0/v1680-series-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.0/v1680-series-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.68.0/v1680-series-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.68.0/v1680-series-m-after.png) |

---

## [1.67.5] — 2026-10-18 · 개인 확장 (카테고리·태그 나누기, 태그 화면 카드)

- 카테고리는 제목 위(폴더 아이콘·회색 글자), 태그는 본문 아래 # 칩, 블로그 글 탭 카테고리 줄, 글 설정·태그 칸 설명
- 태그 화면 가로 줄 카드를 공용 카드 모양으로(사진 위, 한 줄 4개 폭)
- DB 바뀜 없음

#### 전/후 사진

**글 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-post-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-post-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-post-m-after.png) |

**블로그 글 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-blog-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-blog-after.png) |

**태그 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-tagrow-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-tagrow-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-tagrow-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.5/v1675-tagrow-m-after.png) |

---

## [1.67.4] — 2026-10-18 · 개인 확장 (첫 댓글을 기다리는 글 카드 크기)

- 탐색 "첫 댓글을 기다리는 글" 칸 카드를 공용 카드 크기·간격으로
- DB 바뀜 없음

#### 전/후 사진

**탐색 [추천]**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.4/v1674-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.4/v1674-explore-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.4/v1674-explore-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.4/v1674-explore-m-after.png) |

---

## [1.67.3] — 2026-10-18 · 개인 확장 (검색 결과 제목·설명)

- 태그·시리즈·블로그 홈마다 검색용 제목과 설명(공개 글만, 머리말만, 화면 그대로), og:title도 같은 제목
- DB 바뀜 없음

#### 전/후(머리말 문구)

| 화면 | 전 | 후 |
|---|---|---|
| 태그(#일상) 제목 | #일상 · 글그린 | #일상 글 모음 · 글그린 |
| 태그 설명 | 좋은 글이 머무는 곳, 글그린. 다양한 사람들이 전하는 일상과 개발, 그리고 생각들 | 글그린에서 '일상' 태그로 쓴 글 7편. 최근 글: 「목차 확인용 긴 글」, … |
| 시리즈 설명 | (사이트 기본 문구) | 하루의 작은 장면을 모아요(시리즈 설명) |
| 블로그 홈 제목 | writer · 글그린 | 글쓴이의 블로그 · 글그린 |
| 블로그 홈 설명 | (사이트 기본 문구) | 글쓴이님의 글그린 블로그. 최근 글: 「목차 확인용 긴 글」, … |
| 공유 미리보기 제목(og:title) | 글그린 | 위 제목과 같음 |

---

## [1.67.2] — 2026-10-18 · 개인 확장 (글 화면 미션 표시 빼기)

- 글 화면에서 "오늘의 미션" 칩과 미션 완료 상자를 뺌(태그·미션 화면·미션 기록은 그대로)
- DB 바뀜 없음

#### 전/후 사진

**글 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.2/v1672-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.2/v1672-post-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.2/v1672-post-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.2/v1672-post-m-after.png) |

---

## [1.67.1] — 2026-10-18 · 개인 확장 (고양이 기본 썸네일, 시리즈 상자, 목차 스크롤)

- 사진 없는 글 카드에 고양이 기본 썸네일(어두운 화면은 밤 사진, 화면에서만)
- 글 화면 시리즈 상자: 이름·편 수, 접힌 목록(지금 글 표시), 이전·다음 편 카드
- 목차를 누르면 부드럽게 스크롤(움직임이라 사진 없음)
- DB 바뀜 없음

#### 전/후 사진

**사진 없는 글 카드(밝은 화면)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-home-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-home-light-after.png) |

**어두운 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-home-1440-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-home-1440-dark-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-home-m-after.png) |

**글 화면 시리즈 상자**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-box-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-box-after.png) |

*목록을 펼친 모습*

![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-box-open-after.png)

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-box-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.1/v1671-box-m-after.png) |

---

## [1.67.0] — 2026-10-18 · 개인 확장 (함께 쓰기 방 종류, 공개 목록, 초대)

- **111 후속**: 방 종류(승인 후 참여·누구나 바로·링크로만, 기존 방은 링크로만), 함께 쓸 방 찾기(공개 목록·검색), 참여 요청과 승인, 관리자 역할, 팔로워·친구 초대, 정원 2~100명, 초대·참여 요청 알림
- DB: V28 함께 쓰기 방 종류·정원·멤버 역할, writing_club_join 테이블, 알림 club_id와 종류 두 가지(더하기만)

#### 전/후 사진

**함께 쓰기 탭(참여한 사람)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-clubs-reader-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-clubs-reader-after.png) |

**함께 쓰기 탭(만든 사람)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-clubs-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-clubs-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-clubs-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-clubs-m-after.png) |

**방 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.67.0/v1670-club-after.png) |

---

## [1.66.7] — 2026-10-18 · 개인 확장 (줄바꿈 정리, 검색 메뉴 태그)

- 줄바꿈: body는 overflow-wrap break-word, 긴 주소가 들어올 칸만 anywhere, 버튼·표 머리 nowrap, "A · B" 메타 줄은 조각마다 묶어 "·" 사이에서만 꺾임
- 검색 메뉴 추천 태그의 "# #일상"을 "# 일상"으로
- DB 바뀜 없음

#### 전/후 사진

**AI 연결(휴대폰)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-conn-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-conn-m-after.png) |

**시리즈 목록(휴대폰)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-series-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-series-m-after.png) |

**함께 쓰기(휴대폰)**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-clubs-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-clubs-m-after.png) |

**검색 메뉴 추천 태그**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-search-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.7/v1667-search-after.png) |

---

## [1.66.6] — 2026-10-18 · 개인 확장 (글 카드 위아래 줄이기)

- **103 후속**: 넓은 화면(1200px 이상) 한 줄 4개, 좁으면 3·2·1개, 사진 카드 미리보기 3줄, 카드 안쪽 여백·줄 사이 줄임
- DB 바뀜 없음

#### 전/후 사진

**1440 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.6/v1666-home-1440-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.6/v1666-home-1440-after.png) |

**1920 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.6/v1666-home-1920-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.6/v1666-home-1920-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.6/v1666-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.6/v1666-home-m-after.png) |

---

## [1.66.5] — 2026-10-16 · 개인 확장 (내 글 쪽 번호)

- **113 내 글 쪽 번호**: 내 글 목록을 한 쪽 20개와 쪽 번호로([더 보기] 대신)
- DB 바뀜 없음

#### 전/후 사진

**내 글 목록 아래**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.5/v1665-manage-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.5/v1665-manage-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.5/v1665-manage-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.5/v1665-manage-m-after.png) |

---

## [1.66.4] — 2026-10-16 · 개인 확장 (함께 쓰기 첫 화면, 멤버 목록 접기)

- **111 후속**: 함께 쓰기 첫 화면을 누르는 카드(내 이번 기간 상태)로, 만들기 폼은 버튼으로 열기, 빈 화면, 960px
- **111 후속**: 멤버 9명부터 멤버 목록을 처음엔 접어 두고 펼치기
- DB 바뀜 없음

#### 전/후 사진

**함께 쓰기 첫 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-clubs-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-clubs-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-clubs-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-clubs-m-after.png) |

**모임 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-club-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-club-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.4/v1664-club-m-after.png) |

---

## [1.66.3] — 2026-10-16 · 개인 확장 (서식 추천받기 버튼)

- **085 후속**: 글 점검의 서식 추천을 [서식 추천받기] 버튼으로, 결과 아래에 살펴본 항목, [다시 추천받기]
- DB 바뀜 없음

#### 전/후 사진

**글 점검 창**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.3/v1663-check-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.3/v1663-check-after.png) |

*[서식 추천받기]를 누른 뒤*

![](https://github.com/practicech/blog/releases/download/v1.66.3/v1663-check-run-after.png)

---

## [1.66.2] — 2026-10-16 · 개인 확장 (시리즈 관리 묶음 보기, 함께 쓰기 화면 정리)

- **106 후속**: 내 글 > 시리즈 탭을 시리즈마다 접힌 묶음으로, ↑↓ 버튼 자리 고정, 여러 글을 넣을 때 순서가 거꾸로 되던 것 고침
- **111 후속**: 함께 쓰기 화면, 기간 정보 한 줄, 진행 막대와 "N/N명 썼어요", 멤버 줄에 프로필 사진·상태, 초대는 버튼으로, 위쪽 960px
- DB 바뀜 없음

#### 전/후 사진

**내 글 > 시리즈 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-mseries-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-mseries-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-mseries-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-mseries-m-after.png) |

**함께 쓰기 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-club-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-club-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-club-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.2/v1662-club-m-after.png) |

---

## [1.66.1] — 2026-10-16 · 개인 확장 (AI 연결 썸네일)

- **110 후속**: AI에 연결하기에 make_thumbnail(임시글만, 1600×1000 PNG를 카드 대표 사진으로), Pretendard 글꼴(OFL) 포함, 실행 이미지에 fontconfig·libfreetype6
- DB 바뀜 없음

#### 전/후 사진

**AI 연결로 만든 썸네일**(전에는 없던 도구)

| 초록 | 먹색 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.1/v1661-thumb-forest.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.1/v1661-thumb-ink.png) |

---

## [1.66.0] — 2026-10-16 · 개인 확장 (여러 개 선택, 함께 쓰기, 처음 온 사람 안내, 첫 댓글을 기다리는 글, 썸네일 만들기, 릴리스 노트 나눠 불러오기)

- **109 여러 개 선택**: 내 글에서 골라 한 번에 휴지통·공개 범위 바꾸기, 휴지통에서 한 번에 복원·영구 삭제
- **111 함께 쓰기**: 미션 [함께 쓰기] 탭, 초대 링크로만 참여, "N일에 1편" 진행표
- **처음 온 사람 안내**: 홈 소개 한 줄, 글 없는 사람에게 시작하기 3단계, 인증 완료 화면에 첫 글 쓰기
- **103 후속**: 사진 카드 요약 4줄
- **첫 댓글을 기다리는 글**: 탐색 [추천] 맨 위, 최근 7일·댓글 0개 글 4개까지
- **110 썸네일 만들기**: 발행 설정에서 제목·배경 4가지로 대표 사진 만들기
- **112 릴리스 노트 나눠 불러오기**: 처음 10개, 내리면 10개씩
- DB: V27 함께 쓰기 테이블 두 개 새로 만듦(더하기만)

#### 전/후 사진

**내 글 여러 개 골라 한 번에**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-manage-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-manage-after.png) |

**함께 쓰기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-missions-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-missions-after.png) |

*모임 화면*

![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-club-after.png)

*모임 화면(휴대폰)*

![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-club-m-after.png)

**처음 온 사람 안내와 사진 카드 요약 4줄**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-home-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-home-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-home-m-after.png) |

**첫 댓글을 기다리는 글**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-explore-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-explore-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-explore-m-after.png) |

**썸네일 만들기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-publish-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-publish-after.png) |

*썸네일 만들기 창*

![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-thumb-after.png)

**릴리스 노트 조금씩 불러오기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-releases-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-releases-after.png) |

*휴대폰*

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-releases-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.66.0/v1660-releases-m-after.png) |

---

## [1.65.4] — 2026-10-16 · 개인 확장 (릴리스 노트 화면 사진)

- 사이트 릴리스 노트 화면에서 전/후 사진이 "[이미지]" 글자로 나오던 것 고침: GitHub Release 첨부를 /releases/img/{태그}/{파일}로 받아 보여 줌(CSP 그대로)
- DB 바뀜 없음

#### 전/후 사진

**릴리스 노트 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.4/v1654-releases-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.4/v1654-releases-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.4/v1654-releases-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.4/v1654-releases-m-after.png) |

---

## [1.65.3] — 2026-10-16 · 개인 확장 (링크 미리보기 고양이 그림)

- 링크 미리보기(og:image) 기본 그림을 첫 화면 고양이 그림으로(블로그 주소, 사진 없는 글 포함)
- DB 바뀜 없음

#### 전/후 사진

**링크 미리보기 기본 그림**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.3/v1653-og-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.3/v1653-og-after.jpg) |

---

## [1.65.2] — 2026-10-16 · 개인 확장 (검색 결과 문구)

- 홈 title "글그린 블로그 - 매일 쓴 글이 잔디처럼 자라는 곳"과 새 meta description, 모든 화면에 og:site_name=글그린, 홈에 WebSite 구조화 데이터(JSON-LD)
- 화면 바뀜 없음(사진 없음), DB 바뀜 없음

---

## [1.65.1] — 2026-10-16 · 개인 확장 (발행 설정 창 정리)

- 발행 설정 창: 칸 제목 통일과 구분선, 공개 범위 둥근 버튼, 이모지 대신 선 아이콘, 시리즈는 목록 하나("새 시리즈 만들기"일 때만 이름 칸), 대표 사진 버튼 한 줄, 취소·발행 오른쪽 아래 고정
- DB 바뀜 없음

#### 전/후 사진

**발행 설정 창**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.1/v1651-publish-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.1/v1651-publish-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.1/v1651-publish-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.1/v1651-publish-m-after.png) |

---

## [1.65.0] — 2026-10-16 · 개인 확장 (시리즈 최근 업데이트)

- **108 시리즈 최근 업데이트**: 시리즈 카드에 "글 N편 · 날짜 업데이트", 블로그 시리즈 목록은 최근 업데이트 순, 탐색 시리즈 탭 60개 넘으면 더 보기, 팔로워 새 글 알림이 시리즈 글이면 "시리즈 「이름」 새 편"
- DB 바뀜 없음

#### 전/후 사진

**블로그의 시리즈 목록**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.0/v1650-list-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.0/v1650-list-after.png) |

**탐색 시리즈 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.65.0/v1650-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.65.0/v1650-explore-after.png) |

---

## [1.64.0] — 2026-10-16 · 개인 확장 (시리즈 관리, 설명·대표 사진)

- **106 시리즈 관리**: 내 글에 시리즈 탭(만들기, 글 넣기·빼기, 순서), 글쓰기 발행 설정에 시리즈 칸, 공개 시리즈 화면의 순서 버튼은 시리즈 관리로
- **107 시리즈 설명·대표 사진**: 설명 한 줄과 대표 사진(없으면 1편부터 첫 사진), 빈 시리즈는 남기고 삭제는 버튼으로만
- DB: V26 시리즈 설명·대표 사진 칸(더하기만)

#### 전/후 사진

**내 글 시리즈 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-manage-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-manage-after.png) |

**발행 설정**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-publish-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-publish-after.png) |

**시리즈 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-detail-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-detail-after.png) |

**블로그의 시리즈 목록**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-list-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-list-after.png) |

**탐색 시리즈 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.64.0/v1640-explore-after.png) |

---

## [1.63.0] — 2026-10-16 · 개인 확장 (글 카드 좁히기, 요약 끊김)

- **103 후속 글 카드**: 카드 최소 250px(1440에서 5칸), 사진은 위 끝까지, 한 줄 제목 아래 빈 줄 없음, 사진 카드 요약 3줄과 "…", 글만 있는 카드는 딱 맞는 줄 수
- 잘린 요약 끝에 "…"(렌더링 버전 5, 켜질 때 다시 렌더링, 실행 잠금으로 한 대만)
- DB 바뀜 없음

#### 전/후 사진

**1440 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.63.0/v1630-home-1440-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.63.0/v1630-home-1440-after.png) |

**1920 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.63.0/v1630-home-1920-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.63.0/v1630-home-1920-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.63.0/v1630-home-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.63.0/v1630-home-m-after.png) |

---

## [1.62.0] — 2026-10-16 · 개인 확장 (시리즈 읽는 순서)

- **105 시리즈 읽는 순서**: 시리즈 화면에 1편부터·최신 글부터 정렬, 주인의 앞으로·뒤로는 [순서 바꾸기]를 눌렀을 때만
- DB 바뀜 없음

#### 전/후 사진

**시리즈 화면**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.62.0/v1620-series-owner-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.62.0/v1620-series-owner-after.png) |

**순서 바꾸기를 누른 화면**

![](https://github.com/practicech/blog/releases/download/v1.62.0/v1620-series-edit-after.png)

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.62.0/v1620-series-owner-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.62.0/v1620-series-owner-m-after.png) |

---

## [1.61.0] — 2026-10-16 · 개인 확장 (탐색 시리즈 탭)

- **104 탐색 시리즈 탭**: 탐색에 "시리즈" 탭, 모든 블로그의 공개 시리즈를 인기순·최근순으로
- DB 바뀜 없음

#### 전/후 사진

**탐색 시리즈 탭**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.61.0/v1610-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.61.0/v1610-explore-after.png) |

**휴대폰**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.61.0/v1610-explore-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.61.0/v1610-explore-m-after.png) |

---

## [1.60.0] — 2026-10-16 · 개인 확장 (자체 Nginx로 무중단 배포)

- 8300에 우리 Nginx 컨테이너, 앱 두 대는 8301·8302(공용 Nginx 손대지 않고 무중단), 우리 Nginx가 안 뜨면 예전 방식으로, 처음 전환 때 몇 초 끊길 수 있음
- DB 바뀜 없음

---

## [1.59.0] — 2026-10-16 · 개인 확장 (글 카드 요약, 내 문의, AI 연결 시리즈)

- **103 글 카드 빈 칸 채우기**: 목록 요약을 600자까지 저장해 사진 카드 옆 글 카드의 빈 칸을 요약으로 채움(기존 글은 다시 렌더링 작업이 채움)
- **099 내 문의**: 문의하기에 "내 문의" 탭, 보낸 문의와 답장 보기, 답장이 오면 알림
- AI에 연결하기: 임시글을 시리즈에 넣기(이름·번호, 없으면 새로 만듦)
- DB: V24 글 요약 600자로 넓힘, V25 알림에 문의 번호 칸(더하기만)

#### 전/후 사진

**사진 카드 옆 글 카드의 빈 칸 채우기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.59.0/v1590-home-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.59.0/v1590-home-after.png) |

**내 문의**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.59.0/v1590-contact-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.59.0/v1590-contact-after.png) |

---

## [1.58.3] — 2026-10-16 · 문서

- CHANGELOG의 1.36.1~1.58.2 항목에 릴리스 노트와 같은 전/후 사진을 붙임(앱 바뀜 없음)

---

## [1.58.2] — 2026-10-15 · 개인 확장 (내 글 줄 정리, 통계 숫자)

- 내 글 줄 오른쪽을 정해진 칸으로(태그, 조회·좋아요·댓글 선 아이콘, 날짜, 메뉴), 날짜는 24시간 안이면 상대 시간·그 뒤 2026.10.12, "수정됨"은 날짜 말풍선으로
- 통계: "날짜별 숫자 보기" 펼침·표를 화면에서 뺌(화면 읽기용은 유지), 휴대폰에서 막대를 누르면 "날짜 · N회"
- DB 바뀜 없음


#### 전/후 사진

**내 글 줄 정리**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.58.2/v1582-manage-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.2/v1582-manage-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.58.2/v1582-manage-phone.png)

**통계**

| 전(PC) | 후(휴대폰에서 막대 누름) |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.58.2/v1582-stats-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.2/v1582-stats-phone.png) |

---

## [1.58.1] — 2026-10-15 · 개인 확장 (프로필 버튼·선 아이콘)

- 프로필 방명록·RSS를 선 아이콘이 든 작은 테두리 버튼으로, "오늘 N · 전체 N" 앞 그래프 아이콘, 프로필 화면 남은 이모지를 선 아이콘으로(새 이모지는 테스트가 막음)
- DB 바뀜 없음


#### 전/후 사진

**프로필 방명록·RSS 버튼과 선 아이콘**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-header-pc-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-header-pc-light-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-header-pc-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-header-pc-dark-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-header-phone-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-header-phone-light-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-guestbook-pc-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-guestbook-pc-light-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-home-pc-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.1/v1581-home-pc-light-after.png) |

---

## [1.58.0] — 2026-10-15 · 개인 확장 (내 글 한 줄 목록, 통계)

- **101 내 글 한 줄 목록**: 큰 카드 대신 글마다 한 줄(휴대폰 두 줄)
- 통계: 막대에 날짜·숫자 말풍선, "날짜별 숫자 보기"는 작은 펼침(최신이 위), 많이 읽힌 글은 순위·제목·길이 막대
- DB 바뀜 없음


#### 전/후 사진

**내 글은 한 줄 목록으로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.58.0/v1580-manage-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.0/v1580-manage-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.58.0/v1580-manage-phone.png)

**통계 다듬기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.58.0/v1580-stats-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.58.0/v1580-stats-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.58.0/v1580-stats-phone.png)

---

## [1.57.2] — 2026-10-15 · 개인 확장 (목차 위치)

- 목차를 누르면 제목이 머리말 아래 16px에 보임(가려지지 않음), 지금 읽는 부분을 목차에 표시(PC·휴대폰)
- DB 바뀜 없음


#### 전/후 사진

**목차 누르면 제목이 머리말 아래에**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.2/v1572-toc-pc-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.2/v1572-toc-pc-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.57.2/v1572-toc-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.2/v1572-toc-m-after.png) |

---

## [1.57.1] — 2026-10-15 · 개인 확장 (내 글 메뉴)

- 내 글 "…" 메뉴: 선 아이콘, 공개 범위를 메뉴 안에서 펼쳐 고르기, 내 글 목록 카드에도 같은 메뉴
- (1.57.0에 함께 들어감) 휴대폰 알림 창이 왼쪽으로 잘리던 것 고침
- DB 바뀜 없음


#### 전/후 사진

**내 글 "…" 메뉴 새 모양**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.1/v1571-om-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.1/v1571-om-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.57.1/v1571-om-phone.png) ![](https://github.com/practicech/blog/releases/download/v1.57.1/v1571-om-manage.png)

---

## [1.57.0] — 2026-10-15 · 개인 확장 (결과 알림 토스트, 블로그 주소 칸)

- 설정·알림·내 글·글 화면·로그인·문의·관리 화면의 결과를 머리말 아래 3초 토스트로(성공 초록·오류 빨강), 칸별 오류는 칸 옆 그대로
- 블로그 주소 칸 앞에 실제 사이트 주소, 다음 바꿀 수 있는 날까지 잠김
- DB 바뀜 없음


#### 전/후 사진

**결과 알림을 머리말 아래 토스트로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-settings-saved-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-settings-saved-light-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-settings-saved-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-settings-saved-dark-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-login-error-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-login-error-light-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-login-error-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-login-error-dark-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-settings-nochange-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-settings-nochange-light-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-admin-hide-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-admin-hide-after.png) |

**블로그 주소 칸**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-handle-locked-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-handle-locked-after.png) |

**휴대폰 알림 창이 잘리지 않게**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-before-bell-light.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-after-bell-light.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-before-bell-dark.png) | ![](https://github.com/practicech/blog/releases/download/v1.57.0/v1570-after-bell-dark.png) |

---

## [1.56.1] — 2026-10-15 · 개인 확장 (메뉴 순서, PC 카카오톡 공유, 구글 사이트 확인)

- 머리말 메뉴 순서 홈 · 탐색 · 태그 · 피드 · 미션(휴대폰 ☰ 메뉴도)
- PC 카카오톡 공유 창이 열리게 보안 설정(CSP form-action) 고침
- Google Search Console 사이트 확인 메타 태그(네이버 태그 유지), sitemap.xml에서 테스트 계정 빼기, 블로그 주소 화면의 대표 주소·공유 주소에 /@ 대신 새 주소
- DB 바뀜 없음


#### 전/후 사진

**머리말 메뉴 순서**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.56.1/v1561-nav-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.56.1/v1561-nav-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.56.1/v1561-nav-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.56.1/v1561-nav-m-after.png) |

---

## [1.56.0] — 2026-10-15 · 개인 확장 (글쓰기 도움말, 코드 품질)

- 글쓰기 도움말을 줄별로(서식 창·제목·목록·인용·넣기 메뉴·단축키), 누를 글자는 키 모양, [Markdown으로 쓰기] 간격과 제목 칸 밑줄 다듬음
- SonarQube 새 코드 28가지 정리(동작 그대로)
- DB 바뀜 없음


#### 전/후 사진

**글쓰기 도움말 정리**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.56.0/v1560-help-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.56.0/v1560-help-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.56.0/v1560-help-phone.png)

---

## [1.55.0] — 2026-10-15 · 개인 확장 (서식 창, GIF 안내, 코드 품질)

- 글쓰기 서식 창은 글자를 골랐을 때만(사진·빈 줄만 고르면 안 뜸)
- 1920px·300프레임이 넘는 GIF는 올리기 전에 안내, 지난 올리기 실패 안내는 다음 성공 뒤 사라짐
- SonarQube 버그 8개·작은 문제 9개 고침
- DB 바뀜 없음


#### 전/후 사진

**서식 창은 글자를 골랐을 때만**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.55.0/v1550-format-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.55.0/v1550-format-after.png) |

**큰 GIF 안내와 지난 실패 안내 지우기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.55.0/v1550-gif-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.55.0/v1550-gif-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.55.0/v1550-notice-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.55.0/v1550-notice-after.png) |

---

## [1.54.0] — 2026-10-15 · 개인 확장 (테스트 글 그림, 스크롤 유지)

- **083 추가 테스트 글 그림**: 그림 3장씩 든 긴 테스트 글 8편 추가(첫 그림이 대표 사진, 일반 업로드 경로), 다시 누르면 새 8편만, [모두 지우기]는 그림 파일도
- 같은 화면으로 돌아오는 버튼을 눌러도 스크롤 자리 유지, 화면 밖 "저장했어요" 안내는 머리말 아래 4초
- DB 바뀜 없음


#### 전/후 사진

**테스트용 긴 글에 그림 8편 더**

![](https://github.com/practicech/blog/releases/download/v1.54.0/v1540-sample-posts.png)

---

## [1.53.0] — 2026-10-15 · 개인 확장 (프로필 탭, 꼬리말 줄바꿈)

- **프로필 홈·글·시리즈 탭**: 홈(/주소)은 고정 글·기록 잔디·짧은 기록·최근 글 4개·시리즈 4개 선반과 "모두 보기", 글(/주소/posts)은 최신순·인기순과 분류·태그 거르기, 시리즈(/주소/series)는 주인이 순서 바꾸기, 예전 /주소?tag= 주소는 글 탭으로
- 휴대폰 꼬리말 링크가 낱말 중간에서 끊기지 않고 통째로 줄바꿈
- DB 바뀜 없음


#### 전/후 사진

**프로필 홈·글·시리즈 탭**

| 전 | 후(홈) |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-profile-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-profile-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-profile-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-profile-m-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-posts.png)
![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-series.png)

**휴대폰 꼬리말 줄바꿈**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-foot-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-foot-dark-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-foot-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.53.0/v1530-foot-light-after.png) |

---

## [1.52.0] — 2026-10-15 · 개인 확장 (탐색 정리, 작가 카드, 탭 숫자)

- 탐색: 추천 글이 맨 앞, 사람 줄 두 개를 하나로(같은 사람 한 번만), 모든 줄 같은 칸, 작은 팔로우 버튼, "태그 전체 보기" 뺌
- 글 아래 작가 카드 한 줄: 아바타·이름·한 줄 소개·"글 N · 팔로워 N" + 버튼 하나(팔로우/팔로우 중/내 블로그), [블로그 가기] 뺌, 휴대폰은 버튼이 아래로
- 내 글 탭(발행 글·임시글·휴지통) 숫자가 휴지통 옮기기·되돌리기·완전 삭제 뒤 바로 맞게
- DB 바뀜 없음


#### 전/후 사진

**탐색 화면 정리**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-explore-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-explore-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-explore-m-after.png) |

**글 아래 작가 카드 한 줄**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-card-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-card-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-card-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.52.0/v1520-card-m-after.png) |

---

## [1.51.1] — 2026-10-15 · 개인 확장 (태그 정렬 정리)

- 태그 화면 정렬에서 이름순을 빼고 인기순·최근순만(예전 이름순 주소는 인기순으로)
- DB 바뀜 없음


#### 전/후 사진

**태그 화면 정렬에서 이름순 빼기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.51.1/v1511-tags-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.51.1/v1511-tags-after.png) |

---

## [1.51.0] — 2026-10-15 · 개인 확장 (머리말 테마 버튼, 휴지통 비우기)

- 머리말 알림·프로필 앞에 테마 버튼(라이트는 달, 다크는 해), 로그아웃 상태와 휴대폰(☰ 밖)에서도 보임, 누르기 전엔 기기 설정을 따르고 누르면 "화면 테마" 설정에 저장
- **휴지통 비우기**: 휴지통에 글이 있을 때만 [휴지통 비우기] 버튼(개수와 되돌릴 수 없음을 묻는 확인 창, 내 글만, 하나씩 완전 삭제와 같은 정리), 휴지통 카드에 "임시글" 표시·"N일 남음"·제목 없는 글은 본문 앞부분, 좋아요·댓글 수는 숨김
- DB 바뀜 없음


#### 전/후 사진

**머리말 테마 버튼**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-pc-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-pc-dark-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-pc-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-pc-light-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-m-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-m-dark-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-m-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-hdr-m-light-after.png) |

**휴지통 비우기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-trash-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.51.0/v1510-trash-after.png) |

---

## [1.50.0] — 2026-10-15 · 개인 확장 (관리 목록 통일, 태그 화면)

- **관리 화면 목록 통일(098 후속)**: 회원·신고·문의함·사이트 공지·관리 기록·글·댓글·방명록이 같은 목록 모양(검색·필터·건수·20개씩 쪽 넘김·상태 색), 신고·문의는 목록 옆에 자세히 보기. 검색·필터·쪽 넘김은 DB에서(500개 제한 없음)
- **태그 화면 새 모양**: 태그 수·태그 찾기·정렬(인기·이름·최근), 태그마다 글 카드를 가로 한 줄로(화살표·밀어 넘기기), 10개씩 더 보기, 공개 글만. 머리말에 "태그" 메뉴
- DB 바뀜 없음


#### 전/후 사진

**관리 화면 목록 통일**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-members-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-members-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-reports-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-reports-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-inquiries-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-inquiries-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-notices-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-notices-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-logs-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-logs-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-content-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-content-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-guestbook-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-guestbook-after.png) |

**태그 화면 새 모양**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-tags-dark-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-tags-dark-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-tags-light-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.50.0/v1500-tags-light-after.png) |

---

## [1.49.3] — 2026-10-15 · 개인 확장 (다크 모드 밤 그림)

- 다크 모드에서 홈 사진 배너와 고양이 그림이 밤 그림으로 바뀜(시스템 테마와 사이트 테마 설정 모두 따름, 어둡게 덮지 않음)
- DB 바뀜 없음


#### 전/후 사진

**다크 모드는 밤 그림**

| 전(다크) | 후(다크) |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.49.3/v1493-home-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.49.3/v1493-home-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.49.3/v1493-login-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.49.3/v1493-login-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.49.3/v1493-home-light.png)

---

## [1.49.2] — 2026-10-15 · 개인 확장 (읽기 진행 막대, 메일 제목, 다크 모드 되돌림)

- 1.49.1의 다크 모드 사진·그림 어둡게를 되돌림(밤 그림으로 다시 만들 예정)
- 글 화면 위 읽기 진행 막대가 부드럽게 움직임(짧은 글도), 움직임 줄이기 설정을 따름
- 메일 제목 7가지를 "[글그린] …"으로, 코드 입력·인증 화면에 스팸함 확인 안내
- DB 바뀜 없음


#### 전/후 사진

**메일 제목 "[글그린]"과 스팸함 안내**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.49.2/v1492-signup-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.49.2/v1492-signup-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.49.2/v1492-mail-subjects.png)

---

## [1.49.1] — 2026-10-15 · 개인 확장 (다크 모드 톤)

- 다크 모드에서 홈 사진 배너에 어두운 덧칠, 크림색 고양이 그림은 어둡게(라이트 모드는 그대로)
- DB 바뀜 없음

---

## [1.49.0] — 2026-10-14 · 개인 확장 (관리자 대시보드 다듬기)

- 대시보드 표를 빼고 그래프에 마우스·손가락을 올리면 값이 보이는 말풍선
- 오늘 숫자 카드에 어제보다 ▲/▼
- 아래 카드 정리, 왼쪽 메뉴에서 묶음 제목과 누를 수 있는 항목을 구분
- DB 바뀜 없음


#### 전/후 사진

**관리자 대시보드 다듬기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.49.0/v1490-admin-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.49.0/v1490-admin-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.49.0/v1490-admin-light.png)

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.49.0/v1490-admin-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.49.0/v1490-admin-m-after.png) |

---

## [1.48.0] — 2026-10-14 · 개인 확장 (관리자 시스템 화면)

- **098 관리자 시스템 화면**: 앱 두 대의 상태(30초마다 신호, 90초 없으면 "응답 없음"), 배포 버전(커밋 7자리)과 켜진 시각, 외부 연동 설정 여부(키 값은 안 보임), 기능 스위치(읽기 전용)
- 배포 이미지에 커밋 해시를 버전으로 넣고, 컨테이너 이름을 호스트 이름으로
- DB 바뀜 없음


#### 전/후 사진

**관리자 시스템 화면(새 화면)**

![](https://github.com/practicech/blog/releases/download/v1.48.0/v1480-system.png)

---

## [1.47.1] — 2026-10-14 · 개인 확장 (관리자 대시보드 고침)

- 관리자 대시보드 스위치(`ADMIN_INSIGHTS_ENABLED`)를 끄면 예전 7일 추이가 그대로 나옴
- 대시보드 인기 글·최근 활동에는 지금 공개된 글만 나옴
- DB 바뀜 없음

---

## [1.47.0] — 2026-10-14 · 개인 확장 (관리자 대시보드 개편)

- **098 관리자 대시보드**: 넓은 화면은 왼쪽 묶음 메뉴(휴대폰은 위쪽 가로 메뉴), 오늘 숫자 카드 5개, 30일 추이 그래프 4개, 인기 글, 최근 활동, 서버 상태(`ADMIN_INSIGHTS_ENABLED`)
- **AI 연결 키 경고**: 새 연결 키 화면에 "키를 채팅이나 다른 곳에 붙이지 마세요" 한 줄
- DB 바뀜 없음


#### 전/후 사진

**관리자 대시보드 개편**

| 전 | 후(다크) |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-admin-pc-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-admin-dark-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-admin-light-after.png)

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-admin-m-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-admin-m-after.png) |

**AI 연결 키 경고**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-key-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.47.0/v1470-key-after.png) |

---

## [1.46.0] — 2026-10-14 · 개인 확장 (AI 연결, 텔레그램 알림, 선 아이콘)

- **097 AI에 연결하기**: 설정 > 연결에서 개인 키를 만들고 Claude·Cursor(MCP, `/mcp`, 상태 없는 Streamable HTTP)로 임시글만 만들고 고침, 블로그 주소 예약어에 `mcp`(`blog.ai-connect.enabled`)
- **096 텔레그램 알림**: 관리자에게 새 문의·가입·신고, 배포 결과 알림(`TELEGRAM_BOT_TOKEN`이 없으면 꺼짐)
- **공개 범위 선 아이콘**: 전체 공개는 표시 없음, 비공개·친구·그룹·링크만 선 아이콘, 카드 💬·비밀 🔒·미션 👥도 선 아이콘
- DB: V22 `telegram_link`, V23 `ai_key`(표 추가만)


#### 전/후 사진

**AI에 연결하기(새 화면)**

![](https://github.com/practicech/blog/releases/download/v1.46.0/v1460-connect.png)

**공개 범위 표시를 선 아이콘으로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.46.0/v1460-manage-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.46.0/v1460-manage-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.46.0/v1460-secret-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.46.0/v1460-secret-after.png) |

---

## [1.45.0] — 2026-10-14 · 개인 확장 (사이트 점검 마무리)

- 405 같은 오류도 사이트 디자인 오류 화면으로(예전엔 하얀 기본 화면)
- 404 화면 머리말에 로그인 상태가 제대로 보임
- 휴대폰 홈 배너: 사진은 고양이 쪽으로 자르고 문구는 사진 아래 실제 글자로
- 빈 방명록·짧은 기록의 회색 줄 없앰
- 글이 아닌 화면에도 SEO 메타(설명·공유 미리보기·대표 주소), 검색 제외 화면은 빼고
- 키보드로 검색칸·검색 버튼에 가면 테두리 보임
- DB 바뀜 없음


#### 전/후 사진

**오류 화면도 사이트 디자인으로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-405-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-405-after.png) |

**404 화면의 로그인 상태**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-404-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-404-after.png) |

**휴대폰 홈 배너**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-mhome-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-mhome-after.png) |

**빈 방명록의 회색 줄**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-guestbook-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-guestbook-after.png) |

**카카오톡 공유**

![](https://github.com/practicech/blog/releases/download/v1.45.0/v1450-kakao.png)

---

## [1.44.0] — 2026-10-14 · 개인 확장 (검색 제안, 알약 버튼, 공유 메뉴, 관련 글)

- **094 검색 오타 고침·비슷한 글**: 첫 쪽 결과가 3개 미만이면 "혹시 ○○을(를) 찾으셨나요?"와 고친 검색어 결과, 비슷한 글 최대 4개(외부 AI 없음, `SEARCH_SUGGEST_ENABLED=false`로 끔)
- **알약 버튼**: 혼자 떨어진 밑줄 링크 33곳을 공통 알약 버튼으로(앞으로 ›, 돌아가기 ‹)
- **공유하기 메뉴**: 같은 모양 아이콘, 네이버 공유 추가, 링크 복사 알림, "다른 앱으로"는 터치 기기에서만, 카카오톡은 `KAKAO_JS_KEY`가 있을 때만
- 무한 스크롤 카드·댓글의 글쓴이 링크를 새 블로그 주소로(옛 `/@`는 예약어 아이디만)
- **095 관련 글 4개**: 글 아래 무한 스크롤 대신 관련 글 4개(PC 2×2, 휴대폰 한 줄씩; 태그 겹침 → 같은 글쓴이 최근 글 → 최근 인기 글, 비공개·친구 공개 제외)
- 본문 표 아래 여백, 미션 화면 글자 대비 4.5:1·색 버튼을 기본 초록 버튼으로, 친구 미션 카드 두 겹 상자 고침
- 비밀번호 바꾸기·재설정 화면 규칙 표시 고침, PC 프로필 잔디가 카드 너비를 채움
- CSS·JS 압축(Spring), Nginx 설정 예시 `server_tokens off`
- Nginx 설정 예시 HTTP/2를 1.18 형식으로
- DB 바뀜 없음


#### 전/후 사진

**검색 오타 고침·비슷한 글**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-search-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-search-after.png) |

**밑줄 링크를 알약 버튼으로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-explore-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-explore-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-series-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-series-after.png) |

**공유하기 메뉴 정리**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-share-pc-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-share-pc-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-share-mobile-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-share-mobile-after.png) |

**글 아래 관련 글 4개**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-related-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-related-after.png) |

**표 여백·미션 화면 대비**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-table-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-table-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-mission-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-mission-after.png) |

**비밀번호 규칙 표시·프로필 잔디**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-password-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-password-after.png) |
| ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-grass-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.44.0/v1440-grass-after.png) |

---

## [1.43.0] — 2026-10-14 · 개인 확장 (좋아요한 글 빨간 하트)

- **088 고침 좋아요한 글 하트**: 로그인한 사람이 좋아요한 글은 모든 목록 카드의 하트가 빨갛게(화면의 카드 번호를 `GET /api/likes/mine`로 한 번에 물음, 회원 화면에만)
- DB 바뀜 없음


#### 전/후 사진

**좋아요한 글은 빨간 하트**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.43.0/v1430-likes-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.43.0/v1430-likes-after.png) |

---

## [1.42.0] — 2026-10-14 · 개인 확장 (무중단 배포)

- **093 무중단 배포**: 앱 두 대(8300·8301)를 한 대씩 교체(2번 먼저, 건강 확인 뒤 1번), 실패하면 그 대만 전 버전으로, 내릴 때 30초 우아한 종료
- 한 대당 메모리 1GB 상한, 컨테이너는 127.0.0.1에만 열림
- 예약 작업(미션 주제·태그 아이콘·사진 정리)은 Redis 잠금으로 한 대에서만
- Nginx 설정 예시(두 대 upstream)는 deploy/school에, 서버 적용은 관리자 작업
- DB 바뀜 없음

---

## [1.41.0] — 2026-10-14 · 개인 확장 (카드 높이 맞춤, 친구 링크, 글 화면 넓게)

- **088 고침 카드 높이 맞춤**: 빈 회색 사진 칸 없앰, 나란한 목록은 모든 카드를 가장 큰 카드 높이로(사진 없는 카드는 요약 줄로 채움, 최소 3줄), 반응 줄 맨 아래 고정, 좋아요·댓글 숫자 같은 굵기, 한 줄에 하나일 땐 원래 크기
- **내 블로그 "친구 N"**: 숫자 줄 끝에 나만 보이는 친구 수 링크(친구 관리로)
- **글 화면 넓게**: 본문 칸 768px(휴대폰은 꽉 참), 본문 17.6px·줄 간격 1.8, 사진 크게 가운데, ····공유하기 버튼의 ⇅·⌄ 뺌, 조회수 안내문은 마우스를 올릴 때만
- DB 바뀜 없음


#### 전/후 사진

**카드 높이 맞춤**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.41.0/v1410-cards-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.41.0/v1410-cards-after.png) |

**내 블로그에 "친구 N"**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.41.0/v1410-blog-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.41.0/v1410-blog-after.png) |

**글 화면 넓게**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.41.0/v1410-post-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.41.0/v1410-post-after.png) |

---

## [1.40.0] — 2026-10-14 · 개인 확장 (사진 칸 줄마다, 목록 넓게)

- **빈 사진 칸 고침(088 후속)**: 같은 줄에 사진 카드가 있을 때만 빈 사진 칸, 화면 크기가 바뀌거나 더 불러오면 줄마다 다시 계산
- **목록 넓게**: 홈·피드·탐색·검색·태그·블로그·머리말·꼬리말 1440px, 홈은 1280px부터 4개씩(읽기·글쓰기·설정·약관은 그대로)
- DB 바뀜 없음


#### 전/후 사진

**빈 사진 칸은 사진 있는 줄에만**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.40.0/v1400-slots-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.40.0/v1400-slots-after.png) |

**목록 화면을 넓게**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.40.0/v1400-wide-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.40.0/v1400-wide-after.png) |

---

## [1.39.0] — 2026-10-14 · 개인 확장 (홈 한 칸, 무한 스크롤 고침)

- **홈 한 칸**: 둘러보기 칸을 빼고 최신 글이 화면 폭 전체(넓은 화면 3개씩), 작은 링크는 "최신 글" 제목 오른쪽으로
- **091 무한 스크롤은 내려갔을 때만**: 목록 끝 반 화면 안으로 내려가면 한 묶음씩, 한 번에 한 요청, 첫 화면은 모자랄 때만 채움
- DB 바뀜 없음


#### 전/후 사진

**홈을 한 칸으로**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.39.0/v1390-home-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.39.0/v1390-home-after.png) |

---

## [1.38.0] — 2026-10-14 · 개인 확장 (댓글 좋아요, 문의하기)

- **089 댓글·답글 좋아요**: 댓글마다 ♡와 개수, 댓글 쓴 사람에게 좋아요 알림(`blog.comment.like-enabled`)
- **090 문의하기**: 사이트 안 문의 양식(/contact)과 관리자 문의함, 문의 링크에 관리자 개인 주소를 쓰지 않음(`blog.contact.enabled`)
- **배포 기록에 메일 로그인 확인**: 배포할 때 메일 계정으로 로그인되는지 한 줄로 남김
- DB: V20 댓글 좋아요, V21 문의(표 추가만)


#### 전/후 사진

**댓글·답글 좋아요**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.38.0/v1380-comments-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.38.0/v1380-comments-after.png) |

**문의하기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.38.0/v1380-footer-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.38.0/v1380-footer-after.png) |

![](https://github.com/practicech/blog/releases/download/v1.38.0/v1380-contact.png)

---

## [1.37.0] — 2026-10-14 · 개인 확장 (GitHub 링크, 댓글 아바타, 파비콘, 둥근 자르기)

- **GitHub 링크**: 꼬리말·좁은 화면 ☰ 메뉴·설정 아래 이용약관 옆, PC 홈 오른쪽 문구 끝에 AIPKANG GitHub 링크(새 탭)
- **댓글 아바타**: 첫 글자가 가운데로, 밑줄 없음, 새로 쓰거나 고친 댓글에도 아바타
- **파비콘·탭 제목**: 초록 발바닥 파비콘(홈 화면 아이콘 포함), 탭 제목은 "화면 · 글그린"
- **프로필 사진 자르기**: 둥근 안내선과 어두운 모서리, 초록 확대 막대
- DB 바뀜 없음


#### 전/후 사진

**GitHub 링크**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-footer-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-footer-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-home-side-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-home-side-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-settings-links-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-settings-links-after.png) |
| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-mobile-menu-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-mobile-menu-after.png) |

**댓글 아바타**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-comments-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-comments-after.png) |

**프로필 사진 자르기**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-cropper-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.37.0/v1370-cropper-after.png) |

---

## [1.36.1] — 2026-10-14 · 개인 확장 (네이버 사이트 확인, 가입 고침)

- **네이버 서치어드바이저 사이트 확인**: 모든 화면 머리에 확인용 메타 태그
- **가입 화면 순서 고침**: 코드 확인 전 이메일 칸에서 Enter를 누르면 [인증 코드 받기]가 눌리고 가입 폼은 보내지 않음, 서버도 화면 순서대로 검사(약관 안내는 약관 칸 바로 위)
- **메일 보낸 사람 이름**: 인증 메일이 "글그린"으로 보임
- DB 바뀜 없음


#### 전/후 사진

**가입 화면에서 Enter를 눌렀을 때**

| 전 | 후 |
|---|---|
| ![](https://github.com/practicech/blog/releases/download/v1.36.1/v1361-signup-before.png) | ![](https://github.com/practicech/blog/releases/download/v1.36.1/v1361-signup-after.png) |

---

## [1.36.0] — 2026-10-14 · 개인 확장 (글 카드 한 모양)

- **088 글 카드 한 모양**: 홈·피드·탐색·검색·태그·블로그·시리즈·내 글·미션 어디서나 같은 카드 — 맨 위 대표 사진, 아바타·아이디·태그, 제목, 요약, 좋아요·댓글·날짜(하트는 회색)
- DB 바뀜 없음

---

## [1.35.2] — 2026-10-14 · 개인 확장 (코드 리뷰 고침)

- 배포 때 Gemini 키를 명령 줄 인자 대신 표준 입력으로 넘겨, 공용 서버의 프로세스 목록에 키가 보이지 않게
- 키는 앞뒤 가장자리만 다듬고, 키 확인 상태 문구를 나눔
- 글 점검의 긴 문장 AI 고치기가 정확히 그 문장 범위만 바꾸게
- DB 바뀜 없음

---

## [1.35.1] — 2026-10-14 · 개인 확장 (배포 고침)

- 배포할 때 Gemini 키 앞뒤의 공백·줄바꿈을 지우고 서버 설정에 넣음(키를 붙여 넣다 섞인 줄바꿈 때문에 키가 거부되지 않게)

---

## [1.35.0] — 2026-10-14 · 개인 확장 (글 점검 고치기 버튼·Gemini 키 확인)

- **글 점검**: 힌트마다 [고치기] 버튼
- **배포 로그**: 배포할 때 Gemini 키가 실제로 되는지 한 줄로 알려 줌
- DB 바뀜 없음, 테스트 687개 통과

---

## [1.34.0] — 2026-10-14 · 개인 확장 (글 점검 서식 힌트)

- **글 점검 서식 힌트**: 긴 문단·긴 문장, 소제목 없는 긴 구간, 반복되는 낱말(고치기 버튼), 같은 말로 시작하는 문장, 설명 없는 사진, 긴 제목을 알려 주고, 고칠 것이 없으면 무엇을 점검했는지 보여 준다
- AI 기능(Gemini 키)을 이 배포부터 켬
- DB 바뀜 없음

---

## [1.33.0] — 2026-10-14 · 개인 확장 (피드·홈·탐색 나누기)

- **086 피드·홈·탐색 나누기**: 피드는 팔로우한 글만, 추천 글과 트렌딩은 탐색으로, 홈은 최신 글만. 인기 태그는 검색창과 `/tags`에서만, 검색·미션 글도 같은 카드, 카드 맨 위에 대표 사진, 아이디는 잘리지 않음
- DB 바뀜 없음, 테스트 687개 통과

---

## [1.32.0] — 2026-10-14 · 개인 확장 (글 점검·친구 그룹 한 화면)

- **085 글 점검**: 글쓰기 미리보기 자리에 [글 점검], 맞춤법과 서식을 추천
- **087 친구·그룹 한 화면**: 설정의 친구와 친구 그룹을 한 화면으로 합치고(예전 그룹 주소는 새 화면으로 넘김), 비공개 계정 스위치는 설정 > 계정으로
- DB 바뀜 없음, 테스트 688개 통과

---

## [1.31.0] — 2026-10-14 · 개인 확장 (탐색 화면 아이디·코드 신뢰성)

- **탐색 화면**: 새로 온 작가·추천 글 작성자·추천 블로그도 아이디가 주 이름
- **코드 신뢰성(SonarQube)**: 신뢰성 등급 C → A로 고침, SonarQube 분석은 develop에서만 돈다
- DB 바뀜 없음, 테스트 687개 통과

---

## [1.30.0] — 2026-10-14 · 개인 확장 (카드 정렬·피드 추천 한 번만)

- **084 카드 정렬**: 제목·요약 줄 자리를 고정해 나란히 놓인 카드의 위아래가 맞는다(홈·피드·태그·검색·블로그·시리즈·내 글·미션 글, 추천 블로그·미션 주제 카드)
- **피드**: 추천 글이 두 번 보이던 문제를 고쳐 한 번만, "추천"은 작성자 줄의 칩으로
- DB 바뀜 없음

---

## [1.29.0] — 2026-10-13 · 개인 확장 (닉네임 30자)

- **닉네임 2~30자**(전에는 10자): 가입·소셜 가입·프로필 편집과 서버 검사가 같은 규칙. 긴 닉네임은 사람 목록·프로필·카드·댓글·오른쪽 칸에서 한 줄로 자르고 … 표시
- DB: V19가 닉네임 칸을 넓히기만 해서 기존 값은 그대로

---

## [1.28.0] — 2026-10-13 · 개인 확장 (미션 화면 새 디자인·테스트용 긴 글)

- **미션 화면 새 디자인**: 기록과 고양이가 있는 머리 영역, 같은 크기의 주제 카드 세 개, 이번 달 달력, 배지 모음, 글 카드
- **083 테스트용 긴 글**: 관리자 화면에 [테스트 글 넣기]·[모두 지우기], 로그인할 수 없는 테스트 계정 4개와 긴 글 12개(기존 데이터는 건드리지 않음)
- DB 바뀜 없음, 테스트 686개 통과

---

## [1.27.0] — 2026-10-13 · 개인 확장 (프로필 메뉴 정리·미션 완료 카드·코드 품질 고침)

- **프로필 메뉴**: 계정 항목만 남기고, 이용약관·개인정보 처리방침·릴리스 노트·문의하기는 설정 맨 아래로
- **미션 완료 카드**: 미션 글을 발행하면 글 아래에 축하 카드
- **코드 품질(SonarQube)**: 정수 넘침·정규식 스택 넘침 같은 실제 버그와 막는 이슈를 고치고(AI 태그 입력 정리에서 작은 동작 변화 하나), 동작이 바뀌지 않는 코드 냄새 136개 정리
- DB 바뀜 없음, 테스트 683개 통과

---

## [1.26.0] — 2026-10-13 · 개인 확장 (미션 화면 다듬기·코드 품질 검사)

- **미션 화면 다듬기**: 첫 주제 카드를 크게, 주제·글은 홈과 같은 카드 모양, 연속 일수 칩
- **코드 품질 검사**: GitHub Actions에 SonarQube 분석(학교 SonarQube, 토큰 Secret이 없으면 건너뜀)과 CodeRabbit 리뷰 설정(`.coderabbit.yaml`)
- DB 바뀜 없음, 테스트 683개 통과

---

## [1.25.0] — 2026-10-13 · 개인 확장 (글 화면 velog식·아이디가 주 이름·피드 추천 섞기)

- **080 글 화면**: velog처럼 큰 제목 아래 작성자 줄 오른쪽에 팔로우(내 글이면 ⋯ 메뉴), 태그 칩은 작성자 줄 바로 아래. 댓글 다음에 "관심 있을 만한 글"이 카드로 이어지고 내리면 계속 붙는다(같은 태그·같은 작가·관심 태그·최신 순, 관련 글은 여기로 합침)
- **082 아이디가 주 이름**: 인스타그램처럼 작성자 줄·카드·댓글·알림 등에는 아이디(@ 없이), 닉네임은 블로그 첫 화면 프로필 줄과 사람 목록에서 아이디 아래 작게
- **081 피드에 추천 글 섞기**: 팔로우한 글 사이에 "추천" 표시와 이유를 단 글이 섞이고, 팔로우한 글을 다 보면 추천 글로 이어진다
- DB 바뀜 없음, 테스트 683개 통과

---

## [1.24.0] — 2026-10-13 · 개인 확장 (탐색·미션 화면 정리)

- **079 탐색**: 위 메뉴가 홈 · 탐색 · 미션 · 피드(PC·휴대폰 ☰, 비회원도). `/explore`에서 인기 태그, 새로 온 작가, 추천 글, 추천 블로그를 한 화면에서 본다
- **미션 화면**: 오늘의 주제를 맨 위에 크게, 내 기록·배지·지난 미션은 아래에 접어 둠
- DB 바뀜 없음, 테스트 681개 통과

---

## [1.23.0] — 2026-10-13 · 개인 확장 (화면 다듬기)

- **펼침 메뉴**: 프로필·⋯·☰·공유 같은 펼침 메뉴는 바깥을 누르거나 Esc로 닫히고, 한 번에 하나만 열린다
- **사람 목록·블로그 프로필**: 아이디를 위에, 닉네임은 아래에 작게
- **PC 홈 오른쪽 칸**: 아래 링크를 한 줄로
- DB 바뀜 없음, 테스트 680개 통과

---

## [1.22.1] — 2026-10-13 · 개인 확장 (버그 수정)

- PC 홈 오른쪽 칸 위에서 마우스 휠로 화면이 내려가지 않던 문제 고침(`overscroll-behavior` 제거)

---

## [1.22.0] — 2026-10-13 · 개인 확장 (글 화면 정리·주소에서 @ 빼기·검색창 추천 태그)

- **075 글 화면 정리**: 글쓴이 관리(수정·공개 범위·삭제)는 ⋯ 메뉴로, 고정·카테고리·시리즈는 글 설정 창으로. 닉네임 옆 @아이디는 블로그 첫 화면 프로필 줄에만
- **076 블로그 주소에서 @ 빼기**: 블로그는 `/{주소}`로 열리고, 예전 `/@{주소}` 링크는 새 주소로 넘겨 준다(301). 예약어와 겹치는 기존 주소는 `/@` 주소를 그대로 쓴다
- **077 PC 홈**: 꼬리말은 한 번만(프로필 메뉴 안 꼬리말 링크도 숨김), 오른쪽 칸에 추천 글
- **078 검색창 추천 태그**: 머리말의 "태그" 대신 🔍를 누르면 검색창과 추천 태그 10개
- 릴리스 노트에서 사람 이름을 뺐다("개인 확장", "팀 3명")
- DB 바뀜 없음, 테스트 680개 통과

---

## [1.21.0] — 2026-10-13 · 개인 확장 (화면 정리 후속)

- **내 글·시리즈 카드**: 홈과 같은 카드 조각으로, 공개 범위·예약 같은 관리 정보는 배지, 관리 버튼은 ⋯ 메뉴로
- **홈**: 탭 자리 고정, 트렌딩을 다 보면 최신 글이 이어 붙음(더 보기 버튼 숨김), 오른쪽 칸 고정과 추천 블로그
- **머리말·꼬리말**: 머리말 왼쪽 위에서 프로필 메뉴와 겹치는 내 글·설정 빼기, 좁은 화면 ☰ 메뉴에 이용약관·개인정보 처리방침·릴리스 노트·문의하기(비회원도)
- **글쓰기**: 사진·표 버튼을 위쪽 글쓰기 머리줄로(좁은 화면은 아이콘만), 본문 위 줄 없앰
- DB 바뀜 없음, 테스트 674개 통과

---

## [1.20.0] — 2026-10-13 · 개인 확장 (인스타식 계정·새로 온 작가 한 줄)

- **074 인스타식 계정**: 이메일 → 6자리 코드 → 별명·아이디·비밀번호 → 약관 순서로 가입(같은 카드에 Google·GitHub), 이메일이나 아이디로 로그인, 비밀번호 찾기도 6자리 코드. 블로그 주소(아이디)는 14일에 두 번 바꿀 수 있고 옛 주소로 오면 새 주소로 넘겨 주며, 소셜 접두어(go-·gi-) 없이 아무 주소나 고를 수 있다. 닉네임도 14일에 두 번. 블로그 머리말에 숫자 줄(V18: 주소 변경 기록 표·닉네임 직전 변경 시각, 기존 데이터는 그대로)
- **067 화면 정리 후속**: 홈 "새로 온 작가"를 한 줄로 가로로 밀어 보기
- 끄기 스위치 `blog.insta-account.enabled`
- 테스트 674개 통과

---

## [1.19.0] — 2026-10-13 · 개인 확장 (친한 친구 후속·비공개 계정·비밀 대댓글·화면 정리 후속)

> ⚠️ 되돌릴 수 없는 데이터 변경(069 후속, V16): 1.18.0에서 옮긴 친한 친구 중 내가 팔로우하지 않는 사람과, 친구가 아닌 그룹 구성원을 지운다

- **069 친한 친구 후속**: 친구는 내가 팔로우하는 사람 중에서 고른다(상대가 나를 팔로우하지 않아도 됨, 설정 > 친구에서 주소·별명으로 찾기). 내가 언팔하면 친구와 내 그룹에서도 빠진다
- **073 비공개 계정**: 설정 > 친구 > 🔒 비공개 계정을 켜면 팔로우가 요청이 되고(주인에게 알림), 수락한 팔로워만 글·방명록·시리즈 등을 본다. 수락 전에는 프로필 윗부분과 "비공개 계정이에요"만 보이고, 여러 사람 글 목록(홈·피드·검색 등)에서도 빠진다(V17)
- **072 비밀 대댓글·공유하기 접기**: 보통 댓글에도 🔒 비밀 답글(글쓴이·원댓글 쓴 사람·나만 봄), 공유하기는 좋아요 옆 📤 버튼 하나의 메뉴로
- **067 화면 정리 후속**: 첫 배너 사진 WebP(230KB → 72KB), 홈 인기 태그는 오른쪽 상자 하나만
- 문서: guide를 지금 코드에 맞춤(조회수 30분 5회, 태그 종류, 테스트 수, 모듈 경계 등), deploy.yml 머리 주석 정리
- 테스트 667개 통과

---

## [1.18.0] — 2026-10-13 · 개인 확장 (화면 정리·기능 다듬기·친한 친구·가입 화면·릴리스 노트)

> 10-13 목록 15개를 역할 스레드 넷으로 나눠 작업(1·2·6~15번). 3·4·5번(GitHub 관리·블로그 주소·AI 붙이기)은 설명만

> ⚠️ 되돌릴 수 없는 데이터 변경(069): 서로 친구였던 관계는 양쪽 "친한 친구" 두 줄로 옮기고, 대기 중이던 친구 요청과 그 알림은 지운다(공통 `friendship` 표는 구조를 두고 비움, V15)

- **067 화면 정리**: 내 글·시리즈 글을 카드로, 홈 바로가기 카드(최신·트렌딩 탭과 겹침) 빼기, 카드 목록은 내리면 다음 글이 저절로 붙음([더 보기]는 남김), 꼬리말 꽃 장식을 빼고 여백을 줄임, 첫 배너 사진은 미리 받고 사진(`/images`)도 주소에 내용 지문을 붙여 1년 보관
- **068 기능 다듬기**: 알림 창을 열면 모두 읽음, 피드를 "추천 블로그"(아직 팔로우하지 않은 작가 6명, 팔로우 버튼)와 "팔로잉 블로그 글"로 나눔, AI 태그 추천은 20자부터(전에는 100자), [발행]을 누르면 발행 설정이 팝업으로
- **069 친한 친구**: 서로 요청·수락하던 친구(025)를 바꿔, 나를 팔로우하는 사람 중에서 내가 골라 넣으면(수락·알림 없음) 그 사람이 내 친구 공개 글을 본다. 그룹·미션 친구 글·첫 공개 응원 알림도 같은 기준
- **070 가입 화면 덜어내기**: 로그인과 같은 그림 카드, 비밀번호 규칙은 칸을 누르면 보이고, 주소 안내는 한 줄, 약관은 "모두 동의"(개인정보 동의 때문에 개별 칸은 남김)
- **071 릴리스 노트 화면**: `/releases`가 이 CHANGELOG를 그대로 보여 주고 꼬리말에 링크
- 테스트 661개 통과

---

## [1.17.0] — 2026-10-13 · 개인 확장 (/ 명령·표 넣기·미션 배지)

> 남은 일 6번(기능 추가). 7번(팀 일)은 "최소 요구사항 끝나고 이젠 독립적으로 블로그만드는 단계라 안해도 될듯"으로 하지 않음

- **064 글쓰기 / 명령**: 줄 처음에 `/`를 쓰면 본문·제목·소제목·글머리 목록·번호 목록·할 일 목록·인용·코드 블록·표·구분선·사진을 골라 넣는 창이 뜬다. 글자를 더 쓰면 걸러지고(`/목`, `/표`, `/h2`) ↑↓·Enter로 고르며, Esc로 닫는다. 보이는 대로 쓰기·Markdown 두 칸 모두
- **065 표 넣기**: 글쓰기 [표] 버튼과 `/표`로 3칸 표를 넣는다. 보이는 대로 쓰기 칸에서 Tab은 다음 칸(마지막 칸이면 줄을 하나 더), Shift+Tab은 앞 칸, Enter는 아래 칸·표 밖으로. 저장은 GFM 표 Markdown이고 칸 안 `|`는 `\|`로 지킨다
- **066 미션 배지**: 완료한 날 수·최고 연속으로 정하는 배지 7개(🌱 첫 미션, 🔥 3일 연속, 🌿 미션 10번, ⭐ 일주일 연속, 🌳 미션 30번, 👑 한 달 연속, 🏆 미션 100번). 미션 화면 "내 배지"에서 본인만 본다
- 테스트 659개 통과, 로컬 브라우저로 두 칸 확인

---

## [1.16.0] — 2026-10-13 · 개인 확장 (CSS·JS 브라우저 캐시)

- **CSS·JS 브라우저 캐시**: 화면이 부르는 CSS·JS 주소에 파일 내용 지문을 붙이고(`/css/site-545f….css`) 브라우저가 1년 보관하게 함(`max-age=31536000, public, immutable`). 전에는 새로고침마다 다시 받았음. 배포로 파일이 바뀌면 주소도 바뀌어 바로 새 파일을 받음
- 템플릿은 `th:src="@{/js/…}"`·`th:href="@{/css/…}"`로 씀. 지문 없는 주소를 쓰면 테스트(`StaticCacheIT`)가 실패
- 테스트 658개 통과

---

## [1.15.0] — 2026-10-13 · 개인 확장 (배포 자동 되돌리기)

> 결정: "일단 서버는 1개만 쓰는걸로" — 앱 두 개를 번갈아 쓰는 무중단 배포는 보류하고, 앱 한 개로 할 수 있는 자동 되돌리기만 넣음

- **배포 자동 되돌리기**: 배포 직전에 돌던 앱 이미지를 `team-blog:previous`로 표시해 두고, 새 버전이 3분 안에 켜지지 않으면 그 이미지로 바로 다시 켠다. 사이트는 전 버전으로 계속 동작하고, GitHub Actions는 새 버전이 실패했다고 알린다(새 버전의 마지막 로그 80줄 포함)
- 앱은 한 개라 교체하는 동안 잠깐 끊기는 것은 그대로
- 로컬 도커에서 첫 배포·정상 배포·일부러 망가뜨린 배포로 확인(망가진 배포는 전 버전으로 돌아와 정상 응답). 앱 코드 변경 없음, 테스트 656개 통과

---

## [1.14.1] — 2026-10-13 · 배포 환경 고정

- 배포 워크플로 실행 환경을 `ubuntu-24.04`로 고정(`ubuntu-latest`가 2026-10-19부터 Ubuntu 26으로 바뀌어 검증 없이 환경이 달라지는 것을 막음). 앱 변경 없음

---

## [1.14.0] — 2026-10-12 · 개인 확장 (백엔드·프론트 분리, 깃플로우)

> 피드백: "백엔드 어플리케이션, 프론트 어플리케이션 분리", "배포 전 브랜치를 만들어서 작업 후 머지(깃플로우 전략)"

- **백엔드·프론트 모듈 분리**: 저장소를 `backend/`(Spring Boot 서버·설정·DB 마이그레이션·테스트)와 `frontend/`(Thymeleaf 템플릿·JS·CSS·글꼴·브라우저 라이브러리·화면 JS 검사) 두 Gradle 모듈로 나눔. `frontend` jar가 `backend` 실행 jar에 들어가 배포 단위·서버 설정·사이트 동작은 그대로
- **깃플로우 시작**: `main`(배포) · `develop` · `feature/*` · `release/*` · `hotfix/*`. 이번 릴리스부터 기능 브랜치 → PR → `develop` → `release/X.Y.Z` → PR → `main`(배포·태그) 순서로 진행. README "작업 방식"에 규칙
- 배포 워크플로: `main`·`develop`으로 가는 PR은 테스트만 돌고 배포는 `main`에 머지될 때만
- 설명 문서(배포 순서·Git 규칙·배포 환경·개발 환경·파일 관리·최적화·AI 사용): 문서 저장소 [guide/](https://github.com/AIPKANG/docs/tree/main/guide)
- 테스트 656개 통과

---

## [1.13.0] — 2026-10-12 · 개인 확장 (내 글은 발행 글 먼저, 설정은 섹션 메뉴로)

> 요청: "내 글에서 임시글보다 내글이 더 앞에 나오게 바꿔줘 그리고 설정에서 여러개가 위아래로 나열되어있는데 그거 옆에 섹션별로 묶어주고 누르면 나오는 형식으로 바꿔서 가독성 좋게 만들자"

- [063](https://github.com/AIPKANG/docs/blob/main/specs/063-settings-sections/spec.md) **내 글은 발행 글 먼저**: 내 글 관리 탭이 발행 글 · 임시글 · 휴지통 순서이고, 그냥 열면 발행 글이 보인다
- **설정은 섹션 메뉴로**: 프로필 / 계정 / 블로그·사진 / 친구 / 화면 / 알림 / AI로 묶고, 왼쪽 메뉴(휴대폰은 위쪽 줄)에서 누른 섹션만 보인다. 설정 항목과 동작은 그대로
- 테스트 656개 통과

---

## [1.12.0] — 2026-10-12 · 개인 확장 (빈 임시글이 안 생기게)

- [062](https://github.com/AIPKANG/docs/blob/main/specs/062-no-empty-drafts/spec.md) **빈 임시글이 안 생기게**: [새 글]을 누르고 제목·본문을 아무것도 안 쓴 채 다른 화면으로 가면 그 임시글을 바로 지운다(뒤로 가기는 내 글 관리로). 제목이나 본문 중 하나라도 쓰면 지금처럼 남는다
- [새 글]은 아무것도 안 쓴 임시글이 남아 있으면 새로 만들지 않고 그 글을 다시 연다
- 내 글 관리 임시글 목록·개수에서 빈 임시글(만든 지 2분 지남)을 뺀다. 서버 자동 저장 버퍼에 쓰는 중인 글은 지우지 않는다
- 테스트 656개 통과

---

## [1.11.0] — 2026-10-12 · 개인 확장 (화면 테마를 개인 설정으로)

- [061](https://github.com/AIPKANG/docs/blob/main/specs/061-theme-setting/spec.md) **화면 테마를 개인 설정으로**: 머리말 맨 오른쪽의 다크 모드 버튼을 빼고, 설정 → "화면 테마"에서 시스템 설정 / 라이트 / 다크를 고른다. 고르면 바로 바뀌고 이 기기 브라우저에 저장(로그아웃해도 유지)
- 로그인하지 않은 방문자는 기기 설정을 따른다. 그리기 전 적용·인라인 스크립트 없음 등 공통 024의 나머지는 그대로
- 테스트 654개 통과

---

## [1.10.0] — 2026-10-12 · 개인 확장 (보이는 대로 쓰기)

> 요청: "서식도구 누르면 바로 적용되는 걸 원해, 지금은 ** 이렇게 마크다운형식으로 보여주는데 바꿔줘"

- [060](https://github.com/AIPKANG/docs/blob/main/specs/060-visual-editor/spec.md) **보이는 대로 쓰기**: 글쓰기 본문이 글 화면과 같은 모양으로 보이고, 서식 창 버튼을 누르면 `**` 없이 바로 굵게·색·밑줄·제목·목록·인용·코드 블록이 된다(되돌리기 됨)
- 노션식 단축 입력: 줄 처음에 `#`·`-`·`1.`·`>`·`[]`·```` ``` ```` 다음 띄어쓰기. 코드 블록 안 Enter는 줄바꿈, 빈 줄에서 한 번 더 누르면 밖으로
- 사진은 커서 자리에 한 줄로 들어가고(올리는 중 자리표시), 붙여넣은 글은 위험한 태그를 거른 뒤 서식을 살린다. 할 일 칸은 눌러서 켜고 끈다
- [Markdown으로 쓰기]로 예전 Markdown 칸과 오갈 수 있다(이 기기에 기억). 저장은 그대로 Markdown이라 자동 저장·발행·사진·비교 창은 그대로
- marked·Turndown을 우리 서버(/webjars/)에서 내준다
- 테스트 654개 통과

---

## [1.9.0] — 2026-10-12 · 개인 확장 (노션 모양 서식 창·AI로 글 다듬기)

> 보내 준 노션 선택 메뉴 화면대로 1.8.0의 한 줄 도구를 세로 창으로 바꿨다.

- [058](https://github.com/AIPKANG/docs/blob/main/specs/058-format-toolbar/spec.md) **서식 창**: 글을 고르면 넓은 화면은 본문 오른쪽, 좁으면 글 아래에 창이 뜬다
  - 맨 위 **서식 종류**(본문·제목·소제목·글머리/번호/할 일 목록·인용·코드 블록, 섞였으면 "여러 종류")를 눌러 바꾸기
  - **글자 색·배경색** 9가지(다크 모드 대응), **밑줄**, **서식 지우기** 새로 — 렌더러가 서식 창이 넣는 `<u>`·`<span class="md-…">`만 살리고 정화기가 다시 거른다(RENDER_VERSION 3, XSS 검사 4개 추가)
  - 링크를 고르면 주소를 바꾸거나 빼기, 단축키에 Ctrl/⌘+U 밑줄
- [059](https://github.com/AIPKANG/docs/blob/main/specs/059-writing-ai/spec.md) **AI로 글 다듬기**: 서식 창 아래 "✨ AI로 고치기" — 글 다듬기·맞춤법 고치기·더 짧게·더 쉽게·AI에게 부탁하기(Ctrl/⌘+J). 결과는 [바꾸기]·[아래에 넣기]를 눌러야 들어가고 되돌릴 수 있다. 021과 같은 동의·전체 공개 글만·하루 30번. Gemini 키가 있을 때만 보인다
- 테스트 653개 통과, 서식 규칙 자체 검사 14개

---

## [1.8.0] — 2026-10-12 · 개인 확장 (글쓰기 서식 도구)

- [058](https://github.com/AIPKANG/docs/blob/main/specs/058-format-toolbar/spec.md) **글쓰기 서식 도구**: 노션처럼 본문에서 글을 드래그하면 바로 위에 도구가 뜬다 — 굵게·기울임·취소선·코드·링크 | 제목·소제목·인용·글머리/번호/할 일 목록·코드 블록
- 다시 누르면 서식을 벗기고(버튼이 눌린 모양으로 보임), Ctrl/⌘+Z로 되돌릴 수 있음. 저장은 그대로 Markdown(자동 저장·미리보기 그대로)
- 단축키 Ctrl/⌘ + B·I·K·E, Shift+X(한글 입력 중에도 같은 자리 키)
- 목록 여러 줄을 굵게 하면 줄마다 따로, 목록 표시는 밖에. 링크는 주소만 적으면 https:// 붙여 줌
- 휴대폰에서는 도구가 글 아래에 떠서 기본 복사 메뉴를 가리지 않음
- 테스트 647개 통과, 서식 규칙 자체 검사 11개(`node src/test/js/format-toolbar.test.mjs`)

---

## [1.7.0] — 2026-10-12 · 개인 확장 (오늘의 미션)

> 034 "같은 주제로 쓰기·릴레이"를 요청으로 바꿨다. V14(테이블 2개, 알림 종류 하나).

- [057](https://github.com/AIPKANG/docs/blob/main/specs/057-daily-mission/spec.md) **오늘의 미션**: 매일 주제 3개(0시 1분 AI가 만들고, 없으면 내장 주제 120개에서) → [이 주제로 쓰기]로 글쓰기 창(제목에 주제, 위에 안내) → 그날 안에 발행하면 완료
- 미션 화면: 내 기록(지금까지 N번·연속·최고 연속, 배지는 아직 없음), 친구들의 오늘 글·모두의 오늘 글(댓글 달기), 지난 7일
- 친구가 미션 글(전체·친구 공개)을 올리면 알림 "○○님이 오늘의 미션 글을 올렸어요"(새 글 알림과 겹치지 않음)
- 글 화면에 "🗓 오늘의 미션 · 주제", 글쓴이에게 완료·연속 일수
- 예전 미션 열기·릴레이·글 화면 참여 폼은 없앰(예전 표는 남겨 둠)
- 꼬리말 정원 다듬기(언덕 두 겹, 양 끝 풀숲)
- 테스트 647개 통과

---

## [1.6.0] — 2026-10-12 · 개인 확장 (태그 화면)

- [056](https://github.com/AIPKANG/docs/blob/main/specs/056-tag-icons/spec.md) **태그 화면 리뉴얼**: 고양이 배너, 인기 태그(전체 N개, 글 많은 순·많이 본 순·이름 순), 아이콘 알약
- **태그 아이콘**: 아이콘 41개를 그려 두고 태그마다 내장 규칙으로 바로 정함. Gemini 키가 있으면 AI가 30분마다 다시 골라 다듬음. V13
- 꼬리말 아래 작은 정원 장식, 홈 오른쪽 인기 태그도 아이콘 알약
- 테스트 645개 통과

---

## [1.5.0] — 2026-10-12 · 개인 확장 (카드 대표 사진)

- [055](https://github.com/AIPKANG/docs/blob/main/specs/055-card-cover/spec.md) **카드 대표 사진**: 글쓰기 발행 설정에서 자동(본문 첫 사진) / 본문 사진 고르기 / 새 사진 올리기. 목록 카드·검색·공유 미리보기에 쓰인다. V12(테이블 1개)
- 홈 가로 카드: 사진 칸 폭과 아래 줄 정리
- 테스트 642개 통과

---

## [1.4.1] — 2026-10-12 · 개인 확장 (홈 리뉴얼)

- 시안대로 홈 배치: 바로가기 카드(인기 글·최신 글·관심 태그), 본문 2단(최신 글 가로 카드 2열 + 오른쪽 인기 태그·고양이 카드)
- 머리말: 지금 위치 밑줄, 펜 아이콘 [새 글], 내 프로필 메뉴(내 블로그·내 글 관리·통계·설정·로그아웃)
- 꼬리말: 이용약관(`/terms` 새로)·개인정보 처리방침·문의하기

---

## [1.4.0] — 2026-10-12 · 개인 확장 (블로그 기능)

> **개인 확장**이다. V11 마이그레이션(테이블 2개).

| 명세 | 기능 |
|---|---|
| [051](https://github.com/AIPKANG/docs/blob/main/specs/051-blog-visitors/spec.md) | **방문자 수** — 블로그 머리에 오늘·전체 |
| [052](https://github.com/AIPKANG/docs/blob/main/specs/052-read-progress/spec.md) | **읽기 진행 막대** — 스크롤하면 얼마나 읽었는지 맨 위 막대로 |
| [053](https://github.com/AIPKANG/docs/blob/main/specs/053-share/spec.md) | **공유하기** — 링크 복사·X·페이스북·다른 앱(카카오톡 등) |
| [054](https://github.com/AIPKANG/docs/blob/main/specs/054-pin-post/spec.md) | **글 고정** — 블로그 맨 위 📌 고정 글 |

---

## [1.3.0] — 2026-10-12 · 개인 확장 (관리자 화면)

> **개인 확장**이다. 공통 022(신고·회원 정지) 화면 위에 관리 도구를 더했다. V10 마이그레이션(테이블 2개).

| 명세 | 기능 |
|---|---|
| [045](https://github.com/AIPKANG/docs/blob/main/specs/045-admin-dashboard/spec.md) | **대시보드** `/admin` — 전체·오늘 숫자, 처리할 신고, 최근 7일 그래프 |
| [046](https://github.com/AIPKANG/docs/blob/main/specs/046-admin-members/spec.md) | **회원 관리 강화** — 이메일 검색, 이메일·가입일·글 수 |
| [047](https://github.com/AIPKANG/docs/blob/main/specs/047-admin-content/spec.md) | **글·댓글 관리** — 찾기, 신고 없이 숨기기·해제 |
| [048](https://github.com/AIPKANG/docs/blob/main/specs/048-admin-guestbook/spec.md) | **방명록 관리** — 전체 방명록 보기·지우기 |
| [049](https://github.com/AIPKANG/docs/blob/main/specs/049-site-notice/spec.md) | **사이트 공지** — 모든 화면 위 띠 |
| [050](https://github.com/AIPKANG/docs/blob/main/specs/050-admin-log/spec.md) | **관리 기록·관리자 지정** — 누가 언제 무엇을, `ADMIN_EMAILS`로 첫 관리자 |

---

## [1.2.0] — 2026-10-12 · 개인 확장 (티스토리 참고)

> **개인 확장**이다. 과제 참고 사이트(티스토리)에 있는 기능 8개를 더했다. 공통 완료 기준(001~024)은 그대로 통과한다. 통합 테스트 635개 통과.
> 기능마다 끄는 설정이 있다(README 표). DB는 V9 마이그레이션으로 테이블 6개를 더하고 `comment`에 `secret` 칸만 더했다.

| 명세 | 기능 | 상태 |
|---|---|---|
| [037](https://github.com/AIPKANG/docs/blob/main/specs/037-categories/spec.md) | **카테고리** — 설정에서 만들고·순서·이름 바꾸기, 글마다 하나, 블로그에서 카테고리별 보기 | **구현 완료** |
| [038](https://github.com/AIPKANG/docs/blob/main/specs/038-post-navigation/spec.md) | **이전 글·다음 글 + 관련 글** — 같은 블로그 앞뒤 글, 태그가 겹치는 글 3개 | **구현 완료** |
| [039](https://github.com/AIPKANG/docs/blob/main/specs/039-toc/spec.md) | **목차 자동 생성** — 소제목 2개 이상이면 글 위(넓은 화면은 오른쪽)에 목차 | **구현 완료** |
| [040](https://github.com/AIPKANG/docs/blob/main/specs/040-guestbook/spec.md) | **방명록** — 300자 인사, 비밀글, 쓴 사람·주인이 지우기 | **구현 완료** |
| [041](https://github.com/AIPKANG/docs/blob/main/specs/041-series/spec.md) | **시리즈** — 글 묶기, (n/전체)·이전편·다음편, 시리즈 목록 | **구현 완료** |
| [042](https://github.com/AIPKANG/docs/blob/main/specs/042-scheduled-publish/spec.md) | **예약 발행** — 날짜·시각을 정하면 그때 발행, 내 글 관리에 "⏰ 예약" | **구현 완료** |
| [043](https://github.com/AIPKANG/docs/blob/main/specs/043-secret-comments/spec.md) | **비밀 댓글** — 글쓴이와 쓴 사람(답글은 위 댓글 쓴 사람도)만 보기 | **구현 완료** |
| [044](https://github.com/AIPKANG/docs/blob/main/specs/044-rss/spec.md) | **RSS 구독** — 전체·블로그별 공개 글 최신 20개 | **구현 완료** |

### 그 밖에 (10-10~10-12)

- 사이트 이름 **글그린**, 학교 서버 도메인 `geulgreen.java21.net`(포트 8300)
- 머리말 정리: 검색은 오른쪽 돋보기 버튼, 알림은 종 아이콘
- 글쓰기 태그 입력을 인스타그램처럼(한 상자, 띄어쓰기·붙여 넣기로 태그)
- 로그인·오류 화면 고양이를 수채화 그림으로
- 학교 공용 DB에서 `pg_trgm` 확장 권한이 없으면 확장·trgm 인덱스 없이 설치(검색은 그대로 동작)
- 배포 워크플로는 소스 레포(practicech/blog)에서만 돈다

---

## [1.1.0] — 2026-10-09 · 개인 확장

> 여기부터는 **개인 확장**이다. 팀 공통 범위가 아니며, 공통 완료 기준(001~024)은 그대로 통과한다. 통합 테스트 628개 통과.
> 확장마다 끄는 설정이 있다(README "개인 확장" 표). DB는 V2~V8 마이그레이션으로 테이블만 더하고 공통 테이블은 CHECK 교체만 했다.
> 후보 전체와 순서: [`specs/personal-extensions.md`](https://github.com/AIPKANG/docs/blob/main/specs/personal-extensions.md)

| 명세 | 기능 | 상태 |
|---|---|---|
| [025](https://github.com/AIPKANG/docs/blob/main/specs/025-friends-visibility/spec.md) | **친구 공개** — 친구 요청·수락·거절(상대에게 드러나지 않음), 친구만 보는 글, 친구가 보는 블로그 목록, 끊으면 즉시 404, 탈퇴 정리 | **구현 완료** |
| [026](https://github.com/AIPKANG/docs/blob/main/specs/026-public-switch/spec.md) | **공개 전환** — 공개 범위를 넓힐 때 확인 창, 처음 공개되면 홈 맨 위, 친구에게 첫 공개 응원 알림 | **구현 완료** |
| [027](https://github.com/AIPKANG/docs/blob/main/specs/027-view-dedupe-30m/spec.md) | **조회수 30분 5회** — 같은 사람·같은 글을 30분에 5번까지 센다(공통은 24시간 1회) | **구현 완료** |
| [028](https://github.com/AIPKANG/docs/blob/main/specs/028-reply-depth/spec.md) | **답글 무제한 깊이** — 답글에 답글, 3단계까지 펼치고 더 깊으면 "답글 N개 더보기" | **구현 완료** |
| [029](https://github.com/AIPKANG/docs/blob/main/specs/029-link-visibility/spec.md) | **링크 공개** — 주소(열쇠)를 아는 사람만 읽기, 목록·검색 노출 없음, 새 링크로 예전 주소 막기 | **구현 완료** |
| [030](https://github.com/AIPKANG/docs/blob/main/specs/030-group-visibility/spec.md) | **그룹 공개** — 친구를 그룹으로 묶고, 고른 그룹에만 보이는 글 | **구현 완료** |
| [031](https://github.com/AIPKANG/docs/blob/main/specs/031-activity-streak/spec.md) | **잔디·스트릭** — 블로그에 최근 1년 기록 칸, 연속 기록 일수(공개 활동만) | **구현 완료** |
| [032](https://github.com/AIPKANG/docs/blob/main/specs/032-author-stats/spec.md) | **작성자 통계** — 합계, 최근 30일 조회 그래프, 많이 읽힌 글 5개(본인만) | **구현 완료** |
| [033](https://github.com/AIPKANG/docs/blob/main/specs/033-short-notes/spec.md) | **짧은 기록** — 280자 한 줄 기록(전체·친구·나만), 블로그에 최근 3개, 모아 보기 | **구현 완료** |
| [034](https://github.com/AIPKANG/docs/blob/main/specs/034-missions/spec.md) | **같은 주제로 쓰기·릴레이** — 미션 열기, 내 공개 글로 참여, 참여 순서대로 릴레이 | **구현 완료** |
| [035](https://github.com/AIPKANG/docs/blob/main/specs/035-ai-teaser/spec.md) | **AI 티저** — AI 제안 또는 직접 쓴 한 줄 소개를 카드 요약 자리에 | **구현 완료** |
| [036](https://github.com/AIPKANG/docs/blob/main/specs/036-home-newcomers/spec.md) | **홈 "새로 온 작가"** — 최근 2주 첫 글을 올린 사람과 그 첫 글 | **구현 완료** |

### 공통 쪽 고친 것(1.0.0 알려진 문제)

- 저장 공간 초과 안내를 "7일 뒤 돌아와요"로, 휴지통 복구 뒤 "복구했어요" 안내, 입력 칸·버튼 테두리 대비 3:1, `sitemap.xml`·`robots.txt`(공개 글만), 금칙어 기본 목록, 인증 전 댓글 칸의 [인증 메일 다시 보내기], 코드 블록 있을 때만 문법 강조 불러오기, 발행 화면에서 친구·링크·그룹 공개도 고를 수 있게.

그 밖에 **한 쪽 카드 12개**(시안의 한 줄 4개에 맞춤, 공통은 9개)도 개인 확장이다. 만들지 않은 후보: WYSIWYG 편집기(공통 Markdown 정화 흐름을 흔들어 하지 않음), 태그 글당 5개(공통 10 유지).

---

## [1.0.0] — 2026-10-09 · 팀 공통 최소 요구사항

팀 설계 문서(docs/ 32개)를 Spec Kit 명세 001~024로 옮겨 모두 구현했다. 통합 테스트 607개 통과.

### 기능 (명세 번호)

- **회원**: 이메일 가입·인증, Google·GitHub 로그인(001), 블로그 주소·닉네임(002), 프로필·설정(003), 탈퇴·30일 안 복구(023)
- **글쓰기**: 자동 저장 3단계·충돌 비교(004), 발행·다시 발행(005), 공개 범위 전체 공개/나만 보기(006), Markdown 정화(007), 사진 올리기(008), 태그(013), AI 태그 추천(021)
- **읽기**: 전체 글·개인 블로그(009), 글 상세(010), 내 글 관리·휴지통 30일(011), 트렌딩(019), 검색(020), 다크 모드(024), 코드 문법 강조
- **반응**: 댓글·답글(014), 좋아요(015), 조회수(016), 인앱 알림(017), 팔로우·피드(018)
- **운영**: 접근 권한 공통 규칙(012), 신고·관리자 숨김·회원 정지(022), 개인정보 처리방침(`/privacy`)

### 화면 디자인 (10-08)

- 크림 배경·세이지 버튼·둥근 카드 시안 적용, 색 역할 이름은 그대로(다크 배경 `#121212` 유지)
- 글꼴 Pretendard·Gaegu를 우리 서버에서 제공(브라우저에 1년 보관)
- 홈 사진 배너 + 검색창, 한 줄 4개 카드, 카드 첫 태그, 사진 없는 카드는 빈 칸 없음
- 로그인·가입 카드, 블로그 머리 띠, 403·404·500 화면, 휴대폰 ☰ 메뉴

### 운영 준비

- 상태 확인(`/actuator/health`), 백업·복구 스크립트, 로그 크기 제한
- 학교 공용 서버 배포: GitHub Actions(테스트 → 이미지 → SSH 실행), Crowfoot PostgreSQL, 서버 공용 Nginx(`deploy/school/`)
- 사진은 서버 디스크에 먼저 저장, 서버를 늘릴 때 MinIO로 설정만 바꿔 확장
- 데모 데이터(`db/demo/`), Crowfoot ERD(테이블 20개·관계 41개, 명세와 연결)

### 확인한 것

- 성능: 글 10만 건에서 목록·상세·검색 p95 100ms 아래, 20명 동시 접속 p95 49ms·오류 0 (`specs/qa/2026-10-08-performance.md`)
- 요구사항 반영 점검: 약 485개 중 약 441개 그대로, 약 45개 팀 결정으로 다르게 (`specs/qa/2026-10-09-requirements-audit.md`)

### 알려진 문제 (다음 버전에서 고칠 것)

- 저장 공간 초과 안내가 "다음 날"인데 실제로는 7일 뒤 돌아옴
- 휴지통 복구 뒤 "복구했어요" 안내 없음
- 입력 칸·버튼 테두리 대비가 3:1보다 낮음
- 탈퇴할 때 브라우저 임시 저장본을 지우지 않음
- sitemap 없음, 금칙어 목록이 임시본
- (비공개 → 공개 확인 창은 개인 확장 026에서 다룸)

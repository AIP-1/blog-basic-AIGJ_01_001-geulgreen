# Feature Specification: 블로그 방문자 수

**Feature Branch**: `051-blog-visitors` | **Created**: 2026-10-12 | **Status**: Implemented

**Scope**: **강성찬 개인 확장** (10-12 chan 요청). 끄는 설정 있음.

## Requirements
- **FR-001** 블로그 머리에 "오늘 N · 전체 N". 블로그 첫 화면·글 화면을 본 사람을 하루 한 번 센다(조회수 016과 같은 방문자 키), 블로그 주인 자신은 세지 않는다. Redis가 멈추면 세지 않는다. 설정 `blog.visits.enabled`.

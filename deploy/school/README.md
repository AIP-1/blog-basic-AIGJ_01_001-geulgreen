# 학교 공용 서버 배포 (서버 배포 설명서 10-08)

`main`에 올리면 GitHub Actions가 **테스트 → 도커 이미지 생성 → SSH로 서버에 보내 `run-on-server.sh` 실행**까지 한다.
서버에는 우리 앱 컨테이너 두 대(`APP_PORT`, `APP_PORT`+1)와 Redis 컨테이너만 뜬다(093 무중단 배포). DB는 Crowfoot PostgreSQL, HTTPS·도메인은 서버의 공용 Nginx가 맡는다.

## 0. 워크플로 파일 넣기

배포 워크플로는 `.github/workflows/deploy.yml`이다(`deploy/school/github-deploy.yml`은 같은 내용의 사본). 10-12 소스·문서 저장소를 나눈 뒤로 문서 저장소(AIPKANG/docs)에는 소스가 없다.
**소스코드 레포**에 `.github/workflows/deploy.yml`로 복사해서 올린다. 개인 접근 토큰(PAT)으로 push하면 토큰에 `workflow` 권한이 있어야 한다(없으면 "without `workflow` scope"로 거절). 권한을 늘리기 어렵다면 GitHub 웹에서 Add file → Create new file로 같은 경로에 붙여 넣어도 된다.

## 1. GitHub Repository secrets (Settings → Secrets and variables → Actions)

| 이름 | 넣을 값 |
|---|---|
| `SSH_ADDRESS` | aip 공통 인프라 문서의 서버 주소(학교 와이파이: 내부망, 핫스팟: 외부) |
| `SSH_PORT` | `8822` |
| `SSH_ID` | 문서에서 자기 계정 |
| `SSH_PASSWORD` | 문서 최상단 공용 비밀번호 |
| `APP_PORT` | 받은 포트 번호(예: 8300). 두 번째 앱은 자동으로 +1(8301), Nginx upstream에 둘 다 적는다 |
| `APP_DOMAIN` | 받은 도메인(예: `blogcabin.java21.net`, https 없이) |
| `DB_ADDRESS` | Crowfoot 데이터베이스 탭 맨 위 주소에서 `:숫자`를 뺀 부분 |
| `DB_PORT` | 그 주소 뒤의 숫자 |
| `DB_NAME` | 데이터베이스 |
| `DB_USERNAME` | 계정 |
| `DB_PASSWORD` | 비번 |
| `DB_SCHEMA` | 스키마 탭의 스키마(이 프로젝트: `cf_u39_d1`) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | `.env`의 Gmail 주소·앱 비밀번호 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | `.env` 값 |
| `GH_CLIENT_ID`, `GH_CLIENT_SECRET` | `.env`의 GITHUB_CLIENT_ID·SECRET 값 (GitHub는 GITHUB_로 시작하는 Secret 이름을 막아서 GH_로 넣는다) |
| `GEMINI_API_KEY` | (선택) AI 태그 추천 |
| `ADMIN_EMAILS` | (선택) 이 이메일로 가입·인증한 계정을 관리자로(쉼표로 여러 개). 지정된 뒤 다시 로그인해야 [관리]가 보인다 |

사진 주소 서명용 비밀값(`STORAGE_LOCAL_SECRET`)은 서버가 처음 배포할 때 스스로 만들어 `~/team-blog/storage-secret`에 보관한다.

## 2. 서버의 Nginx 설정 파일

`~/nginx/<도메인>.conf`를 이 폴더의 `geulgreen.java21.net.conf`로 바꾼다(다른 도메인이면 `server_name`·인증서 경로·로그 경로·upstream 포트를 바꿈). 바꾼 뒤 `nginx -t`로 확인하고 reload한다.
- upstream에 앱 두 대(8300, 8301)를 묶고, 한 대가 꺼져 있으면 다른 대로 한 번만 다시 보낸다(`proxy_next_upstream_tries 2`, 연결은 3초에 포기).
- `map`·`upstream` 이름에 `geulgreen_`을 붙인 것은 공용 Nginx에서 다른 팀 설정과 이름이 겹치면 `nginx -t`가 실패하기 때문이다.
- HTTP/2는 주석으로 둔다. 배포 로그의 `nginx -v`가 1.25.1 이상이면 `http2 on;`의 #을 지운다.
- HSTS·`X-Content-Type-Options`는 앱(Spring Security)이 이미 보내므로 Nginx에서 또 붙이지 않는다.

앱은 `X-Forwarded-Proto`·`X-Forwarded-For`를 믿으므로(https 판단·접속 IP), 설정에 아래 줄이 없으면 `location /` 안에 넣는다.

```
proxy_set_header Host $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
client_max_body_size 12m;
```

## 3. 로그인 콜백 주소

Google·GitHub 개발자 콘솔에 운영 주소를 추가한다.
- Google: `https://<도메인>/login/oauth2/code/google`
- GitHub: `https://<도메인>/login/oauth2/code/github`

## 4. 배포·확인

- 자동: `main`에 push. 수동: GitHub Actions → deploy → Run workflow(급하면 테스트 건너뛰기).
- 서버에서 상태: `docker ps`, 로그: `docker logs --tail 100 team-blog-app`(두 번째 대는 `team-blog-app-2`)
- **무중단 배포(093)**: 2번 대(`team-blog-app-2`)를 먼저 새 버전으로 바꿔 `/actuator/health`가 답할 때까지 기다리고, 15초 뒤(Nginx가 2번을 다시 쓰기 시작한 뒤) 1번 대를 바꾼다. 내릴 때는 `docker stop -t 30`으로 받던 요청을 끝내게 하고, 새 요청은 Nginx가 다른 대로 보낸다. 컨테이너는 `127.0.0.1`에만 열고 한 대당 메모리 1GB(`APP_MEMORY`)로 묶는다.
- **자동 되돌리기(1.15.0)**: 배포 직전에 돌고 있던 앱 이미지를 `team-blog:previous`로 표시해 두고, 새 버전이 3분 안에 답하지 않으면 그 대만 그 이미지로 다시 켜고 멈춘다(다른 대는 손대지 않음). Actions는 빨간 X로 알린다(로그에 새 버전의 마지막 80줄).
- **두 대가 같이 돌 때**: 예약 작업은 Redis 잠금(`JobLock`)으로 한 대에서만 돈다. 세션·조회수·요청 제한은 Redis에 있어 어느 대가 받아도 같다. 교체 중에는 새 버전이 마이그레이션한 DB에서 옛 버전이 잠깐 같이 돌므로, 마이그레이션은 지금처럼 "추가만" 한다(컬럼·테이블 삭제나 이름 바꾸기는 다음 릴리스로 미룬다).
- 손으로 전 버전으로 돌리기: `docker rm -f team-blog-app` 후 스크립트의 `docker run`과 같은 옵션으로 이미지 이름만 `team-blog:previous`로 바꿔 실행. 보통은 GitHub에서 문제 커밋을 `git revert`하는 PR을 머지해 다시 배포하는 편이 낫다.
- 처음 켜질 때 Crowfoot 스키마에 테이블 20개가 만들어진다(Flyway). 검색에 쓰는 `pg_trgm` 확장이 DB에 없고 만들 권한도 없으면 이때 실패하니 로그를 본다.
- 실패하면 GitHub inbox(오른쪽 위 프로필 왼쪽 아이콘)의 오류를 복사해 Claude에게 보낸다.

로컬에서 같은 스크립트를 가짜 Crowfoot DB(스키마만 있는 PostgreSQL)에 돌려 테이블 생성·홈·검색이 되는 것을 확인했다(10-08).

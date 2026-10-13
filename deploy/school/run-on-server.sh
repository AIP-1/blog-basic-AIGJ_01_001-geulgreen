#!/usr/bin/env bash
# 학교 공용 서버 배포(서버 배포 설명서 10-08): GitHub Actions가 이미지 파일과 함께 이 스크립트를 서버로 보내 실행한다.
# - DB는 Crowfoot PostgreSQL(밖), Nginx는 서버 공용 Nginx(~/nginx/<도메인>.conf → APP_PORT)
# - 이 스크립트는 우리 것만 띄운다: Redis 컨테이너 + 앱 컨테이너(APP_PORT로만 열림), 사진은 Docker 볼륨
# - 자동 되돌리기(1.15.0): 새 버전이 3분 안에 안 켜지면 바로 전 이미지(team-blog:previous)로 다시 켠다.
#   앱은 한 개만 쓰므로 교체하는 동안 잠깐 끊기는 것은 그대로다. 마이그레이션은 "추가만" 하므로 전 버전도 새 DB에서 켜진다.
# - 비밀값은 Actions가 환경 변수로 넘겨주고, 서버에는 0600 권한 env 파일로만 남긴다(다시 시작할 때 필요)
# 필요한 환경 변수: APP_PORT APP_DOMAIN DB_ADDRESS DB_PORT DB_NAME DB_USERNAME DB_PASSWORD DB_SCHEMA
#   MAIL_USERNAME MAIL_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET GITHUB_CLIENT_ID GITHUB_CLIENT_SECRET
#   (선택) GEMINI_API_KEY, ADMIN_EMAILS
set -euo pipefail

APP_NAME="${APP_NAME:-team-blog}"
WORK="${HOME}/${APP_NAME}"
IMAGE_TAR="${WORK}/image.tar.gz"
NET="${APP_NAME}-net"
REDIS="${APP_NAME}-redis"
APP="${APP_NAME}-app"

for v in APP_PORT APP_DOMAIN DB_ADDRESS DB_PORT DB_NAME DB_USERNAME DB_PASSWORD DB_SCHEMA \
         MAIL_USERNAME MAIL_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET GITHUB_CLIENT_ID GITHUB_CLIENT_SECRET; do
  if [ -z "${!v:-}" ]; then echo "환경 변수 ${v}가 비어 있어요(GitHub Secrets 확인)" >&2; exit 1; fi
done

DOCKER="docker"
if ! docker info > /dev/null 2>&1; then DOCKER="sudo -n docker"; fi
${DOCKER} info > /dev/null 2>&1 || { echo "docker를 쓸 수 없어요(설치·권한 확인)" >&2; exit 1; }

mkdir -p "${WORK}"
chmod 700 "${WORK}"

# 사진 서명 비밀값: 처음 한 번 만들어 서버에만 둔다(바뀌면 예전 사진 주소가 깨지므로 유지)
if [ ! -s "${WORK}/storage-secret" ]; then
  (umask 077; head -c 48 /dev/urandom | base64 | tr -d '\n=+/' > "${WORK}/storage-secret")
fi

# 앱 환경 변수 파일(0600). 값에 줄바꿈이 없다는 전제(키·비밀번호)
ENV_FILE="${WORK}/app.env"
(
  umask 077
  {
    echo "SPRING_PROFILES_ACTIVE=prod"
    echo "DB_URL=jdbc:postgresql://${DB_ADDRESS}:${DB_PORT}/${DB_NAME}?currentSchema=${DB_SCHEMA},public"
    echo "DB_USERNAME=${DB_USERNAME}"
    echo "DB_PASSWORD=${DB_PASSWORD}"
    echo "SPRING_FLYWAY_DEFAULT_SCHEMA=${DB_SCHEMA}"
    echo "SPRING_FLYWAY_SCHEMAS=${DB_SCHEMA}"
    echo "REDIS_HOST=${REDIS}"
    echo "REDIS_PORT=6379"
    echo "APP_BASE_URL=https://${APP_DOMAIN}"
    echo "MAIL_USERNAME=${MAIL_USERNAME}"
    echo "MAIL_PASSWORD=${MAIL_PASSWORD}"
    echo "GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}"
    echo "GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}"
    echo "GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}"
    echo "GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}"
    echo "GEMINI_API_KEY=${GEMINI_API_KEY:-}"
    echo "BLOG_ADMIN_EMAILS=${ADMIN_EMAILS:-}"
    echo "STORAGE_TYPE=local"
    echo "STORAGE_LOCAL_DIR=/app/data"
    echo "STORAGE_LOCAL_SECRET=$(cat "${WORK}/storage-secret")"
  } > "${ENV_FILE}"
)

# 지금 돌고 있는 앱의 이미지를 previous로 표시해 둔다(새 버전이 안 켜지면 이걸로 다시 켠다)
PREV_ID="$(${DOCKER} inspect -f '{{.Image}}' "${APP}" 2>/dev/null || true)"
if [ -n "${PREV_ID}" ]; then ${DOCKER} tag "${PREV_ID}" "${APP_NAME}:previous"; fi

echo "== 이미지 불러오기"
${DOCKER} load -i "${IMAGE_TAR}"
rm -f "${IMAGE_TAR}"

${DOCKER} network inspect "${NET}" > /dev/null 2>&1 || ${DOCKER} network create "${NET}" > /dev/null

echo "== Redis"
if ! ${DOCKER} ps --format '{{.Names}}' | grep -qx "${REDIS}"; then
  ${DOCKER} rm -f "${REDIS}" > /dev/null 2>&1 || true
  ${DOCKER} run -d --name "${REDIS}" --network "${NET}" --restart unless-stopped \
    -v "${APP_NAME}-redis-data:/data" --log-opt max-size=10m --log-opt max-file=3 \
    redis:8 redis-server --appendonly yes > /dev/null
fi

run_app() { # $1 = 이미지
  ${DOCKER} rm -f "${APP}" > /dev/null 2>&1 || true
  ${DOCKER} run -d --name "${APP}" --network "${NET}" --restart unless-stopped \
    -p "${APP_PORT}:8080" --env-file "${ENV_FILE}" \
    -v "${APP_NAME}-data:/app/data" --log-opt max-size=20m --log-opt max-file=5 \
    "$1" > /dev/null
}

wait_healthy() { # 5초마다, 최대 3분
  for i in $(seq 1 36); do
    if curl -fsS "http://127.0.0.1:${APP_PORT}/actuator/health" > /dev/null 2>&1; then return 0; fi
    sleep 5
  done
  return 1
}

echo "== 앱 교체"
run_app "${APP_NAME}:latest"

echo "== 상태 확인(최대 3분)"
if wait_healthy; then
  echo "앱이 켜졌어요: http://127.0.0.1:${APP_PORT} → https://${APP_DOMAIN}"
  ${DOCKER} image prune -f > /dev/null 2>&1 || true
  exit 0
fi
echo "새 버전이 3분 안에 켜지지 않았어요. 마지막 로그:" >&2
${DOCKER} logs --tail 80 "${APP}" >&2 || true
if [ -z "${PREV_ID}" ]; then exit 1; fi # 처음 배포라 되돌릴 버전이 없음

echo "== 전 버전으로 되돌리기" >&2
run_app "${APP_NAME}:previous"
if wait_healthy; then
  echo "전 버전으로 다시 켰어요. 사이트는 전 버전으로 동작해요. 새 버전은 위 로그를 보고 고쳐서 다시 배포해요." >&2
else
  echo "전 버전도 켜지지 않았어요(DB·Redis 같은 서버 쪽 문제일 수 있어요). 마지막 로그:" >&2
  ${DOCKER} logs --tail 80 "${APP}" >&2 || true
fi
exit 1 # 되돌렸어도 새 버전은 실패했으니 Actions에 빨간 X로 알린다

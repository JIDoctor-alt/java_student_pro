#!/usr/bin/env bash
# 在服务器上拉取 GitHub 代码后执行本脚本完成构建与发布。
# 用法:
#   cd /opt/code-student-pro
#   git pull origin feature-develop
#   bash deploy/deploy-on-server.sh

set -euo pipefail

REPO_DIR="${REPO_DIR:-/opt/code-student-pro}"
BRANCH="${DEPLOY_BRANCH:-feature-develop}"
DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-}"

cd "$REPO_DIR"

echo "[deploy] repo: $REPO_DIR (branch=$BRANCH)"

if [ -d .git ]; then
  git fetch origin
  git checkout "$BRANCH"
  git pull origin "$BRANCH"
else
  echo "[deploy] 当前目录不是 git 仓库，请先 clone："
  echo "  git clone -b $BRANCH https://github.com/JIDoctor-alt/java_student_pro.git $REPO_DIR"
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  if command -v yum >/dev/null 2>&1; then
    yum install -y java-21-openjdk java-21-openjdk-devel || yum install -y java-17-openjdk
  elif command -v apt-get >/dev/null 2>&1; then
    apt-get update && apt-get install -y openjdk-21-jdk || apt-get install -y openjdk-17-jdk
  fi
fi

if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://rpm.nodesource.com/setup_20.x | bash - 2>/dev/null || true
  if command -v yum >/dev/null 2>&1; then yum install -y nodejs; else apt-get install -y nodejs npm; fi
fi

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable docker && systemctl start docker
fi

if ! command -v nginx >/dev/null 2>&1; then
  if command -v yum >/dev/null 2>&1; then yum install -y nginx; else apt-get install -y nginx; fi
fi

echo "[deploy] build backend..."
cd "$REPO_DIR/server/server"
chmod +x mvnw
./mvnw -q clean package -Dmaven.test.skip=true
JAR="$REPO_DIR/server/server/target/server-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
  echo "[deploy] jar not found: $JAR"
  exit 1
fi
cp "$JAR" "$REPO_DIR/server.jar"

echo "[deploy] build frontend..."
cd "$REPO_DIR/client"
if [ -f package-lock.json ]; then
  npm ci
else
  npm install
fi
npm run build-only

mkdir -p "$REPO_DIR/frontend"
rm -rf "$REPO_DIR/frontend"/*
cp -r "$REPO_DIR/client/dist/"* "$REPO_DIR/frontend/"

echo "[deploy] start mysql/redis..."
cd "$REPO_DIR/deploy"
mkdir -p data/mysql data/redis sql
cp -f "$REPO_DIR/server/server/src/main/resources/sql/"*.sql sql/ 2>/dev/null || true
if docker compose version >/dev/null 2>&1; then
  docker compose up -d
else
  docker-compose up -d
fi
sleep 10

echo "[deploy] configure systemd + nginx..."
mkdir -p /etc/nginx/conf.d /etc/systemd/system
cp -f "$REPO_DIR/deploy/nginx-code-student-pro.conf" /etc/nginx/conf.d/code-student-pro.conf
cp -f "$REPO_DIR/deploy/code-student-pro.service" /etc/systemd/system/code-student-pro.service

if [ -n "$DEEPSEEK_API_KEY" ]; then
  sed -i "s|^Environment=DEEPSEEK_API_KEY=.*|Environment=DEEPSEEK_API_KEY=$DEEPSEEK_API_KEY|" /etc/systemd/system/code-student-pro.service
fi

rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true
systemctl daemon-reload
systemctl enable code-student-pro
systemctl restart code-student-pro
nginx -t
systemctl enable nginx
systemctl restart nginx

echo "[deploy] health check..."
sleep 5
curl -sf "http://127.0.0.1/api/app/good/list/page/vo" \
  -H 'Content-Type: application/json' \
  -d '{"pageNum":1,"pageSize":1}' >/dev/null && echo "[deploy] API ok" || echo "[deploy] API check failed, see: journalctl -u code-student-pro -n 50"

echo "[deploy] done: http://124.70.195.81/"

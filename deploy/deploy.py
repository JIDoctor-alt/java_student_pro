#!/usr/bin/env python3
"""一键部署 CODE 原创项目到远程服务器。

用法（PowerShell）:
  $env:DEPLOY_HOST='124.70.195.81'
  $env:DEPLOY_USER='root'
  $env:DEPLOY_PASSWORD='你的密码'
  $env:DEEPSEEK_API_KEY='你的DeepSeek密钥'
  python deploy/deploy.py
"""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

try:
    import paramiko
except ImportError:
    print("请先安装 paramiko: pip install paramiko")
    sys.exit(1)

ROOT = Path(__file__).resolve().parent.parent
DEPLOY_DIR = ROOT / "deploy"
REMOTE_DIR = "/opt/code-student-pro"

HOST = os.environ.get("DEPLOY_HOST", "124.70.195.81")
USER = os.environ.get("DEPLOY_USER", "root")
PASSWORD = os.environ.get("DEPLOY_PASSWORD", "")
DEEPSEEK_KEY = os.environ.get("DEEPSEEK_API_KEY", "")


def run_local(cmd: list[str], cwd: Path | None = None) -> None:
    print(f"\n[local] {' '.join(cmd)}")
    subprocess.check_call(cmd, cwd=cwd or ROOT)


def build_artifacts() -> tuple[Path, Path]:
    mvnw = ROOT / "server" / "server" / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    run_local([str(mvnw), "-q", "clean", "package", "-Dmaven.test.skip=true"], cwd=ROOT / "server" / "server")
    jar = ROOT / "server" / "server" / "target" / "server-0.0.1-SNAPSHOT.jar"
    if not jar.exists():
        raise FileNotFoundError(f"未找到 jar: {jar}")

    npm = "npm.cmd" if os.name == "nt" else "npm"
    run_local([npm, "run", "build"], cwd=ROOT / "client")
    dist = ROOT / "client" / "dist"
    if not dist.exists():
        raise FileNotFoundError("前端 dist 未生成")
    return jar, dist


def upload_and_deploy(jar: Path, dist: Path) -> None:
    if not PASSWORD:
        raise ValueError("请设置环境变量 DEPLOY_PASSWORD")

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    print(f"\n[remote] 连接 {USER}@{HOST} ...")
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)

    def exec_cmd(cmd: str) -> None:
        print(f"[remote] {cmd}")
        stdin, stdout, stderr = client.exec_command(cmd, get_pty=True)
        out = stdout.read().decode(errors="ignore")
        err = stderr.read().decode(errors="ignore")
        if out.strip():
            print(out.strip().encode("gbk", errors="ignore").decode("gbk", errors="ignore"))
        if err.strip():
            print(err.strip().encode("gbk", errors="ignore").decode("gbk", errors="ignore"))

    exec_cmd(f"mkdir -p {REMOTE_DIR}/frontend {REMOTE_DIR}/data {REMOTE_DIR}/sql {REMOTE_DIR}/tmp")

    sftp = client.open_sftp()

    def upload_file(local: Path, remote: str) -> None:
        print(f"[upload] {local.name} -> {remote}")
        sftp.put(str(local), remote)

    def upload_dir(local: Path, remote: str) -> None:
        for item in local.rglob("*"):
            if item.is_file():
                rel = item.relative_to(local).as_posix()
                remote_path = f"{remote}/{rel}"
                remote_parent = remote_path.rsplit("/", 1)[0]
                try:
                    sftp.stat(remote_parent)
                except OSError:
                    exec_cmd(f"mkdir -p {remote_parent}")
                sftp.put(str(item), remote_path)

    upload_file(jar, f"{REMOTE_DIR}/server.jar")
    upload_dir(dist, f"{REMOTE_DIR}/frontend")

    sql_dir = ROOT / "server" / "server" / "src" / "main" / "resources" / "sql"
    for sql in sql_dir.glob("*.sql"):
        upload_file(sql, f"{REMOTE_DIR}/sql/{sql.name}")

    upload_file(DEPLOY_DIR / "docker-compose.yml", f"{REMOTE_DIR}/docker-compose.yml")
    exec_cmd("mkdir -p /etc/nginx/conf.d /etc/systemd/system")
    upload_file(DEPLOY_DIR / "nginx-code-student-pro.conf", "/etc/nginx/conf.d/code-student-pro.conf")
    upload_file(DEPLOY_DIR / "code-student-pro.service", "/etc/systemd/system/code-student-pro.service")

    setup_script = f"""
set -e
export DEEPSEEK_API_KEY='{DEEPSEEK_KEY}'

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable docker && systemctl start docker
fi

if ! command -v java >/dev/null 2>&1; then
  if command -v yum >/dev/null 2>&1; then
    yum install -y java-21-openjdk java-21-openjdk-devel || yum install -y java-17-openjdk
  elif command -v apt-get >/dev/null 2>&1; then
    apt-get update && apt-get install -y openjdk-21-jdk || apt-get install -y openjdk-17-jdk
  fi
fi

if ! command -v nginx >/dev/null 2>&1; then
  if command -v yum >/dev/null 2>&1; then yum install -y nginx; else apt-get install -y nginx; fi
fi

if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://rpm.nodesource.com/setup_20.x | bash - 2>/dev/null || true
  if command -v yum >/dev/null 2>&1; then yum install -y nodejs; else apt-get install -y nodejs npm; fi
fi

cd {REMOTE_DIR}
if docker compose version >/dev/null 2>&1; then
  docker compose up -d
else
  docker-compose up -d
fi
sleep 12

rm -f /etc/nginx/sites-enabled/default
sed -i "s|^Environment=DEEPSEEK_API_KEY=.*|Environment=DEEPSEEK_API_KEY=$DEEPSEEK_API_KEY|" /etc/systemd/system/code-student-pro.service
systemctl daemon-reload
systemctl enable code-student-pro
systemctl restart code-student-pro
nginx -t && systemctl enable nginx && systemctl restart nginx

echo 'DEPLOY_DONE'
"""
    exec_cmd(setup_script)
    sftp.close()
    client.close()
    print(f"\n部署完成: http://{HOST}/")


if __name__ == "__main__":
    jar_path, dist_path = build_artifacts()
    upload_and_deploy(jar_path, dist_path)

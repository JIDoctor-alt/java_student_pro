#!/usr/bin/env python3
"""分步完成远程部署，避免长时间阻塞。"""

from __future__ import annotations

import os
import sys
import time
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


def safe_print(text: str) -> None:
    print(text.encode("gbk", errors="ignore").decode("gbk", errors="ignore"))


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 120) -> tuple[int, str, str]:
    print(f"\n>>> {cmd}")
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    exit_code = stdout.channel.recv_exit_status()
    out = stdout.read().decode(errors="ignore")
    err = stderr.read().decode(errors="ignore")
    if out.strip():
        safe_print(out.strip())
    if err.strip():
        safe_print(err.strip())
    return exit_code, out, err


def main() -> None:
    if not PASSWORD:
        raise ValueError("请设置环境变量 DEPLOY_PASSWORD")

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    print(f"[remote] 连接 {USER}@{HOST} ...")
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)

    sftp = client.open_sftp()
    sftp.put(
        str(DEPLOY_DIR / "nginx-code-student-pro.conf"),
        "/etc/nginx/conf.d/code-student-pro.conf",
    )
    sftp.put(
        str(DEPLOY_DIR / "code-student-pro.service"),
        "/etc/systemd/system/code-student-pro.service",
    )
    sftp.close()

    run(client, f"cd {REMOTE_DIR} && docker-compose pull", timeout=600)
    code, _, _ = run(client, f"cd {REMOTE_DIR} && docker-compose up -d", timeout=180)
    if code != 0:
        safe_print("docker-compose up 失败，尝试继续检查容器状态")

    time.sleep(8)
    run(client, "docker ps --format 'table {{.Names}}\t{{.Ports}}\t{{.Status}}' | grep -E 'code-|NAMES'")

    run(client, "rm -f /etc/nginx/sites-enabled/default")
    run(client, f"sed -i 's|^Environment=DEEPSEEK_API_KEY=.*|Environment=DEEPSEEK_API_KEY={DEEPSEEK_KEY}|' /etc/systemd/system/code-student-pro.service")
    run(client, "systemctl daemon-reload")
    run(client, "systemctl enable code-student-pro")
    run(client, "systemctl restart code-student-pro")
    time.sleep(5)
    run(client, "systemctl status code-student-pro --no-pager -l | head -25")
    run(client, "nginx -t")
    run(client, "systemctl restart nginx")

    run(client, "curl -s http://127.0.0.1/ | head -3")
    run(client, "curl -s -o /dev/null -w 'api:%{http_code}' http://127.0.0.1/api/user/get/login")
    run(client, "ss -tlnp | grep -E ':8123|:3306|:6379' || true")

    client.close()
    print(f"\n完成。访问: http://{HOST}/")


if __name__ == "__main__":
    main()

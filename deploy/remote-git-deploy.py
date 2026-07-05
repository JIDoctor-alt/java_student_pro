#!/usr/bin/env python3
"""SSH 到服务器：clone/pull GitHub 并执行 deploy-on-server.sh"""

from __future__ import annotations

import os
import sys
from pathlib import Path

try:
    import paramiko
except ImportError:
    print("pip install paramiko")
    sys.exit(1)

HOST = os.environ.get("DEPLOY_HOST", "124.70.195.81")
USER = os.environ.get("DEPLOY_USER", "root")
PASSWORD = os.environ.get("DEPLOY_PASSWORD", "")
BRANCH = os.environ.get("DEPLOY_BRANCH", "feature-develop")
REPO = "https://github.com/JIDoctor-alt/java_student_pro.git"
REPO_DIR = "/opt/code-student-pro"
DEEPSEEK = os.environ.get("DEEPSEEK_API_KEY", "")
ROOT = Path(__file__).resolve().parent.parent


def safe_print(text: str) -> None:
    print(text.encode("gbk", errors="ignore").decode("gbk", errors="ignore"))


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 900) -> int:
    safe_print(f"\n>>> {cmd}")
    _, stdout, stderr = client.exec_command(cmd, timeout=timeout, get_pty=True)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode(errors="ignore")
    err = stderr.read().decode(errors="ignore")
    if out.strip():
        safe_print(out.strip())
    if err.strip():
        safe_print(err.strip())
    return code


def upload_local_script(client: paramiko.SSHClient) -> None:
    """GitHub 不可达时，上传本地 deploy-on-server.sh"""
    local = ROOT / "deploy" / "deploy-on-server.sh"
    if not local.exists():
        return
    sftp = client.open_sftp()
    run(client, f"mkdir -p {REPO_DIR}/deploy")
    remote = f"{REPO_DIR}/deploy/deploy-on-server.sh"
    sftp.put(str(local), remote)
    sftp.close()
    run(client, f"chmod +x {remote}")


def main() -> None:
    if not PASSWORD:
        raise ValueError("请设置 DEPLOY_PASSWORD")

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    safe_print(f"[remote] 连接 {USER}@{HOST} ...")
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)

    run(client, f"mkdir -p {REPO_DIR}")

    if run(client, f"test -d {REPO_DIR}/.git") == 0:
        run(client, f"cd {REPO_DIR} && git fetch origin && git checkout {BRANCH} && git pull origin {BRANCH}", timeout=300)
    else:
        run(client, f"rm -rf {REPO_DIR}.bak && mv {REPO_DIR} {REPO_DIR}.bak 2>/dev/null || true")
        code = run(
            client,
            f"git clone -b {BRANCH} {REPO} {REPO_DIR}",
            timeout=300,
        )
        if code != 0:
            safe_print("[remote] git clone 失败，尝试恢复旧目录并上传本地脚本...")
            run(client, f"rm -rf {REPO_DIR} && mv {REPO_DIR}.bak {REPO_DIR} 2>/dev/null || mkdir -p {REPO_DIR}")
            upload_local_script(client)

    upload_local_script(client)

    deploy_cmd = f"export DEEPSEEK_API_KEY='{DEEPSEEK}'; export DEPLOY_BRANCH='{BRANCH}'; bash {REPO_DIR}/deploy/deploy-on-server.sh"
    code = run(client, deploy_cmd, timeout=1800)
    client.close()
    if code != 0:
        sys.exit(code)
    safe_print(f"\n完成: http://{HOST}/")


if __name__ == "__main__":
    main()

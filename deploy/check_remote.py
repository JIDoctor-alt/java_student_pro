import os
import paramiko

PASSWORD = os.environ.get("DEPLOY_PASSWORD", "")
client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("124.70.195.81", username="root", password=PASSWORD, timeout=30)

cmds = [
    "systemctl is-active code-student-pro nginx docker 2>&1",
    "docker ps -a --format 'table {{.Names}}\t{{.Ports}}\t{{.Status}}'",
    "ss -tlnp | grep -E ':80|:8123|:3306|:6379' || true",
    "curl -s -o /dev/null -w 'home:%{http_code} ' http://127.0.0.1/",
    "curl -s -o /dev/null -w 'api:%{http_code}' http://127.0.0.1/api/user/get/login",
    "journalctl -u code-student-pro -n 15 --no-pager 2>&1 | tail -15",
    "ls -la /opt/code-student-pro/frontend/index.html 2>&1",
    "cat /etc/nginx/conf.d/code-student-pro.conf 2>&1 | head -8",
]

for cmd in cmds:
    print(">>>", cmd)
    _, stdout, stderr = client.exec_command(cmd)
    out = stdout.read().decode(errors="ignore").strip()
    err = stderr.read().decode(errors="ignore").strip()
    if out:
        print(out)
    if err:
        print(err)
    print()

client.close()

# 部署说明

## 服务器信息

- 地址：`124.70.195.81`
- 部署目录：`/opt/code-student-pro`
- 访问：`http://124.70.195.81/`

## 一键部署（本地 Windows 执行）

```powershell
$env:JAVA_HOME='D:\work\jdk21'
$env:DEPLOY_HOST='124.70.195.81'
$env:DEPLOY_USER='root'
$env:DEPLOY_PASSWORD='你的服务器密码'
$env:DEEPSEEK_API_KEY='你的DeepSeek密钥'

cd D:\cursor-workspace\java_student_pro
pip install paramiko
python deploy/deploy.py
```

## 架构

| 组件 | 说明 |
|------|------|
| Nginx :80 | 前端静态文件 + 反向代理 `/api` |
| Spring Boot :8123 | 后端 API（profile=prod） |
| MySQL :3306 | Docker 容器，库 `student_pro` |
| Redis :6379 | Docker 容器，Session |

## 手动运维

```bash
# 查看后端日志
journalctl -u code-student-pro -f

# 重启后端
systemctl restart code-student-pro

# 重启数据库/Redis
cd /opt/code-student-pro && docker compose restart
```

## 安全提醒

- **切勿**将服务器密码、API Key 提交到 Git
- 生产环境建议修改 MySQL 默认密码并配置防火墙

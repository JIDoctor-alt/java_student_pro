# 数据库快照目录

本目录存放 `student_pro` 库的**数据快照**（mysqldump），与建表脚本（`../create_*.sql`）配合使用。

## 文件说明

| 文件 | 说明 |
|------|------|
| `student_pro_snapshot.sql` | 全库数据导出（含 user / app / chat_history 等表数据） |

## 使用方式

### 手动导出（Windows）

```powershell
.\scripts\db-export.ps1
```

### 手动导出（Git Bash / Linux / macOS）

```bash
./scripts/db-export.sh
```

### 导入恢复

```powershell
.\scripts\db-import.ps1
```

### 推送 GitHub 时自动带上

安装 Git 钩子后，**每次 commit 前**会自动导出并暂存快照文件，随代码一起 push：

```powershell
.\scripts\install-git-hooks.ps1
```

## 环境变量（可选）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_HOST` | `127.0.0.1` | MySQL 地址 |
| `MYSQL_PORT` | `3306` | 端口 |
| `MYSQL_USER` | `root` | 用户名 |
| `MYSQL_PASSWORD` | `root` | 密码 |
| `MYSQL_DATABASE` | `student_pro` | 库名 |
| `MYSQL_CONTAINER` | `mysql-container` | Docker 容器名（优先使用 docker exec） |

## 安全提示

快照中包含用户密码哈希、对话历史等**敏感数据**，请勿将含生产数据的快照推送到公开仓库。

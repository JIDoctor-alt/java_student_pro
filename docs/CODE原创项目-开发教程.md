# CODE 原创项目 — 开发教程

> 全栈 AI 应用生成平台：用户描述需求 → AI 生成网页或 Vue 工程 → 实时预览 → 一键部署。  
> 技术栈：Vue 3 + Spring Boot 3.5 + MySQL + Redis + DeepSeek。

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 快速开始](#2-快速开始)
- [3. 功能模块一览](#3-功能模块一览)
- [4. 前端架构](#4-前端架构)
- [5. 用户模块](#5-用户模块)
- [6. AI 代码生成](#6-ai-代码生成)
- [7. 应用管理](#7-应用管理)
- [8. 对话历史与多轮迭代](#8-对话历史与多轮迭代)
- [9. 预览、SSE 与本地部署](#9-预览sse-与本地部署)
- [10. Vue3 工程项目生成](#10-vue3-工程项目生成)
- [11. 生产环境部署](#11-生产环境部署)
- [12. API 接口速查](#12-api-接口速查)
- [13. 常见问题](#13-常见问题)
- [附录：开发时间线](#附录开发时间线)

---

## 1. 项目概览

### 1.1 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 组合式 API、TypeScript、Vite、Ant Design Vue、Pinia、Vue Router、Axios |
| 后端 | Spring Boot 3.5、Java 21、MyBatis Flex、Redis Session、AOP 权限、LangChain4j |
| 数据 | MySQL 8（Docker）、Redis 7（分布式 Session，**database: 1**） |
| AI | DeepSeek API（OpenAI 兼容协议，环境变量 `DEEPSEEK_API_KEY`） |

### 1.2 目录结构

```
java_student_pro/
├── client/                      # 前端 Vue3
│   └── src/
│       ├── layouts/             # BasicLayout
│       ├── components/          # GlobalHeader / GlobalFooter / AppCard
│       ├── views/               # 首页、对话页、管理页
│       ├── api/                 # 接口封装
│       ├── stores/              # Pinia（loginUser）
│       └── utils/               # SSE 工具
├── server/server/               # 后端 Spring Boot
│   └── src/main/java/com/zhenq/
│       ├── controller/          # User / App / AiCodeGenerator
│       ├── service/             # 业务层
│       ├── ai/                  # LangChain4j AI 服务
│       ├── core/                # 门面、解析器、保存器、Vue 工程
│       ├── aop/                 # 权限切面
│       └── resources/
│           ├── sql/             # 建表脚本
│           └── prompt/          # AI 系统提示词
├── deploy/                      # 生产部署脚本与配置
└── docs/                        # 本文档
```

### 1.3 访问地址

| 环境 | 地址 |
|------|------|
| 前端开发 | http://localhost:5173 |
| 后端 API | http://localhost:8123/api |
| 接口文档 | http://localhost:8123/api/doc.html |
| 生产（示例） | http://124.70.195.81/ |

### 1.4 代码生成模式

| 模式 | `codeGenType` | 产物 | 预览方式 |
|------|---------------|------|----------|
| 原生 HTML | `html` | 单文件 `index.html` | 直接预览源码 |
| 原生多文件 | `multi_file` | html + css + js | 直接预览 `index.html` |
| Vue3 工程 | `vue_project` | 完整 Vite 工程 | 先 `npm build`，预览 `dist/` |

---

## 2. 快速开始

### 2.1 前置依赖

- JDK 21（设置 `JAVA_HOME`）
- Node.js 18+（Vue 工程构建需要）
- Docker（MySQL、Redis）
- DeepSeek API Key

### 2.2 初始化数据库

```bash
# 假设 MySQL 容器名为 mysql-container，库 student_pro，账号 root/root
docker cp server/server/src/main/resources/sql/create_table.sql mysql-container:/tmp/user.sql
docker exec mysql-container sh -c "mysql -uroot -proot < /tmp/user.sql"

docker cp server/server/src/main/resources/sql/create_app_table.sql mysql-container:/tmp/app.sql
docker exec mysql-container sh -c "mysql -uroot -proot < /tmp/app.sql"

docker cp server/server/src/main/resources/sql/create_chat_history_table.sql mysql-container:/tmp/chat_history.sql
docker exec mysql-container sh -c "mysql -uroot -proot < /tmp/chat_history.sql"
```

### 2.3 启动 Redis

```bash
docker run -d --name redis-custom -p 6379:6379 redis:7
# 或
docker start redis-custom
```

> **注意**：Session 使用 Redis **database 1**，避免与其他项目共用 db0 时 Session 反序列化冲突。

### 2.4 启动后端

```powershell
$env:JAVA_HOME="D:\work\jdk21"
$env:DEEPSEEK_API_KEY="你的密钥"

cd server/server
.\mvnw.cmd spring-boot:run
```

### 2.5 启动前端

```bash
cd client
npm install
npm run dev
```

### 2.6 测试账号

- 普通用户：自行注册，或 `testuser` / `12345678`
- 管理员：数据库将 `user_role` 改为 `admin`

---

## 3. 功能模块一览

| 模块 | 核心能力 | 关键文件 |
|------|----------|----------|
| 全局布局 | 上中下响应式布局、菜单权限 | `BasicLayout.vue`、`GlobalHeader.vue` |
| 用户模块 | 注册/登录/Session/权限 | `UserController`、`AuthInterceptor` |
| AI 生成 | HTML / 多文件 / Vue 工程 | `AiCodeGeneratorFacade`、`VueProjectAgentService` |
| 应用管理 | CRUD、精选、部署 | `AppController`、`AppServiceImpl` |
| 对话历史 | 游标分页、AI 记忆、级联删除 | `ChatHistoryServiceImpl` |
| 实时预览 | iframe 两段式预览 | `StaticResourceConfig`、`AppChatView` |
| Vue 工程 | 工具调用落盘、npm 构建 | `VueProjectFileTool`、`VueProjectBuildService` |
| 生产部署 | 一键脚本 + Nginx | `deploy/deploy.py` |

---

## 4. 前端架构

### 4.1 布局

- **BasicLayout**：上（Header）中（RouterView）下（Footer）
- **GlobalHeader**：Logo + 菜单（配置驱动）+ 登录用户
- **GlobalFooter**：版权信息

菜单配置示例（`GlobalHeader.vue`）：

```typescript
const allMenus = [
  { key: '/', label: '首页' },
  { key: '/about', label: '关于' },
  { key: '/admin/user', label: '用户管理', access: ACCESS_ENUM.ADMIN },
  { key: '/admin/app', label: '应用管理', access: ACCESS_ENUM.ADMIN },
]
```

### 4.2 页面说明

| 路由 | 页面 | 说明 |
|------|------|------|
| `/` | HomeView | Hero 输入框 + 生成模式选择 + 我的作品 + 精选案例 |
| `/app/chat/:id` | AppChatView | 左侧对话 + 右侧预览 + 部署按钮 |
| `/user/login` | UserLoginView | 登录 |
| `/user/register` | UserRegisterView | 注册 |
| `/admin/user` | UserManageView | 用户管理（管理员） |
| `/admin/app` | AppManageView | 应用管理（管理员） |

**创建流程**：首页输入描述 → 选择生成模式 → `addApp` → 跳转 `/app/chat/{id}?auto=1` → 自动触发生成。

### 4.3 前端基础设施

| 文件 | 作用 |
|------|------|
| `stores/loginUser.ts` | Pinia 全局登录态 |
| `request.ts` | Axios + `withCredentials: true` + 401 拦截 |
| `router/index.ts` | 路由守卫，`meta.access` 控制权限 |
| `utils/sse.ts` | fetch + ReadableStream 消费 SSE（支持命名事件） |

---

## 5. 用户模块

### 5.1 功能清单

| 功能 | 角色 |
|------|------|
| 注册 / 登录 / 注销 / 获取当前用户 | 游客 / 登录用户 |
| 分页查询、删除用户 | 管理员 |

### 5.2 后端设计

- **鉴权**：Redis 分布式 Session
- **密码**：MD5 + 盐（`UserConstant.SALT`）
- **ORM**：MyBatis Flex，列名 snake_case
- **权限**：`@AuthCheck(mustRole = "admin")` + AOP
- **响应**：`BaseResponse<T>` + `ErrorCode`

### 5.3 核心类

```
controller/UserController
service/UserServiceImpl
aop/AuthInterceptor
annotation/AuthCheck
model/entity/User
model/vo/LoginUserVO, UserVO
config/RedisSessionConfig
```

---

## 6. AI 代码生成

### 6.1 架构（设计模式）

```
AiCodeGeneratorController / AppController
        ↓
AiCodeGeneratorFacade          ← 门面：统一编排
        ↓
AiCodeGeneratorService         ← LangChain4j @AiService（HTML / 多文件）
VueProjectAgentService         ← LangChain4j Agent + @Tool（Vue 工程）
        ↓
CodeParser                     ← 解析 Markdown 代码块
        ↓
CodeFileSaverExecutor          ← 路由到 Html / MultiFile 保存器
        ↓
HtmlCodeFileSaverTemplate      ← 模板方法模式
MultiFileCodeFileSaverTemplate
```

### 6.2 LangChain4j 配置

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY:}
      model-name: deepseek-chat
    streaming-chat-model:
      base-url: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY:}
      model-name: deepseek-chat
```

> API Key **仅**通过环境变量 `DEEPSEEK_API_KEY` 注入，不要写入代码库。

### 6.3 系统提示词

| 文件 | 模式 |
|------|------|
| `prompt/codegen-html-system-prompt.txt` | 单文件 HTML |
| `prompt/codegen-multi-file-system-prompt.txt` | 多文件 |
| `prompt/codegen-vue-project-system-prompt.txt` | Vue3 工程（工具调用） |

### 6.4 落盘目录

```
tmp/code_output/{codeGenType}_{appId}/
```

示例：`tmp/code_output/html_2/index.html`、`tmp/code_output/vue_project_5/dist/index.html`

---

## 7. 应用管理

### 7.1 数据库表 `app`

关键字段：`app_name`、`cover`、`init_prompt`、`code_gen_type`、`deploy_key`、`deployed_time`、`priority`（99=精选）、`user_id`

建表脚本：`server/server/src/main/resources/sql/create_app_table.sql`

### 7.2 权限模型

| 操作 | 用户 | 管理员 |
|------|------|--------|
| 创建 / 编辑 / 删除自己的应用 | ✅ | — |
| 查看自己的应用、精选应用 | ✅ | — |
| AI 生成、部署、查对话历史 | ✅（自己的） | ✅（全部） |
| 管理所有应用、设精选 | — | ✅ |

---

## 8. 对话历史与多轮迭代

### 8.1 解决的问题

每次对话相互独立时，AI 无法基于已有成果增量改进。对话历史模块实现：

- 持久化 user / ai / error 三类消息
- 应用级隔离，删除应用时级联删除
- 游标分页「向上加载更多」
- 将历史注入 prompt，支持多轮迭代

### 8.2 数据库表 `chat_history`

```sql
-- 见 create_chat_history_table.sql
-- 关键字段：app_id, message_type(user/ai/error), content, user_id
-- 索引：idx_app_id_id (app_id, id) — 游标分页
```

### 8.3 游标分页

```
首次（不传 lastId）：ORDER BY id DESC LIMIT 10 → 反转为时间正序
加载更多（传 lastId）：WHERE id < lastId ORDER BY id DESC LIMIT 10 → prepend
```

响应结构 `ChatHistoryCursorPageVO`：

```json
{
  "records": [ /* 时间正序 */ ],
  "hasMore": true,
  "nextCursor": 42
}
```

### 8.4 生成接口集成（`/app/chat/gen/code`）

执行顺序：

1. 校验登录 + 应用归属
2. `buildPromptWithMemory(appId, message)` — 注入最近 20 条历史
3. `saveUserMessage(...)` — 持久化用户消息
4. 调用 AI 流式生成
5. 成功 → `saveAiMessage`；失败 → `saveErrorMessage`

> 先构建记忆、再保存当前消息，避免 prompt 重复。

### 8.5 Redis Session

```yaml
spring:
  session:
    store-type: redis
    timeout: 30d
  data:
    redis:
      host: localhost
      port: 6379
      database: 1    # 独立 db，避免与其他项目冲突
```

启用类：`RedisSessionConfig`（`@EnableRedisHttpSession`）

### 8.6 前端（AppChatView）

| 行为 | 说明 |
|------|------|
| 进入页面 | 加载最新 10 条历史 |
| 加载更多 | 顶部按钮，传 `lastId=nextCursor` |
| 消息类型 | user / ai / error（红色） |
| 欢迎语 | 仅无历史时显示，不入库 |
| 有历史时 | 自动加载预览 URL |

---

## 9. 预览、SSE 与本地部署

### 9.1 实时预览（两段式）

| 阶段 | 方式 |
|------|------|
| 生成中 | `iframe srcdoc` 流式注入（HTML 模式） |
| 生成完成 | `iframe src` → `/api/preview/{类型}_{appId}/index.html` |

静态资源映射（`StaticResourceConfig`）：

| 路径 | 目录 |
|------|------|
| `/preview/**` | `tmp/code_output/` |
| `/static/**` | `tmp/code_deploy/` |

### 9.2 SSE 实现要点

- 后端：**SseEmitter**（非 Flux），避免客户端断开取消 AI 流
- 前端：**fetch + ReadableStream**（`utils/sse.ts`），支持命名事件
- 错误事件名：`gen-error`（避免与 EventSource 原生 error 冲突）
- 流异常时仍保存已生成部分内容

### 9.3 本地在线部署

1. 用户点击「部署」
2. 后端复制 `code_output/{类型}_{appId}/`（Vue 模式复制 `dist/`）→ `code_deploy/{deployKey}/`
3. 更新 `deploy_key`、`deployed_time`
4. 返回：`http://localhost:8123/api/static/{deployKey}/index.html`

---

## 10. Vue3 工程项目生成

> 第三种生成模式 `vue_project`：AI 通过工具调用逐文件写入完整 Vue3 + Vite 工程，构建后预览。

### 10.1 整体流程

```
用户描述需求
  → ChatHistoryService 注入对话记忆
  → VueProjectAgentService（LangChain4j Agent）
  → VueProjectFileTool（saveFile / readFile / listFiles）
  → VueProjectBuildService（npm install + build）
  → iframe 预览 dist/index.html
```

### 10.2 后端核心类

| 类 | 职责 |
|----|------|
| `VueProjectFileTool` | 工具调用落盘，ThreadLocal 绑定 appId |
| `VueProjectAgentService` | LangChain4j Agent 流式生成 |
| `VueProjectCodegenExecutor` | SSE 事件编排 |
| `VueProjectBuildService` | npm install + build |
| `VueProjectPackageNormalizer` | 修正 npm 依赖版本（如 icons-vue） |
| `VueProjectPreviewPathFixer` | vite `base:'./'` + dist 相对路径 |
| `VueProjectContext` | InheritableThreadLocal 传递 appId |

### 10.3 SSE 事件扩展

| 事件 | 说明 | 前端处理 |
|------|------|----------|
| `message` | 文本 token | 追加到 AI 消息 |
| `tool-start` | 工具开始 | 显示「正在保存 xxx.vue」 |
| `build-log` | npm 输出 | 构建日志 |
| `preview-ready` | 构建完成 | 加载 iframe 预览 |
| `gen-error` | 错误 | 红色提示，不触发预览 |
| `done` | 结束 | 关闭 loading |

### 10.4 构建与预览

```bash
cd tmp/code_output/vue_project_{appId}
npm install && npm run build
```

预览 URL：

```
http://localhost:8123/api/preview/vue_project_{appId}/dist/index.html
```

前端 `buildPreviewUrl`：

```typescript
if (codeGenType === 'vue_project') {
  return `${API_BASE_URL}/preview/vue_project_${appId}/dist/index.html`
}
return `${API_BASE_URL}/preview/${codeGenType}_${appId}/index.html`
```

### 10.5 系统提示词要点

见 `prompt/codegen-vue-project-system-prompt.txt`：

1. Vue 3 组合式 API + TypeScript + Vite + Router + Pinia
2. 使用 **saveFile 工具**逐文件写入，禁止一次性输出巨型 Markdown
3. `vite.config.ts` 必须 `base: './'`
4. 固定依赖版本，禁止编造（如 `@ant-design/icons-vue@^7.0.1`）
5. 多轮对话增量修改，结合 readFile + saveFile

### 10.6 与对话历史的协同

- AI 回复可保存**摘要**（如「已生成 12 个文件并完成构建」），完整构建日志不入库
- 多轮迭代时 Agent 通过 readFile 读取已有文件，而非依赖超长 prompt

---

## 11. 生产环境部署

部署相关文件位于 `deploy/` 目录：

| 文件 | 说明 |
|------|------|
| `deploy.py` | 一键部署脚本（本地构建 + SSH 上传 + 远程配置） |
| `finish_deploy.py` | 远程收尾（不重新构建） |
| `check_remote.py` | 远程状态检查 |
| `docker-compose.yml` | MySQL + Redis 容器 |
| `nginx-code-student-pro.conf` | Nginx 反向代理 |
| `code-student-pro.service` | systemd 后端服务 |

### 11.1 一键部署

```powershell
$env:JAVA_HOME='D:\work\jdk21'
$env:DEPLOY_HOST='124.70.195.81'
$env:DEPLOY_USER='root'
$env:DEPLOY_PASSWORD='你的服务器密码'    # 切勿提交到 Git
$env:DEEPSEEK_API_KEY='你的DeepSeek密钥'

pip install paramiko
python deploy/deploy.py
```

### 11.2 生产架构

| 组件 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 前端静态 + 反向代理 `/api` |
| Spring Boot | 8123 | 后端（`SPRING_PROFILES_ACTIVE=prod`） |
| MySQL | 3306 | Docker，库 `student_pro` |
| Redis | 6379 | Docker，Session database 1 |

### 11.3 运维命令

```bash
journalctl -u code-student-pro -f          # 后端日志
systemctl restart code-student-pro          # 重启后端
cd /opt/code-student-pro && docker-compose restart   # 重启数据库
```

> 详细说明见 `deploy/README.md`。生产环境务必修改默认密码，API Key 通过环境变量注入。

---

## 12. API 接口速查

> 所有路径前缀为 `/api`，需登录的接口须携带 Session Cookie。

### 12.1 通用响应

```json
{ "code": 0, "data": {}, "message": "ok" }
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40400 | 数据不存在 |
| 40000 | 参数错误 |

### 12.2 用户接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/user/register` | 注册 | 公开 |
| POST | `/user/login` | 登录 | 公开 |
| POST | `/user/logout` | 注销 | 登录 |
| GET | `/user/current` | 当前用户 | 登录 |
| POST | `/user/list/page` | 分页查询 | 管理员 |
| DELETE | `/user/{id}` | 删除用户 | 管理员 |

### 12.3 应用接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/app/add` | 创建应用 | 登录 |
| POST | `/app/update` | 编辑名称 | 创建者 |
| POST | `/app/delete` | 删除应用 | 创建者 |
| GET | `/app/get/vo?id=` | 应用详情 | 登录 |
| POST | `/app/my/list/page/vo` | 我的应用 | 登录 |
| POST | `/app/good/list/page/vo` | 精选应用 | 登录 |
| GET | `/app/chat/gen/code` | AI 流式生成（SSE） | 创建者 |
| GET | `/app/chat/history` | 对话历史（游标分页） | 创建者/管理员 |
| POST | `/app/deploy` | 部署应用 | 创建者 |
| POST | `/app/admin/delete` | 删除任意应用 | 管理员 |
| POST | `/app/admin/update` | 编辑/设精选 | 管理员 |
| POST | `/app/admin/list/page/vo` | 分页查询 | 管理员 |
| GET | `/app/admin/get/vo?id=` | 应用详情 | 管理员 |

**对话历史参数**：`appId`（必填）、`pageSize`（默认 10，最大 20）、`lastId`（可选）

### 12.4 AI 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/generate?prompt=&codeGenType=` | 同步生成 |
| GET | `/ai/generate/sse?prompt=&codeGenType=` | SSE 流式（通用） |

### 12.5 分页结构

**MyBatis Flex 分页**：

```json
{ "records": [], "pageNumber": 1, "pageSize": 10, "totalRow": 100, "totalPage": 10 }
```

**对话历史游标分页**：

```json
{ "records": [], "hasMore": true, "nextCursor": 42 }
```

---

## 13. 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 生成失败 / closed | API Key 无效或流被中断 | 设置 `DEEPSEEK_API_KEY`；确认已登录；避免重复点击 |
| 预览 404 | URL 用了目录形式 | 使用 `/preview/html_2/index.html` |
| 预览纯文字 | 模型返回说明而非代码 | 重新生成；检查系统提示词 |
| mvn compile 导致重启 | devtools 热重载与 SSE 冲突 | 运行中不要单独 compile |
| Unknown column | 列名不一致 | 统一 snake_case，见 SQL 脚本 |
| Redis 连接超时 | 本地未启动 Redis | `docker start redis-custom` |
| Session 反序列化错误 | 多项目共用 Redis db0 | 改用 `database: 1` |
| 刷新后历史丢失 | 未调用历史接口 | 确认 `chat_history` 表存在；`AppChatView` 加载历史 |
| 多轮仍完全重写 | 记忆未注入 | 确认 `buildPromptWithMemory` 在 `saveUserMessage` 之前 |
| Vue 预览空白 | 未构建或资源路径错误 | 确认 Node.js 已安装；检查 `dist/index.html` 相对路径 |
| npm ETARGET icons-vue | 模型编造版本号 | `VueProjectPackageNormalizer` + 提示词约束 |
| 工具 JSON 乱码显示 | 流式输出含 tool-call | 后端过滤 + 前端 `tool-start` 友好展示 |
| Vue 第二 turn appId 未设置 | ThreadLocal 未传递 | `InheritableThreadLocal` + `BOUND_APP_IDS` |

---

## 附录：开发时间线

| 阶段 | 内容 |
|------|------|
| 1 | 全局布局：BasicLayout、GlobalHeader、GlobalFooter |
| 2 | 用户模块：Session 鉴权、MyBatis Flex、AOP 权限 |
| 3 | AI 生成：LangChain4j + DeepSeek、HTML/多文件、SSE、门面/模板方法 |
| 4 | 应用管理：app 表、用户/管理员接口、主页+对话页+管理页 |
| 5 | 在线部署：deploy 接口、静态资源映射 |
| 6 | 实时预览：/preview 映射、两段式 iframe |
| 7 | 稳定性：EventSource→fetch SSE、SseEmitter、预览 URL 修复 |
| 8 | 对话历史：游标分页、AI 记忆注入、级联删除 |
| 9 | Redis Session：database 1、多实例共享登录态 |
| 10 | Vue 工程：Agent 工具落盘、npm build、SSE 扩展、前端模式选择 |
| 11 | 生产部署：deploy 脚本、Nginx、Docker Compose、systemd |

---

*文档更新时间：2026-06-28 · CODE 原创项目*

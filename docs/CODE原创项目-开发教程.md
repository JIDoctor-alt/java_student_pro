# CODE 原创项目 — 全栈开发教程

> 本文档根据项目完整开发对话记录整理，涵盖从全局布局、用户模块、AI 应用生成、应用管理、在线部署、对话历史、Redis Session 到 Vue3 工程项目生成的全流程。

---

## 目录

1. [项目概览](#1-项目概览)
2. [环境准备与启动](#2-环境准备与启动)
3. [全局基础布局（前端）](#3-全局基础布局前端)
4. [用户模块](#4-用户模块)
5. [AI 应用生成](#5-ai-应用生成)
6. [应用管理模块](#6-应用管理模块)
7. [实时预览与在线部署](#7-实时预览与在线部署)
8. [对话历史模块](#8-对话历史模块)
9. [前端页面说明](#9-前端页面说明)
10. [API 接口速查](#10-api-接口速查)
11. [常见问题排查](#11-常见问题排查)
12. [工程项目生成](#12-工程项目生成)

---

## 1. 项目概览

### 1.1 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 组合式 API、TypeScript、Vite、Ant Design Vue、Pinia、Vue Router、Axios |
| 后端 | Spring Boot 3.5、Java 21、MyBatis Flex、Redis Session 鉴权、AOP 权限、LangChain4j |
| 数据库 | MySQL 8（Docker） |
| 缓存 | Redis 8（Docker，分布式 Session） |
| AI | DeepSeek API（OpenAI 兼容协议） |

### 1.2 目录结构

```
java_student_pro/
├── client/                 # 前端 Vue3 项目
│   └── src/
│       ├── layouts/        # 全局布局
│       ├── components/     # 公共组件
│       ├── views/          # 页面
│       ├── api/            # 接口封装
│       ├── stores/         # Pinia 状态
│       └── utils/          # 工具（SSE 等）
├── server/server/          # 后端 Spring Boot
│   └── src/main/java/com/zhenq/
│       ├── controller/     # 控制器
│       ├── service/        # 业务层
│       ├── ai/             # LangChain4j AI 服务
│       ├── core/           # 门面/模板方法/解析器
│       ├── aop/            # 权限切面
│       └── resources/
│           ├── sql/        # 建表脚本
│           └── prompt/     # AI 系统提示词
└── docs/                   # 本文档
```

### 1.3 访问地址

- 前端：`http://localhost:5173`
- 后端 API：`http://localhost:8123/api`
- 接口文档（Knife4j）：`http://localhost:8123/api/doc.html`

---

## 2. 环境准备与启动

### 2.1 前置依赖

- JDK 21（设置 `JAVA_HOME`）
- Node.js 22+ / 24+
- Docker（运行 MySQL、Redis）
- DeepSeek API Key（环境变量 `DEEPSEEK_API_KEY`）

### 2.2 数据库初始化

```bash
# 启动 MySQL 容器后，执行建表脚本
docker cp server/server/src/main/resources/sql/create_table.sql mysql-container:/tmp/user.sql
docker exec mysql-container sh -c "mysql -uroot -proot < /tmp/user.sql"

docker cp server/server/src/main/resources/sql/create_app_table.sql mysql-container:/tmp/app.sql
docker exec mysql-container sh -c "mysql -uroot -proot < /tmp/app.sql"

docker cp server/server/src/main/resources/sql/create_chat_history_table.sql mysql-container:/tmp/chat_history.sql
docker exec mysql-container sh -c "mysql -uroot -proot < /tmp/chat_history.sql"
```

数据库名：`student_pro`，账号密码见 `application.yml`（默认 `root/root`）。

**Redis（分布式 Session 依赖）**：

```bash
# 若本地尚无 Redis 容器，可快速启动一个
docker run -d --name redis-custom -p 6379:6379 redis:7
# 或启动已有容器
docker start redis-custom
```

### 2.3 启动后端

```powershell
$env:JAVA_HOME="D:\work\jdk21"
$env:PATH="D:\work\jdk21\bin;$env:PATH"
$env:DEEPSEEK_API_KEY="你的密钥"

cd server/server
.\mvnw.cmd spring-boot:run
```

### 2.4 启动前端

```bash
cd client
npm install
npm run dev
```

### 2.5 测试账号

- 普通用户：自行注册，或 `testuser` / `12345678`（若已注册）
- 管理员：需在数据库将 `user_role` 改为 `admin`

---

## 3. 全局基础布局（前端）

### 3.1 需求

- 上中下布局，响应式
- 顶部：Logo + 标题 + 菜单 + 登录用户
- 中间：`RouterView` 内容区
- 底部：固定版权信息

### 3.2 核心文件

| 文件 | 作用 |
|------|------|
| `layouts/BasicLayout.vue` | 上中下 Layout 容器 |
| `components/GlobalHeader.vue` | 导航栏（Menu 配置驱动） |
| `components/GlobalFooter.vue` | 底部版权 |
| `App.vue` | 入口，仅渲染 BasicLayout |
| `main.ts` | 移除 main.css，引入 ant-design-vue reset |

### 3.3 菜单配置示例

```typescript
const allMenus: AppMenuItem[] = [
  { key: '/', label: '首页' },
  { key: '/about', label: '关于' },
  { key: '/admin/user', label: '用户管理', access: ACCESS_ENUM.ADMIN },
  { key: '/admin/app', label: '应用管理', access: ACCESS_ENUM.ADMIN },
]
```

---

## 4. 用户模块

### 4.1 需求分析

| 功能 | 说明 | 角色 |
|------|------|------|
| 注册 | 账号 + 密码 + 确认密码 | 游客 |
| 登录 | Session 登录态 | 游客 |
| 获取当前用户 | 刷新后免重复登录 | 登录用户 |
| 注销 | 清除 Session | 登录用户 |
| 权限控制 | user / admin | 全局 |
| 用户管理 | 搜索、删除用户 | 管理员 |

### 4.2 后端设计要点

- **鉴权**：Redis 分布式 Session（`request.getSession().setAttribute`）
- **密码**：MD5 + 盐（`UserConstant.SALT`）
- **ORM**：MyBatis Flex，列名 snake_case（`user_account`、`is_delete`）
- **权限**：`@AuthCheck(mustRole = "admin")` + AOP 切面
- **统一响应**：`BaseResponse<T>` + `ErrorCode` 枚举

### 4.3 核心类

```
common/          BaseResponse, ErrorCode, PageRequest, ResultUtils
exception/       BusinessException, GlobalExceptionHandler, ThrowUtils
model/entity/    User
model/dto/user/  UserRegisterRequest, UserLoginRequest, UserQueryRequest...
model/vo/        LoginUserVO, UserVO
service/         UserService, UserServiceImpl
controller/      UserController
aop/             AuthInterceptor
annotation/      AuthCheck
```

### 4.4 前端设计要点

- `stores/loginUser.ts`：Pinia 全局登录态
- `request.ts`：Axios + `withCredentials: true` + 401 拦截
- `router/index.ts`：路由守卫，`meta.access` 控制权限
- 页面：`UserLoginView`、`UserRegisterView`、`UserManageView`

### 4.5 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/register` | 注册 |
| POST | `/user/login` | 登录 |
| POST | `/user/logout` | 注销 |
| GET | `/user/current` | 当前用户 |
| POST | `/user/list/page` | 分页查询（管理员） |
| DELETE | `/user/{id}` | 删除用户（管理员） |

---

## 5. AI 应用生成

### 5.1 需求

- 用户描述 → AI 生成完整原生网页或工程化项目
- 三种模式：
  - **HTML 单文件**：原生 HTML
  - **多文件**：index.html + style.css + script.js
  - **Vue 工程**（`vue_project`）：Vue3 + Vite 完整工程项目（见 [第 12 章](#12-工程项目生成)）
- SSE 流式输出提升体验
- 代码落盘到本地

### 5.2 架构设计（设计模式）

```
AiCodeGeneratorController
        ↓
AiCodeGeneratorFacade          ← 门面模式：统一编排
        ↓
AiCodeGeneratorService         ← LangChain4j @AiService
        ↓
CodeParser                     ← 解析 Markdown 代码块
        ↓
CodeFileSaverExecutor          ← 门面：路由保存器
        ↓
HtmlCodeFileSaverTemplate      ← 模板方法模式
MultiFileCodeFileSaverTemplate
```

### 5.3 LangChain4j 配置

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY:你的密钥}
      model-name: deepseek-chat
    streaming-chat-model:
      base-url: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY:你的密钥}
      model-name: deepseek-chat
```

### 5.4 系统提示词

- `prompt/codegen-html-system-prompt.txt`：单文件 HTML 模式
- `prompt/codegen-multi-file-system-prompt.txt`：多文件模式

关键约束：必须直接输出 `<!DOCTYPE html>` 开头代码，禁止对话式回复。

### 5.5 生成模式枚举

```java
public enum CodeGenTypeEnum {
    HTML("原生 HTML 模式", "html"),
    MULTI_FILE("原生多文件模式", "multi_file"),
    VUE_PROJECT("Vue3 工程模式", "vue_project");  // 见第 12 章
}
```

### 5.6 落盘目录

- 生成目录：`tmp/code_output/{类型}_{appId}/`
- 示例：`tmp/code_output/html_2/index.html`

### 5.7 AI 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/generate?prompt=&codeGenType=` | 同步生成 |
| GET | `/ai/generate/sse?prompt=&codeGenType=` | SSE 流式（通用） |
| GET | `/app/chat/gen/code?appId=&message=` | SSE 流式（按应用维度，需登录） |

---

## 6. 应用管理模块

### 6.1 需求

**用户基础功能**
- 创建 / 编辑 / 删除自己的应用
- 查看详情、分页查自己的应用、分页查精选应用

**用户高级功能**
- 实时查看应用效果、应用部署

**管理员功能**
- 管理所有应用、设置精选（priority=99）

### 6.2 数据库表 `app`

```sql
-- 见 server/server/src/main/resources/sql/create_app_table.sql
-- 关键字段：app_name, cover, init_prompt, code_gen_type,
--           deploy_key, deployed_time, priority, user_id
```

### 6.3 后端接口

| 角色 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户 | POST | `/app/add` | 创建应用 |
| 用户 | POST | `/app/update` | 改自己应用名称 |
| 用户 | POST | `/app/delete` | 删自己应用 |
| 用户 | GET | `/app/get/vo?id=` | 查看详情 |
| 用户 | POST | `/app/my/list/page/vo` | 我的应用（≤20/页） |
| 用户 | POST | `/app/good/list/page/vo` | 精选应用（≤20/页） |
| 用户 | GET | `/app/chat/gen/code` | AI 流式生成（SSE，含对话记忆） |
| 用户 | GET | `/app/chat/history` | 游标分页查对话历史 |
| 用户 | POST | `/app/deploy` | 部署应用 |
| 管理员 | POST | `/app/admin/delete` | 删任意应用 |
| 管理员 | POST | `/app/admin/update` | 改名称/封面/优先级 |
| 管理员 | POST | `/app/admin/list/page/vo` | 分页查询 |
| 管理员 | GET | `/app/admin/get/vo?id=` | 查看详情 |

### 6.4 精选应用

- `priority = 99`：精选
- `priority = 0`：普通
- 管理员在「应用管理」页可一键设/取消精选

---

## 7. 实时预览与在线部署

### 7.1 实时预览（两段式）

| 阶段 | 方式 | 说明 |
|------|------|------|
| 生成中 | `iframe srcdoc` | 流式 token 实时注入，HTML 模式即时可见 |
| 生成完成 | `iframe src` | 加载 `/api/preview/{类型}_{appId}/index.html` |

静态资源映射（`StaticResourceConfig`）：

- `/preview/**` → `tmp/code_output/`（生成产物）
- `/static/**` → `tmp/code_deploy/`（部署产物）

### 7.2 在线部署流程

1. 用户点击对话页「部署」按钮
2. 后端将 `code_output/{类型}_{appId}/` 复制到 `code_deploy/{deployKey}/`
3. 更新数据库 `deploy_key`、`deployed_time`
4. 返回访问地址：`http://localhost:8123/api/static/{deployKey}/index.html`

### 7.3 SSE 实现要点

- 后端使用 **SseEmitter**（非 Flux），避免客户端断开时取消 DeepSeek 流
- 前端使用 **fetch + ReadableStream**（`utils/sse.ts`），替代 EventSource
- 错误事件名：`gen-error`（避免与 EventSource 原生 error 冲突）
- 流异常时仍保存已生成部分内容（`doOnError` 落盘）

---

## 8. 对话历史模块

### 8.1 需求分析

**问题**：每次对话相互独立，AI 无法记住之前的交互。用户无法在已生成网站基础上迭代改进（如先建博客 → 再加评论 → 再优化样式），每次都要重新描述完整需求。

**目标**：

| 能力 | 说明 |
|------|------|
| 持久存储 | 用户发消息时保存；AI 成功回复后保存；失败时保存错误信息 |
| 应用级隔离 | 每个应用对话历史独立；删除应用时级联删除历史 |
| 游标分页查询 | 每次加载最新 10 条，支持向前加载更多（创建者 + 管理员可见） |
| 对话记忆 | 将历史注入 AI prompt，实现增量改进而非完全重写 |
| Redis Session | 登录态存 Redis，支持多实例部署共享 Session |

### 8.2 数据库表 `chat_history`

```sql
-- 见 server/server/src/main/resources/sql/create_chat_history_table.sql
-- 关键字段：
--   app_id        所属应用
--   message_type  user / ai / error
--   content       消息正文
--   user_id       发送用户（仅 user 类型有值）
-- 索引：idx_app_id_id (app_id, id)  — 游标分页核心
```

### 8.3 游标分页方案

类似聊天软件「向上加载更多」：

```
首次加载（不传 lastId）：
  SELECT * FROM chat_history WHERE app_id=? ORDER BY id DESC LIMIT 10
  → 反转为时间正序（旧→新）返回前端

向前加载更多（传 lastId = 当前最早消息 id）：
  SELECT * FROM chat_history WHERE app_id=? AND id < lastId ORDER BY id DESC LIMIT 10
  → 反转为正序，prepend 到消息列表顶部
```

**响应结构**（`ChatHistoryCursorPageVO`）：

```json
{
  "records": [ /* ChatHistoryVO 列表，时间正序 */ ],
  "hasMore": true,
  "nextCursor": 42
}
```

- `nextCursor`：当前批次中**最早**一条消息的 id，下次传作 `lastId`
- `hasMore`：是否还有更早消息

### 8.4 后端核心类

```
model/entity/       ChatHistory
model/enums/        ChatMessageTypeEnum（USER / AI / ERROR）
model/dto/chat/     ChatHistoryQueryRequest
model/vo/           ChatHistoryVO, ChatHistoryCursorPageVO
mapper/             ChatHistoryMapper
service/            ChatHistoryService, ChatHistoryServiceImpl
config/             RedisSessionConfig
```

**ChatHistoryService 主要方法**：

| 方法 | 说明 |
|------|------|
| `saveUserMessage` | 保存用户消息 |
| `saveAiMessage` | 保存 AI 回复 |
| `saveErrorMessage` | 保存错误信息 |
| `listHistoryByCursor` | 游标分页查询 |
| `deleteByAppId` | 删除应用下全部历史（逻辑删除） |
| `buildPromptWithMemory` | 构建带对话记忆的 prompt |
| `checkChatHistoryViewAuth` | 校验查看权限（创建者或管理员） |

### 8.5 生成接口集成（`/app/chat/gen/code`）

执行顺序：

1. 校验登录 + 应用归属
2. `buildPromptWithMemory(appId, message)` — 注入最近 20 条历史
3. `saveUserMessage(appId, userId, message)` — 持久化用户消息
4. 用带记忆的 prompt 调用 AI 流式生成
5. 流结束 → `saveAiMessage`；流异常 → `saveErrorMessage`

> 先构建记忆、再保存当前消息，避免当前需求在 prompt 中重复出现。

**对话记忆 prompt 示例结构**：

```
【对话历史】
以下是用户与本应用的过往对话，请基于已有成果进行增量改进，不要完全重写。
用户：帮我做一个博客网站
AI：（截断至 2000 字符）...
...

【当前需求】
在博客基础上添加评论功能

请基于对话历史，在已有网页基础上完成当前需求。
```

### 8.6 应用删除级联

`AppServiceImpl.removeById()` 重写：删除应用成功后调用 `chatHistoryService.deleteByAppId(appId)`，避免孤儿数据。

用户删除（`/app/delete`）和管理员删除（`/app/admin/delete`）均走此逻辑。

### 8.7 Redis 分布式 Session

**依赖**（`pom.xml`）：

- `spring-boot-starter-data-redis`
- `spring-session-data-redis`

**配置**（`application.yml`）：

```yaml
spring:
  session:
    store-type: redis
    timeout: 30d
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

**启用类**（`RedisSessionConfig.java`）：

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400 * 30)
public class RedisSessionConfig {}
```

登录/注销逻辑不变（仍用 `HttpSession`），Session 数据自动存 Redis，多后端实例可共享登录态。

### 8.8 对话历史接口

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/app/chat/history` | `appId`（必填）、`pageSize`（默认 10，最大 20）、`lastId`（可选） | 游标分页查历史 |

**权限**：仅应用创建者或管理员可访问；未登录返回 401，非创建者非管理员返回 403。

### 8.9 前端改造（AppChatView）

| 改动 | 说明 |
|------|------|
| 进入页面 | 调用 `listChatHistory` 加载最新 10 条 |
| 加载更多 | 顶部按钮，传 `lastId=nextCursor`，prepend 到列表并保持滚动位置 |
| 消息类型 | `user` → 用户气泡；`ai` → AI 气泡；`error` → 红色错误气泡 |
| 欢迎语 | 仅无历史记录时显示，不写入数据库 |
| 预览 | 有历史时自动加载 `/preview/{类型}_{appId}/index.html` |

**API 封装**（`client/src/api/app.ts`）：

```typescript
export async function listChatHistory(params: ChatHistoryQueryRequest) {
  const res = await request.get<BaseResponse<ChatHistoryCursorPageVO>>('/app/chat/history', { params })
  return res.data
}
```

---

## 9. 前端页面说明

### 9.1 主页（HomeView）

- Hero：「一句话 🐱 呈所想」+ 大输入框 + 快捷标签
- 「我的作品」：登录用户可见，卡片网格
- 「精选案例」：所有人可见

**创建流程**：输入描述 → `addApp` → 跳转 `/app/chat/{id}?auto=1` → 自动触发生成

### 9.2 对话页（AppChatView）

- 左侧：对话历史（游标分页 + 「加载更早的消息」）+ 输入框
- 右侧：生成网页实时预览（生成中 srcdoc / 完成后 iframe src）
- 顶部：应用名称 + 「部署」按钮
- 多轮对话：后端自动注入历史记忆，在已有网页上增量改进

### 9.3 管理员页面

- `/admin/user`：用户管理（搜索、删除）
- `/admin/app`：应用管理（搜索、编辑、设精选、删除）

---

## 10. API 接口速查

### 10.1 通用响应格式

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

常见错误码：

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40400 | 数据不存在 |
| 40000 | 参数错误 |

### 10.2 分页结构（MyBatis Flex Page）

```json
{
  "records": [],
  "pageNumber": 1,
  "pageSize": 10,
  "totalRow": 100,
  "totalPage": 10
}
```

### 10.3 对话历史游标分页结构

```json
{
  "records": [
    {
      "id": 1,
      "appId": 2,
      "messageType": "user",
      "content": "帮我做一个博客",
      "userId": 1,
      "createTime": "2026-06-28T10:00:00"
    }
  ],
  "hasMore": true,
  "nextCursor": 1
}
```

---

## 11. 常见问题排查

### Q1：生成失败 / closed

**原因**：DeepSeek 流被提前中断，或 API Key 无效。

**解决**：
1. 确认 `DEEPSEEK_API_KEY` 环境变量已设置
2. 确认已登录（生成接口需 Session）
3. 刷新页面重试，避免重复点击发送
4. 后端已用 SseEmitter 解耦客户端断开

### Q2：右侧预览显示 Spring 404 错误页

**原因**：预览 URL 用了目录形式 `/preview/html_2/`，Windows 下静态资源解析失败。

**解决**：预览 URL 改为 `/preview/html_2/index.html`（已修复）。

### Q3：预览显示纯文字而非网页

**原因**：模型返回了说明文字，不是 HTML 代码。

**解决**：系统提示词已加强；重新发送生成请求。

### Q4：后端 mvn compile 导致服务重启

**原因**：devtools 检测到 class 变更自动重启，与进行中的 SSE 请求冲突。

**解决**：运行中不要单独执行 `mvnw compile`；改代码后让 devtools 自动热重载，或完整重启。

### Q5：MySQL 列名 Unknown column

**原因**：数据库列名与 MyBatis Flex 查询不一致。

**解决**：统一使用 snake_case（`user_account`、`is_delete`），见 `create_table.sql`。

### Q6：MyBatis-Spring 版本不兼容

**原因**：Spring Boot 3 需 `mybatis-flex-spring-boot3-starter`。

**解决**：pom.xml 使用 `mybatis-flex-spring-boot3-starter:1.11.7`。

### Q7：后端启动失败，Redis 连接超时

**原因**：启用 Redis Session 后，Spring Boot 启动时会连接 Redis；本地未启动 Redis 容器。

**解决**：

```bash
docker start redis-custom
# 或
docker run -d --name redis-custom -p 6379:6379 redis:7
```

确认 `application.yml` 中 `spring.data.redis.host/port` 与容器一致。

### Q8：刷新页面后对话历史丢失

**原因**：历史未持久化，或前端仍使用硬编码欢迎语而非调用 `/app/chat/history`。

**解决**：确认 `chat_history` 表已创建；生成接口会保存 user/ai/error 三类消息；前端 `AppChatView` 在 `onMounted` 时调用 `loadChatHistory()`。

### Q9：多轮对话 AI 仍完全重写网站

**原因**：对话记忆未注入，或历史为空（首次生成正常）。

**解决**：确认 `buildPromptWithMemory` 在 `saveUserMessage` **之前**调用；数据库中应有上一轮 user/ai 记录；系统提示词要求「增量改进」。

### Q10：Vue 工程项目预览空白

**原因**：Vite 工程不能直接用 `iframe srcdoc` 或静态 `index.html` 预览源码目录，需先 `npm run build` 再预览 `dist/`。

**解决**：见 [12.7 工程项目构建与浏览](#127-工程项目构建与浏览)；确认 Node.js 已安装且构建成功。

---

## 12. 工程项目生成

> 参考能力：美团 NoCode（React + 思考过程 + 工具调用）、百度秒哒、Coze 等 AI 应用生成平台。本平台以 **Vue3 + Vite** 为示例，生成符合企业标准的前端工程化项目。

### 12.1 需求分析

#### 什么是前端工程化项目？

前端工程化项目是指使用现代化工具链、规范化流程和组件化架构构建的前端应用。相比传统 HTML / CSS / JavaScript 三件套，它具备：

| 特性 | 说明 |
|------|------|
| 模块管理 | ES Module、`import/export`、按文件组织代码 |
| 自动化构建 | Vite / Webpack 打包、Tree Shaking、代码分割 |
| 开发体验 | 热更新 HMR、TypeScript 类型检查 |
| 规范流程 | ESLint、Prettier、目录约定、路由与状态管理 |

#### 平台目标

在现有 **HTML / 多文件** 两种模式基础上，新增第三种模式 **`vue_project`**，实现：

1. **流式输出**：生成过程实时展示（含思考过程、工具调用进度）
2. **网站浏览**：构建后可预览运行效果
3. **在线部署**：一键部署构建产物
4. **多轮迭代**：结合对话历史，在已有工程上增量修改（非整项目重写）

#### 与竞品对比

| 平台 | 框架 | 特点 |
|------|------|------|
| 美团 NoCode | React | 「思考中…」展示规划、多文件 Agent 生成、右侧实时预览 |
| 百度秒哒 | 多栈 | 低代码 + AI 生成 |
| Coze | 插件化 | 工作流 + 工具编排 |
| **本平台** | **Vue3 + Vite** | 与自身技术栈一致，可生成标准 Vue 工程 |

#### NoCode 交互参考（UI 目标）

生成复杂项目时，左侧对话区应展示：

1. **思考中…**（可折叠）：需求分析 → 页面规划 → 文件清单 → 技术选型
2. **工具调用进度**：正在保存 `src/views/Index.vue` …
3. **生成中…**：右侧预览区加载构建产物

### 12.2 方案设计

#### 方案 1：直接输出 Markdown（延续现有思路）

AI 在 Markdown 中输出多个 ` ```vue ` / ` ```ts ` 代码块，后端用 `CodeParser` 解析后批量落盘。

| 优点 | 缺点 |
|------|------|
| 与 HTML / 多文件模式一致，改动小 | 大项目输出不稳定，易截断或漏文件 |
| 实现简单 | 难以增量修改单个文件 |
| 流式体验与现有一致 | 无法精确控制「何时写哪个文件」 |

**适用**：快速验证、小型 Demo（3～5 个文件）。

#### 方案 2：工具调用（推荐核心方案）

给 AI 提供 `saveFile`、`readFile`、`listFiles` 等工具，由模型自主决定何时写文件、写哪些文件。

| 优点 | 缺点 |
|------|------|
| 逐文件写入，适合多文件工程 | 需设计工具集与上下文（appId） |
| 支持增量修改（覆盖已有文件） | SSE 协议需扩展（tool-call 事件） |
| LangChain4j 原生支持 `@Tool` | 单次生成耗时更长（多次工具调用） |

**适用**：Vue3 + Vite 标准工程（推荐作为默认工程模式）。

#### 方案 3：Agent 模式（方案 2 + 思考 UI）

在方案 2 基础上，将 Agent 的「感知 → 推理 → 计划 → 执行」过程结构化展示：

```
用户：「酒店预订」
  ↓
Agent 思考：分析需求 → 规划页面（首页/详情/预订/我的订单）→ 选定 Vue3+Vite+Router
  ↓
Agent 执行：逐个 saveFile → 触发 npm build → 返回预览地址
  ↓
用户：「加一个搜索框」
  ↓
Agent：readFile + saveFile 增量修改
```

| 优点 | 缺点 |
|------|------|
| 体验最接近 NoCode | 实现复杂度最高 |
| 用户可理解 AI 在做什么 | 对模型推理能力要求高 |
| 便于调试与教学 | Token 消耗大 |

**推荐组合**：**方案 2（工具调用）+ 方案 3（思考过程 UI 展示）**，Markdown 解析作为降级兜底。

### 12.3 技术选型（Vue 前端工程化技术栈）

以本平台自身前端栈为生成标准：

| 分类 | 选型 |
|------|------|
| 核心框架 | Vue 3 + Composition API + TypeScript |
| 构建工具 | Vite |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia |
| UI 组件库 | Ant Design Vue（可选） |
| 代码规范 | ESLint + Prettier（package.json scripts） |
| 包管理 | npm |

生成产物目录：`tmp/code_output/vue_project_{appId}/`

### 12.4 整体架构

```mermaid
flowchart TB
    subgraph Frontend
        A[AppChatView] -->|SSE| B[/app/chat/gen/code]
        A --> C[思考中 UI]
        A --> D[工具调用进度]
        A --> E[iframe 预览 dist]
    end

    subgraph Backend
        B --> F[ChatHistoryService 记忆注入]
        F --> G[VueProjectAgentService]
        G --> H[VueProjectFileTool saveFile/readFile/listFiles]
        H --> I[tmp/code_output/vue_project_appId]
        G --> J[VueProjectBuildService]
        J -->|npm run build| K[dist/]
    end

    subgraph Static
        K --> L[/preview/vue_project_appId/dist/]
        K --> M[/static/deployKey/ 部署]
    end
```

### 12.5 工具调用设计

#### 工具清单

| 工具 | 参数 | 说明 |
|------|------|------|
| `saveFile` | `path`, `content` | 相对项目根路径写文件；覆盖已有文件 |
| `readFile` | `path` | 读取已有文件（增量修改时使用） |
| `listFiles` | `dir`（可选） | 列出目录下文件，辅助 AI 了解工程结构 |

#### 上下文传递

工具需知道当前 `appId`，通过 `VueProjectContext`（ThreadLocal）在调用 Agent 前设置：

```java
VueProjectContext.setAppId(appId);
try {
    agentService.generateStream(prompt);
} finally {
    VueProjectContext.clear();
}
```

落盘根目录：`{CODE_OUTPUT_ROOT}/vue_project_{appId}/`

#### LangChain4j 示例

```java
@Component
public class VueProjectFileTool {

    @Tool("将代码保存到 Vue 工程项目中的指定路径")
    public String saveFile(
            @P("path") String path,
            @P("content") String content) {
        Long appId = VueProjectContext.getAppId();
        // 写入 tmp/code_output/vue_project_{appId}/{path}
        return "已保存：" + path;
    }
}

@AiService
public interface VueProjectAgentService {
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectStream(String userMessage);
}
```

### 12.6 工具调用流式输出（SSE 协议扩展）

在现有 SSE 基础上新增事件类型：

| 事件名 | data 示例 | 前端处理 |
|--------|-----------|----------|
| `message`（默认） | 文本 token | 追加到 AI 消息 / 思考区 |
| `thinking` | 思考片段 | 折叠面板「思考中…」 |
| `tool-start` | `{"tool":"saveFile","path":"src/App.vue"}` | 显示工具调用进度 |
| `tool-result` | `{"tool":"saveFile","result":"已保存"}` | 更新进度状态 |
| `build-log` | `npm run build 输出行` | 构建日志区 |
| `gen-error` | 错误信息 | 红色提示 |
| `done` | `[DONE]` | 结束流，触发预览 |

**TokenStream 伪代码**：

```java
tokenStream
    .onPartialResponse(text -> emitter.send(SseEmitter.event().data(text)))
    .onToolExecuted(exec -> emitter.send(
        SseEmitter.event().name("tool-start").data(toJson(exec))))
    .onCompleteResponse(resp -> {
        buildService.build(appId);
        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        emitter.complete();
    })
    .start();
```

### 12.7 工程项目构建与浏览

Vue 工程**不能**像 HTML 模式那样直接预览源码，需先构建：

```bash
cd tmp/code_output/vue_project_{appId}
npm install
npm run build
```

构建成功后，预览 URL：

```
http://localhost:8123/api/preview/vue_project_{appId}/dist/index.html
```

| 阶段 | 预览方式 |
|------|----------|
| 生成中 | 左侧展示思考 + 工具进度；右侧显示「生成中…」 |
| 构建中 | 右侧显示构建日志（`build-log` 事件） |
| 构建完成 | iframe 加载 `dist/index.html` |
| 多轮修改 | 仅重新 build 变更后的工程 |

**前置依赖**：服务器需安装 Node.js 18+，且 `npm` 在 PATH 中。

**VueProjectBuildService 职责**：

1. 检测 `package.json` 是否存在
2. 执行 `npm install`（首次或依赖变更）
3. 执行 `npm run build`
4. 返回构建结果（成功 / 失败 + 日志）

### 12.8 工程项目部署

与 HTML 模式类似，但部署 **`dist/` 目录**而非源码：

```
deploy 流程：
1. 确认 vue_project_{appId}/dist/ 存在（不存在则先 build）
2. 复制 dist/ → tmp/code_deploy/{deployKey}/
3. 更新 app.deploy_key、deployed_time
4. 返回 http://localhost:8123/api/static/{deployKey}/index.html
```

### 12.9 前端改造要点

#### 创建应用时选择生成模式

`AppAddRequest` 增加 `codeGenType` 字段；主页创建时可选择：

- 原生 HTML（默认）
- 原生多文件
- **Vue3 工程**

#### AppChatView 改造

| 模块 | 改动 |
|------|------|
| 思考面板 | 可折叠「思考中…」，展示 AI 规划内容 |
| 工具进度 | `tool-start` / `tool-result` 渲染为文件保存列表 |
| SSE 消费 | 扩展 `utils/sse.ts`，支持命名事件回调 |
| 预览逻辑 | `codeGenType === 'vue_project'` 时预览 `dist/index.html` |
| 部署按钮 | vue 模式先 build 再 deploy |

#### buildPreviewUrl 扩展

```typescript
export function buildPreviewUrl(codeGenType: string, appId: number) {
  if (codeGenType === 'vue_project') {
    return `${API_BASE_URL}/preview/vue_project_${appId}/dist/index.html`
  }
  return `${API_BASE_URL}/preview/${codeGenType}_${appId}/index.html`
}
```

### 12.10 系统提示词

完整提示词见：

`server/server/src/main/resources/prompt/codegen-vue-project-system-prompt.txt`

**核心约束摘要**：

1. 必须生成完整 Vue3 + Vite 工程目录（含 `package.json`、`vite.config.ts`）
2. 使用 **saveFile 工具**逐文件写入，禁止一次性输出巨型 Markdown
3. 思考阶段先输出需求分析与文件清单（对应 NoCode「思考中…」）
4. 多轮对话时 **增量修改**已有文件，结合 `readFile` + `saveFile`
5. 代码须能通过 `npm run build`

### 12.11 实现清单（已完成）

| 模块 | 状态 | 关键类 / 文件 |
|------|------|----------------|
| 枚举 + 提示词 | ✅ | `CodeGenTypeEnum.VUE_PROJECT`、`codegen-vue-project-system-prompt.txt` |
| 工具调用 Agent | ✅ | `VueProjectFileTool`、`VueProjectAgentService`、`VueProjectAgentConfig` |
| 构建服务 | ✅ | `VueProjectBuildService`（npm install + build） |
| SSE 集成 | ✅ | `VueProjectCodegenExecutor`、`AppController.chatToGenVueProject` |
| 部署适配 | ✅ | `deployApp` 复制 `dist/` |
| 前端 | ✅ | 主页模式选择、`sse.ts` 命名事件、`AppChatView` 工具进度 |

### 12.12 与对话历史的协同

工程模式下 AI 回复可能很长（多文件 + 构建日志），建议：

- 对话历史 `saveAiMessage` 保存**摘要**（如「已生成 12 个文件并完成构建」），完整日志不入库
- `buildPromptWithMemory` 对 AI 历史仍截断 2000 字符，但工具模式下可改为 `listFiles` + `readFile` 按需读取

---

## 附录：开发时间线（对话记录摘要）

| 阶段 | 内容 |
|------|------|
| 1 | 全局基础布局：BasicLayout、GlobalHeader、GlobalFooter |
| 2 | 用户模块：Session 鉴权、MyBatis Flex、AOP 权限、前后端 CRUD |
| 3 | AI 应用生成：LangChain4j + DeepSeek、两种生成模式、SSE 流式、门面/模板方法 |
| 4 | 应用管理：app 表、用户/管理员接口、主页+对话页+管理页 |
| 5 | 在线部署：deploy 接口、静态资源映射 |
| 6 | 实时预览：/preview 映射、两段式 iframe 预览 |
| 7 | 问题修复：EventSource→fetch SSE、SseEmitter、预览 URL、closed 错误 |
| 8 | 对话历史：chat_history 表、游标分页、AI 记忆注入、级联删除 |
| 9 | Redis 分布式 Session：spring-session-data-redis、多实例共享登录态 |
| 10 | 工程项目生成（设计）：Vue3+Vite 方案对比、工具调用、构建预览部署、系统提示词 |
| 11 | 工程项目生成（实现）：Agent 工具落盘、npm build、SSE tool-start/build-log、前端模式选择 |

---

*文档更新时间：2026-06-28 · CODE 原创项目 by ZhenQ*

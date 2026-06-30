<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  CloudUploadOutlined,
  EditOutlined,
  PaperClipOutlined,
  ReloadOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { buildChatGenUrl, buildPreviewUrl, checkPreviewReady, deployApp, getAppVoById, listChatHistory } from '@/api/app'
import type { AppVO, ChatHistoryVO } from '@/api/types'
import { CODE_GEN_TYPE } from '@/api/types'
import { ACCESS_ENUM, useLoginUserStore } from '@/stores/loginUser'
import { consumeSseStream } from '@/utils/sse'

interface ChatMessage {
  id?: number
  role: 'ai' | 'user' | 'error'
  content: string
  streaming?: boolean
}

const WELCOME_MESSAGE: ChatMessage = {
  role: 'ai',
  content:
    '你好！我是 NoCode，你的 AI 应用开发助手。\n\n我可以帮你：\n· 构建结构化的页面与交互\n· 集成数据与样式美化\n· 一句话生成可运行的网页应用\n\n告诉我你想要什么样的应用，我立即为你生成！',
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = computed(() => Number(route.params.id))
const app = ref<AppVO | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const generating = ref(false)
const deploying = ref(false)
const deployUrl = ref('')
const previewHtml = ref('')
const previewUrl = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const historyLoading = ref(false)
const historyHasMore = ref(false)
const historyNextCursor = ref<number | null>(null)

let abortController: AbortController | null = null

const appName = computed(() => app.value?.appName || '智能助手')
const codeGenType = computed(() => app.value?.codeGenType || 'html')
const isLogin = computed(
  () => !!loginUserStore.loginUser.id && loginUserStore.loginUser.userRole !== ACCESS_ENUM.NOT_LOGIN,
)
const isVueProject = computed(() => codeGenType.value === CODE_GEN_TYPE.VUE_PROJECT)

function isToolCallLikeText(text: string): boolean {
  const t = text.trim()
  return (t.includes('"path"') && t.includes('"content"')) || (t.startsWith('saveFile') && t.includes('{'))
}

function formatVueAiContent(thinking: string, tools: string[]): string {
  const parts: string[] = []
  if (thinking.trim()) {
    parts.push(thinking.trim())
  }
  if (tools.length) {
    parts.push(tools.slice(-12).join('\n'))
  }
  return parts.join('\n\n') || '正在生成 Vue 工程…'
}

/**
 * 从 AI 流式返回文本中提取可预览的 HTML（过滤纯文字说明）
 */
function extractHtml(raw: string): string {
  const htmlFence = raw.match(/```html\s*([\s\S]*?)```/i)
  if (htmlFence?.[1]) return htmlFence[1].trim()
  const anyFence = raw.match(/```[a-z]*\s*([\s\S]*?)```/i)
  if (anyFence?.[1]) return anyFence[1].trim()
  const trimmed = raw.trim()
  if (/^<!DOCTYPE/i.test(trimmed) || /^<html/i.test(trimmed)) return trimmed
  // 流式进行中：```html 尚未闭合
  const partial = raw.match(/```html\s*([\s\S]*)/i)
  if (partial?.[1]) return partial[1].replace(/```\s*$/, '').trim()
  return ''
}

const finishPreview = () => {
  previewUrl.value = `${buildPreviewUrl(codeGenType.value, appId.value)}?t=${Date.now()}`
}

const loadVuePreviewIfReady = async () => {
  if (!isVueProject.value) return
  try {
    const res = await checkPreviewReady(appId.value)
    if (res.code === 0 && res.data) {
      finishPreview()
    }
  } catch {
    // ignore
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const mapHistoryToMessage = (item: ChatHistoryVO): ChatMessage => ({
  id: item.id,
  role: item.messageType === 'user' ? 'user' : item.messageType === 'error' ? 'error' : 'ai',
  content: item.content,
})

const showWelcomeIfNeeded = () => {
  if (messages.value.length === 0) {
    messages.value.push({ ...WELCOME_MESSAGE })
  }
}

/**
 * 加载对话历史（首次或向前加载更多）
 */
const loadChatHistory = async (loadMore = false) => {
  if (historyLoading.value) return
  if (loadMore && !historyHasMore.value) return

  historyLoading.value = true
  const el = messageListRef.value
  const prevScrollHeight = el?.scrollHeight ?? 0

  try {
    const res = await listChatHistory({
      appId: appId.value,
      pageSize: 10,
      lastId: loadMore ? (historyNextCursor.value ?? undefined) : undefined,
    })
    if (res.code !== 0 || !res.data) {
      if (!loadMore) showWelcomeIfNeeded()
      return
    }

    const { records, hasMore, nextCursor } = res.data
    historyHasMore.value = !!hasMore
    historyNextCursor.value = nextCursor ?? null

    const mapped = records.map(mapHistoryToMessage)
    if (loadMore) {
      messages.value = [...mapped, ...messages.value]
      nextTick(() => {
        if (el) el.scrollTop = el.scrollHeight - prevScrollHeight
      })
    } else {
      messages.value = mapped
      if (messages.value.length === 0) {
        showWelcomeIfNeeded()
      } else if (isVueProject.value) {
        const readyRes = await checkPreviewReady(appId.value)
        if (readyRes.code === 0 && readyRes.data) {
          previewUrl.value = buildPreviewUrl(codeGenType.value, appId.value)
        }
      } else {
        previewUrl.value = buildPreviewUrl(codeGenType.value, appId.value)
      }
      scrollToBottom()
    }
  } catch {
    if (!loadMore) showWelcomeIfNeeded()
  } finally {
    historyLoading.value = false
  }
}

const loadApp = async () => {
  const res = await getAppVoById(appId.value)
  if (res.code === 0 && res.data) {
    app.value = res.data
  } else {
    message.error(res.message || '应用不存在')
    router.push('/')
  }
}

const closeStream = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

/**
 * 发起一次生成
 */
const generate = async (userPrompt: string) => {
  if (generating.value) {
    message.warning('正在生成中，请稍候')
    return
  }
  const prompt = userPrompt.trim()
  if (!prompt) return

  if (!isLogin.value) {
    message.warning('请先登录')
    router.push(`/user/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  messages.value.push({ role: 'user', content: prompt })
  const aiIndex = messages.value.push({ role: 'ai', content: '', streaming: true }) - 1
  const aiMsg = messages.value[aiIndex] as ChatMessage
  scrollToBottom()

  generating.value = true
  previewUrl.value = ''
  previewHtml.value = ''
  let raw = ''
  const toolLogs: string[] = []
  let vuePreviewReady = false

  closeStream()
  abortController = new AbortController()
  const url = buildChatGenUrl(appId.value, prompt)

  await consumeSseStream({
    url,
    signal: abortController.signal,
    onChunk: (chunk) => {
      if (isVueProject.value && isToolCallLikeText(chunk)) {
        return
      }
      raw += chunk
      aiMsg.content = isVueProject.value ? formatVueAiContent(raw, toolLogs) : raw
      if (!isVueProject.value) {
        previewHtml.value = extractHtml(raw)
      }
      scrollToBottom()
    },
    onEvent: (eventName, data) => {
      if (eventName === 'tool-start') {
        toolLogs.push(`📁 ${data}`)
        aiMsg.content = formatVueAiContent(raw, toolLogs)
        scrollToBottom()
      } else if (eventName === 'build-log') {
        toolLogs.push(`⚙ ${data}`)
        aiMsg.content = formatVueAiContent(raw, toolLogs)
      } else if (eventName === 'preview-ready') {
        vuePreviewReady = true
        finishPreview()
      } else if (eventName === 'cover-ready') {
        message.success('应用封面已生成')
      }
    },
    onDone: () => {
      aiMsg.streaming = false
      generating.value = false
      if (!isVueProject.value) {
        previewHtml.value = extractHtml(raw)
        finishPreview()
      } else if (vuePreviewReady) {
        finishPreview()
      } else {
        message.warning('Vue 工程尚未构建完成，请等待构建结束或重试')
      }
      closeStream()
    },
    onError: (errMsg) => {
      aiMsg.streaming = false
      generating.value = false
      previewUrl.value = ''
      if (raw.trim() || toolLogs.length) {
        aiMsg.content = isVueProject.value ? formatVueAiContent(raw, toolLogs) : raw
        if (!isVueProject.value) {
          previewHtml.value = extractHtml(raw)
          finishPreview()
          message.warning('生成过程中断，已展示部分内容')
        } else {
          aiMsg.content += `\n\n❌ ${errMsg}`
          message.error(errMsg)
        }
      } else {
        aiMsg.content = errMsg
        aiMsg.role = 'error'
        message.error(errMsg)
      }
      closeStream()
    },
  })
}

const handleSend = () => {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  generate(text)
}

const handleRefresh = async () => {
  if (isVueProject.value) {
    await loadVuePreviewIfReady()
    if (!previewUrl.value) {
      message.warning('预览尚未就绪，请等待构建完成或重新生成')
    }
    return
  }
  if (app.value?.initPrompt) generate(app.value.initPrompt)
}

const handleDeploy = async () => {
  if (!app.value) return
  if (generating.value) {
    message.warning('请等待代码生成完成再部署')
    return
  }
  deploying.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.code === 0 && res.data) {
      deployUrl.value = res.data
      message.success('部署成功')
      window.open(res.data, '_blank')
    } else {
      message.error(res.message || '部署失败')
    }
  } finally {
    deploying.value = false
  }
}

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  await loadApp()
  await loadChatHistory()
  await loadVuePreviewIfReady()
  // 从主页跳转携带 auto，自动用 initPrompt 触发一次生成
  if (route.query.auto === '1' && app.value?.initPrompt) {
    router.replace({ path: route.path })
    generate(app.value.initPrompt)
  }
})

onBeforeUnmount(closeStream)
</script>

<template>
  <div class="chat-page">
    <!-- 顶部栏 -->
    <header class="chat-page__bar">
      <div class="chat-page__name">{{ appName }}</div>
      <a-button type="primary" :loading="deploying" @click="handleDeploy">
        <template #icon><CloudUploadOutlined /></template>
        部署
      </a-button>
    </header>

    <div class="chat-page__body">
      <!-- 左侧对话 -->
      <section class="chat-panel">
        <div ref="messageListRef" class="chat-panel__messages">
          <div v-if="historyHasMore" class="chat-panel__load-more">
            <a-button type="link" size="small" :loading="historyLoading" @click="loadChatHistory(true)">
              加载更早的消息
            </a-button>
          </div>
          <div
            v-for="(msg, idx) in messages"
            :key="msg.id ?? idx"
            class="msg"
            :class="{
              'msg--user': msg.role === 'user',
              'msg--ai': msg.role === 'ai',
              'msg--error': msg.role === 'error',
            }"
          >
            <a-avatar
              v-if="msg.role === 'ai' || msg.role === 'error'"
              class="msg__avatar"
              :style="{ background: msg.role === 'error' ? '#ff4d4f' : '#1677ff' }"
            >
              <template #icon><RobotOutlined /></template>
            </a-avatar>
            <div class="msg__bubble">
              <pre class="msg__content">{{ msg.content }}</pre>
              <span v-if="msg.streaming" class="msg__cursor">▋</span>
            </div>
            <a-avatar
              v-if="msg.role === 'user'"
              class="msg__avatar"
              :src="loginUserStore.loginUser.userAvatar"
            >
              <template #icon><UserOutlined /></template>
            </a-avatar>
          </div>
        </div>

        <div class="chat-input">
          <a-textarea
            v-model:value="inputText"
            :rows="3"
            :bordered="false"
            placeholder="描述需求、页面或具体功能，可以一步步完善生成效果"
            @press-enter.prevent="handleSend"
          />
          <div class="chat-input__bar">
            <div class="chat-input__tools">
              <a-button type="text" size="small">
                <template #icon><PaperClipOutlined /></template>
                上传
              </a-button>
              <a-button type="text" size="small">
                <template #icon><ThunderboltOutlined /></template>
                优化
              </a-button>
              <a-button type="text" size="small">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
            </div>
            <a-button
              type="primary"
              shape="circle"
              :loading="generating"
              @click="handleSend"
            >
              <template #icon><ArrowUpOutlined /></template>
            </a-button>
          </div>
        </div>
      </section>

      <!-- 右侧预览 -->
      <section class="preview-panel">
        <div class="preview-panel__toolbar">
          <span class="preview-panel__hint">生成后的网页展示</span>
          <div class="preview-panel__actions">
            <a v-if="deployUrl" :href="deployUrl" target="_blank" class="preview-panel__link">
              已部署：{{ deployUrl }}
            </a>
            <a-button type="text" size="small" @click="handleRefresh">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </div>
        </div>
        <div class="preview-panel__stage">
          <iframe
            v-if="previewUrl"
            class="preview-panel__iframe"
            :src="previewUrl"
            sandbox="allow-scripts allow-same-origin"
          />
          <iframe
            v-else-if="previewHtml"
            class="preview-panel__iframe"
            :srcdoc="previewHtml"
            sandbox="allow-scripts allow-same-origin"
          />
          <div v-else class="preview-panel__welcome">
            <h2 v-if="generating && isVueProject">生成中…</h2>
            <h2 v-else-if="isVueProject && !previewUrl">等待构建完成</h2>
            <h2 v-else>欢迎页！</h2>
            <p v-if="generating && isVueProject">AI 正在创建 Vue 工程文件并执行 npm build</p>
            <p v-else-if="isVueProject && !previewUrl">构建成功后此处将展示网页预览</p>
            <p v-else>开始构建你的神奇应用！</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 70px);
  min-height: 520px;
}

.chat-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.chat-page__name {
  font-size: 16px;
  font-weight: 600;
}

.chat-page__body {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* 左侧对话 */
.chat-panel {
  display: flex;
  flex: 0 0 38%;
  flex-direction: column;
  min-width: 360px;
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
}

.chat-panel__messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.chat-panel__load-more {
  margin-bottom: 12px;
  text-align: center;
}

.msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.msg--user {
  flex-direction: row-reverse;
}

.msg__avatar {
  flex: 0 0 auto;
}

.msg__bubble {
  position: relative;
  max-width: 80%;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
}

.msg--user .msg__bubble {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
}

.msg--error .msg__bubble {
  color: #cf1322;
  background: #fff2f0;
  border-color: #ffccc7;
}

.msg__content {
  margin: 0;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg__cursor {
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  to {
    visibility: hidden;
  }
}

.chat-input {
  margin: 12px;
  background: #fff;
  border: 1px solid #eaeaea;
  border-radius: 12px;
}

.chat-input__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 10px 8px;
}

.chat-input__tools {
  display: flex;
  gap: 2px;
  color: rgba(0, 0, 0, 0.5);
}

/* 右侧预览 */
.preview-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.preview-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.preview-panel__hint {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.preview-panel__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.preview-panel__link {
  max-width: 360px;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-panel__stage {
  flex: 1;
  min-height: 0;
  background: #fff;
}

.preview-panel__iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-panel__welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(0, 0, 0, 0.65);
}

.preview-panel__welcome h2 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .chat-page__body {
    flex-direction: column;
  }

  .chat-panel {
    flex: 1 1 auto;
    min-width: 0;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
  }
}
</style>

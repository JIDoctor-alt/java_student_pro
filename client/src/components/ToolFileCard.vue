<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { DownOutlined, FileImageOutlined, FileTextOutlined, LoadingOutlined, UpOutlined } from '@ant-design/icons-vue'

export interface FileToolActivity {
  id: string
  action: 'save' | 'read'
  path: string
  content: string
  streaming: boolean
  mediaType: 'code' | 'image' | 'text'
  truncated?: boolean
}

const props = defineProps<{
  activity: FileToolActivity
}>()

const collapsed = ref(false)
/** 屏幕上逐字/逐块揭示的内容（滞后于 activity.content，形成流式视觉效果） */
const displayContent = ref('')
let revealTimer: ReturnType<typeof setInterval> | null = null

const toggleCollapse = () => {
  collapsed.value = !collapsed.value
}

const actionLabel = computed(() => (props.activity.action === 'save' ? '写入' : '读取'))

const fileName = computed(() => {
  const parts = props.activity.path.split('/')
  return parts[parts.length - 1] || props.activity.path
})

/** 是否仍在流式揭示中 */
const isRevealing = computed(
  () => displayContent.value.length < props.activity.content.length,
)

const lineCount = computed(() => {
  const text = displayContent.value || props.activity.content
  if (!text) return 0
  return text.split('\n').length
})

const imageSrc = computed(() => {
  const c = props.activity.content.trim()
  if (!c) return ''
  if (c.startsWith('data:image') || c.startsWith('http://') || c.startsWith('https://')) {
    return c
  }
  const lower = props.activity.path.toLowerCase()
  if (lower.endsWith('.png')) return `data:image/png;base64,${c.replace(/\s/g, '')}`
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return `data:image/jpeg;base64,${c.replace(/\s/g, '')}`
  if (lower.endsWith('.gif')) return `data:image/gif;base64,${c.replace(/\s/g, '')}`
  if (lower.endsWith('.webp')) return `data:image/webp;base64,${c.replace(/\s/g, '')}`
  return ''
})

const showImage = computed(
  () => props.activity.mediaType === 'image' && !!imageSrc.value && !props.activity.streaming && !isRevealing.value,
)

const codeLines = computed(() => displayContent.value.split('\n'))

const codeRef = ref<HTMLElement | null>(null)

const scrollCodeToBottom = () => {
  nextTick(() => {
    const el = codeRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const stopReveal = () => {
  if (revealTimer) {
    clearInterval(revealTimer)
    revealTimer = null
  }
}

/** 启动流式揭示：每 tick 追加若干字符，追上 activity.content */
const startReveal = () => {
  if (revealTimer) return
  revealTimer = setInterval(() => {
    const target = props.activity.content
    const current = displayContent.value.length
    if (current >= target.length) {
      if (!props.activity.streaming) {
        stopReveal()
      }
      return
    }
    // 生成中：慢速揭示；已完成：快速追齐剩余内容
    const step = props.activity.streaming ? 14 : 120
    displayContent.value = target.slice(0, Math.min(current + step, target.length))
    scrollCodeToBottom()
  }, 32)
}

watch(
  () => props.activity.id,
  () => {
    displayContent.value = ''
    stopReveal()
  },
)

watch(
  () => props.activity.content,
  (val) => {
    if (!val) {
      displayContent.value = ''
      return
    }
    startReveal()
  },
)

watch(
  () => props.activity.streaming,
  (streaming) => {
    if (!streaming) {
      // 结束后若仍有未揭示内容，加速追齐
      startReveal()
    }
  },
)

onBeforeUnmount(stopReveal)
</script>

<template>
  <div class="tool-file" :class="{ 'tool-file--streaming': activity.streaming || isRevealing }">
    <div class="tool-file__header" role="button" tabindex="0" @click="toggleCollapse" @keydown.enter="toggleCollapse">
      <FileImageOutlined v-if="activity.mediaType === 'image'" class="tool-file__icon tool-file__icon--image" />
      <FileTextOutlined v-else class="tool-file__icon" />
      <span class="tool-file__action">{{ actionLabel }}</span>
      <span class="tool-file__path">{{ activity.path }}</span>
      <span v-if="!showImage && lineCount > 0" class="tool-file__meta">+{{ lineCount }}</span>
      <LoadingOutlined v-if="activity.streaming || isRevealing" spin class="tool-file__spinner" />
      <UpOutlined v-else-if="!collapsed" class="tool-file__toggle" />
      <DownOutlined v-else class="tool-file__toggle" />
    </div>

    <template v-if="!collapsed">
      <div v-if="showImage" class="tool-file__image-wrap">
        <img :src="imageSrc" :alt="fileName" class="tool-file__image" />
      </div>

      <div v-else-if="displayContent" ref="codeRef" class="tool-file__code">
        <div v-for="(line, idx) in codeLines" :key="idx" class="tool-file__line">
          <span class="tool-file__ln">{{ idx + 1 }}</span>
          <span class="tool-file__text">
            {{ line || ' ' }}<span
              v-if="isRevealing && idx === codeLines.length - 1"
              class="tool-file__cursor"
            />
          </span>
        </div>
      </div>

      <div v-else-if="activity.streaming" class="tool-file__placeholder">
        <span class="tool-file__placeholder-dot" />
        正在流式写入文件…
      </div>

      <p v-if="activity.truncated" class="tool-file__truncated">内容过长，仅展示前 30000 字符</p>
    </template>
  </div>
</template>

<style scoped>
.tool-file {
  margin-top: 10px;
  overflow: hidden;
  border: 1px solid #30363d;
  border-radius: 8px;
  background: #0d1117;
}

.tool-file--streaming {
  border-color: #1677ff66;
  box-shadow: 0 0 0 1px #1677ff33;
}

.tool-file__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  font-size: 13px;
  color: #c9d1d9;
  cursor: pointer;
  user-select: none;
}

.tool-file__header:hover {
  background: #1c2128;
}

.tool-file__icon {
  color: #58a6ff;
  font-size: 14px;
}

.tool-file__icon--image {
  color: #3fb950;
}

.tool-file__action {
  color: #8b949e;
  font-weight: 500;
}

.tool-file__path {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-family: 'Consolas', 'Monaco', monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-file__meta {
  padding: 0 6px;
  color: #3fb950;
  font-size: 12px;
  font-weight: 600;
  transition: color 0.15s;
}

.tool-file--streaming .tool-file__meta {
  color: #58a6ff;
}

.tool-file__spinner {
  color: #1677ff;
  font-size: 14px;
}

.tool-file__toggle {
  color: #8b949e;
  font-size: 12px;
}

.tool-file__code {
  max-height: 320px;
  overflow: auto;
  padding: 8px 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.55;
}

.tool-file__line {
  display: flex;
  min-height: 1.55em;
  animation: line-in 0.12s ease-out;
}

@keyframes line-in {
  from {
    opacity: 0.4;
  }
  to {
    opacity: 1;
  }
}

.tool-file__ln {
  flex: 0 0 42px;
  padding-right: 12px;
  color: #484f58;
  text-align: right;
  user-select: none;
}

.tool-file__text {
  flex: 1;
  padding-right: 12px;
  color: #7ee787;
  white-space: pre-wrap;
  word-break: break-word;
}

.tool-file__cursor {
  display: inline-block;
  width: 7px;
  height: 1.1em;
  margin-left: 1px;
  vertical-align: text-bottom;
  background: #7ee787;
  animation: blink 0.9s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.tool-file__image-wrap {
  padding: 12px;
  text-align: center;
  background: #010409;
}

.tool-file__image {
  max-width: 100%;
  max-height: 240px;
  border-radius: 6px;
  object-fit: contain;
  animation: fade-in 0.35s ease-out;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.tool-file__placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  color: #8b949e;
  font-size: 13px;
}

.tool-file__placeholder-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1677ff;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.tool-file__truncated {
  margin: 0;
  padding: 6px 12px;
  border-top: 1px solid #30363d;
  color: #d29922;
  font-size: 12px;
  background: #161b22;
}
</style>

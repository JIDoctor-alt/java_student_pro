<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { MessageOutlined } from '@ant-design/icons-vue'
import type { AppVO } from '@/api/types'

const props = defineProps<{ app: AppVO }>()

const router = useRouter()

/**
 * 相对时间（如 6 天前、1 周前、3 个月前）
 */
const relativeTime = computed(() => {
  const time = props.app.createTime
  if (!time) return ''
  const diff = Date.now() - new Date(time.replace(' ', 'T')).getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const week = 7 * day
  const month = 30 * day
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < week) return `${Math.floor(diff / day)} 天前`
  if (diff < month) return `${Math.floor(diff / week)} 周前`
  return `${Math.floor(diff / month)} 个月前`
})

const goChat = () => {
  router.push(`/app/chat/${props.app.id}`)
}
</script>

<template>
  <div class="app-card" @click="goChat">
    <div class="app-card__cover">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" class="app-card__cover-img" />
      <div v-else class="app-card__cover-placeholder">
        <span class="app-card__cover-emoji">🐱</span>
      </div>
      <div class="app-card__mask">
        <a-button type="primary" ghost>
          <template #icon><MessageOutlined /></template>
          查看对话
        </a-button>
      </div>
    </div>
    <div class="app-card__info">
      <div class="app-card__name" :title="app.appName">{{ app.appName || '未知应用' }}</div>
      <div class="app-card__meta">创建于 {{ relativeTime }}</div>
    </div>
  </div>
</template>

<style scoped>
.app-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
}

.app-card__cover {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: #f5f5f5;
}

.app-card__cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9eef5 100%);
}

.app-card__cover-emoji {
  font-size: 40px;
  opacity: 0.45;
}

.app-card__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card:hover .app-card__mask {
  opacity: 1;
}

.app-card__info {
  padding: 12px 14px;
}

.app-card__name {
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__meta {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>

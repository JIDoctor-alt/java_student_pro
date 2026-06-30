<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined, PaperClipOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/app'
import type { AppVO } from '@/api/types'
import { CODE_GEN_TYPE } from '@/api/types'
import { ACCESS_ENUM, useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const prompt = ref('')
const creating = ref(false)
const codeGenType = ref<string>(CODE_GEN_TYPE.HTML)

const codeGenOptions = [
  { label: '原生 HTML', value: CODE_GEN_TYPE.HTML },
  { label: '多文件', value: CODE_GEN_TYPE.MULTI_FILE },
  { label: 'Vue3 工程', value: CODE_GEN_TYPE.VUE_PROJECT },
]

const isLogin = computed(
  () => !!loginUserStore.loginUser.id && loginUserStore.loginUser.userRole !== ACCESS_ENUM.NOT_LOGIN,
)

// 快捷提示词
const quickPrompts = [
  '做一个企业网站',
  '电商运营后台',
  '数据分析看板',
  '个人博客社区',
]

const myApps = ref<AppVO[]>([])
const goodApps = ref<AppVO[]>([])

const fillPrompt = (text: string) => {
  prompt.value = text
}

const handleCreate = async () => {
  const initPrompt = prompt.value.trim()
  if (!initPrompt) {
    message.warning('请输入你的应用描述')
    return
  }
  if (!isLogin.value) {
    message.warning('请先登录')
    router.push(`/user/login?redirect=${window.location.pathname}`)
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt, codeGenType: codeGenType.value })
    if (res.code === 0 && res.data) {
      // 携带 auto 标记，进入对话页后自动触发生成
      router.push({ path: `/app/chat/${res.data}`, query: { auto: '1' } })
    } else {
      message.error(res.message || '创建失败')
    }
  } finally {
    creating.value = false
  }
}

const loadMyApps = async () => {
  if (!isLogin.value) {
    myApps.value = []
    return
  }
  const res = await listMyAppVoByPage({ current: 1, pageSize: 12, sortField: 'createTime', sortOrder: 'descend' })
  if (res.code === 0 && res.data) {
    myApps.value = res.data.records
  }
}

const handleAppDeleted = (id: number) => {
  myApps.value = myApps.value.filter((app) => app.id !== id)
}

const loadGoodApps = async () => {
  const res = await listGoodAppVoByPage({ current: 1, pageSize: 12, sortField: 'createTime', sortOrder: 'descend' })
  if (res.code === 0 && res.data) {
    goodApps.value = res.data.records
  }
}

onMounted(() => {
  loadMyApps()
  loadGoodApps()
})
</script>

<template>
  <div class="home">
    <!-- Hero 区 -->
    <section class="hero">
      <h1 class="hero__title">一句话 <span class="hero__brand">🐱</span> 呈所想</h1>
      <p class="hero__subtitle">与 AI 对话轻松创建应用和网站</p>

      <a-segmented v-model:value="codeGenType" :options="codeGenOptions" class="hero__mode" />

      <div class="hero__input">
        <a-textarea
          v-model:value="prompt"
          :rows="3"
          :bordered="false"
          placeholder="使用 NoCode 创建一个数据分析看板，用……"
          class="hero__textarea"
          @press-enter.prevent="handleCreate"
        />
        <div class="hero__input-bar">
          <div class="hero__input-tools">
            <a-button type="text" size="small">
              <template #icon><PaperClipOutlined /></template>
              上传
            </a-button>
            <a-button type="text" size="small">
              <template #icon><ThunderboltOutlined /></template>
              优化
            </a-button>
          </div>
          <a-button
            type="primary"
            shape="circle"
            :loading="creating"
            class="hero__send"
            @click="handleCreate"
          >
            <template #icon><ArrowUpOutlined /></template>
          </a-button>
        </div>
      </div>

      <div class="hero__quick">
        <a-tag
          v-for="item in quickPrompts"
          :key="item"
          class="hero__quick-tag"
          @click="fillPrompt(item)"
        >
          {{ item }}
        </a-tag>
      </div>
    </section>

    <!-- 我的作品 -->
    <section v-if="isLogin" class="section">
      <h2 class="section__title">我的作品</h2>
      <div v-if="myApps.length" class="app-grid">
        <AppCard
          v-for="app in myApps"
          :key="app.id"
          :app="app"
          show-menu
          @deleted="handleAppDeleted"
        />
      </div>
      <a-empty v-else description="还没有作品，输入描述创建你的第一个应用吧" />
    </section>

    <!-- 精选应用 -->
    <section class="section">
      <h2 class="section__title">精选案例</h2>
      <div v-if="goodApps.length" class="app-grid">
        <AppCard v-for="app in goodApps" :key="app.id" :app="app" />
      </div>
      <a-empty v-else description="暂无精选应用" />
    </section>
  </div>
</template>

<style scoped>
.home {
  max-width: 1180px;
  margin: 0 auto;
  padding: 8px 0 40px;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 16px 40px;
  text-align: center;
}

.hero__title {
  margin: 0;
  font-size: 44px;
  font-weight: 800;
  letter-spacing: 2px;
  color: rgba(0, 0, 0, 0.88);
}

.hero__brand {
  font-size: 38px;
}

.hero__subtitle {
  margin: 14px 0 16px;
  font-size: 16px;
  color: rgba(0, 0, 0, 0.5);
}

.hero__mode {
  margin-bottom: 24px;
}

.hero__input {
  width: 100%;
  max-width: 720px;
  padding: 8px 8px 8px 4px;
  background: #fff;
  border: 1px solid #eaeaea;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
}

.hero__textarea {
  font-size: 15px;
  resize: none;
}

.hero__input-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
}

.hero__input-tools {
  display: flex;
  gap: 4px;
  color: rgba(0, 0, 0, 0.5);
}

.hero__quick {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.hero__quick-tag {
  padding: 6px 16px;
  font-size: 13px;
  background: #fff;
  border-radius: 18px;
  cursor: pointer;
}

.hero__quick-tag:hover {
  color: #1677ff;
  border-color: #1677ff;
}

.section {
  margin-top: 40px;
  padding: 0 16px;
}

.section__title {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 700;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 992px) {
  .app-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .hero__title {
    font-size: 32px;
  }

  .app-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>

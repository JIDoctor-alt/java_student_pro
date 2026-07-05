<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getAiModelConfig,
  listAiModelProviders,
  saveAiModelConfig,
  testAiModelConnection,
} from '@/api/aiModel'
import type { AiModelConfigVO, AiModelProviderVO, AiModelScenarioConfigVO } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const providers = ref<AiModelProviderVO[]>([])
const scenarios = ref<AiModelScenarioConfigVO[]>([])

const form = reactive({
  providerId: 'deepseek',
  baseUrl: 'https://api.deepseek.com/v1',
  apiKey: '',
  apiKeyConfigured: false,
})

const currentProvider = computed(() =>
  providers.value.find((item) => item.id === form.providerId),
)

const modelOptions = computed(() =>
  (currentProvider.value?.models ?? []).map((item) => ({
    label: item.description ? `${item.name}（${item.description}）` : item.name,
    value: item.id,
  })),
)

const columns = [
  { title: '场景', dataIndex: 'scenarioName', width: 160 },
  { title: '模型', key: 'modelName', width: 260 },
  { title: '温度', key: 'temperature', width: 120 },
  { title: 'Max Tokens', key: 'maxTokens', width: 140 },
]

const loadData = async () => {
  loading.value = true
  try {
    const [providerRes, configRes] = await Promise.all([listAiModelProviders(), getAiModelConfig()])
    if (providerRes.code === 0 && providerRes.data) {
      providers.value = providerRes.data
    }
    if (configRes.code === 0 && configRes.data) {
      applyConfig(configRes.data)
    }
  } finally {
    loading.value = false
  }
}

const applyConfig = (config: AiModelConfigVO) => {
  form.providerId = config.providerId || 'deepseek'
  form.baseUrl = config.baseUrl
  form.apiKey = config.apiKey || ''
  form.apiKeyConfigured = !!config.apiKeyConfigured
  scenarios.value = (config.scenarios ?? []).map((item) => ({ ...item }))
  normalizeScenarioModels()
}

// 将不属于当前提供商的模型名重置为默认模型，避免运行/测试时报错
const normalizeScenarioModels = () => {
  const provider = currentProvider.value
  if (!provider || !provider.models.length) {
    return
  }
  const validIds = provider.models.map((item) => item.id)
  const defaultModel = provider.models[0]?.id
  if (!defaultModel) {
    return
  }
  scenarios.value.forEach((item) => {
    if (!item.modelName || !validIds.includes(item.modelName)) {
      item.modelName = defaultModel
    }
  })
}

const handleProviderChange = (providerId: string) => {
  const provider = providers.value.find((item) => item.id === providerId)
  if (provider) {
    form.baseUrl = provider.defaultBaseUrl
  }
  normalizeScenarioModels()
}

const handleSave = async () => {
  saving.value = true
  try {
    const res = await saveAiModelConfig({
      providerId: form.providerId,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      scenarios: scenarios.value.map((item) => ({
        scenarioKey: item.scenarioKey,
        providerId: form.providerId,
        baseUrl: form.baseUrl,
        modelName: item.modelName,
        temperature: item.temperature,
        maxTokens: item.maxTokens,
        logRequests: item.logRequests,
        logResponses: item.logResponses,
      })),
    })
    if (res.code === 0) {
      message.success('模型配置已保存')
      await loadData()
    } else {
      message.error(res.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const handleTest = async () => {
  testing.value = true
  try {
    // 测试连接使用当前提供商的模型，避免误用其它场景遗留的旧模型名
    const testModel = currentProvider.value?.models?.[0]?.id
    const res = await testAiModelConnection({
      providerId: form.providerId,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      modelName: testModel,
    })
    if (res.code === 0) {
      message.success('连接成功')
    } else {
      message.error(res.message || '连接失败')
    }
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="ai-model-config">
    <div class="ai-model-config__header">
      <div>
        <h2 class="ai-model-config__title">模型接入</h2>
        <p class="ai-model-config__desc">
          配置 DeepSeek 等 OpenAI 兼容模型，保存后立即生效。参考
          <a href="https://api-docs.deepseek.com/zh-cn/" target="_blank" rel="noreferrer">DeepSeek API 文档</a>
        </p>
      </div>
      <a-space>
        <a-button :loading="testing" @click="handleTest">测试连接</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">保存配置</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <a-card title="提供商配置" class="ai-model-config__card">
        <a-form layout="vertical">
          <a-row :gutter="16">
            <a-col :xs="24" :md="8">
              <a-form-item label="模型提供商">
                <a-select
                  v-model:value="form.providerId"
                  :options="providers.map((item) => ({ label: item.name, value: item.id }))"
                  @change="handleProviderChange"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="10">
              <a-form-item label="Base URL（OpenAI 兼容）">
                <a-input v-model:value="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="API Key">
                <a-input-password
                  v-model:value="form.apiKey"
                  :placeholder="form.apiKeyConfigured ? '已配置，留空则不修改' : '请输入 API Key'"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-alert
            type="info"
            show-icon
            message="DeepSeek 推荐模型：deepseek-v4-flash（快速）、deepseek-v4-pro（更强）。deepseek-chat / deepseek-reasoner 将于 2026/07/24 弃用。"
          />
        </a-form>
      </a-card>

      <a-card title="分场景模型配置" class="ai-model-config__card">
        <a-table :columns="columns" :data-source="scenarios" :pagination="false" row-key="scenarioKey">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'modelName'">
              <a-select
                v-model:value="record.modelName"
                style="width: 100%"
                :options="modelOptions"
              />
            </template>
            <template v-else-if="column.key === 'temperature'">
              <a-input-number
                v-model:value="record.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                style="width: 100%"
              />
            </template>
            <template v-else-if="column.key === 'maxTokens'">
              <a-input-number
                v-model:value="record.maxTokens"
                :min="256"
                :max="65536"
                :step="256"
                style="width: 100%"
                placeholder="可选"
              />
            </template>
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.ai-model-config {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 16px 40px;
}

.ai-model-config__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.ai-model-config__title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.ai-model-config__desc {
  margin: 0;
  color: rgba(0, 0, 0, 0.55);
}

.ai-model-config__card {
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .ai-model-config__header {
    flex-direction: column;
  }
}
</style>

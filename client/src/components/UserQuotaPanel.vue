<script setup lang="ts">
import { computed } from 'vue'
import type { LoginUserVO, UserQuotaVO } from '@/api/types'
import { ACCESS_ENUM } from '@/stores/loginUser'

const props = defineProps<{
  user: LoginUserVO
  quota: UserQuotaVO | null
}>()

defineEmits<{
  logout: []
  refresh: []
}>()

const isAdmin = computed(() => props.user.userRole === ACCESS_ENUM.ADMIN)

const maskedAccount = computed(() => {
  const account = props.user.userAccount || ''
  if (!account) return props.user.userName || '用户'
  if (/^\d{7,}$/.test(account)) {
    return account.slice(0, 3) + '****' + account.slice(-4)
  }
  if (account.length <= 4) return account
  return account.slice(0, 2) + '****' + account.slice(-2)
})

const chatPercent = computed(() => {
  if (!props.quota) return 0
  if (props.quota.chatLimit <= 0) return 100
  return Math.min(100, Math.round((props.quota.chatUsed / props.quota.chatLimit) * 100))
})

const appPercent = computed(() => {
  if (!props.quota) return 0
  if (props.quota.appLimit <= 0) return 100
  return Math.min(100, Math.round((props.quota.appUsed / props.quota.appLimit) * 100))
})

const showDailyBonus = computed(
  () => props.quota && props.quota.dailyLoginBonus > 0 && props.quota.dailyBonusGranted,
)

const planTagColor = computed(() => {
  const plan = props.quota?.userPlan
  if (plan === 'pro') return 'gold'
  if (plan === 'basic') return 'blue'
  return 'default'
})
</script>

<template>
  <div class="quota-panel">
    <div class="quota-panel__header">
      <div class="quota-panel__info">
        <div class="quota-panel__account-row">
          <span class="quota-panel__account">{{ maskedAccount }}</span>
          <a-tag v-if="isAdmin" color="gold" class="quota-panel__plan">管理员</a-tag>
          <a-tag v-if="quota?.userPlanLabel" :color="planTagColor" class="quota-panel__plan">
            {{ quota.userPlanLabel }}
          </a-tag>
        </div>
        <div class="quota-panel__name">{{ user.userName }}</div>
      </div>
      <a-avatar :src="user.userAvatar" :size="40">
        {{ (user.userName || 'U').slice(0, 1) }}
      </a-avatar>
    </div>

    <div v-if="quota" class="quota-panel__stats">
      <div class="quota-row">
        <div class="quota-row__label">
          <span>对话次数</span>
          <span v-if="showDailyBonus" class="quota-row__bonus">每日访问 +{{ quota.dailyLoginBonus }}</span>
        </div>
        <a-progress :percent="chatPercent" :show-info="false" stroke-color="#595959" :stroke-width="6" />
        <div class="quota-row__value">
          {{ quota.chatUsed }} / {{ quota.chatLimit }}
          <span v-if="quota.extraChatQuota" class="quota-row__extra">（含扩容 +{{ quota.extraChatQuota }}）</span>
        </div>
      </div>
      <div class="quota-row">
        <div class="quota-row__label"><span>作品数量</span></div>
        <a-progress :percent="appPercent" :show-info="false" stroke-color="#595959" :stroke-width="6" />
        <div class="quota-row__value">
          {{ quota.appUsed }} / {{ quota.appLimit }}
          <span v-if="quota.extraAppQuota" class="quota-row__extra">（含扩容 +{{ quota.extraAppQuota }}）</span>
        </div>
      </div>
      <div class="quota-panel__hint">提示词优化计入对话次数</div>
    </div>

    <div v-if="quota" class="quota-panel__actions">
      <a-tooltip title="付费扩容功能暂未开发，未对接支付接口">
        <a-button size="small" block disabled>
          扩容对话 +{{ quota.chatExpandPack }}（¥{{ quota.chatExpandPriceYuan }}）
        </a-button>
      </a-tooltip>
      <a-tooltip title="付费扩容功能暂未开发，未对接支付接口">
        <a-button size="small" block disabled>
          扩容作品 +{{ quota.appExpandPack }}（¥{{ quota.appExpandPriceYuan }}）
        </a-button>
      </a-tooltip>
      <a-tooltip title="升级套餐功能暂未开发">
        <a-button size="small" block disabled>升级套餐（暂未开发）</a-button>
      </a-tooltip>
    </div>

    <a-divider style="margin: 12px 0" />
    <a-button type="text" danger block class="quota-panel__logout" @click="$emit('logout')">
      退出登录
    </a-button>
  </div>
</template>

<style scoped>
.quota-panel {
  width: 300px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgb(0 0 0 / 12%);
}

.quota-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.quota-panel__account-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.quota-panel__account {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.quota-panel__plan {
  margin: 0;
}

.quota-panel__name {
  margin-top: 4px;
  font-size: 13px;
  color: #8c8c8c;
}

.quota-row + .quota-row {
  margin-top: 14px;
}

.quota-row__label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  color: #595959;
}

.quota-row__bonus {
  font-size: 12px;
  color: #13a8a8;
}

.quota-row__value {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
  text-align: right;
}

.quota-row__extra {
  color: #13a8a8;
}

.quota-panel__hint {
  margin-top: 10px;
  font-size: 11px;
  color: #bfbfbf;
}

.quota-panel__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.quota-panel__logout {
  text-align: left;
}
</style>

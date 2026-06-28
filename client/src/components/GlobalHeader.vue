<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import logoUrl from '@/assets/logo.png'
import { ACCESS_ENUM, useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/user'

interface AppMenuItem {
  key: string
  label: string
  title?: string
  access?: string
}

const siteTitle = 'CODE原创项目'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 完整菜单配置（access 控制可见性）
const allMenus: AppMenuItem[] = [
  { key: '/', label: '首页', title: '首页' },
  { key: '/about', label: '关于', title: '关于' },
  { key: '/admin/user', label: '用户管理', title: '用户管理', access: ACCESS_ENUM.ADMIN },
  { key: '/admin/app', label: '应用管理', title: '应用管理', access: ACCESS_ENUM.ADMIN },
]

// 根据登录用户角色过滤可见菜单
const menuItems = computed<MenuProps['items']>(() => {
  const role = loginUserStore.loginUser.userRole
  return allMenus
    .filter((item) => {
      if (item.access === ACCESS_ENUM.ADMIN) {
        return role === ACCESS_ENUM.ADMIN
      }
      return true
    })
    .map((item) => ({ key: item.key, label: item.label, title: item.title }))
})

const selectedKeys = computed(() => [route.path])

const isLogin = computed(
  () => !!loginUserStore.loginUser.id && loginUserStore.loginUser.userRole !== ACCESS_ENUM.NOT_LOGIN,
)

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (typeof key === 'string' && key !== route.path) {
    router.push(key)
  }
}

const handleLogout = async () => {
  const res = await userLogout()
  if (res.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录', userRole: ACCESS_ENUM.NOT_LOGIN })
    message.success('已退出登录')
    router.push('/user/login')
  } else {
    message.error(res.message || '退出失败')
  }
}

const userMenuItems = [{ key: 'logout', label: '退出登录', icon: h(LogoutOutlined) }]

const handleUserMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key === 'logout') {
    handleLogout()
  }
}

const goLogin = () => {
  router.push('/user/login')
}
</script>

<template>
  <a-layout-header class="global-header">
    <div class="global-header__brand" @click="router.push('/')">
      <img class="global-header__logo" :src="logoUrl" alt="网站 Logo" />
      <span class="global-header__title">{{ siteTitle }}</span>
    </div>

    <a-menu
      class="global-header__menu"
      mode="horizontal"
      theme="dark"
      :items="menuItems"
      :selected-keys="selectedKeys"
      @click="handleMenuClick"
    />

    <div class="global-header__user">
      <a-dropdown v-if="isLogin">
        <span class="global-header__user-info">
          <a-avatar :src="loginUserStore.loginUser.userAvatar" size="small">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <span class="global-header__user-name">{{ loginUserStore.loginUser.userName }}</span>
        </span>
        <template #overlay>
          <a-menu :items="userMenuItems" @click="handleUserMenuClick" />
        </template>
      </a-dropdown>
      <a-button v-else type="primary" @click="goLogin">登录</a-button>
    </div>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  gap: 24px;
}

.global-header__brand {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  min-width: 0;
  gap: 10px;
  color: #fff;
  cursor: pointer;
}

.global-header__logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.global-header__title {
  overflow: hidden;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.global-header__menu {
  flex: 1 1 auto;
  min-width: 0;
}

.global-header__user {
  flex: 0 0 auto;
}

.global-header__user-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
}

.global-header__user-name {
  font-size: 14px;
}

@media (max-width: 768px) {
  .global-header {
    height: auto;
    min-height: 64px;
    flex-wrap: wrap;
    padding: 12px 16px;
    gap: 12px;
  }

  .global-header__brand {
    flex: 1 1 auto;
  }

  .global-header__menu {
    order: 3;
    flex-basis: 100%;
  }
}
</style>

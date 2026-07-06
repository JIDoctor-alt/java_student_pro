import { createRouter, createWebHistory } from 'vue-router'
import { message } from 'ant-design-vue'
import HomeView from '../views/HomeView.vue'
import { ACCESS_ENUM, useLoginUserStore } from '@/stores/loginUser'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: '主页' },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
      meta: { title: '关于' },
    },
    {
      path: '/app/chat/:id',
      name: 'appChat',
      component: () => import('../views/app/AppChatView.vue'),
      meta: { title: '应用对话', hideInMenu: true, access: ACCESS_ENUM.USER },
    },
    {
      path: '/user/login',
      name: 'userLogin',
      component: () => import('../views/user/UserLoginView.vue'),
      meta: { title: '登录', hideInMenu: true },
    },
    {
      path: '/user/register',
      name: 'userRegister',
      component: () => import('../views/user/UserRegisterView.vue'),
      meta: { title: '注册', hideInMenu: true },
    },
    {
      path: '/admin/user',
      name: 'adminUser',
      component: () => import('../views/user/UserManageView.vue'),
      // 仅管理员可访问
      meta: { title: '用户管理', access: ACCESS_ENUM.ADMIN },
    },
    {
      path: '/admin/app',
      name: 'adminApp',
      component: () => import('../views/app/AppManageView.vue'),
      // 仅管理员可访问
      meta: { title: '应用管理', access: ACCESS_ENUM.ADMIN },
    },
    {
      path: '/admin/ai-model',
      name: 'adminAiModel',
      component: () => import('../views/admin/AiModelConfigView.vue'),
      meta: { title: '模型接入', access: ACCESS_ENUM.ADMIN },
    },
  ],
})

// 全局权限校验
router.beforeEach(async (to) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 首次进入或刷新后尚未拉取用户信息，则远程获取一次
  if (!loginUser || !loginUser.userRole) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
  }

  const needAccess = (to.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN
  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    return true
  }

  const isLogin = !!loginUser?.id && loginUser.userRole !== ACCESS_ENUM.NOT_LOGIN
  // 需要登录但未登录
  if (!isLogin) {
    message.warning('请先登录')
    return `/user/login?redirect=${to.fullPath}`
  }
  // 需要管理员但不是管理员
  if (needAccess === ACCESS_ENUM.ADMIN && loginUser.userRole !== ACCESS_ENUM.ADMIN) {
    message.error('没有访问权限')
    return '/'
  }
  return true
})

export default router

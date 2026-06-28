import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser } from '@/api/user'
import type { LoginUserVO } from '@/api/types'

/**
 * 登录用户角色常量
 */
export const ACCESS_ENUM = {
  NOT_LOGIN: 'notLogin',
  USER: 'user',
  ADMIN: 'admin',
} as const

/**
 * 登录用户全局状态
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<LoginUserVO>({
    userName: '未登录',
    userRole: ACCESS_ENUM.NOT_LOGIN,
  })

  /**
   * 远程拉取当前登录用户
   */
  async function fetchLoginUser() {
    const res = await getCurrentUser()
    if (res.code === 0 && res.data) {
      loginUser.value = res.data
    } else {
      loginUser.value = {
        userName: '未登录',
        userRole: ACCESS_ENUM.NOT_LOGIN,
      }
    }
  }

  /**
   * 设置登录用户（登录/注销后同步）
   */
  function setLoginUser(user: LoginUserVO) {
    loginUser.value = user
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})

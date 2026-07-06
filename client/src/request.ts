import axios from 'axios'
import { message } from 'ant-design-vue'

/**
 * 全局 axios 实例
 * - baseURL 指向后端（context-path 为 /api）
 * - withCredentials 携带 Cookie，配合后端 Session 登录态
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  withCredentials: true,
})

// 响应拦截器：统一处理未登录等情况
request.interceptors.response.use(
  (response) => {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      // 不是获取登录信息接口，且不在登录页时，跳转登录
      const requestUrl = response.config.url ?? ''
      if (
        !requestUrl.includes('user/current') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
  (error) => {
    const serverMessage = error.response?.data?.message
    message.error(serverMessage || error.message || '请求失败')
    return Promise.reject(error)
  },
)

export default request

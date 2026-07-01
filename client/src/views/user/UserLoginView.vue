<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin } from '@/api/user'
import { useLoginUserStore } from '@/stores/loginUser'
import type { UserLoginRequest } from '@/api/types'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const form = reactive<UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const loading = ref(false)

const handleSubmit = async () => {
  loading.value = true
  try {
    const res = await userLogin(form)
    if (res.code === 0 && res.data) {
      loginUserStore.setLoginUser(res.data)
      message.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect.startsWith('http') ? '/' : redirect)
    } else {
      message.error(res.message || '登录失败')
    }
  } catch {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="user-login">
    <h2 class="user-login__title">用户登录</h2>
    <a-form :model="form" layout="vertical" @finish="handleSubmit">
      <a-form-item
        label="账号"
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号' }]"
      >
        <a-input v-model:value="form.userAccount" placeholder="请输入账号" allow-clear />
      </a-form-item>
      <a-form-item
        label="密码"
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不少于 8 位' },
        ]"
      >
        <a-input-password v-model:value="form.userPassword" placeholder="请输入密码" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">登录</a-button>
      </a-form-item>
      <div class="user-login__tips">
        没有账号？
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>
    </a-form>
  </div>
</template>

<style scoped>
.user-login {
  max-width: 420px;
  margin: 40px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.user-login__title {
  margin-bottom: 24px;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
}

.user-login__tips {
  text-align: right;
  font-size: 13px;
}
</style>

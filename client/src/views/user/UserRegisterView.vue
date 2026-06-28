<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userRegister } from '@/api/user'
import type { UserRegisterRequest } from '@/api/types'

const router = useRouter()

const form = reactive<UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const loading = ref(false)

const handleSubmit = async () => {
  if (form.userPassword !== form.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await userRegister(form)
    if (res.code === 0) {
      message.success('注册成功，请登录')
      router.push('/user/login')
    } else {
      message.error(res.message || '注册失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="user-register">
    <h2 class="user-register__title">用户注册</h2>
    <a-form :model="form" layout="vertical" @finish="handleSubmit">
      <a-form-item
        label="账号"
        name="userAccount"
        :rules="[
          { required: true, message: '请输入账号' },
          { min: 4, message: '账号不少于 4 位' },
        ]"
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
      <a-form-item
        label="确认密码"
        name="checkPassword"
        :rules="[
          { required: true, message: '请再次输入密码' },
          { min: 8, message: '密码不少于 8 位' },
        ]"
      >
        <a-input-password
          v-model:value="form.checkPassword"
          placeholder="请再次输入密码"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">注册</a-button>
      </a-form-item>
      <div class="user-register__tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
    </a-form>
  </div>
</template>

<style scoped>
.user-register {
  max-width: 420px;
  margin: 40px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.user-register__title {
  margin-bottom: 24px;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
}

.user-register__tips {
  text-align: right;
  font-size: 13px;
}
</style>

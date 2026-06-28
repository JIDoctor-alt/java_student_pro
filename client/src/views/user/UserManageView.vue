<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { deleteUser, listUserVoByPage } from '@/api/user'
import type { UserQueryRequest, UserVO } from '@/api/types'

const columns = [
  { title: 'id', dataIndex: 'id', width: 80 },
  { title: '账号', dataIndex: 'userAccount' },
  { title: '昵称', dataIndex: 'userName' },
  { title: '头像', dataIndex: 'userAvatar', key: 'userAvatar', width: 80 },
  { title: '简介', dataIndex: 'userProfile' },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 120 },
]

const dataList = ref<UserVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<UserQueryRequest>({
  current: 1,
  pageSize: 10,
  userAccount: '',
  userName: '',
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      ...searchParams,
    })
    if (res.code === 0 && res.data) {
      dataList.value = res.data.records ?? []
      total.value = res.data.totalRow ?? 0
    } else {
      message.error(res.message || '获取数据失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchParams.current = 1
  fetchData()
}

const handleReset = () => {
  searchParams.userAccount = ''
  searchParams.userName = ''
  searchParams.current = 1
  fetchData()
}

const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
  searchParams.current = pagination.current ?? 1
  searchParams.pageSize = pagination.pageSize ?? 10
  fetchData()
}

const handleDelete = (record: UserVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除用户「${record.userAccount}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteUser(record.id)
      if (res.code === 0 && res.data) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error(res.message || '删除失败')
      }
    },
  })
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="user-manage">
    <h2 class="user-manage__title">用户管理</h2>

    <a-form layout="inline" class="user-manage__search">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="请输入账号" allow-clear />
      </a-form-item>
      <a-form-item label="昵称">
        <a-input v-model:value="searchParams.userName" placeholder="请输入昵称" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: searchParams.current,
        pageSize: searchParams.pageSize,
        total: total,
        showTotal: (t: number) => `共 ${t} 条`,
      }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userAvatar'">
          <a-avatar :src="record.userAvatar" />
        </template>
        <template v-else-if="column.key === 'userRole'">
          <a-tag :color="record.userRole === 'admin' ? 'green' : 'blue'">
            {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
.user-manage {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.user-manage__title {
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
}

.user-manage__search {
  margin-bottom: 20px;
  row-gap: 12px;
}
</style>

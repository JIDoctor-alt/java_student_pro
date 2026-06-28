<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  deleteAppByAdmin,
  listAppVoByPageByAdmin,
  updateAppByAdmin,
} from '@/api/app'
import type { AppQueryRequest, AppVO } from '@/api/types'

const GOOD_PRIORITY = 99

const columns = [
  { title: 'id', dataIndex: 'id', width: 80 },
  { title: '封面', dataIndex: 'cover', key: 'cover', width: 100 },
  { title: '应用名称', dataIndex: 'appName' },
  { title: '生成类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 110 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '创建者', dataIndex: 'user', key: 'user', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 220 },
]

const dataList = ref<AppVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<AppQueryRequest>({
  current: 1,
  pageSize: 10,
  appName: '',
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({ ...searchParams })
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
  searchParams.appName = ''
  searchParams.current = 1
  fetchData()
}

const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
  searchParams.current = pagination.current ?? 1
  searchParams.pageSize = pagination.pageSize ?? 10
  fetchData()
}

const handleDelete = (record: AppVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除应用「${record.appName}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteAppByAdmin({ id: record.id })
      if (res.code === 0 && res.data) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error(res.message || '删除失败')
      }
    },
  })
}

const toggleFeatured = async (record: AppVO) => {
  const isGood = record.priority === GOOD_PRIORITY
  const res = await updateAppByAdmin({
    id: record.id,
    priority: isGood ? 0 : GOOD_PRIORITY,
  })
  if (res.code === 0 && res.data) {
    message.success(isGood ? '已取消精选' : '已设为精选')
    fetchData()
  } else {
    message.error(res.message || '操作失败')
  }
}

// 编辑弹窗
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive<{ id?: number; appName: string; cover: string; priority: number }>({
  appName: '',
  cover: '',
  priority: 0,
})

const openEdit = (record: AppVO) => {
  editForm.id = record.id
  editForm.appName = record.appName ?? ''
  editForm.cover = record.cover ?? ''
  editForm.priority = record.priority ?? 0
  editVisible.value = true
}

const handleEditOk = async () => {
  if (!editForm.id) return
  editSaving.value = true
  try {
    const res = await updateAppByAdmin({
      id: editForm.id,
      appName: editForm.appName,
      cover: editForm.cover,
      priority: editForm.priority,
    })
    if (res.code === 0 && res.data) {
      message.success('保存成功')
      editVisible.value = false
      fetchData()
    } else {
      message.error(res.message || '保存失败')
    }
  } finally {
    editSaving.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="app-manage">
    <h2 class="app-manage__title">应用管理</h2>

    <a-form layout="inline" class="app-manage__search">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="请输入应用名称" allow-clear />
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
      :scroll="{ x: 1100 }"
      :pagination="{
        current: searchParams.current,
        pageSize: searchParams.pageSize,
        total: total,
        showTotal: (t: number) => `共 ${t} 条`,
      }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="72" />
          <span v-else class="app-manage__nocover">无</span>
        </template>
        <template v-else-if="column.key === 'codeGenType'">
          <a-tag>{{ record.codeGenType === 'multi_file' ? '多文件' : 'HTML' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'priority'">
          <a-tag v-if="record.priority === GOOD_PRIORITY" color="gold">精选</a-tag>
          <span v-else>{{ record.priority }}</span>
        </template>
        <template v-else-if="column.key === 'user'">
          {{ record.user?.userName || record.userId }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="toggleFeatured(record)">
              {{ record.priority === GOOD_PRIORITY ? '取消精选' : '设为精选' }}
            </a-button>
            <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="editVisible"
      title="编辑应用"
      :confirm-loading="editSaving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleEditOk"
    >
      <a-form layout="vertical">
        <a-form-item label="应用名称">
          <a-input v-model:value="editForm.appName" placeholder="请输入应用名称" />
        </a-form-item>
        <a-form-item label="应用封面（图片 URL）">
          <a-input v-model:value="editForm.cover" placeholder="请输入封面图片地址" />
        </a-form-item>
        <a-form-item label="优先级（99 为精选）">
          <a-input-number v-model:value="editForm.priority" :min="0" :max="99" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.app-manage {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.app-manage__title {
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
}

.app-manage__search {
  margin-bottom: 20px;
  row-gap: 12px;
}

.app-manage__nocover {
  color: rgba(0, 0, 0, 0.35);
}
</style>

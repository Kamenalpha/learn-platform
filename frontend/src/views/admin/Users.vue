<template>
  <div class="page-card">
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="userId" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="nickname" label="昵称" width="180" />
      <el-table-column label="角色" width="140">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : 'primary'" size="small">
            {{ row.role === 1 ? '管理员' : '学生' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.role === 0" size="small" type="warning" plain @click="setRole(row, 1)">
            设为管理员
          </el-button>
          <el-button v-else size="small" plain @click="setRole(row, 0)">设为学生</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const users = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    users.value = await api.users()
  } finally {
    loading.value = false
  }
}

const setRole = async (row, role) => {
  await api.setUserRole(row.userId, role)
  ElMessage.success('已更新角色')
  load()
}

const remove = (row) => {
  ElMessageBox.confirm(`确定删除用户「${row.username}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await api.deleteUser(row.userId)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

onMounted(load)
</script>

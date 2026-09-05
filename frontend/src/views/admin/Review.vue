<template>
  <div class="page-card">
    <div class="audit-head">
      <span class="audit-title">内容审核</span>
      <span class="audit-tip">用户申请公开的课程与教材经审核通过后,游客才可在「公开资源」中看到;驳回则不公开。</span>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane :label="`公开课程申请 (${courses.length})`" name="course">
        <el-table :data="courses" v-loading="loading" stripe>
          <el-table-column prop="courseName" label="课程名称" min-width="180" />
          <el-table-column prop="description" label="简介" min-width="220" show-overflow-tooltip />
          <el-table-column prop="subjectName" label="学科" width="110" />
          <el-table-column prop="ownerName" label="申请人" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.auditStatus)" size="small">{{ statusText(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="success" plain @click="decide('course', row, 'approve')">通过</el-button>
              <el-button size="small" type="danger" plain @click="decide('course', row, 'reject')">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`公开教材申请 (${resources.length})`" name="resource">
        <el-table :data="resources" v-loading="loading" stripe>
          <el-table-column prop="title" label="教材标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="courseName" label="所属课程" min-width="160" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ (row.fileType || '').toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" label="上传人" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.auditStatus)" size="small">{{ statusText(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="success" plain @click="decide('resource', row, 'approve')">通过</el-button>
              <el-button size="small" type="danger" plain @click="decide('resource', row, 'reject')">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const tab = ref('course')
const courses = ref([])
const resources = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const data = await api.auditPending()
    courses.value = data?.courses || []
    resources.value = data?.resources || []
  } finally {
    loading.value = false
  }
}

const decide = (type, row, action) => {
  const name = type === 'course' ? row.courseName : row.title
  const verb = action === 'approve' ? '通过' : '驳回'
  ElMessageBox.confirm(`确定${verb}「${name}」的公开申请?`, '内容审核', { type: action === 'approve' ? 'info' : 'warning' })
    .then(async () => {
      if (type === 'course') {
        await api.auditCourse(row.courseId, action)
      } else {
        await api.auditResource(row.resourceId, action)
      }
      ElMessage.success(`已${verb}`)
      load()
    })
    .catch(() => {})
}

const statusText = (s) => ({ 0: '待审核', 2: '已驳回' }[s] || '未知')
const statusTag = (s) => ({ 0: 'warning', 2: 'danger' }[s] || 'info')
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

onMounted(load)
</script>

<style scoped>
.audit-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 6px; }
.audit-title { font-weight: 700; font-size: 16px; }
.audit-tip { color: var(--ink-3, #909399); font-size: 12.5px; }
</style>

<template>
  <div class="page-card">
    <div class="head">
      <span class="title">课程设计项目辅导</span>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建项目</el-button>
    </div>

    <el-row :gutter="12" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="p in projects" :key="p.projectId">
        <div class="proj-card" @click="openDetail(p)">
          <div class="proj-title">{{ p.projectTitle }}</div>
          <div class="proj-req">{{ p.requirement || '未填写需求' }}</div>
          <div class="proj-time">{{ formatTime(p.createTime) }}</div>
          <el-button size="small" type="danger" text @click.stop="removeProject(p)">删除</el-button>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && projects.length === 0" description="暂无项目,点击右上角新建" />

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建项目辅导" width="560px">
      <el-form label-width="90px">
        <el-form-item label="项目题目"><el-input v-model="form.projectTitle" placeholder="如:校园二手交易平台" /></el-form-item>
        <el-form-item label="需求">
          <el-input v-model="form.requirement" type="textarea" :rows="5" placeholder="描述项目需求(越详细,AI辅导越准)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="create">AI 生成辅导方案</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="current?.projectTitle || ''" size="60%">
      <div v-if="current" class="proj-detail">
        <div class="sub-title">技术方案</div>
        <div class="pre-wrap">{{ current.techSolution }}</div>
        <div class="sub-title">任务清单</div>
        <ul><li v-for="(t, i) in parseList(current.taskList)" :key="i">{{ t }}</li></ul>
        <div class="sub-title">阶段计划</div>
        <ul><li v-for="(t, i) in parseList(current.stagePlan)" :key="i">{{ t }}</li></ul>
        <div class="sub-title">报告/文档大纲</div>
        <div class="pre-wrap">{{ current.docContent }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../api'

const projects = ref([])
const loading = ref(false)
const createVisible = ref(false)
const form = ref({ projectTitle: '', requirement: '' })
const creating = ref(false)
const detailVisible = ref(false)
const current = ref(null)

const load = async () => {
  loading.value = true
  try { projects.value = await api.listProjects() } finally { loading.value = false }
}

const openCreate = () => {
  form.value = { projectTitle: '', requirement: '' }
  createVisible.value = true
}

const create = async () => {
  if (!form.value.projectTitle.trim()) {
    ElMessage.warning('请填写项目题目')
    return
  }
  creating.value = true
  try {
    await api.createProject(form.value)
    ElMessage.success('项目辅导方案已生成')
    createVisible.value = false
    await load()
  } finally { creating.value = false }
}

const openDetail = async (p) => {
  current.value = await api.projectDetail(p.projectId)
  detailVisible.value = true
}

const removeProject = (p) => {
  ElMessageBox.confirm(`删除项目「${p.projectTitle}」?`, '提示', { type: 'warning' })
    .then(async () => { await api.deleteProject(p.projectId); ElMessage.success('已删除'); await load() })
    .catch(() => {})
}

const parseList = (s) => {
  if (!s) return []
  try {
    const arr = JSON.parse(s)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.proj-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  cursor: pointer;
}

.proj-card:hover {
  border-color: var(--amber);
  box-shadow: 0 2px 12px rgba(0, 0, 0, .06);
}

.proj-title {
  font-weight: 600;
}

.proj-req {
  color: var(--mist);
  font-size: 13px;
  margin: 8px 0;
  max-height: 40px;
  overflow: hidden;
}

.proj-time {
  color: var(--mist);
  font-size: 12px;
}

.proj-detail .sub-title {
  font-weight: 600;
  margin: 14px 0 6px;
}
</style>

<template>
  <div class="page-card">
    <div class="head">
      <span class="title">学习计划</span>
      <el-button type="primary" :icon="Plus" @click="openPlan()">新建计划</el-button>
    </div>

    <el-row :gutter="12" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="p in plans" :key="p.planId">
        <div class="plan-card" @click="openPlanDetail(p)">
          <div class="plan-title">
            {{ p.title }}
            <el-tag size="small" :type="planStatusTag(p.status)" style="margin-left:8px">{{ planStatusText(p.status) }}</el-tag>
          </div>
          <div class="plan-goal">{{ p.goal }}</div>
          <div class="plan-date">{{ p.startDate || '—' }} ~ {{ p.endDate || '—' }}</div>
          <div class="plan-actions">
            <el-button size="small" type="danger" plain @click.stop="removePlan(p)">删除</el-button>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && plans.length === 0" description="暂无学习计划,点击右上角新建" />

    <!-- 新建计划 -->
    <el-dialog v-model="planVisible" title="新建学习计划" width="480px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="planForm.title" placeholder="如:高数期末 80+" /></el-form-item>
        <el-form-item label="目标"><el-input v-model="planForm.goal" placeholder="学习目标(可选)" /></el-form-item>
        <el-form-item label="周期">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始" end-placeholder="截止" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- 计划详情:任务 -->
    <el-drawer v-model="detailVisible" :title="currentPlan?.title || ''" size="55%">
      <div class="detail-toolbar">
        <el-input v-model="newTask.title" placeholder="任务标题(可关联课程/章节/知识点)" style="width: calc(100% - 130px)">
          <template #prepend>任务</template>
        </el-input>
        <el-button type="primary" @click="addTask">添加</el-button>
      </div>
      <div v-for="t in tasks" :key="t.taskId" class="task-item" :class="{ done: t.done === 1 }">
        <el-checkbox :model-value="t.done === 1" @change="toggleTask(t)">{{ t.title }}</el-checkbox>
        <span class="task-date">{{ t.planDate || '未排期' }}</span>
        <el-button size="small" type="danger" text @click="removeTask(t)">删除</el-button>
      </div>
      <el-empty v-if="tasks.length === 0" description="暂无任务" />
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../api'

const plans = ref([])
const loading = ref(false)

const currentPlan = ref(null)

const planVisible = ref(false)
const planForm = ref({ title: '', goal: '' })
const range = ref([])

const detailVisible = ref(false)
const tasks = ref([])
const newTask = ref({ title: '' })

const load = async () => {
  loading.value = true
  try {
    plans.value = await api.listPlans()
  } finally {
    loading.value = false
  }
}

const openPlan = () => {
  planForm.value = { title: '', goal: '' }
  range.value = []
  planVisible.value = true
}

const savePlan = async () => {
  if (!planForm.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  const d = {
    title: planForm.value.title.trim(),
    goal: planForm.value.goal,
    startDate: range.value?.[0] || null,
    endDate: range.value?.[1] || null
  }
  await api.addPlan(d)
  ElMessage.success('已创建')
  planVisible.value = false
  await load()
}

const openPlanDetail = async (p) => {
  currentPlan.value = p
  detailVisible.value = true
  const detail = await api.planDetail(p.planId)
  tasks.value = detail.tasks || []
}

const addTask = async () => {
  if (!newTask.value.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  await api.addPlanTask(currentPlan.value.planId, { title: newTask.value.title.trim() })
  newTask.value.title = ''
  const detail = await api.planDetail(currentPlan.value.planId)
  tasks.value = detail.tasks || []
}

const toggleTask = async (t) => {
  const updated = await api.togglePlanTask(t.taskId)
  t.done = updated.done
}

const removeTask = async (t) => {
  await api.deletePlanTask(t.taskId)
  const detail = await api.planDetail(currentPlan.value.planId)
  tasks.value = detail.tasks || []
}

const removePlan = (p) => {
  ElMessageBox.confirm(`删除计划「${p.title}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await api.deletePlan(p.planId)
      ElMessage.success('已删除')
      await load()
    }).catch(() => {})
}

const planStatusTag = (s) => ({ 0: 'primary', 1: 'success', 2: 'info' }[s] || 'info')
const planStatusText = (s) => ({ 0: '进行中', 1: '已完成', 2: '已取消' }[s] || '未知')

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

.plan-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  cursor: pointer;
}

.plan-card:hover {
  border-color: var(--amber);
  box-shadow: 0 2px 12px rgba(28, 37, 52, .07);
}

.plan-title {
  font-weight: 600;
  display: flex;
  align-items: center;
}

.plan-goal {
  color: var(--mist);
  font-size: 13px;
  margin-top: 8px;
}

.plan-date {
  color: var(--mist);
  font-size: 12px;
  margin-top: 6px;
}

.plan-actions {
  margin-top: 10px;
  text-align: right;
}

.detail-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--line);
}

.task-item.done {
  color: var(--mist);
  text-decoration: line-through;
}

.task-date {
  color: var(--mist);
  font-size: 12px;
  flex: 1;
  text-align: right;
}
</style>

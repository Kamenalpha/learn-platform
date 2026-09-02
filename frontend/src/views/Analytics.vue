<template>
  <div class="page-card">
    <div class="head">
      <span class="title">学习画像</span>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-row :gutter="12" v-loading="loading">
      <el-col :xs="12" :sm="8" :md="6" v-for="c in cards" :key="c.label">
        <div class="stat-card">
          <div class="stat-value">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="block">
      <div class="block-title">弱项知识点(按错题统计)</div>
      <el-table :data="overview?.weakPoints || []" size="small">
        <el-table-column prop="name" label="知识点" min-width="200" />
        <el-table-column label="错题次数" width="120">
          <template #default="{ row }">{{ row.cnt }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!overview?.weakPoints?.length" description="暂无错题数据,先去出题模拟中练习" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const overview = ref(null)
const loading = ref(false)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '累计学习', value: fmtDur(o.totalSeconds) },
    { label: '今日学习', value: fmtDur(o.todaySeconds) },
    { label: '连续打卡', value: (o.checkinStreak || 0) + ' 天' },
    { label: '提问次数', value: o.chatCount || 0 },
    { label: '练习次数', value: o.examCount || 0 },
    { label: '错题本', value: o.mistakeCount || 0 },
    { label: '学习计划', value: o.planCount || 0 },
    { label: '资料数', value: o.resourceCount || 0 }
  ]
})

const fmtDur = (sec) => {
  if (!sec) return '0 分钟'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h} 小时 ${m} 分` : `${m} 分钟`
}

const load = async () => {
  loading.value = true
  try {
    overview.value = await api.analyticsOverview()
  } finally {
    loading.value = false
  }
}

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

.stat-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--amber);
}

.stat-label {
  color: var(--mist);
  font-size: 12px;
  margin-top: 4px;
}

.block {
  margin-top: 12px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 8px;
}
</style>

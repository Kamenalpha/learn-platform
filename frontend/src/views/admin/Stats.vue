<template>
  <div>
    <!-- 概览卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>近7天每日提问量</template>
          <div ref="trendRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>各课程文档数量</template>
          <div ref="barRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>问答日志</template>
      <el-table :data="logs" v-loading="logLoading" stripe size="small">
        <el-table-column prop="recordId" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column label="问题" min-width="240">
          <template #default="{ row }">{{ row.question }}</template>
        </el-table-column>
        <el-table-column label="回答摘要" min-width="240">
          <template #default="{ row }">{{ row.answer }}</template>
        </el-table-column>
        <el-table-column prop="elapsedMs" label="耗时(ms)" width="100" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 12px; justify-content: flex-end"
        layout="total, prev, pager, next"
        :total="logTotal"
        :page-size="pageSize"
        v-model:current-page="page"
        @current-change="loadLogs"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { api } from '../../api'

const cards = ref([])
const trendRef = ref(null)
const barRef = ref(null)
const logs = ref([])
const logTotal = ref(0)
const page = ref(1)
const pageSize = 10
const logLoading = ref(false)
let trendChart = null
let barChart = null

const loadOverview = async () => {
  const data = await api.statsOverview()
  cards.value = [
    { label: '用户数', value: data.userCount },
    { label: '文档总数', value: `${data.docCount}(已解析 ${data.parsedCount})` },
    { label: '知识分块数', value: data.chunkCount },
    { label: '累计提问数', value: data.qaCount }
  ]
}

const loadTrend = async () => {
  const data = await api.qaTrend(7)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d) => String(d.date).slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', smooth: true, data: data.map((d) => d.count), areaStyle: { opacity: 0.15 } }]
  })
}

const loadBar = async () => {
  const data = await api.docsByCourse()
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d) => d.name), axisLabel: { interval: 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', barMaxWidth: 40, data: data.map((d) => d.value) }]
  })
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const data = await api.qaLogs(page.value, pageSize)
    logs.value = data.records
    logTotal.value = Number(data.total)
  } finally {
    logLoading.value = false
  }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

const onResize = () => {
  trendChart?.resize()
  barChart?.resize()
}

onMounted(async () => {
  trendChart = echarts.init(trendRef.value)
  barChart = echarts.init(barRef.value)
  window.addEventListener('resize', onResize)
  await loadOverview()
  loadTrend()
  loadBar()
  loadLogs()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.chart-box {
  height: 280px;
}
</style>

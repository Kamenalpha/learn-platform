<template>
  <div class="page-card">
    <el-form :inline="true">
      <el-form-item label="课程">
        <el-select v-model="courseId" placeholder="选择课程" style="width: 220px">
          <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="renderGraph">生成知识图谱</el-button>
        <el-button :loading="refreshing" @click="renderGraph(true)">强制重建</el-button>
      </el-form-item>
    </el-form>
    <el-alert
      style="margin-bottom: 10px"
      title="图谱由 AI 从课程知识块中抽取知识点关键词构建:节点越大表示出现频次越高,连线表示两个知识点常在同一上下文中共现"
      type="info"
      :closable="false"
    />
    <div ref="chartRef" class="graph-box" v-loading="loading" />
    <el-empty v-if="!hasData && !loading" description="选择课程后生成知识图谱" :image-size="80" />
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { api } from '../api'

const courses = ref([])
const courseId = ref(null)
const loading = ref(false)
const refreshing = ref(false)
const hasData = ref(false)
const chartRef = ref(null)
let chart = null

const renderGraph = async (refresh = false) => {
  if (!courseId.value) {
    ElMessage.warning('请选择课程')
    return
  }
  loading.value = true
  refreshing.value = refresh
  try {
    const data = await api.graph(courseId.value, refresh)
    if (!data.nodes?.length) {
      ElMessage.warning('未抽取到知识点')
      return
    }
    hasData.value = true
    const option = {
      tooltip: {},
      series: [
        {
          type: 'graph',
          layout: 'force',
          roam: true,
          label: { show: true, fontSize: 12 },
          edgeSymbol: ['none', 'none'],
          edgeLabel: { show: false },
          force: { repulsion: 260, edgeLength: 90 },
          lineStyle: { color: '#cfccbd', width: 1, curveness: 0.1 },
          itemStyle: { color: '#2f4fd0' },
          emphasis: { focus: 'adjacency', itemStyle: { color: '#2440ad' } },
          data: data.nodes.map((n) => ({
            name: n.name,
            // 节点大小随频次变化
            symbolSize: Math.min(18 + n.value * 4, 52),
            value: n.value
          })),
          links: data.edges.map((e) => ({
            source: e.source,
            target: e.target,
            lineStyle: { width: Math.min(e.weight, 5) }
          }))
        }
      ]
    }
    if (!chart) {
      chart = echarts.init(chartRef.value)
    }
    chart.setOption(option, true)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onResize = () => chart && chart.resize()

onMounted(async () => {
  courses.value = await api.listCourses()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chart) chart.dispose()
})
</script>

<style scoped>
.graph-box {
  height: calc(100vh - 300px);
  min-height: 420px;
}
</style>

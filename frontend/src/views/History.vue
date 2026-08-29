<template>
  <div class="page-card">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="学习历史" name="history" />
      <el-tab-pane label="我的收藏" name="favorites" />
    </el-tabs>

    <el-table :data="records" v-loading="loading" stripe>
      <el-table-column label="问题" min-width="240">
        <template #default="{ row }">
          <span class="q-cell">{{ row.question }}</span>
        </template>
      </el-table-column>
      <el-table-column label="回答摘要" min-width="240">
        <template #default="{ row }">
          <span class="q-cell">{{ (row.answer || '').slice(0, 60) }}...</span>
        </template>
      </el-table-column>
      <el-table-column label="提问时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="view(row)">查看</el-button>
          <el-button size="small" :type="row.isFavorite ? 'warning' : 'default'" @click="toggleFav(row)">
            {{ row.isFavorite ? '已收藏' : '收藏' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="viewVisible" :title="current?.question" width="640px" top="6vh">
      <template v-if="current">
        <div class="pre-wrap" style="margin-bottom: 12px">{{ current.answer }}</div>
        <el-divider content-position="left">引用来源</el-divider>
        <div v-for="(r, i) in parseRef(current.reference)" :key="i" class="source-card">
          [{{ i + 1 }}] {{ r.docTitle }}{{ r.page ? ' · 第' + r.page + '页' : '' }}
          <div class="pre-wrap" style="color: #909399; margin-top: 4px">{{ r.snippet }}</div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const tab = ref('history')
const records = ref([])
const loading = ref(false)
const viewVisible = ref(false)
const current = ref(null)

const load = async () => {
  loading.value = true
  try {
    records.value = tab.value === 'history' ? await api.records() : await api.favorites()
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}

const view = (row) => {
  current.value = row
  viewVisible.value = true
}

const toggleFav = async (row) => {
  const favorited = await api.toggleFavorite(row.recordId)
  row.isFavorite = favorited ? 1 : 0
  ElMessage.success(favorited ? '已收藏' : '已取消收藏')
  if (tab.value === 'favorites') load()
}

const parseRef = (json) => {
  try {
    return JSON.parse(json || '[]')
  } catch (e) {
    return []
  }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

onMounted(load)
</script>

<style scoped>
.q-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>

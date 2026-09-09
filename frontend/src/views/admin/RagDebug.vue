<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>检索测试</span>
        <span class="tip">与真实问答共用同一套检索链路,所见即所得;输入问题查看各阶段召回</span>
      </div>
    </template>

    <el-form :inline="true" @submit.prevent>
      <el-form-item label="问题">
        <el-input v-model="form.question" placeholder="输入一个学生可能会问的问题" style="width: 340px"
          @keyup.enter="run" clearable />
      </el-form-item>
      <el-form-item label="Top-K">
        <el-input-number v-model="form.topK" :min="1" :max="20" />
      </el-form-item>
      <el-form-item label="阈值">
        <el-slider v-model="form.threshold" :min="0" :max="1" :step="0.05" style="width: 160px" />
      </el-form-item>
      <el-form-item label="重排">
        <el-switch v-model="form.rerank" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="run">检索</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="ran && rows.length === 0" type="info" :closable="false"
      title="未召回任何内容:确认课程知识库已有已解析文档,或降低相似度阈值" />

    <el-table v-if="rows.length" :data="rows" border stripe size="small">
      <el-table-column prop="rank" label="名次" width="56" align="center" />
      <el-table-column label="来源" min-width="180">
        <template #default="{ row }">
          <div>{{ row.docTitle }}</div>
          <div v-if="row.page" class="sub">第 {{ row.page }} 页 · 块 #{{ row.chunkId }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="snippet" label="原文片段" min-width="260" show-overflow-tooltip />
      <el-table-column label="关键词分" width="90" align="center">
        <template #default="{ row }">{{ row.keywordScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="向量分" width="90" align="center">
        <template #default="{ row }">{{ row.vectorScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="融合分" width="90" align="center">
        <template #default="{ row }">{{ row.rrfScore }}</template>
      </el-table-column>
      <el-table-column label="重排分" width="90" align="center">
        <template #default="{ row }">{{ row.rerankScore ?? '-' }}</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const form = reactive({ question: '', topK: 5, threshold: 0.5, rerank: true })
const rows = ref([])
const loading = ref(false)
const ran = ref(false)

const run = async () => {
  if (!form.question.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.value = true
  try {
    rows.value = await api.kbDebug({
      question: form.question.trim(),
      topK: form.topK,
      threshold: form.threshold,
      rerank: form.rerank
    })
    ran.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>

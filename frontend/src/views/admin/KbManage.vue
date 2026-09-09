<template>
  <el-row :gutter="16">
    <!-- 检索参数配置 -->
    <el-col :span="10">
      <el-card shadow="never">
        <template #header>检索参数配置</template>
        <el-form :model="form" label-width="120px">
          <el-form-item label="召回数量 Top-K">
            <el-input-number v-model="form.topK" :min="1" :max="20" />
          </el-form-item>
          <el-form-item label="相似度阈值">
            <el-slider v-model="form.similarityThreshold" :min="0" :max="1" :step="0.05" show-input />
          </el-form-item>
          <el-form-item label="分块大小(字符)">
            <el-input-number v-model="form.chunkSize" :min="100" :max="2000" :step="50" />
          </el-form-item>
          <el-form-item label="分块重叠(字符)">
            <el-input-number v-model="form.chunkOverlap" :min="0" :max="500" :step="10" />
          </el-form-item>
          <el-form-item label="重排精排">
            <el-switch v-model="form.rerankEnabled" active-text="开" inactive-text="关" />
            <div class="el-form-item__content" style="margin-left: 120px; line-height: 1.4">
              <span style="font-size: 12px; color: var(--el-text-color-secondary)">
                开启后对双路召回结果用 BGE-Reranker 精排(需配置 RERANK_API_KEY,默认复用 EMBED_API_KEY);关闭则仅 RRF 融合</span>
            </div>
          </el-form-item>
          <el-form-item label="提示词追加">
            <el-input v-model="form.promptSuffix" type="textarea" :rows="3"
              placeholder="追加到系统提示词末尾,如:回答时多结合数字媒体专业的实际应用场景" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <!-- 向量库状态与重建 -->
    <el-col :span="14">
      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>向量库状态</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Chroma 地址">{{ status.chromaUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Collection">{{ status.collection || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文档总数">{{ status.docTotal ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="已解析">{{ status.docParsed ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="分块大小(默认)">{{ status.chunkSize ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="分块重叠(默认)">{{ status.chunkOverlap ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never">
        <template #header>向量库删除重建</template>
        <el-form :inline="true">
          <el-form-item label="课程">
            <el-select v-model="rebuildCourseId" placeholder="选择课程" style="width: 220px">
              <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="warning" :loading="rebuilding" @click="rebuild">
              重建该课程向量(重新解析全部文档)
            </el-button>
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false"
          title="修改分块大小/重叠后,需要对已有文档执行重建才会生效;重建会删除旧向量并重新嵌入入库" />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const form = reactive({
  topK: 5,
  similarityThreshold: 0.5,
  chunkSize: 500,
  chunkOverlap: 50,
  promptSuffix: '',
  rerankEnabled: true
})
const saving = ref(false)
const status = ref({})
const courses = ref([])
const rebuildCourseId = ref(null)
const rebuilding = ref(false)

const load = async () => {
  const cfg = await api.kbConfig()
  Object.assign(form, cfg)
  status.value = await api.kbStatus()
}

const save = async () => {
  saving.value = true
  try {
    await api.saveKbConfig({ ...form })
    ElMessage.success('配置已保存,提问接口即时生效')
  } finally {
    saving.value = false
  }
}

const rebuild = async () => {
  if (!rebuildCourseId.value) {
    ElMessage.warning('请选择课程')
    return
  }
  const course = courses.value.find((c) => c.course_id === rebuildCourseId.value)
  await ElMessageBox.confirm(
    `将重新解析课程「${course?.course_name}」的全部 ${course?.doc_count} 个文档并重建向量,耗时较长,确定?`,
    '删除重建',
    { type: 'warning' }
  )
  rebuilding.value = true
  try {
    const result = await api.rebuildKb(rebuildCourseId.value)
    ElMessage.success(`重建完成:成功 ${result.success} / 失败 ${result.failed}`)
    load()
  } finally {
    rebuilding.value = false
  }
}

onMounted(async () => {
  courses.value = await api.listCourses()
  load()
})
</script>

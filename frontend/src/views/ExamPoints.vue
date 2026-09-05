<template>
  <div class="page-card">
    <el-form :inline="true">
      <el-form-item label="课程">
        <el-select v-model="courseId" placeholder="选择课程" style="width: 220px">
          <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
        </el-select>
      </el-form-item>
      <el-form-item label="章节(可选)">
        <el-input v-model="chapter" placeholder="如:第三章 图像增强" style="width: 220px" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="generating" @click="generate">生成考点与练习题</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="result"
      title="生成完成,以下内容由 AI 基于课程知识库生成,仅供参考,建议结合教材核对"
      type="success"
      :closable="false"
      style="margin-bottom: 12px"
    />
    <el-empty v-if="!result && !generating" description="选择课程后点击生成,将基于知识库输出知识点梳理与练习题" />
    <div v-if="result" class="pre-wrap result-box" v-loading="generating">{{ result }}</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const courses = ref([])
const courseId = ref(null)
const chapter = ref('')
const result = ref('')
const generating = ref(false)

const generate = async () => {
  if (!courseId.value) {
    ElMessage.warning('请选择课程')
    return
  }
  generating.value = true
  result.value = ''
  try {
    result.value = await api.exam({ courseId: courseId.value, chapter: chapter.value })
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  courses.value = await api.listCourses()
})
</script>

<style scoped>
.result-box {
  background: var(--surface-2);
  border-radius: 8px;
  padding: 16px;
  min-height: 200px;
}
</style>

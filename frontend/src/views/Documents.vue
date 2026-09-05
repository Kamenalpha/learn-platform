<template>
  <div class="page-card">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-select v-model="courseId" placeholder="全部课程" clearable style="width: 220px" @change="load">
          <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
        </el-select>
        <span style="color: var(--mist); font-size: 13px">共 {{ docs.length }} 个文档</span>
      </div>
      <el-button v-if="userStore.isAdmin" type="primary" @click="uploadVisible = true">上传文档</el-button>
    </div>

    <el-table :data="docs" v-loading="loading" stripe>
      <el-table-column prop="docTitle" label="文档标题" min-width="220" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.fileType)" size="small">{{ row.fileType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="解析状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.parseStatus)" size="small">{{ statusText(row.parseStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="分块数" width="90" />
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="preview(row)">预览</el-button>
          <el-button v-if="row.fileType === 'pdf'" size="small" @click="openFile(row)">原文件</el-button>
          <el-button v-if="userStore.isAdmin" size="small" type="primary" plain :loading="reparsing === row.docId"
            @click="reparse(row)">重新解析</el-button>
          <el-button v-if="userStore.isAdmin" size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadVisible" title="上传课件文档" width="480px">
      <el-form label-width="90px">
        <el-form-item label="所属课程">
          <el-select v-model="uploadCourseId" placeholder="选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <input type="file" :accept="accept" @change="onFileChange" />
        </el-form-item>
        <el-form-item>
          <span style="color: var(--mist); font-size: 12px">
            支持 PDF / Word / PPT / TXT,单文件 ≤100MB,上传后自动解析并写入向量库
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">上传并解析</el-button>
      </template>
    </el-dialog>

    <!-- 分块预览 -->
    <el-drawer v-model="previewVisible" :title="'分块预览 - ' + (currentDoc?.docTitle || '')" size="55%">
      <div v-for="ch in chunks" :key="ch.chunkId" class="chunk-item">
        <div class="chunk-head">
          #{{ ch.chunkIndex }}
          <el-tag v-if="ch.pageNum" size="small" style="margin-left: 8px">第 {{ ch.pageNum }} 页</el-tag>
        </div>
        <div class="pre-wrap chunk-body">{{ ch.content }}</div>
      </div>
      <el-empty v-if="chunks.length === 0" description="暂无分块,请先解析" />
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const userStore = useUserStore()

const courses = ref([])
const docs = ref([])
const courseId = ref(null)
const loading = ref(false)

const uploadVisible = ref(false)
const uploadCourseId = ref(null)
const uploadFile = ref(null)
const uploading = ref(false)
const accept = '.pdf,.doc,.docx,.ppt,.pptx,.txt'

const previewVisible = ref(false)
const currentDoc = ref(null)
const chunks = ref([])
const reparsing = ref(null)

const load = async () => {
  loading.value = true
  try {
    docs.value = await api.listDocs(courseId.value || undefined)
  } finally {
    loading.value = false
  }
}

const onFileChange = (e) => {
  uploadFile.value = e.target.files[0] || null
}

const doUpload = async () => {
  if (!uploadCourseId.value) {
    ElMessage.warning('请选择所属课程')
    return
  }
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  const formData = new FormData()
  formData.append('file', uploadFile.value)
  formData.append('courseId', uploadCourseId.value)
  try {
    const doc = await api.uploadDoc(formData)
    ElMessage.success(doc.parseStatus === 1 ? '上传并解析成功' : '上传成功,解析失败可稍后重新解析')
    uploadVisible.value = false
    courseId.value = doc.courseId
    load()
  } finally {
    uploading.value = false
  }
}

const preview = async (row) => {
  currentDoc.value = row
  const detail = await api.docDetail(row.docId)
  chunks.value = detail.chunks
  previewVisible.value = true
}

const openFile = (row) => {
  window.open(api.docFileUrl(row.docId, userStore.token), '_blank')
}

const reparse = async (row) => {
  reparsing.value = row.docId
  try {
    await api.reparseDoc(row.docId)
    ElMessage.success('重新解析完成')
    load()
  } finally {
    reparsing.value = null
  }
}

const remove = (row) => {
  ElMessageBox.confirm(`删除文档「${row.docTitle}」将同时删除其向量与分块,确定?`, '提示', { type: 'warning' })
    .then(async () => {
      await api.deleteDoc(row.docId)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

const typeTag = (t) => ({ pdf: 'danger', word: 'primary', ppt: 'warning', txt: 'info' }[t] || 'info')
const statusTag = (s) => ({ 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info')
const statusText = (s) => ({ 0: '未解析', 1: '已解析', 2: '解析失败' }[s] || '未知')
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '')

onMounted(async () => {
  courses.value = await api.listCourses()
  courseId.value = route.query.courseId ? Number(route.query.courseId) : null
  uploadCourseId.value = courseId.value
  load()
})
</script>

<style scoped>
.chunk-item {
  margin-bottom: 14px;
}

.chunk-head {
  font-size: 13px;
  color: var(--amber);
  font-weight: 600;
}

.chunk-body {
  background: var(--surface-2);
  border-radius: 6px;
  padding: 10px;
  font-size: 13px;
  color: var(--paper);
  max-height: 220px;
  overflow-y: auto;
}
</style>

<template>
  <div class="page-card kb-root">
    <!-- 学科顶栏 -->
    <div class="subject-bar">
      <div class="subject-title">知识体系</div>
      <el-tabs v-model="activeSubject" type="card" @tab-change="onSubjectChange">
        <el-tab-pane v-for="s in subjects" :key="s.subjectId" :name="String(s.subjectId)">
          <template #label>{{ s.subjectName }}</template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 课程区 -->
    <div v-if="!selectedCourse" class="course-area">
      <div class="sec-head">
        <span class="sec-title">课程</span>
        <el-button type="primary" size="small" :icon="Plus" @click="openCourseDialog()">新建课程</el-button>
      </div>
      <el-row :gutter="12" v-loading="courseLoading">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="c in visibleCourses" :key="c.course_id">
          <div class="course-card" @click="openCourse(c)">
            <div class="course-name">{{ c.course_name }}</div>
            <div class="course-meta">
              <el-tag size="small" :type="visTag(c.visibility)">{{ visText(c.visibility) }}</el-tag>
              <span>{{ c.doc_count || 0 }} 个资料</span>
            </div>
            <div class="course-desc">{{ c.subject_name || '' }}</div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="!courseLoading && visibleCourses.length === 0" description="该学科暂无课程,点击右上角新建" />
    </div>

    <!-- 课程详情:知识树 + 资料 -->
    <div v-else class="course-detail">
      <div class="detail-head">
        <el-button :icon="ArrowLeft" text @click="closeCourse">返回</el-button>
        <span class="sec-title">{{ selectedCourse.course_name }}</span>
        <div style="flex: 1"></div>
        <el-button v-if="userStore.isAdmin" size="small" @click="openCourseDialog(selectedCourse)">编辑</el-button>
        <el-button size="small" type="danger" plain @click="removeCourse(selectedCourse)">删除</el-button>
        <el-button type="primary" size="small" :icon="Upload" @click="uploadVisible = true">上传资料</el-button>
      </div>

      <el-row :gutter="12">
        <!-- 知识树 -->
        <el-col :xs="24" :sm="8" :md="6">
          <div class="tree-card">
            <div class="tree-head">知识树(章节/知识点)</div>
            <el-tree :data="chapterTree" :props="{ label: 'name', children: 'children' }" node-key="key"
                     :expand-on-click-node="false" default-expand-all>
              <template #default="{ data }">
                <span class="tree-node">
                  {{ data.label }}
                </span>
              </template>
            </el-tree>
            <div v-if="chapterTree.length === 0" style="color:#909399;font-size:12px;padding:12px">暂无章节,可在下方新建</div>
          </div>
          <div class="add-node">
            <el-input v-model="newChapterName" size="small" placeholder="新增章节名" style="width: calc(100% - 84px)">
              <template #prepend>章节</template>
            </el-input>
            <el-button size="small" type="primary" plain @click="addChapter">添加</el-button>
          </div>
        </el-col>

        <!-- 资料列表 -->
        <el-col :xs="24" :sm="16" :md="18">
          <el-table :data="resources" v-loading="resLoading" stripe>
            <el-table-column prop="title" label="资料标题" min-width="200" />
            <el-table-column label="类型" width="84">
              <template #default="{ row }">
                <el-tag :type="typeTag(row.fileType)" size="small">{{ row.fileType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="解析" width="104">
              <template #default="{ row }">
                <el-tooltip :content="row.failReason || ''" :disabled="!row.failReason">
                  <el-tag :type="statusTag(row.parseStatus)" size="small">{{ statusText(row.parseStatus) }}</el-tag>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="分块" width="76" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" @click="preview(row)">预览</el-button>
                <el-button size="small" @click="openFile(row)">原文件</el-button>
                <el-button size="small" type="primary" plain :loading="reparsing === row.resourceId"
                           @click="reparse(row)">重解析</el-button>
                <el-button size="small" type="danger" plain @click="removeDoc(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!resLoading && resources.length === 0" description="该课程暂无资料,点击右上角上传" />
        </el-col>
      </el-row>
    </div>

    <!-- 上传 -->
    <el-dialog v-model="uploadVisible" title="上传教材/资料" width="440px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="uploadTitle" placeholder="留空则取文件名" />
        </el-form-item>
        <el-form-item label="所属课程">
          <el-select v-model="uploadCourseId" style="width:100%" placeholder="选择课程">
            <el-option v-for="c in visibleCourses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <input type="file" :accept="accept" @change="onFileChange" />
        </el-form-item>
        <div style="color:#909399;font-size:12px;line-height:1.5">
          支持 PDF / Word / PPT / TXT,≤100MB;上传后自动解析入库(扫描件走 OCR)。扫描件需平台已配置 OCR 服务,否则会解析为空。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">上传并解析</el-button>
      </template>
    </el-dialog>

    <!-- 分块预览 -->
    <el-drawer v-model="previewVisible" :title="'分块预览 - ' + (currentDoc?.title || '')" size="55%">
      <div v-for="ch in chunks" :key="ch.chunkId" class="chunk-item">
        <div class="chunk-head">#{{ ch.chunkIndex }}<el-tag v-if="ch.pageNum" size="small" style="margin-left:8px">第{{ ch.pageNum }}页</el-tag></div>
        <div class="pre-wrap chunk-body">{{ ch.content }}</div>
      </div>
      <el-empty v-if="chunks.length === 0" description="暂无分块,请先解析" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, ArrowLeft } from '@element-plus/icons-vue'
import { api } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const accept = '.pdf,.doc,.docx,.ppt,.pptx,.txt'

const subjects = ref([])
const courses = ref([])
const activeSubject = ref('')
const courseLoading = ref(false)

const selectedCourse = ref(null)
const resources = ref([])
const resLoading = ref(false)
const chapterTree = ref([])
const newChapterName = ref('')
const loadedKps = ref(new Map()) // chapterId -> kpList(两态)

const uploadVisible = ref(false)
const uploadTitle = ref('')
const uploadCourseId = ref(null)
const uploadFile = ref(null)
const uploading = ref(false)

const previewVisible = ref(false)
const currentDoc = ref(null)
const chunks = ref([])
const reparsing = ref(null)

const visibleCourses = computed(() =>
  courses.value.filter((c) => String(c.subject_id) === activeSubject.value))

const loadSubjects = async () => {
  subjects.value = await api.listSubjects()
  if (subjects.value.length) {
    activeSubject.value = String(subjects.value[0].subjectId)
  }
  await loadCourses()
}

const loadCourses = async () => {
  courseLoading.value = true
  try {
    courses.value = await api.listCourses()
  } finally {
    courseLoading.value = false
  }
}

const onSubjectChange = () => {
  closeCourse()
}

const openCourse = async (c) => {
  selectedCourse.value = c
  await loadCourseDetail()
}

const closeCourse = () => {
  selectedCourse.value = null
  resources.value = []
  chapterTree.value = []
  loadedKps.value = new Map()
}

const loadCourseDetail = async () => {
  await Promise.all([loadResources(), loadChapters()])
}

const loadResources = async () => {
  resLoading.value = true
  try {
    resources.value = await api.listDocs(selectedCourse.value?.course_id)
  } finally {
    resLoading.value = false
  }
}

const loadChapters = async () => {
  const list = await api.listChapters(selectedCourse.value.course_id)
  chapterTree.value = list
    .filter((c) => !c.parentId)
    .map((c) => ({
      key: 'ch-' + c.chapterId,
      label: c.chapterName || ('章节' + c.chapterId),
      chapter: c,
      children: []
    }))
}

const addChapter = async () => {
  if (!newChapterName.value.trim()) {
    ElMessage.warning('请输入章节名')
    return
  }
  await api.addChapter({ courseId: selectedCourse.value.course_id, chapterName: newChapterName.value.trim() })
  newChapterName.value = ''
  ElMessage.success('章节已添加')
  await loadChapters()
}

// 课程 CRUD
const openCourseDialog = (course) => {
  ElMessageBox.prompt('课程名称', course ? '编辑课程' : '新建课程', {
    inputValue: course ? course.course_name : '',
    inputPlaceholder: '如:高等数学(第一章)'
  }).then(async ({ value }) => {
    if (!value.trim()) return
    await api.addCourse({
      courseId: course ? course.course_id : undefined,
      subjectId: Number(activeSubject.value),
      courseName: value.trim()
    })
    ElMessage.success(course ? '已更新' : '已创建')
    await loadCourses()
  }).catch(() => {})
}

const removeCourse = (c) => {
  ElMessageBox.confirm(`删除课程「${c.course_name}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await api.deleteCourse(c.course_id)
      ElMessage.success('已删除')
      closeCourse()
      await loadCourses()
    }).catch(() => {})
}

// 上传
const onFileChange = (e) => {
  uploadFile.value = e.target.files[0] || null
  if (!uploadTitle.value && uploadFile.value) {
    uploadTitle.value = uploadFile.value.name.replace(/\.[^.]+$/, '')
  }
}

const doUpload = async () => {
  const courseId = uploadCourseId.value || selectedCourse.value?.course_id
  if (!courseId) {
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
  formData.append('courseId', courseId)
  try {
    const doc = await api.uploadDoc(formData)
    ElMessage.success(doc.parseStatus === 1 ? '上传并解析成功' : '上传成功;解析失败可重新解析')
    uploadVisible.value = false
    uploadFile.value = null
    uploadTitle.value = ''
    if (selectedCourse.value && String(selectedCourse.value.course_id) === String(courseId)) {
      await loadResources()
    }
  } finally {
    uploading.value = false
  }
}

// 预览 / 原文件 / 重解析 / 删除
const preview = async (row) => {
  currentDoc.value = row
  const detail = await api.docDetail(row.resourceId)
  chunks.value = detail.chunks
  previewVisible.value = true
}

const openFile = async (row) => {
  try {
    const res = await api.fetchDocFile(row.resourceId)
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
  } catch (e) {
    ElMessage.error('打开原文件失败')
  }
}

const reparse = async (row) => {
  reparsing.value = row.resourceId
  try {
    await api.reparseDoc(row.resourceId)
    ElMessage.success('重新解析完成')
    await loadResources()
  } finally {
    reparsing.value = null
  }
}

const removeDoc = (row) => {
  ElMessageBox.confirm(`删除资料「${row.title}」将同时删除其向量与分块,确定?`, '提示', { type: 'warning' })
    .then(async () => {
      await api.deleteDoc(row.resourceId)
      ElMessage.success('已删除')
      await loadResources()
    }).catch(() => {})
}

const typeTag = (t) => ({ pdf: 'danger', word: 'primary', ppt: 'warning', txt: 'info' }[t] || 'info')
const statusTag = (s) => ({ 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info')
const statusText = (s) => ({ 0: '未解析', 1: '已解析', 2: '解析失败' }[s] || '未知')
const visTag = (v) => ({ 0: 'info', 1: 'success', 2: 'warning' }[v] || 'info')
const visText = (v) => ({ 0: '私有', 1: '公开', 2: '分享' }[v] || '未知')

onMounted(async () => {
  await loadSubjects()
})
</script>

<style scoped>
.kb-root {
  min-height: 100%;
}

.subject-bar {
  margin-bottom: 16px;
}

.subject-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.course-area .sec-head,
.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.sec-title {
  font-size: 15px;
  font-weight: 600;
}

.course-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all .2s;
}

.course-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, .06);
}

.course-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 12px;
}

.course-desc {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.tree-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  min-height: 220px;
}

.tree-head {
  font-weight: 600;
  font-size: 13px;
  margin: 4px 4px 8px;
}

.add-node {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}

.chunk-item {
  margin-bottom: 14px;
}

.chunk-head {
  font-size: 13px;
  color: #409eff;
  font-weight: 600;
}

.chunk-body {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  font-size: 13px;
  color: #303133;
  max-height: 220px;
  overflow-y: auto;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .course-card {
    margin-bottom: 8px;
  }
}
</style>

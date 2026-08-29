<template>
  <div class="page-card">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h3 style="margin: 0">课程知识库</h3>
      <el-button v-if="userStore.isAdmin" type="primary" @click="openEdit()">+ 新增课程</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="c in courses" :key="c.course_id" :span="8" style="margin-bottom: 16px">
        <el-card shadow="hover" class="course-card" @click="goDocs(c)">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 16px; font-weight: 600">{{ c.course_name }}</span>
            <span style="color: #409eff">{{ c.doc_count }} 个文档</span>
          </div>
          <p style="color: #909399; font-size: 13px; min-height: 38px">{{ c.description }}</p>
          <div v-if="userStore.isAdmin" style="text-align: right" @click.stop>
            <el-button size="small" @click="openEdit(c)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="removeCourse(c)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="courses.length === 0" description="暂无课程" />

    <!-- 新增/编辑课程 -->
    <el-dialog v-model="editVisible" :title="form.course_id ? '编辑课程' : '新增课程'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="课程名称">
          <el-input v-model="form.course_name" placeholder="如:数字图像处理" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const courses = ref([])
const editVisible = ref(false)
const form = reactive({ course_id: null, course_name: '', description: '' })

const load = async () => {
  courses.value = await api.listCourses()
}

const goDocs = (c) => {
  router.push({ path: '/docs', query: { courseId: c.course_id } })
}

const openEdit = (c) => {
  Object.assign(form, c ? { ...c } : { course_id: null, course_name: '', description: '' })
  editVisible.value = true
}

const saveCourse = async () => {
  if (!form.course_name.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  if (form.course_id) {
    await api.updateCourse({ ...form })
  } else {
    await api.addCourse({ ...form })
  }
  ElMessage.success('保存成功')
  editVisible.value = false
  load()
}

const removeCourse = (c) => {
  ElMessageBox.confirm(`确定删除课程「${c.course_name}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await api.deleteCourse(c.course_id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.course-card {
  cursor: pointer;
}
</style>

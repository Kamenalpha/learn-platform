<template>
  <div class="page-card">
    <el-tabs v-model="tab">
      <el-tab-pane label="出题模拟" name="gen" />
      <el-tab-pane label="考试记录" name="record" />
      <el-tab-pane label="错题本" name="mistake" />
    </el-tabs>

    <!-- 出题 -->
    <div v-if="tab === 'gen'" class="gen-wrap">
      <el-form :inline="true" class="gen-form">
        <el-form-item label="出处">
          <el-radio-group v-model="sourceType">
            <el-radio :label="0">教材块</el-radio>
            <el-radio :label="1">用户重点</el-radio>
            <el-radio :label="2">样卷</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="sourceType !== 1" label="课程">
          <el-select v-model="courseId" placeholder="选择课程" style="width:180px" @change="loadResources">
            <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="sourceType === 2" label="样卷">
          <el-select v-model="resourceId" placeholder="选择样卷资料" style="width:180px">
            <el-option v-for="r in resources" :key="r.resourceId" :label="r.title" :value="r.resourceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="题量">
          <el-input-number v-model="count" :min="2" :max="20" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="difficulty">
            <el-radio :label="1">简单</el-radio>
            <el-radio :label="2">中等</el-radio>
            <el-radio :label="3">难</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="generating" @click="generate">生成题目</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="generated" type="success" :closable="false" show-icon
                title="已生成题目,可预览或开始模拟考试" style="margin-bottom:12px" />

      <!-- 预览 -->
      <div v-if="questions && !answering" class="q-list">
        <div v-for="(q, i) in questions" :key="q.questionId" class="q-item">
          <div class="q-stem"><b>{{ i + 1 }}.</b> [{{ typeName(q.qtype) }}] {{ q.stem }}</div>
          <div v-if="q.options" class="q-opts">
            <div v-for="(op, j) in parseOptions(q.options)" :key="j">{{ op }}</div>
          </div>
          <div class="q-answer">答案:{{ q.answer }} <span v-if="q.analysis">· {{ q.analysis }}</span></div>
        </div>
        <el-button type="primary" @click="startExam">开始模拟考试</el-button>
      </div>

      <!-- 作答 -->
      <div v-else-if="questions && answering" class="q-list">
        <div v-for="(q, i) in questions" :key="q.questionId" class="q-item">
          <div class="q-stem"><b>{{ i + 1 }}.</b> [{{ typeName(q.qtype) }}] {{ q.stem }}</div>
          <template v-if="q.qtype === 0">
            <el-radio-group v-model="answers[q.questionId]">
              <el-radio v-for="(op, j) in parseOptions(q.options)" :key="j" :label="op">{{ op }}</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="q.qtype === 1">
            <el-checkbox-group v-model="answers[q.questionId]">
              <el-checkbox v-for="(op, j) in parseOptions(q.options)" :key="j" :label="op">{{ op }}</el-checkbox>
            </el-checkbox-group>
          </template>
          <template v-else-if="q.qtype === 2">
            <el-radio-group v-model="answers[q.questionId]">
              <el-radio label="对">对</el-radio>
              <el-radio label="错">错</el-radio>
            </el-radio-group>
          </template>
          <template v-else>
            <el-input v-model="answers[q.questionId]" placeholder="请输入答案" />
          </template>
        </div>
        <el-button type="primary" :loading="submitting" @click="submit">交卷</el-button>
        <el-button @click="answering = false">返回预览</el-button>
      </div>

      <el-empty v-if="tab === 'gen' && !questions" description="选择出处与参数,点击生成题目" />
    </div>

    <!-- 考试记录 -->
    <div v-if="tab === 'record'">
      <el-table :data="exams" v-loading="loadingExams">
        <el-table-column prop="title" label="试卷" min-width="200" />
        <el-table-column prop="score" label="得分" width="120" />
        <el-table-column prop="time" label="时间" width="200" />
      </el-table>
      <el-empty v-if="!loadingExams && exams.length === 0" description="暂无考试记录" />
    </div>

    <!-- 错题本 -->
    <div v-if="tab === 'mistake'">
      <el-table :data="mistakeList" v-loading="loadingMistakes">
        <el-table-column prop="stem" label="题干" min-width="260" />
        <el-table-column prop="answer" label="正确答案" width="140" />
        <el-table-column prop="wrongCount" label="错次" width="80" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="master(row)">已掌握</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loadingMistakes && mistakeList.length === 0" description="暂无错题" />
    </div>

    <!-- 评分结果 -->
    <el-dialog v-model="resultVisible" title="考试结果" width="480px">
      <div v-if="result" class="result">
        <div class="result-score">得分:{{ result.score }} / {{ result.total }}</div>
        <el-table :data="result.items" size="small">
          <el-table-column label="题号" width="70">
            <template #default="{}">{{ '—' }}</template>
          </el-table-column>
          <el-table-column label="是否正确" width="110">
            <template #default="{ row }">
              <el-tag :type="row.correct ? 'success' : 'danger'" size="small">{{ row.correct ? '对' : '错' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const tab = ref('gen')
const courses = ref([])
const resources = ref([])
const sourceType = ref(0)
const courseId = ref(null)
const resourceId = ref(null)
const count = ref(5)
const difficulty = ref(1)

const generated = ref(false)
const questions = ref([])
const answering = ref(false)
const answers = ref({})
const paperId = ref(null)
const generating = ref(false)
const submitting = ref(false)

const exams = ref([])
const loadingExams = ref(false)
const mistakeList = ref([])
const loadingMistakes = ref(false)

const resultVisible = ref(false)
const result = ref(null)

const typeName = (t) => ({ 0: '单选', 1: '多选', 2: '判断', 3: '填空', 4: '简答' }[t] || '题')
const parseOptions = (s) => {
  try { return JSON.parse(s) || [] } catch { return [] }
}

const loadResources = async () => {
  resources.value = courseId.value ? await api.listDocs(courseId.value) : []
}

const generate = async () => {
  if (sourceType.value !== 1 && !courseId.value) {
    ElMessage.warning('请选择课程')
    return
  }
  generating.value = true
  try {
    const data = await api.examGenerate({
      courseId: courseId.value, resourceId: resourceId.value, sourceType: sourceType.value,
      count: count.value, difficulty: difficulty.value
    })
    paperId.value = data.paperId
    questions.value = data.questions || []
    answers.value = {}
    generated.value = true
    answering.value = false
    ElMessage.success(data.questions?.length ? '已生成 ' + data.questions.length + ' 道题' : '生成成功')
  } finally {
    generating.value = false
  }
}

const startExam = () => {
  answers.value = {}
  answering.value = true
}

const submit = async () => {
  submitting.value = true
  const list = (questions.value || []).map((q) => ({
    questionId: q.questionId,
    userAnswer: normalizeAnswer(q, answers.value[q.questionId])
  }))
  try {
    result.value = await api.examSubmit({ paperId: paperId.value, answers: list })
    resultVisible.value = true
    answering.value = false
    await Promise.all([loadExams(), loadMistakes()])
  } finally {
    submitting.value = false
  }
}

const normalizeAnswer = (q, v) => {
  if (v == null) return ''
  if (q.qtype === 0) return String(v).replace(/^(.)\.?.*/, '$1') // 单选取首字母
  if (q.qtype === 1) return Array.isArray(v) ? v.map((x) => String(x).replace(/^(.)\.?.*/, '$1')).sort().join(',') : String(v)
  return String(v)
}

const loadExams = async () => {
  loadingExams.value = true
  try { exams.value = await api.examList() } finally { loadingExams.value = false }
}

const loadMistakes = async () => {
  loadingMistakes.value = true
  try { mistakeList.value = await api.mistakes() } finally { loadingMistakes.value = false }
}

const master = async (row) => {
  await api.masterMistake(row.mistakeId)
  ElMessage.success('已标记掌握')
  await loadMistakes()
}

onMounted(async () => {
  courses.value = await api.listCourses()
  await Promise.all([loadExams(), loadMistakes()])
})
</script>

<style scoped>
.gen-form {
  margin-bottom: 12px;
}

.q-list .q-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.q-stem {
  font-size: 14px;
  margin-bottom: 8px;
}

.q-opts {
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}

.q-answer {
  color: #909399;
  font-size: 12px;
}

.result-score {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 12px;
}
</style>

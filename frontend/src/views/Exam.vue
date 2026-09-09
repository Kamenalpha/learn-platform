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
                title="试卷已生成。正式考试开始后计时,答案将在交卷后判定。" style="margin-bottom:12px" />

      <!-- 预览 -->
      <div v-if="questions.length && !answering" class="q-list">
        <div v-for="(q, i) in questions" :key="q.questionId" class="q-item">
          <div class="q-stem"><b>{{ i + 1 }}.</b> [{{ typeName(q.qtype) }}] {{ q.stem }}</div>
          <div v-if="q.options?.length" class="q-opts">
            <div v-for="(op, j) in q.options" :key="j">{{ op }}</div>
          </div>
        </div>
        <el-button type="primary" :loading="starting" @click="startExam">开始模拟考试</el-button>
      </div>

      <!-- 作答 -->
      <div v-else-if="questions.length && answering" class="q-list">
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px"
                  :title="remainingText ? '剩余时间: ' + remainingText : '本试卷不限时'" />
        <div v-for="(q, i) in questions" :key="q.questionId" class="q-item">
          <div class="q-stem"><b>{{ i + 1 }}.</b> [{{ typeName(q.qtype) }}] {{ q.stem }}</div>
          <template v-if="q.qtype === 0">
            <el-radio-group v-model="answers[q.questionId]">
              <el-radio v-for="(op, j) in q.options" :key="j" :value="optionKey(op)">{{ op }}</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="q.qtype === 1">
            <el-checkbox-group v-model="answers[q.questionId]">
              <el-checkbox v-for="(op, j) in q.options" :key="j" :value="optionKey(op)">{{ op }}</el-checkbox>
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
      </div>

      <el-empty v-if="tab === 'gen' && !questions.length" description="选择出处与参数,点击生成题目" />
    </div>

    <!-- 考试记录 -->
    <div v-if="tab === 'record'">
      <el-table :data="exams" v-loading="loadingExams">
        <el-table-column prop="title" label="试卷" min-width="200" />
        <el-table-column prop="score" label="得分" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
            {{ examStatus(row.status) }}
          </el-tag></template>
        </el-table-column>
        <el-table-column label="开始时间" width="170"><template #default="{ row }">{{ formatTime(row.startTime) }}</template></el-table-column>
        <el-table-column label="结束时间" width="170"><template #default="{ row }">{{ formatTime(row.endTime) }}</template></el-table-column>
        <el-table-column prop="durationMin" label="限时(分钟)" width="110" />
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
        <el-alert v-if="result.status === 'expired'" type="error" :closable="false" :title="result.message" />
        <div v-else class="result-score">得分:{{ result.score }} / {{ result.total }}</div>
        <el-table v-if="result.status !== 'expired'" :data="result.items" size="small">
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
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
const examId = ref(null)
const generating = ref(false)
const starting = ref(false)
const submitting = ref(false)
const remainingSec = ref(null)
let timer = null

const exams = ref([])
const loadingExams = ref(false)
const mistakeList = ref([])
const loadingMistakes = ref(false)

const resultVisible = ref(false)
const result = ref(null)

const typeName = (t) => ({ 0: '单选', 1: '多选', 2: '判断', 3: '填空', 4: '简答' }[t] || '题')
const examStatus = (status) => ({ 0: '进行中', 1: '已交卷', 2: '已截止' }[status] || '未知')
const formatTime = (time) => time ? String(time).replace('T', ' ').slice(0, 19) : '—'
const optionKey = (option) => String(option || '').match(/^\s*([A-Za-z])(?:[.、:：)）]|\s)/)?.[1]?.toUpperCase() || String(option)
const remainingText = computed(() => remainingSec.value == null ? ''
  : `${String(Math.floor(remainingSec.value / 60)).padStart(2, '0')}:${String(remainingSec.value % 60).padStart(2, '0')}`)

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
    examId.value = null
    stopTimer()
    generated.value = true
    answering.value = false
    ElMessage.success(data.questions?.length ? '已生成 ' + data.questions.length + ' 道题' : '生成成功')
  } finally {
    generating.value = false
  }
}

const startExam = async () => {
  if (!paperId.value || starting.value) return
  starting.value = true
  try {
    const data = await api.examStart(paperId.value)
    examId.value = data.examId
    questions.value = data.questions || []
    answers.value = {}
    answering.value = true
    startTimer(data.deadline)
  } finally {
    starting.value = false
  }
}

const submit = async () => {
  if (!examId.value || submitting.value) return
  submitting.value = true
  const list = (questions.value || []).map((q) => ({
    questionId: q.questionId,
    userAnswer: normalizeAnswer(q, answers.value[q.questionId])
  }))
  try {
    result.value = await api.examSubmit({ examId: examId.value, answers: list })
    resultVisible.value = true
    answering.value = false
    examId.value = null
    stopTimer()
    await Promise.all([loadExams(), loadMistakes()])
  } finally {
    submitting.value = false
  }
}

const normalizeAnswer = (q, v) => {
  if (v == null) return ''
  if (q.qtype === 0) return optionKey(v)
  if (q.qtype === 1) return Array.isArray(v) ? v.map(optionKey).sort().join(',') : String(v)
  return String(v)
}

const stopTimer = () => {
  if (timer) clearInterval(timer)
  timer = null
  remainingSec.value = null
}

const startTimer = (deadline) => {
  stopTimer()
  if (!deadline) return
  const tick = () => {
    remainingSec.value = Math.max(0, Math.ceil((new Date(deadline).getTime() - Date.now()) / 1000))
    if (remainingSec.value <= 1) {
      remainingSec.value = 0
      clearInterval(timer)
      timer = null
      submit()
    }
  }
  tick()
  if (remainingSec.value > 0) timer = setInterval(tick, 1000)
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
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.gen-form {
  margin-bottom: 12px;
}

.q-list .q-item {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.q-stem {
  font-size: 14px;
  margin-bottom: 8px;
}

.q-opts {
  color: var(--mist);
  font-size: 13px;
  margin-bottom: 6px;
}

.q-answer {
  color: var(--mist);
  font-size: 12px;
}

.result-score {
  font-size: 20px;
  font-weight: 700;
  color: var(--amber);
  margin-bottom: 12px;
}
</style>

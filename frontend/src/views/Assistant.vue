<template>
  <div class="assistant-root">
    <!-- 左边:助手列表 + 管理 -->
    <div class="asst-panel">
      <div class="panel-head">
        <span class="panel-title">我的助手</span>
        <el-button type="primary" size="small" :icon="Plus" @click="openManage(null)">新建</el-button>
      </div>
      <div v-for="a in assistants" :key="a.assistantId"
           class="asst-item" :class="{ active: current && current.assistantId === a.assistantId }"
           @click="switchAssistant(a)">
        <div class="asst-name">{{ a.name }}</div>
        <div class="asst-sub">绑定 {{ (a.courseIds || []).length }} 门课程</div>
        <el-icon class="edit" @click.stop="openManage(a)"><Edit /></el-icon>
      </div>
      <el-empty v-if="assistants.length === 0" description="暂无助手,点击新建" :image-size="60" />
    </div>

    <!-- 右边:对话 -->
    <div class="chat-panel">
      <div v-if="!current" class="chat-empty">
        <el-empty description="选择或新建一个 AI 助手开始学习吧" />
      </div>
      <template v-else>
        <div class="chat-head">
          <span class="asst-title">{{ current.name }}</span>
          <el-tag size="small" type="info">{{ current.model || 'deepseek-chat' }} · 引用{{ current.withReference ? '开' : '关' }}</el-tag>
        </div>
        <div class="chat-body" ref="bodyRef">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div class="bubble">
              <div class="pre-wrap">{{ m.content }}</div>
              <div v-if="m.role === 'assistant' && m.references && m.references.length" class="refs">
                <el-tag v-for="(r, j) in m.references" :key="j" size="small" type="info" class="ref-tag"
                        @click="lookup(r)">[{{ j + 1 }}] {{ r.docTitle }}<template v-if="r.page">·第{{ r.page }}页</template></el-tag>
              </div>
            </div>
          </div>
          <div v-if="loading" class="msg-row assistant"><div class="bubble">正在思考…</div></div>
        </div>
        <div class="chat-input">
          <el-input v-model="question" placeholder="向助手提问(如:什么是导数? 注意回答带引用)" @keyup.enter="send"
                    :disabled="loading" clearable>
            <template #append>
              <el-button :loading="loading" @click="send">{{ loading ? '' : '发送' }}</el-button>
            </template>
          </el-input>
        </div>
      </template>
    </div>

    <!-- 助手管理 -->
    <el-dialog v-model="manageVisible" :title="editing ? '编辑助手' : '新建助手'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="如:高数答疑助手" /></el-form-item>
        <el-form-item label="绑定课程">
          <el-select v-model="form.courseIds" multiple placeholder="选择课程(知识隔离)" style="width:100%">
            <el-option v-for="c in courses" :key="c.course_id" :label="c.course_name" :value="c.course_id" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统提示词">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="3"
                    placeholder="留空则用默认。可写:你是XX课程的助教,回答简洁并引用教材…" />
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="form.model" style="width:160px">
            <el-option label="deepseek-chat" value="deepseek-chat" />
            <el-option label="qwen-plus" value="qwen-plus" />
          </el-select>
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.05" style="width:260px" />
        </el-form-item>
        <el-form-item label="上下文轮数"><el-input-number v-model="form.contextRounds" :min="0" :max="10" /></el-form-item>
        <el-form-item label="引用来源">
          <el-switch v-model="form.withReference" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manageVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssistant">保存</el-button>
      </template>
    </el-dialog>

    <!-- 引用详情 -->
    <el-dialog v-model="refVisible" :title="'引用详情 - ' + (refSelect?.docTitle || '')" width="520px">
      <div v-if="refSelect" class="ref-body">
        <p v-if="refSelect.page">页码:第 {{ refSelect.page }} 页</p>
        <p>相似度:{{ (refSelect.score * 100).toFixed(1) }}%</p>
        <p class="pre-wrap">{{ refSelect.snippet }}</p>
        <el-button size="small" @click="viewSourceFile(refSelect)">查看原文件</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit } from '@element-plus/icons-vue'
import { api } from '../api'

const assistants = ref([])
const courses = ref([])
const current = ref(null)
const messages = ref([])
const question = ref('')
const sessionId = ref('')
const loading = ref(false)
const bodyRef = ref()

const manageVisible = ref(false)
const editing = ref(false)
const form = ref(defaultForm())

const refVisible = ref(false)
const refSelect = ref(null)

function defaultForm() {
  return { assistantId: null, name: '', courseIds: [], systemPrompt: '', model: 'deepseek-chat',
    temperature: 0.7, contextRounds: 3, withReference: 1 }
}

const load = async () => {
  assistants.value = await api.listAssistants()
  if (assistants.value.length) {
    switchAssistant(assistants.value[0])
  }
}

const switchAssistant = (a) => {
  current.value = a
  messages.value = []
  sessionId.value = (crypto.randomUUID && crypto.randomUUID()) || ('s' + Date.now())
}

const send = async () => {
  const q = question.value.trim()
  if (!q || !current.value) return
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  loading.value = true
  try {
    const resp = await api.ask({ sessionId: sessionId.value, question: q, assistantId: current.value.assistantId })
    messages.value.push({ role: 'assistant', content: resp.answer, references: resp.references || [] })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉,回答失败: ' + (e.message || e) })
  } finally {
    loading.value = false
    nextTick(() => { if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight })
  }
}

const lookup = (r) => {
  refSelect.value = r
  refVisible.value = true
}

const viewSourceFile = async (r) => {
  try {
    const res = await api.fetchDocFile(r.docId)
    const blob = new Blob([res.data])
    window.open(URL.createObjectURL(blob), '_blank')
  } catch (e) {
    ElMessage.error('打开原文件失败')
  }
}

const openManage = (a) => {
  editing.value = !!a
  form.value = a ? { assistantId: a.assistantId, name: a.name, courseIds: a.courseIds || [],
    systemPrompt: a.systemPrompt || '', model: a.model || 'deepseek-chat', temperature: a.temperature ?? 0.7,
    contextRounds: a.contextRounds ?? 3, withReference: a.withReference ?? 1 } : defaultForm()
  manageVisible.value = true
}

const saveAssistant = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写助手名称')
    return
  }
  if (!form.value.courseIds.length) {
    ElMessage.warning('请至少绑定一门课程')
    return
  }
  if (editing.value) {
    await api.updateAssistant(form.value)
  } else {
    await api.addAssistant(form.value)
  }
  ElMessage.success('已保存')
  manageVisible.value = false
  await load()
}

onMounted(async () => {
  courses.value = await api.listCourses()
  await load()
})
</script>

<style scoped>
.assistant-root {
  display: flex;
  gap: 12px;
  height: calc(100vh - 120px);
}

.asst-panel {
  width: 220px;
  background: var(--surface-2);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid var(--line);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-title {
  font-weight: 600;
}

.asst-item {
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  margin-bottom: 6px;
  border: 1px solid transparent;
}

.asst-item:hover {
  background: var(--surface-2);
}

.asst-item.active {
  border-color: var(--amber);
  background: var(--accent-wash);
}

.asst-name {
  font-weight: 600;
  font-size: 14px;
}

.asst-sub {
  color: var(--mist);
  font-size: 12px;
  margin-top: 2px;
}

.asst-item .edit {
  position: absolute;
  right: 10px;
  top: 10px;
  color: var(--mist);
  cursor: pointer;
}

.chat-panel {
  flex: 1;
  background: var(--surface-2);
  border-radius: 8px;
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-head {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 10px;
}

.asst-title {
  font-weight: 600;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.msg-row {
  display: flex;
  margin-bottom: 12px;
}

.msg-row.assistant .bubble {
  background: var(--surface-2);
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.user .bubble {
  background: var(--accent-wash);
}

.bubble {
  max-width: 72%;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
}

.refs {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ref-tag {
  cursor: pointer;
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.ref-body p {
  margin: 8px 0;
}

@media (max-width: 768px) {
  .assistant-root { flex-direction: column; height: auto; }
  .asst-panel { width: 100%; }
  .chat-panel { min-height: 480px; }
}
</style>

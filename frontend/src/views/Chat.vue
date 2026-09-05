<template>
  <div class="chat-wrap">
    <!-- 左侧会话列表 -->
    <div class="session-panel">
      <el-button type="primary" style="width: 100%" @click="newSession">+ 新建对话</el-button>
      <div
        v-for="s in sessions"
        :key="s.sessionId"
        class="session-item"
        :class="{ active: s.sessionId === sessionId }"
        @click="openSession(s.sessionId)"
      >
        <div class="session-title">{{ s.lastQuestion || '新对话' }}</div>
        <div class="session-meta">{{ s.msgCount }} 条 · {{ formatTime(s.lastTime) }}</div>
      </div>
    </div>

    <!-- 右侧消息区 -->
    <div class="chat-main">
      <div ref="msgListRef" class="msg-list">
        <div v-if="messages.length === 0" class="empty-tip">
          <h3>你好,我是数媒课程知识库助手 👋</h3>
          <p>可以问我课程知识点,例如:</p>
          <el-space wrap>
            <el-tag v-for="q in sampleQuestions" :key="q" class="sample" @click="quickAsk(q)">{{ q }}</el-tag>
          </el-space>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
          <div class="bubble" :class="m.role">
            <div class="pre-wrap">{{ m.content }}</div>
            <template v-if="m.role === 'assistant' && m.references?.length">
              <el-divider style="margin: 8px 0" />
              <div style="font-size: 12px; color: var(--mist); margin-bottom: 4px">
                参考来源(点击查看,共 {{ m.references.length }} 条,耗时 {{ m.elapsedMs }}ms):
              </div>
              <div
                v-for="(r, j) in m.references"
                :key="j"
                class="source-card"
                @click="showSource(r)"
              >
                [{{ j + 1 }}] {{ r.docTitle }}{{ r.page ? ' · 第' + r.page + '页' : '' }}
                <span v-if="r.score != null">(相似度 {{ r.score.toFixed(3) }})</span>
              </div>
            </template>
          </div>
        </div>
        <div v-if="asking" class="msg-row assistant">
          <div class="bubble assistant">正在检索知识库并生成回答…</div>
        </div>
      </div>

      <div class="input-bar">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入你的问题,Ctrl+Enter 发送"
          @keydown.ctrl.enter.prevent="send"
        />
        <el-button type="primary" :loading="asking" style="height: 66px; width: 90px" @click="send">
          发 送
        </el-button>
      </div>
    </div>

    <!-- 引用原文弹窗 -->
    <el-dialog v-model="sourceVisible" title="引用来源原文" width="560px">
      <template v-if="currentSource">
        <p style="color: var(--amber); font-weight: 600">
          {{ currentSource.docTitle }}{{ currentSource.page ? ' · 第' + currentSource.page + '页' : '' }}
        </p>
        <div class="pre-wrap" style="background: var(--surface-2); padding: 12px; border-radius: 6px">
          {{ currentSource.snippet }}
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const sessions = ref([])
const sessionId = ref('')
const messages = ref([])
const input = ref('')
const asking = ref(false)
const msgListRef = ref(null)
const sourceVisible = ref(false)
const currentSource = ref(null)

const sampleQuestions = [
  '什么是图像直方图均衡化?',
  '简述渲染管线的 stages',
  '帧间预测编码的原理是什么?'
]

const newSession = () => {
  sessionId.value = crypto.randomUUID()
  messages.value = []
}

const openSession = async (id) => {
  sessionId.value = id
  const records = await api.history(id)
  messages.value = []
  for (const r of records) {
    messages.value.push({ role: 'user', content: r.question })
    messages.value.push({
      role: 'assistant',
      content: r.answer,
      references: parseRef(r.reference),
      recordId: r.recordId,
      elapsedMs: r.elapsedMs
    })
  }
  scrollBottom()
}

const quickAsk = (q) => {
  input.value = q
  send()
}

const send = async () => {
  const question = input.value.trim()
  if (!question || asking.value) return
  if (!sessionId.value) sessionId.value = crypto.randomUUID()
  input.value = ''
  messages.value.push({ role: 'user', content: question })
  asking.value = true
  scrollBottom()
  try {
    const resp = await api.ask({ sessionId: sessionId.value, question })
    messages.value.push({
      role: 'assistant',
      content: resp.answer,
      references: resp.references,
      recordId: resp.recordId,
      elapsedMs: resp.elapsedMs
    })
    loadSessions()
  } finally {
    asking.value = false
    scrollBottom()
  }
}

const showSource = (r) => {
  currentSource.value = r
  sourceVisible.value = true
}

const loadSessions = async () => {
  try {
    sessions.value = await api.sessions()
  } catch (e) {
    sessions.value = []
  }
}

const scrollBottom = () => {
  nextTick(() => {
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  })
}

const parseRef = (json) => {
  try {
    return JSON.parse(json || '[]')
  } catch (e) {
    return []
  }
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(5, 16) : '')

onMounted(() => {
  newSession()
  loadSessions()
})
</script>

<style scoped>
.chat-wrap {
  display: flex;
  gap: 12px;
  height: calc(100vh - 104px);
}

.session-panel {
  width: 230px;
  background: var(--surface-2);
  border-radius: 8px;
  padding: 12px;
  overflow-y: auto;
  flex-shrink: 0;
}

.session-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 8px;
}

.session-item:hover,
.session-item.active {
  background: var(--accent-wash);
}

.session-title {
  font-size: 13px;
  color: var(--paper);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  font-size: 12px;
  color: var(--mist);
  margin-top: 2px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--surface-2);
  border-radius: 8px;
  overflow: hidden;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-tip {
  text-align: center;
  color: var(--mist);
  margin-top: 80px;
}

.sample {
  cursor: pointer;
}

.msg-row {
  display: flex;
  margin-bottom: 14px;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.assistant {
  justify-content: flex-start;
}

.input-bar {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--line);
}
</style>

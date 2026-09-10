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
          <h3>你好,我是多学科智能学习助手 👋</h3>
          <p>可以问我课程知识点,例如:</p>
          <el-space wrap>
            <el-tag v-for="q in sampleQuestions" :key="q" class="sample" @click="quickAsk(q)">{{ q }}</el-tag>
          </el-space>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
          <div class="bubble" :class="m.role">
            <div class="pre-wrap">
              <template v-if="m.pending && !m.content">正在检索知识库…</template>
              <template v-else>
                <template v-for="(seg, k) in splitCitations(m.content, m.references?.length || 0)" :key="k">
                  <span v-if="seg.type === 'text'">{{ seg.value }}</span>
                  <el-tag v-else size="small" class="cite-chip" @click="openCitation(i, seg.n)">{{ seg.n }}</el-tag>
                </template>
                <span v-if="m.pending" class="cursor-flash">▍</span>
              </template>
            </div>
            <template v-if="m.role === 'assistant' && m.references?.length">
              <el-divider style="margin: 8px 0" />
              <div style="font-size: 12px; color: var(--mist); margin-bottom: 4px">
                参考来源(点击跳转原文,共 {{ m.references.length }} 条,耗时 {{ m.elapsedMs }}ms):
              </div>
              <div
                v-for="(r, j) in m.references"
                :key="j"
                class="source-card"
                @click="openCitation(i, j + 1)"
              >
                [{{ j + 1 }}] {{ r.docTitle }}{{ r.page ? ' · 第' + r.page + '页' : '' }}
                <span v-if="r.score != null">(得分 {{ Number(r.score).toFixed(4) }})</span>
              </div>
            </template>
          </div>
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

    <!-- 引用来源抽屉:点击回答中的 [n] 或来源卡片打开,支持跳转原文 -->
    <CitationDrawer v-model="citeVisible" :sources="citeSources" :active-index="citeIndex" />
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import CitationDrawer from '../components/CitationDrawer.vue'
import { splitCitations } from '../utils/citations'
import { api } from '../api'

const sessions = ref([])
const sessionId = ref('')
const messages = ref([])
const input = ref('')
const asking = ref(false)
const msgListRef = ref(null)
const citeVisible = ref(false)
const citeSources = ref([])
const citeIndex = ref(0)

const sampleQuestions = [
  '什么是进程?进程和程序有什么区别?',
  '导数的定义是什么?变化率怎么理解?',
  '英语段落写作的主题句怎么写?'
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
  // 流式消息:pending 期间气泡显示占位/闪烁光标,完成后转为普通消息
  const msg = reactive({ role: 'assistant', content: '', references: null, recordId: null, elapsedMs: null, pending: true })
  messages.value.push(msg)
  scrollBottom()
  let streamed = false // 是否已收到至少一段回答(决定是否回退同步接口)
  try {
    await api.askStream(
      { sessionId: sessionId.value, question },
      (type, data) => {
        if (type === 'refs') {
          msg.references = typeof data === 'string' ? parseRef(data) : data
        } else if (type === 'delta') {
          streamed = true
          msg.content += data || ''
          scrollBottom()
        } else if (type === 'done') {
          try {
            const d = typeof data === 'string' ? JSON.parse(data) : data
            msg.recordId = d.recordId
            msg.elapsedMs = d.elapsedMs
          } catch (e) {
            // done 数据异常不阻塞收尾
          }
        } else if (type === 'error') {
          if (!msg.content) msg.content = '回答生成失败: ' + (data || '未知错误')
        }
      }
    )
  } catch (e) {
    // fetch 层异常且一无所获 → 回退同步接口(流式失败时后端未落库,不会重复记录)
  }
  if (!streamed && !msg.content) {
    try {
      const resp = await api.ask({ sessionId: sessionId.value, question })
      msg.content = resp.answer
      msg.references = resp.references
      msg.recordId = resp.recordId
      msg.elapsedMs = resp.elapsedMs
    } catch (e2) {
      // request 拦截器已统一 toast
      if (!msg.content) msg.content = '回答生成失败,请稍后重试'
    }
  }
  msg.pending = false
  asking.value = false
  loadSessions()
  scrollBottom()
}

// 打开引用抽屉并定位到第 n 条来源; n 超出范围时夹到有效区间
const openCitation = (msgIndex, n) => {
  const refs = messages.value[msgIndex]?.references || []
  if (!refs.length) return
  citeSources.value = refs
  citeIndex.value = Math.min(Math.max(n, 1), refs.length) - 1
  citeVisible.value = true
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

.cursor-flash {
  animation: blink 1s step-start infinite;
  color: var(--accent, #4a7dff);
}

.cite-chip {
  cursor: pointer;
  margin: 0 2px;
  vertical-align: baseline;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
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

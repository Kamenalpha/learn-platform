import request from '../utils/request'

/** 后端接口统一封装 */
export const api = {
  // 认证
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  me: () => request.get('/auth/me'),

  // 公开内容(游客可访问,无需登录)
  publicCourses: () => request.get('/public/courses'),
  publicCourseDocs: (courseId) => request.get(`/public/courses/${courseId}/docs`),
  publicDocPreview: (resourceId) => request.get(`/public/docs/${resourceId}/preview`),

  // 知识资讯(列表游客可访问;手动抓取仅管理员)
  newsList: (params) => request.get('/news/list', { params }),
  newsFetch: () => request.post('/admin/news/fetch'),

  // 课程
  listCourses: () => request.get('/courses'),
  addCourse: (data) => request.post('/courses', data),
  updateCourse: (data) => request.put('/courses', data),
  deleteCourse: (id) => request.delete(`/courses/${id}`),

  // 学科(分类顶层)
  listSubjects: () => request.get('/subjects'),
  addSubject: (data) => request.post('/subjects', data),
  updateSubject: (data) => request.put('/subjects', data),
  deleteSubject: (id) => request.delete(`/subjects/${id}`),

  // 章节(分类第三层)
  listChapters: (courseId) => request.get(`/chapters/course/${courseId}`),
  addChapter: (data) => request.post('/chapters', data),
  updateChapter: (data) => request.put('/chapters', data),
  deleteChapter: (id) => request.delete(`/chapters/${id}`),

  // 知识点(分类叶子)
  listKnowledgePoints: (chapterId) => request.get(`/knowledge-points/chapter/${chapterId}`),
  addKnowledgePoint: (data) => request.post('/knowledge-points', data),
  updateKnowledgePoint: (data) => request.put('/knowledge-points', data),
  deleteKnowledgePoint: (id) => request.delete(`/knowledge-points/${id}`),

  // 文档
  uploadDoc: (formData, onProgress) =>
    request.post('/docs/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    }),
  listDocs: (courseId) => request.get('/docs', { params: { courseId } }),
  docDetail: (id) => request.get(`/docs/${id}`),
  reparseDoc: (id) => request.post(`/docs/${id}/reparse`),
  deleteDoc: (id) => request.delete(`/docs/${id}`),
  setDocVisibility: (id, visibility) => request.put(`/docs/${id}/visibility`, null, { params: { visibility } }),
  // 带鉴权头拉取源文件(blob),用于预览/下载
  fetchDocFile: (id) => request.get(`/docs/${id}/file`, { responseType: 'blob' }),

  // AI 助手
  listAssistants: () => request.get('/assistants'),
  getAssistant: (id) => request.get(`/assistants/${id}`),
  addAssistant: (data) => request.post('/assistants', data),
  updateAssistant: (data) => request.put('/assistants', data),
  deleteAssistant: (id) => request.delete(`/assistants/${id}`),

  // 学习计划
  listPlans: () => request.get('/plans'),
  planDetail: (id) => request.get(`/plans/${id}`),
  addPlan: (data) => request.post('/plans', data),
  updatePlan: (data) => request.put('/plans', data),
  deletePlan: (id) => request.delete(`/plans/${id}`),
  addPlanTask: (planId, data) => request.post(`/plans/${planId}/tasks`, data),
  updatePlanTask: (data) => request.put('/plans/tasks', data),
  togglePlanTask: (taskId) => request.post(`/plans/tasks/${taskId}/toggle`),
  deletePlanTask: (taskId) => request.delete(`/plans/tasks/${taskId}`),

  // 社区
  communityFeed: (params) => request.get('/community/posts', { params }),
  postDetail: (id) => request.get(`/community/posts/${id}`),
  createPost: (data) => request.post('/community/posts', data),
  deletePost: (id) => request.delete(`/community/posts/${id}`),
  addAnswer: (postId, data) => request.post(`/community/posts/${postId}/answers`, data),
  acceptAnswer: (answerId) => request.post(`/community/answers/${answerId}/accept`),
  deleteAnswer: (answerId) => request.delete(`/community/answers/${answerId}`),
  addComment: (postId, data) => request.post(`/community/posts/${postId}/comments`, data),
  toggleLike: (postId) => request.post(`/community/posts/${postId}/like`),

  // 学习画像
  analyticsOverview: () => request.get('/analytics/overview'),

  // 出题模拟
  examGenerate: (data) => request.post('/exam/generate', data, { timeout: 300000 }),
  examPaper: (id) => request.get(`/exam/paper/${id}`),
  examSubmit: (data) => request.post('/exam/submit', data, { timeout: 300000 }),
  examList: () => request.get('/exam/exams'),
  mistakes: () => request.get('/exam/mistakes'),
  masterMistake: (id) => request.post(`/exam/mistakes/${id}/master`),

  // 项目辅导
  listProjects: () => request.get('/projects'),
  projectDetail: (id) => request.get(`/projects/${id}`),
  createProject: (data) => request.post('/projects', data, { timeout: 300000 }),
  deleteProject: (id) => request.delete(`/projects/${id}`),

  // 问答
  ask: (data) => request.post('/chat/ask', data),
  // 流式问答(SSE):onEvent(type, data) 回调 refs/delta/done/error 事件;
  // 用 fetch 而非 EventSource 以便携带 Authorization 头。返回 Promise,流结束(含异常)即 resolve
  askStream: (data, onEvent) => {
    const fire = (type, payload) => {
      try {
        onEvent && onEvent(type, payload)
      } catch (e) {
        // 回调异常不中断流
      }
    }
    const handleLine = (line) => {
      const t = line.trim()
      if (!t.startsWith('data:')) return
      const payload = t.slice(5).trim()
      if (!payload) return
      try {
        const evt = JSON.parse(payload)
        fire(evt.type, evt.data)
      } catch (e) {
        // 无法解析的帧忽略
      }
    }
    return (async () => {
      try {
        const resp = await fetch('/api/chat/ask/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + (localStorage.getItem('token') || '')
          },
          body: JSON.stringify(data)
        })
        if (!resp.ok || !resp.body) throw new Error('HTTP ' + resp.status)
        const reader = resp.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buf = ''
        for (;;) {
          const { done, value } = await reader.read()
          if (done) break
          buf += decoder.decode(value, { stream: true })
          const lines = buf.split('\n')
          buf = lines.pop() // 末尾半行留到下一轮
          lines.forEach(handleLine)
        }
        if (buf) handleLine(buf)
      } catch (e) {
        fire('error', e.message || '网络异常')
      }
    })()
  },
  sessions: () => request.get('/chat/sessions'),
  records: () => request.get('/chat/records'),
  history: (sessionId) => request.get('/chat/history', { params: { sessionId } }),
  favorites: () => request.get('/chat/favorites'),
  toggleFavorite: (recordId) => request.post(`/chat/${recordId}/favorite`),

  // 学习辅助
  graph: (courseId, refresh) =>
    request.get('/assist/graph', { params: { courseId, refresh }, timeout: 300000 }),
  exam: (data) => request.post('/assist/exam', data, { timeout: 300000 }),

  // 管理后台
  kbConfig: () => request.get('/admin/kb/config'),
  saveKbConfig: (data) => request.put('/admin/kb/config', data),
  kbStatus: () => request.get('/admin/kb/status'),
  rebuildKb: (courseId) => request.post(`/admin/kb/rebuild/${courseId}`, {}, { timeout: 600000 }),
  statsOverview: () => request.get('/admin/stats/overview'),
  qaTrend: (days) => request.get('/admin/stats/qa-trend', { params: { days } }),
  docsByCourse: () => request.get('/admin/stats/docs-by-course'),
  qaLogs: (page, size) => request.get('/admin/stats/qa-logs', { params: { page, size } }),
  users: () => request.get('/admin/users'),
  setUserRole: (id, role) => request.put(`/admin/users/${id}/role`, null, { params: { role } }),
  deleteUser: (id) => request.delete(`/admin/users/${id}`),

  // 内容审核(管理员):公开申请的通过/驳回
  auditPending: () => request.get('/admin/audit/pending'),
  auditCourse: (id, action) => request.post(`/admin/audit/course/${id}`, null, { params: { action } }),
  auditResource: (id, action) => request.post(`/admin/audit/resource/${id}`, null, { params: { action } })
}

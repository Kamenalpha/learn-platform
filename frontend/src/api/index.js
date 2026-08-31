import request from '../utils/request'

/** 后端接口统一封装 */
export const api = {
  // 认证
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  me: () => request.get('/auth/me'),

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
  deleteUser: (id) => request.delete(`/admin/users/${id}`)
}

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
  docFileUrl: (id, token) => `/api/docs/${id}/file?token=${encodeURIComponent(token)}`,

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

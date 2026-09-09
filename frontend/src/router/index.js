import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/explore', name: 'Explore', component: () => import('../views/Explore.vue') },
  { path: '/news', name: 'News', component: () => import('../views/News.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: 'kb', name: 'Kb', component: () => import('../views/KnowledgeBase.vue'), meta: { title: '我的知识库' } },
      { path: 'assistant', name: 'Assistant', component: () => import('../views/Assistant.vue'), meta: { title: 'AI 助手' } },
      { path: 'plan', name: 'Plan', component: () => import('../views/StudyPlan.vue'), meta: { title: '学习计划' } },
      { path: 'community', name: 'Community', component: () => import('../views/Community.vue'), meta: { title: '社区' } },
      { path: 'analytics', name: 'Analytics', component: () => import('../views/Analytics.vue'), meta: { title: '学习画像' } },
      { path: 'exam', name: 'Exam', component: () => import('../views/Exam.vue'), meta: { title: '出题模拟' } },
      { path: 'project', name: 'Project', component: () => import('../views/Project.vue'), meta: { title: '项目辅导' } },
      { path: 'chat', name: 'Chat', component: () => import('../views/Chat.vue'), meta: { title: '智能问答' } },
      { path: 'graph', name: 'Graph', component: () => import('../views/KnowledgeGraph.vue'), meta: { title: '知识图谱' } },
      { path: 'history', name: 'History', component: () => import('../views/History.vue'), meta: { title: '学习历史' } },
      { path: 'admin/stats', name: 'AdminStats', component: () => import('../views/admin/Stats.vue'), meta: { title: '数据看板', admin: true } },
      { path: 'admin/review', name: 'AdminReview', component: () => import('../views/admin/Review.vue'), meta: { title: '内容审核', admin: true } },
      { path: 'admin/kb', name: 'AdminKb', component: () => import('../views/admin/KbManage.vue'), meta: { title: '知识库管理', admin: true } },
      { path: 'admin/kb-debug', name: 'AdminKbDebug', component: () => import('../views/admin/RagDebug.vue'), meta: { title: '检索测试', admin: true } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue'), meta: { title: '用户管理', admin: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 游客(未登录)也可访问的页面:浏览公开内容即可学习,个性化功能需登录
const GUEST_PATHS = new Set(['/', '/explore', '/news', '/login'])

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!GUEST_PATHS.has(to.path) && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/kb'
  }
  if (to.meta.admin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (userInfo?.role !== 1) {
      ElMessage.error('该页面仅管理员可访问')
      return '/kb'
    }
  }
  return true
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/chat',
    children: [
      { path: 'chat', name: 'Chat', component: () => import('../views/Chat.vue'), meta: { title: '智能问答' } },
      { path: 'courses', name: 'Courses', component: () => import('../views/Courses.vue'), meta: { title: '知识库浏览' } },
      { path: 'docs', name: 'Docs', component: () => import('../views/Documents.vue'), meta: { title: '文档列表' } },
      { path: 'exam', name: 'Exam', component: () => import('../views/ExamPoints.vue'), meta: { title: '考点生成' } },
      { path: 'graph', name: 'Graph', component: () => import('../views/KnowledgeGraph.vue'), meta: { title: '知识图谱' } },
      { path: 'history', name: 'History', component: () => import('../views/History.vue'), meta: { title: '学习历史' } },
      { path: 'admin/stats', name: 'AdminStats', component: () => import('../views/admin/Stats.vue'), meta: { title: '数据看板', admin: true } },
      { path: 'admin/kb', name: 'AdminKb', component: () => import('../views/admin/KbManage.vue'), meta: { title: '知识库管理', admin: true } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue'), meta: { title: '用户管理', admin: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/chat' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/chat'
  }
  if (to.meta.admin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (userInfo?.role !== 1) {
      ElMessage.error('该页面仅管理员可访问')
      return '/chat'
    }
  }
  return true
})

export default router

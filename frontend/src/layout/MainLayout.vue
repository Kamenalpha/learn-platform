<template>
  <el-container style="height: 100%">
    <el-header class="header">
      <div class="header-left">
        <router-link class="logo" to="/">
          <span class="brand-mark">智</span>
          <span class="brand-name">学习平台</span>
        </router-link>
        <nav class="top-nav">
          <el-menu
            mode="horizontal"
            :default-active="menuActive"
            router
            class="top-menu"
            :ellipsis="false"
          >
            <el-sub-menu index="knowledge">
              <template #title>知识中心</template>
              <el-menu-item index="/kb"><el-icon><Collection /></el-icon>我的知识库</el-menu-item>
              <el-menu-item index="/graph"><el-icon><Share /></el-icon>知识图谱</el-menu-item>
              <el-menu-item index="/assistant"><el-icon><ChatDotRound /></el-icon>AI 助手</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="tools">
              <template #title>学习工具</template>
              <el-menu-item index="/plan"><el-icon><Calendar /></el-icon>学习计划</el-menu-item>
              <el-menu-item index="/exam"><el-icon><EditPen /></el-icon>出题模拟</el-menu-item>
              <el-menu-item index="/project"><el-icon><Briefcase /></el-icon>项目辅导</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/community"><el-icon><ChatLineRound /></el-icon>社区</el-menu-item>
            <el-sub-menu index="growth">
              <template #title>个人成长</template>
              <el-menu-item index="/analytics"><el-icon><DataLine /></el-icon>学习画像</el-menu-item>
              <el-menu-item index="/history"><el-icon><Clock /></el-icon>学习历史</el-menu-item>
            </el-sub-menu>
            <el-sub-menu v-if="userStore.isAdmin" index="admin">
              <template #title>系统管理</template>
              <el-menu-item index="/admin/stats"><el-icon><DataAnalysis /></el-icon>数据看板</el-menu-item>
              <el-menu-item index="/admin/kb"><el-icon><Cpu /></el-icon>知识库管理</el-menu-item>
              <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </nav>
      </div>
      <div class="header-right">
        <span class="page-title">{{ $route.meta.title || '' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="30" class="user-avatar">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
            <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            <el-tag size="small" :type="userStore.isAdmin ? 'warning' : 'info'" effect="dark" round>
              {{ userStore.isAdmin ? '管理员' : '学生' }}
            </el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
    <FloatingChat />
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import FloatingChat from '../components/FloatingChat.vue'

const userStore = useUserStore()
const router = useRouter()

// 问答归属社区,进入 /chat 时高亮「社区」
const menuActive = computed(() =>
  String(router.currentRoute.value.path).startsWith('/chat') ? '/community' : router.currentRoute.value.path
)

const onCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(13, 25, 48, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
  height: var(--header-height);
  padding: 0 18px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 40px;
  padding: 0 10px;
  text-decoration: none;
  border-radius: 8px;
  transition: background .25s;
  flex-shrink: 0;
}
.logo:hover {
  background: rgba(245, 241, 232, 0.05);
}
.brand-mark {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  background: var(--amber);
  color: var(--ink);
  font-weight: 800;
  font-size: 16px;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
  box-shadow: 0 0 16px rgba(242, 182, 76, 0.3);
}
.brand-name {
  color: var(--paper);
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.top-nav {
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden;
}
.top-nav::-webkit-scrollbar { height: 4px; }
.top-nav::-webkit-scrollbar-thumb { background: rgba(159, 180, 216, 0.2); }

.top-menu {
  background: transparent;
  border-bottom: none !important;
  --el-menu-item-height: 54px;
  --el-menu-active-color: var(--amber);
}
.top-menu :deep(.el-menu-item),
.top-menu :deep(.el-sub-menu__title) {
  color: var(--mist);
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px 8px 0 0;
  transition: color .25s, background .25s;
}
.top-menu :deep(.el-menu-item:hover),
.top-menu :deep(.el-sub-menu__title:hover) {
  color: var(--paper);
  background: rgba(245, 241, 232, 0.06);
}
.top-menu :deep(.el-menu-item.is-active) {
  color: var(--amber);
  font-weight: 600;
}
.top-menu :deep(.el-sub-menu__icon-arrow) {
  color: var(--mist);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.page-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--paper);
  letter-spacing: 1px;
  opacity: 0.9;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.user-avatar {
  background: rgba(242, 182, 76, 0.18);
  color: var(--amber);
  font-weight: 600;
}
.user-name {
  color: var(--paper);
  font-size: 14px;
}
.user-info :deep(.el-tag) {
  border: none;
}

.main {
  padding: 18px;
  background:
    radial-gradient(ellipse at 85% -10%, rgba(242, 182, 76, 0.05) 0%, rgba(242, 182, 76, 0) 45%),
    var(--content-bg);
}

@media (max-width: 900px) {
  .page-title { display: none; }
  .brand-name { display: none; }
  .user-name { display: none; }
}
</style>

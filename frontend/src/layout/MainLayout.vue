<template>
  <el-container style="height: 100%">
    <el-header class="header">
      <div class="header-left">
        <router-link class="logo" to="/">
          <span class="brand-mark">智</span>
          <span class="brand-name">学习平台</span>
        </router-link>
        <span class="header-divider" aria-hidden="true"></span>
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
            <el-menu-item index="/news"><el-icon><Reading /></el-icon>知识资讯</el-menu-item>
            <el-menu-item index="/community"><el-icon><ChatLineRound /></el-icon>社区</el-menu-item>
            <el-sub-menu index="growth">
              <template #title>个人成长</template>
              <el-menu-item index="/analytics"><el-icon><DataLine /></el-icon>学习画像</el-menu-item>
              <el-menu-item index="/history"><el-icon><Clock /></el-icon>学习历史</el-menu-item>
            </el-sub-menu>
            <el-sub-menu v-if="userStore.isAdmin" index="admin">
              <template #title>系统管理</template>
              <el-menu-item index="/admin/stats"><el-icon><DataAnalysis /></el-icon>数据看板</el-menu-item>
              <el-menu-item index="/admin/review"><el-icon><Stamp /></el-icon>内容审核</el-menu-item>
              <el-menu-item index="/admin/kb"><el-icon><Cpu /></el-icon>知识库管理</el-menu-item>
              <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </nav>
      </div>
      <div class="header-right">
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="30" class="user-avatar">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
            <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            <el-tag size="small" :type="userStore.isAdmin ? 'warning' : 'info'" effect="plain" round>
              {{ userStore.isAdmin ? '管理员' : '学习者' }}
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
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  height: var(--header-height);
  padding: 0 20px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 40px;
  padding: 0 8px;
  text-decoration: none;
  border-radius: var(--radius-ctl);
  transition: opacity .2s;
  flex-shrink: 0;
}
.logo:hover { opacity: .8; }

/* 品牌记号:朱砂印章 */
.brand-mark {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  background: var(--seal);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  box-shadow: inset 0 0 0 1.5px rgba(255, 255, 255, 0.28);
}
.brand-name {
  color: var(--paper);
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--line);
  margin: 0 10px;
  flex-shrink: 0;
}

.top-nav {
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden;
}
.top-nav::-webkit-scrollbar { height: 4px; }
.top-nav::-webkit-scrollbar-thumb { background: var(--line-strong); }

.top-menu {
  background: transparent;
  border-bottom: none !important;
  --el-menu-item-height: var(--header-height);
  --el-menu-active-color: var(--accent-strong);
}
.top-menu :deep(.el-menu-item),
.top-menu :deep(.el-sub-menu__title) {
  color: var(--ink-2);
  font-size: 14px;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  transition: color .15s;
}
.top-menu :deep(.el-menu-item:hover),
.top-menu :deep(.el-sub-menu__title:hover) {
  color: var(--accent-strong);
  background: transparent;
}
.top-menu :deep(.el-menu-item.is-active),
.top-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--accent-strong);
  font-weight: 600;
  border-bottom-color: var(--accent);
}
.top-menu :deep(.el-sub-menu__icon-arrow) { color: var(--ink-3); }

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  border-radius: 999px;
  padding: 3px 6px;
  transition: background .15s;
}
.user-info:hover { background: var(--surface-2); }
.user-avatar {
  background: var(--accent-wash);
  color: var(--accent-strong);
  font-weight: 600;
}
.user-name {
  color: var(--paper);
  font-size: 14px;
}
.user-info :deep(.el-tag) { border: none; }

.main {
  padding: 20px;
  background: var(--content-bg);
}

@media (max-width: 900px) {
  .brand-name { display: none; }
  .user-name { display: none; }
}
</style>

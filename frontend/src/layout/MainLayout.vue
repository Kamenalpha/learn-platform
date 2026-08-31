<template>
  <el-container style="height: 100%">
    <el-aside width="210px" style="background: #001529">
      <div class="logo">📚 智能学习平台</div>
      <el-menu
        :default-active="$route.path"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
        router
        style="border-right: none"
      >
        <el-menu-item index="/kb"><el-icon><Collection /></el-icon>我的知识库</el-menu-item>
        <el-menu-item index="/assistant"><el-icon><ChatDotRound /></el-icon>AI 助手</el-menu-item>
        <el-menu-item index="/plan"><el-icon><Calendar /></el-icon>学习计划</el-menu-item>
        <el-menu-item index="/community"><el-icon><ChatLineRound /></el-icon>社区</el-menu-item>
        <el-menu-item index="/exam"><el-icon><EditPen /></el-icon>出题模拟</el-menu-item>
        <el-menu-item index="/project"><el-icon><Briefcase /></el-icon>项目辅导</el-menu-item>
        <el-menu-item index="/analytics"><el-icon><DataLine /></el-icon>学习画像</el-menu-item>
        <el-menu-item index="/chat"><el-icon><MagicStick /></el-icon>智能问答</el-menu-item>
        <el-menu-item index="/graph"><el-icon><Share /></el-icon>知识图谱</el-menu-item>
        <el-menu-item index="/history"><el-icon><Clock /></el-icon>学习历史</el-menu-item>
        <template v-if="userStore.isAdmin">
          <div class="menu-group">管理员后台</div>
          <el-menu-item index="/admin/stats"><el-icon><DataAnalysis /></el-icon>数据看板</el-menu-item>
          <el-menu-item index="/admin/kb"><el-icon><Cpu /></el-icon>知识库管理</el-menu-item>
          <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || '' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="28" style="margin-right: 8px">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
            ({{ userStore.isAdmin ? '管理员' : '学生' }})
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="padding: 16px; background: #f5f7fa">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const router = useRouter()

const onCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.menu-group {
  color: #5b6b7d;
  font-size: 12px;
  padding: 14px 20px 6px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 56px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
  outline: none;
}
</style>

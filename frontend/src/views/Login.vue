<template>
  <div class="login-bg">
    <div class="bg-glow" aria-hidden="true"></div>
    <svg class="bg-graph" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <g stroke="rgba(159,180,216,0.14)" stroke-width="1">
        <line x1="120" y1="110" x2="300" y2="200" />
        <line x1="300" y1="200" x2="520" y2="150" />
        <line x1="520" y1="150" x2="660" y2="280" />
        <line x1="300" y1="200" x2="260" y2="420" />
        <line x1="660" y1="280" x2="500" y2="450" />
        <line x1="260" y1="420" x2="500" y2="450" />
        <line x1="120" y1="110" x2="260" y2="420" />
        <line x1="520" y1="150" x2="500" y2="450" />
        <line x1="660" y1="280" x2="180" y2="330" />
        <line x1="180" y1="330" x2="260" y2="420" />
        <line x1="300" y1="200" x2="180" y2="330" />
      </g>
      <g fill="rgba(245,241,232,0.07)" stroke="rgba(242,182,76,0.3)" stroke-width="1.2">
        <circle cx="120" cy="110" r="14" />
        <circle cx="300" cy="200" r="18" />
        <circle cx="520" cy="150" r="12" />
        <circle cx="660" cy="280" r="15" />
        <circle cx="260" cy="420" r="13" />
        <circle cx="500" cy="450" r="11" />
        <circle cx="180" cy="330" r="10" />
      </g>
      <g fill="rgba(242,182,76,0.5)" font-size="11" font-weight="700" font-family="ui-monospace,monospace">
        <text x="420" y="90">[1]</text>
        <text x="130" y="250">[2]</text>
        <text x="600" y="450">[3]</text>
      </g>
    </svg>

    <el-card class="login-card">
      <div class="brand">
        <span class="brand-mark">智</span>
        <div class="brand-text">
          <h2>学习平台</h2>
          <p>基于 RAG 的智能学习平台</p>
        </div>
      </div>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @keyup.enter="doLogin">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" size="large">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" size="large">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
              登 录
            </el-button>
            <p class="tip">默认账号:admin / admin123(管理员),student / 123456(学生)</p>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="用户名(3-20位)" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.nickname" placeholder="昵称(选填)" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.password" type="password" show-password placeholder="密码(6-32位)" size="large" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doRegister">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

watch(() => route.query.tab, (value) => {
  tab.value = value === 'register' ? 'register' : 'login'
}, { immediate: true })

const doLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await api.login({ ...loginForm })
    userStore.setLogin(data.token, {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      role: data.role
    })
    ElMessage.success('登录成功')
    router.push('/chat')
  } finally {
    loading.value = false
  }
}

const doRegister = async () => {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await api.register({ ...regForm })
    ElMessage.success('注册成功,请登录')
    loginForm.username = regForm.username
    tab.value = 'login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 78% 30%, rgba(242, 182, 76, 0.08) 0%, rgba(242, 182, 76, 0) 46%),
    linear-gradient(180deg, #0d1f42 0%, #0b1830 55%, #0a1528 100%);
}

.bg-glow {
  position: absolute;
  width: 460px;
  height: 460px;
  right: -120px;
  top: -120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(242, 182, 76, 0.14) 0%, rgba(242, 182, 76, 0) 66%);
}

.bg-graph {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  opacity: 0.9;
}

.login-card {
  width: 400px;
  border-radius: 14px;
  position: relative;
  z-index: 1;
  background: rgba(14, 28, 57, 0.72) !important;
  backdrop-filter: blur(14px);
  border: 1px solid var(--line) !important;
  box-shadow: 0 24px 60px rgba(0, 6, 22, 0.6);
  padding: 8px 6px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 6px 0 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}
.brand-mark {
  width: 46px;
  height: 46px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: var(--amber);
  color: var(--ink);
  font-weight: 800;
  font-size: 24px;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
  box-shadow: 0 0 24px rgba(242, 182, 76, 0.35);
}
.brand-text h2 {
  margin: 0;
  font-size: 21px;
  color: var(--paper);
  letter-spacing: 2px;
}
.brand-text p {
  margin: 5px 0 0;
  color: var(--mist);
  font-size: 12.5px;
  letter-spacing: .5px;
}

.login-card :deep(.el-tabs__item) {
  color: var(--mist);
}
.login-card :deep(.el-tabs__item.is-active) {
  color: var(--amber);
}
.login-card :deep(.el-tabs__active-bar) {
  background-color: var(--amber);
}

.tip {
  color: var(--mist);
  font-size: 12px;
  text-align: center;
  margin: 14px 0 0;
}
</style>

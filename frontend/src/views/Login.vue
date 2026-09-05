<template>
  <div class="login-page">
    <!-- 左:品牌面板 -->
    <aside class="brand-panel">
      <router-link class="brand" to="/">
        <span class="brand-mark">智</span>
        <span class="brand-name">学习平台</span>
      </router-link>
      <div class="brand-body">
        <h1>让每一次回答，<br>都有出处可循。</h1>
        <p>基于 RAG 的智能学习平台：上传你的教材，AI 的回答附带可追溯的原文引用，学习有据可依。</p>
        <ul class="brand-points">
          <li><span class="point-dot"></span>资料入库，按课程与知识点整理</li>
          <li><span class="point-dot"></span>问答溯源，引用直达教材原文</li>
          <li><span class="point-dot"></span>计划、测验与画像，记录成长</li>
        </ul>
      </div>
      <p class="brand-foot">纸页 · 靛青 — 像阅读一本自己的书</p>
    </aside>

    <!-- 右:表单 -->
    <main class="form-panel">
      <div class="form-box">
        <div class="form-switch" role="tablist">
          <button
            v-for="t in [['login', '登录'], ['register', '注册']]"
            :key="t[0]"
            type="button"
            class="switch-btn"
            :class="{ active: tab === t[0] }"
            role="tab"
            :aria-selected="tab === t[0]"
            @click="tab = t[0]"
          >{{ t[1] }}</button>
        </div>

        <form v-if="tab === 'login'" class="form" @submit.prevent="doLogin">
          <div class="field">
            <label for="login-username">用户名</label>
            <el-input id="login-username" v-model="loginForm.username" placeholder="输入用户名" size="large" autocomplete="username" />
          </div>
          <div class="field">
            <label for="login-password">密码</label>
            <el-input id="login-password" v-model="loginForm.password" type="password" show-password placeholder="输入密码" size="large" autocomplete="current-password" />
          </div>
          <el-button type="primary" size="large" class="submit" native-type="submit" :loading="loading">
            登 录
          </el-button>
          <p class="tip">演示账号：admin / admin123（管理员）· student / 123456（学生）</p>
        </form>

        <form v-else class="form" @submit.prevent="doRegister">
          <div class="field">
            <label for="reg-username">用户名</label>
            <el-input id="reg-username" v-model="regForm.username" placeholder="3-20 位字符" size="large" autocomplete="username" />
          </div>
          <div class="field">
            <label for="reg-nickname">昵称<span class="optional">（选填）</span></label>
            <el-input id="reg-nickname" v-model="regForm.nickname" placeholder="将显示在社区与问答中" size="large" />
          </div>
          <div class="field">
            <label for="reg-password">密码</label>
            <el-input id="reg-password" v-model="regForm.password" type="password" show-password placeholder="6-32 位" size="large" autocomplete="new-password" />
          </div>
          <el-button type="primary" size="large" class="submit" native-type="submit" :loading="loading">
            注 册
          </el-button>
        </form>
      </div>
    </main>
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
    ElMessage.success('注册成功，请登录')
    loginForm.username = regForm.username
    tab.value = 'login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(380px, 46%) 1fr;
  background: var(--content-bg);
}

/* —— 左:品牌面板(墨色,整页唯一的沉浸色面) —— */
.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 40px 48px;
  background:
    radial-gradient(ellipse 90% 60% at 110% 110%, rgba(47, 79, 208, 0.22) 0%, rgba(47, 79, 208, 0) 60%),
    #141d30;
  color: #eef1f7;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  text-decoration: none;
  width: fit-content;
}
.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--seal);
  color: #fff;
  font-weight: 700;
  font-size: 19px;
  box-shadow: inset 0 0 0 1.5px rgba(255, 255, 255, 0.28);
}
.brand-name { font-weight: 700; font-size: 17px; }

.brand-body { margin: auto 0; max-width: 420px; }
.brand-body h1 {
  margin: 0 0 18px;
  font-size: clamp(30px, 2.8vw, 40px);
  line-height: 1.35;
  letter-spacing: .01em;
  color: #fff;
}
.brand-body > p {
  margin: 0;
  color: #aeb9d2;
  line-height: 1.9;
  font-size: 15px;
}
.brand-points {
  list-style: none;
  margin: 34px 0 0;
  padding: 0;
  display: grid;
  gap: 14px;
}
.brand-points li {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #ccd4e6;
  font-size: 14px;
}
.point-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--accent);
  box-shadow: 0 0 0 3px rgba(47, 79, 208, 0.25);
}
.brand-foot {
  margin: 0;
  color: #5f6c8c;
  font-size: 12.5px;
  letter-spacing: .08em;
}

/* —— 右:表单 —— */
.form-panel {
  display: grid;
  place-items: center;
  padding: 40px 24px;
}
.form-box { width: min(380px, 100%); }

.form-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 4px;
  margin-bottom: 30px;
  background: var(--surface-2);
  border-radius: 10px;
}
.switch-btn {
  height: 38px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-2);
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s, color .15s, box-shadow .15s;
}
.switch-btn.active {
  background: var(--surface);
  color: var(--accent-strong);
  box-shadow: 0 1px 3px rgba(28, 37, 52, 0.1);
}

.form { display: grid; gap: 18px; }
.field { display: grid; gap: 7px; }
.field label {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--paper);
}
.field .optional { color: var(--ink-3); font-weight: 400; }

.submit { width: 100%; margin-top: 6px; }
.tip {
  margin: 4px 0 0;
  color: var(--ink-3);
  font-size: 12.5px;
  text-align: center;
  line-height: 1.7;
}

/* —— 响应式 —— */
@media (max-width: 860px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
}
</style>

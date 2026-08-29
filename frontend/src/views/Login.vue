<template>
  <div class="login-bg">
    <el-card class="login-card">
      <h2 style="text-align: center; margin: 8px 0 4px">数媒知识库智能问答系统</h2>
      <p style="text-align: center; color: #909399; font-size: 13px; margin-bottom: 20px">
        基于 RAG 的数字媒体专业知识库问答平台
      </p>
      <el-tabs v-model="tab">
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
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

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
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6feb 0%, #6db3f2 100%);
}

.login-card {
  width: 380px;
  border-radius: 10px;
}

.tip {
  color: #909399;
  font-size: 12px;
  text-align: center;
  margin: 12px 0 0;
}
</style>

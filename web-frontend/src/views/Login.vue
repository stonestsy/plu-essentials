<template>
  <div class="login-wrap">
    <el-card class="login-box" shadow="always">
      <h1 class="title">PluEssentials</h1>
      <p class="subtitle">服务器管理后台</p>
      <el-input v-model="pwd" type="password" placeholder="输入管理密码" show-password @keyup.enter="login" />
      <el-button type="primary" style="width:100%;margin-top:16px" @click="login" :loading="loading">登 录</el-button>
      <p v-if="err" class="err">{{ err }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const pwd = ref('')
const loading = ref(false)
const err = ref('')
const router = useRouter()

async function login() {
  if (!pwd.value) { err.value = '请输入管理密码'; return }
  loading.value = true
  err.value = ''
  try {
    const d = await api.post('/api/login', { password: pwd.value })
    if (d.token) {
      localStorage.setItem('plu_token', d.token)
      router.push('/dashboard')
    } else {
      err.value = d.error || '登录失败'
    }
  } catch (e) {
    if (e.response?.status === 401) {
      err.value = e.response?.data?.error || '密码错误'
    } else if (!e.response) {
      err.value = '无法连接到服务器，请确认后台服务已启动'
    } else {
      err.value = e.response?.data?.error || '登录失败'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(circle at 30% 20%, #1a2332, #0f1419 70%); }
.login-box { width: 360px; border-radius: 16px; }
.title { font-size: 24px; margin-bottom: 6px; background: linear-gradient(90deg, #58a6ff, #bc8cff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.subtitle { color: #8b98a9; font-size: 13px; margin-bottom: 20px; }
.err { color: #f85149; margin-top: 12px; font-size: 13px; min-height: 18px; }
</style>

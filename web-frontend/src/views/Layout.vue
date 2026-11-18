<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">PluEssentials</div>
      <el-menu :default-active="route.path" router class="menu" background-color="#161b22" text-color="#8b98a9" active-text-color="#58a6ff">
        <el-menu-item v-for="r in menuRoutes" :key="r.path" :index="'/' + r.path">
          <el-icon><component :is="r.meta.icon" /></el-icon>
          <span>{{ r.meta.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-bottom">
        <el-button size="small" @click="logout" plain>退出登录</el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <h2>{{ currentTitle }}</h2>
        <div class="header-right">
          <el-tag :type="tpsType">TPS: {{ tps }}</el-tag>
          <span class="online">在线: {{ online }}</span>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()
const menuRoutes = router.options.routes[1].children
const currentTitle = computed(() => route.meta?.title || '')
const tps = ref('--')
const online = ref('--')
const tpsType = computed(() => {
  const v = parseFloat(tps.value)
  if (isNaN(v)) return 'info'
  if (v >= 18) return 'success'
  if (v >= 15) return 'warning'
  return 'danger'
})

let timer
async function loadTopbar() {
  try {
    const d = await api.get('/api/dashboard')
    tps.value = d.tps
    online.value = d.online + '/' + d.maxPlayers
  } catch {}
}
onMounted(() => { loadTopbar(); timer = setInterval(loadTopbar, 3000) })
onUnmounted(() => clearInterval(timer))

function logout() {
  api.post('/api/logout').catch(() => {})
  localStorage.removeItem('plu_token')
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #161b22; display: flex; flex-direction: column; }
.logo { padding: 20px; font-size: 18px; font-weight: 700; background: linear-gradient(90deg, #58a6ff, #bc8cff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.menu { flex: 1; border-right: none; }
.aside-bottom { padding: 12px; border-top: 1px solid #2a3344; }
.header { display: flex; align-items: center; justify-content: space-between; background: #161b22; border-bottom: 1px solid #2a3344; }
.header h2 { margin: 0; font-size: 16px; }
.header-right { display: flex; align-items: center; gap: 12px; font-size: 13px; color: #8b98a9; }
.online { color: #8b98a9; }
.main { background: #0f1419; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

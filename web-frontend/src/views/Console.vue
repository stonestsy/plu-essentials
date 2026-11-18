<template>
  <el-card shadow="never">
    <div class="console" ref="consoleRef">
      <div v-for="(line, i) in lines" :key="i" class="line">{{ line }}</div>
    </div>
    <div style="display:flex;gap:8px;margin-top:12px">
      <el-input v-model="cmd" placeholder="输入命令 (无需 / 前缀)" @keyup.enter="send" />
      <el-button type="primary" @click="send">执行</el-button>
      <el-button @click="lines=[]">清空</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import api from '../api'

const lines = ref([])
const cmd = ref('')
const consoleRef = ref(null)
let timer

onMounted(() => { load(); timer = setInterval(load, 2000) })
onUnmounted(() => clearInterval(timer))

async function load() {
  const d = await api.get('/api/console?lines=500')
  lines.value = d.lines || []
  nextTick(() => { if (consoleRef.value) consoleRef.value.scrollTop = consoleRef.value.scrollHeight })
}

async function send() {
  if (!cmd.value.trim()) return
  const c = cmd.value.trim()
  cmd.value = ''
  await api.post('/api/console', { command: c })
  setTimeout(load, 300)
}
</script>

<style scoped>
.console { background: #000; border-radius: 6px; padding: 12px; height: calc(100vh - 280px); overflow-y: auto; font-family: Consolas, monospace; font-size: 12px; line-height: 1.5; }
.line { color: #c9d1d9; white-space: pre-wrap; word-break: break-all; }
</style>

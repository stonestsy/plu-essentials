<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>聊天记录 ({{ logs.length }})</b>
        <div>
          <el-input v-model="keyword" placeholder="按玩家名或消息过滤" size="small" style="width:220px;margin-right:8px" clearable />
          <el-input-number v-model="limit" :min="1" :max="500" size="small" style="width:110px;margin-right:8px" />
          <el-button type="primary" size="small" @click="load">查询</el-button>
        </div>
      </div>
    </template>
    <el-table :data="logs" stripe size="small" max-height="600">
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.time) }}</template>
      </el-table-column>
      <el-table-column prop="name" label="玩家" width="140" />
      <el-table-column prop="world" label="世界" width="120" />
      <el-table-column prop="message" label="消息" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const logs = ref([])
const keyword = ref('')
const limit = ref(100)

onMounted(load)

async function load() {
  let url = '/api/chat?limit=' + limit.value
  if (keyword.value) url += '&player=' + encodeURIComponent(keyword.value)
  const d = await api.get(url)
  logs.value = d.logs || []
}

function fmtTime(ts) {
  const d = new Date(ts)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
</script>

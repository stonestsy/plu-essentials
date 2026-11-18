<template>
  <el-card shadow="never">
    <template #header><b>世界列表</b></template>
    <el-table :data="worlds" stripe size="small">
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="environment" label="环境" width="120" />
      <el-table-column prop="players" label="玩家" width="70" />
      <el-table-column prop="time" label="时间" width="90" />
      <el-table-column label="天气" width="90">
        <template #default="{ row }">
          <el-tag :type="row.weather==='clear'?'success':'primary'">{{ row.weather }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="90" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="act(row.name,'day')">白天</el-button>
          <el-button size="small" @click="act(row.name,'night')">夜晚</el-button>
          <el-button size="small" @click="act(row.name,'weather','clear')">晴</el-button>
          <el-button size="small" @click="act(row.name,'weather','rain')">雨</el-button>
          <el-button size="small" @click="act(row.name,'weather','thunder')">雷</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const worlds = ref([])
onMounted(load)

async function load() {
  const d = await api.get('/api/worlds')
  worlds.value = d.worlds || []
}
async function act(name, a, value) {
  await api.post('/api/worlds/' + name + '/' + a, { value })
  ElMessage.success('操作成功')
  load()
}
</script>

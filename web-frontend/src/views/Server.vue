<template>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card shadow="never">
        <template #header><b>广播消息</b></template>
        <el-input v-model="msg" placeholder="输入广播内容 (支持 & 颜色码)" type="textarea" :rows="3" />
        <el-button type="primary" style="margin-top:12px" @click="broadcast">发送广播</el-button>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="never">
        <template #header><b>服务器控制</b></template>
        <p style="color:#8b98a9;margin-bottom:16px">以下操作将影响整个服务器，请谨慎操作。</p>
        <el-button type="warning" @click="reload">重载服务器 (reload)</el-button>
        <el-button type="danger" @click="stop(0)">立即关闭</el-button>
        <el-button type="danger" plain @click="stop(5)">5秒后关闭</el-button>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const msg = ref('')

async function broadcast() {
  if (!msg.value) return
  await api.post('/api/broadcast', { message: msg.value })
  ElMessage.success('广播已发送')
  msg.value = ''
}

async function reload() {
  await ElMessageBox.confirm('确定重载服务器? 可能造成卡顿。', '警告', { type: 'warning' })
  await api.post('/api/server/reload')
  ElMessage.success('已发送重载指令')
}

async function stop(delay) {
  await ElMessageBox.confirm('确定关闭服务器?', '警告', { type: 'warning' })
  await api.post('/api/server/stop', { delay })
  ElMessage.success('已发送关闭指令')
}
</script>

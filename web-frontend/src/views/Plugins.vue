<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>已安装插件 ({{ plugins.length }})</b>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="plugins" stripe size="small">
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="version" label="版本" width="100" />
      <el-table-column prop="authors" label="作者" width="150">
        <template #default="{ row }">{{ (row.authors || []).join(', ') }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '运行中' : '已停止' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" type="success" :disabled="row.enabled" @click="act(row.name,'enable')">启用</el-button>
          <el-button size="small" type="warning" @click="act(row.name,'reload')">重载</el-button>
          <el-button size="small" type="danger" :disabled="!row.enabled" @click="act(row.name,'disable')">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const plugins = ref([])
onMounted(load)

async function load() {
  const d = await api.get('/api/plugins')
  plugins.value = d.plugins || []
}
async function act(name, a) {
  await api.post('/api/plugins/' + name + '/' + a)
  ElMessage.success('操作成功')
  load()
}
</script>

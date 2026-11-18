<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>领地列表 ({{ claims.length }})</b>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="claims" stripe size="small">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="ownerName" label="归属玩家" width="150" />
      <el-table-column prop="world" label="世界" width="130" />
      <el-table-column label="范围" width="230">
        <template #default="{ row }">
          ({{ row.minX }}, {{ row.minZ }}) ~ ({{ row.maxX }}, {{ row.maxZ }})
        </template>
      </el-table-column>
      <el-table-column prop="area" label="面积" width="100" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const claims = ref([])

onMounted(load)

async function load() {
  const d = await api.get('/api/claims')
  claims.value = d.claims || []
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除领地 #' + id + ' ?', '提示', { type: 'warning' })
  await api.delete('/api/claims/' + id)
  ElMessage.success('已删除')
  load()
}
</script>

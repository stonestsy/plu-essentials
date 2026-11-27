<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>玩家市场上架 ({{ listings.length }})</b>
        <el-tag :type="available ? 'success' : 'danger'">
          {{ available ? '经济系统正常' : '经济系统已关闭（economy.enabled）' }}
        </el-tag>
      </div>
    </template>
    <el-table :data="listings" stripe size="small">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="sellerName" label="卖家" width="150" />
      <el-table-column label="物品">
        <template #default="{ row }">
          {{ row.displayName || row.type }} <span style="color:#8b98a9">x{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="120">
        <template #default="{ row }">{{ row.price.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="上架时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.created) }}</template>
      </el-table-column>
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

const listings = ref([])
const available = ref(false)

onMounted(load)

async function load() {
  const d = await api.get('/api/market')
  listings.value = d.listings || []
  available.value = !!d.available
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除上架 #' + id + ' ?（物品不会退回给卖家）', '提示', { type: 'warning' })
  await api.delete('/api/market/' + id)
  ElMessage.success('已删除')
  load()
}

function fmtTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

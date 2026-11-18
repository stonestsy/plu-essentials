<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>登录账号 ({{ accounts.length }})</b>
        <el-tag :type="enabled ? 'success' : 'info'">
          {{ enabled ? '登录系统已启用' : '登录系统未启用' }}
        </el-tag>
      </div>
    </template>
    <el-table :data="accounts" stripe size="small">
      <el-table-column prop="name" label="玩家" width="160" />
      <el-table-column prop="uuid" label="UUID" />
      <el-table-column label="状态" width="160">
        <template #default="{ row }">
          <el-tag v-if="row.online" :type="row.authed ? 'success' : 'warning'" size="small">
            {{ row.authed ? '在线已登录' : '在线未登录' }}
          </el-tag>
          <el-tag v-else type="info" size="small">离线</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const accounts = ref([])
const enabled = ref(false)

onMounted(load)

async function load() {
  const d = await api.get('/api/accounts')
  accounts.value = d.accounts || []
  enabled.value = !!d.enabled
}
</script>

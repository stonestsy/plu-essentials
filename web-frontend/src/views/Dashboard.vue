<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="hover">
          <div class="stat">
            <el-icon :size="28" :color="c.color" style="float:right;opacity:.5"><component :is="c.icon" /></el-icon>
            <div class="stat-label">{{ c.label }}</div>
            <div class="stat-value" :style="{color: c.color}">{{ c.value }}</div>
            <div class="stat-sub" v-if="c.sub">{{ c.sub }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><b>服务器信息</b></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="版本">{{ info.version }}</el-descriptions-item>
            <el-descriptions-item label="Bukkit">{{ info.bukkitVersion }}</el-descriptions-item>
            <el-descriptions-item label="MOTD">{{ info.motd }}</el-descriptions-item>
            <el-descriptions-item label="插件">{{ info.pluginName }} v{{ info.pluginVersion }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><b>联动插件状态</b></template>
          <el-table :data="hooks" size="small">
            <el-table-column prop="name" label="插件" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="row.ok ? 'success' : 'info'">{{ row.ok ? '已加载' : '未安装' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '../api'

const info = reactive({})
const hooks = ref([])
const cards = ref([])

onMounted(async () => {
  const d = await api.get('/api/dashboard')
  Object.assign(info, d)
  const upDays = Math.floor(d.uptimeMs / 86400000)
  const upHours = Math.floor((d.uptimeMs % 86400000) / 3600000)
  const upMins = Math.floor((d.uptimeMs % 3600000) / 60000)
  const tpsColor = d.tps >= 18 ? '#7ee787' : d.tps >= 15 ? '#d29922' : '#f85149'
  cards.value = [
    { label: 'TPS', value: d.tps, icon: 'Odometer', color: tpsColor },
    { label: '在线玩家', value: d.online + '/' + d.maxPlayers, icon: 'User', color: '#58a6ff' },
    { label: '内存使用', value: d.memoryUsedMb + 'MB', sub: d.memoryPercent + '% of ' + d.memoryMaxMb + 'MB', icon: 'Cpu', color: '#bc8cff' },
    { label: '运行时间', value: upDays + '天' + upHours + '时' + upMins + '分', icon: 'Clock', color: '#7ee787' }
  ]
  hooks.value = [
    { name: d.vaultEnabled ? 'Vault 经济' : (d.vaultInstalled ? 'Vault 已安装（缺少经济插件）' : 'Vault 经济（未安装）'), ok: d.vaultEnabled },
    { name: 'LuckPerms', ok: d.luckpermsEnabled }
  ]
})
</script>

<style scoped>
.stat-label { color: #8b98a9; font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 700; margin-top: 6px; }
.stat-sub { font-size: 12px; color: #8b98a9; }
</style>

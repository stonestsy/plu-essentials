<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>在线玩家 ({{ players.length }})</b>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="players" stripe size="small" style="width:100%">
      <el-table-column prop="name" label="玩家" width="140" />
      <el-table-column label="状态" width="200">
        <template #default="{ row }">
          <el-tag size="small" type="primary">{{ row.gamemode }}</el-tag>
          <el-tag v-if="row.flying" size="small" type="warning">飞行</el-tag>
          <el-tag v-if="row.god" size="small" type="danger">神</el-tag>
          <el-tag v-if="row.vanished" size="small" type="info">隐身</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生命/饥饿" width="110">
        <template #default="{ row }">❤{{ row.health }} 🍗{{ row.food }}</template>
      </el-table-column>
      <el-table-column prop="level" label="等级" width="70" />
      <el-table-column prop="primaryGroup" label="权限组" width="100" />
      <el-table-column prop="balance" label="余额" width="90">
        <template #default="{ row }">{{ row.balance !== undefined ? '$' + row.balance : '-' }}</template>
      </el-table-column>
      <el-table-column prop="world" label="世界" width="120" />
      <el-table-column prop="ping" label="延迟" width="70" />
      <el-table-column label="操作" width="380">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="action(row.name,'heal')">治疗</el-button>
          <el-button size="small" type="primary" @click="action(row.name,'feed')">喂食</el-button>
          <el-button size="small" @click="action(row.name,'fly')">飞行</el-button>
          <el-button size="small" @click="action(row.name,'god')">神</el-button>
          <el-button size="small" type="warning" @click="gm(row.name)">模式</el-button>
          <el-button size="small" @click="msg(row.name)">消息</el-button>
          <el-button size="small" @click="inv(row.name)">背包</el-button>
          <el-button size="small" type="danger" @click="banPlayer(row.name)">封禁</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="invShow" title="玩家背包" width="600px">
    <el-table :data="invItems" size="small" max-height="400">
      <el-table-column prop="slot" label="槽位" width="60" />
      <el-table-column prop="type" label="物品" />
      <el-table-column prop="amount" label="数量" width="70" />
      <el-table-column prop="name" label="名称" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const players = ref([])
const invShow = ref(false)
const invItems = ref([])

onMounted(load)

async function load() {
  const d = await api.get('/api/players')
  players.value = d.players || []
}

async function action(name, act, body = {}) {
  await api.post('/api/players/' + name + '/' + act, body)
  ElMessage.success('操作成功')
  load()
}

async function gm(name) {
  try {
    const { value } = await ElMessageBox.prompt('输入游戏模式 (survival/creative/adventure/spectator)', '切换模式', { inputValue: 'survival' })
    await action(name, 'gamemode', { value })
  } catch {}
}

async function msg(name) {
  try {
    const { value } = await ElMessageBox.prompt('发送消息给 ' + name, '私聊', { inputValue: '' })
    await action(name, 'message', { value })
  } catch {}
}

async function inv(name) {
  const d = await api.post('/api/players/' + name + '/inventory')
  invItems.value = d.inventory || []
  invShow.value = true
}

async function banPlayer(name) {
  try {
    const { value } = await ElMessageBox.prompt('封禁原因 (留空默认)', '封禁 ' + name, { inputValue: '' })
    await api.post('/api/bans', { name, reason: value || '违反服务器规则', duration: '' })
    ElMessage.success('已封禁 ' + name)
    load()
  } catch {}
}
</script>

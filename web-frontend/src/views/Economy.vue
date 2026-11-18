<template>
  <el-card shadow="never">
    <template #header><b>经济管理 (Vault)</b></template>
    <el-input v-model="name" placeholder="玩家名" style="width:200px" />
    <el-button type="primary" style="margin-left:8px" @click="lookup">查询</el-button>
    <div v-if="result" class="result">
      <el-descriptions :column="1" border style="margin-top:16px">
        <el-descriptions-item label="玩家">{{ result.player }}</el-descriptions-item>
        <el-descriptions-item label="余额">
          <span style="color:#7ee787;font-size:20px;font-weight:700">{{ result.balance }}</span> {{ result.currency }}
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:12px;display:flex;gap:8px">
        <el-input-number v-model="amount" :min="0" :controls="false" placeholder="金额" />
        <el-button type="success" @click="op('add')">增加</el-button>
        <el-button type="warning" @click="op('remove')">扣除</el-button>
        <el-button type="primary" @click="op('set')">设置</el-button>
      </div>
    </div>
    <el-alert v-else-if="!vault" type="info" style="margin-top:16px">未检测到 Vault 经济插件，请安装 Vault 及经济插件以启用此功能。</el-alert>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const name = ref('')
const amount = ref(0)
const result = ref(null)
const vault = ref(false)

onMounted(async () => {
  const d = await api.get('/api/dashboard')
  vault.value = d.vaultEnabled
})

async function lookup() {
  if (!name.value) return
  result.value = await api.get('/api/economy/' + name.value)
}

async function op(o) {
  await api.post('/api/economy/' + name.value + '/' + o, { amount: amount.value })
  ElMessage.success('操作成功')
  lookup()
}
</script>

<style scoped>
.result { margin-top: 12px; }
</style>

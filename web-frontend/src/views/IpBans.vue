<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>IP 封禁列表 ({{ bans.length }})</b>
        <el-button type="primary" size="small" @click="showAdd = true">封禁 IP</el-button>
      </div>
    </template>
    <el-table :data="bans" stripe size="small">
      <el-table-column prop="ip" label="IP" width="160" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="banner" label="执行者" width="120" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="unban(row.ip)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showAdd" title="封禁 IP" width="400px">
    <el-form label-width="80px">
      <el-form-item label="IP"><el-input v-model="form.ip" placeholder="1.2.3.4" /></el-form-item>
      <el-form-item label="原因"><el-input v-model="form.reason" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAdd=false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const bans = ref([])
const showAdd = ref(false)
const form = reactive({ ip: '', reason: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/ipbans')
  bans.value = d.bans || []
}

async function submit() {
  if (!form.ip) { ElMessage.warning('请输入 IP'); return }
  await api.post('/api/ipbans', form)
  ElMessage.success('封禁成功')
  showAdd.value = false
  form.ip = form.reason = ''
  load()
}

async function unban(ip) {
  await ElMessageBox.confirm('确定解封 ' + ip + ' ?', '提示', { type: 'warning' })
  await api.delete('/api/ipbans/' + ip)
  ElMessage.success('已解封')
  load()
}
</script>

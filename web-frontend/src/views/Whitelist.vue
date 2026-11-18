<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>白名单管理 ({{ list.length }})</b>
        <div>
          <el-switch v-model="enabled" @change="toggle" active-text="已启用" inactive-text="已关闭" />
          <el-button type="primary" size="small" style="margin-left:12px" @click="showAdd = true">添加玩家</el-button>
        </div>
      </div>
    </template>
    <el-table :data="list" stripe size="small">
      <el-table-column prop="name" label="玩家" width="160" />
      <el-table-column prop="uuid" label="UUID" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row.uuid, row.name)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showAdd" title="添加到白名单" width="380px">
    <el-form label-width="80px">
      <el-form-item label="玩家名"><el-input v-model="name" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAdd=false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const list = ref([])
const enabled = ref(false)
const showAdd = ref(false)
const name = ref('')

onMounted(load)

async function load() {
  const d = await api.get('/api/whitelist')
  list.value = d.list || []
  enabled.value = d.enabled
}

async function toggle(val) {
  await api.post('/api/whitelist', { enabled: val })
  ElMessage.success(val ? '白名单已启用' : '白名单已关闭')
}

async function submit() {
  if (!name.value) { ElMessage.warning('请输入玩家名'); return }
  // resolve uuid via offline player — use name as placeholder; backend resolves
  const enc = encodeURIComponent(name.value)
  await api.post('/api/whitelist/' + enc, { name: name.value })
  ElMessage.success('已添加')
  showAdd.value = false
  name.value = ''
  load()
}

async function remove(uuid, n) {
  await ElMessageBox.confirm('确定移除 ' + n + ' ?', '提示', { type: 'warning' })
  await api.delete('/api/whitelist/' + uuid)
  ElMessage.success('已移除')
  load()
}
</script>

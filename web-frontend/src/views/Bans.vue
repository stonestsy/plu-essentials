<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>封禁列表 ({{ bans.length }})</b>
        <el-button type="primary" size="small" @click="showAdd = true">新增封禁</el-button>
      </div>
    </template>
    <el-table :data="bans" stripe size="small">
      <el-table-column prop="name" label="玩家" width="140" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="banner" label="执行者" width="120" />
      <el-table-column label="剩余时间" width="140">
        <template #default="{ row }">
          <el-tag :type="row.permanent ? 'danger' : 'warning'">
            {{ row.permanent ? '永久' : fmt(row.remaining) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="unban(row.uuid)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showAdd" title="新增封禁" width="400px">
    <el-form label-width="80px">
      <el-form-item label="玩家"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="原因"><el-input v-model="form.reason" /></el-form-item>
      <el-form-item label="时长">
        <el-input v-model="form.duration" placeholder="如 1d, 2h, 30m, 留空=永久" />
      </el-form-item>
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
const form = reactive({ name: '', reason: '', duration: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/bans')
  bans.value = d.bans || []
}

async function submit() {
  if (!form.name) { ElMessage.warning('请输入玩家名'); return }
  await api.post('/api/bans', form)
  ElMessage.success('封禁成功')
  showAdd.value = false
  form.name = form.reason = form.duration = ''
  load()
}

async function unban(uuid) {
  await ElMessageBox.confirm('确定解封?', '提示', { type: 'warning' })
  await api.delete('/api/bans/' + uuid)
  ElMessage.success('已解封')
  load()
}

function fmt(ms) {
  if (!ms || ms <= 0) return '已过期'
  const s = Math.floor(ms / 1000)
  const d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600), m = Math.floor((s % 3600) / 60)
  let r = ''
  if (d) r += d + '天'
  if (h) r += h + '时'
  if (m) r += m + '分'
  return r || Math.floor(s) + '秒'
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div>
          <b>定时任务 ({{ tasks.length }})</b>
          <el-tag size="small" style="margin-left:8px">Cron: 分 时 日 月 周</el-tag>
        </div>
        <el-button type="primary" size="small" @click="openAdd">新增任务</el-button>
      </div>
    </template>
    <el-table :data="tasks" stripe size="small">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="command" label="命令" />
      <el-table-column prop="cron" label="Cron" width="140" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="update(row)" />
        </template>
      </el-table-column>
      <el-table-column label="上次执行" width="170">
        <template #default="{ row }">{{ row.lastRun ? new Date(row.lastRun).toLocaleString() : '未执行' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="run(row.id)">立即执行</el-button>
          <el-button size="small" @click="edit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showDlg" :title="editing ? '编辑任务' : '新增任务'" width="450px">
    <el-form label-width="80px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="命令"><el-input v-model="form.command" placeholder="如: say 每日重启提醒" /></el-form-item>
      <el-form-item label="Cron"><el-input v-model="form.cron" placeholder="如: 0 12 * * * (每天12点)" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showDlg=false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const tasks = ref([])
const showDlg = ref(false)
const editing = ref(false)
const form = reactive({ id: null, name: '', command: '', cron: '', enabled: true })

onMounted(load)

async function load() {
  const d = await api.get('/api/tasks')
  tasks.value = d.tasks || []
}

function openAdd() {
  editing.value = false
  Object.assign(form, { id: null, name: '', command: '', cron: '', enabled: true })
  showDlg.value = true
}

function edit(row) {
  editing.value = true
  Object.assign(form, { id: row.id, name: row.name, command: row.command, cron: row.cron, enabled: row.enabled })
  showDlg.value = true
}

async function submit() {
  if (!form.name || !form.command || !form.cron) { ElMessage.warning('请填写完整'); return }
  if (editing.value) {
    await api.put('/api/tasks/' + form.id, form)
  } else {
    await api.post('/api/tasks', form)
  }
  ElMessage.success('保存成功')
  showDlg.value = false
  load()
}

async function update(row) {
  await api.put('/api/tasks/' + row.id, { name: row.name, command: row.command, cron: row.cron, enabled: row.enabled })
  ElMessage.success('已更新')
}

async function run(id) {
  await api.post('/api/tasks/' + id + '/run')
  ElMessage.success('已执行')
  load()
}

async function del(id) {
  await ElMessageBox.confirm('删除该任务?', '提示', { type: 'warning' })
  await api.delete('/api/tasks/' + id)
  ElMessage.success('已删除')
  load()
}
</script>

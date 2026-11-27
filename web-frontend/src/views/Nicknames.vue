<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>中文昵称</b>
        <el-button type="primary" size="small" @click="openSet">设置昵称</el-button>
      </div>
    </template>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
              title="昵称只影响聊天/TAB/计分板显示，不改变真实登录名。玩家也可用 /nick 自行设置。" />

    <el-table :data="list" stripe size="small">
      <el-table-column prop="name" label="真实玩家名" width="180" />
      <el-table-column prop="nick" label="昵称" />
      <el-table-column prop="uuid" label="UUID" width="300" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row)">清除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!list.length" description="暂无中文昵称" :image-size="60" />
  </el-card>

  <el-dialog v-model="visible" title="设置中文昵称" width="400px">
    <el-form label-width="80px">
      <el-form-item label="玩家名">
        <el-input v-model="form.name" placeholder="真实登录名，支持中文" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nick" placeholder="留空表示清除昵称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const list = ref([])
const visible = ref(false)
const form = reactive({ name: '', nick: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/nicknames')
  list.value = d.nicknames || []
}

function openSet() {
  Object.assign(form, { name: '', nick: '' })
  visible.value = true
}

async function save() {
  if (!form.name) { ElMessage.warning('请填写玩家名'); return }
  await api.post('/api/nicknames', { name: form.name, nick: form.nick })
  ElMessage.success('已保存')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定清除 ' + row.name + ' 的昵称？', '提示', { type: 'warning' })
  await api.delete('/api/nicknames/' + row.uuid)
  ElMessage.success('已清除')
  load()
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>命令告示牌</b>
        <el-button type="primary" size="small" @click="openCreate">添加</el-button>
      </div>
    </template>

    <el-table :data="signs" stripe size="small">
      <el-table-column prop="world" label="世界" width="140" />
      <el-table-column label="坐标" width="150">
        <template #default="{ row }">{{ row.x }}, {{ row.y }}, {{ row.z }}</template>
      </el-table-column>
      <el-table-column label="命令">
        <template #default="{ row }"><code>/{{ row.command }}</code></template>
      </el-table-column>
      <el-table-column prop="permission" label="权限节点" width="180" />
      <el-table-column prop="creator" label="创建者" width="120" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!signs.length" description="暂无命令告示牌，可在游戏内 /cmdsign add 创建" :image-size="60" />
  </el-card>

  <el-dialog v-model="visible" title="添加命令告示牌" width="420px">
    <el-form label-width="80px">
      <el-form-item label="世界"><el-input v-model="form.world" placeholder="world" /></el-form-item>
      <el-row :gutter="8">
        <el-col :span="8"><el-form-item label="X"><el-input v-model="form.x" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="Y"><el-input v-model="form.y" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="Z"><el-input v-model="form.z" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="命令">
        <el-input v-model="form.command" placeholder="spawn 或 console:say 你好" />
      </el-form-item>
      <el-form-item label="权限节点">
        <el-input v-model="form.permission" placeholder="留空所有人可用" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="create">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const signs = ref([])
const visible = ref(false)
const form = reactive({ world: 'world', x: 0, y: 64, z: 0, command: '', permission: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/signs')
  signs.value = d.signs || []
}

function openCreate() {
  Object.assign(form, { world: 'world', x: 0, y: 64, z: 0, command: '', permission: '' })
  visible.value = true
}

async function create() {
  if (!form.command) { ElMessage.warning('请填写命令'); return }
  await api.post('/api/signs', {
    world: form.world, x: Number(form.x), y: Number(form.y), z: Number(form.z),
    command: form.command, permission: form.permission
  })
  ElMessage.success('已保存')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定移除坐标 ' + row.x + ',' + row.y + ',' + row.z + ' 的命令告示牌？', '提示', { type: 'warning' })
  await api.delete('/api/signs/' + row.world + '/' + row.x + '/' + row.y + '/' + row.z)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>Warp 传送点 ({{ warps.length }})</b>
        <el-button type="primary" size="small" @click="showAdd = true">新增 Warp</el-button>
      </div>
    </template>
    <el-table :data="warps" stripe size="small">
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="world" label="世界" width="120" />
      <el-table-column label="坐标">
        <template #default="{ row }">{{ row.x }}, {{ row.y }}, {{ row.z }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="del(row.name)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showAdd" title="新增 Warp" width="400px">
    <el-form label-width="100px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="所在玩家">
        <el-input v-model="form.player" placeholder="在线玩家名 (以其位置创建)" />
      </el-form-item>
      <el-form-item label="或坐标">
        <el-input v-model="form.coords" placeholder="world,x,y,z (与玩家二选一)" />
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

const warps = ref([])
const showAdd = ref(false)
const form = reactive({ name: '', player: '', coords: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/warps')
  warps.value = d.warps || []
}

async function submit() {
  if (!form.name) { ElMessage.warning('请输入名称'); return }
  const body = { name: form.name }
  if (form.player) body.player = form.player
  else if (form.coords) {
    const [world, x, y, z] = form.coords.split(',').map(s => s.trim())
    body.world = world; body.x = +x; body.y = +y; body.z = +z
  } else { ElMessage.warning('请输入玩家名或坐标'); return }
  await api.post('/api/warps', body)
  ElMessage.success('Warp 创建成功')
  showAdd.value = false
  form.name = form.player = form.coords = ''
  load()
}

async function del(name) {
  await ElMessageBox.confirm('删除 warp ' + name + '?', '提示', { type: 'warning' })
  await api.delete('/api/warps/' + name)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>NPC 管理</b>
        <div>
          <el-tag :type="citizens ? 'success' : 'info'" style="margin-right:8px">
            {{ citizens ? 'Citizens 已安装' : '仅内置 NPC' }}
          </el-tag>
          <el-button type="primary" size="small" @click="openCreate">创建 NPC</el-button>
        </div>
      </div>
    </template>

    <el-table :data="npcs" stripe size="small">
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="type" label="类型" width="110" />
      <el-table-column label="位置" width="180">
        <template #default="{ row }">
          {{ row.world }} {{ row.x.toFixed(0) }},{{ row.y.toFixed(0) }},{{ row.z.toFixed(0) }}
        </template>
      </el-table-column>
      <el-table-column label="点击动作" width="220">
        <template #default="{ row }">
          <el-tag size="small" style="margin-right:6px">{{ row.actionType }}</el-tag>
          <span>{{ row.actionValue || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="skin" label="皮肤" width="110" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!npcs.length" description="暂无 NPC，可在游戏内 /npc create 创建" :image-size="60" />
  </el-card>

  <el-dialog v-model="createVisible" title="创建 NPC" width="460px">
    <el-form label-width="80px">
      <el-form-item label="名称"><el-input v-model="form.name" placeholder="支持 & 颜色代码" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type" style="width:100%">
          <el-option label="村民（内置）" value="villager" />
          <el-option label="盔甲架（内置）" value="armor_stand" />
          <el-option label="僵尸（内置）" value="zombie" />
          <el-option label="骷髅（内置）" value="skeleton" />
          <el-option label="玩家皮肤（需 Citizens）" value="player" />
        </el-select>
      </el-form-item>
      <el-form-item label="世界"><el-input v-model="form.world" placeholder="world" /></el-form-item>
      <el-row :gutter="8">
        <el-col :span="8"><el-form-item label="X"><el-input v-model="form.x" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="Y"><el-input v-model="form.y" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="Z"><el-input v-model="form.z" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="create">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editVisible" title="编辑 NPC" width="480px">
    <el-form label-width="90px">
      <el-form-item label="名称"><el-input v-model="edit.name" /></el-form-item>
      <el-form-item label="动作类型">
        <el-select v-model="edit.actionType" style="width:100%">
          <el-option label="玩家执行命令" value="command" />
          <el-option label="控制台执行命令" value="console" />
          <el-option label="打开菜单" value="menu" />
          <el-option label="发送消息" value="message" />
        </el-select>
      </el-form-item>
      <el-form-item label="动作内容">
        <el-input v-model="edit.actionValue" placeholder="命令 / 菜单 id / 消息，支持 {player} {nick}" />
      </el-form-item>
      <el-form-item label="皮肤玩家名">
        <el-input v-model="edit.skin" :disabled="!citizens" placeholder="仅 Citizens 玩家 NPC 可用" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="saveEdit">保存并重生</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const npcs = ref([])
const citizens = ref(false)
const createVisible = ref(false)
const editVisible = ref(false)
const form = reactive({ name: 'NPC', type: 'villager', world: 'world', x: 0, y: 65, z: 0 })
const edit = reactive({ id: '', name: '', actionType: 'command', actionValue: '', skin: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/npcs')
  npcs.value = d.npcs || []
  citizens.value = !!d.citizens
}

function openCreate() {
  Object.assign(form, { name: 'NPC', type: 'villager', world: 'world', x: 0, y: 65, z: 0 })
  createVisible.value = true
}

async function create() {
  if (!form.name) { ElMessage.warning('请填写名称'); return }
  await api.post('/api/npcs', {
    name: form.name, type: form.type, world: form.world,
    x: Number(form.x), y: Number(form.y), z: Number(form.z)
  })
  ElMessage.success('已创建')
  createVisible.value = false
  load()
}

function openEdit(row) {
  Object.assign(edit, {
    id: row.id, name: row.name,
    actionType: row.actionType || 'command',
    actionValue: row.actionValue || '', skin: row.skin || ''
  })
  editVisible.value = true
}

async function saveEdit() {
  await api.post('/api/npcs/' + edit.id, {
    name: edit.name, actionType: edit.actionType, actionValue: edit.actionValue, skin: edit.skin
  })
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除 NPC ' + id + '？', '提示', { type: 'warning' })
  await api.delete('/api/npcs/' + id)
  ElMessage.success('已删除')
  load()
}
</script>

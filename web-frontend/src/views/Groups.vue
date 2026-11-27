<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>权限组 ({{ groups.length }})</b>
        <el-button type="primary" size="small" @click="openCreate">新建分组</el-button>
      </div>
    </template>

    <el-table :data="groups" stripe size="small">
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column label="前缀" width="140">
        <template #default="{ row }">{{ row.prefix || '—' }}</template>
      </el-table-column>
      <el-table-column prop="weight" label="权重" width="90" />
      <el-table-column label="权限节点" width="110">
        <template #default="{ row }">{{ row.permissions.length }}</template>
      </el-table-column>
      <el-table-column label="成员" width="90">
        <template #default="{ row }">{{ row.memberCount }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="openPerms(row)">权限</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="primary" @click="openAddMember(row)">加人</el-button>
          <el-button size="small" type="danger" :disabled="row.name === 'default'" @click="removeGroup(row.name)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新建 -->
  <el-dialog v-model="createVisible" title="新建分组" width="420px">
    <el-form label-width="70px">
      <el-form-item label="名称">
        <el-input v-model="createForm.name" placeholder="仅限 a-z 0-9 _ -" />
      </el-form-item>
      <el-form-item label="前缀">
        <el-input v-model="createForm.prefix" placeholder="可留空，支持 & 颜色代码" />
      </el-form-item>
      <el-form-item label="权重">
        <el-input v-model="createForm.weight" placeholder="数字越大优先级越高" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="submitCreate">创建</el-button>
    </template>
  </el-dialog>

  <!-- 编辑 -->
  <el-dialog v-model="editVisible" title="编辑分组" width="420px">
    <el-form label-width="70px">
      <el-form-item label="分组">{{ editForm.name }}</el-form-item>
      <el-form-item label="前缀"><el-input v-model="editForm.prefix" /></el-form-item>
      <el-form-item label="权重"><el-input v-model="editForm.weight" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>

  <!-- 权限 -->
  <el-dialog v-model="permVisible" :title="'权限节点 - ' + permGroup" width="560px">
    <div style="display:flex;gap:8px;margin-bottom:12px">
      <el-input v-model="permInput" placeholder="如 plu.fly 或 -plu.god（前缀 - 表示否定）" />
      <el-button type="primary" @click="addPerm">添加</el-button>
    </div>
    <el-table :data="perms" size="small" max-height="360">
      <el-table-column prop="perm" label="节点" />
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="removePerm(row.perm)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="perms.length === 0" description="暂无权限节点" :image-size="60" />
  </el-dialog>

  <!-- 加人 -->
  <el-dialog v-model="memberVisible" :title="'添加成员 - ' + memberGroup" width="420px">
    <el-input v-model="memberName" placeholder="玩家名（支持离线玩家）" />
    <template #footer>
      <el-button @click="memberVisible = false">取消</el-button>
      <el-button type="primary" @click="submitMember">添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const groups = ref([])

const createVisible = ref(false)
const createForm = reactive({ name: '', prefix: '', weight: '0' })

const editVisible = ref(false)
const editForm = reactive({ name: '', prefix: '', weight: '0' })

const permVisible = ref(false)
const permGroup = ref('')
const perms = ref([])
const permInput = ref('')

const memberVisible = ref(false)
const memberGroup = ref('')
const memberName = ref('')

onMounted(load)

async function load() {
  const d = await api.get('/api/groups')
  groups.value = d.groups || []
}

function openCreate() {
  Object.assign(createForm, { name: '', prefix: '', weight: '0' })
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.name) { ElMessage.warning('请输入分组名称'); return }
  await api.post('/api/groups', createForm)
  ElMessage.success('已创建')
  createVisible.value = false
  load()
}

function openEdit(row) {
  Object.assign(editForm, { name: row.name, prefix: row.prefix, weight: String(row.weight) })
  editVisible.value = true
}

async function submitEdit() {
  await api.put('/api/groups/' + editForm.name, { prefix: editForm.prefix, weight: editForm.weight })
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

function openPerms(row) {
  permGroup.value = row.name
  perms.value = (row.permissions || []).map(p => ({ perm: p }))
  permInput.value = ''
  permVisible.value = true
}

async function addPerm() {
  const perm = permInput.value.trim()
  if (!perm) { ElMessage.warning('请输入权限节点'); return }
  await api.post('/api/groups/' + permGroup.value + '/permissions', { permission: perm, action: 'add' })
  permInput.value = ''
  await refreshPerms()
}

async function removePerm(perm) {
  await api.post('/api/groups/' + permGroup.value + '/permissions', { permission: perm, action: 'remove' })
  await refreshPerms()
}

async function refreshPerms() {
  await load()
  const g = groups.value.find(x => x.name === permGroup.value)
  perms.value = g ? (g.permissions || []).map(p => ({ perm: p })) : []
}

function openAddMember(row) {
  memberGroup.value = row.name
  memberName.value = ''
  memberVisible.value = true
}

async function submitMember() {
  const name = memberName.value.trim()
  if (!name) { ElMessage.warning('请输入玩家名'); return }
  await api.post('/api/groups/' + memberGroup.value + '/players/' + encodeURIComponent(name))
  ElMessage.success('已加入分组')
  memberVisible.value = false
  load()
}

async function removeGroup(name) {
  await ElMessageBox.confirm('确定删除分组 ' + name + ' ? 成员会同步失去该组权限。', '提示', { type: 'warning' })
  await api.delete('/api/groups/' + name)
  ElMessage.success('已删除')
  load()
}
</script>

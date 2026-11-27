<template>
  <el-row :gutter="16">
    <el-col :span="7">
      <el-card shadow="never">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <b>菜单列表</b>
            <el-button type="primary" size="small" @click="createVisible = true">新建</el-button>
          </div>
        </template>
        <el-menu :default-active="activeId" @select="select">
          <el-menu-item v-for="m in menus" :key="m.id" :index="m.id">
            {{ m.id }} <span style="margin-left:8px;color:#909399">{{ m.title }}</span>
          </el-menu-item>
        </el-menu>
        <el-empty v-if="!menus.length" description="暂无菜单" :image-size="60" />
      </el-card>
    </el-col>

    <el-col :span="17">
      <el-card v-if="current" shadow="never">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <b>编辑：{{ current.id }}</b>
            <div>
              <el-button size="small" type="danger" @click="removeMenu">删除菜单</el-button>
              <el-button size="small" type="primary" @click="saveMenu">保存菜单</el-button>
            </div>
          </div>
        </template>

        <el-form label-width="70px">
          <el-form-item label="标题">
            <el-input v-model="current.title" placeholder="支持 & 颜色代码" />
          </el-form-item>
          <el-form-item label="行数">
            <el-input-number v-model="current.size" :min="9" :max="54" :step="9" />
            <span style="margin-left:10px;color:#909399">每行 9 格，9-54</span>
          </el-form-item>
        </el-form>

        <div style="display:flex;justify-content:space-between;align-items:center;margin:10px 0">
          <b>菜单项</b>
          <el-button type="primary" size="small" @click="openItem(null)">添加菜单项</el-button>
        </div>

        <el-table :data="current.items" stripe size="small">
          <el-table-column prop="slot" label="槽位" width="70" />
          <el-table-column prop="material" label="材料" width="130" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="permission" label="权限" width="140" />
          <el-table-column label="动作" width="160">
            <template #default="{ row }">
              <span v-if="row.open">打开 {{ row.open }}</span>
              <span v-else>{{ (row.commands || []).join(', ') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ $index, row }">
              <el-button size="small" @click="openItem($index)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
      <el-empty v-else description="请选择或创建一个菜单" />
    </el-col>
  </el-row>

  <el-dialog v-model="createVisible" title="新建菜单" width="380px">
    <el-form label-width="60px">
      <el-form-item label="ID"><el-input v-model="newId" placeholder="英文数字，如 shop" /></el-form-item>
      <el-form-item label="行数"><el-input-number v-model="newSize" :min="9" :max="54" :step="9" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="createMenu">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="itemVisible" :title="itemIndex === null ? '添加菜单项' : '编辑菜单项'" width="560px">
    <el-form label-width="90px">
      <el-row :gutter="10">
        <el-col :span="12"><el-form-item label="槽位"><el-input-number v-model="item.slot" :min="0" :max="53" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="材料"><el-input v-model="item.material" placeholder="COMPASS" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="名称"><el-input v-model="item.name" placeholder="支持 & 颜色代码" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="item.loreText" type="textarea" :rows="2" placeholder="每行一条" /></el-form-item>
      <el-row :gutter="10">
        <el-col :span="12"><el-form-item label="数量"><el-input-number v-model="item.amount" :min="1" :max="64" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="权限"><el-input v-model="item.permission" placeholder="留空所有人可用" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="命令"><el-input v-model="item.commandsText" type="textarea" :rows="3" placeholder="每行一条；console: 前缀为控制台执行" /></el-form-item>
      <el-row :gutter="10">
        <el-col :span="12"><el-form-item label="打开子菜单"><el-input v-model="item.open" placeholder="菜单 id" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="点击后关闭"><el-switch v-model="item.close" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="itemVisible = false">取消</el-button>
      <el-button type="primary" @click="saveItem">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const menus = ref([])
const activeId = ref('')
const current = ref(null)
const createVisible = ref(false)
const newId = ref('')
const newSize = ref(27)
const itemVisible = ref(false)
const itemIndex = ref(null)
const item = reactive({ slot: 0, material: 'STONE', name: '', loreText: '', amount: 1, permission: '', commandsText: '', open: '', close: false })

onMounted(load)

async function load(keep) {
  const d = await api.get('/api/menus')
  menus.value = d.menus || []
  if (keep && menus.value.find(m => m.id === activeId.value)) select(activeId.value)
  else if (menus.value.length) select(menus.value[0].id)
  else current.value = null
}

async function select(id) {
  activeId.value = id
  const d = await api.get('/api/menus/' + id)
  current.value = d
  current.value.items = (current.value.items || []).map(it => ({ ...it, lore: it.lore || [], commands: it.commands || [] }))
}

async function createMenu() {
  if (!newId.value) { ElMessage.warning('请填写 ID'); return }
  await api.post('/api/menus', { id: newId.value, size: newSize.value })
  ElMessage.success('已创建')
  createVisible.value = false
  activeId.value = newId.value
  newId.value = ''
  load(true)
}

async function saveMenu() {
  await api.put('/api/menus/' + current.value.id, current.value)
  ElMessage.success('已保存')
  load(true)
}

async function removeMenu() {
  await ElMessageBox.confirm('确定删除菜单 ' + current.value.id + '？', '提示', { type: 'warning' })
  await api.delete('/api/menus/' + current.value.id)
  ElMessage.success('已删除')
  current.value = null
  load()
}

function openItem(index) {
  itemIndex.value = index
  if (index === null) {
    Object.assign(item, { slot: 0, material: 'STONE', name: '', loreText: '', amount: 1, permission: '', commandsText: '', open: '', close: false })
  } else {
    const it = current.value.items[index]
    Object.assign(item, {
      slot: it.slot, material: it.material, name: it.name,
      loreText: (it.lore || []).join('\n'), amount: it.amount || 1,
      permission: it.permission || '', commandsText: (it.commands || []).join('\n'),
      open: it.open || '', close: !!it.close
    })
  }
  itemVisible.value = true
}

function saveItem() {
  const data = {
    slot: item.slot, material: item.material, name: item.name,
    lore: item.loreText.split('\n').map(s => s.trim()).filter(Boolean),
    amount: item.amount, permission: item.permission,
    commands: item.commandsText.split('\n').map(s => s.trim()).filter(Boolean),
    open: item.open, close: item.close
  }
  if (itemIndex.value === null) current.value.items.push(data)
  else current.value.items.splice(itemIndex.value, 1, data)
  itemVisible.value = false
}

function removeItem(index) {
  current.value.items.splice(index, 1)
}
</script>

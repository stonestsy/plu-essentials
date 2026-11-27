<template>
  <el-card shadow="never" style="margin-bottom:16px">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>充值礼包</b>
        <div>
          <el-tag :type="enabled ? 'success' : 'info'" style="margin-right:8px">
            {{ enabled ? '充值已开启' : '充值未开启' }}
          </el-tag>
          <el-tag :type="mock ? 'warning' : 'success'" style="margin-right:8px">
            {{ mock ? '模拟支付' : '微信 JSAPI' }}
          </el-tag>
          <el-button type="primary" size="small" @click="openCreate">新建礼包</el-button>
        </div>
      </div>
    </template>

    <el-table :data="packages" stripe size="small">
      <el-table-column prop="id" label="ID" width="120" />
      <el-table-column prop="name" label="名称" />
      <el-table-column label="价格" width="110">
        <template #default="{ row }">{{ (row.priceFen / 100).toFixed(2) }} 元</template>
      </el-table-column>
      <el-table-column prop="coins" label="到账货币" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="removePackage(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-card shadow="never">
    <template #header><b>充值订单（最近 200 条）</b></template>
    <el-table :data="orders" stripe size="small" max-height="440">
      <el-table-column prop="no" label="订单号" width="190" />
      <el-table-column prop="player" label="玩家" width="120" />
      <el-table-column prop="packageId" label="礼包" width="100" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">{{ row.amount.toFixed(2) }} 元</template>
      </el-table-column>
      <el-table-column prop="coins" label="货币" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ fmt(row.created) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" type="success"
                     @click="confirmOrder(row.no)">确认到账</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑礼包' : '新建礼包'" width="440px">
    <el-form label-width="90px">
      <el-form-item label="ID">
        <el-input v-model="form.id" :disabled="editing" placeholder="如 p100，仅英文数字" />
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="如 100 金币" />
      </el-form-item>
      <el-form-item label="价格(分)">
        <el-input v-model="form.priceFen" placeholder="100 = 1.00 元" />
      </el-form-item>
      <el-form-item label="到账货币">
        <el-input v-model="form.coins" placeholder="100" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const enabled = ref(false)
const mock = ref(true)
const packages = ref([])
const orders = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = reactive({ id: '', name: '', priceFen: '100', coins: '100', enabled: true })

onMounted(load)

async function load() {
  const d = await api.get('/api/pay')
  enabled.value = !!d.enabled
  mock.value = !!d.mock
  packages.value = d.packages || []
  orders.value = d.orders || []
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: '', name: '', priceFen: '100', coins: '100', enabled: true })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, name: row.name, priceFen: String(row.priceFen),
    coins: String(row.coins), enabled: row.enabled
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.id || !form.name) { ElMessage.warning('请填写 ID 与名称'); return }
  const payload = {
    id: form.id,
    name: form.name,
    priceFen: Number(form.priceFen),
    coins: Number(form.coins),
    enabled: form.enabled
  }
  if (editing.value) await api.put('/api/pay/packages/' + form.id, payload)
  else await api.post('/api/pay/packages', payload)
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

async function removePackage(id) {
  await ElMessageBox.confirm('确定删除礼包 ' + id + '？', '提示', { type: 'warning' })
  await api.delete('/api/pay/packages/' + id)
  ElMessage.success('已删除')
  load()
}

async function confirmOrder(no) {
  await ElMessageBox.confirm('确认已收到款项并为订单 ' + no + ' 发货？', '人工确认', { type: 'warning' })
  await api.post('/api/pay/orders/' + no + '/confirm')
  ElMessage.success('已发货')
  load()
}

function statusText(s) {
  return s === 'PAID' ? '已支付' : s === 'PENDING' ? '待支付' : s
}
function statusType(s) {
  return s === 'PAID' ? 'success' : s === 'PENDING' ? 'warning' : 'info'
}
function fmt(ts) {
  if (!ts) return '—'
  const d = new Date(ts)
  return d.toLocaleString()
}
</script>

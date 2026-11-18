<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <b>兑换码 ({{ codes.length }})</b>
        <div>
          <el-tag v-if="!economyAvailable" type="danger" style="margin-right:10px">经济系统不可用</el-tag>
          <el-button type="primary" size="small" @click="showAdd = true">新建兑换码</el-button>
        </div>
      </div>
    </template>

    <el-table :data="codes" stripe size="small">
      <el-table-column prop="code" label="兑换码" width="150" />
      <el-table-column label="金额" width="120">
        <template #default="{ row }">{{ row.amount.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="使用次数" width="120">
        <template #default="{ row }">
          {{ row.maxUses === 0 ? '不限' : row.usedCount + ' / ' + row.maxUses }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="!row.enabled" type="info" size="small">已停用</el-tag>
          <el-tag v-else-if="row.expired" type="danger" size="small">已过期</el-tag>
          <el-tag v-else type="success" size="small">可用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="到期时间" width="170">
        <template #default="{ row }">{{ row.expires === 0 ? '永久' : fmtTime(row.expires) }}</template>
      </el-table-column>
      <el-table-column prop="creator" label="创建者" width="120" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="toggle(row.code)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="remove(row.code)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="showAdd" title="新建兑换码" width="440px">
    <el-form label-width="90px">
      <el-form-item label="兑换码">
        <el-input v-model="form.code" placeholder="留空或填 # 随机生成" />
      </el-form-item>
      <el-form-item label="金额">
        <el-input v-model="form.amount" placeholder="例如 100" />
      </el-form-item>
      <el-form-item label="可用次数">
        <el-input v-model="form.maxUses" placeholder="0 = 不限，默认 1" />
      </el-form-item>
      <el-form-item label="有效时长">
        <el-input v-model="form.duration" placeholder="如 7d、12h，留空或 0 = 永久" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAdd = false">取消</el-button>
      <el-button type="primary" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const codes = ref([])
const economyAvailable = ref(false)
const showAdd = ref(false)
const form = reactive({ code: '', amount: '', maxUses: '', duration: '' })

onMounted(load)

async function load() {
  const d = await api.get('/api/redeem')
  codes.value = d.codes || []
  economyAvailable.value = !!d.economyAvailable
}

async function submit() {
  if (!form.amount) { ElMessage.warning('请输入金额'); return }
  await api.post('/api/redeem', {
    code: form.code || '#',
    amount: form.amount,
    maxUses: form.maxUses || '1',
    duration: form.duration
  })
  ElMessage.success('兑换码已创建')
  showAdd.value = false
  Object.assign(form, { code: '', amount: '', maxUses: '', duration: '' })
  load()
}

async function toggle(code) {
  await api.post('/api/redeem/' + code + '/toggle')
  load()
}

async function remove(code) {
  await ElMessageBox.confirm('确定删除兑换码 ' + code + ' ?', '提示', { type: 'warning' })
  await api.delete('/api/redeem/' + code)
  ElMessage.success('已删除')
  load()
}

function fmtTime(ts) {
  const d = new Date(ts)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

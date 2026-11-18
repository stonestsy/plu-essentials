<template>
  <el-card shadow="never">
    <template #header><b>公告与规则（游戏内以书本形式展示）</b></template>

    <el-tabs v-model="tab">
      <el-tab-pane label="服务器公告" name="news">
        <el-alert type="info" :closable="false" style="margin-bottom:12px"
          title="每行内容对应书本的一页，支持 & 颜色代码（如 &6金色）" />
        <el-input v-model="newsText" type="textarea" :rows="10" placeholder="第一页内容&#10;第二页内容" />
        <div style="margin-top:10px">
          <el-button type="primary" @click="save('news')">保存公告</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="服务器规则" name="rules">
        <el-alert type="info" :closable="false" style="margin-bottom:12px"
          title="每行内容对应书本的一页，支持 & 颜色代码" />
        <el-input v-model="rulesText" type="textarea" :rows="10" placeholder="第一条规则" />
        <div style="margin-top:10px">
          <el-button type="primary" @click="save('rules')">保存规则</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const tab = ref('news')
const newsText = ref('')
const rulesText = ref('')

onMounted(load)

async function load() {
  const d = await api.get('/api/announcements')
  newsText.value = (d.news?.pages || []).join('\n')
  rulesText.value = (d.rules?.pages || []).join('\n')
}

async function save(section) {
  const text = section === 'news' ? newsText.value : rulesText.value
  const pages = text.split('\n').filter(s => s.trim().length > 0)
  if (pages.length === 0) { ElMessage.warning('内容不能为空'); return }
  await api.post('/api/announcements/' + section, { pages })
  ElMessage.success('已保存')
}
</script>

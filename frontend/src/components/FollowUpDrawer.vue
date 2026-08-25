<template>
  <el-drawer
    v-model="visible"
    :title="`${customerName} - 跟进记录`"
    size="480px"
    :close-on-click-modal="true"
    close-on-press-escape
    destroy-on-close
    @close="handleClose"
  >
    <div class="followup-body">
      <!-- Input area -->
      <div class="input-area">
        <el-input
          v-model="newContent"
          type="textarea"
          :rows="3"
          placeholder="输入跟进内容（电话沟通、上门量尺、报价反馈等）"
          maxlength="2000"
          show-word-limit
          @keydown.ctrl.enter="handleAdd"
        />
        <div class="input-actions">
          <span class="input-hint">Ctrl+Enter 发送</span>
          <el-button class="btn-gold" type="primary" size="small" :loading="adding" @click="handleAdd" :disabled="!newContent.trim()">添加记录</el-button>
        </div>
      </div>

      <!-- Timeline -->
      <div class="timeline-wrap" v-loading="loading">
        <template v-if="list.length">
          <div class="timeline-item" v-for="item in list" :key="item.id">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-header">
                <span class="timeline-user">{{ item.username || '系统' }}</span>
                <span class="timeline-time">{{ formatTime(item.createTime) }}</span>
                <button class="btn-delete-followup" @click="handleDelete(item)" title="删除">x</button>
              </div>
              <div class="timeline-text">{{ item.content }}</div>
            </div>
          </div>
        </template>
        <div v-else-if="!loading" class="empty-followup">暂无跟进记录</div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCustomerFollowups, addCustomerFollowup, deleteCustomerFollowup } from '@/api/admin'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: Number, default: null },
  customerName: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const list = ref([])
const loading = ref(false)
const adding = ref(false)
const newContent = ref('')

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.customerId) loadList()
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

async function loadList() {
  loading.value = true
  try {
    const res = await getCustomerFollowups({ customerId: props.customerId, page: 1, size: 100 })
    const data = res.data?.data || res.data
    list.value = data.records || []
  } catch (err) {
    console.error('加载跟进记录失败:', err)
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const content = newContent.value.trim()
  if (!content) return
  adding.value = true
  try {
    await addCustomerFollowup({ customerId: props.customerId, content })
    newContent.value = ''
    await loadList()
    ElMessage.success('已添加')
  } catch (err) {
    console.error('添加跟进记录失败:', err)
    if (!err._handled) ElMessage.error('添加失败')
  } finally {
    adding.value = false
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm('确定删除这条跟进记录？', '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteCustomerFollowup(item.id)
    await loadList()
    ElMessage.success('已删除')
  } catch (err) {
    console.error('删除跟进记录失败:', err)
    if (!err._handled) ElMessage.error('删除失败')
  }
}

function handleClose() {
  newContent.value = ''
}

function formatTime(val) {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  const now = new Date()
  const isToday = d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (isToday) return `今天 ${time}`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`
}
</script>

<style scoped>
.followup-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 16px;
}

.input-area {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.input-hint {
  font-size: 12px;
  color: #bbb;
}
.btn-gold {
  background: #C5A265 !important;
  border-color: #C5A265 !important;
}
.btn-gold:hover {
  background: #d4b885 !important;
  border-color: #d4b885 !important;
}

.timeline-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  position: relative;
}
.timeline-item:last-child {
  border-bottom: none;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #C5A265;
  margin-top: 6px;
  flex-shrink: 0;
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.timeline-user {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.timeline-time {
  font-size: 12px;
  color: #aaa;
}

.btn-delete-followup {
  margin-left: auto;
  background: none;
  border: none;
  color: #ccc;
  font-size: 16px;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  transition: color 0.15s;
}
.btn-delete-followup:hover {
  color: #e74c3c;
}

.timeline-text {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-followup {
  text-align: center;
  padding: 40px 20px;
  color: #ccc;
  font-size: 14px;
}
</style>

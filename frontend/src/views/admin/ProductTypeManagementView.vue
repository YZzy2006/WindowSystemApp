<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>商品细目</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="load" />
        <input v-model="keyword" class="search-input" placeholder="搜索细目名称..." />
        <button class="btn-primary" @click="openAdd">＋ 新增细目</button>
      </div>
    </div>

    <div class="card">
      <div class="batch-bar" v-if="selectedIds.length > 0">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 项</span>
        <button class="btn-batch danger" @click="batchDelete" :disabled="batchOperating">{{ batchOperating ? '处理中...' : '批量删除' }}</button>
        <button class="btn-batch" @click="clearSelection">取消选择</button>
      </div>

      <table class="data-table" v-if="filteredList.length">
        <thead>
          <tr>
            <th class="col-check">
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" />
            </th>
            <th>细目名称</th>
            <th class="col-desc">总结单位</th>
            <th class="col-sort">排序</th>
            <th class="col-time">创建时间</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in filteredList" :key="c.id" >
            <td class="col-check">
              <input type="checkbox" :checked="selectedIds.includes(c.id)" @change="toggleSelect(c.id)" />
            </td>
            <td>{{ c.name }}</td>
            <td class="col-desc">{{ c.summaryDesc || '-' }}</td>
            <td class="col-sort">{{ c.sort ?? 0 }}</td>
            <td class="col-time">{{ formatTime(c.createTime) }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openEdit(c)">编辑</button>
              <button class="btn-text danger" @click="handleDel(c)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ list.length ? '未匹配到细目' : '暂无商品细目' }}</p>
    </div>

    <div class="modal-overlay" v-if="showModal" @click.self="showModal=false" @keydown.esc="showModal=false" tabindex="-1">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editId ? '编辑细目' : '新增细目' }}</h3>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <div class="form-group">
            <label>细目名称</label>
            <input v-model="form.name" placeholder="请输入细目名称" required />
          </div>
          <div class="form-group">
            <label>总结单位</label>
            <input v-model="form.summaryDesc" placeholder="如：樘/方、套/㎡" />
            <span class="field-hint">控制细目统计时的显示单位，用 "/" 分隔，如 "樘/方" 显示为 "x樘 x方"</span>
          </div>
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="form.sort" type="number" placeholder="数字越小越靠前" />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showModal=false">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductTypes, addProductType, updateProductType, deleteProductType, restoreEntity } from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

const list = ref([])
const keyword = ref('')
const showModal = ref(false)
const saving = ref(false)
useCtrlEnterSave(showModal, handleSave)
const form = reactive({ name: '', summaryDesc: '', sort: 0 })
let editId = null

const selectedIds = ref([])
const batchOperating = ref(false)
const isAllSelected = computed(() => filteredList.value.length > 0 && selectedIds.value.length === filteredList.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < filteredList.value.length)

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}
function toggleSelectAll() {
  if (isAllSelected.value) selectedIds.value = []
  else selectedIds.value = filteredList.value.map(c => c.id)
}
function clearSelection() { selectedIds.value = [] }

const { handleDelete: undoDelete } = useUndoDelete({
  deleteApi: deleteProductType,
  restoreApi: (id) => restoreEntity('product-type', id),
  onSuccess: load,
  entityLabel: '商品细目',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateProductType,
  onSuccess: load,
  entityLabel: '商品细目',
})

onMounted(load)

const filteredList = computed(() => {
  if (!keyword.value.trim()) return list.value
  const kw = keyword.value.trim().toLowerCase()
  return list.value.filter(c => c.name?.toLowerCase().includes(kw))
})

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

function load() {
  getProductTypes().then(r => {
    const data = r.data?.data ?? r.data
    list.value = Array.isArray(data) ? data : []
  })
}

function openAdd() {
  editId = null
  form.name = ''
  form.summaryDesc = ''
  form.sort = 0
  showModal.value = true
}

function openEdit(c) {
  snapshotBeforeEdit(c)
  editId = c.id
  form.name = c.name
  form.summaryDesc = c.summaryDesc || ''
  form.sort = c.sort ?? 0
  showModal.value = true
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入细目名称')
    return
  }
  saving.value = true
  try {
    if (editId) {
      await updateProductType(editId, { ...form })
      afterEditSave({ id: editId, ...form })
      ElMessage.success('细目已更新')
    } else {
      await addProductType({ ...form })
      ElMessage.success('细目已添加')
    }
    showModal.value = false
    load()
  } finally { saving.value = false }
}

function handleDel(c) { undoDelete(c) }

async function batchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个细目？`, '批量删除', { type: 'warning' })
  } catch { return }
  batchOperating.value = true
  try {
    let success = 0
    for (const id of ids) {
      try { await deleteProductType(id); success++ } catch (e) { console.error(e) }
    }
    ElMessage.success(`已删除 ${success} 个细目`)
    clearSelection()
    load()
  } finally { batchOperating.value = false }
}
</script>

<style scoped>
.col-check { width: 40px; text-align: center; }
.col-desc { color: #999; font-size: 12px; }
.col-sort { width: 80px; text-align: center; color: var(--text-placeholder); }
.field-hint { font-size: 11px; color: #bbb; margin-top: 3px; display: block; }
.col-time { width: 160px; color: var(--text-placeholder); font-size: 12px; }
.col-action { width: 140px; }
.modal { width: 440px; }

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #f6f8fa;
  border-bottom: 1px solid #eee;
}
.batch-info { font-size: 13px; color: #666; }
.batch-info strong { color: #C5A265; }
.btn-batch {
  background: none;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  color: #555;
}
.btn-batch:hover { border-color: #C5A265; color: #C5A265; }
.btn-batch.danger { color: #e74c3c; border-color: #f5c6cb; }
.btn-batch.danger:hover { background: #fff5f5; }
</style>

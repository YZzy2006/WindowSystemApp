<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="load" />
        <input v-model="keyword" class="search-input" placeholder="搜索分类名称..." />
        <button class="btn-primary" @click="openAdd">＋ 新增分类</button>
      </div>
    </div>

    <div class="card">
      <table class="data-table" v-if="filteredList.length">
        <thead><tr><th>ID</th><th>名称</th><th>排序</th><th class="col-action">操作</th></tr></thead>
        <tbody>
          <tr v-for="c in filteredList" :key="c.id" >
            <td class="col-id">{{ c.id }}</td>
            <td>{{ c.name }}</td>
            <td>{{ c.sort }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openEdit(c)">编辑</button>
              <button class="btn-text danger" @click="handleDel(c)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ list.length ? '未匹配到分类' : '暂无分类' }}</p>
    </div>

    <div class="modal-overlay" v-if="showModal" @click.self="showModal=false" @keydown.esc="showModal=false" tabindex="-1">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editId ? '编辑分类' : '新增分类' }}</h3>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <div class="form-group">
            <label>名称</label>
            <input v-model="form.name" placeholder="分类名称" required />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategories, addCategory, updateCategory, deleteCategory, restoreEntity } from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

const list = ref([])
const keyword = ref('')

const FILTER_KEY = 'categories_filters'
let _restoring = false
function restoreFilters() {
  try { const s = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}'); _restoring = true; if (s.keyword) keyword.value = s.keyword; _restoring = false } catch {}
}
function syncFilters() { if (_restoring) return; sessionStorage.setItem(FILTER_KEY, JSON.stringify({ keyword: keyword.value })) }
watch(keyword, syncFilters)
const showModal = ref(false)
const saving = ref(false)
useCtrlEnterSave(showModal, handleSave)
const form = reactive({ name: '', sort: 0 })
let editId = null

// ===== Undo composables =====
const { handleDelete: undoDelete } = useUndoDelete({
  deleteApi: deleteCategory,
  restoreApi: (id) => restoreEntity('category', id),
  onSuccess: load,
  entityLabel: '分类',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateCategory,
  onSuccess: load,
  entityLabel: '分类',
})

onMounted(() => { restoreFilters(); load() })

const filteredList = computed(() => {
  if (!keyword.value.trim()) return list.value
  const kw = keyword.value.trim().toLowerCase()
  return list.value.filter(c => c.name?.toLowerCase().includes(kw))
})

function load() { getCategories().then(r => list.value = r.data.data || []) }

function openAdd() { editId = null; form.name = ''; form.sort = 0; showModal.value = true }
function openEdit(c) { snapshotBeforeEdit(c); editId = c.id; form.name = c.name; form.sort = c.sort; showModal.value = true }

async function handleSave() {
  saving.value = true
  try {
    if (editId) {
      await updateCategory(editId, { ...form })
      afterEditSave({ id: editId, ...form })
    } else {
      await addCategory({ ...form })
      ElMessage.success('分类已添加')
    }
    showModal.value = false; load()
  } finally { saving.value = false }
}

function handleDel(c) { undoDelete(c) }
</script>

<style scoped>
.col-id { width: 80px; color: var(--text-placeholder); }
.col-action { width: 160px; }
.modal { width: 440px; }
</style>

<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>供应商管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="load" />
        <input v-model="keyword" class="search-input" placeholder="搜索供应商名称 / 联系人..." @keyup.enter="handleSearch" />
        <button class="btn-search" @click="handleSearch" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          搜索
        </button>
        <button class="btn-outline" @click="handleExport">导出</button>
        <button class="btn-outline" @click="importDialogVisible = true">导入</button>
        <button class="btn-primary" @click="openAdd">
          <span class="btn-icon">+</span> 新增供应商
        </button>
      </div>
    </div>

    <div class="summary-bar">
      <div class="summary-card"><div class="sc-label">供应商总数</div><div class="sc-value">{{ total }} 个</div></div>
    </div>

    <div class="card">
      <!-- Batch action bar -->
      <div class="batch-bar" v-if="selectedIds.length > 0">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 项</span>
        <button class="btn-batch" @click="batchToggleStar(1)">⭐ 批量标星</button>
        <button class="btn-batch" @click="batchToggleStar(0)">☆ 批量取消星标</button>
        <button class="btn-batch danger" @click="batchDelete">🗑 批量删除</button>
        <button class="btn-batch" @click="clearSelection">取消选择</button>
      </div>

      <!-- Loading skeleton -->
      <div v-if="loading" class="loading-wrap">
        <div class="skeleton-row" v-for="i in 5" :key="i">
          <span class="sk-cell w50"></span>
          <span class="sk-cell w150"></span>
          <span class="sk-cell w100"></span>
          <span class="sk-cell w120"></span>
          <span class="sk-cell w200"></span>
          <span class="sk-cell w140"></span>
          <span class="sk-cell w100"></span>
        </div>
      </div>

      <!-- Data table -->
      <table class="data-table" v-else-if="list.length">
        <thead>
          <tr>
            <th class="col-check">
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" />
            </th>
            <th class="col-star"></th>
            <th class="col-id">ID</th>
            <th class="sortable" @click="toggleSort('name')">供应商名称{{ sortArrow('name') }}</th>
            <th class="sortable" @click="toggleSort('contact')">联系人{{ sortArrow('contact') }}</th>
            <th class="sortable" @click="toggleSort('phone')">电话{{ sortArrow('phone') }}</th>
            <th>地址</th>
            <th class="col-time sortable" @click="toggleSort('createTime')">创建时间{{ sortArrow('createTime') }}</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in sortedList" :key="s.id" >
            <td class="col-check">
              <input type="checkbox" :checked="selectedIds.includes(s.id)" @change="toggleSelect(s.id)" />
            </td>
            <td class="col-star">
              <span class="star-icon" :class="{ starred: s.isStarred === 1 }" @click="handleToggleStar(s)" :title="s.isStarred === 1 ? '取消星标' : '星标'">
                {{ s.isStarred === 1 ? '★' : '☆' }}
              </span>
            </td>
            <td class="col-id">{{ s.id }}</td>
            <td class="col-name">{{ s.name }}</td>
            <td>{{ s.contact || '-' }}</td>
            <td>
              <a v-if="s.phone" :href="'tel:' + s.phone" class="phone-link">{{ s.phone }}</a>
              <span v-else class="cell-na">-</span>
            </td>
            <td class="col-address">{{ s.address || '-' }}</td>
            <td class="col-time">{{ formatDate(s.createTime) }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openEdit(s)">编辑</button>
              <button class="btn-text danger" @click="handleDel(s)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Empty state -->
      <div v-else class="empty-state">
        <span class="empty-icon">&#9783;</span>
        <p>{{ keyword ? '未匹配到供应商' : '暂无供应商数据' }}</p>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- Add / Edit Dialog -->
    <div class="modal-overlay" v-if="showModal" @click.self="closeModal" @keydown.esc="closeModal" tabindex="-1">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editId ? '编辑供应商' : '新增供应商' }}</h3>
          <button class="modal-close" @click="closeModal">&times;</button>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <div class="form-group">
            <label>供应商名称 <span class="required">*</span></label>
            <input v-model="form.name" placeholder="请输入供应商名称" required />
            <span v-if="errors.name" class="field-error">{{ errors.name }}</span>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>联系人</label>
              <input v-model="form.contact" placeholder="请输入联系人姓名" />
            </div>
            <div class="form-group">
              <label>电话</label>
              <input v-model="form.phone" placeholder="请输入联系电话" />
            </div>
          </div>
          <div class="form-group">
            <label>地址</label>
            <input v-model="form.address" placeholder="请输入供应商地址" />
          </div>
          <div class="form-group">
            <label>备注</label>
            <textarea v-model="form.remark" placeholder="备注信息（选填）" rows="3"></textarea>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入供应商"
      :column-map="importConfigs.suppliers.columns"
      :module="'suppliers'"
      :on-complete="load"
      :sample-data="importConfigs.suppliers.sample"
      template-filename="供应商导入模板"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSuppliers, addSupplier, updateSupplier, deleteSupplier, toggleSupplierStar, restoreEntity } from '@/api/admin'

const route = useRoute()
const router = useRouter()
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { useUndoAction } from '@/composables/useUndoAction'
import { exportToExcel } from '@/utils/exportExcel'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import { importConfigs } from '@/utils/importConfigs'
import { useTableSort } from '@/composables/useTableSort'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

// ===== State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { createTime: 'date' }, { storageKey: 'suppliers_filters' })
const keyword = ref('')
const importDialogVisible = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const showModal = ref(false)
useCtrlEnterSave(showModal, handleSave)
const editId = ref(null)
const selectedIds = ref([])

const FILTER_KEY = 'suppliers_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  if (q.keyword || q.page || q.size) {
    _restoring = true
    if (q.keyword) keyword.value = q.keyword
    if (q.page && Number(q.page) > 1) currentPage.value = Number(q.page)
    if (q.size && Number(q.size) > 0) pageSize.value = Number(q.size)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.page && saved.page > 1) currentPage.value = saved.page
    if (saved.size && saved.size > 0) pageSize.value = saved.size
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (keyword.value) { query.keyword = keyword.value; storage.keyword = keyword.value }
  if (currentPage.value > 1) { query.page = String(currentPage.value); storage.page = currentPage.value }
  if (pageSize.value !== 10) { query.size = String(pageSize.value); storage.size = pageSize.value }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

const form = reactive({
  name: '',
  contact: '',
  phone: '',
  address: '',
  remark: ''
})
const errors = reactive({
  name: ''
})

// ===== Undo composables =====
const { handleDelete: undoDelete, handleBatchDelete } = useUndoDelete({
  deleteApi: deleteSupplier,
  restoreApi: (id) => restoreEntity('supplier', id),
  onSuccess: () => { if (list.value.length <= 1 && currentPage.value > 1) currentPage.value--; load() },
  entityLabel: '供应商',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateSupplier,
  onSuccess: load,
  entityLabel: '供应商',
})

const { handleBatchAction: handleBatchToggleStar } = useUndoAction({
  actionApi: toggleSupplierStar,
  reverseApi: toggleSupplierStar,
  onSuccess: load,
  entityLabel: '供应商',
  actionLabel: '标星',
})

// ===== Lifecycle =====
onMounted(() => { restoreFilters(); load() })

// ===== Data Loading =====
async function load() {
  loading.value = true
  try {
    const res = await getSuppliers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined
    })
    const data = res.data.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载供应商列表失败')
  } finally {
    loading.value = false
    syncFiltersToUrl()
  }
}

function handleSearch() {
  currentPage.value = 1
  load()
}

// ===== Form Helpers =====
function resetForm() {
  form.name = ''
  form.contact = ''
  form.phone = ''
  form.address = ''
  form.remark = ''
  errors.name = ''
}

function validateForm() {
  errors.name = ''
  if (!form.name.trim()) {
    errors.name = '请输入供应商名称'
    return false
  }
  return true
}

function closeModal() {
  showModal.value = false
  resetForm()
}

// ===== Add =====
async function handleExport() {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const columns = [
    { label: '供应商名称', key: 'name', width: 16 },
    { label: '联系人', key: 'contact', width: 12 },
    { label: '电话', key: 'phone', width: 14 },
    { label: '地址', key: 'address', width: 24 },
    { label: '备注', key: 'remark', width: 20 },
  ]
  await exportToExcel(list.value, columns, '供应商列表', { sheetName: '供应商' })
}

function openAdd() {
  editId.value = null
  resetForm()
  showModal.value = true
}

// ===== Edit =====
function openEdit(s) {
  snapshotBeforeEdit(s)
  editId.value = s.id
  form.name = s.name || ''
  form.contact = s.contact || ''
  form.phone = s.phone || ''
  form.address = s.address || ''
  form.remark = s.remark || ''
  errors.name = ''
  showModal.value = true
}

// ===== Save =====
async function handleSave() {
  if (!validateForm()) return
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      contact: form.contact.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
      remark: form.remark.trim()
    }
    if (editId.value) {
      await updateSupplier(editId.value, payload)
      closeModal()
      afterEditSave({ id: editId.value, ...payload })
      load()
    } else {
      await addSupplier(payload)
      ElMessage.success('供应商新增成功')
      closeModal()
      load()
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败，请重试')
  } finally {
    saving.value = false
  }
}

// ===== Delete =====
function handleDel(s) {
  undoDelete(s)
}

// ===== Toggle Star =====
async function handleToggleStar(s) {
  try {
    await toggleSupplierStar(s.id)
    s.isStarred = s.isStarred === 1 ? 0 : 1
    ElMessage.success(s.isStarred === 1 ? '已标星' : '已取消星标')
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

// ===== Batch Operations =====
const isAllSelected = computed(() => list.value.length > 0 && selectedIds.value.length === list.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < list.value.length)

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}

function toggleSelectAll() {
  if (isAllSelected.value) selectedIds.value = []
  else selectedIds.value = list.value.map(s => s.id)
}

function clearSelection() {
  selectedIds.value = []
}

async function batchToggleStar(val) {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  await handleBatchToggleStar(ids)
  clearSelection()
}

async function batchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  await handleBatchDelete(ids)
  clearSelection()
}

// ===== Format =====
function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
/* ===== Summary bar ===== */
.summary-bar {
  display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;
}
.summary-card {
  background: #fff; border-radius: 8px; padding: 14px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04); min-width: 140px;
}
.sc-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.sc-value { font-size: 20px; font-weight: 700; color: #333; }
.summary-card.gold .sc-value { color: #C5A265; }
.summary-card.green .sc-value { color: #52c41a; }
.summary-card.red .sc-value { color: #e74c3c; }

/* ===== Page layout ===== */
.admin-page {
  padding: 24px 28px 40px;
  background: var(--bg-page, #FAFAF8);
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 14px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-title, #1A1A1A);
  letter-spacing: 0.5px;
  position: relative;
  padding-left: 14px;
}

.page-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 22px;
  background: #C5A265;
  border-radius: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ===== Buttons ===== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  background: #C5A265;
  color: #fff;
  border: none;
  border-radius: var(--radius, 8px);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-family: inherit;
}

.btn-primary:hover {
  background: #D4B885;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(197, 162, 101, 0.35);
}

.btn-primary:active {
  transform: translateY(0) scale(0.97);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-icon {
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.btn-search {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  background: #fff;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius, 8px);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-search:hover {
  border-color: #C5A265;
  color: #C5A265;
}

.btn-search:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Search Input ===== */
.search-input {
  padding: 8px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius, 8px);
  font-size: 13px;
  outline: none;
  width: 220px;
  transition: all 0.2s;
  font-family: inherit;
}

.search-input:focus {
  border-color: #C5A265;
  box-shadow: 0 0 0 3px rgba(197, 162, 101, 0.1);
}

/* ===== Card & Table ===== */
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.03);
  overflow: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}

.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 13px;
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
  letter-spacing: 0.3px;
}

.data-table th.sortable { cursor: pointer; user-select: none; }
.data-table th.sortable:hover { color: #C5A265; }

.data-table td {
  padding: 13px 16px;
  font-size: 13px;
  border-bottom: 1px solid #f8f8f8;
  color: #444;
  vertical-align: middle;
}

.data-table tbody tr {
  transition: background 0.15s ease;
}

.data-table tbody tr:hover {
  background: #fafbfc;
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.col-id {
  width: 60px;
  color: #bbb;
  font-size: 12px;
}
.col-check {
  width: 40px;
  text-align: center;
  padding: 14px 8px;
}
.col-check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #C5A265;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fffbe6;
  border-bottom: 1px solid #ffe58f;
  font-size: 13px;
}
.batch-info {
  color: #666;
  margin-right: 4px;
}
.batch-info strong {
  color: #C5A265;
}
.btn-batch {
  padding: 5px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-batch:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.btn-batch.danger {
  color: #e74c3c;
  border-color: #f5c6cb;
}
.btn-batch.danger:hover {
  background: #fff1f0;
  border-color: #e74c3c;
}
.col-star {
  width: 36px;
  text-align: center;
  padding: 14px 8px;
}
.star-icon {
  cursor: pointer;
  font-size: 18px;
  color: #ddd;
  transition: all 0.2s;
  user-select: none;
}
.star-icon:hover {
  color: #f5a623;
  transform: scale(1.2);
}
.star-icon.starred {
  color: #f5a623;
}

.col-name {
  font-weight: 600;
  color: #2c2c2c;
}

.col-address {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-time {
  width: 150px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.col-action {
  width: 130px;
  white-space: nowrap;
}

.phone-link {
  color: #C5A265;
  text-decoration: none;
  font-variant-numeric: tabular-nums;
  transition: color 0.15s;
}

.phone-link:hover {
  color: #A8884E;
  text-decoration: underline;
}

.cell-na {
  color: #ccc;
  font-style: italic;
}

/* ===== Table Buttons ===== */
.btn-text {
  padding: 5px 12px;
  background: transparent;
  border: none;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-text:hover {
  background: #f5f5f5;
  color: #C5A265;
}

.btn-text.danger:hover {
  background: #fff1f0;
  color: #e74c3c;
}

/* ===== Empty State ===== */
.empty-state {
  text-align: center;
  padding: 64px 20px;
  color: #ccc;
}

.empty-icon {
  font-size: 44px;
  display: block;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
  color: #aaa;
}

/* ===== Loading Skeleton ===== */
.loading-wrap {
  padding: 16px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #f8f8f8;
}

.sk-cell {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-cell.w50 { width: 50px; }
.sk-cell.w100 { width: 100px; }
.sk-cell.w120 { width: 120px; }
.sk-cell.w140 { width: 140px; }
.sk-cell.w150 { width: 150px; }
.sk-cell.w200 { width: 200px; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

.pagination-wrap :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-hover-color: #C5A265;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #C5A265;
}

/* ===== Modal ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal {
  background: #fff;
  border-radius: 12px;
  width: 560px;
  max-width: 92vw;
  max-height: 88vh;
  overflow: hidden;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(24px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1A1A1A;
}

.modal-close {
  width: 30px;
  height: 30px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  color: #999;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  line-height: 1;
}

.modal-close:hover {
  background: #fff1f0;
  color: #e74c3c;
}

.modal-body {
  padding: 24px;
  max-height: calc(88vh - 70px);
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

/* ===== Form ===== */
.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: #555;
  margin-bottom: 7px;
  font-weight: 500;
}

.required {
  color: #e74c3c;
  font-size: 12px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius, 8px);
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  font-family: inherit;
  resize: vertical;
  background: #fff;
  color: #333;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #bbb;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #C5A265;
  box-shadow: 0 0 0 3px rgba(197, 162, 101, 0.1);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-error {
  display: block;
  font-size: 12px;
  color: #e74c3c;
  margin-top: 5px;
}

.btn-cancel {
  padding: 9px 22px;
  background: #fff;
  color: #888;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius, 8px);
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}

.btn-cancel:hover {
  border-color: #ccc;
  color: #555;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .admin-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
  }

  .search-input {
    width: 100%;
    flex: 1;
  }

  .modal {
    width: 96vw;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>

<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>客户管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <input v-model="keyword" class="search-input" placeholder="搜索客户名称 / 联系人 / 电话..." @input="onSearchInput" @keyup.enter="handleSearch" />
        <button class="btn-search" @click="handleSearch">搜索</button>
        <button class="btn-outline" @click="handleExport">导出</button>
        <button class="btn-outline" @click="importDialogVisible = true">导入</button>
        <button class="btn-primary" @click="openAdd">＋ 新增客户</button>
      </div>
    </div>

    <div class="summary-bar">
      <div class="summary-card"><div class="sc-label">客户总数</div><div class="sc-value">{{ total }} 个</div></div>
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

      <table class="data-table" v-if="list.length" v-loading="loading">
        <thead>
          <tr>
            <th class="col-check">
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" />
            </th>
            <th class="col-star"></th>
            <th class="col-id">ID</th>
            <th class="sortable" @click="toggleSort('name')">客户名称{{ sortArrow('name') }}</th>
            <th class="sortable" @click="toggleSort('contact')">联系人{{ sortArrow('contact') }}</th>
            <th class="sortable" @click="toggleSort('phone')">电话{{ sortArrow('phone') }}</th>
            <th>地址</th>
            <th class="col-time sortable" @click="toggleSort('createTime')">创建时间{{ sortArrow('createTime') }}</th>
            <th style="width:200px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in sortedList" :key="item.id" >
            <td class="col-check">
              <input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleSelect(item.id)" />
            </td>
            <td class="col-star">
              <span class="star-icon" :class="{ starred: item.isStarred === 1 }" @click="handleToggleStar(item)" :title="item.isStarred === 1 ? '取消星标' : '星标'">
                {{ item.isStarred === 1 ? '★' : '☆' }}
              </span>
            </td>
            <td class="col-id">{{ item.id }}</td>
            <td class="col-name">{{ item.name }}</td>
            <td>{{ item.contact || '-' }}</td>
            <td>{{ item.phone || '-' }}</td>
            <td class="col-address">{{ item.address || '-' }}</td>
            <td class="col-time">{{ formatDate(item.createTime) }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openFollowUp(item)">跟进</button>
              <button class="btn-text" @click="goCreateOrder(item)">销售单</button>
              <button class="btn-text" @click="openEdit(item)">编辑</button>
              <button class="btn-text danger" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <TableSkeleton v-else-if="loading" :rows="8" :columns="6" />
      <div v-else class="empty-state">
        <p>{{ keyword ? '未匹配到客户' : '暂无客户数据' }}</p>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑客户' : '新增客户'"
      width="560px"
      :close-on-click-modal="false"
      close-on-press-escape
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入客户名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" maxlength="200" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入客户"
      :column-map="importConfigs.customers.columns"
      :module="'customers'"
      :on-complete="loadList"
      :sample-data="importConfigs.customers.sample"
      template-filename="客户导入模板"
    />

    <FollowUpDrawer
      v-model="followupVisible"
      :customer-id="followupCustomerId"
      :customer-name="followupCustomerName"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCustomers, addCustomer, updateCustomer, deleteCustomer, toggleCustomerStar, restoreEntity } from '@/api/admin'

const route = useRoute()
const router = useRouter()
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { useUndoAction } from '@/composables/useUndoAction'
import { exportToExcel } from '@/utils/exportExcel'
import RefreshButton from '@/components/RefreshButton.vue'
import TableSkeleton from '@/components/TableSkeleton.vue'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import FollowUpDrawer from '@/components/FollowUpDrawer.vue'
import { importConfigs } from '@/utils/importConfigs'
import { useTableSort } from '@/composables/useTableSort'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

// ===== State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { createTime: 'date' }, { storageKey: 'customers_filters' })
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const importDialogVisible = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedIds = ref([])

const followupVisible = ref(false)
const followupCustomerId = ref(null)
const followupCustomerName = ref('')

const FILTER_KEY = 'customers_filters'
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
  if (pageSize.value !== 20) { query.size = String(pageSize.value); storage.size = pageSize.value }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

const dialogVisible = ref(false)
useCtrlEnterSave(dialogVisible, handleSave)
const editId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  contact: '',
  phone: '',
  address: '',
  remark: ''
})

const rules = {
  name: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { max: 100, message: '客户名称不能超过100个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^[\d\-+()]*$/, message: '请输入正确的电话号码', trigger: 'blur' }
  ]
}

// ===== Undo composables =====
const { handleDelete, handleBatchDelete } = useUndoDelete({
  deleteApi: deleteCustomer,
  restoreApi: (id) => restoreEntity('customer', id),
  onSuccess: () => { if (list.value.length <= 1 && currentPage.value > 1) currentPage.value--; loadList() },
  entityLabel: '客户',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateCustomer,
  onSuccess: loadList,
  entityLabel: '客户',
})

const { handleBatchAction: handleBatchToggleStar } = useUndoAction({
  actionApi: toggleCustomerStar,
  reverseApi: toggleCustomerStar,
  onSuccess: loadList,
  entityLabel: '客户',
  actionLabel: '标星',
})

// ===== Lifecycle =====
onMounted(() => { restoreFilters(); loadList() })

// ===== Methods =====
function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadList() {
  loading.value = true
  try {
    const res = await getCustomers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined
    })
    const data = res.data?.data || res.data
    list.value = data.records || []
    total.value = data.total || 0
  } catch (err) {
    console.error('加载客户列表失败:', err)
    if (!err._handled) ElMessage.error('加载客户列表失败，请稍后重试')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
    syncFiltersToUrl()
  }
}

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadList()
  }, 350)
}

function handleSearch() {
  clearTimeout(searchTimer)
  currentPage.value = 1
  loadList()
}

function resetForm() {
  form.name = ''
  form.contact = ''
  form.phone = ''
  form.address = ''
  form.remark = ''
}

async function handleExport() {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const columns = [
    { label: '客户名称', key: 'name', width: 16 },
    { label: '联系人', key: 'contact', width: 12 },
    { label: '电话', key: 'phone', width: 14 },
    { label: '地址', key: 'address', width: 24 },
    { label: '备注', key: 'remark', width: 20 },
  ]
  await exportToExcel(list.value, columns, '客户列表', { sheetName: '客户' })
}

function openAdd() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openFollowUp(item) {
  followupCustomerId.value = item.id
  followupCustomerName.value = item.name
  followupVisible.value = true
}

function goCreateOrder(item) {
  router.push({
    path: '/admin/orders/sale',
    query: {
      create: '1',
      customerName: item.name || '',
      customerPhone: item.phone || '',
      customerAddress: item.address || ''
    }
  })
}

function openEdit(row) {
  snapshotBeforeEdit(row)
  editId.value = row.id
  form.name = row.name || ''
  form.contact = row.contact || ''
  form.phone = row.phone || ''
  form.address = row.address || ''
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const data = {
      name: form.name.trim(),
      contact: form.contact.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
      remark: form.remark.trim()
    }
    if (editId.value) {
      await updateCustomer(editId.value, data)
      dialogVisible.value = false
      afterEditSave({ id: editId.value, ...data })
      loadList()
    } else {
      await addCustomer(data)
      ElMessage.success('客户已添加')
      dialogVisible.value = false
      loadList()
    }
  } catch (err) {
    console.error('保存客户失败:', err)
    if (!err._handled) ElMessage.error(editId.value ? '更新失败，请稍后重试' : '添加失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function handleToggleStar(row) {
  try {
    await toggleCustomerStar(row.id)
    row.isStarred = row.isStarred === 1 ? 0 : 1
    ElMessage.success(row.isStarred === 1 ? '已标星' : '已取消星标')
  } catch (err) {
    console.error('标星操作失败:', err)
    if (!err._handled) ElMessage.error('操作失败，请稍后重试')
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
  else selectedIds.value = list.value.map(item => item.id)
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

/* ===== Page header ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ===== Search & Buttons ===== */
.search-input {
  padding: 8px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  outline: none;
  width: 240px;
  transition: border-color 0.2s;
  font-family: inherit;
}
.search-input:focus {
  border-color: #C5A265;
}
.search-input::placeholder {
  color: #bbb;
}

.btn-search {
  padding: 8px 18px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-search:hover {
  border-color: #C5A265;
  color: #C5A265;
}

.btn-primary {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #C5A265;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}
.btn-primary:hover {
  background: #d4b885;
}
.btn-outline {
  padding: 7px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-outline:hover {
  border-color: #C5A265;
  color: #C5A265;
}

/* ===== Card / Table ===== */
.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 12px;
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
}
.data-table td {
  padding: 14px 16px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
  vertical-align: middle;
}
.data-table tbody tr {
  transition: background 0.15s;
}
.data-table tbody tr:hover {
  background: #fafbfc;
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
  color: #1a1a1a;
}
.col-address {
  max-width: 200px;
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
  white-space: nowrap;
}

.btn-text {
  padding: 4px 10px;
  border: none;
  background: none;
  color: #C5A265;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
  font-family: inherit;
}
.btn-text:hover {
  background: rgba(197, 162, 101, 0.08);
}
.btn-text.danger {
  color: #e74c3c;
}
.btn-text.danger:hover {
  background: rgba(231, 76, 60, 0.06);
}

/* ===== Empty state ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
  font-size: 14px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f5f5f5;
}

/* ===== Dialog gold button ===== */
.btn-gold {
  background: #C5A265 !important;
  border-color: #C5A265 !important;
}
.btn-gold:hover,
.btn-gold:focus {
  background: #d4b885 !important;
  border-color: #d4b885 !important;
}

/* ===== Element Plus overrides ===== */
:deep(.el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 24px;
  margin-right: 0;
}
:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
:deep(.el-dialog__body) {
  padding: 24px;
}
:deep(.el-dialog__footer) {
  border-top: 1px solid #f0f0f0;
  padding: 14px 24px;
}
:deep(.el-form-item__label) {
  font-size: 13px;
  color: #555;
}
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 6px;
}
:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #C5A265;
}
:deep(.el-pagination.is-background .el-pager li:not(.is-active):hover) {
  color: #C5A265;
}
:deep(.el-loading-mask) {
  border-radius: 10px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-right {
    width: 100%;
    flex-wrap: wrap;
  }
  .search-input {
    width: 100%;
    flex: 1;
    min-width: 160px;
  }
  .col-address {
    max-width: 120px;
  }
}

/* ===== Sortable columns ===== */
.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}
.data-table th.sortable:hover {
  color: #C5A265;
}
</style>

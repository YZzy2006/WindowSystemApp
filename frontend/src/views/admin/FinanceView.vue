<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>收付款管理</h2>
    </div>

    <!-- Summary Cards -->
    <div class="summary-grid">
      <div class="summary-card" style="--accent: #52c41a">
        <div class="summary-label">总收款</div>
        <div class="summary-value">{{ formatMoney(paymentSummary.totalReceipt) }}</div>
      </div>
      <div class="summary-card" style="--accent: #e74c3c">
        <div class="summary-label">总付款</div>
        <div class="summary-value">{{ formatMoney(paymentSummary.totalPayment) }}</div>
      </div>
      <div class="summary-card" style="--accent: #1677ff">
        <div class="summary-label">净收入</div>
        <div class="summary-value">{{ formatMoney(paymentSummary.net) }}</div>
      </div>
      <div class="summary-card" style="--accent: #faad14">
        <div class="summary-label">期末余额</div>
        <div class="summary-value">{{ formatMoney(paymentSummary.closingBalance) }}</div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="paymentFilter.type" placeholder="全部类型" clearable style="width: 120px" @change="onTypeChange">
            <el-option label="全部" value="" />
            <el-option label="收款" value="receipt" />
            <el-option label="付款" value="payment" />
          </el-select>
          <el-date-picker
            v-model="paymentFilter.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            format="YYYY年MM月DD日"
            style="width: 155px"
            clearable
            @change="val => { if (!validateDateRange(val, paymentFilter.endDate, 'start')) paymentFilter.startDate = null }"
          />
          <span style="margin: 0 4px; color: #999;">至</span>
          <el-date-picker
            v-model="paymentFilter.endDate"
            type="date"
            placeholder="截止日期"
            value-format="YYYY-MM-DD"
            format="YYYY年MM月DD日"
            style="width: 155px"
            clearable
            @change="val => { if (!validateDateRange(paymentFilter.startDate, val, 'end')) paymentFilter.endDate = null }"
          />
          <el-input
            v-model="paymentFilter.keyword"
            placeholder="搜索单号 / 对方名称..."
            clearable
            style="width: 220px"
            @input="handlePaymentSearch"
            @clear="handlePaymentSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" class="btn-gold" @click="handlePaymentSearch">搜索</el-button>
        </div>
        <div style="display:flex;gap:10px">
          <RefreshButton :on-refresh="loadPayments" />
          <button class="btn-outline" @click="handleExportPayments">导出Excel</button>
          <button class="btn-outline" @click="importPaymentVisible = true">导入</button>
          <el-button type="primary" class="btn-gold" @click="openAddPayment">＋ 新增收付款</el-button>
        </div>
      </div>

      <!-- Batch action bar -->
      <div class="batch-bar" v-if="selectedIds.length > 0">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 项</span>
        <button class="btn-batch danger" @click="batchDeleteSelected">🗑 批量删除</button>
        <button class="btn-batch" @click="clearSelection">取消选择</button>
      </div>

      <!-- Table -->
      <el-table
        v-loading="paymentLoading"
        :data="sortedList"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#243447', color: 'rgba(255,255,255,0.85)', fontWeight: 600, fontSize: '12px', borderBottomColor: 'rgba(255,255,255,0.08)' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="单号" width="80" />
        <el-table-column prop="orderNo" min-width="130">
          <template #header>
            <span class="sortable" @click="toggleSort('orderNo')">关联订单号 {{ sortArrow('orderNo') }}</span>
          </template>
          <template #default="{ row }">
            {{ row.orderNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="结清" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.orderNo" class="clear-badge" :class="row.orderCleared ? 'cleared' : 'uncleared'">{{ row.orderCleared ? '已结清' : '未结清' }}</span>
            <span v-else style="color:#ccc;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentDate" width="120">
          <template #header>
            <span class="sortable" @click="toggleSort('paymentDate')">日期 {{ sortArrow('paymentDate') }}</span>
          </template>
          <template #default="{ row }">
            {{ formatDate(row.paymentDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'receipt' ? 'success' : 'danger'" size="small" effect="dark" round>
              {{ row.type === 'receipt' ? '收款' : '付款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" width="120" align="right">
          <template #header>
            <span class="sortable" @click="toggleSort('amount')">金额 {{ sortArrow('amount') }}</span>
          </template>
          <template #default="{ row }">
            <span class="money" :class="{ 'money-in': row.type === 'receipt', 'money-out': row.type === 'payment' }">
              {{ row.type === 'receipt' ? '+' : '-' }}{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="partyName" min-width="130">
          <template #header>
            <span class="sortable" @click="toggleSort('partyName')">对方名称 {{ sortArrow('partyName') }}</span>
          </template>
          <template #default="{ row }">
            {{ row.partyName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" min-width="150" show-overflow-tooltip>
          <template #header>
            <span class="sortable" @click="toggleSort('remark')">备注 {{ sortArrow('remark') }}</span>
          </template>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link class="link-gold" @click="openEditPayment(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeletePayment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Skeleton -->
      <TableSkeleton v-if="paymentLoading && paymentList.length === 0" :rows="8" :columns="7" />

      <!-- Empty -->
      <div v-if="!paymentLoading && paymentList.length === 0" class="empty-state">
        <p>{{ hasFilter ? '未匹配到收付款记录' : '暂无收付款记录' }}</p>
      </div>

      <!-- Pagination -->
      <div class="pagination-wrap" v-if="paymentTotal > 0">
        <el-pagination
          v-model:current-page="paymentPage"
          v-model:page-size="paymentSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="paymentTotal"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadPayments"
          @current-change="loadPayments"
        />
      </div>
    </div>

    <!-- ===== 收付款 Dialog ===== -->
    <el-dialog
      v-model="paymentDialogVisible"
      :title="paymentEditId ? '编辑收付款' : '新增收付款'"
      width="560px"
      :close-on-click-modal="false"
      close-on-press-escape
      destroy-on-close
    >
      <el-form
        ref="paymentFormRef"
        :model="paymentForm"
        :rules="paymentRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="关联订单" prop="orderNo">
          <el-select
            v-model="paymentForm.orderNo"
            filterable
            remote
            :remote-method="searchUnpaidOrders"
            :loading="unpaidLoading"
            placeholder="输入订单号或客户名搜索"
            clearable
            allow-create
            default-first-option
            style="width: 100%"
            @change="handleOrderSelect"
          >
            <el-option
              v-for="o in unpaidOrders"
              :key="o.orderNo"
              :label="o.orderNo"
              :value="o.orderNo"
            >
              <span>{{ o.orderNo }}</span>
              <span style="float: right; color: #999; font-size: 12px">{{ o.customerName || '' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <div v-if="selectedOrderInfo" class="order-summary-card">
          <div class="order-summary-row">
            <div class="order-summary-item">
              <span class="order-summary-label">订单金额</span>
              <span class="order-summary-val">¥{{ formatMoney(selectedOrderInfo.totalAmount) }}</span>
            </div>
            <div class="order-summary-item">
              <span class="order-summary-label">定金</span>
              <span class="order-summary-val deposit">¥{{ formatMoney(selectedOrderInfo.deposit) }}</span>
            </div>
          </div>
          <div class="order-summary-row">
            <div class="order-summary-item">
              <span class="order-summary-label">{{ selectedOrderInfo.orderType === 'purchase' || selectedOrderInfo.orderType === 'purchase_return' ? '已付' : '已收' }}</span>
              <span class="order-summary-val paid">¥{{ formatMoney(selectedOrderInfo.paidAmount) }}</span>
            </div>
            <div class="order-summary-item">
              <span class="order-summary-label">{{ selectedOrderInfo.orderType === 'purchase' || selectedOrderInfo.orderType === 'purchase_return' ? '未付' : '未收' }}</span>
              <span class="order-summary-val unpaid">¥{{ formatMoney(selectedOrderInfo.unpaidAmount) }}</span>
            </div>
          </div>
          <div class="order-summary-progress">
            <div class="order-summary-bar">
              <div class="order-summary-bar-fill" :style="{ width: selectedOrderInfo.totalAmount > 0 ? Math.min(selectedOrderInfo.paidAmount / selectedOrderInfo.totalAmount * 100, 100).toFixed(1) + '%' : '0%' }"></div>
            </div>
            <span class="order-summary-pct">{{ selectedOrderInfo.totalAmount > 0 ? Math.min(selectedOrderInfo.paidAmount / selectedOrderInfo.totalAmount * 100, 100).toFixed(1) : '0.0' }}%</span>
          </div>
        </div>
        <el-form-item label="收付日期" prop="paymentDate">
          <el-date-picker
            v-model="paymentForm.paymentDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            format="YYYY年MM月DD日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="paymentForm.type">
            <el-radio value="receipt">收款</el-radio>
            <el-radio value="payment">付款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="paymentForm.amount"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
            placeholder="请输入金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="对方名称" prop="partyName">
          <el-input v-model="paymentForm.partyName" placeholder="请输入对方名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="paymentForm.remark" type="textarea" :rows="3" placeholder="备注信息" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="paymentSaving" @click="handleSavePayment">保存</el-button>
      </template>
    </el-dialog>

    <ImportExcelDialog
      v-model="importPaymentVisible"
      title="导入收付款记录"
      :column-map="importConfigs.payments.columns"
      :module="'payments'"
      :on-complete="loadPayments"
      :sample-data="importConfigs.payments.sample"
      template-filename="收付款导入模板"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { exportToExcel, fmtDate, fmtMoney as fmtMoneyExport } from '@/utils/exportExcel'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import TableSkeleton from '@/components/TableSkeleton.vue'
import { importConfigs } from '@/utils/importConfigs'
import {
  getPayments,
  addPayment,
  updatePayment,
  deletePayment,
  getPaymentSummary,
  getUnpaidOrders,
  restoreEntity,
} from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { useTableSort } from '@/composables/useTableSort'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

// ==================== URL 筛选状态持久化 ====================
const route = useRoute()
const router = useRouter()
const FINANCE_FILTER_KEY = 'finance_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.type || q.keyword || q.startDate || q.endDate || q.page
  if (hasUrlParams) {
    _restoring = true
    if (q.type) paymentFilter.type = q.type
    if (q.keyword) paymentFilter.keyword = q.keyword
    if (q.startDate) paymentFilter.startDate = q.startDate
    if (q.endDate) paymentFilter.endDate = q.endDate
    if (q.page && Number(q.page) > 1) paymentPage.value = Number(q.page)
    if (q.size && Number(q.size) > 0) paymentSize.value = Number(q.size)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FINANCE_FILTER_KEY) || '{}')
    _restoring = true
    if (saved.type) paymentFilter.type = saved.type
    if (saved.keyword) paymentFilter.keyword = saved.keyword
    if (saved.startDate) paymentFilter.startDate = saved.startDate
    if (saved.endDate) paymentFilter.endDate = saved.endDate
    if (saved.page && saved.page > 1) paymentPage.value = saved.page
    if (saved.size && saved.size > 0) paymentSize.value = saved.size
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (paymentFilter.type) { query.type = paymentFilter.type; storage.type = paymentFilter.type }
  if (paymentFilter.keyword) { query.keyword = paymentFilter.keyword; storage.keyword = paymentFilter.keyword }
  if (paymentFilter.startDate) { query.startDate = paymentFilter.startDate; storage.startDate = paymentFilter.startDate }
  if (paymentFilter.endDate) { query.endDate = paymentFilter.endDate; storage.endDate = paymentFilter.endDate }
  if (paymentPage.value > 1) { query.page = String(paymentPage.value); storage.page = paymentPage.value }
  if (paymentSize.value !== 20) { query.size = String(paymentSize.value); storage.size = paymentSize.value }
  router.replace({ query })
  sessionStorage.setItem(FINANCE_FILTER_KEY, JSON.stringify(storage))
}

// ==================== Common ====================
function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatMoney(val) {
  if (val == null || isNaN(val)) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ==================== Undo composables ====================
const { handleDelete: undoDeletePayment, handleBatchDelete: batchDeletePayments } = useUndoDelete({
  deleteApi: deletePayment,
  restoreApi: (id) => restoreEntity('payment', id),
  onSuccess: () => { loadPayments(); loadPaymentSummary() },
  entityLabel: '收付款',
})

const { snapshotBeforeEdit: snapshotPayment, afterEditSave: afterPaymentSave } = useUndoEdit({
  updateApi: updatePayment,
  onSuccess: () => { loadPayments(); loadPaymentSummary() },
  entityLabel: '收付款',
})

// ==================== Payment (收付款) ====================
const paymentList = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(paymentList, {
  paymentDate: 'date',
  amount: 'number',
}, { storageKey: 'finance_filters' })
const importPaymentVisible = ref(false)
const paymentLoading = ref(false)
const paymentPage = ref(1)
const paymentSize = ref(20)
const paymentTotal = ref(0)

const paymentFilter = reactive({
  type: '',
  startDate: null,
  endDate: null,
  keyword: ''
})

const hasFilter = computed(() => !!(paymentFilter.type || paymentFilter.keyword.trim() || paymentFilter.startDate || paymentFilter.endDate))

watch(() => paymentFilter.startDate, handlePaymentSearch)
watch(() => paymentFilter.endDate, handlePaymentSearch)
watch(paymentPage, handlePageChange)
watch(paymentSize, handleSizeChange)

const paymentSummary = reactive({
  openingBalance: 0,
  totalReceipt: 0,
  totalPayment: 0,
  net: 0,
  closingBalance: 0
})

const paymentDialogVisible = ref(false)
useCtrlEnterSave(paymentDialogVisible, handleSavePayment)
const paymentEditId = ref(null)
const paymentSaving = ref(false)
const paymentFormRef = ref(null)

const paymentForm = reactive({
  orderId: null,
  orderNo: '',
  orderType: '',
  paymentDate: '',
  type: 'receipt',
  amount: 0,
  partyName: '',
  remark: ''
})
const selectedOrderInfo = ref(null)

const paymentRules = {
  paymentDate: [{ required: true, message: '请选择收付日期', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

const unpaidOrders = ref([])
const unpaidLoading = ref(false)
const selectedIds = ref([])

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}
function clearSelection() {
  selectedIds.value = []
}

async function batchDeleteSelected() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  await batchDeletePayments(ids)
  clearSelection()
}

onMounted(() => {
  restoreFilters()
  loadPayments()
  loadPaymentSummary()
})

async function loadPayments() {
  paymentLoading.value = true
  try {
    const params = {
      page: paymentPage.value,
      size: paymentSize.value
    }
    if (paymentFilter.type) params.type = paymentFilter.type
    if (paymentFilter.keyword.trim()) params.keyword = paymentFilter.keyword.trim()
    if (paymentFilter.startDate) params.startDate = paymentFilter.startDate
    if (paymentFilter.endDate) params.endDate = paymentFilter.endDate
    const res = await getPayments(params)
    const data = res.data?.data || res.data
    paymentList.value = data.records || []
    paymentTotal.value = data.total || 0
  } catch (err) {
    console.error('加载收付款列表失败:', err)
    if (!err._handled) ElMessage.error('加载收付款列表失败')
    paymentList.value = []
    paymentTotal.value = 0
  } finally {
    paymentLoading.value = false
  }
}

async function loadPaymentSummary() {
  try {
    const params = {}
    if (paymentFilter.type) params.type = paymentFilter.type
    if (paymentFilter.keyword.trim()) params.keyword = paymentFilter.keyword.trim()
    if (paymentFilter.startDate) params.startDate = paymentFilter.startDate
    if (paymentFilter.endDate) params.endDate = paymentFilter.endDate
    const res = await getPaymentSummary(params)
    const data = res.data?.data || res.data || {}
    paymentSummary.openingBalance = data.openingBalance || 0
    paymentSummary.totalReceipt = data.totalReceipt || 0
    paymentSummary.totalPayment = data.totalPayment || 0
    paymentSummary.net = data.net || 0
    paymentSummary.closingBalance = data.closingBalance || 0
  } catch (err) {
    console.error('加载收付款汇总失败:', err)
  }
}

let _paymentSearchTimer = null
function handlePaymentSearch() {
  clearTimeout(_paymentSearchTimer)
  _paymentSearchTimer = setTimeout(() => {
    _restoring = true
    paymentPage.value = 1
    _restoring = false
    loadPayments()
    loadPaymentSummary()
    syncFiltersToUrl()
  }, 300)
}

function onTypeChange() {
  handlePaymentSearch()
}

function handlePageChange() {
  if (_restoring) return
  loadPayments()
  syncFiltersToUrl()
}

function handleSizeChange() {
  if (_restoring) return
  _restoring = true
  paymentPage.value = 1
  _restoring = false
  loadPayments()
  syncFiltersToUrl()
}

function resetPaymentForm() {
  paymentForm.orderId = null
  paymentForm.orderNo = ''
  paymentForm.orderType = ''
  paymentForm.paymentDate = ''
  paymentForm.type = 'receipt'
  paymentForm.amount = 0
  paymentForm.partyName = ''
  paymentForm.remark = ''
  selectedOrderInfo.value = null
}

function openAddPayment() {
  paymentEditId.value = null
  resetPaymentForm()
  unpaidOrders.value = []
  paymentDialogVisible.value = true
}

function openEditPayment(row) {
  snapshotPayment(row)
  paymentEditId.value = row.id
  paymentForm.orderId = row.orderId || null
  paymentForm.orderNo = row.orderNo || ''
  paymentForm.orderType = row.orderType || ''
  paymentForm.paymentDate = row.paymentDate || ''
  paymentForm.type = row.type || 'receipt'
  paymentForm.amount = row.amount || 0
  paymentForm.partyName = row.partyName || ''
  paymentForm.remark = row.remark || ''
  unpaidOrders.value = []
  selectedOrderInfo.value = null
  paymentDialogVisible.value = true
}

let _unpaidSearchTimer = null
let _unpaidSearchSeq = 0
function searchUnpaidOrders(query) {
  clearTimeout(_unpaidSearchTimer)
  if (!query || !query.trim()) {
    unpaidOrders.value = []
    return
  }
  _unpaidSearchTimer = setTimeout(async () => {
    const seq = ++_unpaidSearchSeq
    unpaidLoading.value = true
    try {
      const res = await getUnpaidOrders({ keyword: query.trim() })
      if (seq !== _unpaidSearchSeq) return
      unpaidOrders.value = res.data?.data || res.data || []
    } catch (err) {
      console.error('搜索未付款订单失败:', err)
      if (seq === _unpaidSearchSeq) unpaidOrders.value = []
    } finally {
      if (seq === _unpaidSearchSeq) unpaidLoading.value = false
    }
  }, 250)
}

function handleOrderSelect(orderNo) {
  if (!orderNo) {
    paymentForm.orderId = null
    paymentForm.orderType = ''
    selectedOrderInfo.value = null
    return
  }
  const found = unpaidOrders.value.find(o => o.orderNo === orderNo)
  if (found) {
    paymentForm.orderId = found.orderId || found.id || null
    paymentForm.orderType = found.orderType || ''
    if (found.customerName && !paymentForm.partyName) {
      paymentForm.partyName = found.customerName
    }
    selectedOrderInfo.value = {
      totalAmount: Number(found.totalAmount) || 0,
      paidAmount: Number(found.paidAmount) || 0,
      unpaidAmount: Number(found.unpaidAmount) || 0,
      deposit: Number(found.deposit) || 0,
      orderType: found.orderType || '',
    }
  } else {
    paymentForm.orderId = null
    paymentForm.orderType = ''
    selectedOrderInfo.value = null
  }
}

async function handleSavePayment() {
  if (!paymentFormRef.value) return
  try {
    await paymentFormRef.value.validate()
  } catch {
    return
  }

  if (paymentForm.amount <= 0) {
    ElMessage.warning('金额必须大于 0')
    return
  }

  paymentSaving.value = true
  try {
    const data = {
      orderId: paymentForm.orderId,
      orderNo: paymentForm.orderNo || undefined,
      orderType: paymentForm.orderType || undefined,
      paymentDate: paymentForm.paymentDate,
      type: paymentForm.type,
      amount: paymentForm.amount,
      partyName: paymentForm.partyName.trim() || undefined,
      remark: paymentForm.remark.trim() || undefined
    }
    if (paymentEditId.value) {
      await updatePayment(paymentEditId.value, data)
      afterPaymentSave({ id: paymentEditId.value, ...data })
    } else {
      await addPayment(data)
      ElMessage.success('收付款记录已添加')
    }
    paymentDialogVisible.value = false
    loadPayments()
    loadPaymentSummary()
  } catch (err) {
    console.error('保存收付款失败:', err)
    if (!err._handled) ElMessage.error(paymentEditId.value ? '更新失败，请稍后重试' : '添加失败，请稍后重试')
  } finally {
    paymentSaving.value = false
  }
}

function handleDeletePayment(row) { undoDeletePayment(row) }

// ==================== Export ====================
async function handleExportPayments() {
  if (!paymentList.value.length) {
    ElMessage.warning('暂无收付款数据可导出')
    return
  }
  const columns = [
    { key: 'id', label: '单号', width: 10 },
    { key: 'orderNo', label: '关联订单号', width: 18 },
    { key: 'paymentDate', label: '日期', width: 14, format: fmtDate },
    { key: 'type', label: '类型', width: 10, format: (v) => v === 'receipt' ? '收款' : '付款' },
    { key: 'amount', label: '金额', width: 14, format: fmtMoneyExport },
    { key: 'partyName', label: '对方名称', width: 18 },
    { key: 'remark', label: '备注', width: 24 },
  ]
  await exportToExcel(paymentList.value, columns, '收付款记录', { sheetName: '收支记录' })
}
</script>

<style scoped>
/* ===== Page Header ===== */
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

/* ===== Summary Cards ===== */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.summary-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 10px;
  padding: 20px 24px;
  border-left: 4px solid var(--accent);
  border-top: 1px solid rgba(255,255,255,0.06);
  border-right: 1px solid rgba(255,255,255,0.06);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.summary-label {
  font-size: 12px;
  color: rgba(255,255,255,0.50);
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}
.summary-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}

/* ===== Card ===== */
.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* ===== Toolbar ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
  flex-wrap: wrap;
  gap: 10px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
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

/* ===== Money ===== */
.money {
  font-weight: 600;
  font-size: 13px;
}
.money-in {
  color: #52c41a;
}
.money-out {
  color: #e74c3c;
}
.clear-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.clear-badge.cleared {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.clear-badge.uncleared {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}

/* ===== Empty State ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
  font-size: 14px;
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f5f5f5;
}

/* ===== Gold Button ===== */
.btn-gold {
  background: #C5A265 !important;
  border-color: #C5A265 !important;
}
.btn-gold:hover,
.btn-gold:focus {
  background: #d4b885 !important;
  border-color: #d4b885 !important;
}

.btn-outline {
  padding: 8px 18px;
  background: #fff;
  color: #C5A265;
  border: 1px solid #C5A265;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-outline:hover {
  background: rgba(197, 162, 101, 0.06);
}
.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Gold Link ===== */
.link-gold {
  color: #C5A265 !important;
}
.link-gold:hover {
  color: #d4b885 !important;
}

/* ===== Element Plus Overrides ===== */
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
:deep(.el-table) {
  --el-table-border-color: #f5f5f5;
}
:deep(.el-table td.el-table__cell) {
  font-size: 13px;
  color: #444;
}
:deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #C5A265;
  background-color: #C5A265;
}
:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #C5A265;
}

/* ===== Order Summary Card ===== */
.order-summary-card {
  background: #f8f9fb;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 14px 18px;
  margin: 0 0 18px 0;
}
.order-summary-row {
  display: flex;
  gap: 24px;
  margin-bottom: 8px;
}
.order-summary-row:last-of-type {
  margin-bottom: 0;
}
.order-summary-item {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-summary-label {
  font-size: 13px;
  color: #666;
}
.order-summary-val {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  font-variant-numeric: tabular-nums;
}
.order-summary-val.deposit {
  color: #C5A265;
}
.order-summary-val.paid {
  color: #67c23a;
}
.order-summary-val.unpaid {
  color: #e6a23c;
}
.order-summary-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
.order-summary-bar {
  flex: 1;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}
.order-summary-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #85ce61);
  border-radius: 4px;
  transition: width 0.3s ease;
}
.order-summary-pct {
  font-size: 12px;
  font-weight: 600;
  color: #67c23a;
  min-width: 45px;
  text-align: right;
}

/* ===== Sortable Headers ===== */
.sortable {
  cursor: pointer;
  user-select: none;
}
.sortable:hover {
  color: #C5A265;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  .toolbar-left {
    width: 100%;
  }
}
</style>

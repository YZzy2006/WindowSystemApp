<template>
  <div class="page admin-page">
    <!-- ===== Header ===== -->
    <div class="page-header">
      <h2>出库管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <button class="btn-outline" @click="handleExport">导出Excel</button>
        <button class="btn-outline" @click="importDialogVisible = true">导入</button>
        <button class="btn-outline" @click="handlePrint">打印</button>
        <button class="btn-primary" @click="openAdd">+ 新建出库单</button>
      </div>
    </div>

    <!-- ===== Toolbar ===== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="date-range">
          <el-date-picker
            v-model="startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            format="YYYY年MM月DD日"
            size="default"
            :clearable="true"
            @change="onStartDateChange"
          />
          <span class="date-sep">~</span>
          <el-date-picker
            v-model="endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            format="YYYY年MM月DD日"
            size="default"
            :clearable="true"
            @change="onEndDateChange"
          />
        </div>
      </div>
      <div class="toolbar-right">
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            v-model="keyword"
            placeholder="搜单号 / 申请人 / 仓管员..."
            class="search-input"
            @input="onSearchInput"
          />
        </div>
        <button class="btn-search" @click="handleSearch" :disabled="loading">搜索</button>
        <button class="btn-reset" @click="handleReset" :disabled="loading">重置</button>
      </div>
    </div>

    <!-- ===== Summary Bar ===== -->
    <div class="summary-bar" v-if="summary">
      <div class="summary-card">
        <div class="sc-label">出库单数</div>
        <div class="sc-value">{{ summary.totalCount ?? 0 }} 单</div>
      </div>
      <div class="summary-card">
        <div class="sc-label">总数量</div>
        <div class="sc-value">{{ summary.totalQuantity ?? 0 }}</div>
      </div>
      <div class="summary-card gold">
        <div class="sc-label">总金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalAmount) }}</div>
      </div>
    </div>

    <!-- ===== Batch Bar ===== -->
    <div v-if="selectedIds.size" class="batch-bar">
      <span class="batch-info">已选 {{ selectedIds.size }} 项</span>
      <button class="btn-batch-danger" @click="handleBatchDelete" :disabled="batchOperating">{{ batchOperating ? '处理中...' : '批量删除' }}</button>
      <button class="btn-batch" @click="clearSelection">取消选择</button>
    </div>

    <!-- ===== Table ===== -->
    <div class="card">
      <div v-if="loading" class="loading-wrap">
        <div class="skeleton-row" v-for="i in 8" :key="i">
          <span class="sk-cell w80"></span>
          <span class="sk-cell w120"></span>
          <span class="sk-cell w100"></span>
          <span class="sk-cell w100"></span>
          <span class="sk-cell w100"></span>
          <span class="sk-cell w140"></span>
          <span class="sk-cell w150"></span>
          <span class="sk-cell w100"></span>
        </div>
      </div>

      <table class="data-table" v-else-if="list.length">
        <thead>
          <tr>
            <th style="width:40px"><input type="checkbox" :checked="isAllSelected" :indeterminate.prop="isIndeterminate" @change="toggleSelectAll" /></th>
            <th class="col-orderno sortable" @click="toggleSort('orderNo')">单号 {{ sortArrow('orderNo') }}</th>
            <th class="col-date sortable" @click="toggleSort('orderDate')">日期 {{ sortArrow('orderDate') }}</th>
            <th class="sortable" @click="toggleSort('applicant')">申请人 {{ sortArrow('applicant') }}</th>
            <th>仓管员</th>
            <th>经办人</th>
            <th class="col-remark">备注</th>
            <th class="col-date sortable" @click="toggleSort('createTime')">创建时间 {{ sortArrow('createTime') }}</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in sortedList" :key="row.id" >
            <td><input type="checkbox" :checked="selectedIds.has(row.id)" @change="toggleSelect(row.id)" /></td>
            <td class="col-orderno">
              <span class="order-no" @click="openDetail(row)">{{ row.orderNo }}</span>
            </td>
            <td class="col-date">{{ formatDate(row.orderDate) }}</td>
            <td>{{ row.applicant || '-' }}</td>
            <td>{{ row.warehouseKeeper || '-' }}</td>
            <td>{{ row.operator || '-' }}</td>
            <td class="col-remark">
              <span class="remark-text" :title="row.remark">{{ row.remark || '-' }}</span>
            </td>
            <td class="col-date">{{ formatDateTime(row.createTime) }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openDetail(row)">详情</button>
              <button class="btn-text" @click="openEdit(row)">编辑</button>
              <button class="btn-text danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <p>{{ hasFilter ? '未匹配到出库单' : '暂无出库单，点击右上角新建' }}</p>
      </div>
    </div>

    <!-- ===== Pagination ===== -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- ===== Detail Drawer ===== -->
    <el-drawer
      v-model="drawerVisible"
      title="出库单详情"
      size="720px"
      close-on-press-escape
      :destroy-on-close="true"
      @closed="router.replace({ query: { ...route.query, detailId: undefined } })"
    >
      <template #footer>
        <el-button @click="handlePrint">打印出库单</el-button>
      </template>
      <div class="drawer-content" v-if="detailOrder">
        <!-- Order Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>出库信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">出库单号</span>
              <span class="detail-val mono">{{ detailOrder.orderNo }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">出库日期</span>
              <span class="detail-val">{{ formatDate(detailOrder.orderDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请人</span>
              <span class="detail-val">{{ detailOrder.applicant || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">仓管员</span>
              <span class="detail-val">{{ detailOrder.warehouseKeeper || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">经办人</span>
              <span class="detail-val">{{ detailOrder.operator || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">创建时间</span>
              <span class="detail-val">{{ formatDateTime(detailOrder.createTime) }}</span>
            </div>
          </div>
        </div>

        <!-- Items Table -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>商品明细</h4>
          <div class="detail-items-table-wrap">
            <table class="detail-items-table" v-if="detailItems.length">
              <thead>
                <tr>
                  <th>#</th>
                  <th>商品名称</th>
                  <th>分类</th>
                  <th>规格</th>
                  <th>单位</th>
                  <th>库位</th>
                  <th class="col-num">数量</th>
                  <th class="col-num">单价</th>
                  <th class="col-num">金额</th>
                  <th>备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in detailItems" :key="idx">
                  <td class="row-num">{{ idx + 1 }}</td>
                  <td>{{ item.productName || '-' }}</td>
                  <td>{{ item.productCategory || '-' }}</td>
                  <td>{{ item.spec || '-' }}</td>
                  <td>{{ item.unit || '-' }}</td>
                  <td>{{ item.warehouseLoc || '-' }}</td>
                  <td class="col-num">{{ item.quantity ?? 0 }}</td>
                  <td class="col-num">¥{{ formatMoney(item.unitPrice) }}</td>
                  <td class="col-num money-gold">¥{{ formatMoney(item.amount ?? multiplyMoney(item.quantity || 0, item.unitPrice || 0)) }}</td>
                  <td>{{ item.remark || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-items">暂无商品明细</div>
          </div>
        </div>

        <!-- Remark -->
        <div class="detail-section" v-if="detailOrder.remark">
          <h4 class="section-title"><span class="section-dash"></span>备注</h4>
          <p class="detail-remark">{{ detailOrder.remark }}</p>
        </div>
      </div>
    </el-drawer>

    <!-- Print Component -->
    <div style="position:absolute;left:-9999px;top:0;pointer-events:none;">
      <PrintOrder ref="printComp" title="出库单" :order="{ ...(detailOrder || {}), totalAmount: detailOrder?.totalAmount ?? detailItemsTotal }" :items="detailItems" type="purchase" :print-config="printConfig" />
    </div>

    <!-- ===== Add / Edit Dialog ===== -->
    <div class="modal-overlay" v-if="showModal" @click.self="closeModal" @keydown.esc="closeModal" tabindex="-1">
      <div class="modal modal-order">
        <div class="modal-header">
          <h3>{{ editId ? '编辑出库单' : '新建出库单' }}</h3>
          <button class="modal-close" @click="closeModal">&times;</button>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <!-- Order Info Section -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>出库信息</h4>
            <div class="form-row">
              <div class="form-group">
                <label>出库单号 <span class="req">*</span></label>
                <input v-model="form.orderNo" maxlength="50" placeholder="如：CK20240101001" required />
              </div>
              <div class="form-group">
                <label>出库日期 <span class="req">*</span></label>
                <input v-model="form.orderDate" type="date" required />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>申请人</label>
                <input v-model="form.applicant" maxlength="50" placeholder="申请人姓名" />
              </div>
              <div class="form-group">
                <label>仓管员</label>
                <input v-model="form.warehouseKeeper" placeholder="仓管员姓名" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>经办人</label>
                <input v-model="form.operator" placeholder="经办人姓名" />
              </div>
              <div class="form-group">
                <label>备注</label>
                <input v-model="form.remark" maxlength="500" placeholder="出库备注（选填）" />
              </div>
            </div>
          </div>

          <!-- Items Section -->
          <div class="form-section">
            <div class="section-header">
              <h4 class="section-title"><span class="section-dash"></span>商品明细</h4>
              <button type="button" class="btn-add-item" @click="addItem">+ 添加商品</button>
            </div>

            <div class="items-table-wrap">
              <table class="items-table" v-if="form.items.length">
                <thead>
                  <tr>
                    <th class="col-num">#</th>
                    <th class="col-commodity">商品</th>
                    <th>名称</th>
                    <th>分类</th>
                    <th>规格</th>
                    <th>单位</th>
                    <th>库位</th>
                    <th class="col-num">数量</th>
                    <th class="col-num">单价</th>
                    <th class="col-num">金额</th>
                    <th>备注</th>
                    <th class="col-op"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in form.items" :key="idx">
                    <td class="row-num">{{ idx + 1 }}</td>
                    <td class="col-commodity">
                      <select v-model="item.commodityId" @change="onCommodityChange(idx)" class="item-select">
                        <option :value="null">选择商品</option>
                        <option v-for="c in commodityOptions" :key="c.id" :value="c.id">{{ c.code ? c.code + ' - ' : '' }}{{ c.name }}</option>
                      </select>
                    </td>
                    <td>
                      <input v-model="item.productName" class="item-input" placeholder="名称" />
                    </td>
                    <td>
                      <input v-model="item.productCategory" class="item-input" placeholder="分类" />
                    </td>
                    <td>
                      <input v-model="item.spec" class="item-input" placeholder="规格" />
                    </td>
                    <td>
                      <input v-model="item.unit" class="item-input w60" placeholder="单位" />
                    </td>
                    <td>
                      <input v-model="item.warehouseLoc" class="item-input" placeholder="库位" />
                    </td>
                    <td class="col-num">
                      <input v-model.number="item.quantity" type="number" min="0" step="1" class="item-input w70" placeholder="0" />
                    </td>
                    <td class="col-num">
                      <input v-model.number="item.unitPrice" type="number" min="0" step="0.01" class="item-input w80" placeholder="0.00" />
                    </td>
                    <td class="col-num money-gold">
                      ¥{{ formatMoney(calcItemAmount(item)) }}
                    </td>
                    <td>
                      <input v-model="item.remark" class="item-input" placeholder="备注" />
                    </td>
                    <td class="col-op">
                      <button type="button" class="btn-remove-item" @click="removeItem(idx)" title="删除此行">&times;</button>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="9" class="total-label">合计金额</td>
                    <td class="col-num money-gold total-value">¥{{ formatMoney(calcTotalAmount) }}</td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
              <div v-else class="empty-items-hint">
                <p>暂无商品，请点击"添加商品"按钮</p>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>

    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入出库单"
      :column-map="importConfigs.stockOut.columns"
      :module="'stockOut'"
      :on-complete="loadList"
      :sample-data="importConfigs.stockOut.sample"
      template-filename="出库导入模板"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getStockOutList,
  getStockOutDetail,
  addStockOut,
  updateStockOut,
  deleteStockOut,
  getStockOutSummary,
  getCommodities,
  restoreEntity,
} from '@/api/admin'
import PrintOrder from '@/components/PrintOrder.vue'
import { usePrintConfig } from '@/composables/usePrintConfig'
const { printConfig } = usePrintConfig()
import { exportToExcel, fmtDate, fmtMoney } from '@/utils/exportExcel'
import { multiplyMoney } from '@/utils/format'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import { importConfigs } from '@/utils/importConfigs'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { useTableSort } from '@/composables/useTableSort'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'
import { useUnsavedConfirm } from '@/composables/useUnsavedConfirm'

// ===== State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { orderDate: 'date', createTime: 'date' }, { storageKey: 'stock_out_filters' })
const loading = ref(false)
const importDialogVisible = ref(false)
const saving = ref(false)
const keyword = ref('')
const startDate = ref(null)
const endDate = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const summary = ref(null)

// ===== Batch Selection =====
const selectedIds = ref(new Set())
const batchOperating = ref(false)

const isAllSelected = computed(() => list.value.length > 0 && list.value.every(item => selectedIds.value.has(item.id)))
const isIndeterminate = computed(() => selectedIds.value.size > 0 && !isAllSelected.value)

function toggleSelectAll() {
  if (isAllSelected.value) { selectedIds.value = new Set() }
  else { selectedIds.value = new Set(list.value.map(item => item.id)) }
}
function toggleSelect(id) {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  selectedIds.value = s
}
function clearSelection() { selectedIds.value = new Set() }

async function handleBatchDelete() {
  if (!selectedIds.value.size) return
  const confirmed = await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.size} 条记录？`, '批量删除', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  batchOperating.value = true
  try {
    const ids = [...selectedIds.value]
    let success = 0, failedMsgs = []
    for (const id of ids) {
      try { await deleteStockOut(id); success++ } catch (e) { failedMsgs.push(e?.response?.data?.msg || e?.message || '未知错误') }
    }
    if (failedMsgs.length) ElMessage.warning(`成功 ${success} 条，失败 ${failedMsgs.length} 条：${failedMsgs[0]}`)
    else ElMessage.success(`成功删除 ${success} 条`)
    clearSelection()
    loadList()
    loadSummary()
  } finally { batchOperating.value = false }
}

// URL filter persistence
const route = useRoute()
const router = useRouter()
const FILTER_KEY = 'stock_out_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.keyword || q.startDate || q.page
  if (hasUrlParams) {
    _restoring = true
    if (q.keyword) keyword.value = q.keyword
    if (q.startDate) startDate.value = q.startDate
    if (q.endDate) endDate.value = q.endDate
    if (q.page) currentPage.value = Number(q.page)
    if (q.pageSize) pageSize.value = Number(q.pageSize)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.startDate) startDate.value = saved.startDate
    if (saved.endDate) endDate.value = saved.endDate
    if (saved.page) currentPage.value = Number(saved.page)
    if (saved.pageSize) pageSize.value = Number(saved.pageSize)
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (keyword.value) {
    query.keyword = keyword.value
    storage.keyword = keyword.value
  }
  if (startDate.value) {
    query.startDate = startDate.value
    storage.startDate = startDate.value
  }
  if (endDate.value) {
    query.endDate = endDate.value
    storage.endDate = endDate.value
  }
  if (currentPage.value > 1) {
    query.page = String(currentPage.value)
    storage.page = currentPage.value
  }
  if (pageSize.value !== 10) {
    query.pageSize = String(pageSize.value)
    storage.pageSize = pageSize.value
  }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

// Filter watchers
watch(currentPage, () => { if (!_restoring) { loadList(); syncFiltersToUrl() } })
watch(pageSize, () => {
  if (_restoring) return
  if (currentPage.value === 1) { loadList(); syncFiltersToUrl() }
  else { currentPage.value = 1 }
})

// Detail drawer
const drawerVisible = ref(false)
const detailOrder = ref(null)
const printComp = ref(null)
const detailItems = ref([])

const detailItemsTotal = computed(() => {
  return detailItems.value.reduce((sum, item) => {
    if (item.amount != null) return sum + item.amount
    if (item.quantity != null && item.unitPrice != null) return sum + multiplyMoney(item.quantity, item.unitPrice)
    return sum
  }, 0)
})

// Add/Edit modal
const showModal = ref(false)
const editId = ref(null)

// Options
const commodityOptions = ref([])

// Form
const defaultItem = () => ({
  commodityId: null,
  productName: '',
  productCategory: '',
  spec: '',
  unit: '',
  warehouseLoc: '',
  quantity: null,
  unitPrice: null,
  remark: '',
})

const defaultForm = () => ({
  orderNo: '',
  orderDate: toLocalDateStr(new Date()),
  applicant: '',
  warehouseKeeper: '',
  operator: '',
  remark: '',
  items: [defaultItem()],
})

const form = reactive(defaultForm())
useCtrlEnterSave(showModal, handleSave)
const { resetDirty, closeGuard } = useUnsavedConfirm(showModal, () => JSON.stringify(form), { form })

// ===== Undo Delete =====
const { handleDelete: undoHandleDelete } = useUndoDelete({
  deleteApi: deleteStockOut,
  restoreApi: (id) => restoreEntity('stock-out', id),
  onSuccess: () => {
    if (list.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    loadList()
    loadSummary()
  },
  entityLabel: '出库单',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateStockOut,
  onSuccess: () => { loadList(); loadSummary() },
  entityLabel: '出库单',
})

// ===== Computed =====
const hasFilter = computed(() => {
  return !!(keyword.value || startDate.value || endDate.value)
})

const calcTotalAmount = computed(() => {
  return form.items.reduce((sum, item) => sum + calcItemAmount(item), 0)
})

function calcItemAmount(item) {
  const qty = Number(item.quantity) || 0
  const price = Number(item.unitPrice) || 0
  return multiplyMoney(qty, price)
}

// ===== Formatters =====
function formatMoney(v) {
  if (v == null || isNaN(v)) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function toLocalDateStr(d) {
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

function formatDateTime(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ===== Lifecycle =====
onMounted(async () => {
  await loadCommodityOptions()
  restoreFilters()
  loadList()
  loadSummary()
})

watch(() => route.query.detailId, (val) => {
  if (!val) return
  const id = Number(val)
  if (Number.isInteger(id) && id > 0) nextTick(() => openDetail({ id }))
}, { immediate: true })

// ===== Data Loading =====
async function loadList() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await getStockOutList(params)
    const data = res.data?.data || res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data?.records || data?.list || data?.content || []
      total.value = data?.total ?? list.value.length
    }
  } catch (err) {
    console.error('加载出库单列表失败:', err)
    ElMessage.error('加载列表失败，请稍后重试')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    const params = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getStockOutSummary(params)
    summary.value = res.data?.data || res.data || null
  } catch (err) {
    console.error('加载汇总失败:', err)
  }
}

async function loadCommodityOptions() {
  try {
    const res = await getCommodities({ page: 1, size: 9999 })
    const data = res.data?.data || res.data
    commodityOptions.value = data?.records || data?.list || (Array.isArray(data) ? data : [])
  } catch (err) {
    console.error('加载商品列表失败:', err)
    commodityOptions.value = []
  }
}

// ===== Search with debounce =====
let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadList()
    loadSummary()
    syncFiltersToUrl()
  }, 300)
}

// ===== Filter actions =====
function onStartDateChange() {
  if (!validateDateRange(startDate.value, endDate.value, 'start')) { startDate.value = null; return }
  handleSearch()
}
function onEndDateChange() {
  if (!validateDateRange(startDate.value, endDate.value, 'end')) { endDate.value = null; return }
  handleSearch()
}
function handleSearch() {
  currentPage.value = 1
  loadList()
  loadSummary()
  syncFiltersToUrl()
}

function handleReset() {
  keyword.value = ''
  startDate.value = null
  endDate.value = null
  currentPage.value = 1
  pageSize.value = 20
  sessionStorage.removeItem(FILTER_KEY)
  router.replace({ query: {} })
  loadList()
  loadSummary()
}

// ===== Detail Drawer =====
async function openDetail(row) {
  drawerVisible.value = true
  detailOrder.value = null
  detailItems.value = []
  try {
    const res = await getStockOutDetail(row.id)
    const data = res.data?.data || res.data
    detailOrder.value = data?.order || data || row
    detailItems.value = data?.items || []
  } catch (err) {
    console.error('加载出库单详情失败:', err)
    ElMessage.error('加载详情失败')
    detailOrder.value = row
    detailItems.value = []
  }
}

// ===== Add / Edit =====
function resetForm() {
  const df = defaultForm()
  Object.keys(df).forEach((key) => {
    if (key === 'items') {
      form.items = [defaultItem()]
    } else {
      form[key] = df[key]
    }
  })
}

function openAdd() {
  editId.value = null
  resetForm()
  showModal.value = true
}

function openEdit(row) {
  snapshotBeforeEdit(row)
  editId.value = row.id
  form.orderNo = row.orderNo || ''
  form.orderDate = row.orderDate || toLocalDateStr(new Date())
  form.applicant = row.applicant || ''
  form.warehouseKeeper = row.warehouseKeeper || ''
  form.operator = row.operator || ''
  form.remark = row.remark || ''
  form.items = [defaultItem()]

  // Load full detail to get items
  loadDetailForEdit(row.id)
  showModal.value = true
}

async function loadDetailForEdit(id) {
  try {
    const res = await getStockOutDetail(id)
    const data = res.data?.data || res.data
    const order = data?.order || data
    const items = data?.items || []

    // Update form with full order data
    if (order) {
      form.orderNo = order.orderNo || form.orderNo
      form.orderDate = order.orderDate || form.orderDate
      form.applicant = order.applicant || form.applicant
      form.warehouseKeeper = order.warehouseKeeper || form.warehouseKeeper
      form.operator = order.operator || form.operator
      form.remark = order.remark || form.remark
    }

    if (items.length) {
      form.items = items.map((item) => ({
        commodityId: item.commodityId ?? null,
        productName: item.productName || '',
        productCategory: item.productCategory || '',
        spec: item.spec || '',
        unit: item.unit || '',
        warehouseLoc: item.warehouseLoc || '',
        quantity: item.quantity ?? null,
        unitPrice: item.unitPrice ?? null,
        remark: item.remark || '',
      }))
    }
  } catch (err) {
    console.error('加载编辑详情失败:', err)
  }
}

function closeModal() {
  closeGuard(() => { showModal.value = false })
}

// ===== Items management =====
function addItem() {
  form.items.push(defaultItem())
}

function removeItem(idx) {
  if (form.items.length <= 1) {
    ElMessage.warning('至少保留一条商品明细')
    return
  }
  form.items.splice(idx, 1)
}

function onCommodityChange(idx) {
  const item = form.items[idx]
  const c = commodityOptions.value.find((c) => c.id === item.commodityId)
  if (c) {
    item.productName = c.name || ''
    item.productCategory = c.categoryName || c.category || ''
    item.spec = c.model || c.spec || ''
    item.unit = c.unit || ''
    item.warehouseLoc = c.warehouseLoc || c.location || ''
    if (c.defaultPrice != null) item.unitPrice = Number(c.defaultPrice)
    if (c.costPrice != null && item.unitPrice == null) item.unitPrice = Number(c.costPrice)
  }
}

// ===== Save =====
async function handleSave() {
  if (saving.value) return
  // Validation
  if (!form.orderNo.trim()) {
    ElMessage.warning('请填写出库单号')
    return
  }
  if (!form.orderDate) {
    ElMessage.warning('请选择出库日期')
    return
  }
  const validItems = form.items.filter(
    (item) => item.productName.trim() && item.quantity > 0
  )
  if (!validItems.length) {
    ElMessage.warning('请至少添加一条有效的商品明细（名称和数量必填）')
    return
  }
  for (let i = 0; i < form.items.length; i++) {
    const it = form.items[i]
    if (it.unitPrice == null || it.unitPrice < 0) {
      ElMessage.warning(`第 ${i + 1} 行单价不能为负数`)
      return
    }
  }

  saving.value = true
  try {
    const data = {
      orderNo: form.orderNo.trim(),
      orderDate: form.orderDate,
      applicant: form.applicant.trim(),
      warehouseKeeper: form.warehouseKeeper.trim(),
      operator: form.operator.trim(),
      remark: form.remark.trim(),
      items: validItems.map((item) => ({
        commodityId: item.commodityId,
        productName: item.productName.trim(),
        productCategory: item.productCategory.trim(),
        spec: item.spec.trim(),
        unit: item.unit.trim(),
        warehouseLoc: item.warehouseLoc.trim(),
        quantity: Number(item.quantity) || 0,
        unitPrice: Number(item.unitPrice) || 0,
        remark: item.remark.trim(),
      })),
    }

    if (editId.value) {
      await updateStockOut(editId.value, data)
      afterEditSave({ id: editId.value, ...data })
    } else {
      await addStockOut(data)
      ElMessage.success('出库单已创建')
    }
    resetDirty()
    showModal.value = false
    loadList()
    loadSummary()
  } catch (err) {
    console.error('保存出库单失败:', err)
    ElMessage.error(editId.value ? '更新失败，请稍后重试' : '创建失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// ===== Print =====
function handlePrint() {
  if (!detailOrder.value) {
    ElMessage.warning('请先打开一个出库单详情再打印')
    return
  }
  printComp.value?.doPrint()
}

// ===== Export =====
async function handleExport() {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const columns = [
    { label: '单号', key: 'orderNo', width: 18 },
    { label: '日期', key: 'orderDate', width: 14, format: fmtDate },
    { label: '申请人', key: 'applicant', width: 12 },
    { label: '仓管员', key: 'warehouseKeeper', width: 12 },
    { label: '经办人', key: 'operator', width: 12 },
    { label: '备注', key: 'remark', width: 20 },
    { label: '创建时间', key: 'createTime', width: 18, format: fmtDate },
  ]
  await exportToExcel(list.value, columns, '出库单', { sheetName: '出库单' })
}

// ===== Delete =====
function handleDelete(row) {
  undoHandleDelete(row)
}
</script>

<style scoped>
/* ===== Page Header ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
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

/* ===== Toolbar ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.date-range {
  display: flex;
  align-items: center;
  gap: 6px;
}
.date-range :deep(.el-date-editor) {
  --el-date-editor-width: 155px;
}
.date-sep {
  color: #999;
  font-size: 13px;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 6px 12px;
  transition: border-color 0.2s;
}
.search-wrap:focus-within {
  border-color: #C5A265;
}
.search-wrap svg {
  color: #bbb;
  flex-shrink: 0;
}
.search-input {
  border: none;
  outline: none;
  font-size: 13px;
  color: #333;
  width: 200px;
  font-family: inherit;
  background: transparent;
}
.search-input::placeholder {
  color: #bbb;
}

.btn-search {
  padding: 8px 16px;
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
.btn-search:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-reset {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-reset:hover {
  border-color: #999;
  color: #666;
}

.btn-outline {
  padding: 9px 18px;
  background: #fff;
  color: #C5A265;
  border: 1px solid #C5A265;
  border-radius: 8px;
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
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== Summary Bar ===== */
.summary-bar {
  display: flex;
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 10px;
  margin-bottom: 14px;
  overflow: hidden;
}
.summary-card {
  flex: 1;
  padding: 14px 10px;
  text-align: center;
  border-right: 1px solid rgba(255,255,255,0.08);
}
.summary-card:last-child {
  border-right: none;
}
.sc-label {
  font-size: 11px;
  color: rgba(255,255,255,0.50);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}
.sc-value {
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  font-variant-numeric: tabular-nums;
}
.summary-card.gold .sc-value  { color: #ffd54f; }

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
  min-width: 860px;
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
  user-select: none;
}
.data-table th.sortable { cursor: pointer; user-select: none; }
.data-table th.sortable:hover { color: #C5A265; }
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

.col-orderno {
  white-space: nowrap;
}
.order-no {
  color: #C5A265;
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s;
}
.order-no:hover {
  color: #b89555;
  text-decoration: underline;
}
.col-date {
  white-space: nowrap;
  font-size: 12px;
  color: #888;
}
.col-remark {
  max-width: 200px;
}
.remark-text {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #999;
  font-size: 12px;
}
.col-action {
  white-space: nowrap;
}

.btn-text {
  padding: 4px 10px;
  border: none;
  background: none;
  color: #C5A265;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
  font-family: inherit;
  font-weight: 500;
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

/* ===== Loading skeleton ===== */
.loading-wrap {
  padding: 16px;
}
.skeleton-row {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f8f8f8;
}
.sk-cell {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.sk-cell.w60 { width: 60px; }
.sk-cell.w80 { width: 80px; }
.sk-cell.w100 { width: 100px; }
.sk-cell.w120 { width: 120px; }
.sk-cell.w140 { width: 140px; }
.sk-cell.w150 { width: 150px; }
.sk-cell.w200 { width: 200px; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
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
  padding: 16px 0;
}

/* ===== Drawer Detail ===== */
.drawer-content {
  padding: 0 4px;
}
.detail-section {
  margin-bottom: 24px;
}
.detail-section:last-child {
  margin-bottom: 0;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.section-dash {
  display: inline-block;
  width: 16px;
  height: 2px;
  background: #C5A265;
  border-radius: 1px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item.full {
  grid-column: 1 / -1;
}
.detail-label {
  font-size: 12px;
  color: #999;
}
.detail-val {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}
.detail-val.mono {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
}
.money-gold {
  color: #C5A265 !important;
  font-weight: 700;
}

.detail-items-table-wrap {
  overflow-x: auto;
}
.detail-items-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 600px;
}
.detail-items-table th {
  background: #fafbfc;
  font-weight: 600;
  color: #666;
  font-size: 12px;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}
.detail-items-table td {
  padding: 10px 12px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
}
.detail-items-table .col-num {
  text-align: right;
  font-family: 'SF Mono', 'Consolas', monospace;
}
.detail-items-table .row-num {
  color: #bbb;
  font-size: 12px;
  text-align: center;
  width: 40px;
}
.empty-items {
  text-align: center;
  padding: 30px 0;
  color: #ccc;
  font-size: 13px;
}
.detail-remark {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  padding: 12px 16px;
  background: #fafbfc;
  border-radius: 6px;
  margin: 0;
}

/* ===== Modal ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 30px;
  overflow-y: auto;
}
.modal {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-order {
  width: 1060px;
  max-width: 95vw;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
  border-radius: 12px 12px 0 0;
}
.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  font-size: 22px;
  color: #999;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.modal-close:hover {
  background: #f5f5f5;
  color: #333;
}
.modal-body {
  padding: 20px 24px 24px;
}

/* Form sections */
.form-section {
  margin-bottom: 22px;
}
.form-section:last-of-type {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.section-header .section-title {
  margin-bottom: 0;
}

.form-row {
  display: flex;
  gap: 14px;
  margin-bottom: 12px;
}
.form-row:last-child {
  margin-bottom: 0;
}
.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: #555;
  margin-bottom: 5px;
}
.form-group .req {
  color: #e74c3c;
}
.form-group input {
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  font-family: inherit;
  color: #333;
  transition: border-color 0.2s;
  background: #fff;
}
.form-group input:focus {
  border-color: #C5A265;
}
.form-group input::placeholder {
  color: #ccc;
}

/* ===== Items table in form ===== */
.btn-add-item {
  padding: 5px 14px;
  border: 1px dashed #C5A265;
  border-radius: 6px;
  background: none;
  color: #C5A265;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-add-item:hover {
  background: rgba(197, 162, 101, 0.06);
}

.items-table-wrap {
  overflow-x: auto;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}
.items-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 880px;
}
.items-table th {
  background: #fafbfc;
  font-weight: 600;
  color: #666;
  font-size: 11px;
  padding: 10px 8px;
  text-align: left;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}
.items-table td {
  padding: 6px 8px;
  font-size: 13px;
  border-bottom: 1px solid #f8f8f8;
  color: #444;
  vertical-align: middle;
}
.items-table .col-num {
  text-align: center;
  width: 50px;
}
.items-table .row-num {
  color: #bbb;
  font-size: 12px;
}
.items-table .col-commodity {
  min-width: 160px;
}
.items-table .col-op {
  width: 40px;
  text-align: center;
}
.items-table tfoot td {
  padding: 10px 8px;
  font-weight: 600;
  border-top: 2px solid #f0f0f0;
  background: #fafbfc;
}
.total-label {
  text-align: right;
  font-size: 13px;
  color: #666;
}
.total-value {
  font-size: 15px;
}

.item-select {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 12px;
  outline: none;
  font-family: inherit;
  color: #333;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s;
}
.item-select:focus {
  border-color: #C5A265;
}
.item-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 12px;
  outline: none;
  font-family: inherit;
  color: #333;
  transition: border-color 0.2s;
}
.item-input:focus {
  border-color: #C5A265;
}
.item-input::placeholder {
  color: #ddd;
}
.item-input.w60 {
  width: 60px;
}
.item-input.w70 {
  width: 70px;
  text-align: right;
}
.item-input.w80 {
  width: 80px;
  text-align: right;
}

.btn-remove-item {
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: #ccc;
  font-size: 18px;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.btn-remove-item:hover {
  background: rgba(231, 76, 60, 0.08);
  color: #e74c3c;
}

.empty-items-hint {
  text-align: center;
  padding: 40px 20px;
  color: #ccc;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: 20px;
}
.btn-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-cancel:hover {
  border-color: #999;
  color: #333;
}

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; margin-bottom: 12px;
  background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px;
}
.batch-info { font-size: 13px; color: #d46b08; font-weight: 600; }
.btn-batch {
  padding: 6px 14px; border: 1px solid #e0e0e0; border-radius: 4px;
  background: #fff; color: #666; font-size: 12px; cursor: pointer;
}
.btn-batch:hover { border-color: #C5A265; color: #C5A265; }
.btn-batch-danger {
  padding: 6px 14px; border: 1px solid #ff4d4f; border-radius: 4px;
  background: #fff; color: #ff4d4f; font-size: 12px; cursor: pointer;
}
.btn-batch-danger:hover { background: #ff4d4f; color: #fff; }

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
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left,
  .toolbar-right {
    width: 100%;
    flex-wrap: wrap;
  }
  .search-input {
    width: 100%;
    flex: 1;
    min-width: 140px;
  }
  .summary-bar {
    flex-wrap: wrap;
  }
  .summary-card {
    flex: 1 1 33%;
    min-width: 120px;
  }
  .modal-order {
    width: 95vw;
    margin: 0 10px;
  }
  .form-row {
    flex-direction: column;
    gap: 10px;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="page admin-page">
    <!-- ===== Header ===== -->
    <div class="page-header">
      <h2>销售退货</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <el-dropdown trigger="click" @command="handleExportCommand">
          <button class="btn-outline">导出Excel <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="page-detail">当前页 - 详细</el-dropdown-item>
              <el-dropdown-item command="page-simple">当前页 - 简易</el-dropdown-item>
              <el-dropdown-item divided command="all-detail">全部 - 详细</el-dropdown-item>
              <el-dropdown-item command="all-simple">全部 - 简易</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button class="btn-outline" @click="importDialogVisible = true">导入</button>
        <button class="btn-outline" @click="handlePrint" :disabled="!list.length">打印</button>
        <button class="btn-primary" @click="openAdd">+ 新建退货单</button>
      </div>
    </div>

    <!-- ===== Toolbar ===== -->
    <div class="toolbar">
      <div class="status-tabs">
        <span
          v-for="tab in statusTabs"
          :key="tab.value"
          class="status-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="switchStatus(tab.value)"
        >{{ tab.label }}</span>
      </div>
      <div class="toolbar-right">
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="开始日期"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          size="default"
          :clearable="true"
          @change="onStartDateChange"
          class="date-picker"
          style="width: 155px"
        />
        <span style="margin: 0 4px; color: #999;">至</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="截止日期"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          size="default"
          :clearable="true"
          @change="onEndDateChange"
          class="date-picker"
          style="width: 155px"
        />
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="keyword" placeholder="搜单号 / 客户名..." class="search-input" @input="onSearchInput" />
        </div>
      </div>
    </div>

    <!-- ===== Summary Bar ===== -->
    <div class="summary-bar" v-if="summary">
      <div class="summary-card">
        <div class="sc-label">退货单数</div>
        <div class="sc-value">{{ summary.totalCount ?? total }} 单</div>
      </div>
      <div class="summary-card gold">
        <div class="sc-label">退货总金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalAmount) }}</div>
      </div>
      <div class="summary-card green">
        <div class="sc-label">已退金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalPaid) }}</div>
      </div>
    </div>

    <!-- ===== Batch Bar ===== -->
    <div v-if="selectedIds.size" class="batch-bar">
      <span class="batch-info">已选 {{ selectedIds.size }} 项</span>
      <button class="btn-batch-danger" @click="handleBatchDelete" :disabled="batchOperating">{{ batchOperating ? '处理中...' : '批量删除' }}</button>
      <el-dropdown trigger="click" @command="handleBatchStatus">
        <button class="btn-batch" :disabled="batchOperating">批量状态 ▾</button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="pending">待处理</el-dropdown-item>
            <el-dropdown-item command="processing">进行中</el-dropdown-item>
            <el-dropdown-item command="completed">已完成</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button class="btn-batch" @click="clearSelection">取消选择</button>
    </div>

    <!-- ===== Table ===== -->
    <div class="card" v-loading="loading">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th style="width:40px"><input type="checkbox" :checked="isAllSelected" :indeterminate.prop="isIndeterminate" @change="toggleSelectAll" /></th>
            <th class="col-order-no sortable" @click="toggleSort('orderNo')">退货单号 {{ sortArrow('orderNo') }}</th>
            <th class="col-order-no">原订单号</th>
            <th class="sortable" @click="toggleSort('customerName')">客户名称 {{ sortArrow('customerName') }}</th>
            <th class="col-date sortable" @click="toggleSort('returnDate')">退货日期 {{ sortArrow('returnDate') }}</th>
            <th class="col-money sortable" @click="toggleSort('totalAmount')">总金额 {{ sortArrow('totalAmount') }}</th>
            <th class="col-money">已退金额</th>
            <th class="col-status">状态</th>
            <th style="width:240px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in sortedList" :key="item.id" >
            <td><input type="checkbox" :checked="selectedIds.has(item.id)" @change="toggleSelect(item.id)" /></td>
            <td class="col-order-no">{{ item.orderNo || '-' }}</td>
            <td class="col-order-no">{{ item.originalOrderNo || '-' }}</td>
            <td class="col-customer">{{ item.customerName || '-' }}</td>
            <td class="col-date">{{ formatDate(item.returnDate) }}</td>
            <td class="col-money gold">{{ formatMoney(item.totalAmount) }}</td>
            <td class="col-money">{{ formatMoney(item.paidAmount) }}</td>
            <td class="col-status">
              <span class="status-badge" :class="'status-' + item.status">{{ statusLabel(item.status) }}</span>
            </td>
            <td class="col-action">
              <button class="btn-text" @click="openDetail(item)">查看详情</button>
              <button class="btn-text" @click="openEdit(item)">编辑</button>
              <el-dropdown trigger="click" @command="(cmd) => handleChangeStatus(item, cmd)">
                <button class="btn-text status-btn">状态<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="pending" :disabled="item.status === 'pending'">待处理</el-dropdown-item>
                    <el-dropdown-item command="processing" :disabled="item.status === 'processing'">进行中</el-dropdown-item>
                    <el-dropdown-item command="completed" :disabled="item.status === 'completed'">已完成</el-dropdown-item>
                    <el-dropdown-item command="cancelled" :disabled="item.status === 'cancelled'" divided>已取消</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <button class="btn-text danger" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        <p>{{ hasFilter ? '未匹配到退货单' : '暂无销售退货记录' }}</p>
      </div>
    </div>

    <!-- ===== Pagination ===== -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
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
      title="退货单详情"
      direction="rtl"
      size="680px"
      close-on-press-escape
      :destroy-on-close="true"
      @closed="router.replace({ query: { ...route.query, detailId: undefined } })"
    >
      <div class="detail-content" v-loading="detailLoading" v-if="detailOrder">
        <!-- Order Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>退货信息</h4>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">退货单号</span>
              <span class="field-value mono">{{ detailOrder.orderNo || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">退货状态</span>
              <span class="status-badge" :class="'status-' + detailOrder.status">{{ statusLabel(detailOrder.status) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">原订单号</span>
              <span class="field-value mono">{{ detailOrder.originalOrderNo || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">客户名称</span>
              <span class="field-value">{{ detailOrder.customerName || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">退货日期</span>
              <span class="field-value">{{ formatDate(detailOrder.returnDate) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">是否结清</span>
              <span class="field-value">
                <span class="clear-tag" :class="{ cleared: detailOrder.isCleared }">{{ detailOrder.isCleared ? '已结清' : '未结清' }}</span>
              </span>
            </div>
          </div>
        </div>

        <!-- Financial Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>财务信息</h4>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">退货总金额</span>
              <span class="field-value money gold">{{ formatMoney(detailOrder.totalAmount) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">已退金额</span>
              <span class="field-value money">{{ formatMoney(detailOrder.paidAmount) }}</span>
            </div>
          </div>
        </div>

        <!-- Remark -->
        <div class="detail-section" v-if="detailOrder.remark">
          <h4 class="section-title"><span class="section-dash"></span>备注</h4>
          <div class="detail-grid">
            <div class="detail-field full">
              <span class="field-value">{{ detailOrder.remark }}</span>
            </div>
          </div>
        </div>

        <!-- Items Table -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>退货商品明细</h4>
          <div class="items-table-wrap">
            <table class="items-table" v-if="detailItems.length">
              <thead>
                <tr>
                  <th>商品名称</th>
                  <th>分类</th>
                  <th>规格</th>
                  <th>单位</th>
                  <th class="col-num">数量</th>
                  <th class="col-money">单价</th>
                  <th class="col-money">金额</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in detailItems" :key="idx">
                  <td>{{ item.productName || '-' }}</td>
                  <td>{{ item.productCategory || '-' }}</td>
                  <td>{{ item.spec || '-' }}</td>
                  <td>{{ item.unit || '-' }}</td>
                  <td class="col-num">{{ item.quantity ?? '-' }}</td>
                  <td class="col-money">{{ formatMoney(item.unitPrice) }}</td>
                  <td class="col-money gold">{{ formatMoney(item.quantity != null && item.unitPrice != null ? multiplyMoney(item.quantity, item.unitPrice) : null) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="6" class="total-label">合计</td>
                  <td class="col-money gold total-value">{{ formatMoney(detailOrder.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
            <p v-else class="empty-items">暂无退货商品明细</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="handlePrint">打印退货单</el-button>
      </template>
    </el-drawer>

    <!-- Print Template -->
    <div style="position:absolute;left:-9999px;top:0;pointer-events:none;">
      <PrintOrder
        ref="printComp"
        title="销售退货单"
        :order="detailOrder || {}"
        :items="detailItems"
        type="return"
        :print-config="printConfig"
      />
    </div>

    <!-- ===== Add / Edit Dialog ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑退货单' : '新建退货单'"
      width="960px"
      :close-on-click-modal="false"
      close-on-press-escape
      destroy-on-close
      top="30px"
      :before-close="closeGuard"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right">
        <!-- Order Info -->
        <div class="dialog-section">
          <h4 class="section-title"><span class="section-dash"></span>退货信息</h4>
          <div class="form-row-3">
            <el-form-item label="原订单号" prop="originalOrderNo">
              <el-input v-model="form.originalOrderNo" placeholder="请输入原销售订单号" />
            </el-form-item>
            <el-form-item label="客户名称" prop="customerName">
              <el-select
                v-model="form.customerName"
                filterable
                remote
                reserve-keyword
                placeholder="搜索客户..."
                :remote-method="searchCustomers"
                :loading="customerSearching"
                @change="onCustomerSelect"
                style="width:100%"
              >
                <el-option
                  v-for="c in customerOptions"
                  :key="c.id"
                  :label="c.name"
                  :value="c.name"
                >
                  <span>{{ c.name }}</span>
                  <span v-if="c.phone" style="float:right;color:#999;font-size:12px">{{ c.phone }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="退货日期" prop="returnDate">
              <el-date-picker v-model="form.returnDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" style="width:100%" />
            </el-form-item>
          </div>
          <div class="form-row-2">
            <el-form-item label="备注" prop="remark" class="span-2">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="备注信息（选填）" />
            </el-form-item>
          </div>
        </div>

        <!-- Items Table -->
        <div class="dialog-section">
          <h4 class="section-title">
            <span class="section-dash"></span>退货商品明细
            <button type="button" class="btn-add-item" @click="addItem">+ 添加商品</button>
          </h4>
          <div class="items-edit-wrap">
            <table class="items-edit-table">
              <thead>
                <tr>
                  <th class="col-commodity">商品</th>
                  <th class="col-category">分类</th>
                  <th class="col-spec">规格</th>
                  <th class="col-unit">单位</th>
                  <th class="col-num">数量</th>
                  <th class="col-price">单价</th>
                  <th class="col-price">金额</th>
                  <th class="col-remark">备注</th>
                  <th class="col-act">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in form.items" :key="idx">
                  <td class="col-commodity">
                    <el-select
                      v-model="item.commodityId"
                      filterable
                      remote
                      reserve-keyword
                      placeholder="搜索商品..."
                      :remote-method="searchCommodities"
                      :loading="commoditySearching"
                      @change="(val) => onCommoditySelect(idx, val)"
                      size="small"
                      style="width:100%"
                    >
                      <el-option
                        v-for="c in commodityOptions"
                        :key="c.id"
                        :label="c.name"
                        :value="c.id"
                      >
                        <span>{{ c.name }}</span>
                        <span style="float:right;color:#999;font-size:12px">{{ c.defaultPrice != null ? '¥' + c.defaultPrice : '' }}</span>
                      </el-option>
                    </el-select>
                  </td>
                  <td class="col-category">
                    <el-input v-model="item.productCategory" size="small" placeholder="分类" />
                  </td>
                  <td class="col-spec">
                    <el-input v-model="item.spec" size="small" placeholder="规格" />
                  </td>
                  <td class="col-unit">
                    <el-input v-model="item.unit" size="small" placeholder="单位" />
                  </td>
                  <td class="col-num">
                    <el-input-number v-model="item.quantity" :min="1" :precision="0" size="small" controls-position="right" style="width:100%" />
                  </td>
                  <td class="col-price">
                    <el-input-number v-model="item.unitPrice" :min="0" :precision="2" size="small" controls-position="right" style="width:100%" />
                  </td>
                  <td class="col-price col-amount">
                    {{ item.quantity != null && item.unitPrice != null ? formatMoney(multiplyMoney(item.quantity, item.unitPrice)) : '-' }}
                  </td>
                  <td class="col-remark">
                    <el-input v-model="item.remark" size="small" placeholder="备注" />
                  </td>
                  <td class="col-act">
                    <button type="button" class="btn-remove-item" @click="removeItem(idx)" title="删除此行">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </td>
                </tr>
              </tbody>
              <tfoot v-if="form.items.length">
                <tr>
                  <td colspan="6" class="total-label">合计金额</td>
                  <td class="col-price gold total-value">{{ formatMoney(itemsTotal) }}</td>
                  <td colspan="2"></td>
                </tr>
              </tfoot>
            </table>
            <div v-if="!form.items.length" class="empty-items-edit">
              <p>暂无退货商品，请点击"添加商品"</p>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closeGuard(() => { dialogVisible = false })">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="saving" @click="handleSave">
          {{ editId ? '保存修改' : '创建退货单' }}
        </el-button>
      </template>
    </el-dialog>

    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入销售退货单"
      :column-map="importConfigs.saleReturns.columns"
      :module="'saleReturns'"
      group-by="orderNo"
      :transform-record="transformImportRecord"
      :on-complete="() => { loadList(); loadSummary() }"
      :sample-data="importConfigs.saleReturns.sample"
      template-filename="销售退货导入模板"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PrintOrder from '@/components/PrintOrder.vue'
import { usePrintConfig } from '@/composables/usePrintConfig'
const { printConfig } = usePrintConfig()
import { exportToExcel, fmtDate, fmtMoney } from '@/utils/exportExcel'
import { multiplyMoney } from '@/utils/format'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import { importConfigs } from '@/utils/importConfigs'
import {
  getSaleReturns,
  getSaleReturnDetail,
  getSaleReturnItemsBatch,
  addSaleReturn,
  updateSaleReturn,
  deleteSaleReturn,
  updateSaleReturnStatus,
  getSaleReturnSummary,
  getCommodities,
  getCustomers,
  restoreEntity,
} from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { useOrderLock } from '@/composables/useOrderLock'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { useRecentOrders } from '@/composables/useRecentOrders'
import RefreshButton from '@/components/RefreshButton.vue'
import { useTableSort } from '@/composables/useTableSort'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'
import { useUnsavedConfirm } from '@/composables/useUnsavedConfirm'

// ===== Status Config =====
const statusTabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'pending' },
  { label: '进行中', value: 'processing' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
]

const statusMap = {
  pending: '待处理',
  processing: '进行中',
  completed: '已完成',
  cancelled: '已取消',
}

function statusLabel(status) {
  return statusMap[status] || status || '-'
}

// ===== List State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { returnDate: 'date', totalAmount: 'number', paidAmount: 'number' }, { storageKey: 'sale_returns_filters' })
const importDialogVisible = ref(false)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeStatus = ref('')
const keyword = ref('')
const startDate = ref(null)
const endDate = ref(null)
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
      try { await deleteSaleReturn(id); success++ } catch (e) { failedMsgs.push(e?.response?.data?.msg || e?.message || '未知错误') }
    }
    if (failedMsgs.length) ElMessage.warning(`成功 ${success} 条，失败 ${failedMsgs.length} 条：${failedMsgs[0]}`)
    else ElMessage.success(`成功删除 ${success} 条`)
    clearSelection()
    loadList()
    loadSummary()
  } finally { batchOperating.value = false }
}

async function handleBatchStatus(newStatus) {
  if (!selectedIds.value.size) return
  const label = statusLabel(newStatus)
  const confirmed = await ElMessageBox.confirm(`确认将选中的 ${selectedIds.value.size} 条记录状态更改为「${label}」？`, '批量状态变更', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  batchOperating.value = true
  try {
    const ids = [...selectedIds.value]
    let success = 0
    for (const id of ids) {
      try { await updateSaleReturnStatus(id, newStatus); success++ } catch (e) { console.error(e) }
    }
    ElMessage.success(`成功变更 ${success} 条状态为「${label}」`)
    clearSelection()
    loadList()
    loadSummary()
  } finally { batchOperating.value = false }
}

let searchTimer = null

// URL filter persistence
const route = useRoute()
const router = useRouter()
const FILTER_KEY = 'sale_returns_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.type || q.keyword || q.startDate || q.endDate || q.page
  if (hasUrlParams) {
    _restoring = true
    if (q.type) activeStatus.value = q.type
    if (q.keyword) keyword.value = q.keyword
    if (q.startDate) startDate.value = q.startDate
    if (q.endDate) endDate.value = q.endDate
    if (q.page) page.value = Number(q.page)
    if (q.pageSize) pageSize.value = Number(q.pageSize)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.type) activeStatus.value = saved.type
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.startDate) startDate.value = saved.startDate
    if (saved.endDate) endDate.value = saved.endDate
    if (saved.page) page.value = Number(saved.page)
    if (saved.pageSize) pageSize.value = Number(saved.pageSize)
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (activeStatus.value !== undefined && activeStatus.value !== '') {
    query.type = String(activeStatus.value)
    storage.type = activeStatus.value
  }
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
  if (page.value > 1) {
    query.page = String(page.value)
    storage.page = page.value
  }
  if (pageSize.value !== 20) {
    query.pageSize = String(pageSize.value)
    storage.pageSize = pageSize.value
  }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

const hasFilter = computed(() => {
  return !!(activeStatus.value || keyword.value.trim() || startDate.value || endDate.value)
})

// ===== Detail State =====
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref(null)
const detailItems = ref([])

// ===== Dialog State =====
const dialogVisible = ref(false)
const editId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const commodityOptions = ref([])
const commoditySearching = ref(false)
const customerOptions = ref([])
const customerSearching = ref(false)

let commoditySearchTimer = null
let customerSearchTimer = null

const defaultForm = () => ({
  originalOrderNo: '',
  customerName: '',
  customerId: null,
  returnDate: '',
  remark: '',
  items: [],
})

const form = reactive(defaultForm())
useCtrlEnterSave(dialogVisible, handleSave)
const { formDirty, resetDirty, closeGuard } = useUnsavedConfirm(dialogVisible, () => JSON.stringify(form), { form })

const rules = {
  customerName: [
    { required: true, message: '请选择客户', trigger: 'change' },
  ],
  returnDate: [
    { required: true, message: '请选择退货日期', trigger: 'change' },
  ],
}

// ===== Undo Delete =====
const { handleDelete: undoHandleDelete } = useUndoDelete({
  deleteApi: deleteSaleReturn,
  restoreApi: (id) => restoreEntity('sale-return', id),
  onSuccess: () => {
    if (list.value.length === 1 && page.value > 1) {
      page.value--
    }
    loadList()
    loadSummary()
  },
  entityLabel: '退货单',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateSaleReturn,
  onSuccess: () => { loadList(); loadSummary() },
  entityLabel: '退货单',
})
const { acquireLock, releaseLock } = useOrderLock('sale_return')
const { addRecent } = useRecentOrders()
watch(dialogVisible, (val) => { if (!val) releaseLock() })

// ===== Computed =====
const itemsTotal = computed(() => {
  return form.items.reduce((sum, item) => {
    if (item.quantity != null && item.unitPrice != null) {
      return sum + multiplyMoney(item.quantity, item.unitPrice)
    }
    return sum
  }, 0)
})

// ===== Helpers =====
function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatMoney(v) {
  if (v == null || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ===== Lifecycle =====
onMounted(() => {
  restoreFilters()
  loadList()
  loadSummary()
})

// ===== Watchers =====
watch(activeStatus, () => {
  if (_restoring) return
  page.value = 1
  loadList()
  syncFiltersToUrl()
})
watch(page, () => { if (!_restoring) { loadList(); syncFiltersToUrl() } })
watch(pageSize, () => {
  if (_restoring) return
  if (page.value === 1) { loadList(); syncFiltersToUrl() }
  else { page.value = 1 }
})

// ===== Data Loading =====
async function loadList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
    }
    if (activeStatus.value) params.status = activeStatus.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await getSaleReturns(params)
    const data = res.data?.data ?? res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data?.records || data?.list || data?.content || []
      total.value = data?.total ?? list.value.length
    }
  } catch (e) {
    console.error('加载销售退货列表失败:', e)
    ElMessage.error('加载退货列表失败，请稍后重试')
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
    const res = await getSaleReturnSummary(params)
    summary.value = res.data?.data ?? res.data ?? null
  } catch {
    summary.value = null
  }
}

// ===== Toolbar Handlers =====
function switchStatus(val) {
  activeStatus.value = val
  syncFiltersToUrl()
}

function onStartDateChange() {
  if (!validateDateRange(startDate.value, endDate.value, 'start')) { startDate.value = null; return }
  onFilterChange()
}
function onEndDateChange() {
  if (!validateDateRange(startDate.value, endDate.value, 'end')) { endDate.value = null; return }
  onFilterChange()
}
function onFilterChange() {
  page.value = 1
  loadList()
  loadSummary()
  syncFiltersToUrl()
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadList()
    syncFiltersToUrl()
  }, 350)
}

// ===== Detail Drawer =====
async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  detailItems.value = []
  try {
    const res = await getSaleReturnDetail(row.id)
    const data = res.data?.data ?? res.data
    if (data && data.order) {
      detailOrder.value = data.order
      detailItems.value = data.items || []
    } else {
      detailOrder.value = data || row
      detailItems.value = data?.items || []
    }
    addRecent({ id: row.id, orderNo: detailOrder.value?.orderNo || row.orderNo, type: 'sale-return', customerName: detailOrder.value?.customerName || row.customerName })
  } catch (e) {
    console.error('加载退货单详情失败:', e)
    ElMessage.error('加载退货单详情失败')
    detailOrder.value = row
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

// ===== Status Change =====
async function handleChangeStatus(row, newStatus) {
  if (row.status === newStatus) return
  const label = statusLabel(newStatus)
  try {
    await ElMessageBox.confirm(
      `确定将退货单「${row.orderNo}」的状态更改为「${label}」吗？`,
      '更改状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await updateSaleReturnStatus(row.id, newStatus)
    ElMessage.success(`状态已更改为「${label}」`)
    row.status = newStatus
    loadSummary()
  } catch (e) {
    console.error('更改状态失败:', e)
    ElMessage.error('更改状态失败，请稍后重试')
  }
}

// ===== Delete =====
function handleDelete(row) {
  undoHandleDelete(row)
}

// ===== Add / Edit Dialog =====
function openAdd() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  if (!await acquireLock(row.id)) return
  addRecent({ id: row.id, orderNo: row.orderNo, type: 'sale-return', customerName: row.customerName })
  snapshotBeforeEdit(row)
  editId.value = row.id
  resetForm()
  // Pre-fill form from row data
  form.originalOrderNo = row.originalOrderNo || ''
  form.customerName = row.customerName || ''
  form.customerId = row.customerId || null
  form.returnDate = row.returnDate || ''
  form.remark = row.remark || ''
  // Load detail to get items
  await loadDetailForEdit(row.id)
  dialogVisible.value = true
}

async function loadDetailForEdit(id) {
  try {
    const res = await getSaleReturnDetail(id)
    const data = res.data?.data ?? res.data
    const order = data?.order || data
    if (order) {
      form.originalOrderNo = order.originalOrderNo || form.originalOrderNo
      form.customerName = order.customerName || form.customerName
      form.customerId = order.customerId || form.customerId
      form.returnDate = order.returnDate || form.returnDate
      form.remark = order.remark || form.remark
    }
    if (data?.items && data.items.length) {
      form.items = data.items.map((it) => createItemRow({
        commodityId: it.commodityId || null,
        productName: it.productName || '',
        productCategory: it.productCategory || '',
        spec: it.spec || '',
        unit: it.unit || '',
        quantity: it.quantity || 1,
        unitPrice: it.unitPrice || null,
        remark: it.remark || '',
      }))
    }
  } catch (e) {
    console.error('加载退货单详情失败:', e)
  }
}

function resetForm() {
  form.originalOrderNo = ''
  form.customerName = ''
  form.customerId = null
  form.returnDate = ''
  form.remark = ''
  form.items = []
}

function createItemRow(data = {}) {
  return reactive({
    commodityId: data.commodityId || null,
    productName: data.productName || '',
    productCategory: data.productCategory || '',
    spec: data.spec || '',
    unit: data.unit || '',
    quantity: data.quantity || 1,
    unitPrice: data.unitPrice || null,
    remark: data.remark || '',
  })
}

function addItem() {
  form.items.push(createItemRow())
}

function removeItem(idx) {
  form.items.splice(idx, 1)
}

// ===== Customer Selector =====
let _customerSearchSeq = 0
async function searchCustomers(query) {
  clearTimeout(customerSearchTimer)
  if (!query && customerOptions.value.length) return

  customerSearchTimer = setTimeout(async () => {
    const seq = ++_customerSearchSeq
    customerSearching.value = true
    try {
      const params = { size: 50 }
      if (query) params.keyword = query.trim()
      const res = await getCustomers(params)
      if (seq !== _customerSearchSeq) return
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
      customerOptions.value = records
    } catch (e) {
      console.error('搜索客户失败:', e)
      if (seq === _customerSearchSeq) customerOptions.value = []
    } finally {
      customerSearching.value = false
    }
  }, 300)
}

function onCustomerSelect(name) {
  const customer = customerOptions.value.find((c) => c.name === name)
  if (customer) {
    form.customerId = customer.id
    form.customerName = customer.name
  }
}

// ===== Commodity Selector =====
let _commoditySearchSeq = 0
async function searchCommodities(query) {
  clearTimeout(commoditySearchTimer)
  if (!query && commodityOptions.value.length) return

  commoditySearchTimer = setTimeout(async () => {
    const seq = ++_commoditySearchSeq
    commoditySearching.value = true
    try {
      const params = { size: 50 }
      if (query) params.keyword = query.trim()
      const res = await getCommodities(params)
      if (seq !== _commoditySearchSeq) return
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
      commodityOptions.value = records
    } catch (e) {
      console.error('搜索商品失败:', e)
      commodityOptions.value = []
    } finally {
      commoditySearching.value = false
    }
  }, 300)
}

function onCommoditySelect(idx, commodityId) {
  const commodity = commodityOptions.value.find((c) => c.id === commodityId)
  if (!commodity) return
  const item = form.items[idx]
  item.productName = commodity.name || ''
  item.unitPrice = commodity.defaultPrice ?? null
  item.unit = commodity.unit || ''
  item.productCategory = commodity.category || ''
  item.spec = commodity.spec || ''
}

// ===== Save =====
async function handleSave() {
  if (saving.value) return
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (!form.items.length) {
    ElMessage.warning('请至少添加一个退货商品')
    return
  }

  // Validate items
  for (let i = 0; i < form.items.length; i++) {
    const it = form.items[i]
    if (!it.productName && !it.commodityId) {
      ElMessage.warning(`第 ${i + 1} 行商品信息不完整，请选择商品或填写名称`)
      return
    }
    if (it.quantity == null || it.quantity <= 0) {
      ElMessage.warning(`第 ${i + 1} 行数量必须大于 0`)
      return
    }
    if (it.unitPrice == null || it.unitPrice < 0) {
      ElMessage.warning(`第 ${i + 1} 行单价不能为负数`)
      return
    }
  }

  saving.value = true
  try {
    const data = {
      originalOrderNo: form.originalOrderNo.trim(),
      customerName: form.customerName.trim(),
      customerId: form.customerId,
      returnDate: form.returnDate,
      status: editId.value ? undefined : 'pending',
      remark: form.remark.trim(),
      items: form.items.map((it) => ({
        commodityId: it.commodityId,
        productName: it.productName,
        productCategory: it.productCategory,
        spec: it.spec,
        unit: it.unit,
        quantity: it.quantity,
        unitPrice: it.unitPrice,
        remark: it.remark,
      })),
    }

    if (editId.value) {
      await updateSaleReturn(editId.value, data)
      afterEditSave({ id: editId.value, ...data })
    } else {
      await addSaleReturn(data)
      ElMessage.success('退货单已创建')
    }
    resetDirty()
    dialogVisible.value = false
    loadList()
    loadSummary()
  } catch (e) {
    console.error('保存退货单失败:', e)
    ElMessage.error(editId.value ? '更新失败，请稍后重试' : '创建失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// ===== Print =====
const printComp = ref(null)

function handlePrint() {
  if (!detailOrder.value) {
    ElMessage.warning('请先打开一个退货单详情再打印')
    return
  }
  printComp.value?.doPrint()
}

// ===== Export =====
async function fetchItemsChunked(orders, batchFn, chunkSize = 100) {
  const ids = orders.map(o => o.id).filter(Boolean)
  if (!ids.length) return
  try {
    const itemsMap = {}
    for (let i = 0; i < ids.length; i += chunkSize) {
      const chunk = ids.slice(i, i + chunkSize)
      const res = await batchFn(chunk)
      const data = res.data?.data ?? res.data ?? {}
      Object.assign(itemsMap, data)
    }
    for (const order of orders) {
      if (itemsMap[order.id]) order.items = itemsMap[order.id]
    }
  } catch (e) {
    console.error('获取商品明细失败:', e)
  }
}

function handleExportCommand(cmd) {
  const [scope, mode] = cmd.split('-')
  if (scope === 'page') handleExport(mode)
  else handleExportAll(mode)
}

async function handleExport(mode = 'detail') {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const orders = list.value
  if (mode === 'detail') await fetchItemsChunked(orders, getSaleReturnItemsBatch)
  await doExport(orders, mode)
}

async function handleExportAll(mode = 'detail') {
  const loadingMsg = ElMessage({ message: '正在导出全部退货单...', type: 'info', duration: 0 })
  try {
    const params = { page: 1, size: 10000 }
    if (activeStatus.value) params.status = activeStatus.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getSaleReturns(params)
    const data = res.data?.data ?? res.data
    const orders = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
    if (mode === 'detail') await fetchItemsChunked(orders, getSaleReturnItemsBatch)
    await doExport(orders, mode)
  } catch (e) {
    console.error('导出全部失败:', e)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    loadingMsg.close()
  }
}

async function doExport(orders, mode = 'detail') {
  const allColumns = [
    { key: 'orderNo', label: '退货单号', width: 18 },
    { key: 'originalOrderNo', label: '原订单号', width: 18 },
    { key: 'customerName', label: '客户名称', width: 16 },
    { key: 'returnDate', label: '退货日期', width: 14, format: fmtDate },
    { key: 'totalAmount', label: '总金额', width: 14, format: fmtMoney },
    { key: 'paidAmount', label: '已退金额', width: 14, format: fmtMoney },
    { key: 'status', label: '状态', width: 10, format: (v) => statusLabel(v) },
    { key: 'remark', label: '备注', width: 20 },
    { key: 'productName', label: '商品名称', width: 16 },
    { key: 'productCategory', label: '分类', width: 10 },
    { key: 'spec', label: '规格', width: 12 },
    { key: 'unit', label: '单位', width: 8 },
    { key: 'quantity', label: '数量', width: 10 },
    { key: 'unitPrice', label: '单价', width: 12, format: fmtMoney },
    { key: 'amount', label: '金额', width: 12, format: fmtMoney },
    { key: 'itemRemark', label: '明细备注', width: 16 },
  ]
  const ITEM_KEYS = ['productName','productCategory','spec','unit','quantity','unitPrice','amount','itemRemark']
  const columns = mode === 'detail' ? allColumns : allColumns.filter(c => !ITEM_KEYS.includes(c.key))
  let flatRows
  if (mode === 'simple') {
    flatRows = orders
  } else {
    flatRows = []
    for (const order of orders) {
      const items = order.items && order.items.length ? order.items : [{}]
      for (const item of items) {
        const row = {}
        for (const col of columns) {
          if (ITEM_KEYS.includes(col.key)) {
            if (col.key === 'amount') {
              row[col.key] = item.amount ?? multiplyMoney(Number(item.quantity) || 0, Number(item.unitPrice) || 0)
            } else if (col.key === 'itemRemark') {
              row[col.key] = item.remark
            } else {
              row[col.key] = item[col.key]
            }
          } else {
            row[col.key] = order[col.key]
          }
        }
        flatRows.push(row)
      }
    }
  }
  await exportToExcel(flatRows, columns, `销售退货单`, {
    sheetName: '销售退货',
    mergeKeys: mode === 'detail' ? allColumns.filter(c => !ITEM_KEYS.includes(c.key)).map(c => c.key) : [],
  })
  ElMessage.success('导出成功')
}

function toLocalDateStr(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function transformImportRecord(record) {
  return {
    ...record,
    returnDate: record.returnDate || toLocalDateStr(new Date()),
    items: (record.items || []).map(item => {
      const { itemRemark, ...rest } = item
      return {
        ...rest,
        remark: itemRemark || '',
      }
    }),
  }
}

// Initial loads
onMounted(() => {
  searchCommodities('')
  searchCustomers('')
})

watch(() => route.query.detailId, (val) => {
  if (!val) return
  const id = Number(val)
  if (Number.isInteger(id) && id > 0) nextTick(() => openDetail({ id }))
}, { immediate: true })
</script>

<style scoped>
/* ===== Page Header ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: -0.5px;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-primary {
  padding: 9px 22px;
  background: #C5A265;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}
.btn-primary:hover {
  background: #b89555;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  padding: 9px 22px;
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

/* ===== Toolbar ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Status Tabs */
.status-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 3px;
}
.status-tab {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  white-space: nowrap;
}
.status-tab:hover {
  color: #C5A265;
  background: rgba(197, 162, 101, 0.06);
}
.status-tab.active {
  color: #fff;
  background: #C5A265;
  font-weight: 600;
}

/* Date picker */

/* Search */
.search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
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
  color: #ccc;
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
.summary-card.green .sc-value { color: #69db7c; }

/* ===== Table ===== */
.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: auto;
  min-height: 120px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1000px;
}
.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 12px;
  padding: 13px 14px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
  user-select: none;
}
.data-table th.sortable { cursor: pointer; }
.data-table th.sortable:hover { color: #C5A265; }
.data-table td {
  padding: 13px 14px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
  vertical-align: middle;
}
.data-table tbody tr {
  transition: background 0.12s;
}
.data-table tbody tr:hover {
  background: #fafbfc;
}

.col-order-no {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}
.col-customer {
  font-weight: 600;
  color: #1a1a1a;
}
.col-date {
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}
.col-money {
  font-weight: 600;
  white-space: nowrap;
  font-size: 13px;
  color: #555;
}
.col-money.gold {
  color: #C5A265;
}
.col-status {
  white-space: nowrap;
}
.col-action {
  white-space: nowrap;
}

/* Status Badges */
.status-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.status-pending {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}
.status-processing {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
.status-completed {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.status-cancelled {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
}

/* Action buttons */
.btn-text {
  background: none;
  border: none;
  color: #C5A265;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.15s;
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
.status-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.status-btn svg {
  transition: transform 0.2s;
}

/* Empty state */
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

/* ===== Detail Drawer ===== */
.detail-content {
  padding: 0 4px;
}
.detail-section {
  margin-bottom: 28px;
}
.detail-section:last-child {
  margin-bottom: 0;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
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
  gap: 14px 20px;
}
.detail-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-field.full {
  grid-column: 1 / -1;
}
.field-label {
  font-size: 11px;
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.field-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}
.field-value.mono {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  color: #666;
}
.field-value.money {
  font-weight: 700;
  font-size: 14px;
  color: #333;
}
.field-value.money.gold {
  color: #C5A265;
}

.clear-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  background: #fff7e6;
  color: #d46b08;
}
.clear-tag.cleared {
  background: #f6ffed;
  color: #389e0d;
}

/* Detail items table */
.items-table-wrap {
  overflow-x: auto;
}
.items-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 480px;
}
.items-table th {
  background: #fafbfc;
  font-weight: 600;
  color: #666;
  font-size: 12px;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}
.items-table td {
  padding: 10px 12px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
}
.items-table tfoot td {
  border-top: 2px solid #eee;
  font-weight: 700;
  padding: 12px;
}
.items-table .col-num {
  text-align: center;
  min-width: 60px;
}
.items-table .col-money {
  text-align: right;
  white-space: nowrap;
}
.items-table .col-money.gold {
  color: #C5A265;
}
.items-table .total-label {
  text-align: right;
  color: #666;
  font-size: 13px;
}
.items-table .total-value {
  font-size: 15px;
  color: #C5A265;
  font-weight: 700;
}
.empty-items {
  text-align: center;
  padding: 30px 0;
  color: #ccc;
  font-size: 13px;
}

/* ===== Add/Edit Dialog ===== */
.dialog-section {
  margin-bottom: 24px;
}
.dialog-section:last-of-type {
  margin-bottom: 8px;
}

.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0 16px;
  margin-bottom: 4px;
}
.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
  margin-bottom: 4px;
}
.span-2 {
  grid-column: span 2;
}

/* Items edit table */
.items-edit-wrap {
  overflow-x: auto;
  margin-top: 8px;
}
.items-edit-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}
.items-edit-table th {
  background: #fafbfc;
  font-weight: 600;
  color: #666;
  font-size: 12px;
  padding: 8px 6px;
  text-align: left;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}
.items-edit-table td {
  padding: 6px 4px;
  vertical-align: top;
  border-bottom: 1px solid #f5f5f5;
}
.items-edit-table .col-commodity {
  width: 180px;
  min-width: 160px;
}
.items-edit-table .col-category {
  width: 80px;
}
.items-edit-table .col-spec {
  width: 80px;
}
.items-edit-table .col-unit {
  width: 60px;
}
.items-edit-table .col-num {
  width: 80px;
}
.items-edit-table .col-price {
  width: 100px;
}
.items-edit-table .col-amount {
  white-space: nowrap;
  font-weight: 600;
  color: #C5A265;
  text-align: right;
  padding-right: 10px;
  font-size: 13px;
  vertical-align: middle;
}
.items-edit-table .col-remark {
  width: 100px;
}
.items-edit-table .col-act {
  width: 40px;
  text-align: center;
}
.items-edit-table tfoot td {
  border-top: 2px solid #eee;
  font-weight: 700;
  padding: 10px 6px;
}
.items-edit-table .total-label {
  text-align: right;
  color: #666;
  font-size: 13px;
}
.items-edit-table .total-value {
  font-size: 15px;
  color: #C5A265;
  text-align: right;
  padding-right: 10px;
}

.btn-add-item {
  margin-left: auto;
  padding: 4px 14px;
  background: rgba(197, 162, 101, 0.1);
  color: #C5A265;
  border: 1px solid rgba(197, 162, 101, 0.3);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-add-item:hover {
  background: rgba(197, 162, 101, 0.2);
  border-color: #C5A265;
}

.btn-remove-item {
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  color: #ccc;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.btn-remove-item:hover {
  background: rgba(231, 76, 60, 0.08);
  color: #e74c3c;
}

.empty-items-edit {
  text-align: center;
  padding: 30px 0;
  color: #ccc;
  font-size: 13px;
  border: 1px dashed #e8e8e8;
  border-radius: 8px;
  margin-top: 8px;
}

/* ===== Element Plus Overrides ===== */
.btn-gold {
  background: #C5A265 !important;
  border-color: #C5A265 !important;
}
.btn-gold:hover,
.btn-gold:focus {
  background: #d4b885 !important;
  border-color: #d4b885 !important;
}

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
  max-height: calc(100vh - 200px);
  overflow-y: auto;
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
:deep(.el-drawer__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
  margin-bottom: 0;
}
:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
:deep(.el-drawer__body) {
  padding: 20px;
}
:deep(.el-date-editor) {
  --el-date-editor-width: 155px;
}
:deep(.el-dropdown-menu__item.is-disabled) {
  opacity: 0.4;
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
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right {
    flex-wrap: wrap;
  }
  .search-input {
    width: 100%;
  }
  .status-tabs {
    overflow-x: auto;
  }
  .summary-bar {
    flex-wrap: wrap;
  }
  .summary-card {
    flex: 1 1 33%;
    min-width: 120px;
  }
  .form-row-3,
  .form-row-2 {
    grid-template-columns: 1fr;
  }
  .span-2 {
    grid-column: span 1;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

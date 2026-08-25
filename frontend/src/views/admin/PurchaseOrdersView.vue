<template>
  <div class="page admin-page">
    <!-- ===== Header ===== -->
    <div class="page-header">
      <h2>采购订单</h2>
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
        <button class="btn-outline" @click="handlePrint">打印</button>
        <button class="btn-primary" @click="openAdd">+ 新建采购单</button>
      </div>
    </div>

    <!-- ===== Status Tabs ===== -->
    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        class="tab-btn"
        :class="{ active: activeStatus === tab.value }"
        @click="switchStatus(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count != null" class="tab-count">{{ tab.count }}</span>
      </button>
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
        </div>
      </div>
      <div class="toolbar-right">
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            v-model="keyword"
            placeholder="搜单号 / 供应商 / 联系人..."
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
        <div class="sc-label">订单数</div>
        <div class="sc-value">{{ summary.totalCount ?? 0 }} 单</div>
      </div>
      <div class="summary-card gold">
        <div class="sc-label">总金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalAmount) }}</div>
      </div>
      <div class="summary-card green">
        <div class="sc-label">已付金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalPaid) }}</div>
      </div>
      <div class="summary-card red">
        <div class="sc-label">未付金额</div>
        <div class="sc-value">{{ formatMoney((summary.totalAmount || 0) - (summary.totalPaid || 0)) }}</div>
      </div>
    </div>

    <!-- ===== Table ===== -->
    <div class="card">
      <TableSkeleton v-if="loading" :columns="10" :rows="8" />

      <table class="data-table" v-else-if="list.length">
        <thead>
          <tr>
            <th style="width:40px"><input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAllRows" /></th>
            <th class="col-orderno sortable" @click="toggleSort('orderNo')">单号{{ sortArrow('orderNo') }}</th>
            <th class="sortable" @click="toggleSort('supplierName')">供应商{{ sortArrow('supplierName') }}</th>
            <th>联系人</th>
            <th>电话</th>
            <th class="col-date sortable" @click="toggleSort('orderDate')">日期{{ sortArrow('orderDate') }}</th>
            <th class="col-money sortable" @click="toggleSort('totalAmount')">总金额{{ sortArrow('totalAmount') }}</th>
            <th class="col-money sortable" @click="toggleSort('paidAmount')">已付{{ sortArrow('paidAmount') }}</th>
            <th class="col-status">状态</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in sortedList" :key="row.id" :class="{ 'selected-row': selectedIds.has(row.id) }" class="row-clickable" @click="handleRowClick($event, row)">
            <td><input type="checkbox" :checked="selectedIds.has(row.id)" @change="toggleSelectRow(row.id)" /></td>
            <td class="col-orderno">
              <span class="order-no" @click="openDetail(row)">{{ row.orderNo }}</span>
            </td>
            <td class="col-supplier">{{ row.supplierName || '-' }}</td>
            <td>{{ row.supplierContact || '-' }}</td>
            <td>
              <a v-if="row.supplierPhone" :href="'tel:' + row.supplierPhone" class="phone-link">{{ row.supplierPhone }}</a>
              <span v-else class="cell-na">-</span>
            </td>
            <td class="col-date">{{ formatDate(row.orderDate) }}</td>
            <td class="col-money">{{ formatMoney(row.totalAmount) }}</td>
            <td class="col-money" :class="{ 'money-cleared': row.isCleared }">
              {{ formatMoney(row.paidAmount) }}
              <span v-if="row.isCleared" class="cleared-badge">结清</span>
            </td>
            <td class="col-status">
              <span class="status-tag" :class="'status-' + row.status">{{ statusText(row.status) }}</span>
            </td>
            <td class="col-action">
              <button class="btn-text" @click="openDetail(row)">详情</button>
              <button class="btn-text" @click="openEdit(row)">编辑</button>
              <el-dropdown trigger="click" @command="(cmd) => handleDropdownCommand(row, cmd)">
                <button class="btn-text status-btn">状态<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="pending" :disabled="row.status === 'pending'">待处理</el-dropdown-item>
                    <el-dropdown-item command="processing" :disabled="row.status === 'processing'">进行中</el-dropdown-item>
                    <el-dropdown-item command="completed" :disabled="row.status === 'completed'">已完成</el-dropdown-item>
                    <el-dropdown-item command="cancelled" :disabled="row.status === 'cancelled'">已取消</el-dropdown-item>
                    <el-dropdown-item :command="row.isCleared ? 'unclear' : 'clear'" divided>
                      <span :style="{ color: row.isCleared ? '#faad14' : '#52c41a' }">{{ row.isCleared ? '取消结清' : '标记结清' }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <button class="btn-text danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <p>{{ hasFilter ? '未匹配到采购订单' : '暂无采购订单，点击右上角新建' }}</p>
      </div>
    </div>

    <!-- ===== Batch Action Bar ===== -->
    <div class="batch-bar" v-if="selectedIds.size > 0">
      <span class="batch-count">已选 {{ selectedIds.size }} 项</span>
      <el-dropdown trigger="click" @command="batchChangeStatus">
        <button class="btn-outline batch-btn">批量改状态 <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="pending">待处理</el-dropdown-item>
            <el-dropdown-item command="processing">进行中</el-dropdown-item>
            <el-dropdown-item command="completed">已完成</el-dropdown-item>
            <el-dropdown-item command="cancelled">已取消</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button class="btn-outline batch-btn" @click="selectedIds = new Set()">取消选择</button>
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
      title="采购订单详情"
      size="720px"
      close-on-press-escape
      :destroy-on-close="true"
      @closed="router.replace({ query: { ...route.query, detailId: undefined } })"
    >
      <div class="drawer-content" v-if="detailOrder">
        <!-- Order Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>订单信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">订单编号</span>
              <span class="detail-val mono">{{ detailOrder.orderNo }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">订单日期</span>
              <span class="detail-val">{{ formatDate(detailOrder.orderDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">订单状态</span>
              <span class="status-tag" :class="'status-' + detailOrder.status">{{ statusText(detailOrder.status) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">已结清</span>
              <span class="detail-val">{{ detailOrder.isCleared ? '是' : '否' }}</span>
            </div>
          </div>
        </div>

        <!-- Supplier Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>供应商信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">供应商名称</span>
              <span class="detail-val">{{ detailOrder.supplierName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">联系人</span>
              <span class="detail-val">{{ detailOrder.supplierContact || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">电话</span>
              <span class="detail-val">{{ detailOrder.supplierPhone || '-' }}</span>
            </div>
            <div class="detail-item full">
              <span class="detail-label">地址</span>
              <span class="detail-val">{{ detailOrder.supplierAddress || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- Amount Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>金额信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">总金额</span>
              <span class="detail-val money-gold">¥{{ formatMoney(detailOrder.totalAmount) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">已付金额</span>
              <span class="detail-val money-green">¥{{ formatMoney(detailOrder.paidAmount) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">未付金额</span>
              <span class="detail-val money-red">¥{{ formatMoney((detailOrder.totalAmount || 0) - (detailOrder.paidAmount || 0)) }}</span>
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
      <template #footer>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <div>
            <el-button @click="exportImage">导出图片</el-button>
            <el-button @click="exportPDF">导出PDF</el-button>
          </div>
          <el-button type="primary" @click="handlePrint">打印订单</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- Print Template -->
    <div style="position:absolute;left:-9999px;top:0;pointer-events:none;">
      <PrintOrder
        ref="printComp"
        title="采购单"
        :order="detailOrder || {}"
        :items="detailItems"
        type="purchase"
        :print-config="printConfig"
      />
    </div>

    <!-- ===== Add / Edit Dialog ===== -->
    <div class="modal-overlay" v-if="showModal" @click.self="closeModal" @keydown.esc="closeModal" tabindex="-1">
      <div class="modal modal-order">
        <div class="modal-header">
          <h3>{{ editId ? '编辑采购单' : '新建采购单' }}</h3>
          <button class="modal-close" @click="closeModal">&times;</button>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <!-- Supplier Section -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>供应商信息</h4>
            <div class="form-row">
              <div class="form-group">
                <label>供应商 <span class="req">*</span></label>
                <div class="supplier-select-wrap">
                  <select v-model="form.supplierId" @change="onSupplierChange" class="form-select">
                    <option :value="null" disabled>请选择供应商</option>
                    <option v-for="s in supplierOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
                  </select>
                </div>
              </div>
              <div class="form-group">
                <label>联系人</label>
                <input v-model="form.supplierContact" placeholder="联系人姓名" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>电话</label>
                <input v-model="form.supplierPhone" placeholder="联系电话" />
              </div>
              <div class="form-group">
                <label>地址</label>
                <input v-model="form.supplierAddress" placeholder="供应商地址" />
              </div>
            </div>
          </div>

          <!-- Order Info Section -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>订单信息</h4>
            <div class="form-row">
              <div class="form-group">
                <label>订单日期 <span class="req">*</span></label>
                <input v-model="form.orderDate" type="date" required />
              </div>
              <div class="form-group">
                <label>备注</label>
                <input v-model="form.remark" placeholder="订单备注（选填）" />
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

    <!-- ===== Status Change Dialog ===== -->
    <div class="modal-overlay" v-if="statusDialogVisible" @click.self="statusDialogVisible = false" @keydown.esc="statusDialogVisible = false" tabindex="-1">
      <div class="modal modal-status">
        <div class="modal-header">
          <h3>更改订单状态</h3>
          <button class="modal-close" @click="statusDialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="status-change-info" v-if="statusTarget">
            <p>订单编号：<strong>{{ statusTarget.orderNo }}</strong></p>
            <p>当前状态：<span class="status-tag" :class="'status-' + statusTarget.status">{{ statusText(statusTarget.status) }}</span></p>
          </div>
          <div class="form-group" style="margin-top: 16px;">
            <label>新状态 <span class="req">*</span></label>
            <select v-model="statusNewValue" class="form-select">
              <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="statusDialogVisible = false">取消</button>
          <button type="button" class="btn-primary" @click="confirmStatusChange">确定更改</button>
        </div>
      </div>
    </div>
    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入采购订单"
      :column-map="importConfigs.purchaseOrders.columns"
      :module="'purchaseOrders'"
      group-by="orderNo"
      :transform-record="transformImportRecord"
      :on-complete="() => { loadList(); loadSummary() }"
      :sample-data="importConfigs.purchaseOrders.sample"
      template-filename="采购订单导入模板"
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
import { toPng } from 'html-to-image'
import { jsPDF } from 'jspdf'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import TableSkeleton from '@/components/TableSkeleton.vue'
import { importConfigs } from '@/utils/importConfigs'
import {
  getPurchaseOrders,
  getPurchaseOrderDetail,
  getPurchaseOrderItemsBatch,
  addPurchaseOrder,
  updatePurchaseOrder,
  deletePurchaseOrder,
  updatePurchaseOrderStatus,
  togglePurchaseOrderCleared,
  getPurchaseOrderSummary,
  getCommodities,
  getSuppliers,
  restoreEntity,
} from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { useOrderLock } from '@/composables/useOrderLock'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { useRecentOrders } from '@/composables/useRecentOrders'
import { useTableSort } from '@/composables/useTableSort'

// ===== Status helpers =====
const STATUS_MAP = {
  pending: '待处理',
  processing: '进行中',
  completed: '已完成',
  cancelled: '已取消',
}
const STATUS_OPTIONS = [
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '进行中' },
  { value: 'completed', label: '已完成' },
  { value: 'cancelled', label: '已取消' },
]

function statusText(s) {
  return STATUS_MAP[s] || s || '-'
}

// ===== State =====
const importDialogVisible = ref(false)
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { orderDate: 'date', totalAmount: 'number', paidAmount: 'number' }, { storageKey: 'purchase_orders_filters' })
const loading = ref(false)
const selectedIds = ref(new Set())
const saving = ref(false)
const keyword = ref('')
const startDate = ref(null)
const endDate = ref(null)
const activeStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const summary = ref(null)

// ===== URL 筛选状态持久化 =====
const route = useRoute()
const router = useRouter()
const FILTER_KEY = 'purchase_orders_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.status || q.keyword || q.startDate || q.page
  if (hasUrlParams) {
    _restoring = true
    if (q.status) activeStatus.value = q.status
    if (q.keyword) keyword.value = q.keyword
    if (q.startDate) startDate.value = q.startDate
    if (q.endDate) endDate.value = q.endDate
    if (q.page && Number(q.page) > 1) currentPage.value = Number(q.page)
    if (q.size && Number(q.size) > 0) pageSize.value = Number(q.size)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.status) activeStatus.value = saved.status
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.startDate) startDate.value = saved.startDate
    if (saved.endDate) endDate.value = saved.endDate
    if (saved.page && saved.page > 1) currentPage.value = saved.page
    if (saved.size && saved.size > 0) pageSize.value = saved.size
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (activeStatus.value) { query.status = activeStatus.value; storage.status = activeStatus.value }
  if (keyword.value) { query.keyword = keyword.value; storage.keyword = keyword.value }
  if (startDate.value) { query.startDate = startDate.value; storage.startDate = startDate.value }
  if (endDate.value) { query.endDate = endDate.value; storage.endDate = endDate.value }
  if (currentPage.value > 1) { query.page = String(currentPage.value); storage.page = currentPage.value }
  if (pageSize.value !== 20) { query.size = String(pageSize.value); storage.size = pageSize.value }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

// Detail drawer
const drawerVisible = ref(false)
const detailOrder = ref(null)
const printComp = ref(null)
const detailItems = ref([])

// Add/Edit modal
const showModal = ref(false)
const editId = ref(null)

// Status change dialog
const statusDialogVisible = ref(false)
const statusTarget = ref(null)
const statusNewValue = ref('')

// Options
const supplierOptions = ref([])
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
  supplierId: null,
  supplierName: '',
  supplierContact: '',
  supplierPhone: '',
  supplierAddress: '',
  orderDate: toLocalDateStr(new Date()),
  status: 'pending',
  remark: '',
  items: [defaultItem()],
})

const form = reactive(defaultForm())

// ===== Undo Delete =====
const { handleDelete: undoHandleDelete } = useUndoDelete({
  deleteApi: deletePurchaseOrder,
  restoreApi: (id) => restoreEntity('purchase-order', id),
  onSuccess: () => {
    if (list.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    loadList()
    loadSummary()
  },
  entityLabel: '采购单',
})

const { snapshotBeforeEdit: snapshotOrder, afterEditSave: afterOrderSave } = useUndoEdit({
  updateApi: updatePurchaseOrder,
  onSuccess: () => { loadList(); loadSummary() },
  entityLabel: '采购单',
})
const { acquireLock, releaseLock } = useOrderLock('purchase')
const { addRecent } = useRecentOrders()
watch(showModal, (val) => { if (!val) releaseLock() })
watch(currentPage, () => { if (!_restoring) { loadList(); syncFiltersToUrl() } })
watch(pageSize, () => {
  if (_restoring) return
  if (currentPage.value === 1) { loadList(); syncFiltersToUrl() }
  else { currentPage.value = 1 }
})

// ===== Status Tabs =====
const statusTabs = computed(() => {
  return [
    { value: '', label: '全部', count: total.value },
    { value: 'pending', label: '待处理', count: null },
    { value: 'processing', label: '进行中', count: null },
    { value: 'completed', label: '已完成', count: null },
    { value: 'cancelled', label: '已取消', count: null },
  ]
})

// ===== Computed =====
const hasFilter = computed(() => {
  return !!(keyword.value || startDate.value || endDate.value || activeStatus.value)
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

// ===== Lifecycle =====
onMounted(async () => {
  restoreFilters()
  await Promise.all([loadSupplierOptions(), loadCommodityOptions()])
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
  selectedIds.value = new Set()
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (activeStatus.value) params.status = activeStatus.value
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await getPurchaseOrders(params)
    const data = res.data?.data || res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data?.records || data?.list || data?.content || []
      total.value = data?.total ?? list.value.length
    }
  } catch (err) {
    console.error('加载采购订单列表失败:', err)
    if (!err._handled) ElMessage.error('加载列表失败，请稍后重试')
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
    const res = await getPurchaseOrderSummary(params)
    summary.value = res.data?.data || res.data || null
  } catch (err) {
    console.error('加载汇总失败:', err)
  }
}

async function loadSupplierOptions() {
  try {
    const res = await getSuppliers({ page: 1, size: 9999 })
    const data = res.data?.data || res.data
    supplierOptions.value = data?.records || data?.list || (Array.isArray(data) ? data : [])
  } catch (err) {
    console.error('加载供应商列表失败:', err)
    supplierOptions.value = []
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
function switchStatus(val) {
  activeStatus.value = val
  currentPage.value = 1
  loadList()
  syncFiltersToUrl()
}

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
  activeStatus.value = ''
  currentPage.value = 1
  loadList()
  loadSummary()
  syncFiltersToUrl()
}

function handleRowClick(e, row) {
  const tag = e.target.tagName
  if (tag === 'BUTTON' || tag === 'A' || tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return
  if (e.target.closest('button') || e.target.closest('a') || e.target.closest('.el-dropdown') || e.target.closest('.el-dialog')) return
  openDetail(row)
}

// ===== Detail Drawer =====
async function openDetail(row) {
  drawerVisible.value = true
  detailOrder.value = null
  detailItems.value = []
  try {
    const res = await getPurchaseOrderDetail(row.id)
    const data = res.data?.data || res.data
    detailOrder.value = data?.order || data || row
    detailItems.value = data?.items || []
    addRecent({ id: row.id, orderNo: detailOrder.value?.orderNo || row.orderNo, type: 'purchase', customerName: detailOrder.value?.supplierName || row.supplierName })
  } catch (err) {
    console.error('加载订单详情失败:', err)
    if (!err._handled) ElMessage.error('加载详情失败')
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

async function openEdit(row) {
  if (!await acquireLock(row.id)) return
  addRecent({ id: row.id, orderNo: row.orderNo, type: 'purchase', customerName: row.supplierName })
  snapshotOrder(row)
  editId.value = row.id
  // Populate order-level fields
  form.supplierId = row.supplierId ?? null
  form.supplierName = row.supplierName || ''
  form.supplierContact = row.supplierContact || ''
  form.supplierPhone = row.supplierPhone || ''
  form.supplierAddress = row.supplierAddress || ''
  form.orderDate = row.orderDate || toLocalDateStr(new Date())
  form.status = row.status || 'pending'
  form.remark = row.remark || ''
  form.items = [defaultItem()]

  // Load full detail to get items
  loadDetailForEdit(row.id)
  showModal.value = true
}

async function loadDetailForEdit(id) {
  try {
    const res = await getPurchaseOrderDetail(id)
    const data = res.data?.data || res.data
    const order = data?.order || data
    const items = data?.items || []

    // Update form with full order data
    if (order) {
      form.supplierId = order.supplierId ?? form.supplierId
      form.supplierName = order.supplierName || form.supplierName
      form.supplierContact = order.supplierContact || form.supplierContact
      form.supplierPhone = order.supplierPhone || form.supplierPhone
      form.supplierAddress = order.supplierAddress || form.supplierAddress
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
  showModal.value = false
}

// ===== Supplier change =====
function onSupplierChange() {
  const s = supplierOptions.value.find((s) => s.id === form.supplierId)
  if (s) {
    form.supplierName = s.name || ''
    form.supplierContact = s.contact || ''
    form.supplierPhone = s.phone || ''
    form.supplierAddress = s.address || ''
  }
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
  if (!form.supplierId && !form.supplierName.trim()) {
    ElMessage.warning('请选择或填写供应商')
    return
  }
  if (!form.orderDate) {
    ElMessage.warning('请选择订单日期')
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
      supplierId: form.supplierId,
      supplierName: form.supplierName.trim(),
      supplierContact: form.supplierContact.trim(),
      supplierPhone: form.supplierPhone.trim(),
      supplierAddress: form.supplierAddress.trim(),
      orderDate: form.orderDate,
      status: form.status,
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
      await updatePurchaseOrder(editId.value, data)
      afterOrderSave({ id: editId.value, ...data })
    } else {
      await addPurchaseOrder(data)
      ElMessage.success('采购单已创建')
    }
    showModal.value = false
    loadList()
    loadSummary()
  } catch (err) {
    console.error('保存采购单失败:', err)
    if (!err._handled) ElMessage.error(editId.value ? '更新失败，请稍后重试' : '创建失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// ===== Status change =====
function handleDropdownCommand(row, cmd) {
  if (cmd === 'clear' || cmd === 'unclear') {
    handleToggleCleared(row)
  } else {
    handleChangeStatus(row, cmd)
  }
}

async function handleChangeStatus(row, newStatus) {
  if (row.status === newStatus) return
  const label = statusText(newStatus)
  try {
    await ElMessageBox.confirm(
      `确定将订单「${row.orderNo}」的状态更改为「${label}」吗？`,
      '更改状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await updatePurchaseOrderStatus(row.id, newStatus)
    ElMessage.success(`已更改为「${label}」`)
    loadList()
    loadSummary()
  } catch (err) {
    console.error('更新状态失败:', err)
    if (!err._handled) ElMessage.error('状态更新失败')
  }
}

async function handleToggleCleared(row) {
  const label = row.isCleared ? '未结清' : '已结清'
  try {
    await ElMessageBox.confirm(
      `确定将订单「${row.orderNo}」标记为「${label}」吗？`,
      '更改结清状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await togglePurchaseOrderCleared(row.id)
    ElMessage.success(`已标记为「${label}」`)
    loadList()
    loadSummary()
  } catch (err) {
    console.error('切换结清状态失败:', err)
    if (!err._handled) ElMessage.error('操作失败')
  }
}

function handleStatusChange(row) {
  statusTarget.value = row
  statusNewValue.value = row.status
  statusDialogVisible.value = true
}

async function confirmStatusChange() {
  if (!statusTarget.value || !statusNewValue.value) return
  if (statusNewValue.value === statusTarget.value.status) {
    statusDialogVisible.value = false
    return
  }
  try {
    await updatePurchaseOrderStatus(statusTarget.value.id, statusNewValue.value)
    ElMessage.success(`状态已更改为「${statusText(statusNewValue.value)}」`)
    statusDialogVisible.value = false
    loadList()
    loadSummary()
  } catch (err) {
    console.error('更新状态失败:', err)
    if (!err._handled) ElMessage.error('状态更新失败')
  }
}

// ===== Batch Operations =====
const isAllSelected = computed(() => list.value.length > 0 && list.value.every(row => selectedIds.value.has(row.id)))
const isIndeterminate = computed(() => {
  const count = list.value.filter(row => selectedIds.value.has(row.id)).length
  return count > 0 && count < list.value.length
})

function toggleSelectAllRows() {
  if (isAllSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(list.value.map(row => row.id))
  }
}

function toggleSelectRow(id) {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  selectedIds.value = s
}

async function batchChangeStatus(newStatus) {
  const label = statusText(newStatus)
  const ids = [...selectedIds.value]
  try {
    await ElMessageBox.confirm(
      `确定将 ${ids.length} 个订单的状态更改为「${label}」吗？`,
      '批量更改状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    await Promise.all(ids.map(id => updatePurchaseOrderStatus(id, newStatus)))
    ElMessage.success(`已将 ${ids.length} 个订单状态更改为「${label}」`)
    selectedIds.value = new Set()
    loadList()
    loadSummary()
  } catch (e) {
    console.error('批量更改状态失败:', e)
    if (!e._handled) ElMessage.error('部分订单状态更改失败，请检查')
  }
}

// ===== Delete =====
function handleDelete(row) {
  undoHandleDelete(row)
}

// ===== Print =====
function handlePrint() {
  if (!detailOrder.value) {
    ElMessage.warning('请先打开一个采购单详情再打印')
    return
  }
  printComp.value?.doPrint()
}

function getExportFileName() {
  return detailOrder.value?.orderNo || '采购单'
}

async function exportImage() {
  if (!printComp.value?.$el) return
  const { saveDataUrl } = await import('@/utils/download')
  const dataUrl = await toPng(printComp.value.$el, { pixelRatio: 4, cacheBust: true })
  await saveDataUrl(dataUrl, `${getExportFileName()}.png`, { successMsg: '图片已保存' })
}

async function exportPDF() {
  if (!printComp.value?.$el) return
  // pixelRatio 5 gives ~480 DPI on A4 — solid borders, no dashed lines
  const dataUrl = await toPng(printComp.value.$el, { pixelRatio: 5, cacheBust: true })
  const orient = printConfig.value?.orientation || 'portrait'
  const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: orient })
  const pageW = orient === 'landscape' ? 297 : 210
  const pageH = orient === 'landscape' ? 210 : 297
  pdf.addImage(dataUrl, 'PNG', 0, 0, pageW, pageH, undefined, 'FAST')
  const blob = new Blob([pdf.output('arraybuffer')], { type: 'application/pdf' })
  const { saveFile } = await import('@/utils/download')
  await saveFile(blob, `${getExportFileName()}.pdf`, { successMsg: 'PDF已保存' })
}

// ===== Export =====
function handleExportCommand(cmd) {
  const [scope, mode] = cmd.split('-')
  if (scope === 'page') handleExport(mode)
  else handleExportAll(mode)
}

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

async function handleExport(mode = 'detail') {
  const orders = list.value
  if (mode === 'detail') await fetchItemsChunked(orders, getPurchaseOrderItemsBatch)
  await doExport(orders, mode)
}

async function doExport(orders, mode = 'detail') {
  const allColumns = [
    { key: 'orderNo', label: '订单编号', width: 18 },
    { key: 'supplierName', label: '供应商', width: 14 },
    { key: 'supplierContact', label: '联系人', width: 10 },
    { key: 'supplierPhone', label: '电话', width: 14 },
    { key: 'supplierAddress', label: '地址', width: 20 },
    { key: 'orderDate', label: '订单日期', width: 12, format: fmtDate },
    { key: 'totalAmount', label: '总金额', width: 14, format: fmtMoney },
    { key: 'paidAmount', label: '已付', width: 12, format: fmtMoney },
    { key: 'status', label: '状态', width: 8, format: (v) => statusText(v) },
    { key: 'remark', label: '备注', width: 20 },
    { key: 'productName', label: '商品名称', width: 16 },
    { key: 'productCategory', label: '分类', width: 10 },
    { key: 'spec', label: '规格', width: 12 },
    { key: 'unit', label: '单位', width: 8 },
    { key: 'warehouseLoc', label: '库位', width: 10 },
    { key: 'quantity', label: '数量', width: 10 },
    { key: 'unitPrice', label: '单价', width: 12, format: fmtMoney },
    { key: 'amount', label: '金额', width: 12, format: fmtMoney },
    { key: 'itemRemark', label: '明细备注', width: 16 },
  ]
  const ITEM_KEYS = ['productName','productCategory','spec','unit','warehouseLoc','quantity','unitPrice','amount','itemRemark']
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
  await exportToExcel(flatRows, columns, `采购订单_${toLocalDateStr(new Date())}`, {
    sheetName: '采购订单',
    mergeKeys: mode === 'detail' ? allColumns.filter(c => !ITEM_KEYS.includes(c.key)).map(c => c.key) : [],
  })
  ElMessage.success('导出成功')
}

async function handleExportAll(mode = 'detail') {
  const loadingMsg = ElMessage({ message: '正在导出全部订单...', type: 'info', duration: 0 })
  try {
    const params = { page: 1, size: 10000 }
    if (activeStatus.value) params.status = activeStatus.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getPurchaseOrders(params)
    const data = res.data?.data ?? res.data
    const orders = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])

    if (mode === 'detail') await fetchItemsChunked(orders, getPurchaseOrderItemsBatch)
    await doExport(orders, mode)
  } catch (e) {
    console.error('导出全部失败:', e)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    loadingMsg.close()
  }
}

function transformImportRecord(record) {
  return {
    ...record,
    orderDate: record.orderDate || toLocalDateStr(new Date()),
    items: (record.items || []).map(item => {
      const { itemRemark, ...rest } = item
      return {
        ...rest,
        remark: itemRemark || '',
      }
    }),
  }
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

/* ===== Status Tabs ===== */
.status-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 0;
}
.tab-btn {
  padding: 8px 18px;
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 500;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
  font-family: inherit;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tab-btn:hover {
  color: #C5A265;
}
.tab-btn.active {
  color: #C5A265;
  border-bottom-color: #C5A265;
  font-weight: 600;
}
.tab-count {
  font-size: 11px;
  font-weight: 600;
  background: rgba(197, 162, 101, 0.1);
  color: #C5A265;
  padding: 0 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
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

.btn-outline {
  padding: 9px 18px;
  background: transparent;
  color: #C5A265;
  border: 1px solid #C5A265;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-outline:hover {
  background: rgba(197, 162, 101, 0.08);
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
.summary-card.red .sc-value   { color: #ff6b6b; }

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
  min-width: 900px;
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
.data-table tbody tr.row-clickable { cursor: pointer; }

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
.col-supplier {
  font-weight: 600;
  color: #1a1a1a;
}
.col-date {
  white-space: nowrap;
  font-size: 12px;
  color: #888;
}
.col-money {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.money-cleared {
  color: #52c41a;
}
.cleared-badge {
  display: inline-block;
  margin-left: 4px;
  padding: 0 6px;
  font-size: 10px;
  font-weight: 600;
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  vertical-align: middle;
}
.col-status {
  white-space: nowrap;
}
.col-action {
  white-space: nowrap;
}

/* Status tags */
.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
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
  border: 1px solid #e0e0e0;
}

.phone-link {
  color: #C5A265;
  text-decoration: none;
  transition: color 0.2s;
}
.phone-link:hover {
  color: #b89555;
  text-decoration: underline;
}
.cell-na {
  color: #ccc;
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
.status-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.status-btn svg {
  transition: transform 0.2s;
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

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; margin-top: 12px;
  background: #1a2332; border-radius: 10px;
  border: 1px solid rgba(197,162,101,.3);
}
.batch-count { color: #C5A265; font-weight: 600; font-size: 13px; }
.batch-btn { padding: 6px 14px; font-size: 12px; }
.selected-row { background: rgba(197,162,101,.06) !important; }

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
.money-green {
  color: #52c41a !important;
  font-weight: 600;
}
.money-red {
  color: #e74c3c !important;
  font-weight: 600;
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
.modal-status {
  width: 420px;
  max-width: 90vw;
}
.status-change-info {
  padding: 12px 16px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.status-change-info p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
}
.status-change-info p:last-child {
  margin-bottom: 0;
}
.status-change-info strong {
  color: #1a1a1a;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
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
.form-group input,
.form-select {
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
.form-group input:focus,
.form-select:focus {
  border-color: #C5A265;
}
.form-group input::placeholder {
  color: #ccc;
}
.form-select {
  cursor: pointer;
}

.supplier-select-wrap {
  flex: 1;
}
.supplier-select-wrap .form-select {
  width: 100%;
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

/* ===== Print Overlay ===== */
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
  .status-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
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

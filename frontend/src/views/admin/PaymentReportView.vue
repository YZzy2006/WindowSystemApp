<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>收付款查询</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="doSearch" />
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-bar">
      <div class="search-item">
        <label>起始日期：</label>
        <el-date-picker v-model="startDate" type="date" placeholder="起始日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" clearable style="width:155px" @change="val => { if (!validateDateRange(val, endDate, 'start')) { startDate = null } }" />
      </div>
      <div class="search-item">
        <label>截止日期：</label>
        <el-date-picker v-model="endDate" type="date" placeholder="截止日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" clearable style="width:155px" @change="val => { if (!validateDateRange(startDate, val, 'end')) endDate = null }" />
      </div>
      <div class="search-item">
        <label>查询关键字：</label>
        <el-input v-model="keyword" placeholder="订单编号 / 款方名称" clearable style="width:200px" @keyup.enter="doSearch" />
      </div>
      <div class="search-item">
        <el-checkbox v-model="showSummary">分组汇总</el-checkbox>
      </div>
      <button class="btn-primary" @click="doSearch">查询</button>
      <button class="btn-secondary" @click="doClear">清空</button>
    </div>

    <div v-if="loading" v-loading="true" style="height:200px"></div>

    <div v-if="!loading && reportReady" class="report-columns">
      <!-- ===== Left: 收支明细 ===== -->
      <div class="report-col">
        <div class="col-title-bar">
          <span class="col-title">收支明细</span>
          <button class="btn-export" @click="exportPayments" v-if="paymentReport.payments.length">导出 Excel</button>
        </div>
        <div class="balance-row">
          <div class="balance-card">
            <div class="balance-label">起始日期结存</div>
            <div class="balance-value">{{ formatMoney(paymentReport.summary.openingBalance) }}</div>
          </div>
          <div class="balance-card green">
            <div class="balance-label">本期收款</div>
            <div class="balance-value">{{ formatMoney(paymentReport.summary.periodReceipt) }}</div>
          </div>
          <div class="balance-card red">
            <div class="balance-label">本期付款</div>
            <div class="balance-value">{{ formatMoney(paymentReport.summary.periodPayment) }}</div>
          </div>
          <div class="balance-card">
            <div class="balance-label">截止日期结存</div>
            <div class="balance-value">{{ formatMoney(paymentReport.summary.closingBalance) }}</div>
          </div>
        </div>
        <div class="report-table-wrap">
          <table class="rpt-table">
            <colgroup>
              <col style="width:90px">
              <col style="width:90px">
              <col style="width:100px">
              <col style="width:110px">
              <col>
              <col>
            </colgroup>
            <thead>
              <tr>
                <th>日期</th>
                <th>收/付款类型</th>
                <th>金额</th>
                <th>订单编号</th>
                <th>款方名称</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in paymentReport.payments" :key="p.id">
                <td>{{ formatDate(p.paymentDate) }}</td>
                <td>
                  <span v-if="p.type === 'receipt'" class="tag tag-green">收款</span>
                  <span v-else class="tag tag-red">付款</span>
                </td>
                <td class="num" :class="p.type === 'receipt' ? 'green' : 'red'">
                  {{ p.type === 'receipt' ? '+' : '-' }}{{ formatMoney(p.amount) }}
                </td>
                <td>{{ p.orderNo || '-' }}</td>
                <td>{{ p.partyName || '-' }}</td>
                <td>{{ p.remark || '-' }}</td>
              </tr>
              <tr v-if="paymentReport.payments.length === 0">
                <td colspan="6" class="empty-row">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ===== Right: 汇总明细（销售/采购 tab） ===== -->
      <div class="report-col">
        <div class="col-title-bar">
          <button class="tab-btn" :class="{ active: activeReportTab === 'sale' }" @click="activeReportTab = 'sale'">销售汇总</button>
          <button class="tab-btn" :class="{ active: activeReportTab === 'purchase' }" @click="activeReportTab = 'purchase'">采购汇总</button>
          <button class="btn-export" @click="exportSalesReport" v-if="activeReportTab === 'sale' && flatOrders.length">导出 Excel</button>
          <button class="btn-export" @click="exportPurchaseReport" v-if="activeReportTab === 'purchase' && flatPurchaseOrders.length">导出 Excel</button>
        </div>

        <!-- ===== 销售汇总 ===== -->
        <template v-if="activeReportTab === 'sale'">
          <div class="sales-summary-bar">
            <div class="sales-summary-card">
              <div class="ss-label">订单总额</div>
              <div class="ss-value">{{ formatMoney(salesTotal.totalAmount) }}</div>
            </div>
            <div class="sales-summary-card blue">
              <div class="ss-label">已付款</div>
              <div class="ss-value">{{ formatMoney(salesTotal.paidAmount) }}</div>
            </div>
            <div class="sales-summary-card red">
              <div class="ss-label">未付款</div>
              <div class="ss-value">{{ formatMoney(salesTotal.unpaidAmount) }}</div>
            </div>
            <div class="sales-summary-card purple">
              <div class="ss-label">总成本</div>
              <div class="ss-value">{{ formatMoney(salesTotal.cost) }}</div>
            </div>
            <div class="sales-summary-card green">
              <div class="ss-label">毛利</div>
              <div class="ss-value">{{ formatMoney(salesTotal.profit) }}</div>
            </div>
            <div class="sales-summary-card gold">
              <div class="ss-label">毛利率</div>
              <div class="ss-value">{{ formatRate(salesTotal.profitRate) }}</div>
            </div>
          </div>

          <!-- 客户分组模式 -->
          <template v-if="showSummary">
            <div v-for="c in salesReport.customers" :key="c.customerName" class="customer-block">
              <div class="customer-header">
                <div class="customer-name">{{ c.customerName }}</div>
                <div class="customer-stats">
                  <div class="stat-item">
                    <span class="stat-label">订单金额</span>
                    <span class="stat-value">{{ formatMoney(c.totalAmount) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">已付款</span>
                    <span class="stat-value">{{ formatMoney(c.paidAmount) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">未付款</span>
                    <span class="stat-value" :class="{ red: c.unpaidAmount > 0 }">{{ formatMoney(c.unpaidAmount) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">毛利</span>
                    <span class="stat-value">{{ formatMoney(c.profit) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">毛利率</span>
                    <span class="stat-value">{{ formatRate(c.profitRate) }}</span>
                  </div>
                </div>
              </div>
              <div class="order-table-wrap">
                <table class="rpt-table order-table">
                  <thead>
                    <tr>
                      <th>日期</th>
                      <th>订单编号</th>
                      <th>订单金额</th>
                      <th>已付款</th>
                      <th>未付款</th>
                      <th>成本</th>
                      <th>毛利</th>
                      <th>毛利率</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="o in c.orders" :key="o.orderId">
                      <td>{{ formatDate(o.orderDate) }}</td>
                      <td>{{ o.orderNo }}</td>
                      <td class="num">{{ formatMoney(o.totalAmount) }}</td>
                      <td class="num">{{ formatMoney(o.paidAmount) }}</td>
                      <td class="num" :class="{ red: o.unpaidAmount > 0 }">{{ formatMoney(o.unpaidAmount) }}</td>
                      <td class="num">{{ formatMoney(o.cost) }}</td>
                      <td class="num">{{ formatMoney(o.profit) }}</td>
                      <td class="num">{{ formatRate(o.profitRate) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-if="salesReport.customers.length === 0" class="empty-hint">暂无数据</div>
          </template>

          <!-- 平铺模式 -->
          <template v-else>
            <div class="report-table-wrap">
              <table class="rpt-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>订单编号</th>
                    <th>客户</th>
                    <th>订单金额</th>
                    <th>已付款</th>
                    <th>未付款</th>
                    <th>成本</th>
                    <th>毛利</th>
                    <th>毛利率</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="o in flatOrders" :key="o.orderId">
                    <td>{{ formatDate(o.orderDate) }}</td>
                    <td>{{ o.orderNo }}</td>
                    <td>{{ o.customerName }}</td>
                    <td class="num">{{ formatMoney(o.totalAmount) }}</td>
                    <td class="num">{{ formatMoney(o.paidAmount) }}</td>
                    <td class="num" :class="{ red: o.unpaidAmount > 0 }">{{ formatMoney(o.unpaidAmount) }}</td>
                    <td class="num">{{ formatMoney(o.cost) }}</td>
                    <td class="num">{{ formatMoney(o.profit) }}</td>
                    <td class="num">{{ formatRate(o.profitRate) }}</td>
                  </tr>
                  <tr v-if="flatOrders.length === 0">
                    <td colspan="9" class="empty-row">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </template>

        <!-- ===== 采购汇总 ===== -->
        <template v-if="activeReportTab === 'purchase'">
          <div class="sales-summary-bar">
            <div class="sales-summary-card">
              <div class="ss-label">采购总额</div>
              <div class="ss-value">{{ formatMoney(purchaseTotal.totalAmount) }}</div>
            </div>
            <div class="sales-summary-card blue">
              <div class="ss-label">已付款</div>
              <div class="ss-value">{{ formatMoney(purchaseTotal.paidAmount) }}</div>
            </div>
            <div class="sales-summary-card red">
              <div class="ss-label">未付款</div>
              <div class="ss-value">{{ formatMoney(purchaseTotal.unpaidAmount) }}</div>
            </div>
            <div class="sales-summary-card purple">
              <div class="ss-label">订单数</div>
              <div class="ss-value">{{ purchaseReport.suppliers.reduce((n, s) => n + (s.orders || []).length, 0) }}</div>
            </div>
          </div>

          <!-- 供应商分组模式 -->
          <template v-if="showSummary">
            <div v-for="s in purchaseReport.suppliers" :key="s.supplierName" class="customer-block">
              <div class="customer-header">
                <div class="customer-name">{{ s.supplierName }}</div>
                <div class="customer-stats">
                  <div class="stat-item">
                    <span class="stat-label">采购金额</span>
                    <span class="stat-value">{{ formatMoney(s.totalAmount) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">已付款</span>
                    <span class="stat-value">{{ formatMoney(s.paidAmount) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">未付款</span>
                    <span class="stat-value" :class="{ red: s.unpaidAmount > 0 }">{{ formatMoney(s.unpaidAmount) }}</span>
                  </div>
                </div>
              </div>
              <div class="order-table-wrap">
                <table class="rpt-table order-table">
                  <thead>
                    <tr>
                      <th>日期</th>
                      <th>订单编号</th>
                      <th>采购金额</th>
                      <th>已付款</th>
                      <th>未付款</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="o in s.orders" :key="o.orderId">
                      <td>{{ formatDate(o.orderDate) }}</td>
                      <td>{{ o.orderNo }}</td>
                      <td class="num">{{ formatMoney(o.totalAmount) }}</td>
                      <td class="num">{{ formatMoney(o.paidAmount) }}</td>
                      <td class="num" :class="{ red: o.unpaidAmount > 0 }">{{ formatMoney(o.unpaidAmount) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-if="purchaseReport.suppliers.length === 0" class="empty-hint">暂无数据</div>
          </template>

          <!-- 平铺模式 -->
          <template v-else>
            <div class="report-table-wrap">
              <table class="rpt-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>订单编号</th>
                    <th>供应商</th>
                    <th>采购金额</th>
                    <th>已付款</th>
                    <th>未付款</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="o in flatPurchaseOrders" :key="o.orderId">
                    <td>{{ formatDate(o.orderDate) }}</td>
                    <td>{{ o.orderNo }}</td>
                    <td>{{ o.supplierName }}</td>
                    <td class="num">{{ formatMoney(o.totalAmount) }}</td>
                    <td class="num">{{ formatMoney(o.paidAmount) }}</td>
                    <td class="num" :class="{ red: o.unpaidAmount > 0 }">{{ formatMoney(o.unpaidAmount) }}</td>
                  </tr>
                  <tr v-if="flatPurchaseOrders.length === 0">
                    <td colspan="6" class="empty-row">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </template>
      </div>
    </div>

    <div v-if="!loading && !reportReady" class="empty-hint">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPaymentReport, getSaleOrderReport, getPurchaseOrderReport } from '@/api/admin'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { exportToExcel, fmtDate, fmtMoney } from '@/utils/exportExcel'
import RefreshButton from '@/components/RefreshButton.vue'

const route = useRoute()
const router = useRouter()

const now = new Date()
const pad = (n) => String(n).padStart(2, '0')
const startDate = ref(`${now.getFullYear()}-${pad(now.getMonth() + 1)}-01`)
const endDate = ref('')
const keyword = ref('')
const loading = ref(false)
const paymentReport = ref({ summary: { openingBalance: 0, periodReceipt: 0, periodPayment: 0, closingBalance: 0 }, payments: [] })
const salesReport = ref({ summary: {}, customers: [] })
const purchaseReport = ref({ summary: {}, suppliers: [] })
const activeReportTab = ref('sale')
const showSummary = ref(true)

const REPORT_FILTER_KEY = 'payment_report_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.keyword || q.startDate || q.endDate
  if (hasUrlParams) {
    _restoring = true
    if (q.startDate) startDate.value = q.startDate
    if (q.endDate) endDate.value = q.endDate
    if (q.keyword) keyword.value = q.keyword
    if (q.showSummary !== undefined) showSummary.value = q.showSummary === 'true'
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(REPORT_FILTER_KEY) || '{}')
    _restoring = true
    if (saved.startDate) startDate.value = saved.startDate
    if (saved.endDate) endDate.value = saved.endDate
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.showSummary !== undefined) showSummary.value = saved.showSummary
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (startDate.value) { query.startDate = startDate.value; storage.startDate = startDate.value }
  if (endDate.value) { query.endDate = endDate.value; storage.endDate = endDate.value }
  if (keyword.value) { query.keyword = keyword.value; storage.keyword = keyword.value }
  if (!showSummary.value) { query.showSummary = 'false'; storage.showSummary = false }
  router.replace({ query })
  sessionStorage.setItem(REPORT_FILTER_KEY, JSON.stringify(storage))
}

const hasOrders = computed(() => salesReport.value.customers.some(c => (c.orders || []).length > 0))
const hasPurchaseOrders = computed(() => purchaseReport.value.suppliers.some(s => (s.orders || []).length > 0))
const reportReady = computed(() => paymentReport.value.payments.length > 0 || hasOrders.value || hasPurchaseOrders.value)
const flatOrders = computed(() => {
  const all = []
  for (const c of salesReport.value.customers) {
    for (const o of (c.orders || [])) {
      all.push({ ...o, customerName: c.customerName })
    }
  }
  return all.sort((a, b) => new Date(a.orderDate) - new Date(b.orderDate))
})
const salesTotal = computed(() => {
  let totalAmount = 0, paidAmount = 0, unpaidAmount = 0, cost = 0, profit = 0
  for (const c of salesReport.value.customers) {
    totalAmount += c.totalAmount || 0
    paidAmount += c.paidAmount || 0
    unpaidAmount += c.unpaidAmount || 0
    cost += c.cost || 0
    profit += c.profit || 0
  }
  const profitRate = totalAmount > 0 ? profit / totalAmount : 0
  return { totalAmount, paidAmount, unpaidAmount, cost, profit, profitRate }
})
const purchaseTotal = computed(() => {
  let totalAmount = 0, paidAmount = 0, unpaidAmount = 0
  for (const s of purchaseReport.value.suppliers) {
    totalAmount += s.totalAmount || 0
    paidAmount += s.paidAmount || 0
    unpaidAmount += s.unpaidAmount || 0
  }
  return { totalAmount, paidAmount, unpaidAmount }
})
const flatPurchaseOrders = computed(() => {
  const all = []
  for (const s of purchaseReport.value.suppliers) {
    for (const o of (s.orders || [])) {
      all.push({ ...o, supplierName: s.supplierName })
    }
  }
  return all.sort((a, b) => new Date(a.orderDate) - new Date(b.orderDate))
})

function formatDate(val) {
  if (!val) return '-'
  // 直接截取前10位避免时区偏移
  if (typeof val === 'string' && val.length >= 10) return val.substring(0, 10)
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatMoney(val) {
  if (val == null || isNaN(val)) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRate(val) {
  if (val == null || isNaN(val) || !isFinite(val)) return '0.00%'
  return (Number(val) * 100).toFixed(2) + '%'
}

onMounted(() => { restoreFilters(); doSearch() })

watch([startDate, endDate], () => { if (!_restoring) { doSearch(); syncFiltersToUrl() } })

let _keywordTimer = null
watch(keyword, () => {
  if (_restoring) return
  clearTimeout(_keywordTimer)
  _keywordTimer = setTimeout(() => { doSearch(); syncFiltersToUrl() }, 300)
})
watch(showSummary, () => { if (!_restoring) syncFiltersToUrl() })

function doClear() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  _restoring = true
  startDate.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-01`
  endDate.value = ''
  keyword.value = ''
  _restoring = false
  doSearch()
  syncFiltersToUrl()
}

async function doSearch() {
  loading.value = true
  try {
    const params = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const [payResult, saleResult, purchaseResult] = await Promise.allSettled([
      getPaymentReport(params),
      getSaleOrderReport(params),
      getPurchaseOrderReport(params)
    ])

    if (payResult.status === 'fulfilled') {
      const payData = payResult.value.data?.data || payResult.value.data || {}
      paymentReport.value = {
        summary: payData.summary || { openingBalance: 0, periodReceipt: 0, periodPayment: 0, closingBalance: 0 },
        payments: payData.payments || []
      }
      if (payData.truncated) {
        ElMessage.warning('收支明细数据过多，仅显示前 1000 条，请缩小查询范围')
      }
    }
    if (saleResult.status === 'fulfilled') {
      const saleData = saleResult.value.data?.data || saleResult.value.data || {}
      salesReport.value = {
        summary: saleData.summary || {},
        customers: saleData.customers || []
      }
      if (saleData.truncated) {
        ElMessage.warning('销售汇总数据过多，仅显示前 1000 条，请缩小查询范围')
      }
    }
    if (purchaseResult.status === 'fulfilled') {
      const purchaseData = purchaseResult.value.data?.data || purchaseResult.value.data || {}
      purchaseReport.value = {
        summary: purchaseData.summary || {},
        suppliers: purchaseData.suppliers || []
      }
      if (purchaseData.truncated) {
        ElMessage.warning('采购汇总数据过多，仅显示前 1000 条，请缩小查询范围')
      }
    }
    const rejected = [payResult, saleResult, purchaseResult].filter(r => r.status === 'rejected')
    if (rejected.length === 3) {
      ElMessage.error('加载报表失败')
    } else if (rejected.length > 0) {
      ElMessage.warning('部分数据加载失败')
    }
  } catch (err) {
    console.error('加载报表失败:', err)
    if (!err._handled) ElMessage.error('加载报表失败')
  } finally {
    loading.value = false
    syncFiltersToUrl()
  }
}

function typeLabel(type) {
  return type === 'receipt' ? '收款' : '付款'
}

async function exportPayments() {
  if (!paymentReport.value.payments.length) return
  await exportToExcel(paymentReport.value.payments, [
    { key: 'paymentDate', label: '日期', width: 12, format: fmtDate },
    { key: 'type', label: '收/付类型', width: 10, format: (v) => typeLabel(v) },
    { key: 'amount', label: '金额', width: 12, format: fmtMoney },
    { key: 'orderNo', label: '订单编号', width: 18 },
    { key: 'partyName', label: '款方名称', width: 16 },
    { key: 'remark', label: '备注', width: 20 }
  ], '收支明细', { sheetName: '收支明细' })
}

async function exportSalesReport() {
  const data = flatOrders.value
  if (!data.length) return
  await exportToExcel(data, [
    { key: 'orderDate', label: '日期', width: 12, format: fmtDate },
    { key: 'orderNo', label: '订单编号', width: 18 },
    { key: 'customerName', label: '客户', width: 14 },
    { key: 'totalAmount', label: '订单金额', width: 12, format: fmtMoney },
    { key: 'paidAmount', label: '已付款', width: 12, format: fmtMoney },
    { key: 'unpaidAmount', label: '未付款', width: 12, format: fmtMoney },
    { key: 'cost', label: '成本', width: 12, format: fmtMoney },
    { key: 'profit', label: '毛利', width: 12, format: fmtMoney },
    { key: 'profitRate', label: '毛利率', width: 10, format: (v) => v != null ? (Number(v) * 100).toFixed(2) + '%' : '' }
  ], '销售汇总', { sheetName: '销售汇总' })
}

async function exportPurchaseReport() {
  const data = flatPurchaseOrders.value
  if (!data.length) return
  await exportToExcel(data, [
    { key: 'orderDate', label: '日期', width: 12, format: fmtDate },
    { key: 'orderNo', label: '订单编号', width: 18 },
    { key: 'supplierName', label: '供应商', width: 14 },
    { key: 'totalAmount', label: '采购金额', width: 12, format: fmtMoney },
    { key: 'paidAmount', label: '已付款', width: 12, format: fmtMoney },
    { key: 'unpaidAmount', label: '未付款', width: 12, format: fmtMoney }
  ], '采购汇总', { sheetName: '采购汇总' })
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }

/* ===== Search Bar ===== */
.search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-item label {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
}
.btn-primary {
  padding: 8px 28px;
  background: #C5A265;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover { background: #d4b885; }
.btn-secondary {
  padding: 8px 28px;
  background: #f5f5f5;
  color: #555;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover { background: #eee; }

/* ===== Two Columns ===== */
.report-columns {
  display: flex;
  gap: 16px;
}
.report-col {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}
.col-title {
  padding: 12px 20px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}
.col-title-bar {
  display: flex;
  align-items: center;
  background: #C5A265;
}
.btn-export {
  margin-left: auto;
  padding: 6px 14px;
  background: rgba(255,255,255,.15);
  border: 1px solid rgba(255,255,255,.3);
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s;
  white-space: nowrap;
  margin-right: 12px;
}
.btn-export:hover { background: rgba(255,255,255,.25); }
.tab-btn {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  border: none;
  color: rgba(255,255,255,.55);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s;
  border-bottom: 3px solid transparent;
}
.tab-btn:hover { color: rgba(255,255,255,.85); }
.tab-btn.active { color: #fff; border-bottom-color: #fff; }

/* ===== Balance Row ===== */
.balance-row {
  display: flex;
  gap: 0;
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
}
.balance-card {
  flex: 1;
  padding: 14px 10px;
  text-align: center;
  border-right: 1px solid rgba(255,255,255,0.08);
}
.balance-card:last-child { border-right: none; }
.balance-card.green .balance-value { color: #69db7c; }
.balance-card.red .balance-value { color: #ff6b6b; }
.balance-label { font-size: 11px; color: rgba(255,255,255,0.50); margin-bottom: 4px; letter-spacing: 0.5px; }
.balance-value { font-size: 16px; font-weight: 800; color: #fff; font-variant-numeric: tabular-nums; }

/* ===== Sales Summary Bar ===== */
.sales-summary-bar {
  display: flex;
  gap: 0;
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
}
.sales-summary-card {
  flex: 1;
  padding: 10px 8px;
  text-align: center;
  border-right: 1px solid rgba(255,255,255,0.08);
}
.sales-summary-card:last-child { border-right: none; }
.ss-label {
  font-size: 10px;
  color: rgba(255,255,255,0.55);
  margin-bottom: 3px;
  letter-spacing: 0.5px;
}
.ss-value {
  font-size: 15px;
  font-weight: 800;
  color: #fff;
  font-variant-numeric: tabular-nums;
}
.sales-summary-card.blue .ss-value { color: #5b9bd5; }
.sales-summary-card.red .ss-value { color: #ff6b6b; }
.sales-summary-card.purple .ss-value { color: #b388ff; }
.sales-summary-card.green .ss-value { color: #69db7c; }
.sales-summary-card.gold .ss-value { color: #ffd54f; }

/* ===== Table ===== */
.report-table-wrap, .order-table-wrap {
  padding: 0;
  overflow-x: auto;
}
.rpt-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.rpt-table thead th {
  background: #f5f5f5;
  color: #555;
  font-weight: 600;
  padding: 8px 10px;
  border-bottom: 1px solid #eee;
  text-align: left;
  white-space: nowrap;
}
.rpt-table tbody td {
  padding: 7px 10px;
  border-bottom: 1px solid #f5f5f5;
  color: #333;
}
.rpt-table tbody tr:hover { background: #fafafa; }
.rpt-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.rpt-table .bold { font-weight: 600; }
.rpt-table .red { color: #e74c3c; }
.rpt-table .green { color: #52c41a; }
.rpt-table .empty-row { text-align: center; color: #ccc; padding: 30px 0; }

/* ===== Tags ===== */
.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.tag-green { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.tag-red { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }

/* ===== Customer Block ===== */
.customer-block {
  margin: 14px;
  border: 1px solid #d4c9b0;
  border-left: 5px solid #C5A265;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.customer-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f5f0e4 0%, #ece4d2 100%);
  border-bottom: 2px solid #C5A265;
}
.customer-name {
  font-size: 17px;
  font-weight: 700;
  color: #8b6914;
  white-space: nowrap;
  min-width: 80px;
  padding-right: 16px;
  border-right: 2px solid #d4c9b0;
}
.customer-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  flex: 1;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 70px;
}
.stat-label {
  font-size: 11px;
  color: #888;
  margin-bottom: 3px;
}
.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  font-variant-numeric: tabular-nums;
}
.stat-value.red { color: #e74c3c; }
.customer-block .order-table-wrap {
  padding: 0;
}
.customer-block:nth-child(even) .customer-header {
  background: linear-gradient(135deg, #f0ede6 0%, #e8e2d6 100%);
}

/* ===== Empty ===== */
.empty-hint { text-align: center; padding: 60px 20px; color: #ccc; font-size: 14px; }

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .report-columns { flex-direction: column; }
}
</style>

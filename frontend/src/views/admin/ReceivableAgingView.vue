<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>应收账款</h2>
      <p class="page-desc">账龄分析与客户欠款查询</p>
      <div class="header-right">
        <RefreshButton :on-refresh="loadAgingData" />
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <span class="tab-item" :class="{ active: activeTab === 'aging' }" @click="activeTab = 'aging'">账龄分析</span>
      <span class="tab-item" :class="{ active: activeTab === 'customer' }" @click="activeTab = 'customer'">客户查询</span>
    </div>

    <!-- ====== 账龄分析 Tab ====== -->
    <div v-show="activeTab === 'aging'">
      <div class="filter-bar">
        <div class="preset-group">
          <button v-for="p in presets" :key="p.label" class="preset-btn" :class="{ active: activePreset === p.label }" @click="applyPreset(p)">{{ p.label }}</button>
        </div>
        <el-date-picker v-model="startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onDateChange" />
        <span style="margin: 0 4px; color: #999;">至</span>
        <el-date-picker v-model="endDate" type="date" placeholder="截止日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onDateChange" />
      </div>

      <div v-loading="loading">
        <div class="aging-grid">
          <div class="aging-card" v-for="item in agingList" :key="item.bucket" :class="bucketClass(item.bucket)">
            <div class="aging-label">{{ item.bucket }}</div>
            <div class="aging-amount">¥{{ formatMoney(item.amount) }}</div>
            <div class="aging-count">{{ item.count }} 笔未结清</div>
          </div>
        </div>

        <div class="total-bar" v-if="agingList.length">
          <div class="total-item">
            <span class="total-label">应收总额</span>
            <span class="total-value">¥{{ formatMoney(totalAmount) }}</span>
          </div>
          <div class="total-item">
            <span class="total-label">未结清笔数</span>
            <span class="total-value">{{ totalCount }} 笔</span>
          </div>
        </div>

        <div class="chart-section">
          <div class="section-title">账龄分布</div>
          <div ref="chartRef" class="chart-box"></div>
        </div>
      </div>

      <!-- 未结清明细 -->
      <div class="detail-section">
        <div class="section-title">未结清明细 <span class="detail-hint">按账龄天数排序</span>
          <el-dropdown trigger="click" @command="handleExportCommand" v-if="details.length" style="float:right">
            <button class="btn-export">导出 Excel ▾</button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="page">导出当前页</el-dropdown-item>
                <el-dropdown-item command="all">导出全部</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="card" v-loading="detailsLoading">
          <table class="detail-table" v-if="details.length">
            <thead>
              <tr>
                <th>客户</th>
                <th>订单号</th>
                <th>订单日期</th>
                <th class="num">账龄</th>
                <th class="num">总金额</th>
                <th class="num">已付</th>
                <th class="num">未付</th>
                <th>账龄区间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in details" :key="item.id">
                <td class="name-cell">{{ item.customerName }}</td>
                <td><span class="mono order-link" @click="goToOrder(item.id)">{{ item.orderNo }}</span></td>
                <td>{{ formatDate(item.orderDate) }}</td>
                <td class="num"><span class="aging-days" :class="daysClass(item.agingDays)">{{ item.agingDays }}天</span></td>
                <td class="num">¥{{ formatMoney(item.totalAmount) }}</td>
                <td class="num">¥{{ formatMoney(item.paidAmount) }}</td>
                <td class="num money-red">¥{{ formatMoney(item.unpaidAmount) }}</td>
                <td><span class="bucket-tag" :class="bucketClass(item.bucket)">{{ item.bucket }}</span></td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-hint">{{ detailsLoading ? '' : '暂无未结清账款' }}</div>
          <div class="pagination-bar" v-if="detailsTotal > 0">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[20, 50, 100]"
              :total="detailsTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadDetails"
              @size-change="onPageSizeChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- ====== 客户查询 Tab ====== -->
    <div v-show="activeTab === 'customer'">
      <div class="search-bar">
        <div class="search-item">
          <label>客户名称 / 电话：</label>
          <el-input v-model="searchKeyword" placeholder="输入客户名称或电话" clearable style="width: 240px" @keyup.enter="searchCustomer" @clear="clearSearch" />
        </div>
        <button class="btn-primary" @click="searchCustomer">查询</button>
        <button class="btn-secondary" @click="clearSearch">清空</button>
        <el-dropdown trigger="click" @command="handleCustomerExportCommand" v-if="customerData && customerData.customers.length">
          <button class="btn-secondary">导出 Excel ▾</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="page-simple">当前查询 - 简易</el-dropdown-item>
              <el-dropdown-item command="page-detail">当前查询 - 详细</el-dropdown-item>
              <el-dropdown-item divided command="all-simple">全部 - 简易</el-dropdown-item>
              <el-dropdown-item command="all-detail">全部 - 详细</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="filter-bar" style="margin-bottom: 16px;">
        <div class="preset-group">
          <button v-for="p in customerPresets" :key="p.label" class="preset-btn" :class="{ active: customerPreset === p.label }" @click="applyCustomerPreset(p)">{{ p.label }}</button>
        </div>
        <el-date-picker v-model="customerStartDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onCustomerDateChange" />
        <span style="margin: 0 4px; color: #999;">至</span>
        <el-date-picker v-model="customerEndDate" type="date" placeholder="截止日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onCustomerDateChange" />
      </div>

      <div v-if="customerLoading" v-loading="true" style="height: 200px"></div>

      <template v-if="!customerLoading && customerData">
        <!-- 总汇总 -->
        <div class="summary-bar">
          <div class="summary-card">
            <div class="sc-label">匹配客户</div>
            <div class="sc-value">{{ customerData.total.customerCount }} 个</div>
          </div>
          <div class="summary-card">
            <div class="sc-label">订单总数</div>
            <div class="sc-value">{{ customerData.total.orderCount }} 笔</div>
          </div>
          <div class="summary-card">
            <div class="sc-label">订单总额</div>
            <div class="sc-value">¥{{ formatMoney(customerData.total.totalAmount) }}</div>
          </div>
          <div class="summary-card green">
            <div class="sc-label">已付总额</div>
            <div class="sc-value">¥{{ formatMoney(customerData.total.paidAmount) }}</div>
          </div>
          <div class="summary-card red">
            <div class="sc-label">未付总额</div>
            <div class="sc-value">¥{{ formatMoney(customerData.total.unpaidAmount) }}</div>
          </div>
          <div class="summary-card orange">
            <div class="sc-label">未结清笔数</div>
            <div class="sc-value">{{ customerData.total.unpaidCount }} 笔</div>
          </div>
        </div>

        <!-- 按客户分组 -->
        <div v-for="c in customerData.customers" :key="c.customerName" class="customer-block">
          <div class="customer-header">
            <div class="customer-name">{{ c.customerName }}<span class="customer-phone" v-if="c.customerPhone">（{{ c.customerPhone }}）</span></div>
            <div class="customer-stats">
              <div class="stat-item">
                <span class="stat-label">订单</span>
                <span class="stat-value">{{ c.orderCount }} 笔</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">总额</span>
                <span class="stat-value">¥{{ formatMoney(c.totalAmount) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已付</span>
                <span class="stat-value">¥{{ formatMoney(c.paidAmount) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">未付</span>
                <span class="stat-value" :class="{ 'text-red': c.unpaidAmount > 0 }">¥{{ formatMoney(c.unpaidAmount) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">未结清</span>
                <span class="stat-value" :class="{ 'text-red': c.unpaidCount > 0 }">{{ c.unpaidCount }} 笔</span>
              </div>
            </div>
          </div>
          <div class="order-table-wrap">
            <table class="order-table">
              <thead>
                <tr>
                  <th>订单编号</th>
                  <th>订单日期</th>
                  <th class="num">订单金额</th>
                  <th class="num">已付</th>
                  <th class="num">未付</th>
                  <th class="num">账龄</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in c.orders" :key="o.id">
                  <td><span class="mono order-link" @click="goToOrder(o.id)">{{ o.orderNo }}</span></td>
                  <td>{{ formatDate(o.orderDate) }}</td>
                  <td class="num">{{ formatMoney(o.totalAmount) }}</td>
                  <td class="num">{{ formatMoney(o.paidAmount) }}</td>
                  <td class="num" :class="{ 'text-red': o.unpaidAmount > 0 }">{{ formatMoney(o.unpaidAmount) }}</td>
                  <td class="num"><span class="aging-days" :class="daysClass(o.agingDays)">{{ o.agingDays }}天</span></td>
                  <td><span class="bucket-tag" :class="o.bucket === '已结清' ? 'bucket-clear' : bucketClass(o.bucket)">{{ o.bucket }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="customerData.customers.length === 0" class="empty-hint">未找到相关客户</div>
      </template>

      <div v-if="!customerLoading && !customerData" class="empty-hint">请输入客户名称或电话查询</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReceivableAging, getReceivableAgingDetails, getCustomerOrders } from '@/api/admin'
import { exportToExcel, fmtDate, fmtMoney } from '@/utils/exportExcel'

const router = useRouter()
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import RefreshButton from '@/components/RefreshButton.vue'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const activeTab = ref('aging')

// ===== 账龄分析 =====
const loading = ref(false)
const detailsLoading = ref(false)
const agingList = ref([])
const details = ref([])
const chartRef = ref(null)
let chart = null
const startDate = ref(null)
const endDate = ref(null)
const activePreset = ref('全部')
const currentPage = ref(1)
const pageSize = ref(20)
const detailsTotal = ref(0)

const presets = [
  { label: '全部', start: null, end: null },
  { label: '本月', start: () => { const d = new Date(); d.setDate(1); return fmt(d) }, end: () => fmt(new Date()) },
  { label: '本季度', start: () => { const d = new Date(); const q = Math.floor(d.getMonth() / 3); d.setMonth(q * 3, 1); return fmt(d) }, end: () => fmt(new Date()) },
  { label: '本年', start: () => { const d = new Date(); d.setMonth(0, 1); return fmt(d) }, end: () => fmt(new Date()) }
]

// ===== 客户查询 =====
const searchKeyword = ref('')
const customerLoading = ref(false)
const customerData = ref(null)
const customerStartDate = ref(null)
const customerEndDate = ref(null)
const customerPreset = ref('本季度')

const customerPresets = [
  { label: '本月', start: () => { const d = new Date(); d.setDate(1); return fmt(d) }, end: () => fmt(new Date()) },
  { label: '本季度', start: () => { const d = new Date(); const q = Math.floor(d.getMonth() / 3); d.setMonth(q * 3, 1); return fmt(d) }, end: () => fmt(new Date()) },
  { label: '本年', start: () => { const d = new Date(); d.setMonth(0, 1); return fmt(d) }, end: () => fmt(new Date()) }
]

function fmt(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const totalAmount = computed(() => agingList.value.reduce((s, i) => s + (Number(i.amount) || 0), 0))
const totalCount = computed(() => agingList.value.reduce((s, i) => s + (Number(i.count) || 0), 0))

function formatMoney(v) {
  if (v == null || isNaN(v)) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val) {
  if (!val) return '-'
  if (typeof val === 'string' && val.length >= 10) return val.substring(0, 10)
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function bucketClass(bucket) {
  if (bucket.includes('90')) return 'aging-danger'
  if (bucket.includes('60')) return 'aging-warning'
  if (bucket.includes('30-')) return 'aging-caution'
  return 'aging-safe'
}

function daysClass(days) {
  if (days > 90) return 'days-danger'
  if (days > 60) return 'days-warning'
  if (days > 30) return 'days-caution'
  return 'days-safe'
}

function applyPreset(p) {
  activePreset.value = p.label
  startDate.value = p.start ? p.start() : null
  endDate.value = p.end ? p.end() : null
  currentPage.value = 1
  loadAgingData()
}

function onDateChange() {
  activePreset.value = ''
  currentPage.value = 1
  loadAgingData()
}

function onPageSizeChange() {
  currentPage.value = 1
  loadDetails()
}

async function loadDetails() {
  detailsLoading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getReceivableAgingDetails(params)
    const data = res.data?.data || res.data || {}
    details.value = data.list || []
    detailsTotal.value = data.total || 0
  } catch (err) {
    console.error('加载未结清明细失败:', err)
  } finally {
    detailsLoading.value = false
  }
}

function renderChart(data) {
  if (!chartRef.value || !data.length) return
  if (!chart) chart = echarts.init(chartRef.value)
  const colors = ['#52c41a', '#faad14', '#fa8c16', '#ff4d4f']
  const buckets = data.map(d => d.bucket)
  const amounts = data.map(d => Number(d.amount) || 0)
  const counts = data.map(d => Number(d.count) || 0)

  chart.setOption({
    tooltip: {
      formatter: p => {
        const idx = p.dataIndex
        return `${p.name}<br/>金额: ¥${Number(amounts[idx]).toLocaleString('zh-CN')}<br/>笔数: ${counts[idx]} 笔`
      }
    },
    grid: { left: 60, right: 40, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: buckets },
    yAxis: { type: 'value', axisLabel: { formatter: v => v >= 10000 ? (v / 10000) + '万' : v } },
    series: [{
      type: 'bar',
      data: amounts.map((v, i) => ({ value: v, itemStyle: { color: colors[i], borderRadius: [6, 6, 0, 0] } })),
      barWidth: 50,
      label: { show: true, position: 'top', formatter: p => '¥' + Number(p.value).toLocaleString('zh-CN'), fontSize: 11, color: '#666' }
    }]
  })
}

async function loadAgingData() {
  loading.value = true
  try {
    const params = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getReceivableAging(params)
    agingList.value = res.data?.data || res.data || []
    await nextTick()
    if (chart) { chart.dispose(); chart = null }
    renderChart(agingList.value)
  } catch (err) {
    console.error('加载应收账龄失败:', err)
  } finally {
    loading.value = false
  }
  loadDetails()
}

function applyCustomerPreset(p) {
  customerPreset.value = p.label
  customerStartDate.value = p.start()
  customerEndDate.value = p.end()
  doCustomerSearch()
}

function onCustomerDateChange() {
  customerPreset.value = ''
  doCustomerSearch()
}

async function doCustomerSearch() {
  customerLoading.value = true
  try {
    const params = {}
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    if (customerStartDate.value) params.startDate = customerStartDate.value
    if (customerEndDate.value) params.endDate = customerEndDate.value
    const res = await getCustomerOrders(params)
    customerData.value = res.data?.data || res.data || null
    if (customerData.value?.customerTruncated) {
      ElMessage.warning('匹配客户超过10000个，仅显示前10000个，请缩小筛选范围')
    } else if (customerData.value?.orderTruncated) {
      ElMessage.warning('订单数据超过10000条，部分结果已截断')
    }
  } catch (err) {
    console.error('查询客户订单失败:', err)
  } finally {
    customerLoading.value = false
  }
}

function searchCustomer() {
  doCustomerSearch()
}

function clearSearch() {
  searchKeyword.value = ''
  customerData.value = null
}

function goToOrder(orderId) {
  if (!orderId) return
  router.push({ path: '/admin/orders/sale', query: { detailId: orderId } })
}

function handleExportCommand(cmd) {
  if (cmd === 'page') exportAgingDetails()
  else if (cmd === 'all') exportAgingDetailsAll()
}

const agingExportColumns = [
  { key: 'customerName', label: '客户', width: 14 },
  { key: 'orderNo', label: '订单号', width: 18 },
  { key: 'orderDate', label: '订单日期', width: 12, format: fmtDate },
  { key: 'agingDays', label: '账龄（天）', width: 10 },
  { key: 'totalAmount', label: '总金额', width: 12, format: fmtMoney },
  { key: 'paidAmount', label: '已付', width: 12, format: fmtMoney },
  { key: 'unpaidAmount', label: '未付', width: 12, format: fmtMoney },
  { key: 'bucket', label: '账龄区间', width: 10 }
]

async function exportAgingDetails() {
  if (!details.value.length) return
  await exportToExcel(details.value, agingExportColumns, '应收账龄明细', { sheetName: '应收账龄明细' })
  ElMessage.success('导出成功')
}

async function exportAgingDetailsAll() {
  const loadingMsg = ElMessage({ message: '正在导出全部数据...', type: 'info', duration: 0 })
  try {
    const params = { page: 1, size: 10000 }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getReceivableAgingDetails(params)
    const data = res.data?.data || res.data || {}
    const list = data.list || []
    if (!list.length) { ElMessage.warning('暂无数据'); return }
    await exportToExcel(list, agingExportColumns, '应收账龄明细-全部', { sheetName: '应收账龄明细' })
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出全部失败:', e)
    ElMessage.error('导出失败')
  } finally {
    loadingMsg.close()
  }
}

const customerSimpleColumns = [
  { key: 'customerName', label: '客户名称', width: 14 },
  { key: 'customerPhone', label: '电话', width: 14 },
  { key: 'orderCount', label: '订单数', width: 8 },
  { key: 'unpaidCount', label: '未结清数', width: 8 },
  { key: 'totalAmount', label: '订单总额', width: 12, format: fmtMoney },
  { key: 'paidAmount', label: '已付', width: 12, format: fmtMoney },
  { key: 'unpaidAmount', label: '未付', width: 12, format: fmtMoney }
]

const customerDetailColumns = [
  { key: 'customerName', label: '客户名称', width: 14 },
  { key: 'customerPhone', label: '电话', width: 14 },
  { key: 'orderNo', label: '订单编号', width: 18 },
  { key: 'orderDate', label: '订单日期', width: 12, format: fmtDate },
  { key: 'totalAmount', label: '订单金额', width: 12, format: fmtMoney },
  { key: 'paidAmount', label: '已付', width: 12, format: fmtMoney },
  { key: 'unpaidAmount', label: '未付', width: 12, format: fmtMoney },
  { key: 'agingDays', label: '账龄天数', width: 10 },
  { key: 'bucket', label: '账龄区间', width: 10 }
]

function handleCustomerExportCommand(cmd) {
  const [scope, mode] = cmd.split('-')
  if (scope === 'page') doCustomerExport(customerData.value, mode)
  else if (scope === 'all') exportCustomerAll(mode)
}

function doCustomerExport(data, mode) {
  if (!data || !data.customers.length) return
  if (mode === 'simple') {
    const rows = data.customers.map(c => ({
      customerName: c.customerName,
      customerPhone: c.customerPhone || '',
      orderCount: c.orderCount,
      unpaidCount: c.unpaidCount,
      totalAmount: c.totalAmount,
      paidAmount: c.paidAmount,
      unpaidAmount: c.unpaidAmount
    }))
    exportToExcel(rows, customerSimpleColumns, '客户欠款汇总', { sheetName: '客户欠款汇总' })
  } else {
    const allOrders = []
    for (const c of data.customers) {
      for (const o of c.orders) {
        allOrders.push({
          customerName: c.customerName,
          customerPhone: c.customerPhone || '',
          orderNo: o.orderNo,
          orderDate: o.orderDate,
          totalAmount: o.totalAmount,
          paidAmount: o.paidAmount,
          unpaidAmount: o.unpaidAmount,
          agingDays: o.agingDays,
          bucket: o.bucket
        })
      }
    }
    exportToExcel(allOrders, customerDetailColumns, '客户欠款明细', { sheetName: '客户欠款明细' })
  }
  ElMessage.success('导出成功')
}

async function exportCustomerAll(mode) {
  const loadingMsg = ElMessage({ message: '正在导出全部数据...', type: 'info', duration: 0 })
  try {
    const params = {}
    if (customerStartDate.value) params.startDate = customerStartDate.value
    if (customerEndDate.value) params.endDate = customerEndDate.value
    const res = await getCustomerOrders(params)
    const data = res.data?.data || res.data || null
    if (!data || !data.customers.length) { ElMessage.warning('暂无数据'); return }
    doCustomerExport(data, mode)
  } catch (e) {
    console.error('导出全部失败:', e)
    ElMessage.error('导出失败')
  } finally {
    loadingMsg.close()
  }
}

let _searchTimer = null
watch(searchKeyword, (val) => {
  clearTimeout(_searchTimer)
  if (!val.trim()) {
    customerData.value = null
    return
  }
  _searchTimer = setTimeout(() => doCustomerSearch(), 400)
})

function handleResize() { chart?.resize() }

onMounted(() => {
  window.addEventListener('resize', handleResize)
  // 客户查询默认本季度
  const now = new Date()
  const q = Math.floor(now.getMonth() / 3)
  const qStart = new Date(now.getFullYear(), q * 3, 1)
  customerStartDate.value = fmt(qStart)
  customerEndDate.value = fmt(now)
  loadAgingData()
  doCustomerSearch()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.page-desc { font-size: 13px; color: #999; margin-top: 4px; }

/* ===== Tab Bar ===== */
.tab-bar {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}
.tab-item {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.tab-item:hover { color: #fa8c16; }
.tab-item.active { color: #fa8c16; border-bottom-color: #fa8c16; }

/* ===== Filter Bar ===== */
.filter-bar {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 16px; flex-wrap: wrap;
}
.preset-group { display: flex; gap: 8px; }
.preset-btn {
  padding: 6px 16px; border: 1px solid #e0e0e0; border-radius: 6px;
  background: #fff; font-size: 13px; color: #666; cursor: pointer; transition: all 0.2s;
}
.preset-btn:hover { border-color: #fa8c16; color: #fa8c16; }
.preset-btn.active { background: #fa8c16; color: #fff; border-color: #fa8c16; }

/* ===== Search Bar ===== */
.search-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item label { font-size: 13px; color: #555; white-space: nowrap; }
.btn-primary {
  padding: 8px 20px; border: none; border-radius: 6px;
  background: #fa8c16; color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: background 0.2s;
}
.btn-primary:hover { background: #d87008; }
.btn-secondary {
  padding: 8px 20px; border: 1px solid #e0e0e0; border-radius: 6px;
  background: #fff; color: #666; font-size: 13px;
  cursor: pointer; transition: all 0.2s;
}
.btn-secondary:hover { border-color: #fa8c16; color: #fa8c16; }
.btn-export {
  padding: 6px 14px; border: 1px solid #fa8c16; border-radius: 4px;
  background: transparent; color: #fa8c16; font-size: 12px;
  cursor: pointer; transition: all 0.2s;
}
.btn-export:hover { background: #fa8c16; color: #fff; }

/* ===== Summary Bar ===== */
.summary-bar {
  display: flex; gap: 16px; flex-wrap: wrap;
  background: #fff; border-radius: 12px;
  padding: 18px 24px; margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.summary-card { text-align: center; min-width: 90px; }
.summary-card.green .sc-value { color: #52c41a; }
.summary-card.red .sc-value { color: #e74c3c; }
.summary-card.orange .sc-value { color: #fa8c16; }
.sc-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.sc-value { font-size: 22px; font-weight: 800; color: #1a1a1a; font-variant-numeric: tabular-nums; }

/* ===== Customer Block ===== */
.customer-block {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  margin-bottom: 16px;
  overflow: hidden;
}
.customer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px 12px 0 0;
}
.customer-name {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}
.customer-phone { font-size: 13px; color: rgba(255,255,255,0.5); font-weight: 400; }
.customer-stats { display: flex; gap: 24px; }
.stat-item { text-align: center; }
.stat-label { display: block; font-size: 11px; color: rgba(255,255,255,0.4); margin-bottom: 2px; }
.stat-value { font-size: 16px; font-weight: 800; color: rgba(255,255,255,0.95); font-variant-numeric: tabular-nums; }
.stat-value.text-red { color: #ff6b6b; }

.order-table-wrap { overflow-x: auto; }
.order-table { width: 100%; border-collapse: collapse; }
.order-table th {
  text-align: left; padding: 12px 16px; font-size: 12px; font-weight: 600;
  color: #888; background: #fafafa; border-bottom: 1px solid #f0f0f0;
}
.order-table td { padding: 11px 16px; font-size: 13px; color: #333; border-bottom: 1px solid #f5f5f5; }
.order-table tbody tr:hover { background: #fafcff; }
.order-table .num { text-align: right; font-variant-numeric: tabular-nums; font-weight: 600; }
.text-red { color: #e74c3c; font-weight: 600; }
.mono { font-family: "Courier New", monospace; letter-spacing: 0.5px; font-size: 13px; font-weight: 600; color: #1a1a1a; }
.order-link { cursor: pointer; color: #1677ff; }
.order-link:hover { text-decoration: underline; color: #0958d9; }

/* ===== Aging Cards ===== */
.aging-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.aging-card {
  background: #fff;
  border-radius: 12px;
  padding: 22px 20px;
  border-left: 4px solid #ccc;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.aging-safe { border-left-color: #52c41a; }
.aging-caution { border-left-color: #faad14; }
.aging-warning { border-left-color: #fa8c16; }
.aging-danger { border-left-color: #ff4d4f; }
.aging-safe .aging-amount { color: #52c41a; }
.aging-caution .aging-amount { color: #faad14; }
.aging-warning .aging-amount { color: #fa8c16; }
.aging-danger .aging-amount { color: #ff4d4f; }

.aging-label { font-size: 13px; color: #888; margin-bottom: 8px; }
.aging-amount { font-size: 24px; font-weight: 800; font-variant-numeric: tabular-nums; margin-bottom: 4px; }
.aging-count { font-size: 12px; color: #bbb; }

/* ===== Total Bar ===== */
.total-bar {
  display: flex;
  gap: 32px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.total-item { display: flex; align-items: center; gap: 8px; }
.total-label { font-size: 13px; color: #888; }
.total-value { font-size: 18px; font-weight: 700; color: #1a1a1a; font-variant-numeric: tabular-nums; }

/* ===== Chart ===== */
.chart-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  margin-bottom: 20px;
}
.section-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin-bottom: 16px; }
.chart-box { width: 100%; height: 280px; }

/* ===== Detail Section ===== */
.detail-section { margin-bottom: 20px; }
.detail-hint { font-size: 12px; color: #bbb; font-weight: 400; margin-left: 8px; }
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}

.detail-table { width: 100%; border-collapse: collapse; }
.detail-table th {
  text-align: left; padding: 12px 16px; font-size: 12px; font-weight: 600;
  color: #888; background: #fafafa; border-bottom: 1px solid #f0f0f0;
}
.detail-table td { padding: 11px 16px; font-size: 13px; color: #333; border-bottom: 1px solid #f5f5f5; }
.detail-table tbody tr:hover { background: #fafcff; }
.detail-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.detail-table .money-red { color: #e74c3c; font-weight: 600; }
.name-cell { font-weight: 500; }

.aging-days {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.days-safe { background: #f6ffed; color: #389e0d; }
.days-caution { background: #fff7e6; color: #d46b08; }
.days-warning { background: #fff1e6; color: #d4380d; }
.days-danger { background: #fff1f0; color: #cf1322; }

.bucket-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.bucket-tag.aging-safe { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.bucket-tag.aging-caution { background: #fff7e6; color: #d46b08; border: 1px solid #ffd591; }
.bucket-tag.aging-warning { background: #fff1e6; color: #d4380d; border: 1px solid #ffbb96; }
.bucket-tag.aging-danger { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }
.bucket-clear { background: #f5f5f5; color: #999; border: 1px solid #d9d9d9; }

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

.empty-hint { text-align: center; padding: 60px 20px; color: #ccc; font-size: 14px; }

@media (max-width: 1200px) {
  .aging-grid { grid-template-columns: repeat(2, 1fr); }
  .customer-header { flex-direction: column; gap: 12px; align-items: flex-start; }
  .customer-stats { flex-wrap: wrap; gap: 16px; }
}
</style>

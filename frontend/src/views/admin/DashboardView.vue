<template>
  <div class="dashboard admin-page">
    <div class="page-header">
      <div>
        <h2>经营看板</h2>
        <p class="page-desc">{{ todayStr }} · 顺居门业管理系统</p>
      </div>
      <div class="header-actions">
        <RefreshButton :on-refresh="refresh" />
        <button class="btn-action" @click="$router.push('/admin/orders/sale?create=1')">＋ 销售单</button>
        <button class="btn-action" @click="$router.push('/admin/orders/purchase?create=1')">＋ 采购单</button>
        <button class="btn-action" @click="$router.push('/admin/orders/finance')">收付款</button>
      </div>
    </div>

    <div v-loading="loading">
      <!-- ===== 时间段筛选 ===== -->
      <div class="period-bar">
        <button v-for="p in periodOptions" :key="p.key" class="period-tab" :class="{ active: activePeriod === p.key }" @click="selectPeriod(p.key)">{{ p.label }}</button>
        <div v-if="activePeriod === 'custom'" class="custom-range">
          <input type="date" v-model="customStart" class="date-input" @change="onCustomStartChange" />
          <span class="range-sep">~</span>
          <input type="date" v-model="customEnd" class="date-input" @change="onCustomEndChange" />
        </div>
      </div>

      <!-- ===== 顶部统计卡片 ===== -->
      <div class="stat-grid">
        <div class="stat-card" style="--accent:#C5A265">
          <div class="stat-icon-box">📦</div>
          <div class="stat-body">
            <div class="stat-val">{{ data.today?.count || 0 }}</div>
            <div class="stat-label">{{ data.today?.isToday === false ? periodLabel + '订单' : '今日订单' }}</div>
            <div class="stat-sub">¥{{ formatMoney(data.today?.amount) }}</div>
          </div>
        </div>
        <div class="stat-card" style="--accent:#52c41a">
          <div class="stat-icon-box">💰</div>
          <div class="stat-body">
            <div class="stat-val">¥{{ formatMoney(data.month?.salesAmount) }}</div>
            <div class="stat-label">{{ periodLabel }}销售额</div>
            <div class="stat-sub">
              {{ data.month?.orderCount || 0 }} 笔订单
              <span v-if="change.sales != null" class="change-tag" :class="change.sales >= 0 ? 'up' : 'down'">{{ change.sales >= 0 ? '↑' : '↓' }}{{ Math.abs(change.sales).toFixed(1) }}%</span>
            </div>
          </div>
        </div>
        <div class="stat-card" style="--accent:#1677ff">
          <div class="stat-icon-box">📈</div>
          <div class="stat-body">
            <div class="stat-val">¥{{ formatMoney(data.month?.profit) }}</div>
            <div class="stat-label">{{ periodLabel }}毛利</div>
            <div class="stat-sub">
              毛利率 {{ profitRate }}
              <span v-if="change.profit != null" class="change-tag" :class="change.profit >= 0 ? 'up' : 'down'">{{ change.profit >= 0 ? '↑' : '↓' }}{{ Math.abs(change.profit).toFixed(1) }}%</span>
            </div>
          </div>
        </div>
        <div class="stat-card" style="--accent:#fa8c16">
          <div class="stat-icon-box">⏳</div>
          <div class="stat-body">
            <div class="stat-val">¥{{ formatMoney(data.month?.receivable) }}</div>
            <div class="stat-label">应收未收</div>
            <div class="stat-sub">
              {{ periodLabel }}已收 ¥{{ formatMoney(data.month?.receipt) }}
              <span v-if="change.receivable != null" class="change-tag" :class="change.receivable <= 0 ? 'up' : 'down'">{{ change.receivable >= 0 ? '↑' : '↓' }}{{ Math.abs(change.receivable).toFixed(1) }}%</span>
            </div>
          </div>
        </div>
        <div class="stat-card stat-card-clickable" style="--accent:#722ed1" @click="$router.push('/admin/orders/sale?status=pending')">
          <div class="stat-icon-box">🔔</div>
          <div class="stat-body">
            <div class="stat-val">{{ data.todayPending?.pendingSaleCount || 0 }}</div>
            <div class="stat-label">今日待处理</div>
            <div class="stat-sub">
              今日 {{ data.todayPending?.todayOrderCount || 0 }} 单
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 趋势图 ===== -->
      <div class="chart-section">
        <div class="section-title">近12个月销售趋势</div>
        <div class="trend-compare" v-if="data.month && data.lastMonth">
          <div class="compare-item">
            <span class="compare-label">销售额</span>
            <span class="compare-cur">本月 ¥{{ formatMoney(data.month.salesAmount) }}</span>
            <span class="compare-vs">vs</span>
            <span class="compare-last">上月 ¥{{ formatMoney(data.lastMonth.salesAmount) }}</span>
            <span v-if="change.sales != null" class="change-tag" :class="change.sales >= 0 ? 'up' : 'down'">{{ change.sales >= 0 ? '↑' : '↓' }}{{ Math.abs(change.sales).toFixed(1) }}%</span>
          </div>
          <div class="compare-item">
            <span class="compare-label">毛利</span>
            <span class="compare-cur">本月 ¥{{ formatMoney(data.month.profit) }}</span>
            <span class="compare-vs">vs</span>
            <span class="compare-last">上月 ¥{{ formatMoney(data.lastMonth.profit) }}</span>
            <span v-if="change.profit != null" class="change-tag" :class="change.profit >= 0 ? 'up' : 'down'">{{ change.profit >= 0 ? '↑' : '↓' }}{{ Math.abs(change.profit).toFixed(1) }}%</span>
          </div>
          <div class="compare-item">
            <span class="compare-label">订单数</span>
            <span class="compare-cur">本月 {{ data.month.orderCount || 0 }} 单</span>
            <span class="compare-vs">vs</span>
            <span class="compare-last">上月 {{ data.lastMonth.orderCount || 0 }} 单</span>
            <span v-if="change.orderCount != null" class="change-tag" :class="change.orderCount >= 0 ? 'up' : 'down'">{{ change.orderCount >= 0 ? '↑' : '↓' }}{{ Math.abs(change.orderCount).toFixed(1) }}%</span>
          </div>
        </div>
        <div ref="trendChartRef" class="chart-box"></div>
      </div>

      <!-- ===== 待办提醒 ===== -->
      <div class="reminder-grid">
        <div class="reminder-card" :class="{ 'has-value': (reminders.pendingSaleOrders?.count || 0) > 0 }" @click="$router.push('/admin/orders/sale?status=pending')">
          <div class="reminder-icon">📋</div>
          <div class="reminder-body">
            <div class="reminder-val">{{ reminders.pendingSaleOrders?.count || 0 }}</div>
            <div class="reminder-label">待处理销售单</div>
          </div>
          <div class="reminder-items" v-if="reminders.pendingSaleOrders?.items?.length">
            <div class="reminder-item" v-for="item in reminders.pendingSaleOrders.items" :key="item.id">
              <span class="ri-name">{{ item.orderNo }}</span>
              <span class="ri-phone">{{ item.customerName }}</span>
            </div>
          </div>
        </div>
        <div class="reminder-card" :class="{ 'has-value': (reminders.pendingPurchaseOrders?.count || 0) > 0 }" @click="$router.push('/admin/orders/purchase?status=pending')">
          <div class="reminder-icon">📦</div>
          <div class="reminder-body">
            <div class="reminder-val">{{ reminders.pendingPurchaseOrders?.count || 0 }}</div>
            <div class="reminder-label">待处理采购单</div>
          </div>
          <div class="reminder-items" v-if="reminders.pendingPurchaseOrders?.items?.length">
            <div class="reminder-item" v-for="item in reminders.pendingPurchaseOrders.items" :key="item.id">
              <span class="ri-name">{{ item.orderNo }}</span>
              <span class="ri-phone">{{ item.supplierName }}</span>
            </div>
          </div>
        </div>
        <div class="reminder-card" :class="{ 'has-value': (reminders.overdueReceivables?.count || 0) > 0 }" @click="$router.push('/admin/orders/finance')">
          <div class="reminder-icon">⚠️</div>
          <div class="reminder-body">
            <div class="reminder-val">{{ reminders.overdueReceivables?.count || 0 }}</div>
            <div class="reminder-label">逾期应收（&gt;60天）</div>
          </div>
          <div class="reminder-items" v-if="reminders.overdueReceivables?.items?.length">
            <div class="reminder-item" v-for="item in reminders.overdueReceivables.items" :key="item.id">
              <span class="ri-name">{{ item.orderNo }}</span>
              <span class="ri-phone">¥{{ formatMoney(item.unpaidAmount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 客户排名 + 商品排名 + 账龄分析 ===== -->
      <div class="mid-grid">
        <div class="chart-section">
          <div class="section-title">客户销售额排名 TOP 10</div>
          <div ref="rankChartRef" class="chart-box"></div>
        </div>
        <div class="chart-section">
          <div class="section-title">商品销量排名 TOP 10</div>
          <div ref="productRankChartRef" class="chart-box"></div>
        </div>
        <div class="chart-section">
          <div class="section-title">应收账龄分析</div>
          <div ref="agingChartRef" class="chart-box"></div>
        </div>
      </div>

      <!-- ===== 底部两栏 ===== -->
      <div class="bottom-grid">
        <!-- 最近订单 -->
        <div class="bottom-card">
          <div class="section-title">最近订单</div>
          <table class="mini-table" v-if="(data.recentOrders || []).length > 0">
            <thead>
              <tr>
                <th>日期</th>
                <th>订单号</th>
                <th>客户</th>
                <th>金额</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in data.recentOrders" :key="o.id">
                <td>{{ formatDate(o.orderDate) }}</td>
                <td><router-link :to="`/admin/orders/sale?detailId=${o.id}`" class="order-link">{{ o.orderNo }}</router-link></td>
                <td>{{ o.customerName || '-' }}</td>
                <td class="num">¥{{ formatMoney(o.totalAmount) }}</td>
                <td>
                  <span class="status-tag" :class="'status-' + o.status">{{ statusLabel(o.status) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <TableSkeleton v-else-if="loading" :rows="5" :cols="5" />
          <div v-else class="empty-hint">
            <span class="empty-icon">📦</span>
            <p>暂无订单</p>
            <router-link to="/admin/orders/sale" class="empty-cta">+ 新建销售单</router-link>
          </div>
        </div>

        <!-- 订单状态分布 -->
        <div class="bottom-card">
          <div class="section-title">订单状态分布</div>
          <div ref="statusChartRef" class="chart-box-sm"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardOverview, getCustomerRank, getProductRank, getReceivableAging, getDashboardReminders } from '@/api/admin'
import TableSkeleton from '@/components/TableSkeleton.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import * as echarts from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()
const loading = ref(false)
const data = ref({})
const reminders = ref({})
const trendChartRef = ref(null)
const statusChartRef = ref(null)
const rankChartRef = ref(null)
const productRankChartRef = ref(null)
const agingChartRef = ref(null)
let trendChart = null
let statusChart = null
let rankChart = null
let productRankChart = null
let agingChart = null

// 低配机上 5 个图表同时渲染会冲击 CPU，用空闲回调延后渲染，让页面先出首帧
function idle(fn) {
  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(fn, { timeout: 400 })
  } else {
    setTimeout(fn, 80)
  }
}

// 时间段筛选
const activePeriod = ref('month')
const customStart = ref('')
const customEnd = ref('')
const periodOptions = [
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'quarter', label: '本季度' },
  { key: 'year', label: '本年' },
  { key: 'custom', label: '自定义' },
]

function pad(n) { return String(n).padStart(2, '0') }
function fmt(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }

function getDateRange(key) {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  if (key === 'month') {
    const start = new Date(y, m, 1)
    const end = new Date(y, m + 1, 0)
    return { startDate: fmt(start), endDate: fmt(end) }
  }
  if (key === 'lastMonth') {
    const start = new Date(y, m - 1, 1)
    const end = new Date(y, m, 0)
    return { startDate: fmt(start), endDate: fmt(end) }
  }
  if (key === 'quarter') {
    const qm = Math.floor(m / 3) * 3
    const start = new Date(y, qm, 1)
    const end = new Date(y, qm + 3, 0)
    return { startDate: fmt(start), endDate: fmt(end) }
  }
  if (key === 'year') {
    return { startDate: `${y}-01-01`, endDate: `${y}-12-31` }
  }
  if (key === 'custom') {
    const s = customStart.value || null
    const e = customEnd.value || null
    if (!s && !e) return {}
    return { startDate: s, endDate: e }
  }
  return {}
}

const periodLabel = computed(() => {
  const opt = periodOptions.find(p => p.key === activePeriod.value)
  if (activePeriod.value === 'custom') {
    const s = customStart.value
    const e = customEnd.value
    if (s && e) return `${s.substring(5)}~${e.substring(5)}`
    if (s) return `${s.substring(5)}起`
    if (e) return `至${e.substring(5)}`
    return '自定义'
  }
  return opt ? opt.label : '本月'
})

async function selectPeriod(key) {
  activePeriod.value = key
  if (key === 'custom') {
    if (customStart.value || customEnd.value) {
      await loadData(getDateRange('custom'))
    }
    return
  }
  const range = getDateRange(key)
  await loadData(range)
}

function onCustomStartChange() {
  if (!validateDateRange(customStart.value, customEnd.value, 'start')) { customStart.value = ''; return }
  if (customStart.value || customEnd.value) loadData(getDateRange('custom'))
}
function onCustomEndChange() {
  if (!validateDateRange(customStart.value, customEnd.value, 'end')) { customEnd.value = ''; return }
  if (customStart.value || customEnd.value) loadData(getDateRange('custom'))
}

async function refresh() {
  await loadData(getDateRange(activePeriod.value))
}

async function loadData(range) {
  loading.value = true
  try {
    const [dashRes, remindRes, rankRes, productRankRes, agingRes] = await Promise.allSettled([
      getDashboardOverview(range),
      getDashboardReminders(),
      getCustomerRank(range),
      getProductRank(range),
      getReceivableAging(range)
    ])
    if (dashRes.status === 'fulfilled') {
      data.value = dashRes.value.data?.data || dashRes.value.data || {}
      await nextTick()
      idle(() => renderTrendChart(data.value.trend))
      idle(() => renderStatusChart(data.value.statusDistribution))
    }
    if (remindRes.status === 'fulfilled') {
      reminders.value = remindRes.value.data?.data || remindRes.value.data || {}
    }
    // 客户排名柱状图
    if (rankRes.status === 'fulfilled') {
      const rankData = rankRes.value.data?.data || rankRes.value.data || []
      await nextTick()
      idle(() => renderRankChart(rankData))
    }
    // 商品排名柱状图
    if (productRankRes.status === 'fulfilled') {
      const productRankData = productRankRes.value.data?.data || productRankRes.value.data || []
      await nextTick()
      idle(() => renderProductRankChart(productRankData))
    }
    // 应收账龄饼图
    if (agingRes.status === 'fulfilled') {
      const agingData = agingRes.value.data?.data || agingRes.value.data || []
      await nextTick()
      idle(() => renderAgingChart(agingData))
    }
  } catch (err) {
    console.error('加载看板数据失败:', err)
  } finally {
    loading.value = false
  }
}

const todayStr = (() => {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日`
})()

const profitRate = computed(() => {
  const sales = data.value.month?.salesAmount || 0
  const profit = data.value.month?.profit || 0
  if (!sales) return '0.00%'
  return (profit / sales * 100).toFixed(2) + '%'
})

const change = computed(() => {
  const lm = data.value.lastMonth
  const cm = data.value.month
  if (!lm || !cm) return {}
  const pct = (cur, last) => {
    const c = Number(cur) || 0
    const l = Number(last) || 0
    if (l === 0) return c > 0 ? 100 : null
    return ((c - l) / l) * 100
  }
  return {
    sales: pct(cm.salesAmount, lm.salesAmount),
    profit: pct(cm.profit, lm.profit),
    receivable: pct(cm.receivable, lm.receivable),
    orderCount: pct(cm.orderCount, lm.orderCount)
  }
})

const statusMap = { pending: '待处理', processing: '进行中', completed: '已完成', cancelled: '已取消' }
function statusLabel(s) { return statusMap[s] || s || '-' }

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

function renderTrendChart(trend) {
  if (!trendChartRef.value || !trend || trend.length === 0) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  } else {
    trendChart.off('click')
    trendChart._clickBound = false
  }
  const months = trend.map(t => t.month)
  const sales = trend.map(t => Number(t.sales) || 0)
  const profits = trend.map(t => Number(t.profit) || 0)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        let s = `<b>${params[0].axisValue}</b><br/>`
        params.forEach(p => {
          s += `${p.marker} ${p.seriesName}: ¥${Number(p.value).toLocaleString('zh-CN')}<br/>`
        })
        return s
      }
    },
    legend: { data: ['销售额', '毛利'], right: 20, top: 0 },
    grid: { left: 60, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { formatter: v => v >= 10000 ? (v / 10000) + '万' : v } },
    series: [
      {
        name: '销售额', type: 'line', data: sales, smooth: true,
        lineStyle: { width: 3 }, itemStyle: { color: '#C5A265' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(197,162,101,0.25)' },
          { offset: 1, color: 'rgba(197,162,101,0.02)' }
        ])}
      },
      {
        name: '毛利', type: 'line', data: profits, smooth: true,
        lineStyle: { width: 3 }, itemStyle: { color: '#52c41a' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(82,196,26,0.15)' },
          { offset: 1, color: 'rgba(82,196,26,0.02)' }
        ])}
      }
    ]
  })
  if (!trendChart._clickBound) {
    trendChart.on('click', params => {
      const month = params.name // e.g. "2026-06"
      if (month && month.length === 7) {
        const [y, m] = month.split('-').map(Number)
        const start = `${y}-${String(m).padStart(2, '0')}-01`
        const lastDay = new Date(y, m, 0).getDate()
        const end = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
        router.push({ path: '/admin/orders/sale', query: { startDate: start, endDate: end } })
      }
    })
    trendChart.getZr().setCursorStyle('pointer')
    trendChart._clickBound = true
  }
}

function renderStatusChart(dist) {
  if (!statusChartRef.value || !dist) return
  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value)
  } else {
    statusChart.off('click')
  }
  const colorMap = { pending: '#fa8c16', processing: '#1677ff', completed: '#52c41a', cancelled: '#ccc' }
  const statusKeyMap = {}
  const chartData = Object.entries(dist).map(([key, val]) => {
    const label = statusLabel(key)
    statusKeyMap[label] = key
    return {
      name: label,
      value: val,
      itemStyle: { color: colorMap[key] || '#999' }
    }
  })

  statusChart.setOption({
    tooltip: { formatter: '{b}: {c} 单 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      data: chartData,
      label: { formatter: '{b}\n{c} 单', fontSize: 12 },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
    }]
  })
  statusChart.on('click', params => {
    const key = statusKeyMap[params.name]
    if (key && key !== 'cancelled') {
      router.push({ path: '/admin/orders/sale', query: { status: key } })
    }
  })
  statusChart.getZr().setCursorStyle('pointer')
}

function renderRankChart(rankData) {
  if (!rankChartRef.value || !rankData || rankData.length === 0) return
  if (!rankChart) {
    rankChart = echarts.init(rankChartRef.value)
  } else {
    rankChart.off('click')
  }
  const sorted = [...rankData].sort((a, b) => (a.totalAmount || 0) - (b.totalAmount || 0))
  const names = sorted.map(r => r.customerName || r.name || '未知')
  const amounts = sorted.map(r => Number(r.totalAmount) || 0)

  rankChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        return `<b>${p.name}</b><br/>销售额: ¥${Number(p.value).toLocaleString('zh-CN')}`
      }
    },
    grid: { left: 110, right: 40, top: 10, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLabel: { formatter: v => v >= 10000 ? (v / 10000) + '万' : v, fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 12, width: 90, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: amounts,
      barWidth: 18,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#C5A265' },
          { offset: 1, color: '#e0c78a' }
        ])
      },
      label: {
        show: true,
        position: 'right',
        formatter: p => '¥' + Number(p.value).toLocaleString('zh-CN'),
        fontSize: 11,
        color: '#666'
      }
    }]
  })
  rankChart.on('click', params => {
    router.push({ path: '/admin/orders/sale', query: { keyword: params.name } })
  })
  rankChart.getZr().setCursorStyle('pointer')
}

function renderProductRankChart(productRankData) {
  if (!productRankChartRef.value || !productRankData || productRankData.length === 0) return
  if (!productRankChart) {
    productRankChart = echarts.init(productRankChartRef.value)
  } else {
    productRankChart.off('click')
  }
  const sorted = [...productRankData].sort((a, b) => (a.totalAmount || 0) - (b.totalAmount || 0))
  const names = sorted.map(r => r.productName || '未知')
  const amounts = sorted.map(r => Number(r.totalAmount) || 0)

  productRankChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        return `<b>${p.name}</b><br/>销售额: ¥${Number(p.value).toLocaleString('zh-CN')}`
      }
    },
    grid: { left: 110, right: 40, top: 10, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLabel: { formatter: v => v >= 10000 ? (v / 10000) + '万' : v, fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 12, width: 90, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: amounts,
      barWidth: 18,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#1677ff' },
          { offset: 1, color: '#69b1ff' }
        ])
      },
      label: {
        show: true,
        position: 'right',
        formatter: p => '¥' + Number(p.value).toLocaleString('zh-CN'),
        fontSize: 11,
        color: '#666'
      }
    }]
  })
  productRankChart.on('click', params => {
    router.push({ path: '/admin/orders/sale', query: { keyword: params.name } })
  })
  productRankChart.getZr().setCursorStyle('pointer')
}

function renderAgingChart(agingData) {
  if (!agingChartRef.value || !agingData || agingData.length === 0) return
  if (!agingChart) {
    agingChart = echarts.init(agingChartRef.value)
  } else {
    agingChart.off('click')
  }
  const colorPalette = ['#52c41a', '#1677ff', '#fa8c16', '#ff4d4f']
  const chartData = agingData.map((item, i) => ({
    name: item.bucket || item.label || item.range || item.name || '未知',
    value: Number(item.amount || item.value) || 0,
    itemStyle: { color: colorPalette[i % colorPalette.length] }
  }))

  agingChart.setOption({
    tooltip: {
      formatter: p => {
        const total = chartData.reduce((s, d) => s + d.value, 0)
        const pct = total > 0 ? ((p.value / total) * 100).toFixed(1) : 0
        return `<b>${p.name}</b><br/>金额: ¥${Number(p.value).toLocaleString('zh-CN')}<br/>占比: ${pct}%`
      }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      data: chartData,
      label: {
        formatter: p => `${p.name}\n¥${Number(p.value).toLocaleString('zh-CN')}`,
        fontSize: 12
      },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
    }]
  })
  agingChart.on('click', () => {
    router.push('/admin/orders/finance')
  })
  agingChart.getZr().setCursorStyle('pointer')
}

function handleResize() {
  trendChart?.resize()
  statusChart?.resize()
  rankChart?.resize()
  productRankChart?.resize()
  agingChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  rankChart?.dispose()
  productRankChart?.dispose()
  agingChart?.dispose()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.page-desc { font-size: 13px; color: #999; margin-top: 4px; }
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-action {
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
.btn-action:hover {
  border-color: #C5A265;
  color: #C5A265;
}

/* ===== Period Bar ===== */
.period-bar {
  display: flex; align-items: center; gap: 8px; margin-bottom: 20px; flex-wrap: wrap;
}
.period-tab {
  padding: 7px 18px; border: 1px solid #e0e0e0; border-radius: 20px;
  background: #fff; font-size: 13px; color: #666; cursor: pointer;
  transition: all 0.2s; font-family: inherit;
}
.period-tab:hover { border-color: var(--gold-primary); color: var(--gold-primary); }
.period-tab.active {
  background: var(--gold-primary); color: #fff; border-color: var(--gold-primary);
  font-weight: 600;
}
.custom-range {
  display: flex; align-items: center; gap: 6px; margin-left: 4px;
}
.date-input {
  padding: 6px 10px; border: 1px solid #e0e0e0; border-radius: 6px;
  font-size: 13px; color: #333; outline: none; font-family: inherit;
}
.date-input:focus { border-color: var(--gold-primary); }
.range-sep { color: #999; font-size: 13px; }

/* ===== Stat Cards ===== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-left: 4px solid var(--accent);
  transition: transform 0.2s;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-icon-box { font-size: 32px; }
.stat-body { flex: 1; }
.stat-val {
  font-size: 24px;
  font-weight: 800;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
  margin-top: 2px;
  letter-spacing: 0.5px;
}
.stat-sub {
  font-size: 11px;
  color: rgba(255,255,255,0.35);
  margin-top: 4px;
}
.change-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  margin-left: 4px;
}
.change-tag.up { color: #69db7c; }
.change-tag.down { color: #ff6b6b; }

/* ===== Chart Section ===== */
.chart-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}
.chart-box {
  width: 100%;
  height: 320px;
}

/* ===== Mid Grid (rank + aging) ===== */
.mid-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

/* ===== Bottom Grid ===== */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
}
.bottom-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}
.chart-box-sm {
  width: 100%;
  height: 280px;
}

/* ===== Mini Table ===== */
.mini-table {
  width: 100%;
  border-collapse: collapse;
}
.mini-table th {
  text-align: left;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  background: #243447;
  white-space: nowrap;
  user-select: none;
}
.mini-table th:first-child { border-radius: 6px 0 0 0; }
.mini-table th:last-child { border-radius: 0 6px 0 0; }
.mini-table td {
  padding: 10px 12px;
  font-size: 13px;
  color: #444;
  border-bottom: 1px solid #f5f5f5;
}
.mini-table tbody tr:hover { background: #fafbfc; }
.mini-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.mini-table .mono { font-family: "SF Mono", Consolas, Monaco, monospace; font-size: 12px; color: #888; letter-spacing: 0.3px; }
.order-link { color: #409eff; text-decoration: none; font-weight: 600; font-size: 13px; font-family: "SF Mono", Consolas, Monaco, monospace; letter-spacing: 0.3px; }
.order-link:hover { text-decoration: underline; }

/* ===== Status Tags ===== */
.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.status-pending { background: #fff7e6; color: #d46b08; border: 1px solid #ffd591; }
.status-processing { background: #e6f4ff; color: #0958d9; border: 1px solid #91caff; }
.status-completed { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.status-cancelled { background: #f5f5f5; color: #999; border: 1px solid #d9d9d9; }

/* ===== Empty ===== */
.empty-hint { text-align: center; padding: 40px 20px; color: #ccc; font-size: 13px; }
.empty-icon { font-size: 32px; display: block; margin-bottom: 6px; opacity: .6; }
.empty-hint p { margin: 0 0 10px; }
.empty-cta {
  display: inline-block; padding: 7px 18px; border-radius: 20px; font-size: 13px; font-weight: 500;
  background: linear-gradient(135deg, #C5A265 0%, #d4b87a 100%); color: #fff; text-decoration: none;
  transition: all .2s; box-shadow: 0 2px 8px rgba(197,162,101,.3);
}
.empty-cta:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(197,162,101,.4); }

/* ===== Section Divider ===== */
.section-divider {
  display: flex;
  align-items: center;
  margin: 28px 0 18px;
}
.section-divider::before,
.section-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.section-divider span {
  padding: 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #888;
  letter-spacing: 2px;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
  .bottom-grid { grid-template-columns: 1fr; }
  .mid-grid { grid-template-columns: 1fr; }
  .reminder-grid { grid-template-columns: repeat(2, 1fr); }
  .trend-compare { flex-wrap: wrap; gap: 12px; }
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}

/* ===== Trend Compare ===== */
.trend-compare {
  display: flex;
  gap: 32px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fafbfc;
  border-radius: 8px;
}
.compare-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.compare-label {
  font-weight: 600;
  color: #1a1a1a;
  min-width: 48px;
}
.compare-cur { color: #1a1a1a; font-weight: 500; }
.compare-vs { color: #bbb; font-size: 12px; }
.compare-last { color: #999; }

/* ===== Stat Card Clickable ===== */
.stat-card-clickable { cursor: pointer; }
.stat-card-clickable:hover { transform: translateY(-3px); box-shadow: 0 4px 20px rgba(0,0,0,0.15); }

/* ===== Reminder Grid ===== */
.reminder-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.reminder-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.2s;
  border-left: 4px solid #e0e0e0;
  opacity: 0.6;
}
.reminder-card.has-value {
  border-left-color: #fa8c16;
  opacity: 1;
}
.reminder-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.reminder-card .reminder-icon {
  font-size: 24px;
  margin-bottom: 8px;
}
.reminder-card .reminder-body {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.reminder-val {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a1a;
  font-variant-numeric: tabular-nums;
}
.reminder-card.has-value .reminder-val {
  color: #fa8c16;
}
.reminder-label {
  font-size: 12px;
  color: #999;
}
.reminder-items {
  border-top: 1px solid #f5f5f5;
  padding-top: 8px;
}
.reminder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 3px 0;
  font-size: 12px;
}
.ri-name { color: #666; font-weight: 500; }
.ri-phone { color: #999; font-size: 11px; }


</style>

<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>退货分析</h2>
      <p class="page-desc">按商品/客户/供应商维度统计退货情况</p>
      <div class="header-right">
        <RefreshButton :on-refresh="loadData" />
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="preset-group">
        <button v-for="p in presets" :key="p.label" class="preset-btn" :class="{ active: activePreset === p.label }" @click="applyPreset(p)">{{ p.label }}</button>
      </div>
      <el-date-picker v-model="startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onDateChange" />
      <span style="margin: 0 4px; color: #999;">至</span>
      <el-date-picker v-model="endDate" type="date" placeholder="截止日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onDateChange" />
      <div class="tab-switch">
        <button class="tab-btn" :class="{ active: activeTab === 'sale' }" @click="switchTab('sale')">销售退货</button>
        <button class="tab-btn" :class="{ active: activeTab === 'purchase' }" @click="switchTab('purchase')">采购退货</button>
      </div>
      <button class="btn-export" @click="doExport">导出 Excel</button>
    </div>

    <div v-loading="loading">
      <!-- 汇总指标卡 -->
      <div class="summary-row">
        <div class="summary-card">
          <div class="sc-label">退货单数</div>
          <div class="sc-value">{{ data.summary.totalCount || 0 }}</div>
        </div>
        <div class="summary-card red">
          <div class="sc-label">退货总额</div>
          <div class="sc-value">¥{{ formatMoney(data.summary.totalAmount) }}</div>
        </div>
        <div class="summary-card blue">
          <div class="sc-label">商品种类</div>
          <div class="sc-value">{{ data.summary.productKinds || 0 }}</div>
        </div>
        <div class="summary-card purple">
          <div class="sc-label">退货数量</div>
          <div class="sc-value">{{ data.summary.totalQty || 0 }}</div>
        </div>
      </div>

      <!-- 商品排行 -->
      <div class="section">
        <div class="section-title">商品退货排行</div>
        <div class="chart-section">
          <div ref="chartRef" class="chart-box"></div>
        </div>
        <div class="card">
          <table class="rank-table" v-if="data.productRank.length">
            <thead>
              <tr>
                <th style="width:50px">排名</th>
                <th>商品名称</th>
                <th>分类</th>
                <th class="num">退货次数</th>
                <th class="num">退货数量</th>
                <th class="num">退货金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, i) in data.productRank" :key="item.commodityId || i">
                <td><span class="rank-badge" :class="'rank-' + (i + 1)">{{ i + 1 }}</span></td>
                <td class="name-cell">{{ item.productName || '-' }}</td>
                <td>{{ item.productCategory || '-' }}</td>
                <td class="num">{{ item.returnCount }}</td>
                <td class="num">{{ item.totalQty }}</td>
                <td class="num money">¥{{ formatMoney(item.totalAmount) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-hint">暂无数据</div>
        </div>
      </div>

      <!-- 客户/供应商排行 -->
      <div class="section">
        <div class="section-title">{{ activeTab === 'sale' ? '客户' : '供应商' }}退货排行</div>
        <div class="card">
          <table class="rank-table" v-if="data.partyRank.length">
            <thead>
              <tr>
                <th style="width:50px">排名</th>
                <th>{{ activeTab === 'sale' ? '客户名称' : '供应商名称' }}</th>
                <th class="num">退货次数</th>
                <th class="num">退货金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, i) in data.partyRank" :key="item.partyName || i">
                <td><span class="rank-badge" :class="'rank-' + (i + 1)">{{ i + 1 }}</span></td>
                <td class="name-cell">{{ item.partyName || '-' }}</td>
                <td class="num">{{ item.returnCount }}</td>
                <td class="num money">¥{{ formatMoney(item.totalAmount) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-hint">暂无数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getSaleReturnAnalysis, getPurchaseReturnAnalysis } from '@/api/admin'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import RefreshButton from '@/components/RefreshButton.vue'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const activeTab = ref('sale')
const startDate = ref(null)
const endDate = ref(null)
const activePreset = ref('本月')
const chartRef = ref(null)
let chart = null

const data = reactive({
  summary: {},
  productRank: [],
  partyRank: []
})

const presets = [
  { label: '全部', start: null, end: null },
  { label: '本月', start: () => { const d = new Date(); d.setDate(1); return fmt(d) }, end: () => fmt(new Date()) },
  { label: '本季度', start: () => { const d = new Date(); const q = Math.floor(d.getMonth() / 3); d.setMonth(q * 3, 1); return fmt(d) }, end: () => fmt(new Date()) },
  { label: '本年', start: () => { const d = new Date(); d.setMonth(0, 1); return fmt(d) }, end: () => fmt(new Date()) }
]

function fmt(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatMoney(v) {
  if (v == null || isNaN(v)) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function applyPreset(p) {
  activePreset.value = p.label
  startDate.value = p.start ? p.start() : null
  endDate.value = p.end ? p.end() : null
  loadData()
}

function onDateChange() {
  activePreset.value = ''
  loadData()
}

function switchTab(tab) {
  activeTab.value = tab
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const apiFn = activeTab.value === 'sale' ? getSaleReturnAnalysis : getPurchaseReturnAnalysis
    const res = await apiFn(params)
    const d = res.data?.data || res.data || {}
    data.summary = d.summary || {}
    data.productRank = d.productRank || []
    data.partyRank = d.partyRank || []

    await nextTick()
    renderChart()
  } catch (err) {
    console.error('加载退货分析失败:', err)
    ElMessage.error('加载退货分析失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const top10 = data.productRank.slice(0, 10).reverse()
  if (top10.length === 0) {
    chart.clear()
    return
  }
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 120, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    yAxis: {
      type: 'category',
      data: top10.map(p => p.productName || '-'),
      axisLabel: { fontSize: 11, width: 100, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: top10.map(p => Number(p.totalAmount) || 0),
      barMaxWidth: 20,
      itemStyle: { color: '#e74c3c', borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', fontSize: 11, formatter: p => '¥' + formatMoney(p.value) }
    }]
  }, true)
}

function doExport() {
  if (!data.productRank.length && !data.partyRank.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  import('@/utils/exportExcel').then(async ({ exportToExcel, fmtMoney }) => {
    if (data.productRank.length) {
      const columns = [
        { label: '排名', key: 'rank', width: 6 },
        { label: '商品名称', key: 'productName', width: 18 },
        { label: '分类', key: 'productCategory', width: 12 },
        { label: '退货次数', key: 'returnCount', width: 10 },
        { label: '退货数量', key: 'totalQty', width: 10 },
        { label: '退货金额', key: 'totalAmount', width: 12, format: fmtMoney }
      ]
      await exportToExcel(data.productRank.map((p, i) => ({ ...p, rank: i + 1 })), columns, '商品退货排行', { sheetName: '商品退货排名' })
    }
    if (data.partyRank.length) {
      const partyLabel = activeTab.value === 'sale' ? '客户名称' : '供应商名称'
      const columns = [
        { label: '排名', key: 'rank', width: 6 },
        { label: partyLabel, key: 'partyName', width: 18 },
        { label: '退货次数', key: 'returnCount', width: 10 },
        { label: '退货金额', key: 'totalAmount', width: 12, format: fmtMoney }
      ]
      await exportToExcel(data.partyRank.map((p, i) => ({ ...p, rank: i + 1 })), columns, activeTab.value === 'sale' ? '客户退货排行' : '供应商退货排行', { sheetName: activeTab.value === 'sale' ? '客户退货排名' : '供应商退货排名' })
    }
  })
}

onMounted(() => {
  applyPreset(presets[1])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  chart?.dispose()
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  chart?.resize()
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.page-desc { font-size: 13px; color: #999; margin: 4px 0 0; }

.filter-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px; background: #fff; border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); margin-bottom: 16px; flex-wrap: wrap;
}
.preset-group { display: flex; gap: 6px; }
.preset-btn {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px;
  background: #fff; color: #555; font-size: 13px; cursor: pointer; transition: all .2s;
}
.preset-btn:hover { border-color: #C5A265; color: #C5A265; }
.preset-btn.active { background: #C5A265; color: #fff; border-color: #C5A265; }
.tab-switch { display: flex; gap: 0; margin-left: 8px; }
.tab-btn {
  padding: 6px 20px; border: 1px solid #ddd; background: #fff;
  color: #555; font-size: 13px; cursor: pointer; transition: all .2s;
}
.tab-btn:first-child { border-radius: 6px 0 0 6px; }
.tab-btn:last-child { border-radius: 0 6px 6px 0; border-left: none; }
.tab-btn.active { background: #C5A265; color: #fff; border-color: #C5A265; }
.tab-btn:hover:not(.active) { border-color: #C5A265; color: #C5A265; }
.btn-export {
  margin-left: auto; padding: 6px 20px; border: 1px solid #C5A265;
  border-radius: 6px; background: #fff; color: #C5A265;
  font-size: 13px; cursor: pointer; transition: all .2s;
}
.btn-export:hover { background: #C5A265; color: #fff; }

.summary-row {
  display: flex; gap: 16px; margin-bottom: 16px;
}
.summary-card {
  flex: 1; padding: 18px 16px; background: #fff; border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); text-align: center;
  border-top: 3px solid #C5A265;
}
.summary-card.red { border-top-color: #e74c3c; }
.summary-card.blue { border-top-color: #409eff; }
.summary-card.purple { border-top-color: #9b59b6; }
.sc-label { font-size: 12px; color: #999; margin-bottom: 6px; }
.sc-value { font-size: 22px; font-weight: 800; color: #1a1a1a; font-variant-numeric: tabular-nums; }
.summary-card.red .sc-value { color: #e74c3c; }
.summary-card.blue .sc-value { color: #409eff; }
.summary-card.purple .sc-value { color: #9b59b6; }

.section { margin-bottom: 20px; }
.section-title {
  font-size: 15px; font-weight: 600; color: #333; margin-bottom: 10px;
  padding-left: 10px; border-left: 3px solid #C5A265;
}

.chart-section { margin-bottom: 12px; }
.chart-box { width: 100%; height: 300px; background: #fff; border-radius: 10px; }

.card {
  background: #fff; border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); overflow: hidden;
}
.rank-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
}
.rank-table thead th {
  background: #f5f5f5; color: #555; font-weight: 600;
  padding: 10px 14px; border-bottom: 1px solid #eee; text-align: left;
}
.rank-table tbody td {
  padding: 10px 14px; border-bottom: 1px solid #f5f5f5; color: #333;
}
.rank-table tbody tr:hover { background: #fafafa; }
.rank-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.rank-table .money { color: #e74c3c; font-weight: 600; }
.rank-table .name-cell { font-weight: 500; }
.rank-badge {
  display: inline-block; width: 24px; height: 24px; line-height: 24px;
  text-align: center; border-radius: 50%; font-size: 12px; font-weight: 700;
  background: #eee; color: #999;
}
.rank-1 { background: #ffd700; color: #fff; }
.rank-2 { background: #c0c0c0; color: #fff; }
.rank-3 { background: #cd7f32; color: #fff; }
.empty-hint { text-align: center; padding: 40px 20px; color: #ccc; font-size: 14px; }

@media (max-width: 1200px) {
  .summary-row { flex-wrap: wrap; }
  .summary-card { min-width: calc(50% - 8px); }
}
</style>

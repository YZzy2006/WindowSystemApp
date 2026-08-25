<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>商品销量排行</h2>
      <p class="page-desc">按销售金额排名 Top 10</p>
      <div class="header-right">
        <RefreshButton :on-refresh="loadData" />
      </div>
    </div>

    <div class="filter-bar">
      <div class="preset-group">
        <button v-for="p in presets" :key="p.label" class="preset-btn" :class="{ active: activePreset === p.label }" @click="applyPreset(p)">{{ p.label }}</button>
      </div>
      <el-date-picker v-model="startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onDateChange" />
      <span style="margin: 0 4px; color: #999;">至</span>
      <el-date-picker v-model="endDate" type="date" placeholder="截止日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" size="default" style="width: 155px" @change="onDateChange" />
    </div>

    <div v-loading="loading">
      <div class="chart-section">
        <div ref="chartRef" class="chart-box"></div>
      </div>
      <div class="card">
        <table class="rank-table" v-if="list.length">
          <thead>
            <tr>
              <th style="width:50px">排名</th>
              <th>商品名称</th>
              <th class="num">销售数量</th>
              <th class="num">销售金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, i) in list" :key="item.productName">
              <td><span class="rank-badge" :class="'rank-' + (i + 1)">{{ i + 1 }}</span></td>
              <td class="name-cell">{{ item.productName }}</td>
              <td class="num">{{ formatQty(item.totalQuantity) }}</td>
              <td class="num money">¥{{ formatMoney(item.totalAmount) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-hint">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getProductRank } from '@/api/admin'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import RefreshButton from '@/components/RefreshButton.vue'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const list = ref([])
const chartRef = ref(null)
let chart = null
const startDate = ref(null)
const endDate = ref(null)
const activePreset = ref('全部')

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

function formatQty(v) {
  if (v == null || isNaN(v)) return '0'
  return Number(v).toLocaleString('zh-CN')
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

function renderChart(data) {
  if (!chartRef.value || !data.length) return
  if (!chart) chart = echarts.init(chartRef.value)
  const names = data.map(d => d.productName).reverse()
  const amounts = data.map(d => Number(d.totalAmount) || 0).reverse()

  const maxVal = Math.max(...amounts)
  const useWan = maxVal >= 10000

  chart.setOption({
    tooltip: { formatter: p => `${p.name}<br/>销售金额: ¥${Number(p.value).toLocaleString('zh-CN')}` },
    grid: { left: 120, right: 80, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      axisLabel: {
        formatter: v => useWan ? (v / 10000).toFixed(v % 10000 === 0 ? 0 : 1) + '万' : Number(v).toLocaleString('zh-CN')
      }
    },
    yAxis: { type: 'category', data: names, axisLabel: { fontSize: 12 } },
    series: [{
      type: 'bar',
      data: amounts,
      barWidth: 16,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#52c41a' },
          { offset: 1, color: '#95de64' }
        ]),
        borderRadius: [0, 4, 4, 0]
      },
      label: { show: true, position: 'right', formatter: p => '¥' + Number(p.value).toLocaleString('zh-CN'), fontSize: 11, color: '#666' }
    }]
  })
}

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getProductRank(params)
    list.value = res.data?.data || res.data || []
    await nextTick()
    if (chart) { chart.dispose(); chart = null }
    renderChart(list.value)
  } catch (err) {
    console.error('加载商品排行失败:', err)
  } finally {
    loading.value = false
  }
}

function handleResize() { chart?.resize() }

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadData()
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

.filter-bar {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 16px; flex-wrap: wrap;
}
.preset-group { display: flex; gap: 8px; }
.preset-btn {
  padding: 6px 16px; border: 1px solid #e0e0e0; border-radius: 6px;
  background: #fff; font-size: 13px; color: #666; cursor: pointer; transition: all 0.2s;
}
.preset-btn:hover { border-color: #52c41a; color: #52c41a; }
.preset-btn.active { background: #52c41a; color: #fff; border-color: #52c41a; }

.chart-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.chart-box { width: 100%; height: 360px; }

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}

.rank-table { width: 100%; border-collapse: collapse; }
.rank-table th {
  text-align: left; padding: 12px 16px; font-size: 12px; font-weight: 600;
  color: #888; background: #fafafa; border-bottom: 1px solid #f0f0f0;
}
.rank-table td { padding: 12px 16px; font-size: 13px; color: #333; border-bottom: 1px solid #f5f5f5; }
.rank-table tbody tr:hover { background: #fafcff; }
.rank-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.rank-table .money { font-weight: 600; color: #52c41a; }
.name-cell { font-weight: 500; }

.rank-badge {
  display: inline-block; width: 24px; height: 24px; line-height: 24px;
  text-align: center; border-radius: 50%; font-size: 12px; font-weight: 700;
  background: #f0f0f0; color: #999;
}
.rank-1 { background: #f6ffed; color: #389e0d; }
.rank-2 { background: #f0f0f0; color: #666; }
.rank-3 { background: #fff7e6; color: #d48806; }

.empty-hint { text-align: center; padding: 60px 20px; color: #ccc; font-size: 14px; }
</style>

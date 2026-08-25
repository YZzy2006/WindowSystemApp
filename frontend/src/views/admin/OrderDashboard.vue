<template>
  <div class="order-dashboard admin-page">
    <div class="page-header">
      <h2>订单总览</h2>
    </div>

    <!-- 核心指标卡片 -->
    <div class="kpi-grid" v-loading="loading" element-loading-background="transparent">
      <router-link class="kpi-card" v-for="k in kpis" :key="k.label" :to="k.link" :style="{ '--accent': k.color }">
        <div class="kpi-top">
          <div class="kpi-icon-ring">
            <span class="kpi-icon">{{ k.icon }}</span>
          </div>
          <div class="kpi-trend">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg>
          </div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value">{{ k.value }}</div>
          <div class="kpi-label">{{ k.label }}</div>
        </div>
        <div class="kpi-progress">
          <div class="kpi-bar" :style="{ width: k.pct + '%' }"></div>
        </div>
      </router-link>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-grid">
      <router-link v-for="(q, qi) in quickLinks" :key="q.to" :to="q.to" class="quick-card" :class="'qc-' + qi">
        <span class="qc-icon-ring">
          <span class="qc-icon">{{ q.icon }}</span>
        </span>
        <span class="qc-label">{{ q.label }}</span>
        <svg class="qc-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </router-link>
    </div>

    <!-- 两栏：最近订单 + 最近收付款 -->
    <div class="dual-col">
      <div class="panel">
        <div class="panel-head">
          <h3>最近销售订单</h3>
          <router-link to="/admin/orders/sale" class="link-more">查看全部 &rarr;</router-link>
        </div>
        <table class="mini-table" v-if="recentSaleOrders.length">
          <thead><tr><th>单号</th><th>客户</th><th>金额</th><th>状态</th><th>日期</th></tr></thead>
          <tbody>
            <tr v-for="o in recentSaleOrders" :key="o.id">
              <td><router-link :to="saleDetailLink(o)" class="order-link">{{ o.orderNo }}</router-link></td>
              <td>{{ o.customerName }}</td>
              <td class="mono">&yen;{{ formatNum(o.totalAmount) }}</td>
              <td><span class="status-dot" :class="'s-' + o.status"></span>{{ statusText(o.status) }}</td>
              <td class="muted">{{ o.orderDate }}</td>
            </tr>
          </tbody>
        </table>
        <TableSkeleton v-else-if="loading" :rows="5" :cols="5" />
        <div v-else class="empty-hint">
          <span class="empty-icon">📦</span>
          <p>暂无订单数据</p>
          <router-link to="/admin/orders/sale" class="empty-cta">+ 新建第一笔销售单</router-link>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <h3>最近收付款</h3>
          <router-link to="/admin/orders/finance" class="link-more">查看全部 &rarr;</router-link>
        </div>
        <table class="mini-table" v-if="recentPayments.length">
          <thead><tr><th>日期</th><th>类型</th><th>金额</th><th>款方</th><th>订单</th></tr></thead>
          <tbody>
            <tr v-for="p in recentPayments" :key="p.id">
              <td class="muted">{{ p.paymentDate }}</td>
              <td><span class="pay-tag" :class="p.type === 'receipt' ? 'pay-receipt' : 'pay-payment'">{{ p.type === 'receipt' ? '收款' : '付款' }}</span></td>
              <td class="mono" :class="p.type === 'receipt' ? 'green' : 'red'">{{ p.type === 'receipt' ? '+' : '-' }}&yen;{{ formatNum(p.amount) }}</td>
              <td>{{ p.partyName || '-' }}</td>
              <td>
                <router-link v-if="p.orderNo && paymentDetailLink(p)" :to="paymentDetailLink(p)" class="order-link">{{ p.orderNo }}</router-link>
                <span v-else class="muted">{{ p.orderNo || '-' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <TableSkeleton v-else-if="loading" :rows="5" :cols="5" />
        <div v-else class="empty-hint">
          <span class="empty-icon">💰</span>
          <p>暂无收付款记录</p>
          <router-link to="/admin/orders/finance" class="empty-cta">去录入收付款</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSaleOrders, getPayments, getSaleOrderSummary, getPurchaseOrderSummary, getStockInSummary, getStockOutSummary, getSaleReturnSummary, getPurchaseReturnSummary } from '@/api/admin'
import TableSkeleton from '@/components/TableSkeleton.vue'

const router = useRouter()
const loading = ref(true)

const kpis = ref([
  { icon: '📦', label: '销售订单', value: '0', color: '#4FC3F7', pct: 0, link: '/admin/orders/sale' },
  { icon: '🛒', label: '采购订单', value: '0', color: '#66BB6A', pct: 0, link: '/admin/orders/purchase' },
  { icon: '📥', label: '入库单', value: '0', color: '#FFA726', pct: 0, link: '/admin/orders/stock-in' },
  { icon: '📤', label: '出库单', value: '0', color: '#EF5350', pct: 0, link: '/admin/orders/stock-out' },
  { icon: '↩️', label: '销售退货', value: '0', color: '#AB47BC', pct: 0, link: '/admin/orders/sale-return' },
  { icon: '🔄', label: '采购退货', value: '0', color: '#78909C', pct: 0, link: '/admin/orders/purchase-return' },
])

const quickLinks = [
  { icon: '📦', label: '新建销售单', to: '/admin/orders/sale' },
  { icon: '🛒', label: '新建采购单', to: '/admin/orders/purchase' },
  { icon: '📥', label: '入库操作', to: '/admin/orders/stock-in' },
  { icon: '📤', label: '出库操作', to: '/admin/orders/stock-out' },
  { icon: '💰', label: '收付款', to: '/admin/orders/finance' },
  { icon: '📋', label: '商品管理', to: '/admin/orders/commodities' },
  { icon: '👤', label: '客户管理', to: '/admin/orders/customers' },
  { icon: '🏭', label: '供应商管理', to: '/admin/orders/suppliers' },
]

const recentSaleOrders = ref([])
const recentPayments = ref([])

function formatNum(n) {
  if (n == null) return '0.00'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusText(s) {
  const map = { pending: '待处理', processing: '进行中', completed: '已完成', cancelled: '已取消' }
  return map[s] || s || '-'
}

function saleDetailLink(o) {
  return `/admin/orders/sale?detailId=${o.id}`
}

function paymentDetailLink(p) {
  const routeMap = { receipt: '/admin/orders/sale', payment: '/admin/orders/purchase', saleReturn: '/admin/orders/sale-return', purchaseReturn: '/admin/orders/purchase-return' }
  const base = routeMap[p.type]
  return base && p.orderId ? `${base}?detailId=${p.orderId}` : ''
}

const d = (r) => r?.data?.data ?? r?.data ?? r ?? {}
const extractTotal = (r) => d(r).total ?? d(r).totalCount ?? d(r).length ?? 0

async function loadSummary() {
  try {
    const [sale, purchase, sin, sout, sr, pr] = await Promise.all([
      getSaleOrderSummary({}).catch(() => ({ data: {} })),
      getPurchaseOrderSummary({}).catch(() => ({ data: {} })),
      getStockInSummary({}).catch(() => ({ data: {} })),
      getStockOutSummary({}).catch(() => ({ data: {} })),
      getSaleReturnSummary({}).catch(() => ({ data: {} })),
      getPurchaseReturnSummary({}).catch(() => ({ data: {} })),
    ])
    const counts = [
      d(sale).totalCount ?? 0,
      d(purchase).totalCount ?? 0,
      d(sin).totalCount ?? 0,
      d(sout).totalCount ?? 0,
      d(sr).totalCount ?? 0,
      d(pr).totalCount ?? 0,
    ]
    // eslint-disable-next-line no-console
    console.log('OrderDashboard totals:', counts)
    kpis.value = [
      { icon: '📦', label: '销售订单', value: counts[0] + ' 单', color: '#4FC3F7', pct: 0, link: '/admin/orders/sale' },
      { icon: '🛒', label: '采购订单', value: counts[1] + ' 单', color: '#66BB6A', pct: 0, link: '/admin/orders/purchase' },
      { icon: '📥', label: '入库单',     value: counts[2] + ' 单', color: '#FFA726', pct: 0, link: '/admin/orders/stock-in' },
      { icon: '📤', label: '出库单',     value: counts[3] + ' 单', color: '#EF5350', pct: 0, link: '/admin/orders/stock-out' },
      { icon: '↩️', label: '销售退货',   value: counts[4] + ' 单', color: '#AB47BC', pct: 0, link: '/admin/orders/sale-return' },
      { icon: '🔄', label: '采购退货',   value: counts[5] + ' 单', color: '#78909C', pct: 0, link: '/admin/orders/purchase-return' },
    ]
    const maxCount = Math.max(...counts, 1)
    kpis.value.forEach(k => { k.pct = Math.round((parseInt(k.value) / maxCount) * 100) })
  } catch (e) {
    console.error('加载汇总失败', e)
  }
}

onMounted(async () => {
  loadSummary()
  try {
    const [ordersRes, payRes] = await Promise.all([
      getSaleOrders({ page: 1, size: 5 }),
      getPayments({ page: 1, size: 8 })
    ])
    recentSaleOrders.value = d(ordersRes).records || d(ordersRes).list || d(ordersRes).content || []
    const payData = d(payRes)
    recentPayments.value = (payData.records || payData.list || payData.content || (Array.isArray(payData) ? payData : [])).slice(0, 8)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.order-dashboard { }


/* KPI 卡片 */
.kpi-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; margin-bottom: 28px; }
.kpi-card {
  display: flex; flex-direction: column; text-decoration: none;
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px; padding: 0; position: relative;
  overflow: hidden; border: none; transition: all .3s cubic-bezier(.4,0,.2,1);
  color: #fff;
}
.kpi-card::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px;
  background: var(--accent);
}
.kpi-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(0,0,0,.25);
}
.kpi-top {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 0 20px;
}
.kpi-icon-ring {
  width: 42px; height: 42px; border-radius: 10px;
  background: var(--accent);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 10px rgba(0,0,0,.2);
}
.kpi-icon { font-size: 20px; line-height: 1; }
.kpi-trend { color: rgba(255,255,255,.3); opacity: 0; transition: opacity .3s; }
.kpi-card:hover .kpi-trend { opacity: 1; }
.kpi-body { flex: 1; padding: 12px 20px 18px 20px; }
.kpi-value { font-size: 28px; font-weight: 800; font-family: var(--font-mono); letter-spacing: -0.5px; }
.kpi-label { font-size: 12px; color: rgba(255,255,255,.50); margin-top: 4px; font-weight: 500; letter-spacing: 0.5px; }
.kpi-progress { height: 4px; background: rgba(255,255,255,.08); }
.kpi-bar { height: 100%; background: var(--accent); opacity: .5; border-radius: 0 3px 0 0; transition: width .8s cubic-bezier(.4,0,.2,1); }

/* 快捷入口 */
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 28px; }
.quick-card {
  display: flex; align-items: center; gap: 10px; padding: 14px 16px;
  background: #fff; border-radius: 10px; border: 1px solid #f0f0f0;
  transition: all .25s cubic-bezier(.4,0,.2,1); cursor: pointer; font-size: 14px; color: #333;
  text-decoration: none;
}
.quick-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,.08); }
.qc-icon-ring {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.qc-0 .qc-icon-ring { background: #4FC3F7; }
.qc-1 .qc-icon-ring { background: #66BB6A; }
.qc-2 .qc-icon-ring { background: #FFA726; }
.qc-3 .qc-icon-ring { background: #EF5350; }
.qc-4 .qc-icon-ring { background: #66BB6A; }
.qc-5 .qc-icon-ring { background: #AB47BC; }
.qc-6 .qc-icon-ring { background: #42A5F5; }
.qc-7 .qc-icon-ring { background: #78909C; }
.qc-icon { font-size: 17px; line-height: 1; }
.qc-label { flex: 1; font-weight: 500; }
.qc-arrow { color: #ccc; flex-shrink: 0; transition: transform .2s, color .2s; }
.quick-card:hover .qc-arrow { transform: translateX(2px); color: #C5A265; }

/* 双栏 */
.dual-col { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
@media (max-width: 1200px) {
  .kpi-grid { grid-template-columns: repeat(3, 1fr); }
  .quick-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .dual-col { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .kpi-grid { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: 1fr; }
}

.panel {
  background: #fff; border-radius: 12px;
  border: 1px solid #f0f0f0; overflow: hidden;
}
.panel:first-child { border-top: 3px solid #4FC3F7; }
.panel:last-child { border-top: 3px solid #66BB6A; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; background: #fafbfc;
  border-bottom: 1px solid #f0f0f0;
}
.panel-head h3 { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.link-more {
  font-size: 12px; color: #999; transition: color .2s; text-decoration: none;
  font-weight: 500; display: flex; align-items: center; gap: 4px;
}
.link-more:hover { color: #C5A265; }

.mini-table { width: 100%; border-collapse: collapse; }
.mini-table thead { background: #fafbfc; }
.mini-table th {
  text-align: left; padding: 10px 16px; font-size: 11px;
  color: #999; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.5px; border-bottom: 1px solid #f0f0f0;
}
.mini-table td { padding: 11px 16px; font-size: 13px; border-bottom: 1px solid #f5f5f5; }
.mini-table tbody tr { transition: background .15s; }
.mini-table tbody tr:hover { background: #fafcff; }
.mono { font-family: var(--font-mono); font-size: 12px; }
.order-link {
  font-family: var(--font-mono); font-size: 12px; font-weight: 600;
  color: #1677ff; text-decoration: none; cursor: pointer;
  transition: color .2s;
}
.order-link:hover { color: #0958d9; text-decoration: underline; }
.muted { color: #aaa; }

.status-dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
  margin-right: 6px; vertical-align: middle; box-shadow: 0 0 6px currentColor;
}
.s-pending { background: #FFA726; color: #FFA726; }
.s-processing { background: #4FC3F7; color: #4FC3F7; animation: pulse 1.5s infinite; }
.s-completed { background: #66BB6A; color: #66BB6A; }
.s-cancelled { background: #bdbdbd; color: #bdbdbd; }

.pay-tag {
  font-size: 11px; padding: 3px 10px; border-radius: 10px; font-weight: 600;
}
.pay-receipt { background: #e8f5e9; color: #2e7d32; }
.pay-payment { background: #fbe9e7; color: #c62828; }
.green { color: #2e7d32; font-weight: 600; }
.red { color: #c62828; font-weight: 600; }

.empty-hint { text-align: center; color: #ccc; padding: 48px 0; font-size: 14px; }
.empty-icon { font-size: 36px; display: block; margin-bottom: 8px; opacity: .6; }
.empty-hint p { margin: 0 0 12px; }
.empty-cta {
  display: inline-block; padding: 8px 20px; border-radius: 20px; font-size: 13px; font-weight: 500;
  background: linear-gradient(135deg, #C5A265 0%, #d4b87a 100%); color: #fff; text-decoration: none;
  transition: all .2s; box-shadow: 0 2px 8px rgba(197,162,101,.3);
}
.empty-cta:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(197,162,101,.4); }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .4; }
}
</style>

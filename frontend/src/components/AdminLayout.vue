<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-icon">◈</span>
        <span class="brand-text">顺居门业</span>
        <span class="brand-tag">管理后台</span>
      </div>
      <nav class="sidebar-nav">
        <!-- ===== 最近访问 ===== -->
        <div class="recent-section" v-if="recentOrders.length">
          <div class="recent-header">
            <span class="recent-icon">🕐</span>
            <span class="recent-label">最近访问</span>
            <button class="recent-clear" @click="clearRecent" title="清空">✕</button>
          </div>
          <router-link
            v-for="o in recentOrders" :key="o.id + o.type"
            :to="recentRoute(o)"
            class="recent-item"
          >
            <span class="recent-type" :class="'type-' + o.type">{{ recentTypeLabel(o.type) }}</span>
            <span class="recent-customer">{{ o.customerName || o.orderNo || '#' + o.id }}</span>
          </router-link>
        </div>
        <!-- ===== 工作台（可折叠） ===== -->
        <div class="nav-group" :class="{ open: dashboardOpen, active: isDashboardActive }">
          <button class="nav-group-header" @click="dashboardOpen = !dashboardOpen">
            <span class="nav-icon">📊</span>
            <span class="nav-label">工作台</span>
            <span class="nav-arrow" :class="{ rotated: dashboardOpen }">&#9662;</span>
          </button>
          <div class="nav-group-body" v-show="dashboardOpen">
            <router-link to="/admin/dashboard" class="nav-sub-item nav-green" exact-active-class="active">
              <span class="nav-dot"></span> 经营看板
            </router-link>
            <router-link to="/admin/dashboard/customer-rank" class="nav-sub-item" active-class="active">
              <span class="nav-dot"></span> 客户排行
            </router-link>
            <router-link to="/admin/dashboard/product-rank" class="nav-sub-item" active-class="active">
              <span class="nav-dot"></span> 商品排行
            </router-link>
            <router-link to="/admin/dashboard/receivable-aging" class="nav-sub-item" active-class="active">
              <span class="nav-dot"></span> 应收账龄
            </router-link>
            <router-link to="/admin/dashboard/return-analysis" class="nav-sub-item" active-class="active">
              <span class="nav-dot"></span> 退货分析
            </router-link>
          </div>
        </div>
        <!-- ===== 订单系统（可折叠） ===== -->
        <div class="nav-group" :class="{ open: orderOpen, active: isOrderActive }">
          <button class="nav-group-header" @click="orderOpen = !orderOpen">
            <span class="nav-icon">📦</span>
            <span class="nav-label">订单系统</span>
            <span class="nav-arrow" :class="{ rotated: orderOpen }">&#9662;</span>
          </button>
          <div class="nav-group-body" v-show="orderOpen">
            <!-- 订单 -->
            <router-link to="/admin/orders" class="nav-sub-item nav-blue" exact-active-class="active">
              <span class="nav-dot"></span> 订单总览
            </router-link>
            <router-link to="/admin/orders/sale" class="nav-sub-item nav-blue" active-class="active">
              <span class="nav-dot"></span> 销售订单
            </router-link>
            <router-link to="/admin/orders/purchase" class="nav-sub-item nav-blue" active-class="active">
              <span class="nav-dot"></span> 采购订单
            </router-link>
            <router-link to="/admin/orders/sale-return" class="nav-sub-item nav-blue" active-class="active">
              <span class="nav-dot"></span> 销售退货
            </router-link>
            <router-link to="/admin/orders/purchase-return" class="nav-sub-item nav-blue" active-class="active">
              <span class="nav-dot"></span> 采购退货
            </router-link>
            <!-- 仓储/管理 -->
            <router-link to="/admin/orders/stock-in" class="nav-sub-item nav-orange" active-class="active">
              <span class="nav-dot"></span> 入库管理
            </router-link>
            <router-link to="/admin/orders/stock-out" class="nav-sub-item nav-orange" active-class="active">
              <span class="nav-dot"></span> 出库管理
            </router-link>
            <router-link to="/admin/orders/customers" class="nav-sub-item nav-orange" active-class="active">
              <span class="nav-dot"></span> 客户管理
            </router-link>
            <router-link to="/admin/orders/after-sale" class="nav-sub-item nav-orange" active-class="active">
              <span class="nav-dot"></span> 售后管理
            </router-link>
            <!-- 财务 -->
            <router-link to="/admin/orders/finance" class="nav-sub-item nav-green" active-class="active">
              <span class="nav-dot"></span> 收付款
            </router-link>
            <router-link to="/admin/orders/payment-report" class="nav-sub-item nav-green" active-class="active">
              <span class="nav-dot"></span> 收付款查询
            </router-link>
            <!-- 基础数据 -->
            <router-link to="/admin/orders/suppliers" class="nav-sub-item" active-class="active">
              <span class="nav-dot"></span> 供应商
            </router-link>
            <router-link to="/admin/orders/commodities" class="nav-sub-item nav-purple" active-class="active">
              <span class="nav-dot"></span> 商品库存
            </router-link>
            <router-link to="/admin/orders/product-types" class="nav-sub-item nav-purple" active-class="active">
              <span class="nav-dot"></span> 商品细目
            </router-link>
            <router-link to="/admin/orders/formulas" class="nav-sub-item nav-purple" active-class="active">
              <span class="nav-dot"></span> 计算公式
            </router-link>
            <!-- 系统 -->
            <router-link to="/admin/orders/print-settings" class="nav-sub-item nav-gray" active-class="active">
              <span class="nav-dot"></span> 打印设置
            </router-link>
            <router-link to="/admin/orders/system-settings" class="nav-sub-item nav-gray" active-class="active">
              <span class="nav-dot"></span> 系统设置
            </router-link>
            <router-link to="/admin/orders/undo-log" class="nav-sub-item nav-gray" active-class="active">
              <span class="nav-dot"></span> 撤回日志
            </router-link>
            <router-link to="/admin/orders/operation-log" class="nav-sub-item nav-gray" active-class="active">
              <span class="nav-dot"></span> 操作日志
            </router-link>
            <router-link v-if="auth.role === 'super_admin'" to="/admin/orders/backup" class="nav-sub-item nav-red" active-class="active">
              <span class="nav-dot"></span> 数据备份
            </router-link>
          </div>
        </div>

        <router-link v-if="auth.role === 'super_admin'" to="/admin/accounts" class="nav-item" active-class="active">
          <span class="nav-icon">👥</span> 账号管理
        </router-link>
        <router-link to="/admin/password" class="nav-item" active-class="active">
          <span class="nav-icon">🛡</span> 账号与安全
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <button class="logout-btn" @click="handleLogout">
          <span>←</span> 退出登录
        </button>
      </div>
    </aside>
    <main class="main">
      <BreadcrumbNav />
      <router-view />
    </main>
    <UndoPanel />
    <CommandPalette />
    <BackToTop />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter, useRoute } from 'vue-router'
import CommandPalette from './CommandPalette.vue'
import BackToTop from './BackToTop.vue'
import BreadcrumbNav from './BreadcrumbNav.vue'
import { useUndoLogStore } from '@/composables/undoLogStore'
import { useRecentOrders } from '@/composables/useRecentOrders'
import UndoPanel from './UndoPanel.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const { operations, undoOperation } = useUndoLogStore()
const { recentOrders, clearRecent } = useRecentOrders()

const typeRouteMap = {
  sale: '/admin/orders/sale',
  purchase: '/admin/orders/purchase',
  'sale-return': '/admin/orders/sale-return',
  'purchase-return': '/admin/orders/purchase-return'
}
const typeLabelMap = {
  sale: '销售',
  purchase: '采购',
  'sale-return': '销退',
  'purchase-return': '采退'
}
function recentRoute(o) {
  const base = typeRouteMap[o.type] || '/admin/orders/sale'
  return `${base}?detailId=${o.id}`
}
function recentTypeLabel(type) {
  return typeLabelMap[type] || type
}

const orderOpen = ref(true)
const isOrderActive = computed(() => route.path.startsWith('/admin/orders'))

const dashboardOpen = ref(true)
const isDashboardActive = computed(() => route.path.startsWith('/admin/dashboard'))

async function handleLogout() {
  await auth.logout()
  router.push('/admin/login')
}

// 全局快捷键
function handleGlobalKeydown(e) {
  const tag = e.target.tagName
  const inInput = tag === 'INPUT' || tag === 'TEXTAREA' || e.target.isContentEditable

  // Ctrl+Z 撤回（不拦截输入框内的文本撤销）
  if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
    if (inInput) return
    const now = Date.now()
    const latest = operations.value.find(op => !op.undone && op.expiresAt > now)
    if (latest) {
      e.preventDefault()
      undoOperation(latest.id)
    }
  }

  // Ctrl+Q 快速新建销售单
  if ((e.ctrlKey || e.metaKey) && e.key === 'q') {
    if (inInput) return
    e.preventDefault()
    router.push('/admin/orders/sale?create=1')
  }
}
onMounted(() => document.addEventListener('keydown', handleGlobalKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleGlobalKeydown))
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }

.sidebar {
  width: 200px; background: linear-gradient(180deg, #1c1c1c 0%, #141414 100%);
  color: #fff; display: flex; flex-direction: column; flex-shrink: 0; position: fixed; top: 0; left: 0; bottom: 0; z-index: 50;
}

.sidebar-brand {
  padding: 24px 18px 18px; border-bottom: 1px solid rgba(255,255,255,.06);
  display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px;
}
.brand-icon { font-size: 24px; color: #c49a2c; }
.brand-text { font-size: 18px; font-weight: 600; letter-spacing: 2px; }
.brand-tag { font-size: 11px; color: rgba(255,255,255,.3); margin-left: 2px; }

.sidebar-nav { flex: 1; padding: 12px 0; overflow-y: auto; }
.nav-item {
  display: flex; align-items: center; gap: 10px; padding: 11px 18px;
  color: rgba(255,255,255,.55); font-size: 14px; transition: all .2s;
  border-left: 3px solid transparent;
}
.nav-item:hover { color: #fff; background: rgba(255,255,255,.04); }
.nav-item.active { color: #d4af37; background: rgba(196,154,44,.12); border-left-color: #d4af37; font-weight: 500; }
.nav-icon { font-size: 16px; width: 22px; text-align: center; }

/* 订单系统折叠组 */
.nav-group { }
.nav-group-header {
  display: flex; align-items: center; gap: 10px; width: 100%;
  padding: 11px 18px; background: transparent; color: rgba(255,255,255,.7);
  font-size: 14px; font-weight: 600; border: none; cursor: pointer; transition: all .2s;
  border-left: 3px solid transparent;
}
.nav-group-header:hover { color: #fff; background: rgba(255,255,255,.04); }
.nav-group.active .nav-group-header { color: #d4af37; background: rgba(196,154,44,.08); border-left-color: #d4af37; }
.nav-label { flex: 1; text-align: left; }
.nav-arrow { font-size: 11px; transition: transform .25s ease; opacity: .4; }
.nav-arrow.rotated { transform: rotate(0deg); }
.nav-arrow:not(.rotated) { transform: rotate(-90deg); }
.nav-group-body { padding: 2px 0 4px; overflow: hidden; }
.nav-sub-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 18px 8px 32px;
  color: rgba(255,255,255,.65); font-size: 13px; transition: all .2s;
  border-left: 3px solid transparent;
}
.nav-sub-item:hover { color: rgba(255,255,255,.8); background: rgba(255,255,255,.03); }
.nav-sub-item.active { color: #d4af37; background: rgba(196,154,44,.1); border-left-color: rgba(212,175,55,.5); }
.nav-dot {
  width: 5px; height: 5px; border-radius: 50%; background: rgba(255,255,255,.35);
  flex-shrink: 0; transition: background .2s;
}
.nav-sub-item.active .nav-dot { background: #d4af37; box-shadow: 0 0 6px rgba(212,175,55,.4); }
.nav-blue { color: #79bbff; }
.nav-blue .nav-dot { background: #409eff; }
.nav-blue:hover { color: #a0cfff; background: rgba(64,158,255,.06); }
.nav-blue.active { color: #d4af37; }
.nav-orange { color: #f0c78a; }
.nav-orange .nav-dot { background: #e6a23c; }
.nav-orange:hover { color: #f5dab1; background: rgba(230,162,60,.06); }
.nav-orange.active { color: #d4af37; }
.nav-green { color: #95d475; }
.nav-green .nav-dot { background: #67c23a; }
.nav-green:hover { color: #b3e19d; background: rgba(103,194,58,.06); }
.nav-green.active { color: #d4af37; }
.nav-gray { color: #a3a6ad; }
.nav-gray .nav-dot { background: #909399; }
.nav-gray:hover { color: #c0c4cc; background: rgba(144,147,153,.06); }
.nav-gray.active { color: #d4af37; }
.nav-purple { color: #c5a3e8; }
.nav-purple .nav-dot { background: #9b59b6; }
.nav-purple:hover { color: #d4b8f0; background: rgba(155,89,182,.06); }
.nav-purple.active { color: #d4af37; }
.nav-red { color: #f89898; }
.nav-red .nav-dot { background: #f56c6c; }
.nav-red:hover { color: #fab6b6; background: rgba(245,108,108,.06); }
.nav-red.active { color: #d4af37; }

/* 最近访问 */
.recent-section {
  padding: 8px 0;
  border-bottom: 1px solid rgba(255,255,255,.06);
  margin-bottom: 4px;
}
.recent-header {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 18px 4px;
  font-size: 11px; color: rgba(255,255,255,.3);
  text-transform: uppercase; letter-spacing: 1px;
}
.recent-icon { font-size: 12px; }
.recent-label { flex: 1; }
.recent-clear {
  background: none; border: none; color: rgba(255,255,255,.2);
  cursor: pointer; font-size: 11px; padding: 2px 4px; line-height: 1;
}
.recent-clear:hover { color: rgba(255,255,255,.5); }
.recent-item {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 18px 5px 22px;
  font-size: 12px; color: rgba(255,255,255,.4);
  text-decoration: none; transition: all .15s;
  border-left: 3px solid transparent;
}
.recent-item:hover { color: rgba(255,255,255,.7); background: rgba(255,255,255,.03); }
.recent-type {
  font-size: 10px; font-weight: 600; padding: 1px 6px; border-radius: 3px;
  background: rgba(255,255,255,.1); color: rgba(255,255,255,.7);
  flex-shrink: 0;
}
.recent-type.type-sale { background: rgba(64,158,255,.2); color: #79bbff; }
.recent-type.type-purchase { background: rgba(230,162,60,.2); color: #f0c78a; }
.recent-type.type-sale-return { background: rgba(103,194,58,.2); color: #95d475; }
.recent-type.type-purchase-return { background: rgba(155,89,182,.2); color: #c5a3e8; }
.recent-no {
  font-family: "SF Mono", Consolas, monospace; font-size: 11px;
  color: rgba(255,255,255,.5); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.recent-customer {
  font-size: 12px; font-weight: 600; color: rgba(255,255,255,.75);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-left: auto;
}

.sidebar-footer { padding: 14px 16px; border-top: 1px solid rgba(255,255,255,.06); }
.logout-btn {
  width: 100%; padding: 10px; background: transparent; color: rgba(255,255,255,.4);
  border: 1px solid rgba(255,255,255,.1); border-radius: 6px; font-size: 13px;
  cursor: pointer; transition: all .2s; display: flex; align-items: center; justify-content: center; gap: 6px;
}
.logout-btn:hover { color: #e74c3c; border-color: rgba(231,76,60,.3); background: rgba(231,76,60,.06); }

.main { flex: 1; margin-left: 200px; background: #f8f9fb; padding: 28px 32px; min-height: 100vh; }
</style>

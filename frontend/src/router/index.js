import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // 根路径直接跳转管理后台
  { path: '/', redirect: '/admin' },
  // ==================== 后台管理 ====================
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { title: '经营看板' } },
      { path: 'dashboard/customer-rank', name: 'CustomerRank', component: () => import('@/views/admin/CustomerRankView.vue'), meta: { title: '客户排行' } },
      { path: 'dashboard/product-rank', name: 'ProductRank', component: () => import('@/views/admin/ProductRankView.vue'), meta: { title: '商品排行' } },
      { path: 'dashboard/receivable-aging', name: 'ReceivableAging', component: () => import('@/views/admin/ReceivableAgingView.vue'), meta: { title: '应收账龄' } },
      { path: 'dashboard/return-analysis', name: 'ReturnAnalysis', component: () => import('@/views/admin/ReturnAnalysisView.vue'), meta: { title: '退货分析' } },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/CategoriesView.vue'), meta: { title: '分类管理' } },
      { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/ProductsView.vue'), meta: { title: '产品管理' } },
      { path: 'cases', name: 'AdminCases', component: () => import('@/views/admin/CasesView.vue'), meta: { title: '案例管理' } },
      { path: 'password', name: 'ChangePassword', component: () => import('@/views/admin/ChangePasswordView.vue'), meta: { title: '账号与安全' } },
      { path: 'accounts', name: 'AccountManagement', component: () => import('@/views/admin/AccountManagementView.vue'), meta: { requiresSuperAdmin: true, title: '账号管理' } },
      // ===== 订单系统 =====
      { path: 'orders', name: 'OrderDashboard', component: () => import('@/views/admin/OrderDashboard.vue'), meta: { title: '订单总览' } },
      { path: 'orders/sale', name: 'SaleOrders', component: () => import('@/views/admin/SaleOrdersView.vue'), meta: { title: '销售订单' } },
      { path: 'orders/purchase', name: 'PurchaseOrders', component: () => import('@/views/admin/PurchaseOrdersView.vue'), meta: { title: '采购订单' } },
      { path: 'orders/sale-return', name: 'SaleReturns', component: () => import('@/views/admin/SaleReturnsView.vue'), meta: { title: '销售退货' } },
      { path: 'orders/purchase-return', name: 'PurchaseReturns', component: () => import('@/views/admin/PurchaseReturnsView.vue'), meta: { title: '采购退货' } },
      { path: 'orders/stock-in', name: 'StockIn', component: () => import('@/views/admin/StockInView.vue'), meta: { title: '入库管理' } },
      { path: 'orders/stock-out', name: 'StockOut', component: () => import('@/views/admin/StockOutView.vue'), meta: { title: '出库管理' } },
      { path: 'orders/finance', name: 'Finance', component: () => import('@/views/admin/FinanceView.vue'), meta: { title: '收付款' } },
      { path: 'orders/after-sale', name: 'AfterSale', component: () => import('@/views/admin/AfterSaleView.vue'), meta: { title: '售后管理' } },
      { path: 'orders/payment-report', name: 'PaymentReport', component: () => import('@/views/admin/PaymentReportView.vue'), meta: { title: '收付款查询' } },
      { path: 'orders/customers', name: 'Customers', component: () => import('@/views/admin/CustomersView.vue'), meta: { title: '客户管理' } },
      { path: 'orders/suppliers', name: 'Suppliers', component: () => import('@/views/admin/SuppliersView.vue'), meta: { title: '供应商' } },
      { path: 'orders/commodities', name: 'Commodities', component: () => import('@/views/admin/CommoditiesView.vue'), meta: { title: '商品库存' } },
      { path: 'orders/product-types', name: 'ProductTypes', component: () => import('@/views/admin/ProductTypeManagementView.vue'), meta: { title: '商品细目' } },
      { path: 'orders/formulas', name: 'FormulaManagement', component: () => import('@/views/admin/FormulaManagementView.vue'), meta: { title: '计算公式' } },
      { path: 'orders/print-settings', name: 'PrintSettings', component: () => import('@/views/admin/PrintSettingsView.vue'), meta: { title: '打印设置' } },
      { path: 'orders/system-settings', name: 'SystemSettings', component: () => import('@/views/admin/SystemSettingsView.vue'), meta: { title: '系统设置' } },
      { path: 'orders/backup', name: 'Backup', component: () => import('@/views/admin/BackupView.vue'), meta: { title: '数据备份', requiresSuperAdmin: true } },
      { path: 'orders/undo-log', name: 'UndoLog', component: () => import('@/views/admin/UndoLogView.vue'), meta: { title: '撤回日志' } },
      { path: 'orders/operation-log', name: 'OperationLog', component: () => import('@/views/admin/OperationLogView.vue'), meta: { title: '操作日志' } },
      { path: 'enquiries', name: 'AdminEnquiries', component: () => import('@/views/admin/EnquiriesView.vue'), meta: { title: '询价管理' } },

      { path: 'about-settings', name: 'AboutSettings', component: () => import('@/views/admin/AboutSettingsView.vue'), meta: { title: '关于我们' } },
      { path: 'contact-settings', name: 'ContactSettings', component: () => import('@/views/admin/ContactSettingsView.vue'), meta: { title: '联系我们' } },
      { path: 'settings', name: 'SiteSettings', component: () => import('@/views/admin/SiteSettingsView.vue'), meta: { title: '站点设置' } },
      { path: ':pathMatch(.*)*', name: 'AdminNotFound', component: () => import('@/views/admin/NotFoundView.vue'), meta: { title: '404 页面未找到' } },
    ]
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 导航守卫 — 动态标题 + 鉴权
let sessionValidated = false
let progressBarTimer = null

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  document.title = to.meta.title || '顺居门业'

  // 显示进度条
  const bar = document.getElementById('route-progress-bar')
  if (bar) {
    bar.style.display = 'block'
    bar.style.width = '0%'
    requestAnimationFrame(() => { bar.style.width = '70%' })
  }

  // 已登录用户访问登录页 → 跳转管理后台
  if (to.path === '/admin/login') {
    if (authStore.isLoggedIn) return next('/admin/dashboard')
    return next()
  }

  if (to.matched.some(r => r.meta.requiresAuth)) {
    const token = sessionStorage.getItem('token')
    if (!token) {
      return next({ path: '/admin/login', query: { redirect: to.fullPath } })
    }
    if (to.matched.some(r => r.meta.requiresSuperAdmin)) {
      const role = sessionStorage.getItem('role')
      if (role !== 'super_admin') {
        return next('/admin/dashboard')
      }
    }
    // 检查 JWT 是否过期（前端只解码 payload，不验证签名）
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      if (payload.exp && Date.now() >= payload.exp * 1000) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('role')
        sessionStorage.removeItem('loginGeneration')
        return next({ path: '/admin/login', query: { redirect: to.fullPath } })
      }
    } catch {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('loginGeneration')
      return next({ path: '/admin/login', query: { redirect: to.fullPath } })
    }

    // 首次进入管理端：向服务端验证 session 是否仍然有效
    if (!sessionValidated) {
      sessionValidated = true
      await authStore.validateSession()
      if (!authStore.isLoggedIn) {
        return next({ path: '/admin/login', query: { redirect: to.fullPath } })
      }
    }
  }
  next()
})

router.afterEach(() => {
  const bar = document.getElementById('route-progress-bar')
  if (bar) {
    bar.style.width = '100%'
    clearTimeout(progressBarTimer)
    progressBarTimer = setTimeout(() => {
      bar.style.display = 'none'
      bar.style.width = '0%'
    }, 300)
  }
})

export default router

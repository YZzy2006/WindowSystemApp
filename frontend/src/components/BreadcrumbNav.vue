<template>
  <nav class="breadcrumb" v-if="items.length > 1">
    <span v-for="(item, i) in items" :key="i" class="crumb">
      <router-link v-if="item.path && i < items.length - 1" :to="item.path" class="crumb-link">{{ item.label }}</router-link>
      <span v-else class="crumb-current">{{ item.label }}</span>
      <span v-if="i < items.length - 1" class="crumb-sep">/</span>
    </span>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const routeMap = {
  '/admin/dashboard': [{ label: '工作台', path: '/admin/dashboard' }, { label: '经营看板' }],
  '/admin/dashboard/customer-rank': [{ label: '工作台', path: '/admin/dashboard' }, { label: '客户排行' }],
  '/admin/dashboard/product-rank': [{ label: '工作台', path: '/admin/dashboard' }, { label: '商品排行' }],
  '/admin/dashboard/receivable-aging': [{ label: '工作台', path: '/admin/dashboard' }, { label: '应收账龄' }],
  '/admin/dashboard/return-analysis': [{ label: '工作台', path: '/admin/dashboard' }, { label: '退货分析' }],
  '/admin/orders': [{ label: '订单系统', path: '/admin/orders' }, { label: '订单总览' }],
  '/admin/orders/sale': [{ label: '订单系统', path: '/admin/orders' }, { label: '销售订单' }],
  '/admin/orders/purchase': [{ label: '订单系统', path: '/admin/orders' }, { label: '采购订单' }],
  '/admin/orders/sale-return': [{ label: '订单系统', path: '/admin/orders' }, { label: '销售退货' }],
  '/admin/orders/purchase-return': [{ label: '订单系统', path: '/admin/orders' }, { label: '采购退货' }],
  '/admin/orders/stock-in': [{ label: '订单系统', path: '/admin/orders' }, { label: '入库管理' }],
  '/admin/orders/stock-out': [{ label: '订单系统', path: '/admin/orders' }, { label: '出库管理' }],
  '/admin/orders/customers': [{ label: '订单系统', path: '/admin/orders' }, { label: '客户管理' }],
  '/admin/orders/after-sale': [{ label: '订单系统', path: '/admin/orders' }, { label: '售后管理' }],
  '/admin/orders/finance': [{ label: '订单系统', path: '/admin/orders' }, { label: '收付款' }],
  '/admin/orders/payment-report': [{ label: '订单系统', path: '/admin/orders' }, { label: '收付款查询' }],
  '/admin/orders/suppliers': [{ label: '订单系统', path: '/admin/orders' }, { label: '供应商' }],
  '/admin/orders/commodities': [{ label: '订单系统', path: '/admin/orders' }, { label: '商品库存' }],
  '/admin/orders/product-types': [{ label: '订单系统', path: '/admin/orders' }, { label: '商品细目' }],
  '/admin/orders/formulas': [{ label: '订单系统', path: '/admin/orders' }, { label: '计算公式' }],
  '/admin/orders/print-settings': [{ label: '订单系统', path: '/admin/orders' }, { label: '打印设置' }],
  '/admin/orders/system-settings': [{ label: '订单系统', path: '/admin/orders' }, { label: '系统设置' }],
  '/admin/orders/undo-log': [{ label: '订单系统', path: '/admin/orders' }, { label: '撤回日志' }],
  '/admin/orders/operation-log': [{ label: '订单系统', path: '/admin/orders' }, { label: '操作日志' }],
  '/admin/orders/backup': [{ label: '订单系统', path: '/admin/orders' }, { label: '数据备份' }],
  '/admin/categories': [{ label: '网站管理' }, { label: '分类管理' }],
  '/admin/products': [{ label: '网站管理' }, { label: '产品管理' }],
  '/admin/cases': [{ label: '网站管理' }, { label: '案例管理' }],
  '/admin/enquiries': [{ label: '网站管理' }, { label: '询价管理' }],
  '/admin/about-settings': [{ label: '网站管理' }, { label: '关于我们' }],
  '/admin/contact-settings': [{ label: '网站管理' }, { label: '联系我们' }],
  '/admin/settings': [{ label: '网站管理' }, { label: '站点设置' }],
  '/admin/accounts': [{ label: '账号管理' }],
  '/admin/password': [{ label: '账号与安全' }],
}

const items = computed(() => {
  const path = route.path
  // 精确匹配
  if (routeMap[path]) return routeMap[path]
  // 前缀匹配（取最长匹配）
  let best = null
  let bestLen = 0
  for (const key of Object.keys(routeMap)) {
    if (path.startsWith(key) && key.length > bestLen) {
      best = routeMap[key]
      bestLen = key.length
    }
  }
  return best || []
})
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 16px;
  font-size: 13px;
}
.crumb {
  display: inline-flex;
  align-items: center;
}
.crumb-link {
  color: #999;
  text-decoration: none;
  transition: color .2s;
}
.crumb-link:hover { color: var(--gold-primary, #C5A265); }
.crumb-current { color: #1a1a1a; font-weight: 500; }
.crumb-sep { margin: 0 8px; color: #ddd; }
</style>

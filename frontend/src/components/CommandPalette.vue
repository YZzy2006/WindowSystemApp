<template>
  <Teleport to="body">
    <div v-if="visible" class="cmd-overlay" @mousedown.self="close">
      <div class="cmd-box" @keydown="onKeydown">
        <div class="cmd-input-wrap">
          <span class="cmd-icon">⌕</span>
          <input
            ref="inputRef"
            v-model="query"
            class="cmd-input"
            placeholder="搜索页面、客户、订单..."
            spellcheck="false"
          />
          <span class="cmd-hint">/ 或 Ctrl+K 唤起 · ESC 关闭</span>
        </div>
        <div class="cmd-results" v-if="filtered.length">
          <div
            v-for="(item, i) in filtered"
            :key="item.path + (item.sub || '') + i"
            class="cmd-item"
            :class="{ active: i === activeIndex }"
            @click="go(item)"
            @mouseenter="activeIndex = i"
          >
            <span class="cmd-item-icon">{{ item.icon }}</span>
            <span class="cmd-item-main">
              <span class="cmd-item-label">{{ item.label }}</span>
              <span v-if="item.sub" class="cmd-item-sub">{{ item.sub }}</span>
            </span>
            <span class="cmd-item-group">{{ item.group }}</span>
          </div>
        </div>
        <div v-else class="cmd-empty">{{ dataSearching ? '搜索中...' : '没有匹配的结果' }}</div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomers, getSaleOrders } from '@/api/admin'

const visible = ref(false)
const query = ref('')
const activeIndex = ref(0)
const inputRef = ref(null)
const router = useRouter()
const dataResults = ref([])
const dataSearching = ref(false)

const commands = [
  // 工作台
  { icon: '📊', label: '经营看板', path: '/admin/dashboard', group: '工作台', keywords: 'dashboard 看板 数据' },
  { icon: '📊', label: '客户排行', path: '/admin/dashboard/customer-rank', group: '工作台', keywords: 'customer rank 排行 销售额' },
  { icon: '📊', label: '商品排行', path: '/admin/dashboard/product-rank', group: '工作台', keywords: 'product rank 排行' },
  { icon: '📊', label: '应收账龄', path: '/admin/dashboard/receivable-aging', group: '工作台', keywords: 'aging 应收 账龄 欠款' },
  { icon: '📊', label: '退货分析', path: '/admin/dashboard/return-analysis', group: '工作台', keywords: 'return 退货 分析' },
  // 网站管理
  { icon: '🌐', label: '分类管理', path: '/admin/categories', group: '网站管理', keywords: 'category 分类' },
  { icon: '🌐', label: '产品管理', path: '/admin/products', group: '网站管理', keywords: 'product 产品' },
  { icon: '🌐', label: '案例管理', path: '/admin/cases', group: '网站管理', keywords: 'case 案例' },
  { icon: '🌐', label: '询价管理', path: '/admin/enquiries', group: '网站管理', keywords: 'enquiry 询价' },
  { icon: '🌐', label: '关于我们', path: '/admin/about-settings', group: '网站管理', keywords: 'about 关于' },
  { icon: '🌐', label: '联系我们', path: '/admin/contact-settings', group: '网站管理', keywords: 'contact 联系' },
  { icon: '🌐', label: '站点设置', path: '/admin/settings', group: '网站管理', keywords: 'settings 站点 配置' },
  // 订单系统
  { icon: '📦', label: '订单总览', path: '/admin/orders', group: '订单系统', keywords: 'order 订单 总览' },
  { icon: '📦', label: '销售订单', path: '/admin/orders/sale', group: '订单系统', keywords: 'sale 销售 单' },
  { icon: '📦', label: '采购订单', path: '/admin/orders/purchase', group: '订单系统', keywords: 'purchase 采购 单' },
  { icon: '📦', label: '销售退货', path: '/admin/orders/sale-return', group: '订单系统', keywords: 'sale return 退货' },
  { icon: '📦', label: '采购退货', path: '/admin/orders/purchase-return', group: '订单系统', keywords: 'purchase return 退货' },
  { icon: '📦', label: '入库管理', path: '/admin/orders/stock-in', group: '仓储', keywords: 'stock in 入库' },
  { icon: '📦', label: '出库管理', path: '/admin/orders/stock-out', group: '仓储', keywords: 'stock out 出库' },
  { icon: '📦', label: '客户管理', path: '/admin/orders/customers', group: '仓储', keywords: 'customer 客户' },
  { icon: '📦', label: '售后管理', path: '/admin/orders/after-sale', group: '仓储', keywords: 'after sale 售后' },
  { icon: '💰', label: '收付款', path: '/admin/orders/finance', group: '财务', keywords: 'finance 收款 付款' },
  { icon: '💰', label: '收付款查询', path: '/admin/orders/payment-report', group: '财务', keywords: 'payment report 查询' },
  { icon: '📦', label: '供应商', path: '/admin/orders/suppliers', group: '基础数据', keywords: 'supplier 供应商' },
  { icon: '📦', label: '商品库存', path: '/admin/orders/commodities', group: '基础数据', keywords: 'commodity 商品 库存' },
  { icon: '📦', label: '商品细目', path: '/admin/orders/product-types', group: '基础数据', keywords: 'product type 细目' },
  { icon: '📦', label: '计算公式', path: '/admin/orders/formulas', group: '基础数据', keywords: 'formula 公式' },
  { icon: '⚙', label: '打印设置', path: '/admin/orders/print-settings', group: '系统', keywords: 'print 打印' },
  { icon: '⚙', label: '系统设置', path: '/admin/orders/system-settings', group: '系统', keywords: 'system settings 系统' },
  { icon: '⚙', label: '撤回日志', path: '/admin/orders/undo-log', group: '系统', keywords: 'undo log 撤回' },
  { icon: '⚙', label: '操作日志', path: '/admin/orders/operation-log', group: '系统', keywords: 'operation log 操作' },
  { icon: '⚙', label: '数据备份', path: '/admin/orders/backup', group: '系统', keywords: 'backup 备份' },
  { icon: '🛡', label: '账号与安全', path: '/admin/password', group: '账号', keywords: 'password 安全 密码' },
  // 快捷操作
  { icon: '➕', label: '新建销售单', path: '/admin/orders/sale?create=1', group: '快捷操作', keywords: 'new sale 新建 销售' },
]

const navResults = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return commands
  return commands.filter(c =>
    c.label.toLowerCase().includes(q) ||
    c.group.toLowerCase().includes(q) ||
    c.keywords.toLowerCase().includes(q)
  )
})

const filtered = computed(() => {
  return [...navResults.value, ...dataResults.value]
})

watch(query, () => { activeIndex.value = 0; debouncedDataSearch() })

let _searchTimer = null
function debouncedDataSearch() {
  clearTimeout(_searchTimer)
  const q = query.value.trim()
  if (q.length < 2) {
    dataResults.value = []
    return
  }
  _searchTimer = setTimeout(() => searchData(q), 350)
}

async function searchData(q) {
  dataSearching.value = true
  try {
    const [custRes, orderRes] = await Promise.allSettled([
      getCustomers({ keyword: q, page: 1, size: 5 }),
      getSaleOrders({ keyword: q, page: 1, size: 5 }),
    ])
    const results = []
    if (custRes.status === 'fulfilled') {
      const records = custRes.value?.data?.data?.records || custRes.value?.data?.records || []
      records.forEach(r => {
        results.push({
          icon: '👤',
          label: r.name,
          group: '客户',
          path: '/admin/orders/customers',
          isData: true,
          sub: r.contact ? `${r.contact} ${r.phone || ''}`.trim() : (r.phone || ''),
        })
      })
    }
    if (orderRes.status === 'fulfilled') {
      const records = orderRes.value?.data?.data?.records || orderRes.value?.data?.records || []
      records.forEach(r => {
        results.push({
          icon: '📋',
          label: r.orderNo || `#${r.id}`,
          group: '销售单',
          path: `/admin/orders/sale?detailId=${r.id}`,
          isData: true,
          sub: r.customerName ? `${r.customerName} ¥${r.totalAmount || 0}` : '',
        })
      })
    }
    dataResults.value = results
  } catch {
    dataResults.value = []
  } finally {
    dataSearching.value = false
  }
}

watch(visible, (val) => {
  if (val) {
    query.value = ''
    activeIndex.value = 0
    dataResults.value = []
    nextTick(() => inputRef.value?.focus())
  }
})

function open() { visible.value = true }
function close() { visible.value = false }

function go(item) {
  router.push(item.path)
  close()
}

function onKeydown(e) {
  const len = filtered.value.length
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % len
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + len) % len
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (filtered.value[activeIndex.value]) go(filtered.value[activeIndex.value])
  } else if (e.key === 'Escape') {
    close()
  }
}

function globalKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    visible.value ? close() : open()
    return
  }
  // / 键打开命令面板（不在输入框中时）
  if (e.key === '/' && !e.ctrlKey && !e.metaKey && !e.altKey) {
    const tag = document.activeElement?.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement?.isContentEditable) return
    e.preventDefault()
    open()
  }
}

onMounted(() => document.addEventListener('keydown', globalKeydown))
onUnmounted(() => document.removeEventListener('keydown', globalKeydown))
</script>

<style scoped>
.cmd-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,.45);
  display: flex; justify-content: center; padding-top: 15vh;
}
.cmd-box {
  width: 560px; max-height: 480px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,.3);
  display: flex; flex-direction: column; overflow: hidden;
}
.cmd-input-wrap {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 20px; border-bottom: 1px solid #f0f0f0;
}
.cmd-icon { font-size: 18px; color: #999; }
.cmd-input {
  flex: 1; border: none; outline: none; font-size: 16px;
  color: #333; background: transparent; font-family: inherit;
}
.cmd-input::placeholder { color: #bbb; }
.cmd-hint { font-size: 11px; color: #ccc; }
.cmd-results {
  flex: 1; overflow-y: auto; padding: 8px;
}
.cmd-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px;
  cursor: pointer; transition: background .1s;
}
.cmd-item:hover, .cmd-item.active {
  background: #f5f5f5;
}
.cmd-item-icon { font-size: 16px; width: 24px; text-align: center; flex-shrink: 0; }
.cmd-item-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.cmd-item-label { font-size: 14px; color: #333; }
.cmd-item-sub { font-size: 12px; color: #999; margin-top: 1px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cmd-item-group { font-size: 12px; color: #aaa; flex-shrink: 0; }
.cmd-empty {
  padding: 32px; text-align: center; color: #ccc; font-size: 14px;
}
</style>

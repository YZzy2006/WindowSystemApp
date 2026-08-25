<template>
  <div class="print-order" ref="printRef" :style="{ fontSize: (printConfig?.fontSize || 11) + 'px', maxWidth: (printConfig?.pageWidthMm || 210) + 'mm', ...lineWidthVars(printConfig) }">
    <!-- Header -->
    <div class="print-header" :class="secClass('header')" :style="sectionBold('header')" @click="secClick('header')">
      <div class="print-title" :style="sectionFontSize('header', 20)">{{ title }}</div>
      <div class="print-company" :style="sectionFontSize('header', 12)">{{ printConfig?.companyName || '顺居门业' }}</div>
    </div>

    <!-- Order Info Grid -->
    <div class="print-info" :class="secClass('info')" :style="sectionBold('info')" @click="secClick('info')">
      <div class="info-grid" :style="infoAlign()">
        <div class="info-item">
          <span class="info-label" :style="sectionFontSize('info', 10)">订单编号</span>
          <span class="info-value mono" :style="sectionFontSize('info', 10)">{{ order.orderNo || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label" :style="sectionFontSize('info', 10)">开单日期</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ formatDate(order.orderDate || order.returnDate) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label" :style="sectionFontSize('info', 10)">{{ order.customerName ? '客户' : '供应商' }}</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ order.customerName || order.supplierName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label" :style="sectionFontSize('info', 10)">联系电话</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ order.customerPhone || order.supplierPhone || '-' }}</span>
        </div>
        <div class="info-item full">
          <span class="info-label" :style="sectionFontSize('info', 10)">{{ order.customerName ? '安装地址' : '收货地址' }}</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ order.customerAddress || order.supplierAddress || '-' }}</span>
        </div>
      </div>
      <div class="info-grid info-right" :style="infoAlign()">
        <div class="info-item">
          <span class="info-label" :style="sectionFontSize('info', 10)">制单人</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ printConfig?.creatorName || order.creatorName || '冯伟明' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label" :style="sectionFontSize('info', 10)">联系电话</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ printConfig?.creatorPhone || order.creatorPhone || '18948846839' }}</span>
        </div>
        <div class="info-item full">
          <span class="info-label" :style="sectionFontSize('info', 10)">公司地址</span>
          <span class="info-value" :style="sectionFontSize('info', 11)">{{ printConfig?.creatorAddress || order.creatorAddress || '乳源县二九一物流仓库' }}</span>
        </div>
      </div>
    </div>

    <!-- Items Table -->
    <div class="print-table-title" :class="secClass('tableHead')" :style="[sectionBold('tableHead'), sectionFontSize('tableHead', 11)]" @click="secClick('tableHead')">商品明细</div>
    <table class="print-table">
      <thead :class="secClass('tableHead')" :style="sectionBold('tableHead')" @click="secClick('tableHead')">
        <tr>
          <th rowspan="2" class="col-no" :style="[sectionFontSize('tableHead', 9), { width: '36px' }]">序号</th>
          <th rowspan="2" class="col-name" :style="[sectionFontSize('tableHead', 9), { width: colWidth('productName') + 'px' }]">商品名称<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'productName')"></div></th>
          <th v-if="showColor" rowspan="2" class="col-color" :style="[sectionFontSize('tableHead', 9), { width: colWidth('color') + 'px' }]">颜色<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'color')"></div></th>
          <th v-if="showSizeGroup" :colspan="sizeGroupColspan" class="col-size-group" :style="sectionFontSize('tableHead', 9)">尺寸 <span class="unit-hint">mm</span></th>
          <th v-if="showGlassType" rowspan="2" class="col-glassType" :style="[sectionFontSize('tableHead', 9), { width: colWidth('glassType') + 'px' }]">玻璃款式<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'glassType')"></div></th>
          <th v-if="showLock" rowspan="2" class="col-lock" :style="[sectionFontSize('tableHead', 9), { width: colWidth('lockPosition') + 'px' }]">锁位<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'lockPosition')"></div></th>
          <th v-if="showDiaojiao" rowspan="2" class="col-diaojiao" :style="[sectionFontSize('tableHead', 9), { width: colWidth('diaojiao') + 'px' }]">吊脚<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'diaojiao')"></div></th>
          <th v-if="showDoorCount" rowspan="2" class="col-doors" :style="[sectionFontSize('tableHead', 9), { width: colWidth('doorCount') + 'px' }]">樘数<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'doorCount')"></div></th>
          <th v-if="showFangCount" rowspan="2" class="col-fang" :style="[sectionFontSize('tableHead', 9), { width: colWidth('fangCount') + 'px' }]">面积(㎡)<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'fangCount')"></div></th>
          <th v-if="showProductType" rowspan="2" class="col-type" :style="[sectionFontSize('tableHead', 9), { width: colWidth('productType') + 'px' }]">细目<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'productType')"></div></th>
          <th rowspan="2" class="col-num" :style="[sectionFontSize('tableHead', 9), { width: colWidth('unitPrice') + 'px' }]">单价<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'unitPrice')"></div></th>
          <th rowspan="2" class="col-num-amount" :style="[sectionFontSize('tableHead', 9), { width: colWidth('amount') + 'px' }]">金额<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'amount')"></div></th>
          <th v-if="hasRemark" rowspan="2" class="col-remark" :style="[sectionFontSize('tableHead', 9), { width: colWidth('remark') + 'px' }]">备注<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'remark')"></div></th>
        </tr>
        <tr v-if="showSizeGroup">
          <th v-if="showSize" class="col-size" :style="[sectionFontSize('tableHead', 9), { width: colWidth('width') + 'px' }]">宽<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'width')"></div></th>
          <th v-if="showSize" class="col-size" :style="[sectionFontSize('tableHead', 9), { width: colWidth('height') + 'px' }]">高<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'height')"></div></th>
          <th v-if="showWallThickness" class="col-size" :style="[sectionFontSize('tableHead', 9), { width: colWidth('wallThickness') + 'px' }]">墙厚<div class="col-resize-handle" @mousedown.prevent="startResize($event, 'wallThickness')"></div></th>
        </tr>
      </thead>
      <tbody :class="secClass('tableBody')" :style="[sectionBold('tableBody'), sectionFontSize('tableBody', 10)]" @click="secClick('tableBody')">
        <tr v-for="(item, idx) in items" :key="idx">
          <td class="col-no">{{ idx + 1 }}</td>
          <td class="col-name">{{ item.productName || '' }}</td>
          <td v-if="showColor">{{ item.color || '' }}</td>
          <td v-if="showSize">{{ formatSize(item, 'width') }}</td>
          <td v-if="showSize">{{ formatSize(item, 'height') }}</td>
          <td v-if="showWallThickness">{{ item.wallThickness || '' }}</td>
          <td v-if="showGlassType" class="col-glassType">{{ item.glassType || '' }}</td>
          <td v-if="showLock" class="col-lock">{{ item.lockPosition || '' }}</td>
          <td v-if="showDiaojiao" class="col-diaojiao">{{ item.diaojiao > 0 ? item.diaojiao : '' }}</td>
          <td v-if="showDoorCount" class="col-doors">{{ item.doorPending ? '待定' : (item.doorCount > 0 ? item.doorCount : '') }}</td>
          <td v-if="showFangCount" class="col-fang">{{ item.fangPending ? '待定' : (item.fangCount > 0 ? item.fangCount : '') }}</td>
          <td v-if="showProductType">{{ item.productType || '' }}</td>
          <td class="col-num">{{ formatMoney(item.unitPrice) }}</td>
          <td class="col-num-amount">{{ formatMoney(item.amount ?? (item.quantity * item.unitPrice)) }}</td>
          <td v-if="hasRemark" class="col-remark" :style="sectionFontSize('tableBody', 9)">{{ item.remark || '' }}</td>
        </tr>
      </tbody>
      <tfoot>
        <template v-if="configShowTotalRow">
          <tr v-for="(row, ri) in summaryRows" :key="ri" class="type-summary-row">
            <td :colspan="totalColspan" class="type-summary-cell" :style="sectionFontSize('tableBody', 10)">
              <div class="type-summary-grid">
                <div class="type-summary-item" v-for="s in row" :key="s.productType">
                  {{ s.productType }}：<template v-if="s.doorCount > 0">{{ s.doorCount }} {{ parseSummaryUnits(s.productType)[0] }}</template><template v-if="s.doorCount > 0 && s.fangCount > 0"> / </template><template v-if="s.fangCount > 0">{{ s.fangCount }} {{ parseSummaryUnits(s.productType)[1] }}</template>
                </div>
              </div>
            </td>
          </tr>
        </template>
        <tr v-if="order.remark">
          <td :colspan="totalColspan" class="remark-row" :style="sectionFontSize('tableBody', 10)">备注：{{ order.remark }}</td>
        </tr>
      </tfoot>
    </table>

    <!-- Summary -->
    <div class="print-summary" v-if="configShowSummary" :class="secClass('summary')" :style="sectionBold('summary')" @click="secClick('summary')">
      <div class="summary-grid" v-if="!isBudget">
        <div class="summary-group">
          <div class="summary-item" :style="sectionFontSize('summary', 11)">
            <span class="summary-label" :style="sectionFontSize('summary', 12)">合计金额</span>
            <span class="summary-value strong" :style="sectionFontSize('summary', 18)">¥{{ formatMoney(order.totalAmount) }}</span>
          </div>
          <div class="summary-item" :style="sectionFontSize('summary', 11)">
            <span class="summary-label" :style="sectionFontSize('summary', 12)">大写</span>
            <span class="summary-value chinese" :style="sectionFontSize('summary', 12)">{{ amountInChinese }}</span>
          </div>
        </div>
        <div class="summary-group">
          <div class="summary-item" :style="sectionFontSize('summary', 11)">
            <span class="summary-label" :style="sectionFontSize('summary', 12)">{{ paidLabel }}</span>
            <span class="summary-value" :style="sectionFontSize('summary', 16)">¥{{ formatMoney(order.deposit || order.paidAmount) }}</span>
          </div>
          <div class="summary-item" :style="sectionFontSize('summary', 11)">
            <span class="summary-label" :style="sectionFontSize('summary', 12)">大写</span>
            <span class="summary-value chinese" :style="sectionFontSize('summary', 12)">{{ depositInChinese }}</span>
          </div>
        </div>
        <div class="summary-group">
          <div class="summary-item" :style="sectionFontSize('summary', 11)">
            <span class="summary-label" :style="sectionFontSize('summary', 12)">{{ unpaidLabel }}</span>
            <span class="summary-value strong red" :style="sectionFontSize('summary', 18)">¥{{ formatMoney(unpaid) }}</span>
          </div>
          <div class="summary-item" :style="sectionFontSize('summary', 11)">
            <span class="summary-label" :style="sectionFontSize('summary', 12)">大写</span>
            <span class="summary-value chinese" :style="sectionFontSize('summary', 12)">{{ unpaidInChinese }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Payment Terms -->
    <div class="print-terms" v-if="configShowPaymentTerms && (order.notice || paymentTerms)" :class="secClass('terms')" :style="sectionBold('terms')" @click="secClick('terms')">
      <div class="terms-title" :style="sectionFontSize('terms', 10)">付款条款</div>
      <div class="terms-body" :style="sectionFontSize('terms', 9)">{{ order.notice || paymentTerms }}</div>
    </div>

    <!-- Installation Terms -->
    <div class="print-terms" v-if="configShowInstallationTerms && installationTerms" :class="secClass('terms')" :style="sectionBold('terms')" @click="secClick('terms')">
      <div class="terms-title" :style="sectionFontSize('terms', 10)">安装条款</div>
      <div class="terms-body" :style="sectionFontSize('terms', 9)" style="white-space: pre-line;">{{ installationTerms }}</div>
    </div>

    <!-- Warranty Terms -->
    <div class="print-terms" v-if="configShowWarrantyTerms && warrantyTerms" :class="secClass('terms')" :style="sectionBold('terms')" @click="secClick('terms')">
      <div class="terms-title" :style="sectionFontSize('terms', 10)">保修条款</div>
      <div class="terms-body" :style="sectionFontSize('terms', 9)" style="white-space: pre-line;">{{ warrantyTerms }}</div>
    </div>

    <!-- Footer -->
    <div class="print-footer" v-if="configShowFooter" :class="secClass('footer')" :style="sectionBold('footer')" @click="secClick('footer')">
      <div class="footer-row" :style="sectionFontSize('footer', 11)">
        <div class="sign-item">客户签名：______________</div>
        <div class="sign-item">制单人：______________</div>
        <div class="sign-item">审核人：______________</div>
      </div>
      <div class="footer-date" v-if="printConfig?.footerText" :style="sectionFontSize('footer', 9)">{{ printConfig.footerText }}</div>
      <div class="footer-date" :style="sectionFontSize('footer', 9)">打印日期：{{ printDate }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { getSystemSettings } from '@/api/admin'

const props = defineProps({
  title: { type: String, default: '顺居门窗客户销售单' },
  order: { type: Object, default: () => ({}) },
  items: { type: Array, default: () => [] },
  type: { type: String, default: 'sale' },
  hiddenTypes: { type: Object, default: () => new Set() },
  productTypeMap: { type: Object, default: () => ({}) },
  printConfig: { type: Object, default: null },
  formulaMap: { type: Object, default: () => ({}) },
  // 预览可调节模式：区块可点击选中（配合父级浮动调节条）
  selectable: { type: Boolean, default: false },
  selectedSection: { type: String, default: '' },
})
const emit = defineEmits(['section-click'])

// 分区线条粗细 → CSS 变量（与字体样式分区一致：header/info/table/summary/terms/footer）
function lineWidthVars(cfg) {
  const lw = cfg?.lineWidths || {}
  const get = (k) => (lw[k] ?? 2) + 'px'
  return {
    '--lw-header': get('header'),
    '--lw-info': get('info'),
    '--lw-table': get('table'),
    '--lw-summary': get('summary'),
    '--lw-terms': get('terms'),
    '--lw-footer': get('footer'),
  }
}

function secClass(key) {
  return {
    'section-clickable': props.selectable,
    'section-selected': props.selectable && props.selectedSection === key,
  }
}
function secClick(key) {
  if (props.selectable) emit('section-click', key)
}

// 检查商品是否使用包边计价公式
function isBaoBianFormula(item) {
  // 优先按 formulaId 查找
  if (item.formulaId) {
    const formula = Object.values(props.formulaMap).find(f => f.id === item.formulaId)
    if (formula?.name?.includes('包边')) return true
  }
  // 兜底：按商品名称判断
  return item.productName?.includes('包边') || false
}

// 格式化宽高：包边计价显示为米，其他显示原值
function formatSize(item, field) {
  const val = item[field]
  if (val == null || val === '') return '-'
  if (isBaoBianFormula(item)) return +(val / 1000).toFixed(2)
  return formatNum(val)
}

const isBudget = computed(() => props.type === 'presale')

// 解析 summaryDesc（如 "樘/方"）返回 [doorCount单位, fangCount单位]
function parseSummaryUnits(productType) {
  const desc = props.productTypeMap?.[productType] || ''
  if (desc && desc.includes('/')) {
    const parts = desc.split('/')
    return [parts[0]?.trim() || '樘', parts[1]?.trim() || '㎡']
  }
  return ['樘', '㎡']
}

const printRef = ref(null)

const paymentTerms = computed(() => props.printConfig?.paymentTermsText || '客户订货需支付总金额百分之30，安装完成后需付尾款百分之70，剩余货款需在一个月内付清。')

function colVisible(key, autoDetect) {
  if (props.printConfig) {
    const col = props.printConfig.columns?.find(c => c.key === key)
    if (col) return col.visible
  }
  return autoDetect
}

const showColor = computed(() => colVisible('color', props.items.some(i => i.color)))
const showSize = computed(() => colVisible('width', props.items.some(i => i.width || i.height)))
const showWallThickness = computed(() => colVisible('wallThickness', props.items.some(i => i.wallThickness)))
const showGlassType = computed(() => colVisible('glassType', props.items.some(i => i.glassType)))
const showSizeGroup = computed(() => showSize.value || showWallThickness.value)
const sizeGroupColspan = computed(() => {
  let n = 0
  if (showSize.value) n += 2 // 宽 + 高
  if (showWallThickness.value) n++
  return n
})
const showLock = computed(() => colVisible('lockPosition', props.items.some(i => i.lockPosition)))
const showDiaojiao = computed(() => colVisible('diaojiao', props.items.some(i => i.diaojiao != null && i.diaojiao > 0)))
const showDoorCount = computed(() => colVisible('doorCount', props.items.some(i => i.doorCount != null && i.doorCount > 0)))
const showFangCount = computed(() => colVisible('fangCount', props.items.some(i => i.fangCount != null && i.fangCount > 0)))
const showProductType = computed(() => colVisible('productType', props.items.some(i => i.productType)))
const hasRemark = computed(() => colVisible('remark', props.items.some(i => i.remark)))

const configShowSummary = computed(() => props.printConfig ? props.printConfig.showSummary !== false : true)
const configShowTotalRow = computed(() => props.printConfig ? props.printConfig.showTotalRow !== false : true)
const configShowPaymentTerms = computed(() => props.printConfig ? props.printConfig.showPaymentTerms !== false : true)
const configShowInstallationTerms = computed(() => props.printConfig ? props.printConfig.showInstallationTerms === true : false)
const configShowWarrantyTerms = computed(() => props.printConfig ? props.printConfig.showWarrantyTerms === true : false)
const configShowFooter = computed(() => props.printConfig ? props.printConfig.showFooter !== false : true)

// Load terms from sys_config as fallback
const sysConfigTerms = ref({ installation: '', warranty: '' })
onMounted(async () => {
  try {
    const res = await getSystemSettings()
    const data = res.data?.data ?? res.data ?? {}
    sysConfigTerms.value.installation = data.installation_terms || ''
    sysConfigTerms.value.warranty = data.warranty_terms || ''
  } catch { /* ignore */ }
})

const installationTerms = computed(() => props.printConfig?.installationTermsText || sysConfigTerms.value.installation)
const warrantyTerms = computed(() => props.printConfig?.warrantyTermsText || sysConfigTerms.value.warranty)

// Column width helper
function colWidth(key) {
  return props.printConfig?.columnWidths?.[key] ?? DEFAULT_COL_WIDTHS[key] ?? 60
}
const DEFAULT_COL_WIDTHS = {
  productName: 100, color: 50,
  width: 40, height: 40, wallThickness: 40, glassType: 50,
  lockPosition: 36, diaojiao: 40, doorCount: 40, fangCount: 48,
  productType: 60, unitPrice: 56, amount: 60, remark: 70,
}

// Column resize drag logic
let resizeState = null
function startResize(e, colKey) {
  resizeState = { colKey, startX: e.clientX, startWidth: colWidth(colKey) }
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
function onResize(e) {
  if (!resizeState || !props.printConfig) return
  if (!props.printConfig.columnWidths) {
    props.printConfig.columnWidths = { ...DEFAULT_COL_WIDTHS }
  }
  const delta = e.clientX - resizeState.startX
  const newWidth = Math.max(20, resizeState.startWidth + delta)
  props.printConfig.columnWidths[resizeState.colKey] = newWidth
}
function stopResize() {
  resizeState = null
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}
onBeforeUnmount(stopResize)

// Per-section font style: bold applied to container (inherited), font-size to children
function sectionBold(key) {
  const s = props.printConfig?.sectionStyles?.[key]
  return s?.bold ? { fontWeight: 'bold' } : {}
}
function sectionFontSize(key, basePx) {
  const s = props.printConfig?.sectionStyles?.[key]
  const offset = s?.sizeOffset || 0
  return { fontSize: (basePx + offset) + 'px' }
}
function infoAlign() {
  const align = props.printConfig?.sectionStyles?.info?.align || 'left'
  return { textAlign: align }
}

const totalColspan = computed(() => {
  let cols = 2 // 序号 + 商品名称
  if (showColor.value) cols++
  if (showSize.value) cols += 2 // 宽 + 高
  if (showWallThickness.value) cols++
  if (showGlassType.value) cols++
  if (showLock.value) cols++
  if (showDiaojiao.value) cols++
  if (showDoorCount.value) cols++
  if (showFangCount.value) cols++
  if (showProductType.value) cols++
  cols++ // 单价
  cols++ // 金额
  if (hasRemark.value) cols++ // 备注
  return cols
})

const totalWindowArea = computed(() => props.order?.windowArea || 0)
const totalDoorCount = computed(() => props.order?.doorCount || props.order?.windowCount || 0)

const productTypeSummary = computed(() => {
  if (!props.items || !props.items.length) return []
  const map = {}
  for (const item of props.items) {
    const type = item.productType || ''
    if (!map[type]) map[type] = { productType: type, doorCount: 0, fangCount: 0 }
    const dc = item.doorCount > 0 ? Number(item.doorCount) : (item.unit === '套' && item.quantity > 0 ? Number(item.quantity) : 0)
    if (dc > 0) map[type].doorCount += dc
    const fc = item.fangCount > 0 ? Number(item.fangCount) : ((item.unit === '方' || item.unit === 'sqm') && item.quantity > 0 ? Number(item.quantity) : 0)
    if (fc > 0) map[type].fangCount += fc
  }
  const hidden = props.hiddenTypes instanceof Set ? props.hiddenTypes : new Set()
  const list = Object.values(map).filter(s => s.productType && (s.doorCount > 0 || s.fangCount > 0) && !hidden.has(s.productType))
  return list
})

const summaryRows = computed(() => {
  const items = productTypeSummary.value
  if (!items.length) return []
  const rows = []
  for (let i = 0; i < items.length; i += 8) {
    rows.push(items.slice(i, i + 8))
  }
  return rows
})

const paidLabel = computed(() => {
  if (props.type === 'purchase') return '已付金额'
  if (props.type === 'return') return '已退金额'
  return '已收定金'
})
const unpaidLabel = computed(() => {
  if (props.type === 'purchase') return '未付余额'
  if (props.type === 'return') return '未退余额'
  return '未收余款'
})

const unpaid = computed(() => {
  const total = Number(props.order?.totalAmount) || 0
  const paid = Number(props.order?.deposit || props.order?.paidAmount) || 0
  return Math.max(0, total - paid)
})

const totalArea = computed(() => {
  const sum = props.items.reduce((s, item) => {
    if (item.width && item.height) {
      const qty = item.doorCount || item.quantity || 1
      return s + (item.width / 1000) * (item.height / 1000) * qty
    }
    return s
  }, 0)
  return sum > 0 ? sum.toFixed(2) : ''
})

const printDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
})

const CN_NUMS = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
const CN_UNITS = ['', '拾', '佰', '仟']
const CN_BIG_UNITS = ['', '万', '亿']

function amountToChinese(val) {
  if (val == null || val === '' || val === 0) return '零元整'
  const num = Number(val)
  if (isNaN(num)) return '零元整'
  const intPart = Math.floor(Math.abs(num))
  const decPart = Math.round((Math.abs(num) - intPart) * 100)
  const jiao = Math.floor(decPart / 10)
  const fen = decPart % 10
  let result = ''
  if (intPart === 0) {
    result = '零'
  } else {
    const str = String(intPart)
    const groups = []
    for (let i = str.length; i > 0; i -= 4) {
      groups.unshift(str.substring(Math.max(0, i - 4), i))
    }
    for (let g = 0; g < groups.length; g++) {
      const group = groups[g]
      let groupStr = ''
      let zeroFlag = false
      for (let i = 0; i < group.length; i++) {
        const digit = parseInt(group[i])
        const unitIdx = group.length - 1 - i
        if (digit === 0) {
          zeroFlag = true
        } else {
          if (zeroFlag) { groupStr += '零'; zeroFlag = false }
          groupStr += CN_NUMS[digit] + CN_UNITS[unitIdx]
        }
      }
      if (groupStr) {
        result += groupStr + CN_BIG_UNITS[groups.length - 1 - g]
      }
    }
  }
  result += '元'
  if (jiao === 0 && fen === 0) {
    result += '整'
  } else {
    if (jiao > 0) result += CN_NUMS[jiao] + '角'
    else if (fen > 0) result += '零'
    if (fen > 0) result += CN_NUMS[fen] + '分'
  }
  return (num < 0 ? '负' : '') + result
}

const amountInChinese = computed(() => amountToChinese(props.order?.totalAmount))
const depositInChinese = computed(() => amountToChinese(props.order?.deposit || props.order?.paidAmount))
const unpaidInChinese = computed(() => amountToChinese(unpaid.value))

function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

function formatMoney(v) {
  if (v == null || v === '') return '0.00'
  return Number(v).toFixed(2)
}

function formatNum(v) {
  if (v == null || v === '') return ''
  const n = Number(v)
  if (isNaN(n)) return ''
  return n % 1 === 0 ? String(Math.round(n)) : String(n)
}

// Expose doPrint for parent components to call
function doPrint() {
  if (!printRef.value) return
  const html = printRef.value.outerHTML
  const cfg = props.printConfig
  const orient = cfg?.orientation || 'portrait'
  const mt = cfg?.marginTop ?? 10
  const mb = cfg?.marginBottom ?? 10
  const ml = cfg?.marginLeft ?? 8
  const mr = cfg?.marginRight ?? 8
  const fs = cfg?.fontSize || 11
  const ss = cfg?.sectionStyles || {}
  // Helper: compute section font
  const sf = (key, basePx) => {
    const s = ss[key]
    if (!s) return { size: basePx, weight: null }
    return { size: basePx + (s.sizeOffset || 0), weight: s.bold ? 'bold' : null }
  }
  const sHeader = sf('header', 20)
  const sInfo = sf('info', 11)
  const sTableHead = sf('tableHead', 9)
  const sTableBody = sf('tableBody', 10)
  const sSummary = sf('summary', 11)
  const sTerms = sf('terms', 10)
  const sFooter = sf('footer', 11)
  // Dot-matrix optimizations:
  // - All text #000 for carbon copy readability
  // - No background colors (waste ribbon, don't transfer to copies)
  // - Borders #000 with 2-3px thickness
  // - SimHei/SimSun fonts designed for print (not screen-optimized Microsoft YaHei)
  // - Exception: title is a solid black rectangle with white text (反白) — a deliberate
  //   order-form design; on carbon copies it transfers as an inverted gray block.
  const pageW = cfg?.pageWidthMm || 210 // 纸张宽度(mm)，A4=210；复写纸按实际
  const pageH = cfg?.pageHeightMm || 297 // 页面高度(mm)，A4=297；复写纸按实际
  // 横向 A4 保持标准；纵向/自定义纸：宽×高 都按设置走
  let pageSize
  if (orient === 'landscape') pageSize = 'A4 landscape'
  else pageSize = `${pageW}mm ${pageH}mm`
  const contentWidth = `${Math.max(50, pageW - 16)}mm` // 纸宽减去两侧安全边距
  const css = `
    @page { size: ${pageSize}; margin: ${mt}mm ${mr}mm ${mb}mm ${ml}mm; }
    body {
      font-family: 'SimHei', 'SimSun', 'FangSong', sans-serif;
      color: #000;
      background: #fff;
      font-size: ${fs}px;
      line-height: 1.6;
      margin: 0;
      padding: 0;
    }
    .print-order {
      width: 100%;
      max-width: ${contentWidth};
      margin: 0 auto;
      padding: 0;
    }
    .print-header {
      text-align: center;
      padding-bottom: 8px;
      border-bottom: var(--lw-header) solid #000;
      margin-bottom: 10px;
      ${sHeader.weight ? `font-weight: ${sHeader.weight};` : ''}
    }
    .print-title {
      font-size: ${sf('header', 22).size}px;
      font-weight: 900;
      letter-spacing: 6px;
      display: inline-block;
      background: #000;
      color: #fff;
      padding: 6px 28px;
    }
    .print-company {
      font-size: ${sf('header', 12).size}px;
      color: #000;
      margin-top: 2px;
      letter-spacing: 2px;
    }
    .print-info {
      display: flex;
      gap: 20px;
      margin-bottom: 10px;
      padding-bottom: 8px;
      border-bottom: var(--lw-info) solid #000;
      ${sInfo.weight ? `font-weight: ${sInfo.weight};` : ''}
    }
    .info-grid {
      flex: 1;
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 1px 8px;
      align-items: baseline;
      text-align: ${props.printConfig?.sectionStyles?.info?.align || 'left'};
    }
    .info-item { display: contents; }
    .info-label {
      font-size: ${sf('info', 10).size}px;
      color: #000;
      white-space: nowrap;
    }
    .info-value {
      font-size: ${sf('info', 11).size}px;
      color: #000;
      font-weight: 500;
    }
    .info-value.mono {
      font-family: 'Courier New', monospace;
      font-size: ${sf('info', 10).size}px;
      letter-spacing: 0.5px;
    }
    .print-table-title {
      font-size: ${sf('tableHead', 11).size}px;
      font-weight: 700;
      text-align: center;
      margin-bottom: 4px;
      ${sTableHead.weight ? `font-weight: ${sTableHead.weight};` : ''}
    }
    .print-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 10px;
      font-size: ${sTableBody.size}px;
      ${sTableBody.weight ? `font-weight: ${sTableBody.weight};` : ''}
    }
    .print-table th,
    .print-table td {
      border: var(--lw-table) solid #000;
      padding: 4px 5px;
      text-align: center;
      vertical-align: middle;
    }
    .print-table th {
      font-size: ${sf('tableHead', 9).size}px;
      ${sTableHead.weight ? `font-weight: ${sTableHead.weight};` : 'font-weight: 700;'}
      text-align: center;
      white-space: nowrap;
      padding: 2px 5px;
      line-height: 1.6;
      color: #000;
      background: #fff;
    }
    .print-table tfoot td {
      font-weight: 700;
      background: #fff;
    }
    .print-table tfoot tr:last-child td {
      border-top: var(--lw-table) solid #000;
    }
    .type-summary-cell {
      font-weight: 400;
      font-size: ${sf('tableBody', 10).size}px;
      padding: 2px 5px;
      border-top: none;
      border-bottom: var(--lw-table) solid #000;
      background: #fff;
    }
    .type-summary-grid {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 2px 2em;
    }
    .type-summary-item {
      white-space: nowrap;
    }
    .col-no { width: 36px; text-align: center; }
    .col-name { width: ${(cfg?.columnWidths?.productName) || 100}px; }
    .col-glassType { width: ${(cfg?.columnWidths?.glassType) || 50}px; }
    .col-color { width: ${(cfg?.columnWidths?.color) || 50}px; }
    .col-size { width: ${(cfg?.columnWidths?.width) || 40}px; text-align: center; }
    .col-size-group { text-align: center; }
    .unit-hint { font-size: 9px; font-weight: 400; color: #000; }
    .col-lock { width: ${(cfg?.columnWidths?.lockPosition) || 36}px; text-align: center; }
    .col-doors { width: ${(cfg?.columnWidths?.doorCount) || 40}px; text-align: center; }
    .col-fang { width: ${(cfg?.columnWidths?.fangCount) || 48}px; text-align: center; }
    .col-diaojiao { width: ${(cfg?.columnWidths?.diaojiao) || 40}px; text-align: right; font-variant-numeric: tabular-nums; }
    .col-type { width: ${(cfg?.columnWidths?.productType) || 60}px; }
    .col-unit { width: 32px; text-align: center; }
    .col-num { width: ${(cfg?.columnWidths?.unitPrice) || 56}px; text-align: right; font-variant-numeric: tabular-nums; }
    .col-num-amount { width: ${(cfg?.columnWidths?.amount) || 60}px; text-align: right; font-variant-numeric: tabular-nums; }
    .col-remark { width: ${(cfg?.columnWidths?.remark) || 70}px; font-size: ${sf('tableBody', 9).size}px; color: #000; }
    .col-img { width: 60px; text-align: center; }
    .item-img {
      max-width: 50px;
      max-height: 50px;
      object-fit: contain;
    }
    .total-label { text-align: right; font-size: ${sf('tableBody', 11).size}px; }
    .remark-row { text-align: left !important; font-size: ${sf('tableBody', 10).size}px; font-weight: 400; color: #000; padding: 4px 6px; background: #fff; border-top: none; }
    .total-sub { font-size: ${sf('tableBody', 10).size}px; color: #000; }
    .total-value { font-size: ${sf('tableBody', 12).size}px; font-weight: 700; }
    .print-summary {
      margin-bottom: 10px;
      padding: 6px 8px;
      border: var(--lw-summary) solid #000;
      border-radius: 0;
      background: #fff;
      ${sSummary.weight ? `font-weight: ${sSummary.weight};` : ''}
    }
    .summary-grid {
      display: flex;
      justify-content: center;
      gap: 2em;
    }
    .summary-group {
      display: flex;
      flex-direction: column;
      gap: 1px;
    }
    .summary-item {
      display: flex;
      align-items: baseline;
      gap: 6px;
      font-size: ${sf('summary', 13).size}px;
      line-height: 2;
    }
    .summary-label {
      color: #000;
      white-space: nowrap;
      min-width: 55px;
      font-size: ${sf('summary', 12).size}px;
    }
    .summary-value {
      color: #000;
      font-weight: 600;
      font-size: ${sf('summary', 16).size}px;
    }
    .summary-value.strong { font-weight: 700; font-size: ${sf('summary', 18).size}px; }
    .summary-value.chinese { font-size: ${sf('summary', 12).size}px; color: #000; font-weight: 400; }
    .summary-value.red { color: #000; font-weight: 700; }
    .print-terms {
      margin-bottom: 10px;
      padding: 6px 8px;
      border: var(--lw-terms) solid #000;
      border-radius: 0;
      background: #fff;
      ${sTerms.weight ? `font-weight: ${sTerms.weight};` : ''}
    }
    .terms-title { font-size: ${sf('terms', 10).size}px; font-weight: 700; margin-bottom: 3px; color: #000; }
    .terms-body { font-size: ${sf('terms', 9).size}px; color: #000; line-height: 1.5; white-space: pre-line; }
    .print-footer {
      margin-top: 12px;
      padding-top: 8px;
      border-top: var(--lw-footer) solid #000;
      ${sFooter.weight ? `font-weight: ${sFooter.weight};` : ''}
    }
    .footer-row {
      display: flex;
      justify-content: space-between;
      font-size: ${sf('footer', 11).size}px;
      gap: 12px;
    }
    .sign-item { white-space: nowrap; color: #000; }
    .footer-date {
      text-align: right;
      font-size: ${sf('footer', 9).size}px;
      color: #000;
      margin-top: 8px;
    }
  `

  const safeTitle = (props.title || '').replace(/[&<>"']/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c]))
  const fullHtml = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${safeTitle}</title><style>${css}</style></head><body>${html}</body></html>`

  // Use iframe to avoid popup blocker in Tauri WebView
  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.left = '-9999px'
  iframe.style.top = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = 'none'
  document.body.appendChild(iframe)

  const iframeDoc = iframe.contentDocument || iframe.contentWindow.document
  iframeDoc.open()
  iframeDoc.write(fullHtml)
  iframeDoc.close()

  const cleanup = () => {
    setTimeout(() => {
      if (iframe.parentNode) iframe.parentNode.removeChild(iframe)
    }, 500)
  }

  iframe.onload = () => {
    try {
      iframe.contentWindow.print()
    } catch (e) {
      // Fallback: if iframe print fails, try window.open
      const printWin = window.open('', '_blank')
      if (printWin) {
        printWin.document.write(fullHtml)
        printWin.document.close()
        printWin.onload = () => { printWin.print(); printWin.close() }
      } else {
        alert('打印失败，请检查浏览器打印权限')
      }
    }
    cleanup()
  }
}

defineExpose({ doPrint })
</script>

<style scoped>
/* Screen preview only - print styles are injected via doPrint() */
.print-order {
  /* 与 doPrint 打印样式一致(SimHei/SimSun/FangSong 针式复写优化)；padding 置 0，
     页面外边距由父级容器(= @page margin)提供，保证预览所见即打印所得 */
  font-family: 'SimHei', 'SimSun', 'FangSong', sans-serif;
  color: #000;
  background: #fff;
  width: 100%;
  max-width: 210mm;
  margin: 0 auto;
  padding: 0;
  font-size: inherit;
  line-height: 1.6;
  box-sizing: border-box;
}

.print-header {
  text-align: center;
  padding-bottom: 8px;
  border-bottom: var(--lw-header) solid #000;
  margin-bottom: 10px;
}
.print-title {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 6px;
  display: inline-block;
  background: #000;
  color: #fff;
  padding: 6px 28px;
}
.print-company {
  font-size: 12px;
  color: #000;
  margin-top: 2px;
  letter-spacing: 2px;
}

.print-info {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: var(--lw-info) solid #000;
}
.info-grid {
  flex: 1;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 1px 8px;
  align-items: baseline;
}
.info-item { display: contents; }
.info-remark {
  display: flex;
  gap: 6px;
  align-items: baseline;
  margin-top: 4px;
  width: 30%;
}
.info-remark .info-value {
  white-space: normal;
  word-break: break-all;
}
.info-label {
  font-size: 10px;
  color: #000;
  white-space: nowrap;
}
.info-value {
  font-size: 11px;
  color: #000;
  font-weight: 500;
}
.info-value.mono {
  font-family: 'Courier New', monospace;
  font-size: 10px;
  letter-spacing: 0.5px;
}

.print-table-title {
  font-size: 11px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 4px;
}
.print-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;
  font-size: 10px;
}
.print-table th,
.print-table td {
  border: var(--lw-table) solid #000;
  padding: 4px 5px;
  text-align: center;
  vertical-align: middle;
  overflow: hidden;
}
.print-table th {
  background: #fff;
  font-weight: 700;
  font-size: 9px;
  text-align: center;
  white-space: nowrap;
  position: relative;
  padding: 2px 5px;
  line-height: 1.6;
}

/* Column resize handle */
.col-resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 5px;
  cursor: col-resize;
  z-index: 1;
}
.col-resize-handle:hover {
  background: #409eff;
}
.print-table tfoot td {
  font-weight: 700;
  background: #fff;
  text-align: right;
  font-size: 11px;
}
.print-table tfoot tr:last-child td {
  border-top: var(--lw-table) solid #000;
}
.type-summary-cell {
  font-weight: 400;
  font-size: 10px;
  padding: 2px 5px;
  border-top: none;
  border-bottom: var(--lw-table) solid #000;
  background: #fff;
}
.type-summary-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2px 2em;
}
.type-summary-item {
  white-space: nowrap;
}
.col-no { width: 28px; text-align: center; }
.col-name { min-width: 90px; }
.col-glassType { min-width: 50px; }
.col-color { width: 50px; }
.col-size { width: 36px; text-align: center; }
.col-size-group { text-align: center; }
.unit-hint { font-size: 9px; font-weight: 400; color: #000; }
.col-lock { width: 36px; text-align: center; }
.col-doors { width: 40px; text-align: center; }
.col-fang { width: 48px; text-align: center; }
.col-diaojiao { width: 40px; text-align: right; font-variant-numeric: tabular-nums; }
.col-type { width: 60px; }
.col-unit { width: 36px; text-align: center; }
.col-num { width: 56px; text-align: right; font-variant-numeric: tabular-nums; }
.col-num-amount { width: 60px; text-align: right; font-variant-numeric: tabular-nums; }
.col-remark { width: 70px; font-size: 10px; color: #000; }
.col-img { width: 70px; text-align: center; }
.item-img {
  max-width: 55px;
  max-height: 55px;
  object-fit: contain;
  vertical-align: middle;
}
.total-label { text-align: right; font-size: 11px; }
.remark-row { text-align: left !important; font-size: 10px; font-weight: 400; color: #000; padding: 4px 6px; background: #fff; border-top: none; }
.print-table tfoot tr .remark-row { text-align: left; }
.total-sub { font-size: 10px; color: #000; }
.total-value { font-size: 12px; font-weight: 700; }

.print-summary {
  margin-bottom: 10px;
  padding: 6px 8px;
  border: var(--lw-summary) solid #000;
  border-radius: 0;
  background: #fff;
}
.summary-grid {
  display: flex;
  justify-content: center;
  gap: 2em;
}
.summary-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.summary-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
  line-height: 2;
}
.summary-label {
  color: #000;
  white-space: nowrap;
  min-width: 55px;
  font-size: 12px;
}
.summary-value { color: #000; font-weight: 600; font-size: 16px; }
.summary-value.strong { font-weight: 700; font-size: 18px; }
.summary-value.chinese { font-size: 12px; color: #000; font-weight: 400; }
.summary-value.red { color: #000; font-weight: 700; }

.print-terms {
  margin-bottom: 10px;
  padding: 6px 8px;
  border: var(--lw-terms) solid #000;
  border-radius: 0;
  background: #fff;
}
.terms-title { font-size: 10px; font-weight: 700; margin-bottom: 3px; color: #000; }
.terms-body { font-size: 9px; color: #000; line-height: 1.5; white-space: pre-line; }

.print-footer {
  margin-top: 12px;
  padding-top: 8px;
  border-top: var(--lw-footer) solid #000;
}
.footer-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  gap: 12px;
}
.sign-item { white-space: nowrap; }
.footer-date {
  text-align: right;
  font-size: 9px;
  color: #000;
  margin-top: 8px;
}
</style>

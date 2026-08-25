<template>
  <div class="page admin-page">
    <!-- ===== Header ===== -->
    <div class="page-header">
      <h2>商品库存</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <div class="alert-toggle" :class="{ active: showOnlyAlerts }" @click="showOnlyAlerts = !showOnlyAlerts" title="仅显示库存预警商品">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span>库存预警</span>
          <em v-if="alertCount > 0">{{ alertCount }}</em>
        </div>
        <button class="btn-outline" @click="handleExport">导出</button>
        <button class="btn-outline" @click="importDialogVisible = true">导入</button>
        <button class="btn-primary" @click="openAdd">＋ 新增商品</button>
      </div>
    </div>

    <!-- ===== Toolbar ===== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <select v-model="filterProductType" class="cat-select">
          <option value="">全部细目</option>
          <option v-for="t in productTypes" :key="t" :value="t">{{ t }}</option>
        </select>
        <select v-model="filterUnit" class="cat-select">
          <option value="">全部单位</option>
          <option v-for="u in formulaUnitOptions" :key="u" :value="u">{{ u }}</option>
        </select>
      </div>
      <div class="toolbar-right">
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="keyword" placeholder="搜编码 / 名称..." class="search-input" @input="onSearchInput" />
        </div>
      </div>
    </div>

    <!-- ===== Alert Summary Bar ===== -->
    <div class="alert-bar" v-if="alertList.length && !showOnlyAlerts">
      <span class="alert-bar-label">库存预警</span>
      <span class="alert-bar-item" v-for="a in alertList.slice(0, 6)" :key="a.id" :class="alertLevel(a)">
        {{ a.name }}: {{ a.currentQty }}{{ a.unit || '' }}
      </span>
      <span class="alert-bar-more" v-if="alertList.length > 6">+{{ alertList.length - 6 }} 项</span>
    </div>

    <div class="summary-bar">
      <div class="summary-card"><div class="sc-label">商品总数</div><div class="sc-value">{{ total }} 件</div></div>
    </div>

    <!-- ===== Table ===== -->
    <div class="card">
      <!-- Batch action bar -->
      <div class="batch-bar" v-if="selectedIds.length > 0">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 项</span>
        <button class="btn-batch" @click="batchToggleShow(1)">上架</button>
        <button class="btn-batch" @click="batchToggleShow(0)">下架</button>
        <button class="btn-batch danger" @click="batchDelete">🗑 批量删除</button>
        <button class="btn-batch" @click="clearSelection">取消选择</button>
      </div>

      <table class="data-table" v-if="sortedList.length">
        <thead>
          <tr>
            <th class="col-check">
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" />
            </th>
            <th class="col-code sortable" @click="toggleSort('code')">编码{{ sortArrow('code') }}</th>
            <th class="sortable" @click="toggleSort('name')">商品名称{{ sortArrow('name') }}</th>
            <th>单位</th>
            <th class="col-price sortable" @click="toggleSort('defaultPrice')">单价{{ sortArrow('defaultPrice') }}</th>
            <th class="col-price">成本</th>
            <th class="col-price">毛利率</th>
            <th class="col-stock sortable" @click="toggleSort('currentQty')">当前库存{{ sortArrow('currentQty') }}</th>
            <th>计算公式</th>
            <th>上架</th>
            <th style="width:180px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in sortedList" :key="item.id" :class="{ 'row-alert': isAlert(item) }">
            <td class="col-check">
              <input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleSelect(item.id)" />
            </td>
            <td class="col-code">{{ item.code || '-' }}</td>
            <td class="col-name">
              <span class="name-text">{{ item.name }}</span>
              <span class="cat-tag" v-if="item.productType">{{ item.productType }}</span>
            </td>
            <td>{{ item.unit || '-' }}</td>
            <td class="col-price">{{ item.defaultPrice != null ? formatMoney(item.defaultPrice) : '-' }}</td>
            <td class="col-price cost">{{ item.costPrice != null ? formatMoney(item.costPrice) : '-' }}</td>
            <td class="col-price">
              <span v-if="item.defaultPrice > 0 && item.costPrice != null" :class="{ 'profit-neg': item.defaultPrice < item.costPrice }">
                {{ ((item.defaultPrice - item.costPrice) / item.defaultPrice * 100).toFixed(1) }}%
              </span>
              <span v-else style="color:#ccc">-</span>
            </td>
            <td class="col-stock">
              <div class="stock-cell">
                <span class="stock-indicator" :class="stockLevel(item)"></span>
                <span class="stock-num" :class="'stock-' + stockLevel(item)">{{ item.currentQty ?? 0 }}</span>
                <span class="stock-unit">{{ item.unit || '' }}</span>
              </div>
            </td>
            <td>
              <span class="rule-tag">{{ formulaName(item) }}</span>
            </td>
            <td>
              <button class="toggle-switch" :class="{ on: item.isShow === 1 }" @click="handleToggle(item)" :title="item.isShow === 1 ? '点击下架' : '点击上架'">
                <span class="toggle-knob"></span>
              </button>
            </td>
            <td class="col-action">
              <button class="btn-text" @click="openEdit(item)">编辑</button>
              <button class="btn-text" :class="item.isShow === 1 ? 'warn' : 'success'" @click="handleToggle(item)">{{ item.isShow === 1 ? '下架' : '上架' }}</button>
              <button class="btn-text danger" @click="handleDel(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ list.length ? '未匹配到商品' : '暂无商品，点击右上角新增' }}</p>
    </div>

    <!-- ===== Pagination ===== -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadList"
        @size-change="onPageSizeChange"
      />
    </div>

    <!-- ===== Add / Edit Dialog ===== -->
    <div class="modal-overlay" v-if="showModal" @click.self="showModal = false" @keydown.esc="showModal = false" tabindex="-1">
      <div class="modal modal-commodity">
        <div class="modal-header">
          <h3>{{ editId ? '编辑商品' : '新增商品' }}</h3>
          <button class="modal-close" @click="showModal = false">&times;</button>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <!-- Basic Info -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>基本信息</h4>
            <div class="form-row">
              <div class="form-group">
                <label>商品编码 <span class="req">*</span></label>
                <input v-model="form.code" required placeholder="如：SP001" />
              </div>
              <div class="form-group">
                <label>商品名称 <span class="req">*</span></label>
                <input v-model="form.name" required placeholder="商品名称" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>别名</label>
                <input v-model="form.aliasesInput" placeholder="多个别名用顿号分隔，如：80断桥推拉门、客户叫法B" />
              </div>
              <div class="form-group"></div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>商品细目</label>
                <el-select
                  v-model="form.productType"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="选择或输入细目"
                  style="width:100%"
                  @change="blurSelect"
                >
                  <el-option v-for="t in productTypes" :key="t" :label="t" :value="t" />
                </el-select>
              </div>
              <div class="form-group"></div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>单位</label>
                <el-select
                  v-model="form.unit"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="选择或输入单位"
                  style="width:100%"
                  @change="onUnitChange"
                >
                  <el-option v-for="u in formulaUnitOptions" :key="u" :label="u" :value="u" />
                </el-select>
              </div>
              <div class="form-group"></div>
            </div>
          </div>

          <!-- Pricing -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>价格与定价</h4>
            <div class="form-row">
              <div class="form-group">
                <label>销售单价 (¥)</label>
                <input v-model.number="form.defaultPrice" type="number" step="0.01" min="0" placeholder="0.00" />
              </div>
              <div class="form-group">
                <label>材料成本 (¥)</label>
                <input v-model.number="form.materialCost" type="number" step="0.01" min="0" placeholder="0.00" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>人工成本 (¥)</label>
                <input v-model.number="form.laborCost" type="number" step="0.01" min="0" placeholder="0.00" />
              </div>
              <div class="form-group">
                <label>配件成本 (¥)</label>
                <input v-model.number="form.accessoryCost" type="number" step="0.01" min="0" placeholder="0.00" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>总成本 (¥)</label>
                <input type="number" step="0.01" min="0" placeholder="自动计算" :value="totalCost" disabled class="cost-total-input" />
              </div>
              <div class="form-group">
                <label>计算公式 <span class="req">*</span></label>
                <select v-model="form.formulaId" class="formula-select">
                  <option value="" disabled>请选择公式（选单位会自动带入默认公式）</option>
                  <option v-for="f in formulaList" :key="f.id" :value="f.id">{{ f.name }}（{{ f.unit || '未设单位' }}）</option>
                </select>
                <button type="button" class="btn-link-formula" @click="openFormulaCreator">＋ 新建公式</button>
                <div v-if="currentFormula" class="formula-hint">表达式：<code>{{ currentFormula.formula }}</code></div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <div class="profit-hint" v-if="form.defaultPrice != null && totalCost != null && form.defaultPrice > 0">
                  <span class="profit-label">毛利</span>
                  <span class="profit-val" :class="{ negative: form.defaultPrice < totalCost }">
                    {{ formatMoney(form.defaultPrice - totalCost) }}
                    ({{ ((form.defaultPrice - totalCost) / form.defaultPrice * 100).toFixed(1) }}%)
                  </span>
                </div>
                <div class="profit-hint" v-else-if="form.defaultPrice != null && (form.materialCost != null || form.laborCost != null || form.accessoryCost != null) && form.defaultPrice > 0">
                  <span class="profit-label">毛利</span>
                  <span class="profit-val">
                    需填写全部三项成本
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Inventory -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>库存预警</h4>
            <div class="form-row">
              <div class="form-group">
                <label>当前库存</label>
                <input v-model.number="form.currentQty" type="number" min="0" step="1" placeholder="0" />
              </div>
              <div class="form-group"></div>
            </div>
            <div class="form-row alert-thresholds">
              <div class="form-group">
                <label><span class="alert-dot high"></span>高预警值</label>
                <input v-model.number="form.alertHigh" type="number" min="0" step="1" placeholder="如：100" />
                <span class="threshold-hint">库存低于此值显示黄色</span>
              </div>
              <div class="form-group">
                <label><span class="alert-dot mid"></span>中预警值</label>
                <input v-model.number="form.alertMid" type="number" min="0" step="1" placeholder="如：50" />
                <span class="threshold-hint">库存低于此值显示橙色</span>
              </div>
              <div class="form-group">
                <label><span class="alert-dot low"></span>低预警值</label>
                <input v-model.number="form.alertLow" type="number" min="0" step="1" placeholder="如：10" />
                <span class="threshold-hint">库存低于此值显示红色</span>
              </div>
            </div>
          </div>

          <!-- Remark -->
          <div class="form-section">
            <h4 class="section-title"><span class="section-dash"></span>备注</h4>
            <div class="form-group full">
              <textarea v-model="form.remark" rows="2" placeholder="商品备注信息（选填）"></textarea>
            </div>
          </div>

          <div class="form-group full">
            <label class="check-label">
              <input type="checkbox" v-model="form.isShow" :true-value="1" :false-value="0" />
              上架展示
            </label>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showModal = false">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 新建公式（内联） -->
    <div class="modal-overlay" v-if="formulaCreator.show" @click.self="formulaCreator.show = false" @keydown.esc="formulaCreator.show = false" tabindex="-1">
      <div class="modal modal-formula">
        <div class="modal-header">
          <h3>新建计算公式</h3>
          <button class="modal-close" @click="formulaCreator.show = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label>公式名称 <span class="req">*</span></label>
              <input v-model="formulaCreator.name" placeholder="如：面积含墙厚" />
            </div>
            <div class="form-group">
              <label>单位</label>
              <input v-model="formulaCreator.unit" placeholder="如：㎡" />
            </div>
          </div>
          <div class="form-group full">
            <label>公式表达式 <span class="req">*</span></label>
            <input v-model="formulaCreator.formula" placeholder="如：宽*高*墙厚*樘数/1000000" />
            <span class="field-hint">变量可用：宽、高、墙厚、吊脚、面积、樘数；结果=数量，金额=数量×单价</span>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="formulaCreator.show = false">取消</button>
            <button type="button" class="btn-primary" @click="saveFormulaCreator">创建</button>
          </div>
        </div>
      </div>
    </div>

    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入商品"
      :column-map="importConfigs.commodities.columns"
      :module="'commodities'"
      :on-complete="loadList"
      :sample-data="importConfigs.commodities.sample"
      template-filename="商品导入模板"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElPagination, ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
import {
  getCommodities,
  addCommodity,
  updateCommodity,
  deleteCommodity,
  getProductTypes,
  getCommodityAlerts,
  toggleCommodityShow,
  getFormulas,
  addFormula,
  restoreEntity,
} from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoAction } from '@/composables/useUndoAction'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { exportToExcel, fmtMoney } from '@/utils/exportExcel'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import { importConfigs } from '@/utils/importConfigs'
import { useTableSort } from '@/composables/useTableSort'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

// ===== State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { currentQty: 'number', defaultPrice: 'number' }, { storageKey: 'commodities_filters' })
const productTypes = ref([])
const alertList = ref([])
const importDialogVisible = ref(false)
const keyword = ref('')
const filterProductType = ref('')
const filterUnit = ref('')
const showOnlyAlerts = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showModal = ref(false)
useCtrlEnterSave(showModal, handleSave)
const saving = ref(false)
const selectedIds = ref([])
let editId = null
let searchTimer = null

const FILTER_KEY = 'commodities_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  if (q.keyword || q.productType || q.unit || q.alerts || q.page || q.size) {
    _restoring = true
    if (q.keyword) keyword.value = q.keyword
    if (q.productType) filterProductType.value = q.productType
    if (q.unit) filterUnit.value = q.unit
    if (q.alerts === '1') showOnlyAlerts.value = true
    if (q.page && Number(q.page) > 1) page.value = Number(q.page)
    if (q.size && Number(q.size) > 0) pageSize.value = Number(q.size)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.productType) filterProductType.value = saved.productType
    if (saved.unit) filterUnit.value = saved.unit
    if (saved.alerts) showOnlyAlerts.value = true
    if (saved.page && saved.page > 1) page.value = saved.page
    if (saved.size && saved.size > 0) pageSize.value = saved.size
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (keyword.value) { query.keyword = keyword.value; storage.keyword = keyword.value }
  if (filterProductType.value) { query.productType = filterProductType.value; storage.productType = filterProductType.value }
  if (filterUnit.value) { query.unit = filterUnit.value; storage.unit = filterUnit.value }
  if (showOnlyAlerts.value) { query.alerts = '1'; storage.alerts = true }
  if (page.value > 1) { query.page = String(page.value); storage.page = page.value }
  if (pageSize.value !== 20) { query.size = String(pageSize.value); storage.size = pageSize.value }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

// ===== Undo composables =====
const { handleDelete, handleBatchDelete } = useUndoDelete({
  deleteApi: deleteCommodity,
  restoreApi: (id) => restoreEntity('commodity', id),
  onSuccess: () => { loadList(); loadAlerts() },
  entityLabel: '商品',
})

const { handleBatchAction: handleBatchToggleShow } = useUndoAction({
  actionApi: toggleCommodityShow,
  reverseApi: toggleCommodityShow,
  onSuccess: loadList,
  entityLabel: '商品',
  actionLabel: '上下架',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateCommodity,
  onSuccess: () => { loadList(); loadAlerts() },
  entityLabel: '商品',
})

const defaultForm = () => ({
  code: '',
  name: '',
  aliasesInput: '',
  productType: '',
  unit: '',
  defaultPrice: null,
  costPrice: null,
  materialCost: null,
  laborCost: null,
  accessoryCost: null,
  formulaId: null,
  currentQty: 0,
  alertHigh: null,
  alertMid: null,
  alertLow: null,
  isShow: 1,
  remark: '',
})
const form = reactive(defaultForm())

// ===== Alert helpers =====
const alertCount = computed(() => alertList.value.length)

const totalCost = computed(() => {
  const m = Number(form.materialCost || 0)
  const l = Number(form.laborCost || 0)
  const a = Number(form.accessoryCost || 0)
  return (m || l || a) ? m + l + a : null
})

function isAlert(item) {
  return alertLevel(item) !== 'ok'
}

function stockLevel(item) {
  const qty = item.currentQty ?? 0
  if (item.alertLow != null && qty <= item.alertLow) return 'low'
  if (item.alertMid != null && qty <= item.alertMid) return 'mid'
  if (item.alertHigh != null && qty <= item.alertHigh) return 'high'
  return 'ok'
}

function alertLevel(item) {
  return stockLevel(item)
}



function formatMoney(v) {
  if (v == null) return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ===== Pagination =====
const pagedList = computed(() => list.value)

function onPageSizeChange() {
  page.value = 1
  loadList()
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadList()
  }, 350)
}

// ===== Data Loading =====
const formulaMap = ref({})           // 单位 → 默认公式（sort 最小者）
const formulaList = ref([])          // 全部公式（供下拉选择/新建后选中）
const formulaUnitOptions = computed(() => Object.keys(formulaMap.value))
const currentFormula = computed(() => formulaList.value.find(f => f.id === form.formulaId) || null)

function formulaName(item) {
  if (!item.formulaId) return '未指定'
  const f = formulaList.value.find(x => x.id === item.formulaId)
  return f ? (f.name + (f.unit ? '(' + f.unit + ')' : '')) : '未指定'
}

function parseAliases(aliases) {
  if (!aliases) return []
  try {
    const arr = JSON.parse(aliases)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

async function loadFormulas() {
  try {
    const res = await getFormulas()
    const data = res.data?.data ?? res.data
    const list = Array.isArray(data) ? data : []
    formulaList.value = list
    const map = {}
    for (const f of list) {
      if (f.unit && (!map[f.unit] || (map[f.unit].sort ?? 999) > (f.sort ?? 999))) {
        map[f.unit] = f
      }
    }
    formulaMap.value = map
  } catch (e) {
    console.error('加载公式列表失败:', e)
  }
}

function blurSelect() {
  setTimeout(() => { document.activeElement?.blur() }, 0)
}

async function onUnitChange(val) {
  blurSelect()
  if (!val) { form.formulaId = null; return }
  // 自动带入该单位的默认公式（sort 最小者）
  if (formulaMap.value[val]) {
    form.formulaId = formulaMap.value[val].id
    return
  }
  try {
    await ElMessageBox.confirm(
      `单位"${val}"还没有默认公式，是否新建一个？`,
      '未找到公式',
      { confirmButtonText: '新建公式', cancelButtonText: '取消', type: 'warning' }
    )
    openFormulaCreator(val)
  } catch {
    form.unit = ''
  }
}

// ===== 新建公式（商品编辑内联） =====
const formulaCreator = reactive({ show: false, name: '', unit: '', formula: '' })
const FORMULA_PARAMS_ALL = [
  { name: '宽', label: '宽(mm)', unit: 'mm', default: '' },
  { name: '高', label: '高(mm)', unit: 'mm', default: '' },
  { name: '墙厚', label: '墙厚(mm)', unit: 'mm', default: '' },
  { name: '吊脚', label: '吊脚', unit: '个', default: '' },
  { name: '面积', label: '面积(㎡)', unit: '㎡', default: '' },
  { name: '樘数', label: '樘数', unit: '樘', default: '1' },
]
function openFormulaCreator(prefillUnit) {
  formulaCreator.unit = prefillUnit || form.unit || ''
  formulaCreator.name = ''
  formulaCreator.formula = ''
  formulaCreator.show = true
}
async function saveFormulaCreator() {
  if (!formulaCreator.name.trim() || !formulaCreator.formula.trim()) {
    ElMessage.warning('公式名称和表达式不能为空')
    return
  }
  try {
    await addFormula({
      name: formulaCreator.name.trim(),
      unit: formulaCreator.unit.trim(),
      formula: formulaCreator.formula.trim(),
      parameters: JSON.stringify(FORMULA_PARAMS_ALL),
      description: '',
      sort: 999, // 新公式不抢占该单位的默认公式（默认=sort 最小者）
    })
    await loadFormulas()
    const created = formulaList.value.find(f => f.name === formulaCreator.name.trim())
    if (created) form.formulaId = created.id
    formulaCreator.show = false
    ElMessage.success('公式已创建')
  } catch (e) {
    if (!e._handled) ElMessage.error(e.message || '创建失败')
  }
}

onMounted(async () => {
  restoreFilters()
  await Promise.all([loadProductTypes(), loadAlerts(), loadFormulas()])
  loadList()
})

async function loadList() {
  const params = {
    page: page.value,
    size: pageSize.value,
  }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (filterProductType.value) params.productType = filterProductType.value
  if (filterUnit.value) params.unit = filterUnit.value
  if (showOnlyAlerts.value) params.alerts = 1

  try {
    const res = await getCommodities(params)
    const data = res.data?.data ?? res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data?.records || data?.list || data?.content || []
      total.value = data?.total ?? list.value.length
    }
  } catch (e) {
    console.error('加载商品列表失败:', e)
    list.value = []
    total.value = 0
  } finally {
    syncFiltersToUrl()
  }
}

async function loadProductTypes() {
  try {
    const res = await getProductTypes()
    const data = res.data?.data ?? res.data
    productTypes.value = Array.isArray(data) ? data.map(t => t.name) : []
  } catch {
    productTypes.value = []
  }
}

async function loadAlerts() {
  try {
    const res = await getCommodityAlerts()
    alertList.value = res.data?.data ?? res.data ?? []
  } catch {
    alertList.value = []
  }
}

// Watch filters
import { watch } from 'vue'
watch([filterProductType, filterUnit, showOnlyAlerts], () => {
  if (_restoring) return
  page.value = 1
  loadList()
})

// ===== CRUD =====
async function handleExport() {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const columns = [
    { label: '编码', key: 'code', width: 12 },
    { label: '商品名称', key: 'name', width: 18 },
    { label: '单位', key: 'unit', width: 8 },
    { label: '单价', key: 'defaultPrice', width: 12, format: fmtMoney },
    { label: '成本', key: 'costPrice', width: 12, format: fmtMoney },
    { label: '材料成本', key: 'materialCost', width: 12, format: fmtMoney },
    { label: '人工成本', key: 'laborCost', width: 12, format: fmtMoney },
    { label: '配件成本', key: 'accessoryCost', width: 12, format: fmtMoney },
    { label: '计算公式', key: 'formulaId', width: 14, format: (v, row) => formulaName(row) },
  ]
  await exportToExcel(list.value, columns, '商品库存', { sheetName: '商品' })
}

async function openAdd() {
  editId = null
  Object.assign(form, defaultForm())
  await loadFormulas()
  showModal.value = true
}

async function openEdit(item) {
  snapshotBeforeEdit(item)
  editId = item.id
  Object.assign(form, {
    code: item.code || '',
    name: item.name || '',
    aliasesInput: parseAliases(item.aliases).join('、'),
    productType: item.productType || '',
    unit: item.unit || '',
    defaultPrice: item.defaultPrice ?? null,
    costPrice: item.costPrice ?? null,
    materialCost: item.materialCost ?? null,
    laborCost: item.laborCost ?? null,
    accessoryCost: item.accessoryCost ?? null,
    formulaId: item.formulaId ?? null,
    currentQty: item.currentQty ?? 0,
    alertHigh: item.alertHigh ?? null,
    alertMid: item.alertMid ?? null,
    alertLow: item.alertLow ?? null,
    isShow: item.isShow ?? 1,
    remark: item.remark || '',
  })
  await loadFormulas()
  showModal.value = true
}

async function handleSave() {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('编码和名称不能为空')
    return
  }
  if (!form.formulaId) {
    ElMessage.warning('请选择计算公式（选单位会自动带入该单位的默认公式）')
    return
  }
  saving.value = true
  try {
    // 排除 aliasesInput（非实体字段，Jackson 不忽略未知字段会报错）
    const { aliasesInput, ...formRest } = form
    const data = {
      ...formRest,
      aliases: JSON.stringify(form.aliasesInput.split(/[,，、]/).map(s => s.trim()).filter(Boolean)),
      defaultPrice: form.defaultPrice != null ? Number(form.defaultPrice) : null,
      materialCost: form.materialCost != null ? Number(form.materialCost) : null,
      laborCost: form.laborCost != null ? Number(form.laborCost) : null,
      accessoryCost: form.accessoryCost != null ? Number(form.accessoryCost) : null,
      costPrice: totalCost.value != null ? Number(totalCost.value) : (form.costPrice != null ? Number(form.costPrice) : null),
      currentQty: form.currentQty != null ? Number(form.currentQty) : 0,
      alertHigh: form.alertHigh != null ? Number(form.alertHigh) : null,
      alertMid: form.alertMid != null ? Number(form.alertMid) : null,
      alertLow: form.alertLow != null ? Number(form.alertLow) : null,
      isShow: form.isShow ? 1 : 0,
    }
    if (editId) {
      await updateCommodity(editId, data)
      afterEditSave({ id: editId, ...data })
    } else {
      await addCommodity(data)
      ElMessage.success('商品已添加')
    }
    showModal.value = false
    loadList()
    loadAlerts()
  } catch (e) {
    console.error('保存失败:', e)
    if (!e._handled) ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleDel(item) {
  handleDelete(item)
}

function handleToggle(item) {
  handleBatchToggleShow([item.id], item.name)
}

// ===== Batch Operations =====
const isAllSelected = computed(() => sortedList.value.length > 0 && selectedIds.value.length === sortedList.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < sortedList.value.length)

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}

function toggleSelectAll() {
  if (isAllSelected.value) selectedIds.value = []
  else selectedIds.value = sortedList.value.map(item => item.id)
}

function clearSelection() {
  selectedIds.value = []
}

async function batchToggleShow(val) {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  await handleBatchToggleShow(ids)
  clearSelection()
}

async function batchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  await handleBatchDelete(ids)
  clearSelection()
}
</script>

<style scoped>
/* ===== Summary bar ===== */
.summary-bar {
  display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;
}
.summary-card {
  background: #fff; border-radius: 8px; padding: 14px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04); min-width: 140px;
}
.sc-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.sc-value { font-size: 20px; font-weight: 700; color: #333; }
.summary-card.gold .sc-value { color: #C5A265; }
.summary-card.green .sc-value { color: #52c41a; }
.summary-card.red .sc-value { color: #e74c3c; }

/* ===== Header ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: -0.5px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Alert toggle button */
.alert-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.2s;
  font-family: inherit;
}
.alert-toggle:hover {
  border-color: #fa8c16;
  color: #fa8c16;
  background: #fff7e6;
}
.alert-toggle.active {
  border-color: #fa8c16;
  background: #fff7e6;
  color: #fa8c16;
  font-weight: 600;
}
.alert-toggle em {
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
  background: #fa8c16;
  color: #fff;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

/* ===== Toolbar ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cat-select {
  padding: 8px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
  outline: none;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s;
  min-width: 120px;
  font-family: inherit;
}
.cat-select:focus {
  border-color: #C5A265;
}
.rule-select {
  min-width: 130px;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 6px 12px;
  transition: border-color 0.2s;
}
.search-wrap:focus-within {
  border-color: #C5A265;
}
.search-wrap svg {
  color: #bbb;
  flex-shrink: 0;
}
.search-input {
  border: none;
  outline: none;
  font-size: 13px;
  color: #333;
  width: 180px;
  font-family: inherit;
  background: transparent;
}

/* ===== Alert Bar ===== */
.alert-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.alert-bar-label {
  font-size: 12px;
  font-weight: 600;
  color: #d48806;
  margin-right: 4px;
}
.alert-bar-item {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.alert-bar-item.low {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
.alert-bar-item.mid {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}
.alert-bar-item.high {
  background: #fffbe6;
  color: #d48806;
  border: 1px solid #ffe58f;
}
.alert-bar-item.ok {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.alert-bar-more {
  font-size: 11px;
  color: #999;
  margin-left: 4px;
}

/* ===== Table ===== */
.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: auto;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 960px;
}
.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 12px;
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
  user-select: none;
}
.data-table td {
  padding: 12px 14px;
  font-size: 13px;
  border-bottom: 1px solid #f8f8f8;
  color: #444;
  vertical-align: middle;
}
.data-table tbody tr {
  transition: background 0.12s;
}
.data-table tbody tr:hover {
  background: #fafbfc;
}
.data-table tbody tr.row-alert {
  background: rgba(250, 140, 22, 0.03);
  border-left: 3px solid #fa8c16;
}
.data-table tbody tr.row-alert:hover {
  background: rgba(250, 140, 22, 0.06);
}

.col-code {
  width: 100px;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}
.col-check {
  width: 40px;
  text-align: center;
  padding: 14px 8px;
}
.col-check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #C5A265;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fffbe6;
  border-bottom: 1px solid #ffe58f;
  font-size: 13px;
}
.batch-info {
  color: #666;
  margin-right: 4px;
}
.batch-info strong {
  color: #C5A265;
}
.btn-batch {
  padding: 5px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-batch:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.btn-batch.danger {
  color: #e74c3c;
  border-color: #f5c6cb;
}
.btn-batch.danger:hover {
  background: #fff1f0;
  border-color: #e74c3c;
}
.col-name {
  min-width: 140px;
}
.name-text {
  font-weight: 600;
  color: #1a1a1a;
}
.cat-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 10px;
  background: rgba(197, 162, 101, 0.1);
  color: #C5A265;
  font-weight: 500;
  vertical-align: middle;
}
.col-price {
  font-weight: 600;
  color: #C5A265;
  white-space: nowrap;
  font-size: 13px;
}
.col-price.cost {
  color: #999;
  font-weight: 500;
}

/* Stock cell */
.col-stock {
  min-width: 100px;
}
.stock-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.stock-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.2s;
}
.stock-indicator.ok {
  background: #52c41a;
  box-shadow: 0 0 4px rgba(82, 196, 26, 0.3);
}
.stock-indicator.high {
  background: #faad14;
  box-shadow: 0 0 4px rgba(250, 173, 20, 0.4);
}
.stock-indicator.mid {
  background: #fa8c16;
  box-shadow: 0 0 4px rgba(250, 140, 22, 0.4);
  animation: pulse-mid 2s infinite;
}
.stock-indicator.low {
  background: #f5222d;
  box-shadow: 0 0 6px rgba(245, 34, 45, 0.5);
  animation: pulse-low 1.2s infinite;
}
@keyframes pulse-mid {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
@keyframes pulse-low {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.3); }
}

.stock-num {
  font-weight: 700;
  font-size: 14px;
}
.stock-num.stock-ok {
  color: #389e0d;
}
.stock-num.stock-high {
  color: #d48806;
}
.stock-num.stock-mid {
  color: #d46b08;
}
.stock-num.stock-low {
  color: #cf1322;
}
.stock-unit {
  font-size: 11px;
  color: #999;
}

/* Rule tag */
.rule-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  background: #f5f5f5;
  color: #888;
}
.rule-tag.rule-area {
  background: #e6f7ff;
  color: #1890ff;
}
.rule-tag.rule-count {
  background: #f6ffed;
  color: #52c41a;
}
.rule-tag.rule-linear {
  background: #fff7e6;
  color: #fa8c16;
}
.rule-tag.rule-height {
  background: #f9f0ff;
  color: #722ed1;
}

/* Toggle switch */
.toggle-switch {
  position: relative;
  width: 40px;
  height: 22px;
  border-radius: 11px;
  border: none;
  background: #d9d9d9;
  cursor: pointer;
  transition: background 0.25s;
  padding: 0;
}
.toggle-switch.on {
  background: #C5A265;
}
.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.25s;
}
.toggle-switch.on .toggle-knob {
  transform: translateX(18px);
}

.col-action {
  white-space: nowrap;
}
.btn-text {
  background: none;
  border: none;
  color: #C5A265;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.15s;
  font-family: inherit;
  font-weight: 500;
}
.btn-text:hover {
  background: rgba(197, 162, 101, 0.08);
}
.btn-text.danger {
  color: #e74c3c;
}
.btn-text.danger:hover {
  background: rgba(231, 76, 60, 0.06);
}
.btn-text.warn {
  color: #fa8c16;
}
.btn-text.warn:hover {
  background: rgba(250, 140, 22, 0.08);
}
.btn-text.success {
  color: #52c41a;
}
.btn-text.success:hover {
  background: rgba(82, 196, 26, 0.08);
}

.btn-primary {
  padding: 8px 20px;
  background: #C5A265;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}
.btn-primary:hover {
  background: #b89555;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.empty {
  text-align: center;
  padding: 60px 0;
  color: #ccc;
  font-size: 14px;
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

/* ===== Modal / Dialog ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 40px;
  overflow-y: auto;
}
.modal {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-commodity {
  width: 680px;
  max-width: 95vw;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
  border-radius: 12px 12px 0 0;
}
.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  font-size: 22px;
  color: #999;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.modal-close:hover {
  background: #f5f5f5;
  color: #333;
}
.modal-body {
  padding: 20px 24px 24px;
}

/* Form sections */
.form-section {
  margin-bottom: 22px;
}
.form-section:last-of-type {
  margin-bottom: 16px;
}
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.section-dash {
  display: inline-block;
  width: 16px;
  height: 2px;
  background: #C5A265;
  border-radius: 1px;
}

.form-row {
  display: flex;
  gap: 14px;
  margin-bottom: 12px;
}
.form-row:last-child {
  margin-bottom: 0;
}
.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.form-group.full {
  flex: none;
  width: 100%;
}
.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: #555;
  margin-bottom: 5px;
}
.form-group .req {
  color: #e74c3c;
}
.formula-select { width: 100%; }
.btn-link-formula {
  margin-top: 6px; padding: 4px 12px; background: none;
  border: 1px dashed #C5A265; border-radius: 4px; font-size: 12px;
  color: #C5A265; cursor: pointer; transition: all .15s;
}
.btn-link-formula:hover { background: rgba(197,162,101,.08); }
.formula-hint { margin-top: 6px; font-size: 12px; color: #999; }
.formula-hint code { background: #f6f8fa; padding: 1px 6px; border-radius: 4px; font-size: 11px; color: #666; }
.form-group input,
.form-group select,
.form-group textarea {
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  font-family: inherit;
  color: #333;
  transition: border-color 0.2s;
  background: #fff;
}
.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #C5A265;
}
.form-group textarea {
  resize: vertical;
  min-height: 60px;
}
.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #ccc;
}

/* Alert thresholds section */
.alert-thresholds {
  display: flex;
  gap: 12px;
}
.alert-thresholds .form-group {
  position: relative;
}
.threshold-hint {
  font-size: 10px;
  color: #bbb;
  margin-top: 3px;
}
.alert-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}
.alert-dot.high {
  background: #faad14;
}
.alert-dot.mid {
  background: #fa8c16;
}
.alert-dot.low {
  background: #f5222d;
}

/* Profit hint */
.profit-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  margin-top: auto;
}
.profit-label {
  font-size: 11px;
  color: #999;
  font-weight: 500;
}
.profit-val {
  font-size: 13px;
  font-weight: 700;
  color: #389e0d;
}
.profit-val.negative {
  color: #cf1322;
  background: #fff1f0;
}
.profit-neg {
  color: #cf1322;
  font-weight: 600;
}

/* Check label */
.check-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  font-weight: 500;
  margin-bottom: 4px;
}
.check-label input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: #C5A265;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: 20px;
}
.btn-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-cancel:hover {
  border-color: #999;
  color: #333;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-right {
    width: 100%;
    flex-wrap: wrap;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }
  .search-input {
    width: 100%;
  }
  .modal-commodity {
    width: 95vw;
    margin: 0 10px;
  }
  .form-row {
    flex-direction: column;
    gap: 10px;
  }
  .alert-thresholds {
    flex-direction: column;
  }
  .alert-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ===== Sortable columns ===== */
.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}
.data-table th.sortable:hover {
  color: #C5A265;
}
</style>

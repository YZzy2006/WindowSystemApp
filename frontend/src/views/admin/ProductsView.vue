<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>产品管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="load" />
        <input v-model="keyword" class="search-input" placeholder="搜名称 / 型号 / 材质..." />
        <select v-model="filterCatId" class="cat-select">
          <option :value="null">全部分类</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <button class="btn-primary" @click="openAdd">＋ 新增产品</button>
      </div>
    </div>

    <div class="card">
      <table class="data-table" v-if="filteredList.length">
        <thead><tr><th class="col-id">ID</th><th class="col-cover">封面</th><th>名称</th><th>型号</th><th>材质</th><th class="col-price">价格</th><th>上架</th><th style="width:120px">操作</th></tr></thead>
        <tbody>
          <tr v-for="p in filteredList" :key="p.id" >
            <td class="col-id">{{ p.id }}</td>
            <td><img v-if="p.coverImage" :src="p.coverImage" class="thumb" /></td>
            <td>{{ p.name }}</td>
            <td>{{ p.model || '-' }}</td>
            <td>{{ p.material || '-' }}</td>
            <td class="col-price">{{ p.price ? '¥' + Number(p.price).toLocaleString() : '-' }}</td>
            <td>{{ p.isShow ? '是' : '否' }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openEdit(p)">编辑</button>
              <button class="btn-text danger" @click="handleDel(p)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ list.length ? '未匹配到产品' : '暂无产品' }}</p>
    </div>

    <div class="modal-overlay" v-if="showModal" @click.self="closeModal" @keydown.esc="closeModal" tabindex="-1">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editId ? '编辑产品' : '新增产品' }}</h3>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <div class="form-group">
            <label><span class="required">*</span> 分类</label>
            <select v-model="form.categoryId" required><option value="" disabled>请选择分类</option><option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option></select>
          </div>
          <div class="form-row">
            <div class="form-group"><label><span class="required">*</span> 名称</label><input v-model="form.name" required /></div>
            <div class="form-group"><label>型号</label><input v-model="form.model" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label>材质</label><input v-model="form.material" /></div>
            <div class="form-group"><label>单价 (¥/㎡)</label><input v-model="form.price" type="number" step="0.01" /></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label>价格标签</label><input v-model="form.priceTag" placeholder="起" /></div>
            <div class="form-group"><label>参考总价示例</label><input v-model="form.referencePrice" placeholder="1.5m×1.5m窗户，总价约 ¥1,950" /></div>
          </div>

          <!-- 价格构成明细 - 可视化编辑 -->
          <div class="form-group">
            <label>价格构成明细</label>
            <div class="inline-editor">
              <div class="ie-row ie-header"><span class="ie-name">包含项目</span><span class="ie-price">单价</span><span class="ie-act"></span></div>
              <div class="ie-row" v-for="(item, i) in breakdownRows" :key="i">
                <input v-model="item.name" placeholder="如：基础型材框架" class="ie-name" @input="syncBreakdown" />
                <input v-model="item.price" placeholder="¥580/㎡" class="ie-price" @input="syncBreakdown" />
                <button type="button" class="ie-del" @click="breakdownRows.splice(i,1); syncBreakdown()">✕</button>
              </div>
              <button type="button" class="ie-add" @click="breakdownRows.push({name:'',price:''}); syncBreakdown()">+ 添加项目</button>
            </div>
          </div>

          <div class="form-group"><label>计价规则</label><input v-model="form.pricingRule" placeholder="按洞口面积计算,不足1㎡按1㎡计,异形窗另计" /></div>

          <!-- 选项升级加价 - 可视化编辑 -->
          <div class="form-group">
            <label>选项升级加价</label>
            <div class="opt-price-section" v-for="cat in optionCats" :key="cat.key">
              <h5 class="opt-price-title">{{ cat.label }}</h5>
              <div class="inline-editor">
                <div class="ie-row ie-header"><span class="ie-name">选项名称</span><span class="ie-price">加价 (¥/㎡)</span><span class="ie-act"></span></div>
                <div class="ie-row" v-for="(item, i) in cat.rows" :key="i">
                  <input v-model="item.name" :placeholder="cat.phName" class="ie-name" @input="syncOptionPrices" />
                  <input v-model="item.price" placeholder="0 = 不加价" class="ie-price" type="number" step="1" @input="syncOptionPrices" />
                  <button type="button" class="ie-del" @click="cat.rows.splice(i,1); syncOptionPrices()">✕</button>
                </div>
                <button type="button" class="ie-add" @click="cat.rows.push({name:'',price:0}); syncOptionPrices()">+ 添加{{ cat.label }}</button>
              </div>
            </div>
          </div>

          <div class="form-group"><label>封面图</label><ImageUploader v-model="form.coverImage" /></div>
          <div class="form-group"><label>产品图集（最多6张）</label><MultiImageUploader v-model="form.images" /></div>
          <div class="form-group"><label>规格参数</label><SpecEditor v-model="form.specs" /></div>
          <div class="form-group">
            <label>性能参数（留空不显示）</label>
            <div class="inline-editor">
              <div class="ie-row ie-header"><span class="ie-name">指标</span><span class="ie-price">数值</span></div>
              <div class="ie-row" v-for="p in perfRows" :key="p.key">
                <span class="ie-name" style="display:flex;align-items:center;padding:8px 10px;font-size:13px;color:#555">{{ p.label }}</span>
                <input v-model="p.value" :placeholder="p.ph" class="ie-price" @input="syncPerformance" />
              </div>
            </div>
          </div>
          <div class="form-group"><label>描述</label><textarea v-model="form.description" rows="2"></textarea></div>
          <label class="check-label"><input type="checkbox" v-model="form.isShow" :true-value="1" :false-value="0" /> 上架展示</label>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getProducts, addProduct, updateProduct, deleteProduct, getCategories, restoreEntity } from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import ImageUploader from '@/components/ImageUploader.vue'
import MultiImageUploader from '@/components/MultiImageUploader.vue'
import SpecEditor from '@/components/SpecEditor.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'
import { useUnsavedConfirm } from '@/composables/useUnsavedConfirm'

const list = ref([])
const categories = ref([])
const keyword = ref('')
const filterCatId = ref(null)

const FILTER_KEY = 'products_filters'
let _restoring = false
function restoreFilters() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.filterCatId !== undefined) filterCatId.value = saved.filterCatId
    _restoring = false
  } catch {}
}
function syncFilters() {
  if (_restoring) return
  sessionStorage.setItem(FILTER_KEY, JSON.stringify({ keyword: keyword.value, filterCatId: filterCatId.value }))
}
watch([keyword, filterCatId], syncFilters)
const showModal = ref(false)
const saving = ref(false)
const form = reactive({ categoryId: null, name: '', model: '', material: '', price: null, coverImage: '', images: '', specs: '', priceTag: '', priceBreakdown: '', pricingRule: '', referencePrice: '', optionPrices: '', performance: '', description: '', isShow: 1 })
let editId = null
useCtrlEnterSave(showModal, handleSave)
const { resetDirty, closeGuard } = useUnsavedConfirm(showModal, () => JSON.stringify(form), { form })
function closeModal() { closeGuard(() => { showModal.value = false }) }

// ===== Undo composables =====
const { handleDelete: undoDelete } = useUndoDelete({
  deleteApi: deleteProduct,
  restoreApi: (id) => restoreEntity('product', id),
  onSuccess: load,
  entityLabel: '产品',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateProduct,
  onSuccess: load,
  entityLabel: '产品',
})

// Inline editors state
const breakdownRows = ref([])
const optionCats = reactive([
  { key: '花色', label: '🎨 花色/颜色', phName: '如：金属灰', rows: [] },
  { key: '玻璃', label: '🪞 玻璃配置', phName: '如：Low-E镀膜', rows: [] },
])

function syncBreakdown() {
  form.priceBreakdown = breakdownRows.value.filter(r => r.name.trim()).map(r => `${r.name.trim()}:${r.price.trim() || '含'}`).join('\n')
}
function syncOptionPrices() {
  const lines = []
  for (const cat of optionCats) {
    for (const r of cat.rows) {
      if (r.name.trim()) lines.push(`${cat.key}:${r.name.trim()}:${r.price || 0}`)
    }
  }
  form.optionPrices = lines.join('\n')
}

// Performance editor
const perfRows = reactive([
  { key: '抗风压', label: '抗风压', ph: '如：9级', value: '' },
  { key: '气密性', label: '气密性', ph: '如：8级', value: '' },
  { key: '水密性', label: '水密性', ph: '如：6级', value: '' },
  { key: '隔音', label: '隔音', ph: '如：35dB', value: '' },
  { key: '保温K值', label: '保温K值', ph: '如：1.8', value: '' },
])
function syncPerformance() {
  const obj = {}
  for (const p of perfRows) { if (p.value.trim()) obj[p.key] = p.value.trim() }
  form.performance = Object.keys(obj).length ? JSON.stringify(obj) : ''
}
function initPerfRows(raw) {
  for (const p of perfRows) p.value = ''
  if (!raw) return
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    for (const p of perfRows) { if (obj[p.key]) p.value = obj[p.key] }
  } catch {}
}

function normalizeLines(raw) {
  if (!raw) return []
  // Handle both real newlines and literal \n strings from DB
  return raw.replace(/\\n/g, '\n').split('\n').map(s => s.trim()).filter(Boolean)
}
function initBreakdownRows(raw) {
  breakdownRows.value = normalizeLines(raw).map(line => {
    const idx = line.lastIndexOf(':')
    return idx === -1 ? { name: line, price: '' } : { name: line.substring(0, idx).trim(), price: line.substring(idx + 1).trim() }
  })
}
function initOptionRows(raw, specsJson) {
  for (const cat of optionCats) cat.rows = []
  // Load existing pricing from optionPrices
  const priceMap = {}
  normalizeLines(raw).forEach(line => {
    const parts = line.split(':')
    if (parts.length >= 3) {
      const k = parts[0].trim() + '::' + parts[1].trim()
      priceMap[k] = parseInt(parts[2].trim()) || 0
    }
  })
  // Also extract available options from specs
  const specsMap = {}
  if (specsJson) {
    try {
      const obj = typeof specsJson === 'string' ? JSON.parse(specsJson) : specsJson
      const colorRaw = obj['花色:可选颜色'] || obj['花色:外框颜色'] || ''
      const glassRaw = obj['玻璃:可选配置'] || obj['玻璃:玻璃配置'] || ''
      if (colorRaw) specsMap['花色'] = colorRaw.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
      if (glassRaw) specsMap['玻璃'] = glassRaw.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
    } catch {}
  }
  // Merge: specs items first (show all), pricing from optionPrices
  for (const cat of optionCats) {
    if (specsMap[cat.key]) {
      cat.rows = specsMap[cat.key].map(name => ({
        name,
        price: priceMap[cat.key + '::' + name] ?? 0
      }))
    } else {
      // No specs data, fall back to raw optionPrices
      Object.entries(priceMap).forEach(([k, v]) => {
        const idx = k.indexOf('::')
        if (idx > 0 && k.substring(0, idx) === cat.key) {
          cat.rows.push({ name: k.substring(idx + 2), price: v })
        }
      })
    }
  }
}

onMounted(() => { restoreFilters(); load(); getCategories().then(r => categories.value = r.data.data || []) })

const filteredList = computed(() => {
  let arr = list.value
  if (filterCatId.value != null) arr = arr.filter(p => p.categoryId === filterCatId.value)
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    const keywords = kw.split(/\s+/).filter(Boolean)
    arr = arr.filter(p => {
      const text = [p.name, p.model, p.material].map(s => (s || '').toLowerCase()).join(' ')
      return keywords.every(k => text.includes(k))
    })
  }
  return arr
})

function load() { getProducts().then(r => list.value = r.data.data || []).catch(e => { ElMessage.error('加载产品失败'); console.error(e) }) }

function openAdd() {
  editId = null
  Object.assign(form, { categoryId: null, name: '', model: '', material: '', price: null, coverImage: '', images: '', specs: '', priceTag: '', priceBreakdown: '', pricingRule: '', referencePrice: '', optionPrices: '', performance: '', description: '', isShow: 1 })
  breakdownRows.value = []
  for (const cat of optionCats) cat.rows = []
  for (const p of perfRows) p.value = ''
  showModal.value = true
}
function openEdit(p) {
  snapshotBeforeEdit(p)
  editId = p.id
  Object.assign(form, { categoryId: p.categoryId, name: p.name, model: p.model || '', material: p.material || '', price: p.price, coverImage: p.coverImage || '', images: p.images || '', specs: p.specs || '', priceTag: p.priceTag || '', priceBreakdown: p.priceBreakdown || '', pricingRule: p.pricingRule || '', referencePrice: p.referencePrice || '', optionPrices: p.optionPrices || '', performance: p.performance || '', description: p.description || '', isShow: p.isShow != null ? p.isShow : 1 })
  initBreakdownRows(form.priceBreakdown)
  initOptionRows(form.optionPrices, p.specs)
  initPerfRows(form.performance)
  showModal.value = true
}

async function handleSave() {
  if (!form.categoryId) { ElMessage.warning('请选择分类'); return }
  if (!form.name.trim()) { ElMessage.warning('请输入产品名称'); return }
  saving.value = true
  try {
    const data = { ...form, price: form.price ? Number(form.price) : null, isShow: form.isShow ? 1 : 0 }
    if (editId) {
      await updateProduct(editId, data)
      afterEditSave({ id: editId, ...data })
    } else {
      await addProduct(data)
      ElMessage.success('产品已添加')
    }
    resetDirty(); showModal.value = false; load()
  } finally { saving.value = false }
}

function handleDel(p) { undoDelete(p) }
</script>

<style scoped>
.cat-select {
  padding: 8px 14px; border: 1px solid #e0e0e0; border-radius: var(--radius);
  font-size: 13px; color: #666; outline: none; background: #fff; cursor: pointer;
  transition: border-color .2s; min-width: 130px;
}
.cat-select:focus { border-color: var(--gold-primary); }

.col-id { width: 50px; color: var(--text-placeholder); }
.col-cover { width: 64px; }
.col-price { font-weight: 600; color: var(--gold-primary); white-space: nowrap; }

.modal-overlay { align-items: flex-start; padding-top: 24px; }
.modal { width: 720px; max-height: 88vh; overflow-y: auto; }

/* Inline editor */
.inline-editor { border: 1px solid #e8e8e8; border-radius: var(--radius); overflow: hidden; }
.ie-row { display: flex; gap: 6px; align-items: center; padding: 5px 8px; border-bottom: 1px solid #f5f5f5; }
.ie-row:last-child { border-bottom: none; }
.ie-header { background: #fafbfc; padding: 7px 8px; }
.ie-header span { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
.ie-name { flex: 1; min-width: 0; }
.ie-price { flex: 1; min-width: 0; }
.ie-act { width: 24px; flex-shrink: 0; }
.ie-row input {
  padding: 7px 8px; border: 1px solid #e0e0e0; border-radius: var(--radius-sm);
  font-size: 12px; outline: none; font-family: inherit; transition: border-color .15s;
  width: 100%; box-sizing: border-box;
}
.ie-row input:focus { border-color: var(--gold-primary); }
.ie-del {
  width: 28px; height: 28px; border-radius: 50%; border: none; background: none;
  color: #ccc; cursor: pointer; font-size: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; transition: all .15s;
}
.ie-del:hover { background: #fff1f0; color: #e74c3c; }
.ie-add {
  width: 100%; padding: 8px; border: none; background: #fafbfc;
  color: var(--gold-primary); font-size: 12px; cursor: pointer; font-family: inherit;
  transition: background .15s;
}
.ie-add:hover { background: #f0ede5; }

.opt-price-section { margin-bottom: 14px; }
.opt-price-title { font-size: 13px; font-weight: 600; color: #555; margin-bottom: 8px; }
.opt-price-title::before { content: ''; display: inline-block; width: 8px; height: 8px; background: var(--gold-primary); border-radius: 2px; margin-right: 8px; }
.required { color: #e74c3c; margin-right: 4px; }
</style>

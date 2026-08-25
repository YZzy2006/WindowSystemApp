<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>计算公式</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="load" />
        <input v-model="keyword" class="search-input" placeholder="搜索公式名称..." />
        <button class="btn-primary" @click="openAdd">＋ 新增公式</button>
      </div>
    </div>

    <div class="card">
      <!-- Batch Bar -->
      <div class="batch-bar" v-if="selectedIds.length > 0">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 项</span>
        <button class="btn-batch danger" @click="batchDelete" :disabled="batchOperating">{{ batchOperating ? '处理中...' : '批量删除' }}</button>
        <button class="btn-batch" @click="clearSelection">取消选择</button>
      </div>

      <table class="data-table" v-if="filteredList.length">
        <thead>
          <tr>
            <th class="col-check">
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" />
            </th>
            <th>公式名称</th>
            <th class="col-unit">单位</th>
            <th>公式表达式</th>
            <th class="col-params">参数</th>
            <th class="col-sort">排序</th>
            <th class="col-time">创建时间</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in filteredList" :key="f.id" >
            <td class="col-check">
              <input type="checkbox" :checked="selectedIds.includes(f.id)" @change="toggleSelect(f.id)" />
            </td>
            <td class="col-name">
              <span class="name-text">{{ f.name }}</span>
              <span v-if="isDefaultFormula(f)" class="default-badge" title="该单位的默认公式（排序最小者）">默认</span>
              <span class="desc-text" v-if="f.description">{{ f.description }}</span>
            </td>
            <td class="col-unit">{{ f.unit || '-' }}</td>
            <td class="col-formula"><code>{{ f.formula }}</code></td>
            <td class="col-params">
              <span class="param-count">{{ getParamCount(f) }} 个</span>
            </td>
            <td class="col-sort">{{ f.sort ?? 0 }}</td>
            <td class="col-time">{{ formatTime(f.createTime) }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openEdit(f)">编辑</button>
              <button class="btn-text danger" @click="handleDel(f)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ list.length ? '未匹配到公式' : '暂无计算公式' }}</p>
    </div>

    <!-- Add / Edit Modal -->
    <div class="modal-overlay" v-if="showModal" @click.self="showModal=false" @keydown.esc="showModal=false" tabindex="-1">
      <div class="modal modal-formula">
        <div class="modal-header">
          <h3>{{ editId ? '编辑公式' : '新增公式' }}</h3>
          <button class="modal-close" @click="showModal=false">&times;</button>
        </div>
        <form class="modal-body" @submit.prevent="handleSave">
          <div class="form-row">
            <div class="form-group">
              <label>公式名称 <span class="req">*</span></label>
              <input v-model="form.name" placeholder="如：面积计价" required />
            </div>
            <div class="form-group">
              <label>单位</label>
              <input v-model="form.unit" placeholder="如：㎡、樘、米" />
            </div>
          </div>
          <div class="form-group full">
            <label>公式表达式 <span class="req">*</span></label>
            <input v-model="form.formula" placeholder="如：宽*高/1000000*单价" required @input="onFormulaInput" />
            <span class="field-hint">使用参数名称作为变量，如 "宽*高/1000000*单价"；中文括号/乘除号输入后会自动转为英文</span>
          </div>
          <div class="form-group full">
            <label>说明</label>
            <input v-model="form.description" placeholder="公式的用途说明（选填）" />
          </div>
          <div class="form-group full">
            <label>排序</label>
            <input v-model.number="form.sort" type="number" placeholder="数字越小越靠前" />
          </div>

          <!-- Dynamic Parameters -->
          <div class="params-section">
            <div class="params-header">
              <label>公式参数</label>
              <button type="button" class="btn-add-param" @click="addParam">＋ 添加参数</button>
            </div>
            <div class="params-list" v-if="form.parameters.length">
              <div class="param-row" v-for="(p, idx) in form.parameters" :key="idx">
                <select v-model="p.name" @change="onParamNameChange(idx)" class="param-input param-name">
                  <option value="" disabled>选择字段</option>
                  <option v-for="sp in availableParams(idx)" :key="sp.name" :value="sp.name">{{ sp.name }}</option>
                </select>
                <input :value="p.label" class="param-input param-label" readonly tabindex="-1" />
                <input :value="p.unit" class="param-input param-unit" readonly tabindex="-1" />
                <input v-model="p.default" placeholder="默认值" class="param-input param-default" />
                <button type="button" class="btn-remove-param" @click="removeParam(idx)">&times;</button>
              </div>
              <div class="param-labels">
                <span class="param-input param-name">变量名</span>
                <span class="param-input param-label">显示名称</span>
                <span class="param-input param-unit">单位</span>
                <span class="param-input param-default">默认值</span>
              </div>
            </div>
            <p v-else class="param-empty">暂无参数，点击上方按钮添加</p>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showModal=false">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFormulas, addFormula, updateFormula, deleteFormula, restoreEntity } from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'

const list = ref([])
const keyword = ref('')
const showModal = ref(false)
const saving = ref(false)
useCtrlEnterSave(showModal, handleSave)

// 与编辑销售表字段对齐的标准参数
const STANDARD_PARAMS = [
  { name: '宽', label: '宽(mm)', unit: 'mm' },
  { name: '高', label: '高(mm)', unit: 'mm' },
  { name: '墙厚', label: '墙厚(mm)', unit: 'mm' },
  { name: '锁位', label: '锁位', unit: '' },
  { name: '吊脚', label: '吊脚', unit: '个' },
  { name: '樘数', label: '樘数', unit: '樘' },
  { name: '面积', label: '面积(㎡)', unit: '㎡' },
]
const form = reactive({
  name: '',
  unit: '',
  formula: '',
  parameters: [],
  description: '',
  sort: 0,
})
let editId = null

// ===== Selection =====
const selectedIds = ref([])
const batchOperating = ref(false)
const isAllSelected = computed(() => filteredList.value.length > 0 && selectedIds.value.length === filteredList.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < filteredList.value.length)

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}
function toggleSelectAll() {
  if (isAllSelected.value) selectedIds.value = []
  else selectedIds.value = filteredList.value.map(f => f.id)
}
function clearSelection() { selectedIds.value = [] }

// ===== Undo composables =====
const { handleDelete: undoDelete } = useUndoDelete({
  deleteApi: deleteFormula,
  restoreApi: (id) => restoreEntity('pricing-formula', id),
  onSuccess: load,
  entityLabel: '计算公式',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateFormula,
  onSuccess: load,
  entityLabel: '计算公式',
})

onMounted(load)

const filteredList = computed(() => {
  if (!keyword.value.trim()) return list.value
  const kw = keyword.value.trim().toLowerCase()
  return list.value.filter(f => f.name?.toLowerCase().includes(kw))
})

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

function getParamCount(f) {
  try {
    const params = typeof f.parameters === 'string' ? JSON.parse(f.parameters) : f.parameters
    return Array.isArray(params) ? params.length : 0
  } catch {
    // 兼容历史双重转义 [{\"name\":...}]
    try {
      const params = JSON.parse(f.parameters.replace(/\\"/g, '"'))
      return Array.isArray(params) ? params.length : 0
    } catch {
      return 0
    }
  }
}

// 该单位的默认公式 = 排序最小者（商品选单位时自动带入）
function isDefaultFormula(f) {
  const sameUnit = list.value.filter(x => x.unit === f.unit)
  if (!sameUnit.length) return false
  const min = Math.min(...sameUnit.map(x => (x.sort ?? 999)))
  return (f.sort ?? 999) === min
}

function load() {
  getFormulas().then(r => {
    const data = r.data?.data ?? r.data
    list.value = Array.isArray(data) ? data : []
  }).catch(e => {
    console.error('加载公式失败:', e)
    if (!e._handled) ElMessage.error('加载公式失败')
  })
}

function openAdd() {
  editId = null
  form.name = ''
  form.unit = ''
  form.formula = ''
  form.parameters = []
  form.description = ''
  form.sort = 999 // 新公式默认不抢占该单位的默认公式（默认=sort 最小者）
  showModal.value = true
}

function openEdit(f) {
  snapshotBeforeEdit(f)
  editId = f.id
  form.name = f.name || ''
  form.unit = f.unit || ''
  form.formula = f.formula || ''
  form.description = f.description || ''
  form.sort = f.sort ?? 0
  try {
    const params = typeof f.parameters === 'string' ? JSON.parse(f.parameters) : f.parameters
    form.parameters = Array.isArray(params) ? params.map(p => ({ ...p })) : []
  } catch {
    // 兼容历史双重转义 [{\"name\":...}]，避免编辑旧公式时参数被清空
    try {
      const params = JSON.parse(f.parameters.replace(/\\"/g, '"'))
      form.parameters = Array.isArray(params) ? params.map(p => ({ ...p })) : []
    } catch {
      form.parameters = []
    }
  }
  nextTick(() => { showModal.value = true })
}

function addParam() {
  form.parameters.push({ name: '', label: '', unit: '', default: '' })
}

function removeParam(idx) {
  form.parameters.splice(idx, 1)
}

// 返回当前行可选的标准参数（排除已被其他行选中的）
function availableParams(idx) {
  const used = new Set(form.parameters.filter((_, i) => i !== idx).map(p => p.name).filter(Boolean))
  return STANDARD_PARAMS.filter(sp => !used.has(sp.name))
}

// 选择标准参数后自动填充 label 和 unit
function onParamNameChange(idx) {
  const p = form.parameters[idx]
  const sp = STANDARD_PARAMS.find(s => s.name === p.name)
  if (sp) {
    p.label = sp.label
    p.unit = sp.unit
  }
}

// 中文输入法符号 → 英文公式符号（中文括号/乘除号/减号/全角空格）
function normalizeFormulaSymbols(s) {
  return s
    .replace(/（/g, '(')
    .replace(/）/g, ')')
    .replace(/×/g, '*')
    .replace(/÷/g, '/')
    .replace(/＋/g, '+')
    .replace(/－/g, '-')
    .replace(/　/g, ' ') // 全角空格 → 半角
    .replace(/[ \t]+/g, ' ')
}

// 公式表达式输入时实时转英文（显示和保存都是英文，避免用户以为保存后还是中文）
function onFormulaInput(e) {
  const el = e.target
  const pos = el.selectionStart ?? (el.value ? el.value.length : 0)
  const normalized = normalizeFormulaSymbols(form.formula)
  if (normalized !== form.formula) {
    form.formula = normalized
    // 1对1 转换字符数不变，恢复光标位置避免跳末尾
    requestAnimationFrame(() => {
      try { el.setSelectionRange(pos, pos) } catch (err) {}
    })
  }
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入公式名称')
    return
  }
  if (!form.formula.trim()) {
    ElMessage.warning('请输入公式表达式')
    return
  }
  // 中文输入法符号 → 英文（避免中文括号/乘除号导致公式解析失败）
  // 保存前统一转换，用户用中文输入法输入 （）×÷－ 也会自动变成合法符号
  form.formula = normalizeFormulaSymbols(form.formula.trim())
  // 校验公式表达式中的变量名
  const formulaStr = form.formula.trim()
  const paramNames = new Set(form.parameters.map(p => p.name).filter(Boolean))
  // 提取表达式中的中文变量名（连续中文字符）
  const usedVars = formulaStr.match(/[一-鿿]+/g) || []
  const unknownVars = usedVars.filter(v => !paramNames.has(v))
  if (unknownVars.length) {
    await ElMessageBox.confirm(
      `公式中包含未定义的变量：${unknownVars.join('、')}。请检查是否拼写错误，或先在下方参数中添加。\n\n当前已选参数：${[...paramNames].join('、') || '无'}`,
      '变量名不匹配',
      { confirmButtonText: '知道了', showCancelButton: false, type: 'warning' }
    )
    return
  }
  const data = {
    name: form.name.trim(),
    unit: form.unit.trim(),
    formula: form.formula.trim(),
    parameters: JSON.stringify(form.parameters),
    description: form.description.trim(),
    sort: form.sort ?? 0,
  }
  saving.value = true
  try {
    if (editId) {
      await updateFormula(editId, data)
      afterEditSave({ id: editId, ...data })
      ElMessage.success('公式已更新')
    } else {
      await addFormula(data)
      ElMessage.success('公式已添加')
    }
    showModal.value = false
    load()
  } catch (e) {
    if (!e._handled) ElMessage.error(e.message || '保存失败')
  } finally { saving.value = false }
}

function handleDel(f) { undoDelete(f) }

async function batchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个公式？`, '批量删除', { type: 'warning' })
  } catch { return }
  batchOperating.value = true
  try {
    let success = 0
    for (const id of ids) {
      try { await deleteFormula(id); success++ } catch (e) { console.error(e) }
    }
    ElMessage.success(`已删除 ${success} 个公式`)
    clearSelection()
    load()
  } finally { batchOperating.value = false }
}
</script>

<style scoped>
.col-check { width: 40px; text-align: center; }
.col-unit { width: 70px; text-align: center; }
.col-params { width: 70px; text-align: center; }
.col-sort { width: 60px; text-align: center; color: var(--text-placeholder); }
.col-time { width: 150px; color: var(--text-placeholder); font-size: 12px; }
.col-action { width: 120px; }
.col-name { min-width: 120px; }
.col-formula code {
  background: #f6f8fa;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
  font-family: 'SF Mono', 'Consolas', monospace;
}
.name-text { font-weight: 600; color: #1a1a1a; }
.default-badge {
  margin-left: 6px; padding: 1px 6px; border-radius: 8px;
  background: #f0f7ff; border: 1px solid #b3d4f7; color: #2b6cb0;
  font-size: 10px; vertical-align: middle;
}
.desc-text { display: block; font-size: 11px; color: #999; margin-top: 2px; }
.param-count { font-size: 12px; color: #999; }

.modal-formula { width: 620px; max-width: 95vw; }
.modal-close {
  width: 32px; height: 32px; border: none; background: none;
  font-size: 22px; color: #999; cursor: pointer; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.modal-close:hover { background: #f5f5f5; color: #333; }

.batch-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; background: #f6f8fa; border-bottom: 1px solid #eee;
}
.batch-info { font-size: 13px; color: #666; }
.batch-info strong { color: #C5A265; }
.btn-batch {
  background: none; border: 1px solid #ddd; border-radius: 4px;
  padding: 4px 12px; font-size: 12px; cursor: pointer; color: #555;
}
.btn-batch:hover { border-color: #C5A265; color: #C5A265; }
.btn-batch.danger { color: #e74c3c; border-color: #f5c6cb; }
.btn-batch.danger:hover { background: #fff5f5; }

.field-hint { font-size: 11px; color: #bbb; margin-top: 3px; }

/* Parameters editor */
.params-section {
  margin-top: 16px;
  padding: 14px;
  background: #fafbfc;
  border: 1px solid #eee;
  border-radius: 8px;
}
.params-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.params-header label {
  font-size: 12px; font-weight: 600; color: #555;
}
.btn-add-param {
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #C5A265;
  border-radius: 4px;
  font-size: 12px;
  color: #C5A265;
  cursor: pointer;
  transition: all .15s;
}
.btn-add-param:hover { background: rgba(197,162,101,.08); }

.params-list { display: flex; flex-direction: column; gap: 0; }
.param-row {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 0;
}
.param-labels {
  display: flex; align-items: center; gap: 6px;
  padding-top: 6px; border-top: 1px solid #eee; margin-top: 4px;
}
.param-labels span {
  font-size: 10px; color: #bbb; text-align: center;
}
.param-input {
  padding: 6px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 12px;
  outline: none;
  font-family: inherit;
  background: #fff;
}
.param-input:focus { border-color: #C5A265; }
select.param-input { cursor: pointer; appearance: auto; }
.param-input[readonly] { background: #f9f9f9; color: #999; cursor: default; }
.param-name { width: 100px; }
.param-label { width: 100px; }
.param-unit { width: 70px; }
.param-default { width: 70px; }
.btn-remove-param {
  width: 24px; height: 24px;
  background: none; border: none;
  color: #e74c3c; font-size: 16px;
  cursor: pointer; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.btn-remove-param:hover { background: rgba(231,76,60,.08); }
.param-empty { font-size: 12px; color: #ccc; text-align: center; padding: 10px 0; }
</style>

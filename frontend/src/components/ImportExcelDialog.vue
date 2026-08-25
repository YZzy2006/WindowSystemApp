<template>
  <el-dialog
    :model-value="modelValue"
    :title="title || '导入数据'"
    width="680px"
    :close-on-click-modal="false"
    close-on-press-escape
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="resetState"
  >
    <!-- Step 1: Upload -->
    <div v-if="step === 'upload'" class="import-step">
      <div class="upload-zone" @click="triggerFileInput" @drop.prevent="onDrop" @dragover.prevent>
        <div class="upload-icon">📁</div>
        <p class="upload-text">点击选择或拖拽 Excel 文件到此处</p>
        <p class="upload-hint">支持 .xlsx / .xls 格式</p>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChange" />
      </div>
      <p v-if="uploadError" class="error-text">{{ uploadError }}</p>
      <p class="template-link" @click="handleDownloadTemplate">📥 没有模板？点击下载导入模板</p>
    </div>

    <!-- Step 2: Preview -->
    <div v-if="step === 'preview'" class="import-step">
      <div class="preview-info">
        <span>共 <strong>{{ totalCount }}</strong> {{ groupBy ? '个订单' : '条数据' }}</span>
        <span v-if="groupBy" class="preview-detail">（{{ rawRowCount }} 行明细）</span>
      </div>
      <div v-if="previewLoading" class="warning-box">正在检查重复数据...</div>
      <div v-else-if="previewFailed" class="warning-box error">
        <span>无法检查重复数据，导入时由系统判定</span>
      </div>
      <div v-else-if="previewCounts" class="warning-box" :class="{ error: previewCounts.invalid > 0 }">
        <span>
          新增 <strong class="text-success">{{ previewCounts.created }}</strong> 条，
          已存在 <strong class="text-skip">{{ previewCounts.skipped }}</strong> 条（将自动跳过）
          <template v-if="previewCounts.invalid">，校验不合格 <strong class="text-error">{{ previewCounts.invalid }}</strong> 条</template>
        </span>
      </div>
      <div v-if="noItemColumns" class="warning-box error">
        <span>⚠ 该文件未检测到商品明细列，导入的{{ groupBy ? '订单' : '记录' }}将没有明细（金额为 0）。如需完整数据请改用【全部/当前页 - 详细】导出后再导入。</span>
      </div>
      <div v-if="unmatchedHeaders.length" class="warning-box">
        <span>未匹配的列：{{ unmatchedHeaders.join('、') }}（将被忽略）</span>
      </div>
      <div v-if="parseErrors.length" class="warning-box error">
        <span>{{ parseErrors.length }} 条数据校验警告（见下方预览）</span>
      </div>
      <div class="preview-table-wrap">
        <table class="preview-table">
          <thead>
            <tr>
              <th v-if="showStatusCol">状态</th>
              <th v-for="col in previewColumns" :key="col.key">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in previewRows" :key="i">
              <td v-if="showStatusCol" class="status-cell">
                <span v-if="previewStatus[i] === 'exists'" class="tag tag-skip">已存在</span>
                <span v-else-if="previewStatus[i] === 'invalid'" class="tag tag-error">校验不通过</span>
                <span v-else-if="previewStatus[i] === 'new'" class="tag tag-new">新增</span>
              </td>
              <td v-for="col in previewColumns" :key="col.key">{{ formatPreviewVal(row[col.key]) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="totalCount > 10" class="preview-more">...还有 {{ totalCount - 10 }} 条未显示</p>
    </div>

    <!-- Step 3: Importing -->
    <div v-if="step === 'importing'" class="import-step center">
      <el-progress :percentage="progressPercent" :stroke-width="12" style="width:80%;margin:0 auto 16px" />
      <p>正在导入 {{ progress.current }} / {{ progress.total }} ...</p>
      <p class="progress-detail">
        成功 <span class="text-success">{{ progress.success }}</span>，
        <template v-if="progress.skipped">跳过 <span class="text-skip">{{ progress.skipped }}</span>，</template>
        失败 <span class="text-error">{{ progress.failed }}</span>
      </p>
    </div>

    <!-- Step 4: Result -->
    <div v-if="step === 'result'" class="import-step">
      <div class="result-summary">
        <p class="result-title">导入完成</p>
        <p>成功 <span class="text-success">{{ progress.success }}</span> 条，
           <template v-if="progress.skipped">跳过 <span class="text-skip">{{ progress.skipped }}</span> 条，</template>
           失败 <span class="text-error">{{ progress.failed }}</span> 条</p>
      </div>
      <div v-if="skippedRows.length" class="error-table-wrap skip">
        <div class="skip-header">已存在，已跳过（不重复创建）：</div>
        <table class="error-table skip-table">
          <thead><tr><th>行号</th><th>提示信息</th></tr></thead>
          <tbody>
            <tr v-for="(s, i) in skippedRows" :key="i">
              <td>{{ s.index }}</td>
              <td>{{ s.message }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="importErrors.length" class="error-table-wrap">
        <table class="error-table">
          <thead><tr><th>行号</th><th>错误信息</th></tr></thead>
          <tbody>
            <tr v-for="(err, i) in importErrors" :key="i">
              <td>{{ err.index }}</td>
              <td>{{ err.message }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <template #footer>
      <el-button v-if="step === 'upload'" @click="$emit('update:modelValue', false)">取消</el-button>
      <template v-if="step === 'preview'">
        <el-button @click="step = 'upload'">重新选择</el-button>
        <el-button type="primary" class="btn-gold" :loading="previewLoading" @click="startImport">确认导入</el-button>
      </template>
      <el-button v-if="step === 'result'" type="primary" class="btn-gold" @click="$emit('update:modelValue', false)">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { parseExcelFile, mapRows, groupOrderRows, downloadTemplate } from '@/utils/exportExcel'
import { importPreview, importCommit } from '@/api/admin'

const props = defineProps({
  modelValue: Boolean,
  title: String,
  columnMap: { type: Array, required: true },
  module: { type: String, required: true },
  onComplete: Function,
  transformRecord: Function,
  groupBy: String,
  sampleData: Object,
  templateFilename: String,
})

const emit = defineEmits(['update:modelValue'])

const step = ref('upload')
const fileInput = ref(null)
const uploadError = ref('')
const previewLoading = ref(false)
const previewFailed = ref(false)

const totalCount = ref(0)
const rawRowCount = ref(0)
const previewRows = ref([])
const previewColumns = ref([])
const unmatchedHeaders = ref([])
const parseErrors = ref([])
const previewCounts = ref(null)
const previewStatus = ref({})
const noItemColumns = ref(false)

let mappedRecords = []    // 解析后的原始记录（含 _excelRow）
let payloadRecords = []   // 已 transform、去掉 _excelRow 的提交载荷
let rowNos = []           // 每条记录对应的 Excel 行号

const progress = ref({ current: 0, total: 0, success: 0, skipped: 0, failed: 0 })
const importErrors = ref([])
const skippedRows = ref([])

const progressPercent = computed(() => {
  if (!progress.value.total) return 0
  return Math.round((progress.value.current / progress.value.total) * 100)
})

const showStatusCol = computed(() =>
  !previewLoading.value && !previewFailed.value && !!previewCounts.value
)

let cancelled = false

function resetState() {
  step.value = 'upload'
  uploadError.value = ''
  previewLoading.value = false
  previewFailed.value = false
  totalCount.value = 0
  rawRowCount.value = 0
  previewRows.value = []
  previewColumns.value = []
  unmatchedHeaders.value = []
  parseErrors.value = []
  previewCounts.value = null
  previewStatus.value = {}
  noItemColumns.value = false
  mappedRecords = []
  payloadRecords = []
  rowNos = []
  progress.value = { current: 0, total: 0, success: 0, skipped: 0, failed: 0 }
  importErrors.value = []
  skippedRows.value = []
  cancelled = false
}

function handleDownloadTemplate() {
  downloadTemplate(props.columnMap.filter(c => !c.skip), props.templateFilename || '导入模板', {
    sample: props.sampleData,
    groupBy: props.groupBy,
  })
}

function triggerFileInput() {
  fileInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) processFile(file)
  e.target.value = ''
}

function onDrop(e) {
  const file = e.dataTransfer.files?.[0]
  if (file) processFile(file)
}

async function processFile(file) {
  uploadError.value = ''
  try {
    const { headers, rows } = await parseExcelFile(file)
    const result = mapRows(headers, rows, props.columnMap)

    parseErrors.value = result.errors
    unmatchedHeaders.value = result.unmatchedHeaders

    if (result.records.length === 0) {
      uploadError.value = '未解析到有效数据，请检查 Excel 文件格式'
      return
    }

    // Build preview columns (exclude derived/ignored skip columns)
    previewColumns.value = props.columnMap.filter(c => !c.skip).map(c => ({ key: c.key, label: c.label }))

    if (props.groupBy) {
      // Group rows into orders
      const orders = groupOrderRows(result.records, props.groupBy, props.columnMap)
      // Track first Excel row per order for accurate error reporting
      const orderFirstRow = new Map()
      for (const rec of result.records) {
        const key = rec[props.groupBy] || ''
        if (!orderFirstRow.has(key)) orderFirstRow.set(key, rec._excelRow)
      }
      for (const order of orders) {
        const key = order[props.groupBy] || ''
        order._excelRow = orderFirstRow.get(key)
      }
      mappedRecords = orders
      totalCount.value = orders.length
      rawRowCount.value = result.records.length
      // Preview: show first 10 orders (flatten for display)
      previewRows.value = orders.slice(0, 10).map(o => {
        const flat = { ...o }
        if (o.items?.length) {
          // Merge first item into preview
          Object.assign(flat, o.items[0])
          flat._itemCount = o.items.length
        }
        return flat
      })
    } else {
      mappedRecords = result.records
      totalCount.value = result.records.length
      rawRowCount.value = result.records.length
      previewRows.value = result.records.slice(0, 10)
    }

    // 简易导出（或任何无明细列的文件）：全部订单都没有明细 → 提示将产生空订单
    noItemColumns.value = !!(props.groupBy && totalCount.value > 0 && mappedRecords.every(o => !(o.items && o.items.length)))

    // Build commit payloads (transform once, shared by preview & commit)
    payloadRecords = mappedRecords.map((rec, i) => {
      const { _excelRow, ...rest } = rec
      return props.transformRecord ? props.transformRecord(rest) : rest
    })
    rowNos = mappedRecords.map((rec, i) => rec._excelRow || (i + 2))

    step.value = 'preview'
    runPreview()
  } catch (err) {
    uploadError.value = err.message || '文件解析失败'
  }
}

function formatPreviewVal(val) {
  if (val == null || val === '') return '-'
  return String(val)
}

async function runPreview() {
  previewLoading.value = true
  previewFailed.value = false
  previewCounts.value = null
  previewStatus.value = {}
  try {
    const res = await importPreview({ module: props.module, records: payloadRecords })
    const data = res.data?.data ?? {}
    previewCounts.value = { created: data.created || 0, skipped: data.skipped || 0, invalid: data.invalid || 0 }
    const st = {}
    ;(data.results || []).forEach(r => { st[r.row - 1] = r.status })
    previewStatus.value = st
  } catch (e) {
    previewFailed.value = true
  } finally {
    previewLoading.value = false
  }
}

async function startImport() {
  step.value = 'importing'
  cancelled = false
  progress.value = { current: 0, total: payloadRecords.length, success: 0, skipped: 0, failed: 0 }
  importErrors.value = []
  skippedRows.value = []

  const CHUNK = 30
  for (let start = 0; start < payloadRecords.length && !cancelled; start += CHUNK) {
    const chunk = payloadRecords.slice(start, start + CHUNK)
    try {
      const res = await importCommit({ module: props.module, records: chunk })
      const data = res.data?.data ?? {}
      const results = data.results || []
      for (let j = 0; j < results.length; j++) {
        const r = results[j]
        const excelRow = rowNos[start + j]
        if (r.status === 'created') {
          progress.value.success++
        } else if (r.status === 'skipped') {
          progress.value.skipped++
          skippedRows.value.push({ index: excelRow, message: r.message || '已存在，已跳过' })
        } else {
          progress.value.failed++
          importErrors.value.push({ index: excelRow, message: r.message || '导入失败' })
        }
      }
    } catch (e) {
      // 整块请求失败（网络等）→ 块内全部记失败
      for (let j = 0; j < chunk.length; j++) {
        progress.value.failed++
        importErrors.value.push({ index: rowNos[start + j], message: e.response?.data?.msg || e.message || '导入失败' })
      }
    }
    progress.value.current = progress.value.success + progress.value.skipped + progress.value.failed
  }

  step.value = 'result'
  if (props.onComplete) {
    props.onComplete()
  }
}
</script>

<style scoped>
.import-step { min-height: 260px; }
.import-step.center { display: flex; flex-direction: column; align-items: center; justify-content: center; }

.upload-zone {
  border: 2px dashed #d9d9d9; border-radius: 12px; padding: 48px 24px;
  text-align: center; cursor: pointer; transition: all .2s;
}
.upload-zone:hover { border-color: #C5A265; background: rgba(197,162,101,.03); }
.upload-icon { font-size: 40px; margin-bottom: 12px; }
.upload-text { font-size: 15px; color: #333; margin-bottom: 6px; }
.upload-hint { font-size: 12px; color: #999; }

.error-text { color: #e74c3c; font-size: 13px; margin-top: 12px; text-align: center; }

.template-link {
  text-align: center; margin-top: 14px; font-size: 13px; color: #C5A265;
  cursor: pointer; transition: color .2s;
}
.template-link:hover { color: #a88b4c; text-decoration: underline; }

.preview-info { margin-bottom: 12px; font-size: 14px; color: #333; }
.preview-info strong { color: #C5A265; font-size: 18px; }
.preview-detail { color: #999; font-size: 12px; margin-left: 4px; }

.warning-box {
  padding: 8px 12px; background: #fffbe6; border: 1px solid #ffe58f;
  border-radius: 6px; font-size: 12px; color: #d48806; margin-bottom: 12px;
}
.warning-box.error { background: #fff1f0; border-color: #ffa39e; color: #cf1322; }

.preview-table-wrap { max-height: 320px; overflow: auto; border: 1px solid #eee; border-radius: 6px; }
.preview-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.preview-table th {
  background: #fafbfc; font-weight: 600; color: #666; padding: 8px 10px;
  text-align: left; border-bottom: 1px solid #eee; white-space: nowrap; position: sticky; top: 0;
}
.preview-table td {
  padding: 6px 10px; border-bottom: 1px solid #f8f8f8; color: #444; white-space: nowrap; max-width: 200px; overflow: hidden; text-overflow: ellipsis;
}
.preview-more { font-size: 12px; color: #999; margin-top: 8px; text-align: center; }

.status-cell { width: 76px; }
.tag { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 11px; line-height: 18px; white-space: nowrap; }
.tag-new { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.tag-skip { background: #fffbe6; color: #d48806; border: 1px solid #ffe58f; }
.tag-error { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }

.progress-detail { font-size: 13px; color: #666; margin-top: 8px; }
.text-success { color: #52c41a; font-weight: 700; }
.text-skip { color: #d48806; font-weight: 700; }
.text-error { color: #e74c3c; font-weight: 700; }

.result-summary { text-align: center; margin-bottom: 20px; }
.result-title { font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 8px; }

.error-table-wrap.skip { border-color: #ffe58f; margin-bottom: 12px; }
.skip-header {
  padding: 8px 12px; background: #fffbe6; border-bottom: 1px solid #ffe58f;
  border-radius: 6px 6px 0 0; font-size: 12px; color: #d48806;
}
.skip-table th { background: #fffbe6; color: #d48806; }

.error-table-wrap { max-height: 240px; overflow: auto; border: 1px solid #eee; border-radius: 6px; }
.error-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.error-table th {
  background: #fff1f0; font-weight: 600; color: #cf1322; padding: 8px 10px;
  text-align: left; border-bottom: 1px solid #eee;
}
.error-table td {
  padding: 6px 10px; border-bottom: 1px solid #f8f8f8; color: #444;
}

.btn-gold { background: #C5A265; border-color: #C5A265; }
.btn-gold:hover { background: #b89555; border-color: #b89555; }
</style>

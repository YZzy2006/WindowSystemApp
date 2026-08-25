<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>操作日志</h2>
      <span class="header-hint">记录所有增删改操作，仅可查询</span>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <el-dropdown @command="handleExportCommand" trigger="click">
          <button class="btn-outline">导出</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="page">导出当前页</el-dropdown-item>
              <el-dropdown-item command="all">导出全部</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索操作人 / 操作内容..."
          @input="onSearchInput"
          @keyup.enter="handleSearch"
        />
        <select v-model="filterModule" class="filter-select" @change="handleSearch">
          <option value="">全部模块</option>
          <option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</option>
        </select>
        <select v-model="filterAction" class="filter-select" @change="handleSearch">
          <option value="">全部操作</option>
          <option value="新增">新增</option>
          <option value="修改">修改</option>
          <option value="删除">删除</option>
        </select>
        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          clearable
          style="width: 150px"
          @change="onDateChange"
        />
        <div class="quick-time-btns">
          <button class="time-btn" :class="{ active: activeTimeFilter === 'today' }" @click="applyTimeFilter('today')">今天</button>
          <button class="time-btn" :class="{ active: activeTimeFilter === 'week' }" @click="applyTimeFilter('week')">本周</button>
          <button class="time-btn" :class="{ active: activeTimeFilter === 'month' }" @click="applyTimeFilter('month')">本月</button>
          <button class="time-btn" :class="{ active: activeTimeFilter === 'all' }" @click="applyTimeFilter('all')">全部</button>
        </div>
        <button class="btn-search" @click="handleSearch">查询</button>
        <button v-if="hasFilter" class="btn-reset" @click="resetFilters">重置</button>
      </div>

      <!-- 表格 -->
      <table class="data-table" v-if="list.length" v-loading="loading">
        <thead>
          <tr>
            <th class="col-time">操作时间</th>
            <th class="col-user">操作人</th>
            <th class="col-module">模块</th>
            <th class="col-action">操作类型</th>
            <th>操作内容</th>
            <th class="col-ip">IP地址</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id" class="clickable-row" @click="openDetail(item)">
            <td class="col-time">{{ formatTime(item.createTime) }}</td>
            <td class="col-user">{{ item.username || '-' }}</td>
            <td class="col-module">
              <span class="module-tag">{{ item.module }}</span>
            </td>
            <td class="col-action">
              <span class="action-tag" :class="actionClass(item.action)">{{ item.action }}</span>
            </td>
            <td class="col-target">{{ item.target || '-' }}</td>
            <td class="col-ip">{{ item.ip || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state" v-loading="loading">
        <p>暂无操作日志</p>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="操作详情"
      direction="rtl"
      size="500px"
      close-on-press-escape
      :destroy-on-close="true"
    >
      <div class="detail-content" v-if="currentLog">
        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">操作时间</span>
            <span class="detail-value">{{ formatTime(currentLog.createTime) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作人</span>
            <span class="detail-value">{{ currentLog.username || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">模块</span>
            <span class="detail-value"><span class="module-tag">{{ currentLog.module }}</span></span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作类型</span>
            <span class="detail-value"><span class="action-tag" :class="actionClass(currentLog.action)">{{ currentLog.action }}</span></span>
          </div>
          <div class="detail-row">
            <span class="detail-label">IP 地址</span>
            <span class="detail-value detail-ip">{{ currentLog.ip || '-' }}</span>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-section-title">操作内容</div>
          <div class="detail-target">{{ currentLog.target || '-' }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOperationLogs } from '@/api/admin'
import { exportToExcel } from '@/utils/exportExcel'
import RefreshButton from '@/components/RefreshButton.vue'

const route = useRoute()
const router = useRouter()

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const filterModule = ref('')
const filterAction = ref('')
const filterDate = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const drawerVisible = ref(false)
const currentLog = ref(null)
const activeTimeFilter = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')

const hasFilter = computed(() => keyword.value.trim() || filterModule.value || filterAction.value || filterDate.value || activeTimeFilter.value)

const FILTER_KEY = 'operation_logs_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  if (q.keyword || q.module || q.action || q.date || q.page || q.size) {
    _restoring = true
    if (q.keyword) keyword.value = q.keyword
    if (q.module) filterModule.value = q.module
    if (q.action) filterAction.value = q.action
    if (q.date) filterDate.value = q.date
    if (q.page && Number(q.page) > 1) currentPage.value = Number(q.page)
    if (q.size && Number(q.size) > 0) pageSize.value = Number(q.size)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.module) filterModule.value = saved.module
    if (saved.action) filterAction.value = saved.action
    if (saved.date) filterDate.value = saved.date
    if (saved.page && saved.page > 1) currentPage.value = saved.page
    if (saved.size && saved.size > 0) pageSize.value = saved.size
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (keyword.value) { query.keyword = keyword.value; storage.keyword = keyword.value }
  if (filterModule.value) { query.module = filterModule.value; storage.module = filterModule.value }
  if (filterAction.value) { query.action = filterAction.value; storage.action = filterAction.value }
  if (filterDate.value) { query.date = filterDate.value; storage.date = filterDate.value }
  if (currentPage.value > 1) { query.page = String(currentPage.value); storage.page = currentPage.value }
  if (pageSize.value !== 20) { query.size = String(pageSize.value); storage.size = pageSize.value }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

const moduleOptions = [
  '客户管理', '供应商管理', '销售订单', '采购订单',
  '销售退货', '采购退货', '入库管理', '出库管理',
  '收付款', '支出管理', '产品管理', '分类管理',
  '案例管理', '询价管理', '商品库存', '考勤管理',
  '站点设置', '系统设置', '账号安全', '文件上传',
  '登录', '登录日志'
]

onMounted(() => { restoreFilters(); loadList() })

async function loadList() {
  loading.value = true
  try {
    const res = await getOperationLogs({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      module: filterModule.value || undefined,
      action: filterAction.value || undefined,
      date: filterDate.value || undefined,
      startDate: filterStartDate.value || undefined,
      endDate: filterEndDate.value || undefined
    })
    const data = res.data?.data || res.data
    list.value = data.records || []
    total.value = data.total || 0
  } catch (err) {
    console.error('加载操作日志失败:', err)
    if (!err._handled) ElMessage.error('加载操作日志失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
    syncFiltersToUrl()
  }
}

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadList()
  }, 350)
}

function handleSearch() {
  clearTimeout(searchTimer)
  currentPage.value = 1
  loadList()
}

function resetFilters() {
  keyword.value = ''
  filterModule.value = ''
  filterAction.value = ''
  filterDate.value = ''
  activeTimeFilter.value = ''
  filterStartDate.value = ''
  filterEndDate.value = ''
  currentPage.value = 1
  pageSize.value = 20
  sessionStorage.removeItem(FILTER_KEY)
  router.replace({ query: {} })
  loadList()
}

function openDetail(log) {
  currentLog.value = log
  drawerVisible.value = true
}

function pad(n) { return String(n).padStart(2, '0') }
function fmt(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }

function applyTimeFilter(type) {
  activeTimeFilter.value = type
  filterDate.value = ''
  const now = new Date()
  if (type === 'today') {
    filterStartDate.value = fmt(now)
    filterEndDate.value = fmt(now)
  } else if (type === 'week') {
    const d = new Date(now)
    d.setDate(d.getDate() - d.getDay() + 1)
    filterStartDate.value = fmt(d)
    filterEndDate.value = fmt(now)
  } else if (type === 'month') {
    filterStartDate.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-01`
    filterEndDate.value = fmt(now)
  } else {
    filterStartDate.value = ''
    filterEndDate.value = ''
  }
  currentPage.value = 1
  loadList()
}

function onDateChange() {
  activeTimeFilter.value = ''
  filterStartDate.value = ''
  filterEndDate.value = ''
  handleSearch()
}

function formatTime(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function actionClass(action) {
  if (action === '新增') return 'action-add'
  if (action === '修改') return 'action-edit'
  if (action === '删除') return 'action-delete'
  return ''
}

const exportColumns = [
  { label: '操作时间', key: 'createTime', width: 20, format: (v) => formatTime(v) },
  { label: '操作人', key: 'username', width: 12 },
  { label: '模块', key: 'module', width: 14 },
  { label: '操作类型', key: 'action', width: 10 },
  { label: '操作内容', key: 'target', width: 40 },
  { label: 'IP地址', key: 'ip', width: 16 },
]

function handleExportCommand(cmd) {
  if (cmd === 'page') handleExport()
  else handleExportAll()
}

async function handleExport() {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  await exportToExcel(list.value, exportColumns, '操作日志', { sheetName: '操作日志' })
  ElMessage.success('导出成功')
}

async function handleExportAll() {
  const loadingMsg = ElMessage({ message: '正在导出全部数据...', type: 'info', duration: 0 })
  try {
    const res = await getOperationLogs({
      page: 1,
      size: 10000,
      keyword: keyword.value.trim() || undefined,
      module: filterModule.value || undefined,
      action: filterAction.value || undefined,
      date: filterDate.value || undefined,
      startDate: filterStartDate.value || undefined,
      endDate: filterEndDate.value || undefined
    })
    const data = res.data?.data || res.data
    const records = data.records || []
    if (!records.length) {
      ElMessage.warning('暂无数据可导出')
      return
    }
    await exportToExcel(records, exportColumns, '操作日志-全部', { sheetName: '操作日志' })
    ElMessage.success(`导出成功，共 ${records.length} 条${records.length >= 10000 ? '（已截断至10000条）' : ''}`)
  } catch (err) {
    console.error('导出失败:', err)
    if (!err._handled) ElMessage.error('导出失败')
  } finally {
    loadingMsg.close()
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-outline {
  padding: 7px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-outline:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.header-hint {
  font-size: 13px;
  color: #999;
}

.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
  flex-wrap: wrap;
}
.search-input {
  padding: 8px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  outline: none;
  width: 220px;
  transition: border-color 0.2s;
  font-family: inherit;
}
.search-input:focus {
  border-color: #C5A265;
}
.search-input::placeholder {
  color: #bbb;
}
.filter-select {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  outline: none;
  background: #fff;
  cursor: pointer;
  font-family: inherit;
}
.filter-select:focus {
  border-color: #C5A265;
}
.quick-time-btns {
  display: flex;
  gap: 4px;
}
.time-btn {
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.time-btn:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.time-btn.active {
  background: #C5A265;
  border-color: #C5A265;
  color: #fff;
}
.btn-search {
  padding: 8px 18px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-search:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.btn-reset {
  padding: 8px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-reset:hover {
  border-color: #999;
  color: #666;
}

/* 表格 */
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 12px;
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
}
.data-table td {
  padding: 14px 16px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
  vertical-align: middle;
}
.data-table tbody tr {
  transition: background 0.15s;
}
.data-table tbody tr:hover {
  background: #fafbfc;
}

.col-time {
  width: 170px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.col-user {
  width: 90px;
  font-weight: 600;
  color: #1a1a1a;
}
.col-module {
  width: 100px;
}
.col-action {
  width: 80px;
}
.col-target {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.col-ip {
  width: 130px;
  font-size: 12px;
  color: #999;
  font-family: monospace;
}

.module-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f5f5;
  font-size: 12px;
  color: #666;
}

.action-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.action-add {
  background: #e6f7e6;
  color: #52c41a;
}
.action-edit {
  background: #e6f0ff;
  color: #1890ff;
}
.action-delete {
  background: #fff1f0;
  color: #e74c3c;
}

/* 可点击行 */
.clickable-row {
  cursor: pointer;
}
.clickable-row:hover {
  background: #fafbfc;
}

/* 详情抽屉 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.detail-section {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}
.detail-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}
.detail-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
}
.detail-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
  color: #999;
}
.detail-value {
  flex: 1;
  font-size: 13px;
  color: #333;
  word-break: break-all;
}
.detail-ip {
  font-family: monospace;
}
.detail-target {
  font-size: 13px;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f9f9f9;
  padding: 12px 16px;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  max-height: 400px;
  overflow-y: auto;
}

:deep(.el-drawer__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
  margin-bottom: 0;
}
:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
:deep(.el-drawer__body) {
  padding: 20px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #ccc;
  font-size: 14px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f5f5f5;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #C5A265;
}
:deep(.el-pagination.is-background .el-pager li:not(.is-active):hover) {
  color: #C5A265;
}
:deep(.el-loading-mask) {
  border-radius: 10px;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .search-input {
    width: 100%;
  }
}
</style>

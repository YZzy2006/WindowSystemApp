<template>
  <div class="enquiries-page admin-page">
    <!-- ===== 统计卡片（可点击筛选） ===== -->
    <div class="stats-row">
      <div class="stat-card" :class="{ active: quickFilter === 'today' }" @click="quickFilter = quickFilter === 'today' ? '' : 'today'">
        <span class="stat-icon">📥</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.today }}</span>
          <span class="stat-label">今日新询</span>
        </div>
      </div>
      <div class="stat-card highlight" :class="{ active: quickFilter === 'unread' }" @click="quickFilter = quickFilter === 'unread' ? '' : 'unread'">
        <span class="stat-icon">🔔</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.unread }}</span>
          <span class="stat-label">未读待跟进</span>
        </div>
      </div>
      <div class="stat-card" :class="{ active: quickFilter === 'week' }" @click="quickFilter = quickFilter === 'week' ? '' : 'week'">
        <span class="stat-icon">📊</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.week }}</span>
          <span class="stat-label">本周累计</span>
        </div>
      </div>
      <div class="stat-card highlight" :class="{ active: quickFilter === 'starred' }" @click="quickFilter = quickFilter === 'starred' ? '' : 'starred'">
        <span class="stat-icon">⭐</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.starred }}</span>
          <span class="stat-label">星标待跟进</span>
        </div>
      </div>
      <div class="stat-card" :class="{ active: quickFilter === 'measure' }" @click="quickFilter = quickFilter === 'measure' ? '' : 'measure'">
        <span class="stat-icon">📐</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.unmeasured }}</span>
          <span class="stat-label">待上门量尺</span>
        </div>
      </div>
      <div class="stat-card" :class="{ active: quickFilter === 'measured' }" @click="quickFilter = quickFilter === 'measured' ? '' : 'measured'">
        <span class="stat-icon">✅</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.measured }}</span>
          <span class="stat-label">已量尺</span>
        </div>
      </div>
      <div class="stat-card" :class="{ active: quickFilter === 'uncompleted' }" @click="quickFilter = quickFilter === 'uncompleted' ? '' : 'uncompleted'">
        <span class="stat-icon">📝</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.uncompleted }}</span>
          <span class="stat-label">未完单</span>
        </div>
      </div>
      <div class="stat-card" :class="{ active: quickFilter === 'completed' }" @click="quickFilter = quickFilter === 'completed' ? '' : 'completed'">
        <span class="stat-icon">🏁</span>
        <div class="stat-body">
          <span class="stat-num">{{ stats.completed }}</span>
          <span class="stat-label">已完单</span>
        </div>
      </div>
    </div>

    <!-- ===== 工具栏 ===== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="filter-tabs">
          <button :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</button>
          <button :class="{ active: filterStatus === 'unread' }" @click="filterStatus = 'unread'">未读</button>
          <button :class="{ active: filterStatus === 'read' }" @click="filterStatus = 'read'">已读</button>
          <span class="tab-divider"></span>
          <button :class="{ active: filterStatus === 'unmeasured' }" @click="filterStatus = 'unmeasured'">未量尺</button>
          <button :class="{ active: filterStatus === 'measured' }" @click="filterStatus = 'measured'">已量尺</button>
          <span class="tab-divider"></span>
          <button :class="{ active: filterStatus === 'uncompleted' }" @click="filterStatus = 'uncompleted'">未完单</button>
          <button :class="{ active: filterStatus === 'completed' }" @click="filterStatus = 'completed'">已完单</button>
          <span class="tab-divider"></span>
          <button :class="{ active: filterStatus === 'starred' }" @click="filterStatus = 'starred'">⭐ 星标</button>
        </div>
        <div class="date-capsules">
          <button v-for="d in dateOptions" :key="d.value" class="date-cap" :class="{ active: filterDate === d.value }" @click="pickQuick(d.value)">{{ d.label }}</button>
        </div>
      </div>
      <div class="toolbar-right">
        <RefreshButton :on-refresh="load" />
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchText" placeholder="搜索姓名/电话..." class="search-input" />
        </div>
        <span class="count-text" v-if="checkedIds.length">{{ checkedIds.length }} 条已选</span>
        <button class="btn-batch" v-if="checkedIds.length && hasUnreadChecked" @click="batchMarkRead">✓ 标为已读</button>
        <button class="btn-batch danger" v-if="checkedIds.length" @click="batchDelete">🗑 删除</button>
        <button class="btn-batch export" @click="exportExcel">📥 导出</button>
      </div>
    </div>

    <div class="summary-bar">
      <div class="summary-card"><div class="sc-label">询价总数</div><div class="sc-value">{{ total }} 条</div></div>
    </div>

    <!-- ===== 数据表格 ===== -->
    <div class="table-card">
      <table class="enq-table" v-if="filteredList.length">
        <thead>
          <tr>
            <th class="th-check"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th class="th-id sortable" @click="toggleSort('id')">ID <span class="sort-arrow">{{ sortArrow('id') }}</span></th>
            <th class="th-star">⭐</th>
            <th class="sortable" @click="toggleSort('name')">客户 <span class="sort-arrow">{{ sortArrow('name') }}</span></th>
            <th>项目信息</th>
            <th class="th-completed">完单</th>
            <th>楼层</th>
            <th class="sortable" @click="toggleSort('createTime')">时间 <span class="sort-arrow">{{ sortArrow('createTime') }}</span></th>
            <th>状态</th>
            <th>量尺</th>
            <th>预算</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in filteredList" :key="e.id" :class="{ unread: !e.isRead }" @click="openDrawer(e)">
            <td class="td-check" @click.stop><input type="checkbox" :checked="checkedIds.includes(e.id)" @change="toggleCheck(e.id)" /></td>
            <td class="td-id">#{{ e.id }}</td>
            <td class="td-star" @click.stop>
              <button class="star-btn" :class="{ on: e.isStarred }" @click="toggleStar(e)" :title="e.isStarred ? '取消星标' : '设为星标'">
                {{ e.isStarred ? '⭐' : '☆' }}
              </button>
            </td>
            <td>
              <div class="customer-cell">
                <span class="cust-name">{{ e.name }}</span>
                <span class="cust-phone">{{ maskPhone(e.phone) }}</span>
              </div>
            </td>
            <td>
              <div class="project-cell" v-if="getField(e.content, '房屋类型')">
                <span class="proj-type">{{ getField(e.content, '房屋类型') }}</span>
                <span class="proj-count">{{ getField(e.content, '窗户数量') }}</span>
              </div>
              <span v-else class="cell-na">-</span>
            </td>
            <td class="td-completed" @click.stop>
              <button class="complete-btn" :class="{ done: e.isCompleted }" @click="toggleComplete(e)" :title="e.isCompleted ? '标记未完成' : '标记已完单'">
                {{ e.isCompleted ? '✓' : '○' }}
              </button>
            </td>
            <td><span class="cell-val" v-if="getField(e.content, '所在楼层')">{{ getField(e.content, '所在楼层') }}</span><span v-else class="cell-na">-</span></td>
            <td class="td-time">{{ formatDate(e.createTime) }}</td>
            <td>
              <span class="status-tag" :class="e.isRead ? 'read' : 'unread'">
                {{ e.isRead ? '已读' : '未读' }}
              </span>
            </td>
            <td @click.stop>
              <span v-if="e.needMeasure" class="measure-tag" :class="e.isMeasured ? 'done' : 'pending'" @click="toggleMeasure(e)">
                {{ e.isMeasured ? '已量尺' : '待量尺' }}
              </span>
              <span v-else class="remark-none">-</span>
            </td>
            <td class="td-budget" @click.stop>{{ e.budget || getField(e.content, '预算区间') || '-' }}</td>
            <td>
              <span v-if="e.remark" class="remark-tag">已备注</span>
              <span v-else class="remark-none">-</span>
            </td>
            <td class="td-actions" @click.stop>
              <button class="act-icon" title="查看详情" @click="openDrawer(e)"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></button>
              <a :href="'tel:'+e.phone" class="act-icon" title="拨打电话"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg></a>
              <button class="act-icon" title="添加备注" @click="e._showRemark = true; openDrawer(e); $nextTick(() => $refs.drawerRemark?.focus())"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
              <button class="act-icon danger" title="删除" @click="handleDelete(e)"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        <span class="empty-icon">📋</span>
        <p>{{ list.length ? '未匹配到询价记录' : '暂无询价记录' }}</p>
      </div>
    </div>

    <!-- ===== 右侧详情抽屉 ===== -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="drawer" class="drawer-overlay" @click.self="closeDrawer">
          <div class="drawer-panel">
            <div class="drawer-head">
              <div>
                <h3>询价详情 #{{ drawer.id }}</h3>
                <span class="drawer-status" :class="drawer.isRead ? 'read' : 'unread'">{{ drawer.isRead ? '已读' : '未读' }}</span>
                <span v-if="drawer.needMeasure" class="drawer-status" :class="drawer.isMeasured ? 'measured' : 'unmeasured'">{{ drawer.isMeasured ? '已量尺' : '待量尺' }}</span>
              </div>
              <button class="drawer-close" @click="closeDrawer">✕</button>
            </div>

            <div class="drawer-body">
              <!-- 基本信息 -->
              <div class="drawer-section">
                <h4><span class="section-dash"></span>基本信息</h4>
                <div class="drawer-info">
                  <div class="di-row"><span class="di-label">姓名</span><span class="di-val">{{ drawer.name }}</span></div>
                  <div class="di-row"><span class="di-label">电话</span><a :href="'tel:'+drawer.phone" class="di-val di-link">{{ drawer.phone }}</a></div>
                  <div class="di-row"><span class="di-label">提交时间</span><span class="di-val">{{ formatDate(drawer.createTime) }}</span></div>
                  <div class="di-row"><span class="di-label">小区/楼盘</span><span class="di-val">{{ getField(drawer.content, '小区/楼盘') || getField(drawer.content, '小区') || '-' }}</span></div>
                  <div class="di-row"><span class="di-label">预算区间</span><span class="di-val">{{ drawer.budget || getField(drawer.content, '预算区间') || '-' }}</span></div>
                </div>
              </div>

              <!-- 项目信息 -->
              <div class="drawer-section" v-if="hasStructured(drawer.content)">
                <h4><span class="section-dash"></span>项目信息</h4>
                <div class="drawer-info">
                  <div class="di-row" v-for="f in parseFields(drawer.content)" :key="f.key">
                    <span class="di-label">{{ f.key }}</span>
                    <span class="di-val">{{ f.value }}</span>
                  </div>
                </div>
              </div>

              <!-- 补充说明 -->
              <div class="drawer-section" v-if="getField(drawer.content, '补充说明')">
                <h4><span class="section-dash"></span>补充说明</h4>
                <p class="drawer-note">{{ getField(drawer.content, '补充说明') }}</p>
              </div>

              <!-- 现场照片 -->
              <div class="drawer-section" v-if="drawerPhotos.length">
                <h4><span class="section-dash"></span>现场照片 ({{ drawerPhotos.length }}张)</h4>
                <div class="drawer-photos">
                  <img v-for="(p,i) in drawerPhotos" :key="i" :src="p" class="drawer-photo" @click="previewImg = p" />
                </div>
              </div>

              <!-- 跟进备注 -->
              <div class="drawer-section">
                <h4><span class="section-dash"></span>跟进备注</h4>
                <textarea ref="drawerRemark" v-model="drawerRemarkText" class="drawer-remark" placeholder="输入跟进备注..." @blur="saveDrawerRemark"></textarea>
              </div>
            </div>

            <!-- 底部固定操作 -->
            <div class="drawer-foot">
              <a :href="'tel:'+drawer.phone" class="drawer-btn call">📞 拨打电话</a>
              <button class="drawer-btn star" :class="{ on: drawer.isStarred }" @click="markDrawerStarred">{{ drawer.isStarred ? '⭐ 取消星标' : '☆ 设为星标' }}</button>
              <button v-if="drawer.needMeasure && !drawer.isMeasured" class="drawer-btn mark-measured" @click="markDrawerMeasured">📐 确认已量尺</button>
              <button v-if="drawer.needMeasure && drawer.isMeasured" class="drawer-btn undo-measured" @click="undoDrawerMeasured">↩ 取消量尺</button>
              <button v-if="!drawer.isRead" class="drawer-btn mark-read" @click="markDrawerRead">✓ 标为已读</button>
              <button class="drawer-btn del" @click="handleDrawerDelete">🗑 删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 图片预览 -->
    <div class="img-preview" v-if="previewImg" @click="previewImg = null">
      <img :src="previewImg" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getEnquiries, markRead, markUnread, markAllRead, deleteEnquiry, markMeasured, markStarred, markCompleted, restoreEntity } from '@/api/admin'
import { updateRemark } from '@/api/admin'
import { formatDate } from '@/utils/format'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoAction } from '@/composables/useUndoAction'
import RefreshButton from '@/components/RefreshButton.vue'

const list = ref([])
const searchText = ref('')
const keyword = ref('')
let searchTimer = null
watch(searchText, (val) => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { keyword.value = val }, 300)
})
const filterStatus = ref('all')
const filterDate = ref('all')
const drawer = ref(null)
const drawerRemarkText = ref('')
const previewImg = ref(null)
const checkedIds = ref([])
const sortField = ref('createTime')
const sortDir = ref('desc')

const FILTER_KEY = 'enquiries_filters'
let _restoring = false

function restoreFilters() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.keyword) { keyword.value = saved.keyword; searchText.value = saved.keyword }
    if (saved.filterStatus) filterStatus.value = saved.filterStatus
    if (saved.filterDate) filterDate.value = saved.filterDate
    if (saved.sortField) sortField.value = saved.sortField
    if (saved.sortDir) sortDir.value = saved.sortDir
    _restoring = false
  } catch {}
}

function syncFilters() {
  if (_restoring) return
  sessionStorage.setItem(FILTER_KEY, JSON.stringify({
    keyword: keyword.value,
    filterStatus: filterStatus.value,
    filterDate: filterDate.value,
    sortField: sortField.value,
    sortDir: sortDir.value,
  }))
}

watch([keyword, filterStatus, filterDate, sortField, sortDir], syncFilters)

// ===== Undo composables =====
const { handleDelete, handleBatchDelete } = useUndoDelete({
  deleteApi: deleteEnquiry,
  restoreApi: (id) => restoreEntity('enquiry', id),
  onSuccess: load,
  entityLabel: '询价',
})

const { handleBatchAction: handleBatchMarkRead } = useUndoAction({
  actionApi: markRead,
  reverseApi: markUnread,
  onSuccess: load,
  entityLabel: '询价',
  actionLabel: '已读',
})
const drawerRemark = ref(null)
const quickFilter = ref('')

const dateOptions = [
  { label: '全部', value: 'all' },
  { label: '今天', value: 'today' },
  { label: '近7天', value: '7days' },
  { label: '近30天', value: '30days' },
]

onMounted(() => { restoreFilters(); load() })

// ===== Stats =====
const stats = computed(() => {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const weekStart = todayStart - 7 * 86400000
  return {
    today: list.value.filter(e => e.createTime && new Date(e.createTime).getTime() >= todayStart).length,
    unread: list.value.filter(e => !e.isRead).length,
    week: list.value.filter(e => e.createTime && new Date(e.createTime).getTime() >= weekStart).length,
    unmeasured: list.value.filter(e => e.needMeasure && !e.isMeasured).length,
    measured: list.value.filter(e => e.isMeasured).length,
    starred: list.value.filter(e => e.isStarred).length,
    uncompleted: list.value.filter(e => !e.isCompleted).length,
    completed: list.value.filter(e => e.isCompleted).length,
  }
})

// ===== Filtered & sorted list =====
const filteredList = computed(() => {
  let arr = [...list.value]
  if (filterStatus.value === 'unread') arr = arr.filter(e => !e.isRead)
  if (filterStatus.value === 'read') arr = arr.filter(e => e.isRead)
  if (filterStatus.value === 'unmeasured') arr = arr.filter(e => e.needMeasure && !e.isMeasured)
  if (filterStatus.value === 'measured') arr = arr.filter(e => e.isMeasured)
  if (filterStatus.value === 'starred') arr = arr.filter(e => e.isStarred)
  if (filterStatus.value === 'uncompleted') arr = arr.filter(e => !e.isCompleted)
  if (filterStatus.value === 'completed') arr = arr.filter(e => e.isCompleted)
  // Quick filter from stat cards
  if (quickFilter.value === 'today') {
    const today = new Date(); today.setHours(0,0,0,0)
    arr = arr.filter(e => e.createTime && new Date(e.createTime) >= today)
  }
  if (quickFilter.value === 'unread') arr = arr.filter(e => !e.isRead)
  if (quickFilter.value === 'week') {
    const d = new Date(); d.setDate(d.getDate() - 7)
    arr = arr.filter(e => e.createTime && new Date(e.createTime) >= d)
  }
  if (quickFilter.value === 'measure') arr = arr.filter(e => e.needMeasure && !e.isMeasured)
  if (quickFilter.value === 'measured') arr = arr.filter(e => e.isMeasured)
  if (quickFilter.value === 'starred') arr = arr.filter(e => e.isStarred)
  if (quickFilter.value === 'uncompleted') arr = arr.filter(e => !e.isCompleted)
  if (quickFilter.value === 'completed') arr = arr.filter(e => e.isCompleted)
  // Date filter
  if (filterDate.value === 'today') {
    const today = new Date(); today.setHours(0,0,0,0)
    arr = arr.filter(e => e.createTime && new Date(e.createTime) >= today)
  }
  if (filterDate.value === '7days') {
    const d = new Date(); d.setDate(d.getDate() - 7)
    arr = arr.filter(e => e.createTime && new Date(e.createTime) >= d)
  }
  if (filterDate.value === '30days') {
    const d = new Date(); d.setDate(d.getDate() - 30)
    arr = arr.filter(e => e.createTime && new Date(e.createTime) >= d)
  }
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    arr = arr.filter(e => (e.name||'').toLowerCase().includes(kw) || (e.phone||'').toLowerCase().includes(kw) || (e.content||'').toLowerCase().includes(kw))
  }
  // Sort
  arr.sort((a, b) => {
    let va, vb
    switch (sortField.value) {
      case 'id': va = a.id; vb = b.id; break
      case 'name': va = a.name || ''; vb = b.name || ''; break
      case 'createTime': va = a.createTime || ''; vb = b.createTime || ''; break
      case 'area':
        va = (a.content||'').includes('10-20') ? 2 : (a.content||'').includes('20以上') ? 3 : 1
        vb = (b.content||'').includes('10-20') ? 2 : (b.content||'').includes('20以上') ? 3 : 1
        break
      default: return 0
    }
    const cmp = typeof va === 'string' ? va.localeCompare(vb) : (va||0) - (vb||0)
    return sortDir.value === 'asc' ? cmp : -cmp
  })
  return arr
})

const total = computed(() => filteredList.value.length)
const hasUnreadChecked = computed(() => filteredList.value.some(e => checkedIds.value.includes(e.id) && !e.isRead))
const allChecked = computed(() => filteredList.value.length > 0 && filteredList.value.every(e => checkedIds.value.includes(e.id)))

function toggleSort(field) {
  if (sortField.value === field) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortField.value = field; sortDir.value = 'asc' }
}
function sortArrow(field) { return sortField.value === field ? (sortDir.value === 'asc' ? '↑' : '↓') : '' }

function pickQuick(val) { filterDate.value = val }
function toggleAll() {
  if (allChecked.value) checkedIds.value = []
  else checkedIds.value = filteredList.value.map(e => e.id)
}
function toggleCheck(id) {
  const idx = checkedIds.value.indexOf(id)
  if (idx >= 0) checkedIds.value.splice(idx, 1)
  else checkedIds.value.push(id)
}

// ===== Content parsing =====
function getField(content, key) {
  if (!content) return ''
  const lines = content.split('\n')
  for (const line of lines) {
    if (line.startsWith(key + ':') || line.startsWith(key + '：')) {
      return line.substring(line.indexOf(':') > -1 ? line.indexOf(':') + 1 : line.indexOf('：') + 1).trim()
    }
  }
  return ''
}
function hasStructured(content) {
  if (!content) return false
  return content.split('\n').some(l => l.includes(':') && l.indexOf(':') > 1)
}
function parseFields(content) {
  if (!content) return []
  return content.split('\n').filter(l => l.includes(':') && !l.startsWith('data:')).map(l => {
    const idx = l.indexOf(':')
    return { key: l.substring(0, idx).trim(), value: l.substring(idx + 1).trim() }
  }).filter(f => f.key && f.value)
}

const drawerPhotos = computed(() => {
  if (!drawer.value?.content) return []
  const matches = drawer.value.content.match(/data:image\/[^;]+;base64,[A-Za-z0-9+/=]{20,}/g)
  return matches || []
})

// ===== Phone mask =====
function maskPhone(phone) {
  if (!phone || phone.length !== 11) return phone || '-'
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

// ===== Load =====
function load() {
  getEnquiries().then(r => {
    list.value = (r.data.data || []).map(e => ({ ...e, _remark: e.remark || '', _editing: false, _showRemark: false }))
  }).catch(() => { ElMessage.error('加载询价列表失败') })
}

// ===== Drawer =====
async function openDrawer(e) {
  drawer.value = { ...e }
  drawerRemarkText.value = e.remark || ''
  if (!e.isRead) {
    try { await markRead(e.id); e.isRead = 1; drawer.value.isRead = 1 } catch { ElMessage.error('标记已读失败') }
  }
}
function closeDrawer() {
  saveDrawerRemark()
  drawer.value = null
  load()
}
async function handleDrawerDelete() {
  if (!drawer.value) return
  await handleDelete(drawer.value)
  drawer.value = null
  load()
}
async function saveDrawerRemark() {
  if (!drawer.value) return
  if (drawerRemarkText.value === (drawer.value.remark || '')) return
  try { await updateRemark(drawer.value.id, drawerRemarkText.value); drawer.value.remark = drawerRemarkText.value } catch { ElMessage.error('保存备注失败') }
}
async function markDrawerRead() {
  if (!drawer.value || drawer.value.isRead) return
  try { await markRead(drawer.value.id); drawer.value.isRead = 1 } catch { ElMessage.error('标记已读失败') }
  load()
}
async function toggleMeasure(e) {
  const newVal = e.isMeasured ? 0 : 1
  try { await markMeasured(e.id, newVal); e.isMeasured = newVal } catch { ElMessage.error('操作失败') }
}
async function markDrawerMeasured() {
  if (!drawer.value || drawer.value.isMeasured) return
  try { await markMeasured(drawer.value.id, 1); drawer.value.isMeasured = 1 } catch { ElMessage.error('操作失败') }
  load()
}
async function undoDrawerMeasured() {
  if (!drawer.value || !drawer.value.isMeasured) return
  try { await markMeasured(drawer.value.id, 0); drawer.value.isMeasured = 0 } catch { ElMessage.error('操作失败') }
  load()
}
async function toggleStar(e) {
  const val = e.isStarred ? 0 : 1
  try { await markStarred(e.id, val); e.isStarred = val } catch { ElMessage.error('操作失败') }
}
async function toggleComplete(e) {
  const val = e.isCompleted ? 0 : 1
  try { await markCompleted(e.id, val); e.isCompleted = val } catch { ElMessage.error('操作失败') }
}
async function markDrawerStarred() {
  if (!drawer.value) return
  const val = drawer.value.isStarred ? 0 : 1
  try { await markStarred(drawer.value.id, val); drawer.value.isStarred = val } catch { ElMessage.error('操作失败') }
  load()
}

// ===== Actions =====
async function batchMarkRead() {
  const ids = [...checkedIds.value]
  if (!ids.length) return
  await handleBatchMarkRead(ids)
  checkedIds.value = []
}
async function batchDelete() {
  const ids = [...checkedIds.value]
  if (!ids.length) return
  await handleBatchDelete(ids)
  checkedIds.value = []
}
async function exportExcel() {
  const data = filteredList.value.map(e => ({
    ID: e.id, 姓名: e.name, 电话: e.phone,
    房屋类型: getField(e.content, '房屋类型'),
    窗户数量: getField(e.content, '窗户数量'),
    楼层: getField(e.content, '所在楼层'),
    是否量尺: getField(e.content, '是否已量尺'),
    预算: e.budget || getField(e.content, '预算区间') || '-',
    上门时间: getField(e.content, '期望上门时间'),
    小区: getField(e.content, '小区/楼盘') || getField(e.content, '小区'),
    说明: getField(e.content, '补充说明'),
    备注: e.remark || '',
    时间: formatDate(e.createTime),
    状态: e.isRead ? '已读' : '未读',
    星标: e.isStarred ? '⭐' : '',
    量尺: e.isMeasured ? '已量尺' : (e.needMeasure ? '待量尺' : '-'),
    完单: e.isCompleted ? '✓' : ''
  }))
  const csv = ['﻿' + Object.keys(data[0]||{}).join(','), ...data.map(r => Object.values(r).map(v => `"${(v||'').toString().replace(/"/g,'""')}"`).join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const { saveFile } = await import('@/utils/download')
  await saveFile(blob, `询价导出_${new Date().toISOString().slice(0,10)}.csv`, { successMsg: '导出成功' })
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

.enquiries-page { padding: 20px 24px 40px; background: #f5f4f1; min-height: 100%; }

/* ===== Stats Cards ===== */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
.stat-card {
  background: #fff; border-radius: 10px; padding: 18px 20px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  cursor: pointer; transition: all .2s; border: 2px solid transparent;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,.08); border-color: var(--border-gold); }
.stat-card.active { border-color: var(--gold-primary); background: var(--gold-bg); }
.stat-card.highlight { border-left: 3px solid var(--gold-primary); }
.stat-icon { font-size: 28px; }
.stat-body { display: flex; flex-direction: column; }
.stat-num { font-size: 26px; font-weight: 700; color: var(--gold-primary); letter-spacing: -1px; line-height: 1.1; }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

/* ===== Toolbar ===== */
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; gap: 12px; flex-wrap: wrap; }
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.filter-tabs { display: flex; gap: 0; }
.filter-tabs button {
  padding: 8px 18px; background: none; border: none; border-bottom: 2px solid transparent;
  font-size: 13px; color: #999; cursor: pointer; transition: all .2s; font-family: inherit;
}
.filter-tabs button:hover { color: #333; }
.filter-tabs button.active { color: var(--gold-primary); border-bottom-color: var(--gold-primary); font-weight: 600; }

.date-capsules { display: flex; gap: 4px; margin-left: 8px; }
.date-cap {
  padding: 5px 12px; border: 1px solid #e0e0e0; border-radius: 16px;
  background: #fff; font-size: 11px; color: #999; cursor: pointer; transition: all .15s;
  font-family: inherit;
}
.date-cap:hover { border-color: var(--gold-primary); color: var(--gold-primary); }
.date-cap.active { background: var(--gold-bg); border-color: var(--gold-primary); color: var(--gold-primary); font-weight: 600; }

.search-wrap { display: flex; align-items: center; gap: 6px; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 6px 12px; }
.search-wrap svg { color: #bbb; flex-shrink: 0; }
.search-input { border: none; outline: none; font-size: 12px; color: #333; width: 160px; font-family: inherit; }

.count-text { font-size: 12px; color: #999; }
.btn-batch { padding: 6px 12px; border-radius: 6px; border: 1px solid #e0e0e0; background: #fff; font-size: 11px; cursor: pointer; color: #666; transition: all .15s; font-family: inherit; }
.btn-batch:hover { border-color: var(--gold-primary); color: var(--gold-primary); }
.btn-batch.danger:hover { border-color: #e74c3c; color: #e74c3c; }
.btn-batch.export { background: var(--gold-primary); color: #1A1A1A; border-color: transparent; font-weight: 600; }
.btn-batch.export:hover { background: #d4b885; }

/* ===== Table ===== */
.table-card { background: #fff; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,.04); overflow: auto; }
.enq-table { width: 100%; border-collapse: collapse; min-width: 900px; }
.enq-table th {
  background: #243447; font-weight: 600; color: rgba(255,255,255,0.85); font-size: 12px; padding: 12px 14px;
  text-align: left; border-bottom: 1px solid rgba(255,255,255,0.08); white-space: nowrap; user-select: none;
}
.enq-table th.sortable { cursor: pointer; }
.enq-table th.sortable:hover { color: var(--gold-primary); }
.sort-arrow { font-size: 10px; margin-left: 2px; }
.enq-table td { padding: 12px 14px; font-size: 13px; border-bottom: 1px solid #f8f8f8; color: #444; vertical-align: middle; }
.enq-table tbody tr { cursor: pointer; transition: background .12s; }
.enq-table tbody tr:hover { background: #fafbfc; }
.enq-table tbody tr.unread { background: rgba(197,162,101,.03); border-left: 3px solid var(--gold-primary); }
.enq-table tbody tr.unread:hover { background: rgba(197,162,101,.06); }

.th-check { width: 36px; padding-left: 14px !important; }
.td-check { width: 36px; padding-left: 14px !important; }
.td-check input, .th-check input { accent-color: var(--gold-primary); cursor: pointer; }
.td-id { color: var(--text-placeholder); font-size: 12px; width: 60px; }
.th-star { width: 32px; text-align: center; font-size: 12px; }
.td-star { width: 32px; text-align: center; }
.star-btn { background: none; border: none; cursor: pointer; font-size: 16px; padding: 2px; transition: transform .15s; }
.star-btn:hover { transform: scale(1.3); }
.star-btn:not(.on) { opacity: .3; }
.th-completed { width: 50px; text-align: center; }
.td-completed { text-align: center; }
.complete-btn { width: 30px; height: 30px; border-radius: 50%; border: 2px solid #ddd; background: none; cursor: pointer; font-size: 16px; color: #ccc; transition: all .15s; display: inline-flex; align-items: center; justify-content: center; }
.complete-btn:hover { border-color: #52c41a; color: #52c41a; }
.complete-btn.done { border-color: #52c41a; background: #52c41a; color: #fff; }

/* Customer cell */
.customer-cell { display: flex; flex-direction: column; gap: 1px; }
.cust-name { font-weight: 600; color: var(--text-body); font-size: 13px; }
.cust-phone { font-size: 11px; color: var(--text-muted); }

/* Project cell */
.project-cell { display: flex; flex-direction: column; gap: 1px; }
.proj-type { font-size: 12px; color: #555; }
.proj-count { font-size: 11px; color: #999; }

.cell-val { font-size: 13px; color: #555; }
.cell-na { color: var(--text-placeholder); font-style: italic; }

.td-time { font-size: 12px; color: #999; white-space: nowrap; }
.td-budget { font-size: 12px; color: #666; white-space: nowrap; }

/* Status tags */
.status-tag { display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 600; }
.status-tag.unread { background: rgba(197,162,101,.12); color: var(--gold-primary); }
.status-tag.read { background: #f0f0f0; color: #aaa; }

.remark-tag { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; background: rgba(197,162,101,.12); color: var(--gold-primary); }
.remark-none { color: var(--text-placeholder); font-size: 13px; }

.measure-tag { display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 600; cursor: pointer; transition: all .15s; }
.measure-tag.pending { background: #fff7e6; color: #fa8c16; border: 1px solid #ffd591; }
.measure-tag.pending:hover { background: #ffe7ba; }
.measure-tag.done { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.measure-tag.done:hover { background: #d9f7be; }

.tab-divider { width: 1px; height: 16px; background: #e0e0e0; margin: 0 4px; align-self: center; }

/* Action icons */
.td-actions { display: flex; gap: 2px; align-items: center; white-space: nowrap; }
.act-icon {
  width: 30px; height: 30px; border: none; background: none; border-radius: 6px;
  color: #bbb; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .15s; text-decoration: none;
}
.act-icon:hover { color: var(--gold-primary); background: rgba(197,162,101,.08); }
.act-icon.danger:hover { color: #e74c3c; background: rgba(231,76,60,.06); }

.empty-state { text-align: center; padding: 60px 0; color: #ccc; }
.empty-icon { font-size: 40px; display: block; margin-bottom: 8px; }

/* ===== Drawer ===== */
.drawer-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,.35); display: flex; justify-content: flex-end; }
.drawer-panel {
  width: 480px; max-width: 95vw; height: 100%; background: #fff;
  display: flex; flex-direction: column; box-shadow: -4px 0 32px rgba(0,0,0,.12);
}
.drawer-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 20px 24px; background: var(--bg-nav); color: #fff;
  border-bottom: 2px solid var(--gold-primary); flex-shrink: 0;
}
.drawer-head h3 { font-size: 17px; font-weight: 600; }
.drawer-status { font-size: 11px; display: inline-block; padding: 2px 10px; border-radius: 12px; margin-top: 4px; }
.drawer-status.unread { background: rgba(197,162,101,.2); color: var(--gold-primary); }
.drawer-status.read { background: rgba(255,255,255,.1); color: rgba(255,255,255,.5); }
.drawer-status.unmeasured { background: rgba(250,140,22,.2); color: #ffa940; }
.drawer-status.measured { background: rgba(82,196,26,.2); color: #73d13d; }
.drawer-close {
  width: 30px; height: 30px; border: 1px solid #e74c3c; background: rgba(231, 76, 60, .15); border-radius: 50%;
  color: #e74c3c; font-size: 14px; font-weight: 700; cursor: pointer; transition: all .15s;
  display: flex; align-items: center; justify-content: center;
}
.drawer-close:hover { background: #e74c3c; color: #fff; }

.drawer-body { flex: 1; overflow-y: auto; padding: 20px 24px; }

.drawer-section { margin-bottom: 22px; }
.drawer-section h4 {
  font-size: 13px; font-weight: 600; color: #1a1a1a; margin-bottom: 12px;
  display: flex; align-items: center; gap: 8px;
}
.section-dash { display: inline-block; width: 16px; height: 2px; background: var(--gold-primary); border-radius: 1px; }

.drawer-info { display: flex; flex-direction: column; gap: 6px; }
.di-row { display: flex; align-items: baseline; gap: 10px; padding: 6px 0; }
.di-label { font-size: 12px; color: #999; min-width: 56px; flex-shrink: 0; }
.di-val { font-size: 14px; color: #333; font-weight: 500; }
.di-link { color: var(--gold-primary); text-decoration: none; }

.drawer-note { font-size: 13px; color: #666; line-height: 1.7; white-space: pre-wrap; }

.drawer-photos { display: flex; gap: 8px; flex-wrap: wrap; }
.drawer-photo {
  width: 80px; height: 80px; object-fit: cover; border-radius: 6px;
  border: 1px solid #e0e0e0; cursor: pointer;
}
.drawer-photo:hover { border-color: var(--gold-primary); }

.drawer-remark {
  width: 100%; padding: 10px 14px; border: 1px solid #e0e0e0; border-radius: 8px;
  font-size: 13px; outline: none; font-family: inherit; resize: vertical; min-height: 80px;
  color: #333;
}
.drawer-remark:focus { border-color: var(--gold-primary); }

.drawer-foot {
  display: flex; gap: 8px; padding: 14px 24px;
  border-top: 1px solid #f0f0f0; background: #fafbfc; flex-shrink: 0;
}
.drawer-btn {
  flex: 1; padding: 10px; border-radius: 8px; border: 1px solid #e0e0e0;
  background: #fff; font-size: 13px; cursor: pointer; text-align: center;
  font-family: inherit; text-decoration: none; color: #555; transition: all .15s;
}
.drawer-btn.call:hover { border-color: #4a90d9; color: #4a90d9; }
.drawer-btn.mark-read { background: var(--gold-primary); color: #1A1A1A; border-color: transparent; font-weight: 600; }
.drawer-btn.mark-read:hover { background: #d4b885; }
.drawer-btn.mark-measured { background: #52c41a; color: #fff; border-color: transparent; font-weight: 600; }
.drawer-btn.mark-measured:hover { background: #73d13d; }
.drawer-btn.undo-measured { background: #fff7e6; color: #fa8c16; border-color: #ffd591; }
.drawer-btn.undo-measured:hover { background: #ffe7ba; }
.drawer-btn.star { font-weight: 500; }
.drawer-btn.star.on { background: #fff7e6; border-color: #ffd591; color: #fa8c16; }
.drawer-btn.star.on:hover { background: #ffe7ba; }
.drawer-btn.del:hover { border-color: #e74c3c; color: #e74c3c; }

/* Drawer animation */
.drawer-enter-active { transition: opacity .25s ease; }
.drawer-enter-active .drawer-panel { animation: slideIn .3s cubic-bezier(.4,0,.2,1) both; }
.drawer-leave-active { transition: opacity .2s ease; }
.drawer-leave-active .drawer-panel { animation: slideIn .25s cubic-bezier(.4,0,.2,1) reverse both; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
@keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }

/* Image preview */
.img-preview { position: fixed; inset: 0; z-index: 2000; background: rgba(0,0,0,.92); display: flex; align-items: center; justify-content: center; cursor: pointer; }
.img-preview img { max-width: 90vw; max-height: 90vh; object-fit: contain; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .enquiries-page { padding: 12px; }
  .stats-row { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .stats-row .stat-card:last-child { grid-column: span 2; }
  .stat-card { padding: 12px 14px; }
  .stat-num { font-size: 20px; }
  .toolbar { flex-direction: column; align-items: flex-start; }
  .drawer-panel { width: 100vw; }
}
</style>

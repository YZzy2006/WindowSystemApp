<template>
  <div class="page admin-page">
    <!-- ===== 顶部安全概览条 ===== -->
    <div class="security-hero">
      <div class="hero-left">
        <div class="avatar-circle">{{ avatarLetter }}</div>
        <div class="hero-info">
          <h2 class="hero-name">{{ adminName }}</h2>
          <span class="hero-tag">管理员</span>
        </div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <svg class="hero-gauge" viewBox="0 0 100 56">
            <path :d="gaugeBg" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="8" stroke-linecap="round" />
            <path :d="gaugeArc(securityScore)" fill="none" :stroke="securityColor" stroke-width="8" stroke-linecap="round" />
          </svg>
          <div class="hero-stat-text">
            <span class="hero-stat-val" :style="{ color: securityColor }">{{ securityScore }}</span>
            <span class="hero-stat-label">安全评分</span>
          </div>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-val">{{ consecutiveDays }}</span>
          <span class="hero-stat-label">连续登录天数</span>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-val">{{ recentLogin.time }}</span>
          <span class="hero-stat-label">{{ recentLogin.ip }} · {{ recentLogin.browser }}</span>
        </div>
      </div>
    </div>

    <div class="page-header">
      <h2>账号与安全</h2>
      <p class="page-desc">{{ todayStr }} · 顺居门业管理系统</p>
    </div>

    <!-- ===== 系统信息 ===== -->
    <section class="section">
      <h3 class="section-title"><span class="title-dot"></span>系统信息</h3>
      <div class="info-grid" v-if="sysInfo">
        <div class="info-card accent-gold">
          <span class="info-icon">🖥</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.osName }}</span>
            <span class="info-label">操作系统</span>
          </div>
        </div>
        <div class="info-card accent-orange">
          <span class="info-icon">☕</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.javaVersion }}</span>
            <span class="info-label">Java 版本</span>
          </div>
        </div>
        <div class="info-card accent-blue">
          <span class="info-icon">⏱</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.uptime }}</span>
            <span class="info-label">运行时长</span>
          </div>
        </div>
        <div class="info-card accent-purple">
          <span class="info-icon">🧠</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.usedMemory }} / {{ sysInfo.maxMemory }}</span>
            <span class="info-label">内存使用 ({{ sysInfo.cpuCores }} 核)</span>
          </div>
        </div>
        <div class="info-card accent-gold">
          <span class="info-icon">🪟</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.productCount }}</span>
            <span class="info-label">产品总数</span>
          </div>
        </div>
        <div class="info-card accent-orange">
          <span class="info-icon">📁</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.categoryCount }}</span>
            <span class="info-label">产品分类</span>
          </div>
        </div>
        <div class="info-card accent-blue">
          <span class="info-icon">📸</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.caseCount }}</span>
            <span class="info-label">案例展示</span>
          </div>
        </div>
        <div class="info-card accent-purple">
          <span class="info-icon">📋</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.enquiryCount }}</span>
            <span class="info-label">客户询价</span>
          </div>
        </div>
        <div class="info-card accent-green">
          <span class="info-icon">🗄</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.dbVersion || '-' }}</span>
            <span class="info-label">数据库版本</span>
          </div>
        </div>
        <div class="info-card accent-green">
          <span class="info-icon">💾</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.dbSize || '-' }}</span>
            <span class="info-label">数据库大小</span>
          </div>
        </div>
        <div class="info-card accent-green">
          <span class="info-icon">📊</span>
          <div class="info-body">
            <span class="info-val">{{ sysInfo.tableCount ?? '-' }}</span>
            <span class="info-label">数据表数量</span>
          </div>
        </div>
      </div>
      <p v-else class="empty-text">加载中...</p>
    </section>

    <!-- ===== 登录记录 + 修改密码 ===== -->
    <div class="bottom-grid">
      <!-- 登录记录 -->
      <section class="section">
        <h3 class="section-title"><span class="title-dot"></span>登录记录</h3>
        <div class="card heatmap-card" v-if="myLogs.length">
          <div class="heatmap-header">
            <span class="heatmap-title">近 6 个月登录活跃</span>
            <span class="heatmap-total">共 {{ myLogs.length }} 次成功登录</span>
          </div>
          <div class="heatmap-wrap">
            <div class="heatmap-days">
              <span>一</span><span>三</span><span>五</span>
            </div>
            <div class="heatmap-grid">
              <div class="heatmap-col" v-for="(col, ci) in heatmapData.grid" :key="ci">
                <div class="heatmap-month" v-if="col.monthLabel">{{ col.monthLabel }}</div>
                <div class="heatmap-cell" v-else></div>
                <div
                  v-for="(cell, di) in col.days"
                  :key="di"
                  class="heatmap-cell"
                  :class="'level-' + cell.level"
                  :title="cell.date + ': ' + cell.count + ' 次'"
                />
              </div>
            </div>
          </div>
          <div class="heatmap-legend">
            <span class="legend-label">少</span>
            <span class="legend-cell level-0"></span>
            <span class="legend-cell level-1"></span>
            <span class="legend-cell level-2"></span>
            <span class="legend-cell level-3"></span>
            <span class="legend-cell level-4"></span>
            <span class="legend-label">多</span>
          </div>
        </div>
        <p v-else class="empty-text">暂无登录记录</p>

        <!-- 登录明细时间线 -->
        <div class="log-timeline" v-if="logs.length" style="margin-top: 16px">
          <div
            v-for="log in logs.slice(0, logShowCount)"
            :key="log.id"
            class="log-entry"
            :class="log.status ? 'entry-ok' : 'entry-fail'"
          >
            <div class="entry-time">{{ formatSmartTime(log.loginTime) }}</div>
            <div class="entry-body">
              <div class="entry-row">
                <span class="entry-user">{{ log.username || '-' }}</span>
                <span class="entry-status" :class="log.status ? 'st-ok' : 'st-fail'">
                  {{ log.status ? '登录成功' : '登录失败' }}
                </span>
              </div>
              <div class="entry-meta">
                <span>{{ uaIcon(log.userAgent) }} {{ parseUA(log.userAgent) }}</span>
                <span class="meta-sep">·</span>
                <span class="mono">{{ log.loginIp || '-' }}</span>
                <template v-if="!log.status && log.failReason">
                  <span class="meta-sep">·</span>
                  <span class="entry-reason">{{ log.failReason }}</span>
                </template>
              </div>
              <div class="entry-remark" v-if="log.remark || editingRemarkId === log.id">
                <input
                  v-if="editingRemarkId === log.id"
                  class="remark-inline-input"
                  :value="log.remark || ''"
                  placeholder="输入备注..."
                  @blur="saveLogRemark(log, $event.target.value); editingRemarkId = null"
                  @keydown.enter="$event.target.blur()"
                  autofocus
                />
                <span v-else class="remark-text" @click="editingRemarkId = log.id">
                  {{ log.remark }}
                </span>
              </div>
            </div>
            <button
              v-if="!log.remark && editingRemarkId !== log.id"
              class="remark-add-btn"
              @click="editingRemarkId = log.id"
              title="添加备注"
            >+</button>
          </div>
          <button
            v-if="logs.length > logShowCount"
            class="load-more-btn"
            @click="logShowCount += 20"
          >
            加载更多 (还有 {{ logs.length - logShowCount }} 条)
          </button>
        </div>
      </section>

      <!-- 修改密码 -->
      <section class="section">
        <h3 class="section-title"><span class="title-dot"></span>修改密码</h3>
        <div class="pwd-card">
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>原密码</label>
              <input v-model="form.oldPassword" type="password" placeholder="请输入原密码" required autocomplete="current-password" />
            </div>
            <div class="form-group">
              <label>新密码</label>
              <input v-model="form.newPassword" type="password" placeholder="至少8位，需包含字母和数字" required autocomplete="new-password" />
              <div class="pwd-gauge" v-if="form.newPassword">
                <svg class="gauge-svg" viewBox="0 0 120 68">
                  <path :d="gaugeBg" fill="none" stroke="#f0f0f0" stroke-width="10" stroke-linecap="round" />
                  <path :d="gaugeArc(passwordStrength.percent)" fill="none" :stroke="passwordStrength.color" stroke-width="10" stroke-linecap="round" />
                </svg>
                <div class="gauge-label" :style="{ color: passwordStrength.color }">{{ passwordStrength.label }}</div>
              </div>
            </div>
            <div class="form-group">
              <label>确认新密码</label>
              <input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" required autocomplete="new-password" />
            </div>
            <button type="submit" class="btn-primary" :disabled="loading">
              {{ loading ? '提交中...' : '确认修改' }}
            </button>
            <p v-if="msg" :class="['msg', msgType]">{{ msg }}</p>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { changePassword, getSystemInfo, getLoginLogs, updateLoginLogRemark } from '@/api/admin'
import { ElMessage } from 'element-plus'

const sysInfo = ref(null)
const logs = ref([])
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)
const msg = ref('')
const msgType = ref('')
const adminName = ref(sessionStorage.getItem('adminName') || '管理员')
const logShowCount = ref(20)
const editingRemarkId = ref(null)

const todayStr = (() => {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日`
})()

const avatarLetter = computed(() => (adminName.value || 'A').charAt(0).toUpperCase())

/* ---- 密码强度 ---- */
const passwordStrength = computed(() => {
  const p = form.newPassword
  if (!p) return { level: 'weak', percent: 0, label: '', color: '#ff4d4f' }
  let score = 0
  if (p.length >= 8) score++
  if (p.length >= 12) score++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^a-zA-Z0-9]/.test(p)) score++
  if (score <= 1) return { level: 'weak', percent: 20, label: '弱', color: '#ff4d4f' }
  if (score <= 2) return { level: 'fair', percent: 40, label: '一般', color: '#faad14' }
  if (score <= 3) return { level: 'medium', percent: 60, label: '中等', color: '#1677ff' }
  if (score <= 4) return { level: 'strong', percent: 80, label: '强', color: '#52c41a' }
  return { level: 'great', percent: 100, label: '非常强', color: '#13c2c2' }
})

/* ---- 当前用户的日志 ---- */
const myLogs = computed(() => logs.value.filter(l => l.username === adminName.value))

/* ---- 安全评分 ---- */
const securityScore = computed(() => {
  const list = myLogs.value
  if (!list.length) return 50
  const recent = list.slice(0, 30)
  const successRate = recent.filter(l => l.status).length / recent.length
  let score = 50
  if (successRate >= 0.9) score += 30
  else if (successRate >= 0.7) score += 20
  else if (successRate >= 0.5) score += 10
  const now = Date.now()
  const hasRecent = recent.some(l => now - new Date(l.loginTime).getTime() < 86400000)
  if (hasRecent) score += 20
  return Math.min(score, 100)
})
const securityColor = computed(() => {
  if (securityScore.value >= 80) return '#52c41a'
  if (securityScore.value >= 60) return '#C5A265'
  return '#faad14'
})

/* ---- 连续登录天数 ---- */
const consecutiveDays = computed(() => {
  const list = myLogs.value
  if (!list.length) return 0
  const dates = [...new Set(list.map(l => {
    const d = new Date(l.loginTime)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }))].sort().reverse()
  let count = 0
  const now = new Date()
  let check = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  for (const d of dates) {
    const checkStr = `${check.getFullYear()}-${String(check.getMonth() + 1).padStart(2, '0')}-${String(check.getDate()).padStart(2, '0')}`
    if (d === checkStr) {
      count++
      check.setDate(check.getDate() - 1)
    } else if (d < checkStr) break
  }
  return count
})

/* ---- 最近登录 ---- */
const recentLogin = computed(() => {
  const recent = myLogs.value.find(l => l.status)
  if (!recent) return { time: '无记录', ip: '-', browser: '-' }
  return {
    time: formatDate(recent.loginTime),
    ip: recent.loginIp || '-',
    browser: parseUA(recent.userAgent)
  }
})

/* ---- 热力图 ---- */
const heatmapData = computed(() => {
  const list = myLogs.value.filter(l => l.status)
  const countMap = {}
  list.forEach(l => {
    const d = new Date(l.loginTime)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    countMap[key] = (countMap[key] || 0) + 1
  })
  const today = new Date()
  const endDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const startDate = new Date(endDate)
  startDate.setMonth(startDate.getMonth() - 6)
  const firstColDate = new Date(startDate)
  firstColDate.setDate(firstColDate.getDate() - (startDate.getDay() === 0 ? 6 : startDate.getDay() - 1))
  const monthNames = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
  const levelOf = (n) => { if (n >= 5) return 4; if (n >= 3) return 3; if (n >= 2) return 2; if (n >= 1) return 1; return 0 }
  const grid = []
  const cur = new Date(firstColDate)
  let prevMonth = -1
  while (cur <= endDate) {
    const col = { monthLabel: '', days: [] }
    const colMonth = cur.getMonth()
    if (cur.getDay() === 1 && colMonth !== prevMonth) {
      col.monthLabel = monthNames[colMonth]
      prevMonth = colMonth
    }
    for (let r = 0; r < 7; r++) {
      if (cur > endDate) {
        col.days.push({ date: '', count: 0, level: 0 })
      } else {
        const key = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}-${String(cur.getDate()).padStart(2, '0')}`
        const count = countMap[key] || 0
        col.days.push({ date: key, count, level: levelOf(count) })
      }
      cur.setDate(cur.getDate() + 1)
    }
    grid.push(col)
  }
  return { grid, total: list.length }
})

/* ---- 仪表盘弧线 ---- */
const gaugeBg = 'M 10 56 A 40 40 0 0 1 90 56'
function gaugeArc(percent) {
  const clamped = Math.max(0, Math.min(100, percent))
  const angle = Math.PI * (1 - clamped / 100)
  const x = 50 + 40 * Math.cos(angle)
  const y = 56 - 40 * Math.sin(angle)
  const large = clamped > 50 ? 1 : 0
  return `M 10 56 A 40 40 0 ${large} 1 ${x.toFixed(2)} ${y.toFixed(2)}`
}

/* ---- 工具函数 ---- */
function formatDate(val) {
  if (!val) return '-'
  if (typeof val === 'string' && val.length >= 10) return val.substring(0, 10)
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatDateTime(val) {
  if (!val) return '-'
  if (typeof val === 'string' && val.length >= 16) return val.substring(0, 16).replace('T', ' ')
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatSmartTime(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const diff = (today - target) / 86400000
  if (diff === 0) return `今天 ${time}`
  if (diff === 1) return `昨天 ${time}`
  if (diff === 2) return `前天 ${time}`
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`
}

function parseUA(ua) {
  if (!ua) return '-'
  if (ua.includes('Edg')) return 'Edge'
  if (ua.includes('Chrome') && !ua.includes('Edg')) return 'Chrome'
  if (ua.includes('Firefox')) return 'Firefox'
  if (ua.includes('Safari') && !ua.includes('Chrome')) return 'Safari'
  if (ua.includes('MSIE') || ua.includes('Trident')) return 'IE'
  return ua.substring(0, 40) + '...'
}

function uaIcon(ua) {
  if (!ua) return '🌐'
  if (ua.includes('Edg')) return '🔷'
  if (ua.includes('Chrome')) return '🟡'
  if (ua.includes('Firefox')) return '🦊'
  if (ua.includes('Safari')) return '🧭'
  return '🌐'
}

/* ---- 初始化 ---- */
onMounted(async () => {
  getSystemInfo().then(r => { sysInfo.value = r.data.data }).catch(() => { ElMessage.error('加载系统信息失败') })
  getLoginLogs().then(r => { logs.value = r.data.data || [] }).catch(() => { ElMessage.error('加载登录日志失败') })
})

/* ---- 修改密码 ---- */
async function handleSubmit() {
  loading.value = true; msg.value = ''
  if (form.newPassword !== form.confirmPassword) {
    msg.value = '两次输入的新密码不一致'
    msgType.value = 'error'
    loading.value = false
    return
  }
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    msg.value = '密码修改成功，即将跳转到登录页...'
    msgType.value = 'success'
    form.oldPassword = ''; form.newPassword = ''; form.confirmPassword = ''
    setTimeout(() => { sessionStorage.removeItem('token'); window.location.hash = '#/admin/login' }, 1500)
  } catch (e) {
    msg.value = e?.message || '修改失败'
    msgType.value = 'error'
  } finally { loading.value = false }
}

/* ---- 更新备注 ---- */
async function saveLogRemark(log, val) {
  const newVal = val.trim()
  if (newVal === (log.remark || '')) return
  try {
    await updateLoginLogRemark(log.id, newVal)
    log.remark = newVal
  } catch { ElMessage.error('保存备注失败') }
}
</script>

<style scoped>
/* ===== Security Hero Bar ===== */
.security-hero {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 14px; padding: 28px 36px;
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 28px;
}
.hero-left { display: flex; align-items: center; gap: 18px; }
.avatar-circle {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #C5A265, #8B6914);
  color: #fff; font-size: 24px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.hero-name { font-size: 20px; font-weight: 700; color: #fff; margin: 0; }
.hero-tag {
  display: inline-block; padding: 2px 10px; border-radius: 10px;
  font-size: 11px; font-weight: 600;
  background: rgba(197,162,101,0.2); color: #C5A265; margin-top: 4px;
}
.hero-stats { display: flex; align-items: center; gap: 48px; }
.hero-stat { display: flex; flex-direction: column; align-items: center; gap: 6px; position: relative; }
.hero-stat-text { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.hero-stat-val { font-size: 26px; font-weight: 800; color: #C5A265; line-height: 1.2; }
.hero-stat-label { font-size: 11px; color: rgba(255,255,255,0.4); }
.hero-gauge { width: 100px; height: 56px; position: absolute; top: -28px; pointer-events: none; }

/* ===== Page Header ===== */
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.page-desc { font-size: 13px; color: #999; margin-top: 4px; }

/* ===== Section Titles ===== */
.section { margin-bottom: 28px; }
.section-title {
  font-size: 15px; font-weight: 600; color: #1a1a1a; margin-bottom: 16px;
  display: flex; align-items: center; gap: 8px;
}
.title-dot { display: inline-block; width: 4px; height: 16px; background: var(--gold-primary, #C5A265); border-radius: 2px; }

/* ===== System Info Grid (Dashboard Style) ===== */
.info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.info-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px; padding: 20px 22px;
  display: flex; align-items: center; gap: 14px;
  border-left: 4px solid transparent;
  transition: transform .2s, box-shadow .2s;
}
.info-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,.15); }
.info-card.accent-gold { border-left-color: #C5A265; }
.info-card.accent-orange { border-left-color: #fa8c16; }
.info-card.accent-blue { border-left-color: #1677ff; }
.info-card.accent-purple { border-left-color: #722ed1; }
.info-card.accent-green { border-left-color: #52c41a; }
.info-icon { font-size: 28px; flex-shrink: 0; filter: drop-shadow(0 2px 4px rgba(0,0,0,.2)); }
.info-body { display: flex; flex-direction: column; min-width: 0; }
.info-val {
  font-size: 15px; font-weight: 700; color: #fff;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.info-label { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 3px; letter-spacing: 0.3px; }
.empty-text { font-size: 13px; color: #bbb; text-align: center; padding: 20px 0; }

/* ===== Bottom Grid ===== */
.bottom-grid { display: grid; grid-template-columns: 1fr 380px; gap: 28px; }

/* ===== Login Log Timeline ===== */
.log-timeline { display: flex; flex-direction: column; gap: 0; }
.log-entry {
  display: flex; align-items: flex-start; gap: 14px; padding: 14px 18px;
  border-left: 3px solid #e8e8e8; margin-left: 68px; position: relative;
  transition: background .15s;
}
.log-entry:hover { background: #fafbfc; }
.log-entry.entry-ok { border-left-color: #b7eb8f; }
.log-entry.entry-fail { border-left-color: #ffccc7; }
.entry-time {
  position: absolute; left: -82px; top: 14px;
  font-size: 12px; color: #999; white-space: nowrap; width: 72px; text-align: right;
}
.entry-body { flex: 1; min-width: 0; }
.entry-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.entry-user { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.entry-status { font-size: 11px; font-weight: 600; padding: 1px 8px; border-radius: 8px; }
.st-ok { background: #f6ffed; color: #52c41a; }
.st-fail { background: #fff2f0; color: #ff4d4f; }
.entry-meta { font-size: 12px; color: #999; display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.entry-meta .mono { font-family: "SF Mono", Consolas, Monaco, monospace; letter-spacing: 0.3px; }
.meta-sep { color: #ddd; }
.entry-reason { color: #ff4d4f; }
.entry-remark { margin-top: 6px; }
.remark-text {
  font-size: 12px; color: #666; background: #f5f5f5; padding: 2px 8px;
  border-radius: 4px; cursor: pointer; transition: background .15s;
}
.remark-text:hover { background: #eee; }
.remark-inline-input {
  font-size: 12px; padding: 3px 8px; border: 1px solid #d9d9d9; border-radius: 4px;
  width: 200px; outline: none; font-family: inherit;
}
.remark-inline-input:focus { border-color: var(--gold-primary, #C5A265); }
.remark-add-btn {
  background: none; border: none; color: #ccc; font-size: 16px; cursor: pointer;
  padding: 0 4px; line-height: 1; transition: color .15s; flex-shrink: 0;
}
.remark-add-btn:hover { color: var(--gold-primary, #C5A265); }
.load-more-btn {
  margin: 12px 0 0 68px; padding: 10px; background: #fff; border: 1px solid #e8e8e8;
  border-radius: 8px; font-size: 13px; color: #666; cursor: pointer; transition: all .15s;
  text-align: center;
}
.load-more-btn:hover { border-color: var(--gold-primary, #C5A265); color: var(--gold-primary, #C5A265); }

/* Status Dot */
.status-dot-wrap {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 500; white-space: nowrap;
}
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-ok { background: #52c41a; box-shadow: 0 0 6px rgba(82,196,26,.4); }
.dot-fail { background: #ff4d4f; box-shadow: 0 0 6px rgba(255,77,79,.4); }

/* ===== Heatmap ===== */
.heatmap-card { padding: 20px 24px; border-radius: 12px; }
.heatmap-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px; }
.heatmap-title { font-size: 13px; font-weight: 600; color: #1a1a1a; }
.heatmap-total { font-size: 12px; color: #999; }
.heatmap-wrap { display: flex; gap: 6px; }
.heatmap-days { display: flex; flex-direction: column; gap: 3px; padding-top: 18px; }
.heatmap-days span { font-size: 10px; color: #bbb; height: 13px; line-height: 13px; }
.heatmap-grid { display: flex; gap: 3px; overflow-x: auto; flex: 1; }
.heatmap-grid::-webkit-scrollbar { height: 4px; }
.heatmap-grid::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }
.heatmap-col { display: flex; flex-direction: column; gap: 3px; }
.heatmap-month { height: 14px; font-size: 10px; color: #bbb; line-height: 14px; }
.heatmap-cell { width: 13px; height: 13px; border-radius: 2px; background: #ebedf0; transition: outline .15s; }
.heatmap-cell:hover { outline: 2px solid rgba(0,0,0,.15); outline-offset: 1px; }
.heatmap-cell.level-0 { background: #ebedf0; }
.heatmap-cell.level-1 { background: rgba(197,162,101,0.25); }
.heatmap-cell.level-2 { background: rgba(197,162,101,0.45); }
.heatmap-cell.level-3 { background: rgba(197,162,101,0.7); }
.heatmap-cell.level-4 { background: #C5A265; }
.heatmap-legend { display: flex; align-items: center; gap: 3px; margin-top: 10px; justify-content: flex-end; }
.legend-label { font-size: 10px; color: #bbb; margin: 0 2px; }
.legend-cell { width: 11px; height: 11px; border-radius: 2px; }
.legend-cell.level-0 { background: #ebedf0; }
.legend-cell.level-1 { background: rgba(197,162,101,0.25); }
.legend-cell.level-2 { background: rgba(197,162,101,0.45); }
.legend-cell.level-3 { background: rgba(197,162,101,0.7); }
.legend-cell.level-4 { background: #C5A265; }

/* ===== Password Card ===== */
.pwd-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px; padding: 32px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.pwd-card label { color: rgba(255,255,255,0.7); font-size: 13px; font-weight: 500; }
.pwd-card input {
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  color: #fff; border-radius: 8px;
}
.pwd-card input::placeholder { color: rgba(255,255,255,0.25); }
.pwd-card input:focus { border-color: #C5A265; background: rgba(255,255,255,0.08); box-shadow: 0 0 0 2px rgba(197,162,101,0.15); }
.pwd-card .btn-primary {
  width: 100%; padding: 14px; font-size: 15px; margin-top: 8px;
  background: linear-gradient(135deg, #C5A265 0%, #8B6914 100%);
  border: none; border-radius: 8px; color: #fff; font-weight: 600; cursor: pointer;
  transition: all .2s;
}
.pwd-card .btn-primary:hover { filter: brightness(1.1); transform: translateY(-1px); }
.pwd-card .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

/* ===== Password Gauge ===== */
.pwd-gauge {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  margin-top: 10px; padding: 12px 0 4px;
}
.gauge-svg { width: 120px; height: 68px; }
.gauge-label { font-size: 14px; font-weight: 700; }

.msg { margin-top: 14px; text-align: center; font-size: 14px; }
.msg.success { color: #52c41a; }
.msg.error { color: #ff4d4f; }

/* ===== Form Shared ===== */
.form-group { margin-bottom: 18px; }
.form-group label { display: block; margin-bottom: 6px; }
.form-group input {
  width: 100%; padding: 10px 14px; font-size: 14px;
  outline: none; transition: all .2s;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .info-grid { grid-template-columns: repeat(2, 1fr); }
  .bottom-grid { grid-template-columns: 1fr; }
  .hero-stats { gap: 24px; }
}
@media (max-width: 768px) {
  .security-hero { flex-direction: column; gap: 20px; text-align: center; }
  .hero-left { flex-direction: column; }
  .hero-stats { flex-wrap: wrap; justify-content: center; }
  .info-grid { grid-template-columns: 1fr; }
  .log-entry { margin-left: 0; border-left: 3px solid #e8e8e8; padding-left: 14px; }
  .entry-time { position: static; width: auto; text-align: left; margin-bottom: 4px; font-size: 11px; }
  .log-entry.entry-ok { border-left-color: #b7eb8f; }
  .log-entry.entry-fail { border-left-color: #ffccc7; }
  .load-more-btn { margin-left: 0; }
}
</style>

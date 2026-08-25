<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>数据备份</h2>
      <p class="page-desc">手动备份数据库，下载 .sql 文件保存到本地</p>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <button class="btn-primary" :disabled="backing" @click="handleCreate">
        {{ backing ? '备份中...' : '创建备份' }}
      </button>
      <label class="btn-outline restore-btn">
        从文件恢复
        <input type="file" accept=".sql" hidden @change="handleRestoreFile" />
      </label>
    </div>

    <!-- 备份列表 -->
    <div class="card" v-loading="loading">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th>文件名</th>
            <th class="col-size">文件大小</th>
            <th class="col-time">创建时间</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.filename">
            <td class="filename-cell">{{ item.filename }}</td>
            <td class="col-size">{{ formatSize(item.size) }}</td>
            <td class="col-time">{{ formatTimestamp(item.lastModified) }}</td>
            <td class="col-action">
              <button class="btn-text" @click="handleDownload(item.filename)">下载</button>
              <button class="btn-text danger" @click="handleDelete(item.filename)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-hint">暂无备份文件，点击「创建备份」开始</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createBackup, getBackupList, downloadBackup, deleteBackup, restoreBackup } from '@/api/admin'
import { downloadBlob } from '@/utils/download'
import RefreshButton from '@/components/RefreshButton.vue'

const loading = ref(false)
const backing = ref(false)
const list = ref([])

function formatSize(bytes) {
  if (bytes == null || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

function formatTimestamp(ms) {
  if (!ms) return '-'
  const d = new Date(ms)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function loadList() {
  loading.value = true
  try {
    const res = await getBackupList()
    list.value = res.data?.data || res.data || []
  } catch (err) {
    console.error('加载备份列表失败:', err)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  backing.value = true
  try {
    const res = await createBackup()
    ElMessage.success('备份创建成功')
    loadList()
  } catch (err) {
    console.error('创建备份失败:', err)
    if (!err._handled) ElMessage.error(err.message || '创建备份失败')
  } finally {
    backing.value = false
  }
}

async function handleDownload(filename) {
  try {
    const res = await downloadBackup(filename)
    const blob = new Blob([res.data], { type: 'application/sql' })
    await downloadBlob(blob, filename, { ElMessage, successMsg: '下载成功' })
  } catch (err) {
    console.error('下载失败:', err)
    if (!err._handled) ElMessage.error('下载失败')
  }
}

async function handleDelete(filename) {
  try {
    await ElMessageBox.confirm(
      `确定删除备份文件「${filename}」吗？`,
      '删除备份',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    await deleteBackup(filename)
    ElMessage.success('已删除')
    loadList()
  } catch (err) {
    if (!err._handled) ElMessage.error(err?.response?.data?.msg || err?.message || '删除失败')
  }
}

async function handleRestoreFile(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // reset so same file can be selected again
  if (!file) return

  try {
    await ElMessageBox.confirm(
      '此操作将用备份文件覆盖当前数据库中的所有数据，且不可撤销。确定继续吗？',
      '恢复数据',
      {
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch { return }

  const loadingMsg = ElMessage({ message: '正在恢复数据，请勿关闭页面...', type: 'info', duration: 0 })
  try {
    const res = await restoreBackup(file)
    ElMessage.success(res.data?.msg || '数据恢复成功，建议刷新页面或重启服务')
    loadList()
  } catch (err) {
    console.error('恢复失败:', err)
    if (!err._handled) ElMessage.error(err.message || '数据恢复失败')
  } finally {
    loadingMsg.close()
  }
}

onMounted(loadList)
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.page-desc { font-size: 13px; color: #999; margin-top: 4px; }

.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-primary {
  padding: 8px 20px;
  background: #C5A265;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary:hover { background: #b8944f; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-outline {
  padding: 8px 20px;
  background: #fff;
  color: #C5A265;
  border: 1px solid #C5A265;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-outline:hover { background: #faf6ee; }
.restore-btn { display: inline-flex; align-items: center; }

.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}

.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  background: #243447;
  white-space: nowrap;
  user-select: none;
}
.data-table th:first-child { border-radius: 8px 0 0 0; }
.data-table th:last-child { border-radius: 0 8px 0 0; }
.data-table td {
  padding: 12px 16px;
  font-size: 13px;
  color: #444;
  border-bottom: 1px solid #f5f5f5;
}
.data-table tbody tr:hover { background: #fafbfc; }

.filename-cell {
  font-family: "SF Mono", Consolas, Monaco, monospace;
  font-size: 12px;
  color: #666;
}
.col-size { text-align: right; white-space: nowrap; }
.col-time { white-space: nowrap; color: #999; }
.col-action { white-space: nowrap; }

.btn-text {
  background: none;
  border: none;
  color: #C5A265;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.15s;
}
.btn-text:hover { background: rgba(197, 162, 101, 0.08); }
.btn-text.danger { color: #e74c3c; }
.btn-text.danger:hover { background: rgba(231, 76, 60, 0.06); }

.empty-hint {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
  font-size: 14px;
}
</style>

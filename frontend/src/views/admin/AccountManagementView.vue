<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>账号管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <button class="btn-primary" @click="openAdd">＋ 新增管理员</button>
      </div>
    </div>

    <div class="card">
      <table class="data-table" v-if="list.length" v-loading="loading">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>状态</th>
            <th style="width:280px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td class="col-id">{{ item.id }}</td>
            <td class="col-name">{{ item.username }}</td>
            <td>
              <span v-if="item.role === 'super_admin'" class="role-badge super">超级管理员</span>
              <span v-else class="role-badge">管理员</span>
            </td>
            <td>
              <span v-if="item.isFrozen === 1" class="status-badge frozen">已冻结</span>
              <span v-else class="status-badge active">正常</span>
            </td>
            <td class="col-action">
              <button v-if="item.role !== 'super_admin'" class="btn-text" @click="handleFreeze(item)">
                {{ item.isFrozen === 1 ? '解冻' : '冻结' }}
              </button>
              <button class="btn-text" @click="openResetPwd(item)">重置密码</button>
              <button v-if="item.role !== 'super_admin'" class="btn-text danger" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else-if="!loading" class="empty-state">暂无管理员数据</div>
    </div>

    <!-- 新增管理员对话框 -->
    <el-dialog v-model="addVisible" title="新增管理员" width="440px" :close-on-click-modal="false" close-on-press-escape destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="请输入密码" maxlength="100" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="saving" @click="handleAdd">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="400px" :close-on-click-modal="false" close-on-press-escape destroy-on-close>
      <p style="margin-bottom:12px;color:#666;">为 <strong>{{ resetTarget }}</strong> 设置新密码：</p>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码" maxlength="100" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="saving" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminAccounts, createAdminAccount, freezeAdminAccount, unfreezeAdminAccount, resetAdminPassword, deleteAdminAccount } from '@/api/admin'
import RefreshButton from '@/components/RefreshButton.vue'

const list = ref([])
const loading = ref(false)
const saving = ref(false)

// ===== 新增管理员 =====
const addVisible = ref(false)
const addFormRef = ref(null)
const addForm = ref({ username: '', password: '' })
const passwordValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入密码'))
  if (value.length < 8) return callback(new Error('密码至少8个字符'))
  if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) return callback(new Error('密码需包含字母和数字'))
  callback()
}
const addRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ validator: passwordValidator, trigger: 'blur' }]
}

// ===== 重置密码 =====
const resetVisible = ref(false)
const resetTarget = ref('')
const resetTargetId = ref(null)
const resetFormRef = ref(null)
const resetForm = ref({ newPassword: '' })
const resetRules = {
  newPassword: [{ validator: passwordValidator, trigger: 'blur' }]
}

onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    const res = await getAdminAccounts()
    list.value = res.data?.data || res.data || []
  } catch (err) {
    console.error('加载管理员列表失败:', err)
    list.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  addForm.value = { username: '', password: '' }
  addVisible.value = true
}

async function handleAdd() {
  if (!addFormRef.value) return
  try { await addFormRef.value.validate() } catch { return }
  saving.value = true
  try {
    await createAdminAccount(addForm.value)
    ElMessage.success('管理员已创建')
    addVisible.value = false
    loadList()
  } catch (err) {
    if (!err._handled) ElMessage.error(err.response?.data?.msg || '创建失败')
  } finally {
    saving.value = false
  }
}

async function handleFreeze(item) {
  const isFrozen = item.isFrozen === 1
  const action = isFrozen ? '解冻' : '冻结'
  try {
    await ElMessageBox.confirm(`确定${action}账号「${item.username}」？`, `${action}确认`, { type: 'warning' })
  } catch { return }
  try {
    if (isFrozen) await unfreezeAdminAccount(item.id)
    else await freezeAdminAccount(item.id)
    ElMessage.success(`已${action}`)
    loadList()
  } catch (err) {
    if (!err._handled) ElMessage.error(err.response?.data?.msg || `${action}失败`)
  }
}

function openResetPwd(item) {
  resetTarget.value = item.username
  resetTargetId.value = item.id
  resetForm.value.newPassword = ''
  resetVisible.value = true
}

async function handleResetPwd() {
  if (!resetFormRef.value) return
  try { await resetFormRef.value.validate() } catch { return }
  saving.value = true
  try {
    await resetAdminPassword(resetTargetId.value, resetForm.value.newPassword)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (err) {
    if (!err._handled) ElMessage.error(err.response?.data?.msg || '重置失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定删除管理员「${item.username}」？删除后无法恢复。`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteAdminAccount(item.id)
    ElMessage.success('已删除')
    loadList()
  } catch (err) {
    if (!err._handled) ElMessage.error(err.response?.data?.msg || '删除失败')
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 { font-size: 20px; font-weight: 700; color: #1a1a1a; margin: 0; }
.header-right { display: flex; align-items: center; gap: 10px; }

.btn-primary {
  padding: 8px 20px; border: none; border-radius: 6px;
  background: #C5A265; color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: background 0.2s; font-family: inherit;
}
.btn-primary:hover { background: #d4b885; }

.card {
  background: #fff; border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); overflow: hidden;
}

.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  background: #243447; font-weight: 600; color: rgba(255,255,255,0.85);
  font-size: 12px; padding: 14px 16px; text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08); white-space: nowrap;
}
.data-table td {
  padding: 14px 16px; font-size: 13px;
  border-bottom: 1px solid #f5f5f5; color: #444; vertical-align: middle;
}
.data-table tbody tr { transition: background 0.15s; }
.data-table tbody tr:hover { background: #fafbfc; }

.col-id { width: 60px; color: #bbb; font-size: 12px; }
.col-name { font-weight: 600; color: #1a1a1a; }
.col-action { white-space: nowrap; }

.role-badge {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 12px; background: #f0f0f0; color: #666;
}
.role-badge.super { background: #fff7e6; color: #C5A265; font-weight: 600; }

.status-badge {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 12px;
}
.status-badge.active { background: #f6ffed; color: #52c41a; }
.status-badge.frozen { background: #fff1f0; color: #e74c3c; }

.btn-text {
  padding: 4px 10px; border: none; background: none;
  color: #C5A265; font-size: 13px; cursor: pointer;
  border-radius: 4px; transition: background 0.15s; font-family: inherit;
}
.btn-text:hover { background: rgba(197,162,101,0.08); }
.btn-text.danger { color: #e74c3c; }
.btn-text.danger:hover { background: rgba(231,76,60,0.06); }

.empty-state {
  text-align: center; padding: 60px 20px; color: #ccc;
  font-size: 14px; min-height: 120px;
  display: flex; align-items: center; justify-content: center;
}

.btn-gold { background: #C5A265 !important; border-color: #C5A265 !important; }
.btn-gold:hover, .btn-gold:focus { background: #d4b885 !important; border-color: #d4b885 !important; }

:deep(.el-dialog__header) { border-bottom: 1px solid #f0f0f0; padding: 16px 24px; margin-right: 0; }
:deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1a1a1a; }
:deep(.el-dialog__body) { padding: 24px; }
:deep(.el-dialog__footer) { border-top: 1px solid #f0f0f0; padding: 14px 24px; }
:deep(.el-form-item__label) { font-size: 13px; color: #555; }
:deep(.el-input__wrapper) { border-radius: 6px; }
</style>

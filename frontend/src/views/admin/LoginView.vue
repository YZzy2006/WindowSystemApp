<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <span class="login-logo">◈</span>
        <h2>顺居门业</h2>
        <p>管理后台</p>
      </div>
      <div class="login-body">
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin" size="large">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable autofocus />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password clearable />
          </el-form-item>
          <el-form-item>
            <div class="login-options">
              <el-checkbox v-model="remember">记住账号密码</el-checkbox>
              <span class="forgot-link"><a role="button" tabindex="0" @click.prevent="forgotVisible = true" @keydown.enter="forgotVisible = true" @keydown.space.prevent="forgotVisible = true">忘记密码？</a></span>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="warning" native-type="submit" :loading="loading" size="large" round style="width:100%;height:48px;font-size:16px">
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
        <p v-if="kickMsg" class="kick-msg">{{ kickMsg }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </div>

    <!-- 忘记密码对话框 -->
    <el-dialog v-model="forgotVisible" title="重置密码" width="440px" :close-on-click-modal="false" close-on-press-escape destroy-on-close @opened="onForgotOpened">
      <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="110px">
        <el-form-item label="目标用户名" prop="username">
          <el-input ref="forgotUsernameRef" v-model="forgotForm.username" placeholder="要重置密码的用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="超管密码" prop="superAdminPassword">
          <el-input v-model="forgotForm.superAdminPassword" type="password" placeholder="超级管理员密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="至少8位，含字母和数字" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="warning" :loading="forgotLoading" @click="handleForgot">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { forgotPassword } from '@/api/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loginFormRef = ref(null)
const loginForm = ref({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const loading = ref(false)
const error = ref('')
const kickMsg = ref('')
const REMEMBER_KEY = 'shunju_login_remember'
const remember = ref(false)
// base64 混淆存储（UTF-8 安全），仅防明文直读，非真实加密
const encodeText = (s) => btoa(encodeURIComponent(s))
const decodeText = (s) => { try { return decodeURIComponent(atob(s)) } catch { return '' } }
function loadRemembered() {
  try {
    const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY) || 'null')
    if (saved?.u && saved?.p) {
      loginForm.value.username = decodeText(saved.u)
      loginForm.value.password = decodeText(saved.p)
      remember.value = true
    }
  } catch { /* 数据损坏则忽略 */ }
}
function saveRemembered() {
  if (remember.value && loginForm.value.username && loginForm.value.password) {
    localStorage.setItem(REMEMBER_KEY, JSON.stringify({
      u: encodeText(loginForm.value.username),
      p: encodeText(loginForm.value.password),
    }))
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}
onMounted(() => {
  const msg = sessionStorage.getItem('kickMsg')
  if (msg) { kickMsg.value = msg; sessionStorage.removeItem('kickMsg') }
  loadRemembered()
})

async function handleLogin() {
  if (!loginFormRef.value) return
  try { await loginFormRef.value.validate() } catch { return }
  loading.value = true; error.value = ''
  try {
    await auth.login(loginForm.value.username, loginForm.value.password)
    saveRemembered()
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/admin')
  } catch (e) {
    if (e._handled) return
    error.value = e?.message || '账号或密码错误'
    ElMessage.error(error.value)
  } finally { loading.value = false }
}

// ===== 忘记密码 =====
const forgotVisible = ref(false)
const forgotLoading = ref(false)
const forgotFormRef = ref(null)
const forgotUsernameRef = ref(null)
function onForgotOpened() { nextTick(() => forgotUsernameRef.value?.$el?.querySelector('input')?.focus()) }
const forgotForm = ref({ username: '', superAdminPassword: '', newPassword: '', confirmPassword: '' })

const passwordValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入新密码'))
  if (value.length < 8) return callback(new Error('密码至少8个字符'))
  if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) return callback(new Error('密码需包含字母和数字'))
  callback()
}
const confirmValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请确认新密码'))
  if (value !== forgotForm.value.newPassword) return callback(new Error('两次密码不一致'))
  callback()
}
const forgotRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  superAdminPassword: [{ required: true, message: '请输入超级管理员密码', trigger: 'blur' }],
  newPassword: [{ validator: passwordValidator, trigger: 'blur' }],
  confirmPassword: [{ validator: confirmValidator, trigger: 'blur' }]
}

async function handleForgot() {
  if (!forgotFormRef.value) return
  try { await forgotFormRef.value.validate() } catch { return }
  forgotLoading.value = true
  try {
    const res = await forgotPassword({
      username: forgotForm.value.username,
      superAdminPassword: forgotForm.value.superAdminPassword,
      newPassword: forgotForm.value.newPassword
    })
    ElMessage.success(res.data?.msg || '密码重置成功，请用新密码登录')
    forgotVisible.value = false
    // 自动填入刚重置的用户名
    loginForm.value.username = forgotForm.value.username
    loginForm.value.password = ''
    // 记住的密码已失效，清除对应账号的记住数据
    try {
      const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY) || 'null')
      if (saved?.u && decodeText(saved.u) === forgotForm.value.username) {
        localStorage.removeItem(REMEMBER_KEY)
        remember.value = false
      }
    } catch { /* 忽略 */ }
  } catch (e) {
    if (!e._handled) ElMessage.error(e.response?.data?.msg || e.message || '重置失败')
  } finally { forgotLoading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: radial-gradient(ellipse at 50% 50%, rgba(197,162,101,.06) 0%, transparent 60%), #f0f2f5;
}
.login-card {
  width: 400px; background: #fff; border-radius: var(--radius-lg);
  box-shadow: 0 8px 40px rgba(0,0,0,.08); overflow: hidden;
}
.login-header {
  text-align: center; padding: 40px 40px 32px;
  background: linear-gradient(180deg, #1c1c1c 0%, #141414 100%);
}
.login-logo { font-size: 40px; color: var(--gold-primary); }
.login-header h2 { font-size: 22px; color: #fff; margin: 12px 0 4px; letter-spacing: 3px; font-weight: 500; }
.login-header p { font-size: 13px; color: rgba(255,255,255,.35); letter-spacing: 2px; }
.login-body { padding: 32px 40px 40px; }
.input-group {
  display: flex; align-items: center; gap: 12px; margin-bottom: 18px;
  padding: 0 14px; border: 1px solid #e8e8e8; border-radius: var(--radius); transition: border-color .2s;
}
.input-group:focus-within { border-color: var(--gold-primary); }
.input-icon { font-size: 16px; flex-shrink: 0; }
.input-group input {
  flex: 1; padding: 13px 0; border: none; font-size: 14px; outline: none; background: transparent;
}
.login-btn {
  width: 100%; padding: 13px; background: var(--gold-primary); color: #fff; border: none;
  border-radius: var(--radius); font-size: 15px; font-weight: 500; letter-spacing: 4px;
  cursor: pointer; transition: background .2s; margin-top: 8px;
}
.login-btn:hover { background: var(--gold-hover); }
.kick-msg { color: #fa8c16; text-align: center; margin-bottom: 4px; font-size: 13px; background: #fff7e6; padding: 10px 16px; border-radius: var(--radius); border: 1px solid #ffd591; }
.error { color: #e74c3c; text-align: center; margin-top: 16px; font-size: 13px; }
.login-options { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.forgot-link { font-size: 13px; }
.forgot-link a { color: #999; text-decoration: none; transition: color .2s; }
.forgot-link a:hover { color: var(--gold-primary); }
:deep(.el-dialog__header) { border-bottom: 1px solid #f0f0f0; padding: 16px 24px; margin-right: 0; }
:deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1a1a1a; }
:deep(.el-dialog__body) { padding: 24px; }
:deep(.el-dialog__footer) { border-top: 1px solid #f0f0f0; padding: 14px 24px; }
:deep(.el-form-item__label) { font-size: 13px; color: #555; }
:deep(.el-input__wrapper) { border-radius: 6px; }
</style>

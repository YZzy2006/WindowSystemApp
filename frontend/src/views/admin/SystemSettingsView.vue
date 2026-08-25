<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>系统设置</h2>
      <button class="btn-primary" @click="handleSave" :disabled="saving">{{ saving ? '保存中...' : '保存设置' }}</button>
    </div>

    <div class="settings-grid">
      <!-- 单号前缀设置 -->
      <div class="card">
        <h3 class="card-title"><span class="section-dash"></span>单号前缀</h3>
        <div class="form-grid">
          <div class="form-item">
            <label>销售单</label>
            <input v-model="settings.sale_order_prefix" placeholder="XS" />
          </div>
          <div class="form-item">
            <label>预算单</label>
            <input v-model="settings.presale_order_prefix" placeholder="YS" />
          </div>
          <div class="form-item">
            <label>采购单</label>
            <input v-model="settings.purchase_order_prefix" placeholder="CG" />
          </div>
          <div class="form-item">
            <label>销售退货</label>
            <input v-model="settings.sale_return_prefix" placeholder="XT" />
          </div>
          <div class="form-item">
            <label>采购退货</label>
            <input v-model="settings.purchase_return_prefix" placeholder="CT" />
          </div>
          <div class="form-item">
            <label>入库单</label>
            <input v-model="settings.stock_in_prefix" placeholder="RK" />
          </div>
          <div class="form-item">
            <label>出库单</label>
            <input v-model="settings.stock_out_prefix" placeholder="CK" />
          </div>
        </div>
      </div>

      <!-- 公司信息设置 -->
      <div class="card">
        <h3 class="card-title"><span class="section-dash"></span>公司信息（用于打印模板）</h3>
        <div class="form-grid">
          <div class="form-item full">
            <label>公司名称</label>
            <input v-model="settings.company_name" placeholder="顺居门业" />
          </div>
          <div class="form-item">
            <label>联系电话</label>
            <input v-model="settings.company_phone" placeholder="18948846839" />
          </div>
          <div class="form-item">
            <label>公司地址</label>
            <input v-model="settings.company_address" placeholder="乳源县二九一物流仓库" />
          </div>
        </div>
      </div>

      <!-- 打印标题设置 -->
      <div class="card">
        <h3 class="card-title"><span class="section-dash"></span>打印标题</h3>
        <div class="form-grid">
          <div class="form-item full">
            <label>销售单标题</label>
            <input v-model="settings.print_title_sale" placeholder="顺居门业客户销售单" />
          </div>
          <div class="form-item full">
            <label>预算单标题</label>
            <input v-model="settings.print_title_presale" placeholder="顺居门窗客户预算单" />
          </div>
        </div>
      </div>

      <!-- 合同条款设置 -->
      <div class="card">
        <h3 class="card-title"><span class="section-dash"></span>合同条款（用于打印模板）</h3>
        <div class="form-grid">
          <div class="form-item full">
            <label>安装条款</label>
            <textarea v-model="settings.installation_terms" rows="4" placeholder="1. 安装前请确认洞口尺寸，误差超过20mm需客户自行修补&#10;2. 安装完成后请当场验收，签字后视为合格&#10;3. 因客户原因导致二次安装，费用由客户承担"></textarea>
          </div>
          <div class="form-item full">
            <label>保修条款</label>
            <textarea v-model="settings.warranty_terms" rows="4" placeholder="1. 门窗产品保修期为安装完成之日起两年&#10;2. 保修范围内免费维修，人为损坏除外&#10;3. 五金配件保修一年，玻璃不在保修范围内"></textarea>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemSettings, saveSystemSettings } from '@/api/admin'

const saving = ref(false)
const settings = reactive({
  sale_order_prefix: 'XS',
  presale_order_prefix: 'YS',
  purchase_order_prefix: 'CG',
  sale_return_prefix: 'XT',
  purchase_return_prefix: 'CT',
  stock_in_prefix: 'RK',
  stock_out_prefix: 'CK',
  company_name: '顺居门业',
  company_phone: '18948846839',
  company_address: '乳源县二九一物流仓库',
  print_title_sale: '顺居门业客户销售单',
  print_title_presale: '顺居门窗客户预算单',
  installation_terms: '',
  warranty_terms: ''
})

async function loadSettings() {
  try {
    const res = await getSystemSettings()
    const data = res.data?.data ?? res.data ?? {}
    Object.keys(data).forEach(key => {
      if (key in settings) {
        settings[key] = data[key] ?? settings[key]
      }
    })
  } catch (e) {
    console.error('加载设置失败:', e)
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveSystemSettings({ ...settings })
    ElMessage.success('设置已保存')
  } catch (e) {
    console.error('保存设置失败:', e)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.settings-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 20px;
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

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-item.full {
  grid-column: span 2;
}

.form-item label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.form-item input {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.2s;
  outline: none;
}

.form-item input:focus {
  border-color: #C5A265;
}

.form-item input::placeholder {
  color: #ccc;
}

.form-item textarea {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.2s;
  outline: none;
  resize: vertical;
  line-height: 1.6;
}

.form-item textarea:focus {
  border-color: #C5A265;
}

.form-item textarea::placeholder {
  color: #ccc;
}

.btn-primary {
  padding: 9px 22px;
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-item.full {
    grid-column: span 1;
  }
}
</style>

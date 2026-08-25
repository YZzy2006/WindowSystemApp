<template>
  <div class="page admin-page">
    <div class="page-header">
      <h2>案例管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="load" />
        <input v-model="keyword" class="search-input" placeholder="搜标题 / 描述..." />
        <button class="btn-primary" @click="openAdd">＋ 新增案例</button>
      </div>
    </div>

    <div class="card">
      <table class="data-table" v-if="filteredList.length">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-cover">封面</th>
            <th>标题</th>
            <th>图片</th>
            <th>排序</th>
            <th>状态</th>
            <th>描述</th>
            <th>创建时间</th>
            <th style="width:120px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in filteredList" :key="c.id" >
            <td class="col-id">{{ c.id }}</td>
            <td><img v-if="c.image" :src="c.image" class="thumb" /></td>
            <td class="col-title">{{ c.title }}</td>
            <td>
              <div class="thumb-strip" v-if="allImgList(c).length">
                <img v-for="(img, i) in allImgList(c).slice(0, 4)" :key="i" :src="img" class="thumb-sm" />
                <span v-if="allImgList(c).length > 4" class="thumb-more">+{{ allImgList(c).length - 4 }}</span>
              </div>
            </td>
            <td class="col-sort">{{ c.sort ?? '-' }}</td>
            <td>
              <span class="badge" :class="c.isShow === 0 ? 'badge-off' : 'badge-on'">{{ c.isShow === 0 ? '隐藏' : '展示' }}</span>
            </td>
            <td class="col-desc">{{ c.description }}</td>
            <td>{{ formatDate(c.createTime) }}</td>
            <td style="white-space:nowrap">
              <button class="btn-text" style="display:inline-block;margin-right:8px" @click="openEdit(c)">编辑</button>
              <button class="btn-text danger" style="display:inline-block" @click="handleDel(c)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ list.length ? '未匹配到案例' : '暂未添加案例' }}</p>
    </div>

    <!-- 新增 / 编辑 弹窗 -->
    <div class="modal-overlay" v-if="showModal" @click.self="closeModal" @keydown.esc="closeModal" tabindex="-1">
      <div class="modal modal-lg">
        <div class="modal-header"><h3>{{ editingId ? '编辑案例' : '新增案例' }}</h3></div>
        <form class="modal-body" @submit.prevent="handleSave">
          <div class="form-row">
            <div class="form-group flex-1">
              <label>标题 <span class="req">*</span></label>
              <input v-model="form.title" required placeholder="案例标题" />
            </div>
            <div class="form-group col-sort-input">
              <label>排序</label>
              <input v-model.number="form.sort" type="number" min="0" placeholder="0" />
            </div>
            <div class="form-group col-show-input">
              <label>展示状态</label>
              <select v-model.number="form.isShow">
                <option :value="1">展示</option>
                <option :value="0">隐藏</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>改造后封面 <span class="hint-label">（主图）</span></label>
              <ImageUploader v-model="form.image" />
            </div>
            <div class="form-group flex-1">
              <label>改造前封面 <span class="hint-label">（旧窗原状）</span></label>
              <ImageUploader v-model="form.beforeImage" />
            </div>
          </div>

          <div class="form-group">
            <label>改造前多图（可选，最多 6 张）</label>
            <MultiImageUploader v-model="form.beforeImages" />
          </div>

          <div class="form-group">
            <label>改造后多图（可选，最多 6 张）</label>
            <MultiImageUploader v-model="form.images" />
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>改造前描述</label>
              <input v-model="form.beforeDesc" placeholder="如：原窗户漏风渗水，密封差" />
            </div>
            <div class="form-group flex-1">
              <label>改造后描述</label>
              <input v-model="form.afterDesc" placeholder="如：更换断桥铝，隔音密封显著提升" />
            </div>
          </div>

          <div class="form-group">
            <label>描述</label>
            <textarea v-model="form.description" rows="2" placeholder="案例简述"></textarea>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>小区/位置</label>
              <input v-model="form.location" placeholder="如：碧桂园18栋1503" />
            </div>
            <div class="form-group flex-1">
              <label>关联产品 <span class="hint-label">（选填，可多选）</span></label>
              <div class="product-checkboxes">
                <label v-for="p in productList" :key="p.id" class="pcb-item">
                  <input type="checkbox" :value="p.id" :checked="(form.productIds || []).includes(p.id)" @change="toggleProduct(p.id)" />
                  <span>{{ p.name }} <em v-if="p.model">({{ p.model }})</em></span>
                </label>
              </div>
              <span class="pcb-count" v-if="(form.productIds || []).length">已选 {{ form.productIds.length }} 个产品</span>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>产品类型</label>
              <input v-model="form.productType" placeholder="如：断桥铝平开窗" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>型材规格</label>
              <input v-model="form.profileSpec" placeholder="如：108系列，壁厚2.0mm" />
            </div>
            <div class="form-group flex-1">
              <label>玻璃配置</label>
              <input v-model="form.glassConfig" placeholder="如：5+20A+5中空钢化" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>五金品牌</label>
              <input v-model="form.hardwareBrand" placeholder="如：德国好博HOPPE" />
            </div>
            <div class="form-group flex-1">
              <label>安装时长</label>
              <input v-model="form.installDuration" placeholder="如：2天完工" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>客户需求</label>
              <textarea v-model="form.customerNeed" rows="2" placeholder="如：家有小孩，重点考虑安全防护和隔音"></textarea>
            </div>
            <div class="form-group flex-1">
              <label>客户评价</label>
              <textarea v-model="form.customerReview" rows="2" placeholder="如：装完感觉整个家都亮堂了"></textarea>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? '保存中...' : (editingId ? '更新' : '保存') }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getCases, addCase, updateCase, deleteCase, getProducts, restoreEntity } from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import ImageUploader from '@/components/ImageUploader.vue'
import MultiImageUploader from '@/components/MultiImageUploader.vue'
import { formatDate } from '@/utils/format'
import RefreshButton from '@/components/RefreshButton.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'
import { useUnsavedConfirm } from '@/composables/useUnsavedConfirm'

const list = ref([])
const productList = ref([])
const keyword = ref('')

const FILTER_KEY = 'cases_filters'
let _restoring = false
function restoreFilters() {
  try { const s = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}'); _restoring = true; if (s.keyword) keyword.value = s.keyword; _restoring = false } catch {}
}
function syncFilters() { if (_restoring) return; sessionStorage.setItem(FILTER_KEY, JSON.stringify({ keyword: keyword.value })) }
watch(keyword, syncFilters)
const showModal = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', image: '', images: '', beforeImage: '', beforeImages: '', beforeDesc: '', afterDesc: '', description: '', location: '', productIds: [], productType: '', profileSpec: '', glassConfig: '', hardwareBrand: '', customerNeed: '', customerReview: '', installDuration: '', sort: 0, isShow: 1 })
useCtrlEnterSave(showModal, handleSave)
const { resetDirty, closeGuard } = useUnsavedConfirm(showModal, () => JSON.stringify(form), { form })
function closeModal() { closeGuard(() => { showModal.value = false }) }

const filteredList = computed(() => {
  if (!keyword.value.trim()) return list.value
  const kw = keyword.value.trim().toLowerCase()
  return list.value.filter(c =>
    (c.title || '').toLowerCase().includes(kw) ||
    (c.description || '').toLowerCase().includes(kw)
  )
})

// ===== Undo composables =====
const { handleDelete: undoDelete } = useUndoDelete({
  deleteApi: deleteCase,
  restoreApi: (id) => restoreEntity('case', id),
  onSuccess: load,
  entityLabel: '案例',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateCase,
  onSuccess: load,
  entityLabel: '案例',
})

function parseProductIds(v) {
  if (!v) return []
  if (Array.isArray(v)) return v.map(Number)
  return String(v).split(',').map(s => Number(s.trim())).filter(n => n)
}
function toggleProduct(id) {
  const arr = form.productIds || []
  const idx = arr.indexOf(id)
  if (idx >= 0) arr.splice(idx, 1)
  else arr.push(id)
  form.productIds = [...arr]
}

onMounted(() => { restoreFilters(); load(); loadProducts() })

function load() { getCases().then(r => list.value = r.data.data || []).catch(e => { ElMessage.error('加载案例失败'); console.error(e) }) }
function loadProducts() { getProducts().then(r => { productList.value = r.data.data || [] }).catch(e => { ElMessage.error('加载产品失败'); console.error(e) }) }

function allImgList(c) {
  const imgs = []
  if (c.beforeImage) imgs.push(c.beforeImage)
  if (c.beforeImages) {
    c.beforeImages.split(',').map(s => s.trim()).filter(Boolean).forEach(s => imgs.push(s))
  }
  if (c.image) imgs.push(c.image)
  if (c.images) {
    c.images.split(',').map(s => s.trim()).filter(Boolean).forEach(s => imgs.push(s))
  }
  return imgs
}

function openAdd() {
  editingId.value = null
  Object.assign(form, { title: '', image: '', images: '', beforeImage: '', beforeImages: '', beforeDesc: '', afterDesc: '', description: '', location: '', productIds: [], productType: '', profileSpec: '', glassConfig: '', hardwareBrand: '', customerNeed: '', customerReview: '', installDuration: '', sort: 0, isShow: 1 })
  showModal.value = true
}

function openEdit(c) {
  snapshotBeforeEdit(c)
  editingId.value = c.id
  Object.assign(form, {
    title: c.title || '', image: c.image || '', images: c.images || '', description: c.description || '',
    location: c.location || '', productIds: parseProductIds(c.productIds), productType: c.productType || '', profileSpec: c.profileSpec || '',
    glassConfig: c.glassConfig || '', hardwareBrand: c.hardwareBrand || '', beforeImage: c.beforeImage || '',
    beforeImages: c.beforeImages || '', beforeDesc: c.beforeDesc || '', afterDesc: c.afterDesc || '',
    customerNeed: c.customerNeed || '', customerReview: c.customerReview || '', installDuration: c.installDuration || '',
    sort: c.sort ?? 0, isShow: c.isShow ?? 1
  })
  showModal.value = true
}

async function handleSave() {
  if (!form.title.trim()) { ElMessage.warning('标题不能为空'); return }
  const data = {
    title: form.title, image: form.image, images: form.images, description: form.description,
    productIds: (form.productIds || []).join(','),
    location: form.location, productType: form.productType, profileSpec: form.profileSpec,
    glassConfig: form.glassConfig, hardwareBrand: form.hardwareBrand, beforeImage: form.beforeImage,
    beforeImages: form.beforeImages, beforeDesc: form.beforeDesc, afterDesc: form.afterDesc,
    customerNeed: form.customerNeed, customerReview: form.customerReview, installDuration: form.installDuration,
    sort: form.sort, isShow: form.isShow,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateCase(editingId.value, data)
      afterEditSave({ id: editingId.value, ...data })
    } else {
      await addCase(data)
      ElMessage.success('案例已添加')
    }
    resetDirty(); showModal.value = false; load()
  } catch (e) {
    if (!e._handled) ElMessage.error(e.message || '保存失败')
  } finally { saving.value = false }
}

function handleDel(c) { undoDelete(c) }
</script>

<style scoped>
.col-id { width: 50px; color: var(--text-placeholder); }
.col-cover { width: 60px; }
.col-title { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-sort { width: 50px; text-align: center; color: var(--text-placeholder); font-size: 13px; }
.col-desc { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-muted); }

.thumb-strip { display: flex; gap: 4px; align-items: center; }
.thumb-sm { width: 36px; height: 36px; object-fit: cover; border-radius: 4px; border: 1px solid #f0f0f0; }
.thumb-more { font-size: 11px; color: var(--text-placeholder); margin-left: 2px; }

.modal-lg { width: 640px; }

.form-row { display: flex; gap: 14px; }
.form-row .flex-1 { flex: 1; }
.col-sort-input { width: 90px; }
.col-show-input { width: 120px; }
.form-group .req { color: #e74c3c; }
.hint-label { font-weight: 400; font-size: 11px; color: var(--text-placeholder); }

.product-checkboxes { max-height: 180px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: var(--radius); padding: 8px 12px; background: #fafbfc; }
.pcb-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 13px; cursor: pointer; }
.pcb-item input[type="checkbox"] { width: auto; margin: 0; accent-color: var(--gold-primary); }
.pcb-item em { color: var(--text-placeholder); font-style: normal; font-size: 11px; }
.pcb-count { display: block; font-size: 11px; color: var(--gold-primary); margin-top: 6px; font-weight: 500; }
</style>

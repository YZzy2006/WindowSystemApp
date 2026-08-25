<template>
  <div class="page admin-page">
    <!-- ===== Header ===== -->
    <div class="page-header">
      <h2>售后管理</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <button class="btn-primary" @click="openAdd">+ 新建售后单</button>
      </div>
    </div>

    <!-- ===== Toolbar ===== -->
    <div class="toolbar">
      <div class="status-tabs">
        <span
          v-for="tab in statusTabs"
          :key="tab.value"
          class="status-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="switchStatus(tab.value)"
        >{{ tab.label }}</span>
      </div>
      <div class="toolbar-right">
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="开始日期"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          size="default"
          :clearable="true"
          @change="onStartDateChange"
          style="width: 155px"
        />
        <span style="margin: 0 4px; color: #000;">至</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="截止日期"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          size="default"
          :clearable="true"
          @change="onEndDateChange"
          style="width: 155px"
        />
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="keyword" placeholder="搜单号 / 客户名 / 问题描述..." class="search-input" @input="onSearchInput" />
        </div>
        <button class="btn-search" @click="handleSearch" :disabled="loading">搜索</button>
        <button class="btn-reset" @click="handleReset" :disabled="loading">重置</button>
      </div>
    </div>

    <!-- ===== Summary Bar ===== -->
    <div class="summary-bar" v-if="summary">
      <div class="summary-card">
        <div class="sc-label">待处理数</div>
        <div class="sc-value">{{ summary.pendingCount ?? 0 }}</div>
      </div>
      <div class="summary-card">
        <div class="sc-label">处理中数</div>
        <div class="sc-value">{{ summary.processingCount ?? 0 }}</div>
      </div>
      <div class="summary-card green">
        <div class="sc-label">本月完成数</div>
        <div class="sc-value">{{ summary.monthCompletedCount ?? 0 }}</div>
      </div>
      <div class="summary-card gold">
        <div class="sc-label">本月收入</div>
        <div class="sc-value">{{ formatMoney(summary.monthIncome) }}</div>
      </div>
    </div>

    <!-- ===== Batch Bar ===== -->
    <div v-if="selectedIds.size" class="batch-bar">
      <span class="batch-info">已选 {{ selectedIds.size }} 项</span>
      <button class="btn-batch-danger" @click="handleBatchDelete" :disabled="batchOperating">{{ batchOperating ? '处理中...' : '批量删除' }}</button>
      <el-dropdown trigger="click" @command="handleBatchStatus">
        <button class="btn-batch" :disabled="batchOperating">批量状态 ▾</button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="pending">待处理</el-dropdown-item>
            <el-dropdown-item command="assigned">已派单</el-dropdown-item>
            <el-dropdown-item command="processing">处理中</el-dropdown-item>
            <el-dropdown-item command="completed">已完成</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button class="btn-batch" @click="clearSelection">取消选择</button>
    </div>

    <!-- ===== Table ===== -->
    <div class="card" v-loading="loading">
      <table class="data-table" v-if="list.length">
        <thead>
          <tr>
            <th style="width:40px"><input type="checkbox" :checked="isAllSelected" :indeterminate.prop="isIndeterminate" @change="toggleSelectAll" /></th>
            <th class="col-order-no sortable" @click="toggleSort('orderNo')">单号 {{ sortArrow('orderNo') }}</th>
            <th class="sortable" @click="toggleSort('saleOrderNo')">关联订单 {{ sortArrow('saleOrderNo') }}</th>
            <th class="sortable" @click="toggleSort('customerName')">客户 {{ sortArrow('customerName') }}</th>
            <th class="sortable" @click="toggleSort('type')">类型 {{ sortArrow('type') }}</th>
            <th class="col-desc sortable" @click="toggleSort('description')">问题描述 {{ sortArrow('description') }}</th>
            <th class="col-status">状态</th>
            <th class="sortable" @click="toggleSort('technician')">师傅 {{ sortArrow('technician') }}</th>
            <th class="col-date sortable" @click="toggleSort('scheduledDate')">预约日期 {{ sortArrow('scheduledDate') }}</th>
            <th class="col-money sortable" @click="toggleSort('cost')">费用 {{ sortArrow('cost') }}</th>
            <th style="width:260px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in sortedList" :key="item.id" >
            <td><input type="checkbox" :checked="selectedIds.has(item.id)" @change="toggleSelect(item.id)" /></td>
            <td class="col-order-no">
              <span class="order-no-link" @click="openDetail(item)">{{ item.orderNo || '-' }}</span>
            </td>
            <td class="col-order-no">{{ item.saleOrderNo || '-' }}</td>
            <td class="col-customer">{{ item.customerName || '-' }}</td>
            <td>
              <span class="type-tag" :class="'type-' + item.type">{{ typeLabel(item.type) }}</span>
            </td>
            <td class="col-desc">
              <span class="desc-text" :title="item.description">{{ item.description || '-' }}</span>
            </td>
            <td class="col-status">
              <span class="status-badge" :class="'status-' + item.status">{{ statusLabel(item.status) }}</span>
            </td>
            <td>{{ item.assignedTo || item.technician || '-' }}</td>
            <td class="col-date">{{ formatDate(item.scheduledDate || item.appointmentDate) }}</td>
            <td class="col-money gold">{{ item.cost != null && item.cost !== 0 ? formatMoney(item.cost) : '-' }}</td>
            <td class="col-action">
              <button class="btn-text" @click="openDetail(item)">详情</button>
              <button class="btn-text" @click="openEdit(item)">编辑</button>
              <el-dropdown trigger="click" @command="(cmd) => handleChangeStatus(item, cmd)">
                <button class="btn-text status-btn">状态<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="pending" :disabled="item.status === 'pending'">待处理</el-dropdown-item>
                    <el-dropdown-item command="assigned" :disabled="item.status === 'assigned'">已派单</el-dropdown-item>
                    <el-dropdown-item command="processing" :disabled="item.status === 'processing'">处理中</el-dropdown-item>
                    <el-dropdown-item command="completed" :disabled="item.status === 'completed'">已完成</el-dropdown-item>
                    <el-dropdown-item command="closed" :disabled="item.status === 'closed'" divided>已关闭</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <button class="btn-text danger" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        <p>{{ hasFilter ? '未匹配到售后单' : '暂无售后记录，点击右上角新建' }}</p>
      </div>
    </div>

    <!-- ===== Pagination ===== -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- ===== Detail Drawer ===== -->
    <el-drawer
      v-model="drawerVisible"
      title="售后单详情"
      direction="rtl"
      size="720px"
      close-on-press-escape
      :destroy-on-close="true"
    >
      <div class="detail-content" v-loading="detailLoading" v-if="detailOrder">
        <!-- Basic Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">售后单号</span>
              <span class="field-value mono">{{ detailOrder.orderNo || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">状态</span>
              <span class="status-badge" :class="'status-' + detailOrder.status">{{ statusLabel(detailOrder.status) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">关联订单</span>
              <span class="field-value mono">{{ detailOrder.saleOrderNo || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">客户</span>
              <span class="field-value">{{ detailOrder.customerName || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">类型</span>
              <span class="type-tag" :class="'type-' + detailOrder.type">{{ typeLabel(detailOrder.type) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">来源</span>
              <span class="field-value">{{ sourceLabel(detailOrder.source) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">师傅</span>
              <span class="field-value">{{ detailOrder.assignedTo || detailOrder.technician || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">预约日期</span>
              <span class="field-value">{{ formatDate(detailOrder.scheduledDate || detailOrder.appointmentDate) }}</span>
            </div>
          </div>
        </div>

        <!-- Problem Description -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>问题描述</h4>
          <p class="detail-remark">{{ detailOrder.description || '-' }}</p>
        </div>

        <!-- Photos -->
        <div class="detail-section" v-if="detailOrder.images">
          <h4 class="section-title"><span class="section-dash"></span>现场照片</h4>
          <div class="photo-wall">
            <div
              v-for="(photo, i) in parseImagesWithDesc(detailOrder.images)"
              :key="i"
              class="photo-wall-item"
            >
              <img
                :src="photo.url"
                class="photo-thumb"
                @click="previewImage(photo.url)"
              />
              <div class="photo-desc" v-if="photo.desc">{{ photo.desc }}</div>
            </div>
          </div>
        </div>

        <!-- Processing Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>处理信息</h4>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">处理方式</span>
              <span class="field-value">{{ resolutionLabel(detailOrder.resolution) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">费用</span>
              <span class="field-value money gold">{{ detailOrder.cost != null ? formatMoney(detailOrder.cost) : '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">是否质保期内</span>
              <span class="field-value">
                <span class="warranty-tag" :class="{ active: detailOrder.isWarranty }">{{ detailOrder.isWarranty ? '是' : '否' }}</span>
              </span>
            </div>
          </div>
        </div>

        <!-- Remark -->
        <div class="detail-section" v-if="detailOrder.remark">
          <h4 class="section-title"><span class="section-dash"></span>备注</h4>
          <p class="detail-remark">{{ detailOrder.remark }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="handlePrint">打印售后单</el-button>
      </template>
    </el-drawer>

    <!-- Print Preview Drawer -->
    <el-drawer
      v-model="previewDrawerVisible"
      title="打印预览"
      direction="btt"
      size="100%"
      close-on-press-escape
      :destroy-on-close="true"
    >
      <div style="display:flex;flex-direction:column;height:100%;">
        <div style="background:#e8e8e8;padding:20px;border-radius:6px;overflow:auto;flex:1;">
          <div class="print-page" ref="printPageRef" :style="printPageStyle">
            <!-- Print Header -->
            <div class="print-header">
              <div class="print-title">顺居门业售后单</div>
              <div class="print-company">{{ printCfg.companyName || '顺居门业' }}</div>
            </div>

            <!-- Order Info -->
            <div class="print-info-section">
              <div class="print-info-row">
                <div class="print-info-item">
                  <span class="print-label">售后单号</span>
                  <span class="print-value mono">{{ printData.orderNo || '-' }}</span>
                </div>
                <div class="print-info-item">
                  <span class="print-label">创建日期</span>
                  <span class="print-value">{{ formatDate(printData.createTime) }}</span>
                </div>
                <div class="print-info-item">
                  <span class="print-label">状态</span>
                  <span class="print-value">{{ statusLabel(printData.status) }}</span>
                </div>
              </div>
              <div class="print-info-row">
                <div class="print-info-item">
                  <span class="print-label">客户</span>
                  <span class="print-value">{{ printData.customerName || '-' }}</span>
                </div>
                <div class="print-info-item" v-if="printCfg.showSaleOrderNo !== false">
                  <span class="print-label">关联订单</span>
                  <span class="print-value mono">{{ printData.saleOrderNo || '-' }}</span>
                </div>
                <div class="print-info-item">
                  <span class="print-label">类型</span>
                  <span class="print-value">{{ typeLabel(printData.type) }}</span>
                </div>
              </div>
              <div class="print-info-row">
                <div class="print-info-item" v-if="printCfg.showSource !== false">
                  <span class="print-label">来源</span>
                  <span class="print-value">{{ sourceLabel(printData.source) }}</span>
                </div>
                <div class="print-info-item" v-if="printCfg.showTechnician !== false">
                  <span class="print-label">师傅</span>
                  <span class="print-value">{{ printData.assignedTo || printData.technician || '-' }}</span>
                </div>
                <div class="print-info-item">
                  <span class="print-label">预约日期</span>
                  <span class="print-value">{{ formatDate(printData.scheduledDate || printData.appointmentDate) }}</span>
                </div>
              </div>
            </div>

            <!-- Problem Description -->
            <div class="print-section">
              <div class="print-section-title">问题描述</div>
              <div class="print-section-body">{{ printData.description || '-' }}</div>
            </div>

            <!-- Processing Info -->
            <div class="print-section" v-if="printCfg.showProcessingInfo !== false">
              <div class="print-section-title">处理信息</div>
              <div class="print-info-row">
                <div class="print-info-item">
                  <span class="print-label">处理方式</span>
                  <span class="print-value">{{ resolutionLabel(printData.resolution) }}</span>
                </div>
                <div class="print-info-item">
                  <span class="print-label">费用</span>
                  <span class="print-value">{{ printData.cost != null ? formatMoney(printData.cost) : '-' }}</span>
                </div>
                <div class="print-info-item" v-if="printCfg.showWarranty !== false">
                  <span class="print-label">质保期内</span>
                  <span class="print-value">{{ printData.isWarranty ? '是' : '否' }}</span>
                </div>
              </div>
            </div>

            <!-- Remark -->
            <div class="print-section" v-if="printData.remark">
              <div class="print-section-title">备注</div>
              <div class="print-section-body">{{ printData.remark }}</div>
            </div>

            <!-- Signature Area -->
            <div class="print-signature" v-if="printCfg.showSignature !== false">
              <div class="sig-item">
                <span class="sig-label">客户签名：</span>
                <span class="sig-line"></span>
              </div>
              <div class="sig-item">
                <span class="sig-label">师傅签名：</span>
                <span class="sig-line"></span>
              </div>
              <div class="sig-item">
                <span class="sig-label">日期：</span>
                <span class="sig-line"></span>
              </div>
            </div>

            <!-- Footer -->
            <div class="print-footer">
              <span>{{ printCfg.companyName || '顺居门业' }} · 售后服务单</span>
              <span>打印日期：{{ new Date().toLocaleDateString('zh-CN') }}</span>
            </div>
            <div class="print-footer-text" v-if="printCfg.footerText">{{ printCfg.footerText }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div style="display:flex;justify-content:flex-end;gap:10px;">
          <el-button @click="previewDrawerVisible = false">关闭</el-button>
          <el-button type="primary" @click="doPrint">打印</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- ===== Add / Edit Dialog ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑售后单' : '新建售后单'"
      width="880px"
      :close-on-click-modal="false"
      close-on-press-escape
      destroy-on-close
      top="30px"
      :before-close="closeGuard"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <div class="dialog-section">
          <h4 class="section-title"><span class="section-dash"></span>基本信息</h4>
          <div class="form-row-2">
            <el-form-item label="关联订单" prop="saleOrderId">
              <el-select
                v-model="form.saleOrderId"
                filterable
                remote
                reserve-keyword
                placeholder="搜索订单..."
                :remote-method="searchOrders"
                :loading="orderSearching"
                @change="onOrderSelect"
                style="width:100%"
                clearable
              >
                <el-option
                  v-for="o in orderOptions"
                  :key="o.id"
                  :label="o.orderNo"
                  :value="o.id"
                >
                  <span>{{ o.orderNo }}</span>
                  <span v-if="o.customerName" style="float:right;color:#999;font-size:12px">{{ o.customerName }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="客户" prop="customerId">
              <el-select
                v-model="form.customerId"
                filterable
                remote
                reserve-keyword
                placeholder="搜索客户..."
                :remote-method="searchCustomers"
                :loading="customerSearching"
                @change="onCustomerSelect"
                style="width:100%"
              >
                <el-option
                  v-for="c in customerOptions"
                  :key="c.id"
                  :label="c.name"
                  :value="c.id"
                >
                  <span>{{ c.name }}</span>
                  <span v-if="c.phone" style="float:right;color:#999;font-size:12px">{{ c.phone }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </div>
          <div class="form-row-3">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="选择类型" style="width:100%">
                <el-option label="质保维修" value="warranty" />
                <el-option label="付费维修" value="repair" />
                <el-option label="安装问题" value="install" />
                <el-option label="投诉" value="complaint" />
              </el-select>
            </el-form-item>
            <el-form-item label="来源" prop="source">
              <el-select v-model="form.source" placeholder="选择来源" style="width:100%">
                <el-option label="电话" value="phone" />
                <el-option label="微信" value="wechat" />
                <el-option label="上门" value="visit" />
              </el-select>
            </el-form-item>
            <el-form-item label="师傅" prop="technician">
              <el-input v-model="form.technician" maxlength="50" placeholder="指派师傅姓名" />
            </el-form-item>
          </div>
          <div class="form-row-1">
            <el-form-item label="问题描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请详细描述售后问题..." />
            </el-form-item>
          </div>
          <div class="form-row-1">
            <el-form-item label="现场照片">
              <MultiImageUploader v-model="form.images" />
            </el-form-item>
          </div>
          <div class="photo-desc-editor" v-if="photoUrlList.length > 0">
            <div class="photo-desc-item" v-for="url in photoUrlList" :key="url">
              <img :src="url" class="photo-desc-thumb" />
              <el-input v-model="photoDescs[url]" size="small" placeholder="添加照片描述（可选）" maxlength="100" />
            </div>
          </div>
        </div>

        <div class="dialog-section">
          <h4 class="section-title"><span class="section-dash"></span>处理信息</h4>
          <div class="form-row-3">
            <el-form-item label="预约日期" prop="appointmentDate">
              <el-date-picker v-model="form.appointmentDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" style="width:100%" />
            </el-form-item>
            <el-form-item label="处理方式" prop="resolution">
              <el-select v-model="form.resolution" placeholder="选择处理方式" style="width:100%" clearable>
                <el-option label="维修" value="repair" />
                <el-option label="更换部件" value="replace" />
                <el-option label="整单更换" value="full_replace" />
                <el-option label="退款" value="refund" />
              </el-select>
            </el-form-item>
            <el-form-item label="费用" prop="cost">
              <el-input-number v-model="form.cost" :min="0" :precision="2" controls-position="right" style="width:100%" />
            </el-form-item>
          </div>
          <div class="form-row-2">
            <el-form-item label="质保期内">
              <el-switch v-model="form.isWarranty" />
            </el-form-item>
          </div>
          <div class="form-row-1">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="备注信息（选填）" />
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closeGuard(() => { dialogVisible = false })">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="saving" @click="handleSave">
          {{ editId ? '保存修改' : '创建售后单' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAfterSaleOrders,
  getAfterSaleOrderById,
  addAfterSaleOrder,
  updateAfterSaleOrder,
  deleteAfterSaleOrder,
  updateAfterSaleOrderStatus,
  getAfterSaleOrderSummary,
  getCustomers,
  getSaleOrders,
  restoreEntity,
} from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { usePrintConfig } from '@/composables/usePrintConfig'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { useTableSort } from '@/composables/useTableSort'
import RefreshButton from '@/components/RefreshButton.vue'
import MultiImageUploader from '@/components/MultiImageUploader.vue'
import { useCtrlEnterSave } from '@/composables/useCtrlEnterSave'
import { useUnsavedConfirm } from '@/composables/useUnsavedConfirm'

const { printConfig: asPrintConfig } = usePrintConfig('after_sale')

// ===== Status / Type / Resolution / Source Config =====
const statusTabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'pending' },
  { label: '已派单', value: 'assigned' },
  { label: '处理中', value: 'processing' },
  { label: '已完成', value: 'completed' },
  { label: '已关闭', value: 'closed' },
]

const statusMap = {
  pending: '待处理',
  assigned: '已派单',
  processing: '处理中',
  completed: '已完成',
  closed: '已关闭',
}

const typeMap = {
  warranty: '质保维修',
  repair: '付费维修',
  install: '安装问题',
  complaint: '投诉',
}

const resolutionMap = {
  repair: '维修',
  replace: '更换部件',
  full_replace: '整单更换',
  refund: '退款',
}

const sourceMap = {
  phone: '电话',
  wechat: '微信',
  visit: '上门',
}

function statusLabel(status) {
  return statusMap[status] || status || '-'
}

function typeLabel(type) {
  return typeMap[type] || type || '-'
}

function resolutionLabel(resolution) {
  return resolutionMap[resolution] || resolution || '-'
}

function sourceLabel(source) {
  return sourceMap[source] || source || '-'
}

// ===== List State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, {
  cost: 'number',
  scheduledDate: 'date',
}, { storageKey: 'after_sale_filters' })
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeStatus = ref('')
const keyword = ref('')
const startDate = ref(null)
const endDate = ref(null)
const summary = ref(null)

// ===== Batch Selection =====
const selectedIds = ref(new Set())
const batchOperating = ref(false)

const isAllSelected = computed(() => list.value.length > 0 && list.value.every(item => selectedIds.value.has(item.id)))
const isIndeterminate = computed(() => selectedIds.value.size > 0 && !isAllSelected.value)

function toggleSelectAll() {
  if (isAllSelected.value) { selectedIds.value = new Set() }
  else { selectedIds.value = new Set(list.value.map(item => item.id)) }
}
function toggleSelect(id) {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  selectedIds.value = s
}
function clearSelection() { selectedIds.value = new Set() }

async function handleBatchDelete() {
  if (!selectedIds.value.size) return
  const confirmed = await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.size} 条记录？`, '批量删除', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  batchOperating.value = true
  try {
    const ids = [...selectedIds.value]
    let success = 0, failedMsgs = []
    for (const id of ids) {
      try { await deleteAfterSaleOrder(id); success++ } catch (e) { failedMsgs.push(e?.response?.data?.msg || e?.message || '未知错误') }
    }
    if (failedMsgs.length) ElMessage.warning(`成功 ${success} 条，失败 ${failedMsgs.length} 条：${failedMsgs[0]}`)
    else ElMessage.success(`成功删除 ${success} 条`)
    clearSelection()
    loadList()
    loadSummary()
  } finally { batchOperating.value = false }
}

async function handleBatchStatus(newStatus) {
  if (!selectedIds.value.size) return
  const label = statusLabel(newStatus)
  const confirmed = await ElMessageBox.confirm(`确认将选中的 ${selectedIds.value.size} 条记录状态更改为「${label}」？`, '批量状态变更', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  batchOperating.value = true
  try {
    const ids = [...selectedIds.value]
    let success = 0
    for (const id of ids) {
      try { await updateAfterSaleOrderStatus(id, newStatus); success++ } catch (e) { console.error(e) }
    }
    ElMessage.success(`成功变更 ${success} 条状态为「${label}」`)
    clearSelection()
    loadList()
    loadSummary()
  } finally { batchOperating.value = false }
}

let searchTimer = null

// ===== Undo Delete / Edit =====
const { handleDelete: undoHandleDelete } = useUndoDelete({
  deleteApi: deleteAfterSaleOrder,
  restoreApi: (id) => restoreEntity('after-sale', id),
  onSuccess: () => {
    if (list.value.length === 1 && page.value > 1) {
      page.value--
    }
    loadList()
    loadSummary()
  },
  entityLabel: '售后单',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateAfterSaleOrder,
  onSuccess: () => { loadList(); loadSummary() },
  entityLabel: '售后单',
})

// URL filter persistence
const route = useRoute()
const router = useRouter()
const FILTER_KEY = 'after_sale_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.type || q.keyword || q.startDate || q.endDate || q.page
  if (hasUrlParams) {
    _restoring = true
    if (q.type) activeStatus.value = q.type
    if (q.keyword) keyword.value = q.keyword
    if (q.startDate) startDate.value = q.startDate
    if (q.endDate) endDate.value = q.endDate
    if (q.page) page.value = Number(q.page)
    if (q.pageSize) pageSize.value = Number(q.pageSize)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.type) activeStatus.value = saved.type
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.startDate) startDate.value = saved.startDate
    if (saved.endDate) endDate.value = saved.endDate
    if (saved.page) page.value = Number(saved.page)
    if (saved.pageSize) pageSize.value = Number(saved.pageSize)
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (activeStatus.value !== undefined && activeStatus.value !== '') {
    query.type = String(activeStatus.value)
    storage.type = activeStatus.value
  }
  if (keyword.value) {
    query.keyword = keyword.value
    storage.keyword = keyword.value
  }
  if (startDate.value) {
    query.startDate = startDate.value
    storage.startDate = startDate.value
  }
  if (endDate.value) {
    query.endDate = endDate.value
    storage.endDate = endDate.value
  }
  if (page.value > 1) {
    query.page = String(page.value)
    storage.page = page.value
  }
  if (pageSize.value !== 20) {
    query.pageSize = String(pageSize.value)
    storage.pageSize = pageSize.value
  }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

const hasFilter = computed(() => {
  return !!(activeStatus.value || keyword.value.trim() || startDate.value || endDate.value)
})

// ===== Watchers =====
watch(activeStatus, () => {
  if (_restoring) return
  page.value = 1
  loadList()
  syncFiltersToUrl()
})
watch(page, () => { if (!_restoring) { loadList(); syncFiltersToUrl() } })
watch(pageSize, () => {
  if (_restoring) return
  if (page.value === 1) { loadList(); syncFiltersToUrl() }
  else { page.value = 1 }
})

// ===== Detail State =====
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref(null)

// ===== Print State =====
const previewDrawerVisible = ref(false)
const printPageRef = ref(null)
const printData = ref({})

const printCfg = computed(() => asPrintConfig.value || {})

const printPageStyle = computed(() => {
  const cfg = printCfg.value || {}
  const portrait = cfg.orientation !== 'landscape'
  const w = cfg.pageWidthMm || 210
  const h = cfg.pageHeightMm || 297
  return {
    width: portrait ? `${w}mm` : '297mm',
    minHeight: portrait ? `${h}mm` : '210mm',
    padding: `${cfg.marginTop || 10}mm ${cfg.marginRight || 8}mm ${cfg.marginBottom || 10}mm ${cfg.marginLeft || 8}mm`,
    fontSize: (cfg.fontSize || 12) + 'px',
    boxSizing: 'border-box',
  }
})

function handlePrint() {
  printData.value = detailOrder.value || {}
  previewDrawerVisible.value = true
}

function doPrint() {
  const printContent = printPageRef.value
  if (!printContent) return
  const cfg = printCfg.value
  const fs = cfg.fontSize || 12
  const mt = cfg.marginTop || 10
  const mr = cfg.marginRight || 8
  const mb = cfg.marginBottom || 10
  const ml = cfg.marginLeft || 8
  const orient = cfg.orientation || 'portrait'
  const pageW = cfg.pageWidthMm || 210 // 纸张宽度(mm)，A4=210；复写纸按实际
  const pageH = cfg.pageHeightMm || 297 // 页面高度(mm)，A4=297；复写纸按实际
  // 横向 A4 保持标准；纵向/自定义纸：宽×高 都按设置走
  let pageSize
  if (orient === 'landscape') pageSize = 'A4 landscape'
  else pageSize = `${pageW}mm ${pageH}mm`
  const contentWidth = `${Math.max(50, pageW - 16)}mm` // 纸宽减去两侧安全边距

  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>顺居门业售后单 - ${printData.value.orderNo || ''}</title>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'SimHei', 'SimSun', 'FangSong', sans-serif; font-size: ${fs}px; color: #000; padding: ${mt}mm ${mr}mm ${mb}mm ${ml}mm; background: #fff; }
        .print-page { width: 100%; max-width: ${contentWidth}; margin: 0 auto; }
        .print-header { text-align: center; margin-bottom: 20px; padding-bottom: 15px; border-bottom: 3px solid #000; }
        .print-title { font-size: 22px; font-weight: 900; letter-spacing: 4px; margin-bottom: 6px; background: #000; color: #fff; padding: 10px 20px; }
        .print-company { font-size: 13px; color: #000; }
        .print-info-section { margin-bottom: 16px; }
        .print-info-row { display: flex; gap: 20px; margin-bottom: 8px; }
        .print-info-item { flex: 1; display: flex; gap: 6px; }
        .print-label { color: #000; white-space: nowrap; min-width: 60px; }
        .print-value { font-weight: 500; }
        .mono { font-family: "Courier New", monospace; letter-spacing: 1px; }
        .print-section { margin-bottom: 14px; }
        .print-section-title { font-size: 13px; font-weight: 700; color: #000; padding-bottom: 6px; border-bottom: 2px solid #000; margin-bottom: 8px; }
        .print-section-body { line-height: 1.8; color: #000; white-space: pre-wrap; }
        .print-signature { display: flex; gap: 40px; margin-top: 40px; padding-top: 20px; }
        .sig-item { display: flex; align-items: baseline; gap: 4px; }
        .sig-label { white-space: nowrap; color: #000; }
        .sig-line { display: inline-block; width: 120px; border-bottom: 3px solid #000; }
        .print-footer { margin-top: 30px; padding-top: 10px; border-top: 2px solid #000; display: flex; justify-content: space-between; font-size: 10px; color: #000; }
        .print-footer-text { text-align: right; font-size: 9px; color: #000; margin-top: 2px; }
        @media print {
          body { padding: ${mt}mm ${mr}mm ${mb}mm ${ml}mm; }
          @page { size: ${pageSize}; margin: 0; }
        }
      </style>
    </head>
    <body>
      <div class="print-page">
        ${printContent.innerHTML}
      </div>
    </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.onload = () => {
    printWindow.print()
    printWindow.close()
  }
}

// ===== Dialog State =====
const dialogVisible = ref(false)
const editId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  saleOrderId: null,
  saleOrderNo: '',
  customerId: null,
  customerName: '',
  type: '',
  source: '',
  description: '',
  images: '',
  technician: '',
  appointmentDate: '',
  resolution: '',
  cost: null,
  isWarranty: false,
  remark: '',
})

const form = reactive(defaultForm())
useCtrlEnterSave(dialogVisible, handleSave)
const { formDirty, resetDirty, closeGuard } = useUnsavedConfirm(dialogVisible, () => JSON.stringify(form), { form })
const photoDescs = ref({}) // {url: description} — per-photo descriptions

const rules = {
  customerId: [
    { required: true, message: '请选择客户', trigger: 'change' },
  ],
  type: [
    { required: true, message: '请选择类型', trigger: 'change' },
  ],
  description: [
    { required: true, message: '请填写问题描述', trigger: 'blur' },
  ],
}

// ===== Remote Select Options =====
const orderOptions = ref([])
const orderSearching = ref(false)
const customerOptions = ref([])
const customerSearching = ref(false)

let orderSearchTimer = null
let customerSearchTimer = null
let _orderSearchSeq = 0
let _customerSearchSeq = 0

async function searchOrders(query) {
  clearTimeout(orderSearchTimer)
  if (!query && orderOptions.value.length) return

  orderSearchTimer = setTimeout(async () => {
    const seq = ++_orderSearchSeq
    orderSearching.value = true
    try {
      const params = { size: 50 }
      if (query) params.keyword = query.trim()
      const res = await getSaleOrders(params)
      if (seq !== _orderSearchSeq) return
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
      orderOptions.value = records
    } catch (e) {
      console.error('搜索订单失败:', e)
      if (seq === _orderSearchSeq) orderOptions.value = []
    } finally {
      orderSearching.value = false
    }
  }, 300)
}

function onOrderSelect(orderId) {
  const order = orderOptions.value.find((o) => o.id === orderId)
  if (order) {
    form.saleOrderNo = order.orderNo || ''
    if (order.customerId && !form.customerId) {
      form.customerId = order.customerId
      form.customerName = order.customerName || ''
      if (order.customerName) {
        customerOptions.value = [{ id: order.customerId, name: order.customerName }]
      }
    }
  } else {
    form.saleOrderNo = ''
  }
}

async function searchCustomers(query) {
  clearTimeout(customerSearchTimer)
  if (!query && customerOptions.value.length) return

  customerSearchTimer = setTimeout(async () => {
    const seq = ++_customerSearchSeq
    customerSearching.value = true
    try {
      const params = { size: 50 }
      if (query) params.keyword = query.trim()
      const res = await getCustomers(params)
      if (seq !== _customerSearchSeq) return
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
      customerOptions.value = records
    } catch (e) {
      console.error('搜索客户失败:', e)
      if (seq === _customerSearchSeq) customerOptions.value = []
    } finally {
      customerSearching.value = false
    }
  }, 300)
}

function onCustomerSelect(customerId) {
  const customer = customerOptions.value.find((c) => c.id === customerId)
  if (customer) {
    form.customerName = customer.name || ''
  }
}

// ===== Image Helpers =====
// Parse images from either JSON array [{url,desc}] or legacy comma-separated URLs
function parseImages(images) {
  if (!images) return []
  // Try JSON array format first
  if (images.startsWith('[')) {
    try {
      const arr = JSON.parse(images)
      if (Array.isArray(arr)) return arr.map(item => typeof item === 'string' ? item : item.url).filter(Boolean)
    } catch { /* fall through */ }
  }
  // Legacy comma-separated format
  return images.split(',').map(s => s.trim()).filter(Boolean)
}

// Parse images with descriptions for detail drawer
function parseImagesWithDesc(images) {
  if (!images) return []
  if (images.startsWith('[')) {
    try {
      const arr = JSON.parse(images)
      if (Array.isArray(arr)) return arr.map(item => typeof item === 'string' ? { url: item, desc: '' } : { url: item.url, desc: item.desc || '' }).filter(i => i.url)
    } catch { /* fall through */ }
  }
  return images.split(',').map(s => s.trim()).filter(Boolean).map(url => ({ url, desc: '' }))
}

// Computed URL list from form.images for photo desc editor
const photoUrlList = computed(() => {
  return form.images ? form.images.split(',').map(s => s.trim()).filter(Boolean) : []
})

// Build images JSON with descriptions for save
function buildImagesJson() {
  const urls = photoUrlList.value
  if (urls.length === 0) return ''
  const hasDescs = urls.some(u => photoDescs.value[u])
  if (!hasDescs) return urls.join(',') // No descriptions, keep legacy format for simplicity
  return JSON.stringify(urls.map(url => ({ url, desc: photoDescs.value[url] || '' })))
}

function previewImage(url) {
  window.open(url, '_blank')
}

// ===== Formatters =====
function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatMoney(v) {
  if (v == null || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ===== Lifecycle =====
onMounted(() => {
  restoreFilters()
  loadList()
  loadSummary()
})

// ===== Data Loading =====
async function loadList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
    }
    if (activeStatus.value) params.status = activeStatus.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await getAfterSaleOrders(params)
    const data = res.data?.data ?? res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data?.records || data?.list || data?.content || []
      total.value = data?.total ?? list.value.length
    }
  } catch (e) {
    console.error('加载售后单列表失败:', e)
    if (!e._handled) ElMessage.error('加载售后列表失败，请稍后重试')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    const params = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await getAfterSaleOrderSummary(params)
    summary.value = res.data?.data ?? res.data ?? null
  } catch {
    summary.value = null
  }
}

// ===== Toolbar Handlers =====
function switchStatus(val) {
  activeStatus.value = val
  syncFiltersToUrl()
}

function onStartDateChange() {
  if (!validateDateRange(startDate.value, endDate.value, 'start')) { startDate.value = null; return }
  onFilterChange()
}
function onEndDateChange() {
  if (!validateDateRange(startDate.value, endDate.value, 'end')) { endDate.value = null; return }
  onFilterChange()
}
function onFilterChange() {
  page.value = 1
  loadList()
  loadSummary()
  syncFiltersToUrl()
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadList()
    loadSummary()
    syncFiltersToUrl()
  }, 300)
}

function handleSearch() {
  page.value = 1
  loadList()
  loadSummary()
  syncFiltersToUrl()
}

function handleReset() {
  activeStatus.value = ''
  keyword.value = ''
  startDate.value = null
  endDate.value = null
  page.value = 1
  pageSize.value = 20
  sessionStorage.removeItem(FILTER_KEY)
  router.replace({ query: {} })
  loadList()
  loadSummary()
}

// ===== Detail Drawer =====
async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  try {
    const res = await getAfterSaleOrderById(row.id)
    const data = res.data?.data ?? res.data
    detailOrder.value = data || row
  } catch (e) {
    console.error('加载售后单详情失败:', e)
    if (!e._handled) ElMessage.error('加载售后单详情失败')
    detailOrder.value = row
  } finally {
    detailLoading.value = false
  }
}

// ===== Status Change =====
async function handleChangeStatus(row, newStatus) {
  if (row.status === newStatus) return
  const label = statusLabel(newStatus)
  try {
    await ElMessageBox.confirm(
      `确定将售后单「${row.orderNo}」的状态更改为「${label}」吗？`,
      '更改状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await updateAfterSaleOrderStatus(row.id, newStatus)
    ElMessage.success(`状态已更改为「${label}」`)
    row.status = newStatus
    loadSummary()
  } catch (e) {
    console.error('更改状态失败:', e)
    if (!e._handled) ElMessage.error('更改状态失败，请稍后重试')
  }
}

// ===== Delete =====
function handleDelete(row) {
  undoHandleDelete(row)
}

// ===== Add / Edit Dialog =====
function openAdd() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  snapshotBeforeEdit(row)
  editId.value = row.id
  resetForm()

  // Pre-fill from row
  form.saleOrderId = row.saleOrderId || null
  form.saleOrderNo = row.saleOrderNo || ''
  form.customerId = row.customerId || null
  form.customerName = row.customerName || ''
  form.type = row.type || ''
  form.source = row.source || ''
  form.description = row.description || ''
  form.images = row.images || ''
  form.technician = row.assignedTo || row.technician || ''
  form.appointmentDate = row.scheduledDate || row.appointmentDate || ''
  form.resolution = row.resolution || ''
  form.cost = row.cost ?? null
  form.isWarranty = !!row.isWarranty
  form.remark = row.remark || ''

  // Ensure options are populated for pre-selected values
  if (form.customerId && form.customerName) {
    customerOptions.value = [{ id: form.customerId, name: form.customerName }]
  }
  if (form.saleOrderId && form.saleOrderNo) {
    orderOptions.value = [{ id: form.saleOrderId, orderNo: form.saleOrderNo, customerName: form.customerName }]
  }

  // Load full detail
  try {
    const res = await getAfterSaleOrderById(row.id)
    const data = res.data?.data ?? res.data
    if (data) {
      form.saleOrderId = data.saleOrderId ?? form.saleOrderId
      form.saleOrderNo = data.saleOrderNo || form.saleOrderNo
      form.customerId = data.customerId ?? form.customerId
      form.customerName = data.customerName || form.customerName
      form.type = data.type || form.type
      form.source = data.source || form.source
      form.description = data.description || form.description
      // Parse images with descriptions
      if (data.images && data.images.startsWith('[')) {
        try {
          const arr = JSON.parse(data.images)
          if (Array.isArray(arr)) {
            const urls = []
            const descs = {}
            arr.forEach(item => {
              if (typeof item === 'string') { urls.push(item) }
              else if (item.url) { urls.push(item.url); if (item.desc) descs[item.url] = item.desc }
            })
            form.images = urls.join(',')
            photoDescs.value = descs
          }
        } catch { form.images = data.images || form.images; photoDescs.value = {} }
      } else {
        form.images = data.images || form.images
        photoDescs.value = {}
      }
      form.technician = data.assignedTo || data.technician || form.technician
      form.appointmentDate = data.scheduledDate || data.appointmentDate || form.appointmentDate
      form.resolution = data.resolution || form.resolution
      form.cost = data.cost ?? form.cost
      form.isWarranty = data.isWarranty ?? form.isWarranty
      form.remark = data.remark || form.remark
    }
  } catch (e) {
    console.error('加载售后单详情失败:', e)
  }

  dialogVisible.value = true
}

function resetForm() {
  const df = defaultForm()
  Object.keys(df).forEach((key) => {
    form[key] = df[key]
  })
  photoDescs.value = {}
}

// ===== Save =====
async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const data = {
      saleOrderId: form.saleOrderId,
      saleOrderNo: form.saleOrderNo,
      customerId: form.customerId,
      customerName: form.customerName,
      type: form.type,
      source: form.source,
      description: form.description.trim(),
      images: buildImagesJson(),
      technician: form.technician.trim(),
      appointmentDate: form.appointmentDate || null,
      resolution: form.resolution || null,
      cost: form.cost,
      isWarranty: form.isWarranty,
      remark: form.remark.trim(),
    }

    if (editId.value) {
      await updateAfterSaleOrder(editId.value, data)
      afterEditSave({ id: editId.value, ...data })
    } else {
      await addAfterSaleOrder(data)
      ElMessage.success('售后单已创建')
    }
    resetDirty()
    dialogVisible.value = false
    loadList()
    loadSummary()
  } catch (e) {
    console.error('保存售后单失败:', e)
    if (!e._handled) ElMessage.error(editId.value ? '更新失败，请稍后重试' : '创建失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
/* ===== Page Header ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: -0.5px;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
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

/* ===== Toolbar ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Status Tabs */
.status-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 3px;
}
.status-tab {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #000;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  white-space: nowrap;
}
.status-tab:hover {
  color: #C5A265;
  background: rgba(197, 162, 101, 0.06);
}
.status-tab.active {
  color: #fff;
  background: #C5A265;
  font-weight: 600;
}

/* Search */
.search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 6px 12px;
  transition: border-color 0.2s;
}
.search-wrap:focus-within {
  border-color: #C5A265;
}
.search-wrap svg {
  color: #bbb;
  flex-shrink: 0;
}
.search-input {
  border: none;
  outline: none;
  font-size: 13px;
  color: #000;
  width: 220px;
  font-family: inherit;
  background: transparent;
}
.search-input::placeholder {
  color: #ccc;
}

.btn-search {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #000;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-search:hover {
  border-color: #C5A265;
  color: #C5A265;
}
.btn-search:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-reset {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #000;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-reset:hover {
  border-color: #000;
  color: #000;
}

/* ===== Summary Bar ===== */
.summary-bar {
  display: flex;
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 10px;
  margin-bottom: 14px;
  overflow: hidden;
}
.summary-card {
  flex: 1;
  padding: 14px 10px;
  text-align: center;
  border-right: 1px solid rgba(255,255,255,0.08);
}
.summary-card:last-child {
  border-right: none;
}
.sc-label {
  font-size: 11px;
  color: rgba(255,255,255,0.50);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}
.sc-value {
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  font-variant-numeric: tabular-nums;
}
.summary-card.gold .sc-value  { color: #ffd54f; }
.summary-card.green .sc-value { color: #69db7c; }

/* ===== Table ===== */
.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: auto;
  min-height: 120px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1100px;
}
.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 12px;
  padding: 13px 14px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
  user-select: none;
}
.data-table th.sortable {
  cursor: pointer;
}
.data-table th.sortable:hover {
  color: #C5A265;
}
.data-table td {
  padding: 13px 14px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #000;
  vertical-align: middle;
}
.data-table tbody tr {
  transition: background 0.12s;
}
.data-table tbody tr:hover {
  background: #fafbfc;
}

.col-order-no {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #000;
  white-space: nowrap;
}
.order-no-link {
  color: #C5A265;
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s;
}
.order-no-link:hover {
  color: #b89555;
  text-decoration: underline;
}
.col-customer {
  font-weight: 600;
  color: #1a1a1a;
}
.col-desc {
  max-width: 180px;
}
.desc-text {
  display: inline-block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #000;
  font-size: 12px;
}
.col-date {
  font-size: 12px;
  color: #000;
  white-space: nowrap;
}
.col-money {
  font-weight: 600;
  white-space: nowrap;
  font-size: 13px;
  color: #555;
}
.col-money.gold {
  color: #C5A265;
}
.col-status {
  white-space: nowrap;
}
.col-action {
  white-space: nowrap;
}

/* Type Tags */
.type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.type-warranty {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
.type-repair {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}
.type-install {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.type-complaint {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}

/* Status Badges */
.status-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.status-pending {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}
.status-assigned {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
.status-processing {
  background: #fffbe6;
  color: #d4b106;
  border: 1px solid #ffe58f;
}
.status-completed {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.status-closed {
  background: #f5f5f5;
  color: #000;
  border: 1px solid #d9d9d9;
}

/* Warranty Tag */
.warranty-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  background: #fff7e6;
  color: #d46b08;
}
.warranty-tag.active {
  background: #f6ffed;
  color: #389e0d;
}

/* Action buttons */
.btn-text {
  background: none;
  border: none;
  color: #C5A265;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.15s;
  font-family: inherit;
  font-weight: 500;
}
.btn-text:hover {
  background: rgba(197, 162, 101, 0.08);
}
.btn-text.danger {
  color: #e74c3c;
}
.btn-text.danger:hover {
  background: rgba(231, 76, 60, 0.06);
}
.status-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.status-btn svg {
  transition: transform 0.2s;
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
  font-size: 14px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

/* ===== Detail Drawer ===== */
.detail-content {
  padding: 0 4px;
}
.detail-section {
  margin-bottom: 28px;
}
.detail-section:last-child {
  margin-bottom: 0;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
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

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}
.detail-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-field.full {
  grid-column: 1 / -1;
}
.field-label {
  font-size: 11px;
  color: #000;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.field-value {
  font-size: 13px;
  color: #000;
  font-weight: 500;
}
.field-value.mono {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  color: #000;
}
.field-value.money {
  font-weight: 700;
  font-size: 14px;
  color: #000;
}
.field-value.money.gold {
  color: #C5A265;
}

.detail-remark {
  font-size: 13px;
  color: #000;
  line-height: 1.6;
  padding: 12px 16px;
  background: #fafbfc;
  border-radius: 6px;
  margin: 0;
}

/* Photo Wall */
.photo-wall {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}
.photo-wall-item {
  display: flex;
  flex-direction: column;
}
.photo-thumb {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.photo-thumb:hover {
  transform: scale(1.03);
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.photo-desc {
  font-size: 11px;
  color: #000;
  margin-top: 4px;
  line-height: 1.4;
  word-break: break-all;
}

/* Photo Description Editor (in edit dialog) */
.photo-desc-editor {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.photo-desc-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.photo-desc-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  flex-shrink: 0;
}

/* ===== Add/Edit Dialog ===== */
.dialog-section {
  margin-bottom: 24px;
}
.dialog-section:last-of-type {
  margin-bottom: 8px;
}

.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0 16px;
  margin-bottom: 4px;
}
.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
  margin-bottom: 4px;
}
.form-row-1 {
  margin-bottom: 4px;
}

/* Element Plus Overrides */
.btn-gold {
  background: #C5A265 !important;
  border-color: #C5A265 !important;
}
.btn-gold:hover,
.btn-gold:focus {
  background: #d4b885 !important;
  border-color: #d4b885 !important;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 24px;
  margin-right: 0;
}
:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
:deep(.el-dialog__body) {
  padding: 24px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
:deep(.el-dialog__footer) {
  border-top: 1px solid #f0f0f0;
  padding: 14px 24px;
}
:deep(.el-form-item__label) {
  font-size: 13px;
  color: #555;
}
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 6px;
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
:deep(.el-dropdown-menu__item.is-disabled) {
  opacity: 0.4;
}

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; margin-bottom: 12px;
  background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px;
}
.batch-info { font-size: 13px; color: #d46b08; font-weight: 600; }
.btn-batch {
  padding: 6px 14px; border: 1px solid #e0e0e0; border-radius: 4px;
  background: #fff; color: #000; font-size: 12px; cursor: pointer;
}
.btn-batch:hover { border-color: #C5A265; color: #C5A265; }
.btn-batch-danger {
  padding: 6px 14px; border: 1px solid #ff4d4f; border-radius: 4px;
  background: #fff; color: #ff4d4f; font-size: 12px; cursor: pointer;
}
.btn-batch-danger:hover { background: #ff4d4f; color: #fff; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right {
    flex-wrap: wrap;
  }
  .search-input {
    width: 100%;
  }
  .status-tabs {
    overflow-x: auto;
  }
  .summary-bar {
    flex-wrap: wrap;
  }
  .summary-card {
    flex: 1 1 50%;
    min-width: 120px;
  }
  .form-row-3,
  .form-row-2 {
    grid-template-columns: 1fr;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== Print Preview ===== */
.print-page {
  background: #fff;
  width: 210mm;
  min-height: 297mm;
  margin: 0 auto;
  padding: 20mm;
  box-shadow: 0 2px 12px rgba(0,0,0,.15);
  box-sizing: border-box;
  font-size: 12px;
  color: #000;
}
.print-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 3px solid #000;
}
.print-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 6px;
  margin-bottom: 8px;
  background: #000;
  color: #fff;
  padding: 10px 20px;
}
.print-company {
  font-size: 14px;
  color: #000;
}
.print-info-section {
  margin-bottom: 20px;
}
.print-info-row {
  display: flex;
  gap: 24px;
  margin-bottom: 10px;
}
.print-info-item {
  flex: 1;
  display: flex;
  gap: 8px;
  align-items: baseline;
}
.print-label {
  color: #000;
  white-space: nowrap;
  min-width: 60px;
  font-size: 11px;
}
.print-value {
  font-weight: 500;
  font-size: 12px;
}
.print-section {
  margin-bottom: 18px;
}
.print-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #000;
  padding-bottom: 8px;
  border-bottom: 1px solid #ddd;
  margin-bottom: 10px;
}
.print-section-body {
  line-height: 1.8;
  color: #000;
  white-space: pre-wrap;
  font-size: 12px;
}
.print-signature {
  display: flex;
  gap: 40px;
  margin-top: 50px;
  padding-top: 24px;
}
.sig-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.sig-label {
  white-space: nowrap;
  color: #000;
  font-size: 12px;
}
.sig-line {
  display: inline-block;
  width: 120px;
  border-bottom: 1px solid #333;
}
.print-footer {
  margin-top: 40px;
  padding-top: 12px;
  border-top: 1px solid #ddd;
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #000;
}
.print-footer-text {
  text-align: right;
  font-size: 9px;
  color: #000;
  margin-top: 2px;
}
</style>

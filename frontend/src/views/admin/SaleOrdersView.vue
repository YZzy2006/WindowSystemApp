<template>
  <div class="page admin-page">
    <!-- ===== Header ===== -->
    <div class="page-header">
      <h2>销售订单</h2>
      <div class="header-right">
        <RefreshButton :on-refresh="loadList" />
        <el-dropdown trigger="click" @command="handleExportCommand">
          <button class="btn-outline">导出Excel <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="page-detail">当前页 - 详细</el-dropdown-item>
              <el-dropdown-item command="page-simple">当前页 - 简易</el-dropdown-item>
              <el-dropdown-item divided command="all-detail">全部 - 详细</el-dropdown-item>
              <el-dropdown-item command="all-simple">全部 - 简易</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button class="btn-outline" @click="importDialogVisible = true">导入</button>
        <button class="btn-outline" @click="handlePrint">打印</button>
        <button class="btn-outline" @click="openAdd('presale')">+ 新建预算单</button>
        <button class="btn-primary" @click="openAdd('sale')">+ 新建销售单</button>
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
          class="date-picker"
          style="width: 155px"
        />
        <span style="margin: 0 4px; color: #999;">至</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="截止日期"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          size="default"
          :clearable="true"
          @change="onEndDateChange"
          class="date-picker"
          style="width: 155px"
        />
        <div class="search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="keyword" placeholder="搜单号 / 客户名 / 电话..." class="search-input" @input="onSearchInput" />
        </div>
      </div>
    </div>

    <!-- ===== Summary Bar ===== -->
    <div class="summary-bar" v-if="summary">
      <div class="summary-card">
        <div class="sc-label">{{ summaryLabel }}</div>
        <div class="sc-value">{{ summary.totalCount ?? total }} 单</div>
      </div>
      <div class="summary-card gold">
        <div class="sc-label">订单总额</div>
        <div class="sc-value">{{ formatMoney(summary.totalAmount) }}</div>
      </div>
      <div class="summary-card green">
        <div class="sc-label">已收金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalPaid) }}</div>
      </div>
      <div class="summary-card red">
        <div class="sc-label">未收金额</div>
        <div class="sc-value">{{ formatMoney(summary.totalUnpaid) }}</div>
      </div>
      <div class="summary-card blue">
        <div class="sc-label">总成本</div>
        <div class="sc-value">{{ formatMoney(summary.totalCost) }}</div>
      </div>
      <div class="summary-card teal">
        <div class="sc-label">毛利</div>
        <div class="sc-value">{{ formatMoney(summary.totalProfit) }}</div>
      </div>
    </div>

    <!-- ===== Table ===== -->
    <div class="card" v-loading="loading">
      <table class="data-table" v-if="list.length" @mousedown="onListMouseDown" @mousemove="onListMouseMove" @mouseup="onListMouseUp" @mouseleave="onListMouseUp">
        <thead>
          <tr>
            <th style="width:40px"><input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAllRows" /></th>
            <th class="col-order-no sortable" @click="toggleSort('orderNo')">单号{{ sortArrow('orderNo') }}</th>
            <th>类型</th>
            <th class="sortable" @click="toggleSort('customerName')">客户名称{{ sortArrow('customerName') }}</th>
            <th>联系电话</th>
            <th class="col-date sortable" @click="toggleSort('orderDate')">订单日期{{ sortArrow('orderDate') }}</th>
            <th class="col-money sortable" @click="toggleSort('totalAmount')">总金额{{ sortArrow('totalAmount') }}</th>
            <th class="col-money">定金</th>
            <th class="col-money sortable" @click="toggleSort('paidAmount')">已收{{ sortArrow('paidAmount') }}</th>
            <th class="col-money">未收</th>
            <th class="col-status">结清</th>
            <th class="col-status">状态</th>
            <th style="width:260px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in sortedList" :key="item.id" :class="{ 'selected-row': selectedIds.includes(item.id) }" class="row-clickable" @click="handleRowClick($event, item)">
            <td><input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleSelectRow(item.id)" /></td>
            <td class="col-order-no">{{ item.orderNo || '-' }}</td>
            <td>
              <span class="type-tag" :class="item.orderType === 'presale' ? 'type-presale' : 'type-sale'">
                {{ item.orderType === 'presale' ? '预算单' : '销售单' }}
              </span>
            </td>
            <td class="col-customer">{{ item.customerName || '-' }}</td>
            <td>{{ item.customerPhone || '-' }}</td>
            <td class="col-date">{{ formatDate(item.orderDate) }}</td>
            <td class="col-money gold">{{ formatMoney(item.totalAmount) }}</td>
            <td class="col-money">{{ formatMoney(item.deposit) }}</td>
            <td class="col-money">{{ formatMoney(item.paidAmount) }}</td>
            <td class="col-money" :class="{ 'money-red': getUnpaid(item) > 0 }">{{ formatMoney(getUnpaid(item)) }}</td>
            <td class="col-status">
              <span class="clear-badge" :class="item.status === 'cancelled' ? 'cancelled' : (item.isCleared ? 'cleared' : 'uncleared')">{{ item.status === 'cancelled' ? '已取消' : (item.isCleared ? '已结清' : '未结清') }}</span>
            </td>
            <td class="col-status">
              <span class="status-badge" :class="'status-' + item.status">{{ statusLabel(item.status) }}</span>
            </td>
            <td class="col-action">
              <button class="btn-text" @click="openDetail(item)">详情</button>
              <button class="btn-text" @click="handlePreview(item)">预览</button>
              <button class="btn-text" @click="openEdit(item)">编辑</button>
              <button v-if="item.orderType === 'presale'" class="btn-text confirm-btn" @click="handleConfirmSale(item)">转销售单</button>
              <el-dropdown trigger="click" @command="(cmd) => handleDropdownCommand(item, cmd)">
                <button class="btn-text status-btn">状态<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="pending" :disabled="item.status === 'pending'">待处理</el-dropdown-item>
                    <el-dropdown-item command="processing" :disabled="item.status === 'processing'">进行中</el-dropdown-item>
                    <el-dropdown-item command="completed" :disabled="item.status === 'completed'">已完成</el-dropdown-item>
                    <el-dropdown-item command="cancelled" :disabled="item.status === 'cancelled'">已取消</el-dropdown-item>
                    <el-dropdown-item :command="item.isCleared ? 'unclear' : 'clear'" divided>
                      <span :style="{ color: item.isCleared ? '#faad14' : '#52c41a' }">{{ item.isCleared ? '取消结清' : '标记结清' }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <button class="btn-text danger" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <TableSkeleton v-else-if="loading" :columns="13" :rows="8" />
      <div v-else class="empty-state">
        <p>{{ hasFilter ? '未匹配到订单' : '暂无销售订单' }}</p>
      </div>
    </div>

    <!-- ===== Batch Action Bar ===== -->
    <div class="batch-bar" v-if="list.length > 0">
      <span class="batch-count" v-if="selectedIds.length > 0">已选 {{ selectedIds.length }} 项</span>
      <span class="batch-hint" v-else>勾选左侧复选框进行批量操作</span>
      <span v-if="selectedIds.length > 0" style="display:contents">
      <button class="btn-outline batch-btn danger" @click="handleBatchDelete" :disabled="batchOperating || !selectedIds.length">{{ batchOperating ? '处理中...' : '批量删除' }}</button>
      <el-dropdown trigger="click" @command="batchChangeStatus">
        <button class="btn-outline batch-btn" :disabled="batchOperating || !selectedIds.length">批量改状态 <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="pending">待处理</el-dropdown-item>
            <el-dropdown-item command="processing">进行中</el-dropdown-item>
            <el-dropdown-item command="completed">已完成</el-dropdown-item>
            <el-dropdown-item command="cancelled">已取消</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown trigger="click" @command="handleBatchToggleCleared">
        <button class="btn-outline batch-btn" :disabled="batchOperating || !selectedIds.length">批量改结清 <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="clear">标记结清</el-dropdown-item>
            <el-dropdown-item command="unclear">取消结清</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button class="btn-outline batch-btn" @click="clearSelection">取消选择</button>
      </span>
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
      title="订单详情"
      direction="rtl"
      size="680px"
      close-on-press-escape
      :destroy-on-close="true"
      @closed="router.replace({ query: { ...route.query, detailId: undefined } })"
    >
      <div class="detail-content" v-loading="detailLoading" v-if="detailOrder">
        <!-- Order Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>订单信息</h4>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">订单编号</span>
              <span class="field-value mono">{{ detailOrder.orderNo || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">订单状态</span>
              <span class="status-badge" :class="'status-' + detailOrder.status">{{ statusLabel(detailOrder.status) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">客户名称</span>
              <span class="field-value">{{ detailOrder.customerName || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">联系电话</span>
              <span class="field-value">{{ detailOrder.customerPhone || '-' }}</span>
            </div>
            <div class="detail-field full">
              <span class="field-label">客户地址</span>
              <span class="field-value">{{ detailOrder.customerAddress || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">订单日期</span>
              <span class="field-value">{{ formatDate(detailOrder.orderDate) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">订单类型</span>
              <span class="field-value">{{ detailOrder.orderType === 'sale' ? '销售单' : detailOrder.orderType || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- Financial Info -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>财务信息</h4>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">订单总金额</span>
              <span class="field-value money gold">{{ formatMoney(detailOrder.totalAmount) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">定金</span>
              <span class="field-value money">{{ formatMoney(detailOrder.deposit) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">已收金额</span>
              <span class="field-value money">{{ formatMoney(detailOrder.paidAmount) }}</span>
            </div>
            <div class="detail-field">
              <span class="field-label">是否结清</span>
              <span class="field-value">
                <span class="clear-tag" :class="detailOrder.status === 'cancelled' ? 'cancelled' : (detailOrder.isCleared ? 'cleared' : '')">{{ detailOrder.status === 'cancelled' ? '已取消' : (detailOrder.isCleared ? '已结清' : '未结清') }}</span>
              </span>
            </div>
          </div>
        </div>

        <!-- Remark / Notice -->
        <div class="detail-section" v-if="detailOrder.remark || detailOrder.notice">
          <h4 class="section-title"><span class="section-dash"></span>备注与通知</h4>
          <div class="detail-grid">
            <div class="detail-field full" v-if="detailOrder.remark">
              <span class="field-label">备注</span>
              <span class="field-value">{{ detailOrder.remark }}</span>
            </div>
            <div class="detail-field full" v-if="detailOrder.notice">
              <span class="field-label">通知</span>
              <span class="field-value">{{ detailOrder.notice }}</span>
            </div>
          </div>
        </div>

        <!-- Items Table -->
        <div class="detail-section">
          <h4 class="section-title"><span class="section-dash"></span>商品明细</h4>
          <div class="items-table-wrap">
            <table class="items-table" v-if="detailItems.length">
              <thead>
                <tr>
                  <th>商品名称</th>
                  <th v-if="detailItems.some(i => i.series)">系列</th>
                  <th v-if="detailItems.some(i => i.color)">颜色</th>
                  <th v-if="detailItems.some(i => i.width)">宽/mm</th>
                  <th v-if="detailItems.some(i => i.height)">高/mm</th>
                  <th v-if="detailItems.some(i => i.wallThickness)">墙厚/mm</th>
                  <th v-if="detailItems.some(i => i.glassType)">玻璃款式</th>
                  <th v-if="detailItems.some(i => i.lockPosition)">锁位</th>
                  <th v-if="detailItems.some(i => i.diaojiao > 0)" class="col-num">吊脚</th>
                  <th v-if="detailItems.some(i => i.doorCount > 0 || i.doorPending)" class="col-num">樘数</th>
                  <th v-if="detailItems.some(i => i.fangCount > 0 || i.fangPending)" class="col-num">面积(㎡)</th>
                  <th class="col-money">单价</th>
                  <th class="col-money">金额</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in detailItems" :key="idx">
                  <td>{{ item.productName || '-' }}</td>
                  <td v-if="detailItems.some(i => i.series)">{{ item.series || '-' }}</td>
                  <td v-if="detailItems.some(i => i.color)">{{ item.color || '-' }}</td>
                  <td v-if="detailItems.some(i => i.width)">{{ formatSize(item, 'width') }}</td>
                  <td v-if="detailItems.some(i => i.height)">{{ formatSize(item, 'height') }}</td>
                  <td v-if="detailItems.some(i => i.wallThickness)">{{ item.wallThickness || '-' }}</td>
                  <td v-if="detailItems.some(i => i.glassType)">{{ item.glassType || '-' }}</td>
                  <td v-if="detailItems.some(i => i.lockPosition)">{{ item.lockPosition || '-' }}</td>
                  <td v-if="detailItems.some(i => i.diaojiao > 0)" class="col-num">{{ item.diaojiao ?? '-' }}</td>
                  <td v-if="detailItems.some(i => i.doorCount > 0 || i.doorPending)" class="col-num">{{ item.doorPending ? '待定' : (item.doorCount > 0 ? item.doorCount : '') }}</td>
                  <td v-if="detailItems.some(i => i.fangCount > 0 || i.fangPending)" class="col-num">{{ item.fangPending ? '待定' : (item.fangCount ?? '-') }}</td>
                  <td class="col-money">{{ formatMoney(item.unitPrice) }}</td>
                  <td class="col-money gold">{{ formatMoney(item.amount ?? (item.quantity != null && item.unitPrice != null ? multiplyMoney(item.quantity, item.unitPrice) : null)) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td :colspan="detailColspan" class="total-label" style="text-align: right">合计：{{ formatMoney(detailOrder.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
            <p v-else class="empty-items">暂无商品明细</p>
          </div>
        </div>

        <!-- Product Type Summary -->
        <div class="detail-section" v-if="productTypeSummaryAll.length">
          <h4 class="section-title"><span class="section-dash"></span>细目统计</h4>
          <div class="type-summary-grid">
            <div class="type-summary-item" v-for="s in productTypeSummaryAll" :key="s.productType"
                 :class="{ 'type-hidden': detailHiddenTypes.has(s.productType) }"
                 @click="toggleDetailType(s.productType)" style="cursor:pointer;" :title="detailHiddenTypes.has(s.productType) ? '点击显示' : '点击隐藏'">
              <span class="type-summary-label">{{ s.productType }}</span>
              <span class="type-summary-val" v-if="s.doorCount > 0">{{ s.doorCount }} {{ parseSummaryUnits(s.productType)[0] }}</span>
              <span class="type-summary-val" v-if="s.fangCount > 0">{{ s.fangCount }} {{ parseSummaryUnits(s.productType)[1] }}</span>
              <span class="type-toggle-icon">{{ detailHiddenTypes.has(s.productType) ? '🙈' : '👁' }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="handlePrint">打印订单</el-button>
      </template>
    </el-drawer>

    <!-- Print Preview Drawer -->
    <el-drawer v-model="previewDrawerVisible" title="打印预览" direction="btt" size="100%" close-on-press-escape :destroy-on-close="true">
      <template #default>
      <div style="display:flex;flex-direction:column;height:100%;">
      <div style="display:flex;align-items:center;gap:12px;padding:0 0 12px;border-bottom:1px solid #e0e0e0;">
        <span style="font-size:13px;">预设：</span>
        <el-select v-model="previewPresetName" size="small" style="width:160px;" @change="onPreviewPresetChange" placeholder="当前配置">
          <el-option label="当前配置" value="" />
          <el-option v-for="p in allPresets" :key="p.name" :label="p.name" :value="p.name" />
        </el-select>
        <span style="font-size:12px;color:#888;flex:1;">💡 选择预设应用到当前打印配置；点击预览区域可调字号/粗体/编辑文字，结果同步打印设置与打印</span>
        <el-button size="small" type="primary" plain @click="previewSavePresetVisible = true">保存为预设</el-button>
      </div>
      <div style="background:#e8e8e8;padding:20px;border-radius:6px;overflow:auto;flex:1;">
        <div ref="previewPageRef" :style="{
          background:'#fff',
          width: (printConfig?.pageWidthMm || (printConfig?.orientation === 'landscape' ? 297 : 210)) + 'mm',
          minHeight: (printConfig?.pageHeightMm || (printConfig?.orientation === 'landscape' ? 210 : 297)) + 'mm',
          margin:'0 auto',
          padding: (printConfig?.marginTop || 0)+'mm '+(printConfig?.marginRight || 0)+'mm '+(printConfig?.marginBottom || 0)+'mm '+(printConfig?.marginLeft || 0)+'mm',
          boxShadow:'0 2px 12px rgba(0,0,0,.15)',
          boxSizing:'border-box',
          position:'relative'
        }">
          <PrintOrder
            ref="printComp"
            :title="printTitle"
            :order="detailOrder || {}"
            :items="detailItems"
            :type="detailOrder?.orderType || 'sale'"
            :hidden-types="detailHiddenTypes"
            :product-type-map="productTypeMap"
            :print-config="printConfig"
            :formula-map="formulaMap"
            :selectable="true"
            :selected-section="previewSelectedSection"
            @section-click="onPreviewSectionClick"
          />
        </div>
      </div>
      </div>
      </template>
      <template #footer>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <div>
            <el-button @click="exportImage">导出图片</el-button>
            <el-button @click="exportPDF">导出PDF</el-button>
          </div>
          <el-button type="primary" @click="printComp?.doPrint()">打印</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 预览区块浮动调节条（视口右下角） -->
    <div v-if="previewDrawerVisible && previewSelectedSection && previewSectionStyles" class="preview-adjust-bar">
      <span class="bar-name">{{ PREVIEW_SECTION_LABELS[previewSelectedSection] }}</span>
      <el-switch v-model="previewSectionStyles[previewSelectedSection].bold" size="small" active-text="粗" inactive-text="" />
      <div class="bar-size">
        <button class="size-btn" :disabled="previewSectionStyles[previewSelectedSection].sizeOffset <= -7" @click="previewSectionStyles[previewSelectedSection].sizeOffset--">−</button>
        <span class="size-display">{{ (printConfig?.fontSize || 11) + (previewSectionStyles[previewSelectedSection].sizeOffset || 0) }}px</span>
        <button class="size-btn" :disabled="previewSectionStyles[previewSelectedSection].sizeOffset >= 7" @click="previewSectionStyles[previewSelectedSection].sizeOffset++">+</button>
      </div>
      <span class="lw-inline">
        <span class="lw-mini">线</span>
        <el-select v-model="printConfig.lineWidths[previewLwKey]" size="small" style="width:64px">
          <el-option label="细" :value="1" />
          <el-option label="中" :value="2" />
          <el-option label="粗" :value="3" />
        </el-select>
      </span>
      <el-radio-group v-if="previewSelectedSection === 'info'" v-model="previewSectionStyles.info.align" size="small">
        <el-radio-button value="left">左</el-radio-button>
        <el-radio-button value="center">中</el-radio-button>
        <el-radio-button value="right">右</el-radio-button>
      </el-radio-group>
      <el-button v-if="previewEditFields.length" size="small" type="warning" plain @click="openPreviewEditText">✏️ 编辑文字</el-button>
      <button class="bar-close" @click="previewSelectedSection = ''">✕</button>
    </div>

    <!-- 编辑文字对话框 -->
    <el-dialog v-model="previewEditTextVisible" title="编辑文字" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item v-for="f in previewEditFields" :key="f.key" :label="f.label">
          <el-input v-model="previewEditTextForm[f.key]" :type="f.long ? 'textarea' : 'text'" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="previewEditTextVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPreviewEditText">确定</el-button>
      </template>
    </el-dialog>

    <!-- 保存预设对话框 -->
    <el-dialog v-model="previewSavePresetVisible" title="保存为预设" width="380px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="预设名称">
          <el-input v-model="previewNewPresetName" placeholder="如：A4简洁版" @keyup.enter="savePreviewPreset" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="previewSavePresetVisible = false">取消</el-button>
        <el-button type="primary" @click="savePreviewPreset" :disabled="!previewNewPresetName.trim()">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== Add / Edit Dialog ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      class="edit-dialog"
      width="98vw"
      destroy-on-close
      :close-on-click-modal="false"
      close-on-press-escape
      :before-close="handleDialogBeforeClose"
      append-to-body
      top="20px"
      @opened="onDialogOpened"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right">
        <!-- Order Info -->
        <div class="dialog-section">
          <h4 class="section-title"><span class="section-dash"></span>订单信息</h4>
          <div class="form-row-3">
            <el-form-item label="订单类型" prop="orderType">
              <el-radio-group v-model="form.orderType">
                <el-radio value="sale">销售单</el-radio>
                <el-radio value="presale" :disabled="editIsSaleOrder">预算单</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="客户名称" prop="customerName">
              <el-select
                v-model="form.customerName"
                filterable
                remote
                reserve-keyword
                allow-create
                default-first-option
                placeholder="输入客户名搜索..."
                :remote-method="searchCustomers"
                :loading="customerSearching"
                @change="onCustomerSelect"
                clearable
                style="width:100%"
              >
                <el-option
                  v-for="c in customerOptions"
                  :key="c.id"
                  :label="c.name"
                  :value="c.name"
                >
                  <div style="display:flex;justify-content:space-between;align-items:center;">
                    <span>{{ c.name }}</span>
                    <span style="color:#999;font-size:11px;">{{ c.phone || '' }}</span>
                  </div>
                </el-option>
                <div style="padding:6px 12px;border-top:1px solid #eee;cursor:pointer;" @click.stop="openAddCustomer">
                  <span style="color:#C5A265;font-size:12px;">+ 新建客户</span>
                </div>
              </el-select>
            </el-form-item>
            <el-form-item label="联系电话" prop="customerPhone">
              <el-input v-model="form.customerPhone" maxlength="20" :placeholder="form.customerPhone ? '' : '选择客户后自动填充'" />
            </el-form-item>
            <el-form-item label="订单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" format="YYYY年MM月DD日" style="width:100%" />
            </el-form-item>
          </div>
          <div class="form-row-3">
            <el-form-item label="客户地址" prop="customerAddress" class="span-2">
              <el-input v-model="form.customerAddress" maxlength="200" :placeholder="form.customerAddress ? '' : '选择客户后自动填充'" />
            </el-form-item>
            <el-form-item label="定金" prop="deposit">
              <el-input-number v-model="form.deposit" :min="0" :precision="2" :step="100" controls-position="right" placeholder="0.00" style="width:100%" />
            </el-form-item>
          </div>
          <div class="form-row-2">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" maxlength="500" placeholder="备注信息（选填）" />
            </el-form-item>
            <el-form-item label="通知" prop="notice">
              <el-input v-model="form.notice" maxlength="500" placeholder="通知信息（选填）" />
            </el-form-item>
          </div>
        </div>

        <!-- Items Table -->
        <div class="dialog-section">
          <h4 class="section-title">
            <span class="section-dash"></span>商品明细
            <div class="item-actions">
              <button v-if="selectedItems.length" type="button" class="btn-batch-move" :disabled="!selectedItems.length" @click="batchMoveUp">▲ 上移</button>
              <button v-if="selectedItems.length" type="button" class="btn-batch-move" :disabled="!selectedItems.length" @click="batchMoveDown">▼ 下移</button>
              <button v-if="selectedPushPullWithoutEdge.length" type="button" class="btn-edge-wrap" @click="batchAddEdgeWrap('单包')">单包({{ selectedPushPullWithoutEdge.length }})</button>
              <button v-if="selectedPushPullWithoutEdge.length" type="button" class="btn-edge-wrap" @click="batchAddEdgeWrap('双包')">双包({{ selectedPushPullWithoutEdge.length }})</button>
              <button v-if="selectedItems.length" type="button" class="btn-batch-item" :disabled="!selectedItems.length" @click="batchDuplicate">批量复制({{ selectedItems.length }})</button>
              <button v-if="selectedItems.length" type="button" class="btn-batch-item btn-batch-del" :disabled="!selectedItems.length" @click="batchDelete">批量删除({{ selectedItems.length }})</button>
              <button type="button" class="btn-add-item" @click="addItem">+ 添加商品</button>
            </div>
          </h4>
          <div class="items-edit-wrap">
            <table class="items-edit-table" @input.capture="onItemsTableInput">
              <thead>
                <tr>
                  <th class="col-check">
                    <input type="checkbox" :checked="selectedItems.length === form.items.length && form.items.length > 0" @change="toggleSelectAll" />
                  </th>
                  <th class="col-commodity">商品</th>
                  <th class="col-color">颜色</th>
                  <th class="col-type">细目</th>
                  <th class="col-size">宽/mm</th>
                  <th class="col-size">高/mm</th>
                  <th class="col-size">墙厚/mm</th>
                  <th class="col-size">玻璃款式</th>
                  <th class="col-lock">锁位</th>
                  <th class="col-num">吊脚</th>
                  <th class="col-num">樘数</th>
                  <th class="col-num">面积(㎡)</th>
                  <th class="col-price">单价</th>
                  <th class="col-price col-unit">单位</th>
                  <th class="col-price">金额</th>
                  <th class="col-remark">备注</th>
                  <th class="col-formula">公式</th>
                  <th class="col-price">成本 <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="opacity:0.4;vertical-align:middle"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg></th>
                  <th class="col-act">操作</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="(item, idx) in form.items" :key="item._uid">
                <tr>
                  <td class="col-check">
                    <input type="checkbox" :checked="selectedItems.includes(idx)" @change="toggleSelectItem(idx)" />
                  </td>
                  <td class="col-commodity">
                    <el-select
                      v-model="item._pickKey"
                      filterable
                      allow-create
                      default-first-option
                      placeholder="搜索商品 / 直接输入名称"
                      @change="(val) => onPickChange(idx, val)"
                      @focus="focusedPickIdx = idx"
                      @blur="onPickBlur(idx)"
                      @keydown.tab.prevent="navigateField($event, idx, 'commodityId')"
                      size="default"
                      style="width:100%"
                      :data-tab-row="idx" data-tab-field="commodityId"
                    >
                      <el-option
                        v-for="o in commodityPickOptions"
                        :key="o.value"
                        :label="o.label"
                        :value="o.value"
                        :class="o.value.startsWith('a') ? 'alias-option' : ''"
                      >
                        <div style="display:flex;justify-content:space-between;align-items:center;">
                          <span>{{ o.label }}</span>
                          <span v-if="!o.value.startsWith('a')" style="color:#C5A265;font-size:12px;margin-left:auto;">¥{{ (commodityOptions.find(c => c.id === o.commodityId))?.defaultPrice ?? '-' }}</span>
                          <span v-else style="color:#999;font-size:12px;margin-left:auto;">别名</span>
                        </div>
                      </el-option>
                      <div v-if="focusedPickIdx === idx && canSaveAsAlias(item)" style="padding:6px 12px;border-top:1px solid #eee;cursor:pointer;" @click.stop="saveAsAlias(idx)">
                        <span style="color:#C5A265;font-size:12px;">💾 把「{{ item.productName }}」存为「{{ commodityName(item.commodityId) }}」的别名</span>
                      </div>
                      <div style="padding:6px 12px;border-top:1px solid #eee;cursor:pointer;" @click.stop="openAddCommodity(idx)">
                        <span style="color:#C5A265;font-size:12px;">+ 新建商品</span>
                      </div>
                      <template #empty>
                        <div style="padding:8px 12px;cursor:pointer;text-align:center;" @click.stop="openAddCommodity(idx)">
                          <span style="color:#C5A265;font-size:13px;">+ 新建商品</span>
                        </div>
                      </template>
                    </el-select>
                  </td>
                  <td class="col-color">
                    <el-select v-model="item.color" filterable allow-create default-first-option size="default" placeholder="颜色" style="width:100%" @keydown.tab.prevent="navigateField($event, idx, 'color')" @keydown.enter.prevent="navigateField($event, idx, 'color')" :data-tab-row="idx" data-tab-field="color">
                      <el-option v-for="c in colorOptions" :key="c" :label="c" :value="c" />
                    </el-select>
                  </td>
                  <td class="col-type">
                    <el-select v-model="item.productType" filterable allow-create default-first-option size="default" placeholder="类型" style="width:100%" @change="(val) => onProductTypeChange(idx, val)" @keydown.tab.prevent="navigateField($event, idx, 'productType')" @keydown.enter.prevent="navigateField($event, idx, 'productType')" :data-tab-row="idx" data-tab-field="productType">
                      <el-option v-for="t in productTypeOptions" :key="t" :label="t" :value="t" />
                    </el-select>
                  </td>
                  <td class="col-size">
                    <input type="number" :value="displaySizeInput(item, 'width')" min="0" :placeholder="isBaoBianFormula(item) ? '宽(m)' : '宽'" @change="onSizeInput(item, 'width', $event.target.value); calcItemQty(idx)" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'width')" @keydown.enter.prevent="navigateField($event, idx, 'width')" :data-tab-row="idx" data-tab-field="width" />
                  </td>
                  <td class="col-size">
                    <input type="number" :value="displaySizeInput(item, 'height')" min="0" :placeholder="isBaoBianFormula(item) ? '高(m)' : '高'" @change="onSizeInput(item, 'height', $event.target.value); calcItemQty(idx)" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'height')" @keydown.enter.prevent="navigateField($event, idx, 'height')" :data-tab-row="idx" data-tab-field="height" />
                  </td>
                  <td class="col-size">
                    <input type="number" v-model.number="item.wallThickness" min="0" placeholder="墙厚" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'wallThickness')" @keydown.enter.prevent="navigateField($event, idx, 'wallThickness')" :data-tab-row="idx" data-tab-field="wallThickness" />
                  </td>
                  <td class="col-size">
                    <input type="text" v-model="item.glassType" placeholder="玻璃款式" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'glassType')" @keydown.enter.prevent="navigateField($event, idx, 'glassType')" :data-tab-row="idx" data-tab-field="glassType" />
                  </td>
                  <td class="col-lock">
                    <el-select v-model="item.lockPosition" filterable allow-create default-first-option size="default" placeholder="锁位" clearable style="width:100%" @keydown.tab.prevent="navigateField($event, idx, 'lockPosition')" @keydown.enter.prevent="navigateField($event, idx, 'lockPosition')" :data-tab-row="idx" data-tab-field="lockPosition">
                      <el-option v-for="lp in lockPositionOptions" :key="lp" :label="lp" :value="lp" />
                    </el-select>
                  </td>
                  <td class="col-num">
                    <input type="number" v-model.number="item.diaojiao" min="0" placeholder="吊脚" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'diaojiao')" @keydown.enter.prevent="navigateField($event, idx, 'diaojiao')" :data-tab-row="idx" data-tab-field="diaojiao" />
                  </td>
                  <td class="col-num">
                    <input v-if="!item.doorPending" type="number" v-model.number="item.doorCount" min="0" placeholder="樘" @change="calcItemQty(idx)" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'doorCount')" @keydown.enter.prevent="navigateField($event, idx, 'doorCount')" :data-tab-row="idx" data-tab-field="doorCount" />
                    <span v-else class="pending-text" @click="togglePending(item, 'doorPending', idx)" title="点击取消待定">待定</span>
                    <button v-if="!item.doorPending" class="pending-in-btn" @click="togglePending(item, 'doorPending', idx)" type="button">待定</button>
                  </td>
                  <td class="col-num">
                    <input v-if="!item.fangPending" type="number" v-model.number="item.fangCount" min="0" placeholder="面积(㎡)" @input="item._fangManual = $event.target.value !== ''" @change="calcItemQty(idx)" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'fangCount')" @keydown.enter.prevent="navigateField($event, idx, 'fangCount')" :data-tab-row="idx" data-tab-field="fangCount" />
                    <span v-else class="pending-text" @click="togglePending(item, 'fangPending', idx)" title="点击取消待定">待定</span>
                    <button v-if="!item.fangPending" class="pending-in-btn" @click="togglePending(item, 'fangPending', idx)" type="button">待定</button>
                  </td>
                  <td class="col-price">
                    <input type="number" v-model.number="item.unitPrice" min="0" step="0.01" class="num-input" @keydown.tab.prevent="navigateField($event, idx, 'unitPrice')" @keydown.enter.prevent="navigateField($event, idx, 'unitPrice')" :data-tab-row="idx" data-tab-field="unitPrice" />
                  </td>
                  <td class="col-price col-unit">
                    <el-select v-model="item.unit" filterable allow-create default-first-option size="default" placeholder="单位" clearable style="width:100%"
                      @keydown.tab.prevent="navigateField($event, idx, 'unit')" @keydown.enter.prevent="navigateField($event, idx, 'unit')" :data-tab-row="idx" data-tab-field="unit">
                      <el-option v-for="u in unitOptions" :key="u" :label="u === 'sqm' ? '㎡' : u" :value="u" />
                    </el-select>
                  </td>
                  <td class="col-price col-amount">
                    <template v-if="editAmountIdx === idx">
                      <input ref="amountInputRefs" type="number" :value="item._manualAmount ?? ''" min="0" step="0.01" class="num-input amount-input"
                        @input="onAmountInput(idx, $event.target.value)" @change="finishAmountEdit(idx, $event.target.value)"
                        @keydown.enter.prevent="finishAmountEdit(idx, $event.target.value)"
                        @keydown.tab.prevent="finishAmountEdit(idx, $event.target.value); navigateField($event, idx, 'amount')"
                        @blur="finishAmountEdit(idx, $event.target.value)" />
                    </template>
                    <template v-else>
                      <span class="amount-display" :class="{ 'amount-edited': item._manualAmount != null }"
                        @click="startAmountEdit(idx)" :title="item._manualAmount != null ? '已手改金额，点击可重新编辑' : '点击编辑金额（砍价）'">
                        {{ displayItemAmount(item) }}
                      </span>
                    </template>
                  </td>
                  <td class="col-remark">
                    <input type="text" v-model="item.remark" placeholder="备注" class="remark-input" @keydown.tab.prevent="navigateField($event, idx, 'remark')" @keydown.enter.prevent="navigateField($event, idx, 'remark')" :data-tab-row="idx" data-tab-field="remark" />
                  </td>
                  <td class="col-formula">
                    <el-select v-model="item.formulaId" filterable size="default" placeholder="自动" clearable style="width:100%"
                      @change="onFormulaChange(idx)" @keydown.tab.prevent="navigateField($event, idx, 'formula')"
                      @keydown.enter.prevent="navigateField($event, idx, 'formula')" :data-tab-row="idx" data-tab-field="formula">
                      <el-option v-for="f in formulaList" :key="f.id" :label="f.name + ' (' + (f.unit || '') + ')'" :value="f.id" />
                    </el-select>
                    <div v-if="item._formulaError" class="formula-err" title="公式缺少参数或表达式有误，请补全宽/高/樘数等">公式缺参数</div>
                  </td>
                  <td class="col-price">
                    <span class="cost-clickable" @click="openCostDialog(idx)" tabindex="0" data-tab-field="cost" :data-tab-row="idx" @keydown.enter.prevent="openCostDialog(idx)" @keydown.tab.prevent="navigateField($event, idx, 'cost')" title="点击编辑成本明细">
                      {{ totalItemCost(item) != null ? formatMoney(totalItemCost(item)) : '-' }}
                    </span>
                  </td>
                  <td class="col-act">
                    <button type="button" class="btn-dup-item" @click="duplicateItem(idx)" title="复制此行">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
                    </button>
                    <button type="button" class="btn-remove-item" @click="removeItem(idx)" title="删除此行">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </td>
                </tr>
                <!-- 包边类型选择提示（推拉门选中后轻量内联显示） -->
                <tr v-if="edgeWrapPromptIdx === idx" class="edge-wrap-prompt">
                  <td :colspan="19" class="edge-wrap-prompt-cell">
                    <span class="edge-wrap-label">选择推拉门，需要：</span>
                    <button type="button" class="edge-wrap-btn" @click="selectEdgeWrap(idx, '单包')">单包</button>
                    <button type="button" class="edge-wrap-btn" @click="selectEdgeWrap(idx, '双包')">双包</button>
                    <button type="button" class="edge-wrap-skip" @click="edgeWrapPromptIdx = null">不需要</button>
                  </td>
                </tr>
                </template>
              </tbody>
              <tfoot v-if="form.items.length">
                <tr>
                  <td colspan="19" class="total-label" style="text-align: right">合计金额：{{ formatMoney(itemsTotal) }}</td>
                </tr>
              </tfoot>
            </table>
            <div v-if="!form.items.length" class="empty-items-edit">
              <p>暂无商品，请点击"添加商品"</p>
            </div>
            <div class="type-summary-grid" v-if="formItemsSummaryAll.length" style="margin-top:12px;">
              <div class="type-summary-item" v-for="s in formItemsSummaryAll" :key="s.productType"
                   :class="{ 'type-hidden': editHiddenTypes.has(s.productType) }"
                   @click="toggleEditType(s.productType)" style="cursor:pointer;" :title="editHiddenTypes.has(s.productType) ? '点击显示' : '点击隐藏'">
                <span class="type-summary-label">{{ s.productType }}</span>
                <span class="type-summary-val" v-if="s.doorCount > 0">{{ s.doorCount }} {{ parseSummaryUnits(s.productType)[0] }}</span>
                <span class="type-summary-val" v-if="s.fangCount > 0">{{ s.fangCount }} {{ parseSummaryUnits(s.productType)[1] }}</span>
                <span class="type-toggle-icon">{{ editHiddenTypes.has(s.productType) ? '🙈' : '👁' }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogBeforeClose(() => dialogVisible = false)">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="saving" @click="handleSave">
          {{ editId ? '保存修改' : '创建订单' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Quick Add Customer Dialog -->
    <el-dialog v-model="showAddCustomer" title="快速新建客户" width="420px" :close-on-click-modal="false" close-on-press-escape destroy-on-close>
      <el-form :model="newCustomerForm" label-width="70px" label-position="right">
        <el-form-item label="客户名称" required>
          <el-input v-model="newCustomerForm.name" maxlength="100" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="newCustomerForm.contact" maxlength="50" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="newCustomerForm.phone" maxlength="20" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="newCustomerForm.address" maxlength="200" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="newCustomerForm.remark" type="textarea" :rows="2" placeholder="备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddCustomer = false">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="savingCustomer" @click="handleQuickAddCustomer">创建并选中</el-button>
      </template>
    </el-dialog>

    <!-- Quick Add Commodity Dialog -->
    <el-dialog v-model="showAddCommodity" title="快速新建商品" width="460px" :close-on-click-modal="false" close-on-press-escape destroy-on-close>
      <el-form :model="newCommodityForm" label-width="80px" label-position="right">
        <el-form-item label="商品编码">
          <el-input v-model="newCommodityForm.code" maxlength="50" placeholder="商品编码（选填）" />
        </el-form-item>
        <el-form-item label="商品名称" required>
          <el-input v-model="newCommodityForm.name" maxlength="100" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品细目">
          <el-select
            v-model="newCommodityForm.productType"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入细目"
            style="width:100%"
          >
            <el-option v-for="t in productTypeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-select
            v-model="newCommodityForm.unit"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入单位"
            style="width:100%"
            @change="onUnitChange"
          >
            <el-option v-for="u in formulaUnitOptions" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认售价">
          <el-input-number v-model="newCommodityForm.defaultPrice" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="材料成本">
          <el-input-number v-model="newCommodityForm.materialCost" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="人工成本">
          <el-input-number v-model="newCommodityForm.laborCost" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="配件成本">
          <el-input-number v-model="newCommodityForm.accessoryCost" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddCommodity = false">取消</el-button>
        <el-button class="btn-gold" type="primary" :loading="savingCommodity" @click="handleQuickAddCommodity">创建并选中</el-button>
      </template>
    </el-dialog>

    <!-- Cost Edit Dialog -->
    <el-dialog v-model="costDialogVisible" title="编辑成本明细" width="420px" :close-on-click-modal="false" destroy-on-close append-to-body @close="handleCostDialogClose">
      <el-form label-width="90px" label-position="right">
        <el-form-item label="材料成本">
          <el-input-number v-model="costForm.materialCost" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="人工成本">
          <el-input-number v-model="costForm.laborCost" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="配件成本">
          <el-input-number v-model="costForm.accessoryCost" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="总成本">
          <el-input-number :model-value="totalEditCost" disabled :precision="2" controls-position="right" style="width:100%;color:#C5A265;font-weight:600" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="costDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCostEdit">确定</el-button>
      </template>
    </el-dialog>

    <ImportExcelDialog
      v-model="importDialogVisible"
      title="导入销售订单"
      :column-map="importConfigs.saleOrders.columns"
      :module="'saleOrders'"
      group-by="orderNo"
      :transform-record="transformImportRecord"
      :on-complete="() => { loadList(); loadSummary() }"
      :sample-data="importConfigs.saleOrders.sample"
      template-filename="销售订单导入模板"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PrintOrder from '@/components/PrintOrder.vue'
import { usePrintConfig } from '@/composables/usePrintConfig'
import { BUILT_IN_PRESETS } from '@/composables/printPresets'
import { savePrintSetting } from '@/api/admin'
import { toPng } from 'html-to-image'
import { jsPDF } from 'jspdf'
const { printConfig } = usePrintConfig()
import { exportToExcel, fmtDate, fmtMoney } from '@/utils/exportExcel'
import { multiplyMoney } from '@/utils/format'
import ImportExcelDialog from '@/components/ImportExcelDialog.vue'
import RefreshButton from '@/components/RefreshButton.vue'
import TableSkeleton from '@/components/TableSkeleton.vue'
import { importConfigs } from '@/utils/importConfigs'
import {
  getSaleOrders,
  getSaleOrderDetail,
  getSaleOrderItemsBatch,
  addSaleOrder,
  updateSaleOrder,
  deleteSaleOrder,
  updateSaleOrderStatus,
  batchDeleteSaleOrders,
  batchUpdateSaleOrderStatus,
  toggleSaleOrderCleared,
  getSaleOrderSummary,
  getCommodities,
  getCommodityById,
  addCommodity,
  updateCommodity,
  getCommodityCategories,
  getCustomers,
  addCustomer,
  getFormulas,
  addFormula,
  getProductTypes,
  restoreEntity,
} from '@/api/admin'
import { useUndoDelete } from '@/composables/useUndoDelete'
import { useUndoEdit } from '@/composables/useUndoEdit'
import { useOrderLock } from '@/composables/useOrderLock'
import { validateDateRange } from '@/composables/useDateRangeValidate'
import { useRecentOrders } from '@/composables/useRecentOrders'
import { useTableSort } from '@/composables/useTableSort'

// ===== Status Config =====
const statusTabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'pending' },
  { label: '进行中', value: 'processing' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
  { label: '已结清', value: 'cleared' },
  { label: '未结清', value: 'uncleared' },
]

const statusMap = {
  pending: '待处理',
  processing: '进行中',
  completed: '已完成',
  cancelled: '已取消',
}

function statusLabel(status) {
  return statusMap[status] || status || '-'
}

// ===== List State =====
const list = ref([])
const { sortedList, toggleSort, sortArrow } = useTableSort(list, { orderDate: 'date', totalAmount: 'number', paidAmount: 'number' }, { storageKey: 'sale_orders_filters' })
const loading = ref(false)
const selectedIds = ref([])
const importDialogVisible = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeStatus = ref('')
const keyword = ref('')
const startDate = ref(null)
const endDate = ref(null)
const summary = ref(null)

let searchTimer = null

// ===== URL 筛选状态持久化 =====
const route = useRoute()
const router = useRouter()
const FILTER_KEY = 'sale_orders_filters'
let _restoring = false

function restoreFilters() {
  const q = route.query
  const hasUrlParams = q.status || q.keyword || q.startDate || q.page
  if (hasUrlParams) {
    _restoring = true
    if (q.status) activeStatus.value = q.status
    if (q.keyword) keyword.value = q.keyword
    if (q.startDate) startDate.value = q.startDate
    if (q.endDate) endDate.value = q.endDate
    if (q.page && Number(q.page) > 1) page.value = Number(q.page)
    if (q.size && Number(q.size) > 0) pageSize.value = Number(q.size)
    _restoring = false
    return
  }
  try {
    const saved = JSON.parse(sessionStorage.getItem(FILTER_KEY) || '{}')
    _restoring = true
    if (saved.status) activeStatus.value = saved.status
    if (saved.keyword) keyword.value = saved.keyword
    if (saved.startDate) startDate.value = saved.startDate
    if (saved.endDate) endDate.value = saved.endDate
    if (saved.page && saved.page > 1) page.value = saved.page
    if (saved.size && saved.size > 0) pageSize.value = saved.size
    _restoring = false
  } catch {}
}

function syncFiltersToUrl() {
  if (_restoring) return
  const query = {}
  const storage = {}
  if (activeStatus.value) { query.status = activeStatus.value; storage.status = activeStatus.value }
  if (keyword.value) { query.keyword = keyword.value; storage.keyword = keyword.value }
  if (startDate.value) { query.startDate = startDate.value; storage.startDate = startDate.value }
  if (endDate.value) { query.endDate = endDate.value; storage.endDate = endDate.value }
  if (page.value > 1) { query.page = String(page.value); storage.page = page.value }
  if (pageSize.value !== 20) { query.size = String(pageSize.value); storage.size = pageSize.value }
  router.replace({ query })
  sessionStorage.setItem(FILTER_KEY, JSON.stringify(storage))
}

const hasFilter = computed(() => {
  return !!(activeStatus.value || keyword.value.trim() || startDate.value || endDate.value)
})

const summaryLabel = computed(() => {
  const tab = statusTabs.find(t => t.value === activeStatus.value)
  return tab && tab.value ? tab.label : '订单总数'
})

// ===== Detail State =====
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref(null)
const detailItems = ref([])

const detailColspan = computed(() => {
  const items = detailItems.value
  if (!items || !items.length) return 4
  let cols = 1 // 商品名称
  if (items.some(i => i.series)) cols++
  if (items.some(i => i.color)) cols++
  if (items.some(i => i.width)) cols++
  if (items.some(i => i.height)) cols++
  if (items.some(i => i.wallThickness)) cols++
  if (items.some(i => i.glassType)) cols++
  if (items.some(i => i.lockPosition)) cols++
  if (items.some(i => i.diaojiao > 0)) cols++
  if (items.some(i => i.doorCount > 0)) cols++
  if (items.some(i => i.fangCount > 0)) cols++
  cols++ // 单价
  cols++ // 金额
  return cols
})

const productTypeSummaryAll = computed(() => {
  const items = detailItems.value
  if (!items || !items.length) return []
  const map = {}
  for (const item of items) {
    const type = item.productType || ''
    if (!map[type]) map[type] = { productType: type, doorCount: 0, fangCount: 0 }
    const dc = item.doorCount > 0 ? Number(item.doorCount) : (item.unit === '套' && item.quantity > 0 ? Number(item.quantity) : 0)
    if (dc > 0) map[type].doorCount += dc
    const fc = item.fangCount > 0 ? Number(item.fangCount) : ((item.unit === '方' || item.unit === 'sqm') && item.quantity > 0 ? Number(item.quantity) : 0)
    if (fc > 0) map[type].fangCount += fc
  }
  const list = Object.values(map).filter(s => s.productType && (s.doorCount > 0 || s.fangCount > 0))
  list.sort((a, b) => (!a.productType) - (!b.productType))
  return list
})

const productTypeSummary = computed(() => {
  return productTypeSummaryAll.value.filter(s => !detailHiddenTypes.value.has(s.productType))
})

async function toggleDetailType(type) {
  const s = new Set(detailHiddenTypes.value)
  if (s.has(type)) s.delete(type)
  else s.add(type)
  detailHiddenTypes.value = s
  // Persist to DB
  if (detailOrder.value?.id) {
    try {
      await updateSaleOrder(detailOrder.value.id, {
        hiddenProductTypes: JSON.stringify([...s]),
      })
    } catch (e) {
      console.error('保存隐藏分类失败:', e)
    }
  }
}

// ===== Dialog State =====
const dialogVisible = ref(false)
const editId = ref(null)
const editIsSaleOrder = ref(false)
const saving = ref(false)
const formRef = ref(null)
const commodityOptions = ref([])
const commoditySearching = ref(false)
// 当前聚焦的商品下拉所在行（用于在下拉底部显示"存为别名"入口）
const focusedPickIdx = ref(null)
// 订单行自定义名称（未存为商品别名）的合成下拉选项，用于回显
const customPickOptions = ref([])
let commoditySearchTimer = null
watch(dialogVisible, (val) => { if (!val) releaseLock() })

// Customer search
const customerOptions = ref([])
const customerSearching = ref(false)
let customerSearchTimer = null

// Quick add dialogs
const showAddCustomer = ref(false)
const savingCustomer = ref(false)
const newCustomerForm = reactive({ name: '', contact: '', phone: '', address: '', remark: '' })
const showAddCommodity = ref(false)
const savingCommodity = ref(false)
const addCommodityIdx = ref(-1)
const newCommodityForm = reactive({ code: '', name: '', productType: '', unit: '', defaultPrice: null, costPrice: null, materialCost: null, laborCost: null, accessoryCost: null, aliases: '' })
const commodityCategories = ref([])
const selectedItems = ref([])
const costDialogVisible = ref(false)
const costEditIndex = ref(-1)
const costForm = reactive({ materialCost: null, laborCost: null, accessoryCost: null })
const totalEditCost = computed(() => {
  const m = Number(costForm.materialCost || 0)
  const l = Number(costForm.laborCost || 0)
  const a = Number(costForm.accessoryCost || 0)
  return (m || l || a) ? m + l + a : 0
})
const selectedPushPullWithoutEdge = computed(() => {
  return selectedItems.value.filter(idx => {
    const item = form.items[idx]
    if (!item || item.productType !== '推拉门') return false
    return !form.items.some(it => it.sourceIdx === idx)
  })
})
const editHiddenTypes = ref(new Set())
const detailHiddenTypes = ref(new Set())
const colorOptions = ref([])
const lockPositionOptions = ref([
  '左', '右', '前锁', '后锁', '左前锁', '右前锁', '左后锁', '右后锁', '中锁',
])
const productTypeOptions = ref([])
const productTypeMap = ref({})
const unitOptions = computed(() => {
  const set = new Set(['套', '方', '米', '吊脚', '樘', '扇', '个'])
  formulaList.value.forEach(f => { if (f.unit) set.add(f.unit) })
  commodityOptions.value.forEach(c => { if (c.unit) set.add(c.unit) })
  return [...set]
})

const defaultForm = () => ({
  orderType: 'sale',
  customerName: '',
  customerPhone: '',
  customerAddress: '',
  orderDate: '',
  deposit: 0,
  remark: '',
  notice: '',
  items: [],
})

const form = reactive(defaultForm())
const formSnapshotJson = ref(null) // 用于判断是否有实际改动
const formDirty = ref(false) // 表单是否有未保存的修改

// 监听表单变化，标记脏状态
watch(form, () => {
  if (dialogVisible.value && formSnapshotJson.value != null) {
    formDirty.value = formSnapshotJson.value !== JSON.stringify({ ...form, hiddenProductTypes: [...editHiddenTypes.value] })
  }
}, { deep: true })

const rules = {
  customerName: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
  ],
  orderDate: [
    { required: true, message: '请选择订单日期', trigger: 'change' },
  ],
}

// ===== Undo Delete =====
const { handleDelete: undoHandleDelete } = useUndoDelete({
  deleteApi: deleteSaleOrder,
  restoreApi: (id) => restoreEntity('sale-order', id),
  onSuccess: () => {
    if (list.value.length === 1 && page.value > 1) {
      page.value--
    }
    loadList()
    loadSummary()
  },
  entityLabel: '销售单',
})

const { snapshotBeforeEdit, afterEditSave } = useUndoEdit({
  updateApi: updateSaleOrder,
  onSuccess: () => { loadList(); loadSummary() },
  entityLabel: '销售单',
})
const { acquireLock, releaseLock } = useOrderLock('sale')
const { addRecent } = useRecentOrders()

// ===== Computed =====
const itemsTotal = computed(() => {
  return form.items.reduce((sum, item) => {
    let total = 0
    // 手改金额（砍价）优先：用户手动编辑过金额，直接用，不再按数量×单价
    if (item._manualAmount != null) {
      total = Number(item._manualAmount)
    } else if (item.quantity != null && item.unitPrice != null) {
      total = multiplyMoney(item.quantity, item.unitPrice)
    }
    if (item.extraFee != null && Number(item.extraFee) > 0) {
      total += Number(item.extraFee)
    }
    return sum + total
  }, 0)
})

const formItemsSummaryAll = computed(() => {
  const items = form.items
  if (!items || !items.length) return []
  const map = {}
  for (const item of items) {
    const type = item.productType || ''
    if (!map[type]) map[type] = { productType: type, doorCount: 0, fangCount: 0 }
    const dc = item.doorCount > 0 ? Number(item.doorCount) : (item.unit === '套' && item.quantity > 0 ? Number(item.quantity) : 0)
    if (dc > 0) map[type].doorCount += dc
    const fc = item.fangCount > 0 ? Number(item.fangCount) : ((item.unit === '方' || item.unit === 'sqm') && item.quantity > 0 ? Number(item.quantity) : 0)
    if (fc > 0) map[type].fangCount += fc
  }
  const list = Object.values(map).filter(s => s.productType && (s.doorCount > 0 || s.fangCount > 0))
  list.sort((a, b) => (!a.productType) - (!b.productType))
  return list
})

const formItemsSummary = computed(() => {
  return formItemsSummaryAll.value.filter(s => !editHiddenTypes.value.has(s.productType))
})

function toggleEditType(type) {
  const s = new Set(editHiddenTypes.value)
  if (s.has(type)) s.delete(type)
  else s.add(type)
  editHiddenTypes.value = s
}

const printTitle = computed(() => {
  if (detailOrder.value?.orderType === 'presale') return '顺居门窗客户预算单'
  return '顺居门业客户销售单'
})

const dialogTitle = computed(() => {
  const type = form.orderType === 'presale' ? '预算单' : '销售单'
  return editId.value ? `编辑${type}` : `新建${type}`
})

function getUnpaid(row) {
  return Math.max(0, (row.totalAmount || 0) - (row.paidAmount || 0))
}

// ===== Helpers =====
function formatDate(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function toLocalDateStr(d) {
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

function formatMoney(v) {
  if (v == null || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ===== Lifecycle =====
const formulaMap = ref({})
const formulaList = ref([]) // 公式数组，用于下拉列表
const formulaUnitOptions = computed(() => Object.keys(formulaMap.value))

// 检查商品是否使用包边计价公式
function isBaoBianFormula(item) {
  // 1. 按 formulaId 查找实际公式（不能按"单位默认公式"判断——
  //    米单位的默认公式是"包边计价"，会把所有米单位商品误判为包边，
  //    导致自定义公式（如 (宽+高*2)/1000）的宽高被按米换算、面积列算错）
  if (item.formulaId) {
    const formula = formulaList.value.find(f => f.id === item.formulaId)
    if (formula?.name?.includes('包边')) return true
  }
  // 2. 兜底：按商品名称判断
  return item.productName?.includes('包边') || false
}

// 格式化宽高：包边计价显示为米，其他显示原值
function formatSize(item, field) {
  const val = item[field]
  if (val == null || val === '') return '-'
  if (isBaoBianFormula(item)) return +(val / 1000).toFixed(2)
  return val
}

// 宽/高输入显示值：包边计价按米显示（mm/1000），其他按 mm
function displaySizeInput(item, field) {
  const v = item[field]
  if (v == null || v === '') return ''
  return isBaoBianFormula(item) ? String(+(v / 1000).toFixed(3)) : String(v)
}
// 宽/高输入提交：包边计价按米输入（mm = 输入×1000），存储仍为 mm
function onSizeInput(item, field, val) {
  if (val === '' || val == null) { item[field] = null; return }
  const n = parseFloat(val)
  if (isNaN(n)) return
  item[field] = isBaoBianFormula(item) ? Math.round(n * 1000) : n
}

onMounted(async () => {
  restoreFilters()
  loadList()
  loadSummary()
  searchCommodities('')
  searchCustomers('')
  loadFormulas()
  loadProductTypes()
})

// 从其他页面跳转过来自动打开详情（支持首次加载 + 路由变化）
watch(() => route.query.detailId, (val) => {
  if (!val) return
  const id = Number(val)
  if (Number.isInteger(id) && id > 0) {
    nextTick(() => openDetail({ id }))
  }
}, { immediate: true })

// 从客户列表跳转过来自动打开新建弹窗并预填客户
watch(() => route.query.create, (val) => {
  if (val !== '1') return
  const prefill = {
    customerName: route.query.customerName || '',
    customerPhone: route.query.customerPhone || '',
    customerAddress: route.query.customerAddress || ''
  }
  nextTick(() => {
    openAdd('sale', prefill)
    // 清理 query 参数避免刷新重复打开
    router.replace({ query: {} })
  })
}, { immediate: true })

async function loadFormulas() {
  try {
    const res = await getFormulas()
    const data = res.data?.data ?? res.data
    const list = Array.isArray(data) ? data : []
    const map = {}
    for (const f of list) {
      // 默认公式 = 该单位 sort 最小者（与商品编辑一致）
      if (f.unit && (!map[f.unit] || (map[f.unit].sort ?? 999) > (f.sort ?? 999))) {
        map[f.unit] = f
      }
    }
    formulaMap.value = map
    formulaList.value = list
  } catch (e) {
    console.error('加载公式列表失败:', e)
  }
}

function resolveFormulas() {
  const map = formulaMap.value
  const allFormulas = Object.values(map)
  if (!allFormulas.length) return
  for (const item of form.items) {
    // 行级 formulaId 优先
    let formula = null
    if (item.formulaId) {
      formula = allFormulas.find(f => f.id === item.formulaId) || null
    }
    // 回退到 unit 匹配
    if (!formula) {
      formula = map[item.unit]
    }
    if (!formula) continue
    const prevExpr = item.formulaExpression
    item.formulaId = formula.id
    item.formulaExpression = formula.formula || ''
    // 公式变了或之前没公式时重新计算
    if (prevExpr !== item.formulaExpression || !prevExpr) {
      calcItemQty(form.items.indexOf(item))
    }
  }
}

async function loadProductTypes() {
  try {
    const res = await getProductTypes()
    const data = res.data?.data ?? res.data
    const list = Array.isArray(data) ? data : []
    productTypeOptions.value = list.map(t => t.name)
    const map = {}
    for (const t of list) {
      map[t.name] = t.summaryDesc || ''
    }
    productTypeMap.value = map
  } catch (e) {
    console.error('加载细目列表失败:', e)
  }
}

// 解析 summaryDesc（如 "樘/方"）返回 [doorCount单位, fangCount单位]
function parseSummaryUnits(productType) {
  const desc = productTypeMap.value[productType] || ''
  if (desc && desc.includes('/')) {
    const parts = desc.split('/')
    return [parts[0]?.trim() || '樘', parts[1]?.trim() || '㎡']
  }
  return ['樘', '㎡']
}

// ===== Watchers =====
watch(activeStatus, () => {
  page.value = 1
  loadList()
  loadSummary()
  syncFiltersToUrl()
})
watch(page, () => { if (!_restoring) { loadList(); syncFiltersToUrl() } })
watch(pageSize, () => {
  if (_restoring) return
  if (page.value === 1) { loadList(); syncFiltersToUrl() }
  else { page.value = 1 }
})
watch(formulaMap, () => {
  if (dialogVisible.value && form.items.length) resolveFormulas()
})

// ===== Data Loading =====
function buildFilterParams() {
  const params = {}
  if (activeStatus.value === 'cleared') {
    params.isCleared = 1
  } else if (activeStatus.value === 'uncleared') {
    params.isCleared = 0
  } else if (activeStatus.value) {
    params.status = activeStatus.value
  }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (startDate.value) params.startDate = startDate.value
  if (endDate.value) params.endDate = endDate.value
  return params
}

async function loadList() {
  loading.value = true
  selectedIds.value = []
  allMatchingCache.value = []
  try {
    const params = { page: page.value, size: pageSize.value, ...buildFilterParams() }

    const res = await getSaleOrders(params)
    const data = res.data?.data ?? res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data?.records || data?.list || data?.content || []
      total.value = data?.total ?? list.value.length
    }
  } catch (e) {
    console.error('加载销售订单失败:', e)
    ElMessage.error('加载订单列表失败，请稍后重试')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 按当前筛选条件分页拉取全部匹配订单（后端 size 上限 100，循环取完）
async function fetchAllMatchingOrders() {
  const records = []
  const base = buildFilterParams()
  base.size = 100
  const expectTotal = total.value || 0
  for (let p = 1; p <= 200; p++) {
    const res = await getSaleOrders({ ...base, page: p })
    const data = res.data?.data ?? res.data
    const arr = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
    records.push(...arr)
    // 用已知总数兜底，防止后端异常时死循环（200 页 = 20000 条上限）
    if (arr.length < 100 || (expectTotal > 0 && records.length >= expectTotal)) break
  }
  return records
}

async function loadSummary() {
  try {
    const params = buildFilterParams()
    const res = await getSaleOrderSummary(params)
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
    syncFiltersToUrl()
  }, 350)
}

// ===== Detail Drawer =====
function handleRowClick(e, row) {
  // Shift 拖拽多选结束后会补发 click，抑制一次避免再次切换勾选
  if (suppressNextRowClick.value) { suppressNextRowClick.value = false; return }
  // 点击行任意位置切换勾选（按钮/链接/输入框等交互元素除外）
  const tag = e.target.tagName
  if (tag === 'BUTTON' || tag === 'A' || tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return
  if (e.target.closest('button') || e.target.closest('a') || e.target.closest('.el-dropdown') || e.target.closest('.el-dialog')) return
  toggleSelectRow(row.id)
}

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  detailItems.value = []
  detailHiddenTypes.value = new Set()
  try {
    const res = await getSaleOrderDetail(row.id)
    const data = res.data?.data ?? res.data
    if (data && data.order) {
      detailOrder.value = data.order
      detailItems.value = data.items || []
    } else if (data && (data.orderNo || data.customerName)) {
      // API returns order directly without wrapping
      detailOrder.value = data
      detailItems.value = data?.items || []
    } else {
      // Order not found or deleted
      ElMessage.warning('订单不存在或已被删除')
      drawerVisible.value = false
      detailLoading.value = false
      return
    }
    // Load hidden product types from DB
    const order = detailOrder.value
    try {
      const hpt = order?.hiddenProductTypes
      const arr = typeof hpt === 'string' ? JSON.parse(hpt) : (Array.isArray(hpt) ? hpt : [])
      detailHiddenTypes.value = new Set(arr)
    } catch {
      detailHiddenTypes.value = new Set()
    }
    addRecent({ id: row.id, orderNo: detailOrder.value?.orderNo || row.orderNo, type: 'sale', customerName: detailOrder.value?.customerName || row.customerName })
  } catch (e) {
    console.error('加载订单详情失败:', e)
    if (!e._handled) ElMessage.error('加载订单详情失败')
    if (row.orderNo || row.customerName) {
      // Came from list — show partial data
      detailOrder.value = row
    } else {
      // Came from URL redirect — close drawer
      drawerVisible.value = false
    }
    detailItems.value = []
    detailHiddenTypes.value = new Set()
  } finally {
    detailLoading.value = false
  }
}

// ===== Status Change =====
function handleDropdownCommand(row, cmd) {
  if (cmd === 'clear' || cmd === 'unclear') {
    handleToggleCleared(row)
  } else {
    handleChangeStatus(row, cmd)
  }
}

async function handleChangeStatus(row, newStatus) {
  if (row.status === newStatus) return
  const label = statusLabel(newStatus)
  try {
    await ElMessageBox.confirm(
      `确定将订单「${row.orderNo}」的状态更改为「${label}」吗？`,
      '更改状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await updateSaleOrderStatus(row.id, newStatus)
    ElMessage.success(`状态已更改为「${label}」`)
    loadList()
    loadSummary()
  } catch (e) {
    console.error('更改状态失败:', e)
    ElMessage.error('更改状态失败，请稍后重试')
  }
}

// ===== Batch Operations =====
const batchOperating = ref(false)
// 全选时拉取的全量匹配订单（跨页批量操作用，筛选/翻页变化时清空）
const allMatchingCache = ref([])

// 全选框勾选状态按"当前分类当前页"计算
const currentPageIds = computed(() => list.value.map(o => o.id))
const isAllSelected = computed(() => currentPageIds.value.length > 0 && currentPageIds.value.every(id => selectedIds.value.includes(id)))
const isIndeterminate = computed(() => {
  const selectedOnPage = currentPageIds.value.filter(id => selectedIds.value.includes(id))
  return selectedOnPage.length > 0 && selectedOnPage.length < currentPageIds.value.length
})

async function toggleSelectAllRows() {
  if (isAllSelected.value) {
    selectedIds.value = []
    return
  }
  if (!list.value.length || batchOperating.value) return
  batchOperating.value = true
  try {
    // 按当前分类/筛选条件全量拉取（跨页），全选当前分类下的全部订单
    const all = await fetchAllMatchingOrders()
    allMatchingCache.value = all
    selectedIds.value = all.map(o => o.id)
    if (selectedIds.value.length) ElMessage.success(`已全选当前分类下全部 ${selectedIds.value.length} 条`)
  } catch (e) {
    ElMessage.error('全选失败，请稍后重试')
  } finally {
    batchOperating.value = false
  }
}

function toggleSelectRow(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}

// ===== Shift + 鼠标拖拽多选 =====
const dragSelect = ref({ active: false, startIdx: -1, lastIdx: -1 })
const suppressNextRowClick = ref(false)

function dragRowIndex(e) {
  const tr = e.target.closest('tr')
  if (!tr || tr.parentElement?.tagName !== 'TBODY') return -1
  return [...tr.parentElement.children].indexOf(tr)
}

function onListMouseDown(e) {
  if (!e.shiftKey || e.button !== 0) return
  const idx = dragRowIndex(e)
  if (idx === -1) return
  if (e.target.closest('button') || e.target.closest('a') || e.target.closest('input') || e.target.closest('.el-dropdown')) return
  e.preventDefault() // 防止拖拽时选中表格文本
  dragSelect.value = { active: true, startIdx: idx, lastIdx: idx }
  applyDragRange(idx, idx)
}

function onListMouseMove(e) {
  const ds = dragSelect.value
  if (!ds.active) return
  const idx = dragRowIndex(e)
  if (idx === -1 || idx === ds.lastIdx) return
  ds.lastIdx = idx
  applyDragRange(ds.startIdx, idx)
}

function onListMouseUp() {
  if (!dragSelect.value.active) return
  dragSelect.value.active = false
  // 拖拽结束后浏览器会补发一次行 click，抑制它避免再次切换勾选
  suppressNextRowClick.value = true
  setTimeout(() => { suppressNextRowClick.value = false }, 120)
}

function applyDragRange(a, b) {
  const [lo, hi] = a <= b ? [a, b] : [b, a]
  selectedIds.value = sortedList.value.slice(lo, hi + 1).map(o => o.id)
}

function clearSelection() {
  selectedIds.value = []
}

async function batchChangeStatus(newStatus) {
  const label = statusLabel(newStatus)
  const allIds = [...selectedIds.value]
  const orderMap = new Map((allMatchingCache.value.length ? allMatchingCache.value : list.value).map(o => [o.id, o]))
  const alreadyTarget = []
  // 内部系统：任意状态可改（不再跳过已完成/已取消终态）
  const validIds = allIds.filter(id => {
    const order = orderMap.get(id)
    if (!order) return false
    if (order.status === newStatus) { alreadyTarget.push(order.orderNo || `#${id}`); return false }
    return true
  })

  if (!validIds.length) {
    let reason = '没有可更改状态的订单。'
    if (alreadyTarget.length) reason += ` ${alreadyTarget.length} 个已处于「${label}」状态。`
    ElMessage.warning(reason)
    return
  }

  batchOperating.value = true
  try {
    try {
      await ElMessageBox.confirm(
        `确定将 ${validIds.length} 个订单的状态更改为「${label}」吗？`,
        '批量更改状态',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
    } catch { return }

    await batchUpdateSaleOrderStatus(validIds, newStatus)
    const msgs = []
    msgs.push(`成功更改 ${validIds.length} 条`)
    if (alreadyTarget.length) msgs.push(`跳过 ${alreadyTarget.length} 条（已是「${label}」）`)
    ElMessage.success(msgs.join('，'))
    selectedIds.value = []
    loadList()
    loadSummary()
  } finally {
    batchOperating.value = false
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return

  // 内部系统：任意状态的订单都可删除，关联收付款一并清除
  const orderMap = new Map((allMatchingCache.value.length ? allMatchingCache.value : list.value).map(o => [o.id, o]))
  const validIds = selectedIds.value.filter(id => orderMap.has(id))

  if (!validIds.length) {
    ElMessage.warning('请选择要删除的订单')
    return
  }

  let confirmMsg = `确认删除选中的 ${validIds.length} 个订单？删除后关联的收付款记录将一并清除，此操作不可撤销。`
  try {
    await ElMessageBox.confirm(confirmMsg, '批量删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  batchOperating.value = true
  try {
    await batchDeleteSaleOrders(validIds)
    ElMessage.success(`成功删除 ${validIds.length} 条`)
    selectedIds.value = []
    loadList()
    loadSummary()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '批量删除失败')
  } finally { batchOperating.value = false }
}

async function handleBatchToggleCleared(command) {
  if (!selectedIds.value.length) return
  const label = command === 'clear' ? '已结清' : '未结清'
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selectedIds.value.length} 个订单标记为「${label}」吗？`,
      '批量更改结清状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  batchOperating.value = true
  try {
    const ids = [...selectedIds.value]
    let success = 0
    const failedMsgs = []
    for (const id of ids) {
      try { await toggleSaleOrderCleared(id); success++ }
      catch (e) { failedMsgs.push(e?.response?.data?.msg || e?.message || '未知错误') }
    }
    if (failedMsgs.length) {
      const unique = [...new Set(failedMsgs)]
      ElMessage.warning(`成功 ${success} 条，失败 ${failedMsgs.length} 条：${unique.join('；')}`)
    } else {
      ElMessage.success(`已将 ${success} 个订单标记为「${label}」`)
    }
    selectedIds.value = []
    loadList()
    loadSummary()
  } finally { batchOperating.value = false }
}

// ===== Toggle Cleared =====
async function handleToggleCleared(row) {
  const label = row.isCleared ? '未结清' : '已结清'
  try {
    await ElMessageBox.confirm(
      `确定将订单「${row.orderNo}」标记为「${label}」吗？`,
      '更改结清状态',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await toggleSaleOrderCleared(row.id)
    ElMessage.success(`已标记为「${label}」`)
    loadList()
    loadSummary()
  } catch (e) {
    console.error('更改结清状态失败:', e)
    ElMessage.error('更改结清状态失败，请稍后重试')
  }
}

// ===== Confirm Sale (预算单 → 销售单) =====
async function handleConfirmSale(row) {
  try {
    await ElMessageBox.confirm(
      `确定将预算单「${row.orderNo}」转为正式销售单吗？转为销售单后不可退回。`,
      '确认转为销售单',
      { confirmButtonText: '确定转换', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await updateSaleOrder(row.id, { orderType: 'sale' })
    ElMessage.success('已转为正式销售单')
    row.orderType = 'sale'
    loadList()
  } catch (e) {
    console.error('转换失败:', e)
    ElMessage.error('转换失败，请稍后重试')
  }
}

// ===== Delete =====
function handleDelete(row) {
  undoHandleDelete(row)
}

// ===== Print =====
const printComp = ref(null)
const previewDrawerVisible = ref(false)

const allPresets = computed(() => {
  const customs = printConfig.value?.customPresets || []
  return [...BUILT_IN_PRESETS, ...customs]
})
const previewPageRef = ref(null)

// ===== 预览区块点击调节（与打印设置共用 printConfig，改动即时同步） =====
const PREVIEW_SECTION_LABELS = {
  header: '标题',
  info: '订单信息',
  tableHead: '表头',
  tableBody: '表体',
  summary: '汇总',
  terms: '条款',
  footer: '页脚',
}
const PREVIEW_EDIT_FIELDS = {
  header: [{ key: 'companyName', label: '公司名称' }],
  info: [
    { key: 'creatorName', label: '制单人' },
    { key: 'creatorPhone', label: '联系电话' },
    { key: 'creatorAddress', label: '公司地址' },
  ],
  terms: [
    { key: 'paymentTermsText', label: '付款条款', long: true },
    { key: 'installationTermsText', label: '安装条款', long: true },
    { key: 'warrantyTermsText', label: '保修条款', long: true },
  ],
  footer: [{ key: 'footerText', label: '页脚附加文字', long: true }],
}
const previewSelectedSection = ref('')
function onPreviewSectionClick(key) {
  previewSelectedSection.value = previewSelectedSection.value === key ? '' : key
}
const previewEditFields = computed(() => PREVIEW_EDIT_FIELDS[previewSelectedSection.value] || [])
// 预览分区 → lineWidths 键（表头/表体共用「表格」线）
const PREVIEW_SECTION_TO_LW = { header: 'header', info: 'info', tableHead: 'table', tableBody: 'table', summary: 'summary', terms: 'terms', footer: 'footer' }
const previewLwKey = computed(() => PREVIEW_SECTION_TO_LW[previewSelectedSection.value] || previewSelectedSection.value)

// ===== 预览/打印预设：选择预设 → 应用到当前配置（调节/打印/打印设置全部一致） =====
const previewPresetName = ref('')
let previewBaseSnapshot = null
function applyPresetToConfig(target, presetCfg) {
  for (const [k, v] of Object.entries(presetCfg)) {
    if (k === 'sectionStyles' || k === 'columnWidths' || k === 'columns') continue
    if (v !== undefined) target[k] = JSON.parse(JSON.stringify(v))
  }
  for (const k of ['sectionStyles', 'columnWidths']) {
    if (presetCfg[k]) {
      if (!target[k]) target[k] = {}
      Object.assign(target[k], JSON.parse(JSON.stringify(presetCfg[k])))
    }
  }
  if (presetCfg.columns) target.columns = JSON.parse(JSON.stringify(presetCfg.columns))
}
function onPreviewPresetChange(name) {
  if (!printConfig.value) return
  if (name) {
    if (!previewBaseSnapshot) previewBaseSnapshot = JSON.parse(JSON.stringify(printConfig.value))
    const preset = allPresets.value.find(p => p.name === name)
    if (preset) applyPresetToConfig(printConfig.value, preset.config)
  } else if (previewBaseSnapshot) {
    Object.keys(printConfig.value).forEach(k => delete printConfig.value[k])
    Object.assign(printConfig.value, previewBaseSnapshot)
    previewBaseSnapshot = null
  }
  ElMessage.success(name ? `已应用预设「${name}」` : '已恢复当前配置')
}
// 兜底初始化 sectionStyles/lineWidths（旧配置可能缺失），保证浮动条可调
const previewSectionStyles = computed(() => {
  const cfg = printConfig.value
  if (!cfg) return null
  if (!cfg.sectionStyles) cfg.sectionStyles = {}
  for (const k of ['header', 'info', 'tableHead', 'tableBody', 'summary', 'terms', 'footer']) {
    if (!cfg.sectionStyles[k]) cfg.sectionStyles[k] = { bold: false, sizeOffset: 0 }
  }
  if (!cfg.lineWidths) cfg.lineWidths = {}
  for (const k of ['header', 'info', 'table', 'summary', 'terms', 'footer']) {
    if (cfg.lineWidths[k] == null) cfg.lineWidths[k] = 2
  }
  return cfg.sectionStyles
})
const previewEditTextVisible = ref(false)
const previewEditTextForm = ref({})
function openPreviewEditText() {
  previewEditTextForm.value = {}
  for (const f of previewEditFields.value) previewEditTextForm.value[f.key] = printConfig.value?.[f.key] || ''
  previewEditTextVisible.value = true
}
function confirmPreviewEditText() {
  for (const f of previewEditFields.value) {
    if (printConfig.value) printConfig.value[f.key] = (previewEditTextForm.value[f.key] || '').trim()
  }
  previewEditTextVisible.value = false
  ElMessage.success('文字已更新')
}
const previewSavePresetVisible = ref(false)
const previewNewPresetName = ref('')
function savePreviewPreset() {
  const name = previewNewPresetName.value.trim()
  if (!name) return
  if ((printConfig.value?.customPresets || []).some(p => p.name === name)) {
    ElMessage.warning('预设名称已存在')
    return
  }
  if (!printConfig.value) return
  const preset = { name, builtIn: false, config: JSON.parse(JSON.stringify(printConfig.value)) }
  if (!printConfig.value.customPresets) printConfig.value.customPresets = []
  printConfig.value.customPresets.push(preset)
  savePrintSetting('order', printConfig.value).then(() => {
    ElMessage.success(`预设「${name}」已保存`)
  }).catch(() => {
    ElMessage.warning('预设已保存到本地，但同步打印设置失败')
  })
  previewSavePresetVisible.value = false
  previewNewPresetName.value = ''
}

async function exportImage() {
  if (!previewPageRef.value) return
  const { saveDataUrl } = await import('@/utils/download')
  const dataUrl = await toPng(previewPageRef.value, {
    pixelRatio: 4,
    cacheBust: true,
  })
  await saveDataUrl(dataUrl, `${printTitle.value || '订单'}.png`, { successMsg: '图片已保存' })
}

async function exportPDF() {
  if (!previewPageRef.value) return
  // pixelRatio 5 gives ~480 DPI on A4 — solid borders, no dashed lines
  const dataUrl = await toPng(previewPageRef.value, {
    pixelRatio: 5,
    cacheBust: true,
  })
  const orient = printConfig.value?.orientation || 'portrait'
  const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: orient })
  const pageW = orient === 'landscape' ? 297 : 210
  const pageH = orient === 'landscape' ? 210 : 297
  pdf.addImage(dataUrl, 'PNG', 0, 0, pageW, pageH, undefined, 'FAST')
  const blob = new Blob([pdf.output('arraybuffer')], { type: 'application/pdf' })
  const { saveFile } = await import('@/utils/download')
  await saveFile(blob, `${printTitle.value || '订单'}.pdf`, { successMsg: 'PDF已保存' })
}
function handlePrint() {
  if (!detailOrder.value) {
    ElMessage.warning('请先点击订单列表中的"查看详情"打开一个订单，然后再打印')
    return
  }
  previewDrawerVisible.value = true
}

async function handlePreview(row) {
  try {
    const res = await getSaleOrderDetail(row.id)
    const data = res.data?.data ?? res.data
    if (data && data.order) {
      detailOrder.value = data.order
      detailItems.value = data.items || []
    } else {
      detailOrder.value = data || row
      detailItems.value = data?.items || []
    }
    // Load hidden product types from DB
    const order = detailOrder.value
    try {
      const hpt = order?.hiddenProductTypes
      const arr = typeof hpt === 'string' ? JSON.parse(hpt) : (Array.isArray(hpt) ? hpt : [])
      detailHiddenTypes.value = new Set(arr)
    } catch {
      detailHiddenTypes.value = new Set()
    }
    await nextTick()
    previewDrawerVisible.value = true
  } catch (e) {
    console.error('加载订单详情失败:', e)
    ElMessage.error('加载订单详情失败')
  }
}

// ===== Export =====
const EXPORT_COLUMNS = [
  { key: 'orderNo', label: '订单编号', width: 18 },
  { key: 'orderType', label: '订单类型', width: 10, format: (v) => v === 'presale' ? '预算单' : '销售单' },
  { key: 'customerName', label: '客户名称', width: 14 },
  { key: 'customerPhone', label: '联系电话', width: 14 },
  { key: 'customerAddress', label: '客户地址', width: 20 },
  { key: 'orderDate', label: '订单日期', width: 12, format: fmtDate },
  { key: 'deposit', label: '定金', width: 12, format: fmtMoney },
  { key: 'status', label: '状态', width: 8, format: (v) => statusLabel(v) },
  { key: 'remark', label: '备注', width: 20 },
  { key: 'notice', label: '发货通知', width: 16 },
  { key: 'productName', label: '商品名称', width: 16 },
  { key: 'series', label: '系列', width: 10 },
  { key: 'color', label: '颜色', width: 10 },
  { key: 'productType', label: '细目', width: 10 },
  { key: 'width', label: '宽(mm)', width: 10 },
  { key: 'height', label: '高(mm)', width: 10 },
  { key: 'wallThickness', label: '墙厚(mm)', width: 10 },
  { key: 'glassType', label: '玻璃款式', width: 10 },
  { key: 'lockPosition', label: '锁位', width: 10 },
  { key: 'diaojiao', label: '吊脚', width: 10 },
  { key: 'doorCount', label: '樘数', width: 10 },
  { key: 'fangCount', label: '面积(㎡)', width: 10 },
  { key: 'unit', label: '单位', width: 8 },
  { key: 'quantity', label: '数量', width: 10 },
  { key: 'unitPrice', label: '单价', width: 12, format: fmtMoney },
  { key: 'extraFee', label: '附加费', width: 12, format: fmtMoney },
  { key: 'materialCost', label: '材料成本', width: 12, format: fmtMoney },
  { key: 'laborCost', label: '人工成本', width: 12, format: fmtMoney },
  { key: 'accessoryCost', label: '配件成本', width: 12, format: fmtMoney },
  { key: 'itemRemark', label: '明细备注', width: 16 },
]
const ITEM_KEYS = ['productName','series','color','productType','unit','width','height','wallThickness','glassType','lockPosition','diaojiao','doorCount','fangCount','quantity','unitPrice','extraFee','materialCost','laborCost','accessoryCost','itemRemark']
const EXPORT_COLUMNS_SIMPLE = EXPORT_COLUMNS.filter(c => !ITEM_KEYS.includes(c.key))

function flattenOrders(orders) {
  const flatRows = []
  for (const order of orders) {
    const items = order.items && order.items.length ? order.items : [{}]
    for (const item of items) {
      const row = {}
      for (const col of EXPORT_COLUMNS) {
        if (ITEM_KEYS.includes(col.key)) {
          if (col.key === 'amount') {
            row[col.key] = item.amount ?? multiplyMoney(Number(item.quantity) || 0, Number(item.unitPrice) || 0)
          } else if (col.key === 'itemRemark') {
            row[col.key] = item.remark
          } else {
            row[col.key] = item[col.key]
          }
        } else {
          row[col.key] = order[col.key]
        }
      }
      flatRows.push(row)
    }
  }
  return flatRows
}

async function handleExportCommand(cmd) {
  const [scope, mode] = cmd.split('-')
  if (scope === 'page') {
    await handleExport(mode)
  } else {
    await handleExportAll(mode)
  }
}

async function fetchItemsChunked(orders, batchFn, chunkSize = 100) {
  const ids = orders.map(o => o.id).filter(Boolean)
  if (!ids.length) return
  try {
    const itemsMap = {}
    for (let i = 0; i < ids.length; i += chunkSize) {
      const chunk = ids.slice(i, i + chunkSize)
      const res = await batchFn(chunk)
      const data = res.data?.data ?? res.data ?? {}
      Object.assign(itemsMap, data)
    }
    for (const order of orders) {
      if (itemsMap[order.id]) order.items = itemsMap[order.id]
    }
  } catch (e) {
    console.error('获取商品明细失败:', e)
  }
}

async function handleExport(mode = 'detail') {
  const orders = list.value
  if (mode === 'detail') await fetchItemsChunked(orders, getSaleOrderItemsBatch)
  await doExport(orders, mode)
}

async function doExport(orders, mode = 'detail') {
  const flatRows = mode === 'detail' ? flattenOrders(orders) : orders
  const cols = mode === 'detail' ? EXPORT_COLUMNS : EXPORT_COLUMNS_SIMPLE
  await exportToExcel(flatRows, cols, `销售订单_${toLocalDateStr(new Date())}`, {
    sheetName: '销售订单',
    mergeKeys: mode === 'detail' ? EXPORT_COLUMNS_SIMPLE.map(c => c.key) : [],
  })
  ElMessage.success('导出成功')
}

async function handleExportAll(mode = 'detail') {
  const loadingMsg = ElMessage({ message: '正在导出全部订单...', type: 'info', duration: 0 })
  try {
    const baseParams = {}
    if (activeStatus.value === 'cleared') {
      baseParams.isCleared = 1
    } else if (activeStatus.value === 'uncleared') {
      baseParams.isCleared = 0
    } else if (activeStatus.value) {
      baseParams.status = activeStatus.value
    }
    if (keyword.value.trim()) baseParams.keyword = keyword.value.trim()
    if (startDate.value) baseParams.startDate = startDate.value
    if (endDate.value) baseParams.endDate = endDate.value

    // Fetch all orders page by page
    const allOrders = []
    let page = 1
    const pageSize = 100
    while (true) {
      const res = await getSaleOrders({ ...baseParams, page, size: pageSize })
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || [])
      const total = data?.total ?? 0
      if (!records.length) break
      allOrders.push(...records)
      if (allOrders.length >= total) break
      page++
    }

    if (mode === 'detail') await fetchItemsChunked(allOrders, getSaleOrderItemsBatch)
    await doExport(allOrders, mode)
    ElMessage.success(`导出成功，共 ${allOrders.length} 条订单`)
  } catch (e) {
    console.error('导出全部失败:', e)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    loadingMsg.close()
  }
}

function transformImportRecord(record) {
  return {
    ...record,
    orderDate: record.orderDate || toLocalDateStr(new Date()),
    orderType: record.orderType || 'sale',
    items: (record.items || []).map(item => {
      const { itemRemark, ...rest } = item
      return {
        ...rest,
        remark: itemRemark || '',
      }
    }),
  }
}

// ===== Add / Edit Dialog =====
function openAdd(type = 'sale', prefill = null) {
  editId.value = null
  resetForm()
  selectedItems.value = []
  edgeWrapPromptIdx.value = null
  editHiddenTypes.value = new Set()
  form.orderType = type
  if (prefill) {
    form.customerName = prefill.customerName || ''
    form.customerPhone = prefill.customerPhone || ''
    form.customerAddress = prefill.customerAddress || ''
  }
  formSnapshotJson.value = JSON.stringify({ ...form, hiddenProductTypes: [...editHiddenTypes.value] })
  formDirty.value = false
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
  loadFormulas()
  loadProductTypes()
}

async function openEdit(row) {
  if (!await acquireLock(row.id)) return
  addRecent({ id: row.id, orderNo: row.orderNo, type: 'sale', customerName: row.customerName })
  snapshotBeforeEdit(row)
  editId.value = row.id
  editIsSaleOrder.value = row.orderType === 'sale'
  resetForm()
  selectedItems.value = []
  edgeWrapPromptIdx.value = null
  editHiddenTypes.value = new Set()
  form.orderType = row.orderType || 'sale'
  form.customerName = row.customerName || ''
  form.customerPhone = row.customerPhone || ''
  form.customerAddress = row.customerAddress || ''
  form.orderDate = row.orderDate || ''
  form.deposit = row.deposit || 0
  form.remark = row.remark || ''
  form.notice = row.notice || ''
  await Promise.all([loadFormulas(), loadProductTypes(), loadDetailForEdit(row.id)])
  formSnapshotJson.value = JSON.stringify({ ...form, hiddenProductTypes: [...editHiddenTypes.value] })
  formDirty.value = false
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function loadDetailForEdit(id) {
  try {
    const res = await getSaleOrderDetail(id)
    const data = res.data?.data ?? res.data
    const order = data?.order || data
    if (order) {
      form.orderType = order.orderType || form.orderType
      form.customerName = order.customerName || form.customerName
      form.customerPhone = order.customerPhone || form.customerPhone
      form.customerAddress = order.customerAddress || form.customerAddress
      form.orderDate = order.orderDate || form.orderDate
      form.deposit = order.deposit ?? form.deposit
      form.remark = order.remark || form.remark
      form.notice = order.notice || form.notice
      // Load hidden product types from DB
      try {
        const hpt = order.hiddenProductTypes
        const arr = typeof hpt === 'string' ? JSON.parse(hpt) : (Array.isArray(hpt) ? hpt : [])
        editHiddenTypes.value = new Set(arr)
      } catch {
        editHiddenTypes.value = new Set()
      }
    }
    if (data?.items && data.items.length) {
      form.items = data.items.map((it) => createItemRow({
        commodityId: it.commodityId || null,
        productName: it.productName || '',
        series: it.series || '',
        color: it.color || '',
        productType: it.productType || '',
        width: it.width || null,
        height: it.height || null,
        wallThickness: it.wallThickness || null,
        glassType: it.glassType || '',
        lockPosition: it.lockPosition || '',
        doorCount: it.doorCount || null,
        fangCount: it.fangCount || null,
        diaojiao: it.diaojiao || null,
        quantity: it.quantity ?? 1,
        unitPrice: it.unitPrice || null,
        cost: it.cost || null,
        materialCost: it.materialCost || null,
        laborCost: it.laborCost || null,
        accessoryCost: it.accessoryCost || null,
        extraFee: it.extraFee ?? null,
        amount: it.amount != null ? Number(it.amount) : null,
        manualArea: it.manualArea ?? false,
        fangPending: it.fangPending ?? false,
        doorPending: it.doorPending ?? false,
        formulaId: it.formulaId ?? null,
        formulaSnapshot: it.formulaSnapshot || '',
        unit: it.unit || '',
        remark: it.remark || '',
      }))
      await reconcileItemPicks()
      resolveFormulas()
      rebuildSourceIdx()
    }
  } catch (e) {
    console.error('加载订单详情失败:', e)
  }
}

// 编辑已存订单时：预加载订单内商品，使下拉能回显真名 / 别名 / 自定义名称
async function reconcileItemPicks() {
  const ids = [...new Set(form.items.map((it) => it.commodityId).filter(Boolean))]
  if (ids.length) {
    await Promise.allSettled(ids.map(async (id) => {
      try {
        const res = await getCommodityById(id)
        const c = res.data?.data ?? res.data
        if (c && !commodityOptions.value.some((x) => x.id === c.id)) {
          commodityOptions.value.push(c)
        }
      } catch { /* ignore */ }
    }))
  }
  const current = new Set(commodityPickOptions.value.map((o) => o.value))
  for (const item of form.items) {
    if (!item.commodityId) {
      // 纯自定义名称行（未选商品只填了名字）：生成裸选项回显
      if (item.productName) {
        const key = item.productName
        if (!current.has(key)) {
          customPickOptions.value.push({ value: key, label: item.productName, commodityId: null, fromOrder: true })
          current.add(key)
        }
        item._pickKey = key
      }
      continue
    }
    const c = commodityOptions.value.find((x) => x.id === item.commodityId)
    if (!c || !item.productName) continue
    if (item.productName === (c.name || '')) {
      item._pickKey = 'c' + c.id
      continue
    }
    const key = 'a' + c.id + '|' + item.productName
    if (!current.has(key)) {
      customPickOptions.value.push({ value: key, label: item.productName, commodityId: c.id, fromOrder: true })
      current.add(key)
    }
    item._pickKey = key
  }
}

function resetForm() {
  form.orderType = 'sale'
  form.customerName = ''
  form.customerPhone = ''
  form.customerAddress = ''
  form.orderDate = toLocalDateStr(new Date())
  form.deposit = 0
  form.remark = ''
  form.notice = ''
  form.items = []
  customPickOptions.value = []
  focusedPickIdx.value = null
  // 商品下拉未确认的输入缓存随表单重置清空，避免跨对话框残留错位
  Object.keys(pendingPickText).forEach(k => delete pendingPickText[k])
}

let _itemUid = 0
function createItemRow(data = {}) {
  return reactive({
    _uid: ++_itemUid,
    _pickKey: data.commodityId ? 'c' + data.commodityId : '',
    _fangManual: !!data.manualArea,
    manualArea: !!data.manualArea,
    fangPending: !!data.fangPending,
    doorPending: !!data.doorPending,
    _lastCommodityId: data.commodityId || null,
    commodityId: data.commodityId || null,
    productName: data.productName || '',
    series: data.series || '',
    color: data.color || '',
    productType: data.productType || '',
    unit: data.unit || '',
    width: data.width || null,
    height: data.height || null,
    wallThickness: data.wallThickness || null,
    glassType: data.glassType || '',
    lockPosition: data.lockPosition || '',
    doorCount: data.doorCount || null,
    fangCount: data.fangCount || null,
    diaojiao: data.diaojiao || null,
    quantity: data.quantity ?? 1,
    unitPrice: data.unitPrice || null,
    cost: data.cost || null,
    materialCost: data.materialCost || null,
    laborCost: data.laborCost || null,
    accessoryCost: data.accessoryCost || null,
    remark: data.remark || '',
    extraFee: data.extraFee ?? null,
    _manualAmount: data.amount != null ? Number(data.amount) : null,
    formulaId: data.formulaId || null,
    formulaSnapshot: data.formulaSnapshot || '',
    formulaParams: data.formulaParams ? { ...data.formulaParams } : {},
    formulaExpression: data.formulaExpression || '',
    sourceIdx: data.sourceIdx ?? null, // 来源行索引（包边行关联推拉门行）
  })
}

// Tab/Enter 键字段导航顺序
const TAB_FIELDS = ['commodityId', 'color', 'productType', 'width', 'height', 'wallThickness', 'glassType', 'lockPosition', 'diaojiao', 'doorCount', 'fangCount', 'unitPrice', 'unit', 'remark', 'formula', 'cost']

// el-select filterable allow-create 字段：Tab 时需手动将输入文字提交到 model
// 因为 el-select 只在 Enter 时创建新选项，Tab/blur 会还原输入文字
const SELECT_INPUT_FIELDS = ['productType', 'lockPosition', 'color']
const PRESET_COLORS = [
  '黑色', '白色', '灰色', '深灰色', '浅灰色', '银灰色',
  '咖啡色', '深咖啡', '浅咖啡', '香槟色', '红木色', '红樱桃',
  '沙比利', '柚木色', '胡桃木', '黑胡桃', '原木色', '水曲柳',
  '花梨木', '白橡木', '红橡木', '枫木色', '榆木色', '檀木色',
  '松木色', '樱桃木', '黄花梨', '紫檀色', '金橡木', '黄桃木',
  '开放白', '象牙白', '乳白色', '米白色',
  '不锈钢色', '拉丝银', '拉丝金', '玫瑰金',
]

function navigateField(event, idx, fieldName) {
  if (event.isComposing) return
  event.preventDefault()
  const target = event.target
  if (target && target.tagName === 'INPUT' && target.closest('.el-select') && SELECT_INPUT_FIELDS.includes(fieldName)) {
    form.items[idx][fieldName] = target.value
  }
  if (target && typeof target.blur === 'function') {
    target.blur()
  }
  // nextTick 等 Vue 处理完 blur 带来的 v-model 变更后再计算和跳转
  nextTick(() => {
    // 只在公式相关字段变化时才重算数量，避免覆盖用户手动输入
    const CALC_FIELDS = ['width', 'height', 'doorCount']
    if (CALC_FIELDS.includes(fieldName)) {
      calcItemQty(idx)
    }
    const fieldIdx = TAB_FIELDS.indexOf(fieldName)
    if (fieldIdx === -1) return
    if (fieldIdx < TAB_FIELDS.length - 1) {
      focusField(idx, TAB_FIELDS[fieldIdx + 1])
    } else if (idx < form.items.length - 1) {
      focusField(idx + 1, TAB_FIELDS[0])
    }
  })
}

function focusField(rowIdx, fieldName) {
  nextTick(() => {
    const el = document.querySelector(`[data-tab-row="${rowIdx}"][data-tab-field="${fieldName}"]`)
    if (!el) return
    const input = el.querySelector('.el-input__inner') || el.querySelector('input,textarea') || el
    input.focus()
  })
}

function addItem() {
  form.items.push(createItemRow())
}

function removeItem(idx) {
  delete pendingPickText[idx] // 行被删后索引会偏移，未确认输入一并作废
  const relatedIdx = form.items.findIndex((it, i) => i !== idx && it.sourceIdx === idx)
  // 始终先删高索引，避免 splice 后索引偏移
  const first = Math.max(idx, relatedIdx)
  const second = Math.min(idx, relatedIdx)
  if (first >= 0) form.items.splice(first, 1)
  if (second >= 0) form.items.splice(second, 1)
  // 重建 sourceIdx
  form.items.forEach(it => {
    if (it.sourceIdx !== null) {
      if (it.sourceIdx === idx || it.sourceIdx === relatedIdx) it.sourceIdx = null
      else {
        let shift = 0
        if (idx < it.sourceIdx) shift++
        if (relatedIdx !== -1 && relatedIdx < it.sourceIdx) shift++
        it.sourceIdx -= shift
      }
    }
  })
  // 调整包边提示索引
  if (edgeWrapPromptIdx.value !== null) {
    if (edgeWrapPromptIdx.value === idx || edgeWrapPromptIdx.value === relatedIdx) edgeWrapPromptIdx.value = null
    else {
      let shift = 0
      if (idx < edgeWrapPromptIdx.value) shift++
      if (relatedIdx !== -1 && relatedIdx < edgeWrapPromptIdx.value) shift++
      edgeWrapPromptIdx.value -= shift
    }
  }
}

const edgeWrapPromptIdx = ref(null) // 当前显示包边选择提示的行索引

function onProductTypeChange(idx, val) {
  if (val !== '推拉门') {
    if (edgeWrapPromptIdx.value === idx) edgeWrapPromptIdx.value = null
    return
  }
  const alreadyHasEdge = form.items.some((it, i) => i !== idx && it.sourceIdx === idx)
  if (alreadyHasEdge) {
    if (edgeWrapPromptIdx.value === idx) edgeWrapPromptIdx.value = null
    return
  }
  // 内联显示包边选择，不弹阻塞框
  edgeWrapPromptIdx.value = idx
}

async function selectEdgeWrap(idx, type) {
  edgeWrapPromptIdx.value = null
  await addEdgeWrapRow(idx, type)
}

async function addEdgeWrapRow(srcIdx, type) {
  const srcItem = form.items[srcIdx]
  // 搜索包边商品：优先按细目（productType）匹配，再按名称匹配
  let commodity = commodityOptions.value.find(c => c.name?.includes('包边') && c.productType === type)
  if (!commodity) {
    commodity = commodityOptions.value.find(c => c.name === `包边(${type})`)
  }
  if (!commodity) {
    try {
      const res = await getCommodities({ keyword: '包边', size: 50 })
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
      commodity = records.find(c => c.name?.includes('包边') && c.productType === type)
        || records.find(c => c.name === `包边(${type})`)
        || records.find(c => c.name?.includes('包边')) || null
    } catch { /* ignore */ }
  }
  const h = srcItem.height != null && srcItem.height !== '' ? Number(srcItem.height) : 0
  const newRow = createItemRow({
    sourceIdx: srcIdx,
    width: srcItem.width || null,
    height: h > 0 ? h * 2 : null,
    color: srcItem.color || '',
    doorCount: srcItem.doorCount || 1,
  })
  if (commodity) {
    // 确保 commodityOptions 包含该商品，否则 el-select 无法显示 label
    if (!commodityOptions.value.find(c => c.id === commodity.id)) {
      commodityOptions.value.push(commodity)
    }
    newRow.commodityId = commodity.id
    newRow._pickKey = 'c' + commodity.id
    newRow.productName = commodity.name || ''
    newRow.unit = commodity.unit || ''
    newRow.productType = commodity.productType || type || ''
    newRow.unitPrice = commodity.defaultPrice || null
    newRow.formulaId = commodity.formulaId || null
  }
  // 插入到推拉门行的下一行
  const insertIdx = srcIdx + 1
  form.items.splice(insertIdx, 0, newRow)
  // 修正后续 sourceIdx（因为插入了一行，后面的 sourceIdx 需要 +1）
  form.items.forEach((it, i) => {
    if (i !== insertIdx && it.sourceIdx !== null && it.sourceIdx > srcIdx) {
      it.sourceIdx++
    }
  })
  // 触发公式计算和面积计算
  calcItemQty(insertIdx)
}

async function batchAddEdgeWrap(type) {
  // 从高到低插入，避免索引错乱
  const targets = [...selectedPushPullWithoutEdge.value].sort((a, b) => b - a)
  for (const idx of targets) {
    await addEdgeWrapRow(idx, type)
  }
  selectedItems.value = []
}

function updateFilterOptions() {
  const colorSet = new Set(PRESET_COLORS)
  for (const c of commodityOptions.value) {
    if (c.color) colorSet.add(c.color)
  }
  colorOptions.value = [...colorSet]
}

// ===== Customer Search =====
let _customerSearchSeq = 0
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
      if (seq === _customerSearchSeq) customerSearching.value = false
    }
  }, 300)
}

function onCustomerSelect(val) {
  const customer = customerOptions.value.find((c) => c.name === val)
  if (!customer) return
  form.customerPhone = customer.phone || ''
  form.customerAddress = customer.address || ''
}

function openAddCustomer() {
  newCustomerForm.name = form.customerName || ''
  newCustomerForm.contact = ''
  newCustomerForm.phone = ''
  newCustomerForm.address = ''
  newCustomerForm.remark = ''
  showAddCustomer.value = true
}

async function handleQuickAddCustomer() {
  if (!newCustomerForm.name?.trim()) {
    ElMessage.warning('请输入客户名称')
    return
  }
  savingCustomer.value = true
  try {
    const res = await addCustomer({
      name: newCustomerForm.name.trim(),
      contact: newCustomerForm.contact?.trim() || '',
      phone: newCustomerForm.phone?.trim() || '',
      address: newCustomerForm.address?.trim() || '',
      remark: newCustomerForm.remark?.trim() || '',
    })
    const created = res.data?.data ?? res.data
    ElMessage.success('客户创建成功')
    showAddCustomer.value = false
    // Auto-fill form
    form.customerName = created?.name || newCustomerForm.name.trim()
    form.customerPhone = created?.phone || newCustomerForm.phone?.trim() || ''
    form.customerAddress = created?.address || newCustomerForm.address?.trim() || ''
    // Refresh customer list
    searchCustomers(form.customerName)
  } catch (e) {
    console.error('创建客户失败:', e)
    ElMessage.error('创建客户失败，请稍后重试')
  } finally {
    savingCustomer.value = false
  }
}

// ===== Commodity Selector =====
let _commoditySearchSeq = 0
function searchCommodities(query) {
  clearTimeout(commoditySearchTimer)
  if (!query && commodityOptions.value.length) return

  commoditySearchTimer = setTimeout(async () => {
    const seq = ++_commoditySearchSeq
    commoditySearching.value = true
    try {
      const params = { size: 500 }
      if (query) params.keyword = query.trim()
      const res = await getCommodities(params)
      if (seq !== _commoditySearchSeq) return
      const data = res.data?.data ?? res.data
      const records = Array.isArray(data) ? data : (data?.records || data?.list || data?.content || [])
      commodityOptions.value = records
      updateFilterOptions()
    } catch (e) {
      console.error('搜索商品失败:', e)
      if (seq === _commoditySearchSeq) commodityOptions.value = []
    } finally {
      commoditySearching.value = false
    }
  }, 300)
}

async function onCommoditySelect(idx, commodityId) {
  const item = form.items[idx]
  if (commodityId === item._lastCommodityId) return
  item._lastCommodityId = commodityId
  item._fangManual = false // 换商品重置手填面积标记
  item.manualArea = false
  item.fangPending = false
  item.doorPending = false
  item._manualAmount = null // 换商品重置手改金额
  const commodity = commodityOptions.value.find((c) => c.id === commodityId)
  if (!commodity) return
  item.productName = commodity.name || ''
  item.unitPrice = commodity.defaultPrice ?? null
  item.unit = commodity.unit || ''
  item.materialCost = commodity.materialCost ?? null
  item.laborCost = commodity.laborCost ?? null
  item.accessoryCost = commodity.accessoryCost ?? null
  // 总成本 = 材料 + 人工 + 配件，若三者都为空则回退使用原有 costPrice
  const mc = Number(commodity.materialCost || 0)
  const lc = Number(commodity.laborCost || 0)
  const ac = Number(commodity.accessoryCost || 0)
  item.cost = (mc || lc || ac) ? mc + lc + ac : (commodity.costPrice ?? null)
  if (commodity.productType && !item.productType) item.productType = commodity.productType

  // 商品自带"推拉门"类型时，同样触发包边选择提示（选商品不经过类型列的 @change）
  if (commodity.productType === '推拉门') {
    onProductTypeChange(idx, '推拉门')
  } else if (edgeWrapPromptIdx.value === idx) {
    // 换成了非推拉门商品，清掉残留的包边提示
    edgeWrapPromptIdx.value = null
  }

  // 方/sqm 切换时清空面积（会由公式重算）
  const unit = commodity.unit || ''
  if (unit === '方' || unit === 'sqm') {
    item.fangCount = null
  }

  // 优先使用商品绑定的 formulaId（用完整列表，避免非默认公式找不到），没有则按 unit 匹配
  let formula = null
  if (commodity.formulaId) {
    formula = formulaList.value.find(f => f.id === commodity.formulaId) || null
  }
  if (!formula) {
    formula = formulaMap.value[commodity.unit]
  }
  if (formula) {
    item.formulaId = formula.id
    item.formulaExpression = formula.formula || ''
  } else {
    item.formulaId = null
    item.formulaExpression = ''
  }

  // 商品选择后回填下拉 key（显示真名）
  item._pickKey = 'c' + commodityId

  calcItemQty(idx)
}

function parseAliases(aliases) {
  if (!aliases) return []
  try {
    const arr = JSON.parse(aliases)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

// 商品下拉选项：真名 + 别名 + 订单行自定义名称（value 编码 id 与名称，选中即知用哪个名字）
const commodityPickOptions = computed(() => {
  const opts = []
  for (const c of commodityOptions.value) {
    opts.push({ value: 'c' + c.id, label: c.name, commodityId: c.id })
    for (const alias of parseAliases(c.aliases)) {
      if (alias && alias !== c.name) {
        opts.push({ value: 'a' + c.id + '|' + alias, label: alias, commodityId: c.id })
      }
    }
  }
  for (const o of customPickOptions.value) opts.push(o)
  return opts
})

function commodityName(id) {
  return commodityOptions.value.find((c) => c.id === id)?.name || ''
}

// 下拉变化：解析 c{id} / a{id}|{别名} / 自定义名称 → 设商品 → 若是别名/自定义则覆盖名称
function onPickChange(idx, pickKey) {
  delete pendingPickText[idx] // 用户已点选/回车确认，此前的未确认输入作废
  if (!pickKey) return
  const item = form.items[idx]
  if (pickKey.startsWith('a')) {
    const sep = pickKey.indexOf('|')
    const id = Number(pickKey.slice(1, sep))
    const alias = pickKey.slice(sep + 1)
    item.commodityId = id
    onCommoditySelect(idx, id)
    item.productName = alias || item.productName
    item._pickKey = pickKey // 选中别名时下拉显示别名而非真名
  } else if (/^c\d+$/.test(pickKey)) {
    const id = Number(pickKey.slice(1))
    item.commodityId = id
    onCommoditySelect(idx, id)
  } else {
    // allow-create 输入/点创建项时：若文本精确匹配已有商品真名 → 等同选中该商品
    // （否则 allow-create 会把输入当作裸名称，单价/公式/成本都不会带入）
    const exactCommodity = commodityOptions.value.find(c => c.name === pickKey)
    if (exactCommodity) {
      item.commodityId = exactCommodity.id
      onCommoditySelect(idx, exactCommodity.id)
    } else {
      // 自定义名称：保留已选商品关联，仅覆盖本行显示名
      item.productName = pickKey
      item._pickKey = pickKey // 下拉同步显示自定义名称（无需选中创建项）
    }
  }
}

// ===== 商品下拉：输入即提交（无需点选创建项）=====
// el-select allow-create 失焦会丢弃未确认的输入文字，这里在输入时实时捕获，
// 失焦/保存时自动提交为自定义名称（或精确命中的已有商品/别名）
const pendingPickText = {}

function onItemsTableInput(e) {
  const input = e.target
  if (input.tagName !== 'INPUT') return
  const td = input.closest('td')
  if (!td || !td.classList.contains('col-commodity')) return
  // data-tab-row 在 td 内部的 el-select 根元素上，须从 input 向上找（td 向上找不到）
  const rowEl = input.closest('[data-tab-row]')
  const idx = Number(rowEl?.getAttribute('data-tab-row'))
  if (Number.isNaN(idx)) return
  pendingPickText[idx] = input.value
}

function onPickBlur(idx) {
  const text = (pendingPickText[idx] || '').trim()
  delete pendingPickText[idx]
  if (!text) return
  const item = form.items[idx]
  if (!item) return
  const cur = commodityPickOptions.value.find(o => o.value === item._pickKey)
  if (cur && cur.label === text) return
  // 精确命中已有商品名/别名 → 等同选中该项；否则按自定义名称提交
  const exact = commodityPickOptions.value.find(o => o.label === text)
  onPickChange(idx, exact ? exact.value : text)
}

// 保存前兜底提交所有未失焦的输入（如 Ctrl+Enter 保存时焦点还在输入框）
function commitAllPendingPicks() {
  for (const idxStr of Object.keys(pendingPickText)) {
    const idx = Number(idxStr)
    if (Number.isNaN(idx) || !form.items[idx]) continue
    onPickBlur(idx)
  }
}

// 当前名称可"存为别名"：条件是已选商品 且 名称非真名 且 非已有别名
function canSaveAsAlias(item) {
  if (!item || !item.commodityId) return false
  const name = (item.productName || '').trim()
  if (!name) return false
  const c = commodityOptions.value.find(x => x.id === item.commodityId)
  if (!c) return false
  if (name === (c.name || '')) return false
  if (parseAliases(c.aliases).includes(name)) return false
  return true
}

async function saveAsAlias(idx) {
  onPickBlur(idx) // 输入未确认的文字先提交，再按最新名称存别名
  const item = form.items[idx]
  if (!canSaveAsAlias(item)) return
  const name = (item.productName || '').trim()
  const c = commodityOptions.value.find(x => x.id === item.commodityId)
  if (!c) return
  const newAliases = [...parseAliases(c.aliases), name]
  try {
    // 商品更新接口要求 code/name 等必填，需传完整商品字段（剔除时间/软删）
    const { createTime, updateTime, deleted, ...rest } = c
    await updateCommodity(item.commodityId, { ...rest, aliases: JSON.stringify(newAliases) })
    // 更新本地缓存
    c.aliases = JSON.stringify(newAliases)
    // 该名称已成为正式别名，移除订单行合成的自定义选项，避免重复
    customPickOptions.value = customPickOptions.value.filter((o) => o.value !== 'a' + item.commodityId + '|' + name)
    ElMessage.success(`已把「${name}」存为「${c.name}」的别名`)
  } catch (e) {
    if (!e._handled) ElMessage.error(e.message || '保存别名失败')
  }
}

function totalItemCost(item) {
  if (!item) return null
  const mc = Number(item.materialCost || 0)
  const lc = Number(item.laborCost || 0)
  const ac = Number(item.accessoryCost || 0)
  if (mc || lc || ac) return mc + lc + ac
  return item.cost != null ? Number(item.cost) : null
}

function openCostDialog(idx) {
  const item = form.items[idx]
  if (!item) return
  costEditIndex.value = idx
  costForm.materialCost = item.materialCost ?? null
  costForm.laborCost = item.laborCost ?? null
  costForm.accessoryCost = item.accessoryCost ?? null
  costDialogVisible.value = true
}

function handleCostDialogClose() {
  // Just close without saving when clicking outside/Escape
  costDialogVisible.value = false
}

function saveCostEdit() {
  const idx = costEditIndex.value
  if (idx < 0 || idx >= form.items.length) return
  const item = form.items[idx]
  const origMc = item.materialCost
  const origLc = item.laborCost
  const origAc = item.accessoryCost
  const origCost = item.cost

  item.materialCost = costForm.materialCost
  item.laborCost = costForm.laborCost
  item.accessoryCost = costForm.accessoryCost

  const mc = Number(costForm.materialCost || 0)
  const lc = Number(costForm.laborCost || 0)
  const ac = Number(costForm.accessoryCost || 0)

  if (mc || lc || ac) {
    item.cost = mc + lc + ac
  } else if (origMc !== costForm.materialCost || origLc !== costForm.laborCost || origAc !== costForm.accessoryCost) {
    item.cost = null
  } else {
    item.cost = origCost
  }

  costDialogVisible.value = false
}

// 计算单项行总金额（数量×单价+附加费）
function computedItemTotal(item) {
  if (!item) return null
  if (item.fangPending || item.doorPending) return 0
  let total = 0
  if (item.quantity != null && item.unitPrice != null) {
    total = multiplyMoney(item.quantity, item.unitPrice)
  }
  if (item.extraFee != null && Number(item.extraFee) > 0) {
    total += Number(item.extraFee)
  }
  return total || null
}

// ===== 金额自由编辑（砍价）：点击金额列可手改，改后优先显示手改值 =====
const editAmountIdx = ref(null) // 当前处于编辑态的金额行索引
const amountInputRefs = ref([])

function displayItemAmount(item) {
  if (item._manualAmount != null) return formatMoney(item._manualAmount)
  const auto = computedItemTotal(item)
  return auto != null ? formatMoney(auto) : '-'
}

function startAmountEdit(idx) {
  const item = form.items[idx]
  if (!item) return
  // 待定行金额为 0，不允许手改
  if (item.fangPending || item.doorPending) {
    ElMessage.warning('待定行金额为 0，请先取消待定')
    return
  }
  editAmountIdx.value = idx
  nextTick(() => {
    const el = amountInputRefs.value[idx]
    if (el) {
      el.focus()
      el.select()
    }
  })
}

function onAmountInput(idx, val) {
  const item = form.items[idx]
  if (!item) return
  if (val === '' || val == null) {
    item._manualAmount = null
    return
  }
  const n = Number(val)
  item._manualAmount = isNaN(n) || n < 0 ? null : Math.round(n * 100) / 100
}

function finishAmountEdit(idx, val) {
  const item = form.items[idx]
  if (!item) return
  onAmountInput(idx, val)
  editAmountIdx.value = null
  if (item._manualAmount != null) {
    ElMessage.success(`该行金额已改为 ¥${formatMoney(item._manualAmount)}`)
  }
}

// 公式覆盖变更时
function onFormulaChange(idx) {
  const item = form.items[idx]
  if (item.formulaId) {
    const formula = formulaList.value.find(f => f.id === item.formulaId)
    if (formula) {
      item.formulaExpression = formula.formula || ''
    }
  } else {
    // 清空选择 = 回退到 unit 匹配
    item.formulaId = null
    item.formulaExpression = ''
  }
  calcItemQty(idx)
}

// 附加费变更时（触发表格重算显示）
function onExtraFeeChange(idx) {
  calcItemQty(idx)
}

async function onUnitChange(val) {
  if (!val || formulaMap.value[val]) return
  try {
    await ElMessageBox.confirm(
      `单位"${val}"在计算公式中不存在，是否新增一个公式？`,
      '单位不存在',
      { confirmButtonText: '新增公式', cancelButtonText: '取消', type: 'warning' }
    )
    const nameMap = { '套': '樘数计价', '方': '面积计价', 'sqm': '面积计价', '米': '延米计价', '吊脚': '吊脚计价' }
    const formulaExpr = { '套': '樘数', '方': '宽*高*樘数/1000000', 'sqm': '宽*高*樘数/1000000', '米': '宽/1000', '吊脚': '吊脚' }
    const paramDefs = {
      '套': [{ name: '樘数', label: '樘数', unit: '樘' }],
      '方': [{ name: '宽', label: '宽(mm)', unit: 'mm' }, { name: '高', label: '高(mm)', unit: 'mm' }, { name: '樘数', label: '樘数', unit: '樘', default: '1' }],
      'sqm': [{ name: '宽', label: '宽(mm)', unit: 'mm' }, { name: '高', label: '高(mm)', unit: 'mm' }, { name: '樘数', label: '樘数', unit: '樘', default: '1' }],
      '米': [{ name: '宽', label: '长度(mm)', unit: 'mm' }],
      '吊脚': [{ name: '吊脚', label: '吊脚', unit: '个' }],
    }
    const formulaData = {
      name: nameMap[val] || `${val}计价`,
      unit: val,
      formula: formulaExpr[val] || val,
      parameters: JSON.stringify(paramDefs[val] || []),
      description: '',
      sort: 0,
    }
    await addFormula(formulaData)
    await loadFormulas()
    ElMessage.success(`公式"${formulaData.name}"已创建`)
  } catch {
    // user cancelled, clear the unit
    newCommodityForm.unit = ''
  }
}


function openAddCommodity(idx) {
  addCommodityIdx.value = idx
  newCommodityForm.code = ''
  newCommodityForm.name = ''
  newCommodityForm.productType = ''
  newCommodityForm.unit = ''
  newCommodityForm.defaultPrice = null
  newCommodityForm.costPrice = null
  newCommodityForm.materialCost = null
  newCommodityForm.laborCost = null
  newCommodityForm.accessoryCost = null
  showAddCommodity.value = true
}

async function handleQuickAddCommodity() {
  if (!newCommodityForm.name?.trim()) {
    ElMessage.warning('请输入商品名称')
    return
  }
  savingCommodity.value = true
  try {
    const payload = {
      code: newCommodityForm.code?.trim() || '',
      name: newCommodityForm.name.trim(),
      productType: newCommodityForm.productType?.trim() || '',
      unit: newCommodityForm.unit?.trim() || '',
      defaultPrice: newCommodityForm.defaultPrice,
      costPrice: newCommodityForm.costPrice,
      materialCost: newCommodityForm.materialCost,
      laborCost: newCommodityForm.laborCost,
      accessoryCost: newCommodityForm.accessoryCost,
      aliases: JSON.stringify((newCommodityForm.aliases || '').split(/[,，、]/).map(s => s.trim()).filter(Boolean)),
    }
    const res = await addCommodity(payload)
    const created = res.data?.data ?? res.data
    ElMessage.success('商品创建成功')
    showAddCommodity.value = false
    // Auto-fill the item row
    const idx = addCommodityIdx.value
    if (idx >= 0 && idx < form.items.length) {
      const item = form.items[idx]
      item.commodityId = created?.id || null
      if (created?.id) item._pickKey = 'c' + created.id
      item.productName = created?.name || newCommodityForm.name.trim()
      item.productType = created?.productType || newCommodityForm.productType?.trim() || ''
      item.unitPrice = created?.defaultPrice ?? newCommodityForm.defaultPrice ?? null
      item.unit = created?.unit || newCommodityForm.unit?.trim() || ''
      item.materialCost = created?.materialCost ?? newCommodityForm.materialCost ?? null
      item.laborCost = created?.laborCost ?? newCommodityForm.laborCost ?? null
      item.accessoryCost = created?.accessoryCost ?? newCommodityForm.accessoryCost ?? null
      const mc = Number(item.materialCost || 0)
      const lc = Number(item.laborCost || 0)
      const ac = Number(item.accessoryCost || 0)
      item.cost = (mc || lc || ac) ? mc + lc + ac : (created?.costPrice ?? newCommodityForm.costPrice ?? null)
      if (created.formulaId) item.formulaId = created.formulaId
      calcItemQty(idx)
    }
    // Refresh commodity list so new item appears in dropdown
    searchCommodities('')
  } catch (e) {
    console.error('创建商品失败:', e)
    ElMessage.error('创建商品失败，请稍后重试')
  } finally {
    savingCommodity.value = false
  }
}

// 安全的数学表达式求值（不依赖 eval/new Function，避免 CSP 限制）
function safeEval(expr) {
  // 只允许数字、运算符（含 % 取模）、括号、小数点、空格
  // 与后端 SafeMathEvaluator 保持一致：支持 %、一元正负号；缺右括号/多点小数/尾随字符 → 判定失败返回 NaN
  if (!/^[\d+\-*/().%\s]+$/.test(expr)) return NaN
  try {
    // 用递归下降解析器处理四则运算、取模和括号。
    // 保留空格（不剥离）：空格分隔两个数字（如"宽 高"漏写运算符）会被尾随校验拦截，
    // 而不是剥空格后粘成一个大数静默算错。
    const s = expr
    let pos = 0
    function skipWs() { while (pos < s.length && s[pos] === ' ') pos++ }
    function parseExpression() {
      skipWs()
      let val = parseTerm()
      while (true) {
        skipWs()
        if (s[pos] === '+') { pos++; val += parseTerm() }
        else if (s[pos] === '-') { pos++; val -= parseTerm() }
        else break
      }
      return val
    }
    function parseTerm() {
      skipWs()
      let val = parseFactor()
      while (true) {
        skipWs()
        if (s[pos] === '*') { pos++; val *= parseFactor() }
        else if (s[pos] === '/') {
          pos++
          const divisor = parseFactor()
          if (divisor === 0) return NaN
          val /= divisor
        }
        else if (s[pos] === '%') {
          pos++
          const divisor = parseFactor()
          if (divisor === 0) return NaN
          val %= divisor
        }
        else break
      }
      return val
    }
    function parseFactor() {
      skipWs()
      if (s[pos] === '(') {
        pos++
        const val = parseExpression()
        skipWs()
        if (s[pos] !== ')') return NaN // 缺右括号 → 判定失败（与后端一致）
        pos++ // skip ')'
        return val
      }
      if (s[pos] === '-') {
        pos++
        return -parseFactor()
      }
      if (s[pos] === '+') {
        pos++ // 一元正号（与后端一致）
        return parseFactor()
      }
      const start = pos
      let hasDot = false
      while (pos < s.length && /[\d.]/.test(s[pos])) {
        if (s[pos] === '.') {
          if (hasDot) return NaN // 多点小数 → 判定失败
          hasDot = true
        }
        pos++
      }
      if (start === pos) return NaN // 无数字（如连续运算符）
      return parseFloat(s.slice(start, pos))
    }
    const result = parseExpression()
    skipWs()
    if (pos !== s.length) return NaN // 尾随字符（多余括号/操作数漏运算符）→ 判定失败（与后端一致）
    return result
  } catch {
    return NaN
  }
}

// 中文输入法符号 → 英文公式符号（历史公式可能含中文括号/乘除号，统一转换避免解析失败）
function normalizeFormulaSymbols(s) {
  return s
    .replace(/（/g, '(')
    .replace(/）/g, ')')
    .replace(/×/g, '*')
    .replace(/÷/g, '/')
    .replace(/＋/g, '+')
    .replace(/－/g, '-')
    .replace(/　/g, ' ')
}

function evalFormula(expression, params) {
  if (!expression) return null
  let expr = normalizeFormulaSymbols(expression)
  // 按键名长度降序替换，避免短名误替长名（如 "宽" 替换 "宽高" 的一部分）
  const keys = Object.keys(params).sort((a, b) => b.length - a.length)
  for (const k of keys) {
    const numVal = parseFloat(params[k]) || 0
    expr = expr.replaceAll(k, numVal)
  }
  const result = safeEval(expr)
  return isNaN(result) ? null : Math.round(result * 100) / 100
}

// 公式变量名 → item 字段的固定映射
const FORMULA_VAR_MAP = {
  '宽': 'width',
  '高': 'height',
  '墙厚': 'wallThickness',
  '吊脚': 'diaojiao',
  '樘数': 'doorCount',
  '面积': 'fangCount',
}

// 待定开关：面积/樘数标"待定"时金额按 0，仅显示单价；取消后重新计算
function togglePending(item, key, idx) {
  item[key] = !item[key]
  if (key === 'fangPending' && item.fangPending) {
    item._fangManual = false // 待定时忽略手填面积标记，取消后按宽高自动算
  }
  calcItemQty(idx)
}

function calcItemQty(idx) {
  const item = form.items[idx]
  // 待定：面积或樘数任一标"待定" → 金额按 0，仅显示单价
  if (item.fangPending || item.doorPending) {
    item.quantity = 0
    item.manualArea = false
    return
  }

  // 优先按 formulaId 查找绑定的公式（用完整列表 formulaList，
  // formulaMap 只含每单位默认公式，手动选的非默认/新增公式会找不到而被重置）
  let formula = null
  if (item.formulaId) {
    formula = formulaList.value.find(f => f.id === item.formulaId) || null
  }
  if (!formula) {
    formula = formulaMap.value[item.unit]
  }

  if (formula) {
    item._formulaError = false
    // 使用绑定公式时，同步更新 expression 和 unit 相关信息
    item.formulaId = formula.id
    if (!item.formulaExpression || item.formulaExpression !== formula.formula) {
      item.formulaExpression = formula.formula || ''
    }

    // 从公式定义中解析参数默认值，回填到空的 item 字段
    let params = []
    if (typeof formula.parameters === 'string' && formula.parameters) {
      try {
        params = JSON.parse(formula.parameters)
      } catch {
        // 兼容历史数据双重转义 [{\"name\":...}] → [{"name":...}]
        try { params = JSON.parse(formula.parameters.replace(/\\"/g, '"')) } catch { params = [] }
      }
    } else if (formula.parameters) {
      params = formula.parameters
    }
    for (const p of params) {
      const field = FORMULA_VAR_MAP[p.name]
      const autoKey = '_auto_' + field
      // 回填条件：字段为空，或字段仍等于上次公式的自动默认值（切换公式时允许覆盖为新默认）
      const isOldAuto = item[autoKey] != null && Number(item[field]) === Number(item[autoKey])
      if (field && p.default != null && p.default !== '' && (item[field] == null || item[field] === '' || isOldAuto)) {
        item[field] = Number(p.default) || 0
        item[autoKey] = Number(p.default) || 0
      } else {
        delete item[autoKey]
      }
    }

    // 直接从 item 字段构建公式变量，不依赖 formulaParams
    const vars = {}
    for (const [varName, field] of Object.entries(FORMULA_VAR_MAP)) {
      const val = item[field]
      if (val != null && val !== '') vars[varName] = Number(val) || 0
    }
    // 樘数兜底：公式若含"樘数"但 item.doorCount 为空时，默认为 1 并回填输入框显示
    if (vars['樘数'] == null) {
      if (item.formulaExpression.includes('樘数')) item.doorCount = 1
      vars['樘数'] = 1
    }

    const result = evalFormula(item.formulaExpression, vars)
    const dc = item.doorCount != null && item.doorCount > 0 ? Number(item.doorCount) : 1
    if (result != null && result > 0 && !item._fangManual) {
      // 单面积模型：公式只算"每樘计量值"，数量 = 每樘值 × 樘数（樘数即数量）
      item.quantity = +(result * dc).toFixed(2)
      // 面积列显示实际公式的每樘值（不能硬编码包边/面积算法，否则自定义公式的
      // 加减乘除（如 (宽+高*2)/1000 里的 *2）会被漏掉，面积列与金额不一致）
      item.fangCount = +(result).toFixed(2)
      item.manualArea = false
    } else if (item._fangManual && item.fangCount != null && item.fangCount > 0) {
      // 用户手填面积优先（手填值为每樘计量值），数量 = 手填值 × 樘数。
      // 不依赖公式是否含 /1000000——用户手填了就用手填值（与后端 manualArea 无条件分支一致）
      item.quantity = +(Number(item.fangCount) * dc).toFixed(2)
      item.manualArea = true
    } else {
      // 公式无法计算（缺参数或表达式有误）——标记供行内提示，金额按 0
      // （不要保留 quantity=1 默认值：面积商品不填尺寸却按 1 兜出金额，会让用户误以为算好了）
      item._formulaError = true
      item.manualArea = false
      item.quantity = 0
    }
  } else {
    item._formulaError = false
    item.manualArea = false
    // 无公式的 fallback
    if (item.doorCount != null && item.doorCount > 0) {
      item.quantity = item.doorCount
    }

    // 无公式时面积列按商品类型兜底：包边每樘米数 (宽+高)/1000，其他每樘㎡ 宽×高/1000000
    if (isBaoBianFormula(item) && item.width != null && item.width > 0 && item.height != null && item.height > 0) {
      item.fangCount = +((item.width + item.height) / 1000).toFixed(2)
    } else if (!item._fangManual && item.width != null && item.width > 0 && item.height != null && item.height > 0) {
      item.fangCount = +(item.width * item.height / 1000000).toFixed(2)
    }
  }

  // 推拉门行改高/宽/樘数时，同步到其包边行（包边高=上一行高×2，樘数跟随）
  if (item.productType === '推拉门' && item.height != null && item.height > 0) {
    form.items.forEach((edgeItem, i) => {
      if (edgeItem.sourceIdx === idx && edgeItem.productName) {
        edgeItem.height = item.height * 2
        edgeItem.width = item.width || edgeItem.width
        edgeItem.doorCount = item.doorCount != null ? item.doorCount : edgeItem.doorCount
        calcItemQty(i)
      }
    })
  }
}

// 复制一行商品：sourceIdx 传 newSourceIdx（null 表示不联动）
function copyItemRow(src, newSourceIdx) {
  const newRow = createItemRow({
    commodityId: src.commodityId,
    productName: src.productName,
    series: src.series,
    color: src.color,
    productType: src.productType,
    unit: src.unit,
    width: src.width,
    height: src.height,
    wallThickness: src.wallThickness,
    glassType: src.glassType,
    lockPosition: src.lockPosition,
    doorCount: src.doorCount,
    fangCount: src.fangCount,
    diaojiao: src.diaojiao,
    quantity: src.quantity,
    unitPrice: src.unitPrice,
    cost: src.cost,
    materialCost: src.materialCost ?? null,
    laborCost: src.laborCost ?? null,
    accessoryCost: src.accessoryCost ?? null,
    extraFee: src.extraFee ?? null,
    remark: src.remark,
    formulaId: src.formulaId,
    formulaSnapshot: src.formulaSnapshot || '',
    formulaParams: src.formulaParams,
    formulaExpression: src.formulaExpression,
    sourceIdx: newSourceIdx,
    // 忠实继承源行的状态：手填面积/待定标记（否则副本会因 quantity=0 无法保存）
    _fangManual: !!src._fangManual,
    manualArea: !!src.manualArea,
    fangPending: !!src.fangPending,
    doorPending: !!src.doorPending,
  })
  // 继承源行的下拉 key，别名/自定义名称的显示保持一致
  newRow._pickKey = src._pickKey || (src.commodityId ? 'c' + src.commodityId : '')
  return newRow
}

function duplicateItem(idx) {
  const src = form.items[idx]
  const newRow = copyItemRow(src, src.sourceIdx)
  form.items.splice(idx + 1, 0, newRow)
  return newRow
}

function toggleSelectItem(idx) {
  const item = form.items[idx]
  const i = selectedItems.value.indexOf(idx)
  if (i === -1) {
    // 勾选
    selectedItems.value.push(idx)
    // 如果是推拉门，同时勾选所有以 idx 为父的包边行
    if (item.productType === '推拉门') {
      form.items.forEach((it, j) => {
        if (it.sourceIdx === idx && !selectedItems.value.includes(j)) {
          selectedItems.value.push(j)
        }
      })
    }
    // 如果是包边，同时勾选父行
    if (item.sourceIdx !== null) {
      const parentIdx = item.sourceIdx
      if (!selectedItems.value.includes(parentIdx)) {
        selectedItems.value.push(parentIdx)
      }
    }
  } else {
    // 取消勾选
    selectedItems.value.splice(i, 1)
    // 如果是推拉门，同时取消所有以 idx 为父的包边行
    if (item.productType === '推拉门') {
      form.items.forEach((it, j) => {
        if (it.sourceIdx === idx) {
          const si = selectedItems.value.indexOf(j)
          if (si !== -1) selectedItems.value.splice(si, 1)
        }
      })
    }
    // 如果是包边，同时取消父行
    if (item.sourceIdx !== null) {
      const parentIdx = item.sourceIdx
      const si = selectedItems.value.indexOf(parentIdx)
      if (si !== -1) selectedItems.value.splice(si, 1)
    }
  }
}

function toggleSelectAll() {
  if (selectedItems.value.length === form.items.length) {
    selectedItems.value = []
  } else {
    selectedItems.value = form.items.map((_, i) => i)
  }
}

function batchDuplicate() {
  if (!selectedItems.value.length) {
    ElMessage.warning('请先勾选要复制的行')
    return
  }
  // 按原顺序复制，全部追加到末尾（不插入到中间）
  const selected = [...selectedItems.value].sort((a, b) => a - b)
  const base = form.items.length
  const srcToCopyIdx = {}
  selected.forEach((idx, k) => { srcToCopyIdx[idx] = base + k })
  const copies = selected.map(idx => {
    const src = form.items[idx]
    // 包边联动：父行(sourceIdx)若也在选中范围内，指向其副本的新索引
    const parentCopyIdx = src.sourceIdx != null ? srcToCopyIdx[src.sourceIdx] : null
    return copyItemRow(src, parentCopyIdx)
  })
  form.items.push(...copies)
  selectedItems.value = []
  ElMessage.success(`已复制 ${copies.length} 行`)
}

function batchDelete() {
  if (!selectedItems.value.length) {
    ElMessage.warning('请先勾选要删除的行')
    return
  }
  const count = selectedItems.value.length
  const sorted = [...selectedItems.value].sort((a, b) => b - a)
  for (const idx of sorted) {
    form.items.splice(idx, 1)
  }
  rebuildSourceIdx()
  selectedItems.value = []
  edgeWrapPromptIdx.value = null
  ElMessage.success(`已删除 ${count} 行`)
}

function getLinkedBlock(idx) {
  const block = [idx]
  const item = form.items[idx]
  // 如果是推拉门，包含所有包边子行
  if (item.productType === '推拉门') {
    form.items.forEach((it, i) => {
      if (it.sourceIdx === idx) block.push(i)
    })
  }
  // 如果是包边，包含父行和父行的所有子行
  if (item.sourceIdx !== null) {
    const parentIdx = item.sourceIdx
    if (!block.includes(parentIdx)) block.push(parentIdx)
    form.items.forEach((it, i) => {
      if (it.sourceIdx === parentIdx && !block.includes(i)) block.push(i)
    })
  }
  return [...new Set(block)].sort((a, b) => a - b)
}

function swapBlocks(blockA, blockB) {
  const sortedA = [...blockA].sort((a, b) => a - b)
  const sortedB = [...blockB].sort((a, b) => a - b)
  // 提取两个块的 items
  const aItems = sortedA.map(i => form.items[i])
  const bItems = sortedB.map(i => form.items[i])
  // 合并所有需要删除的索引（从大到小）
  const allIndices = [...sortedA, ...sortedB].sort((a, b) => b - a)
  for (const i of allIndices) {
    form.items.splice(i, 1)
  }
  // 在较小的原始位置插入：blockA 的位置放 blockB，blockB 的位置放 blockA
  const insertAt = Math.min(...sortedA, ...sortedB)
  // 原来 blockA 在前时：先放 bItems 再放 aItems（交换位置）
  const aBeforeB = sortedA[0] < sortedB[0]
  if (aBeforeB) {
    form.items.splice(insertAt, 0, ...bItems, ...aItems)
  } else {
    form.items.splice(insertAt, 0, ...aItems, ...bItems)
  }
  rebuildSourceIdx()
}

function batchMoveUp() {
  if (!selectedItems.value.length) return
  // 收集所有需要移动的块（去重）
  const blocks = []
  const visited = new Set()
  const sorted = [...selectedItems.value].sort((a, b) => a - b)
  for (const idx of sorted) {
    if (visited.has(idx)) continue
    const block = getLinkedBlock(idx)
    block.forEach(i => visited.add(i))
    blocks.push(block)
  }
  // 从上到下逐块上移
  for (const block of blocks) {
    const blockStart = Math.min(...block)
    if (blockStart === 0) continue
    // 上方的目标行
    const targetIdx = blockStart - 1
    if (block.includes(targetIdx)) continue
    const targetBlock = getLinkedBlock(targetIdx)
    if (block.some(i => targetBlock.includes(i))) continue
    swapBlocks(targetBlock, block)
  }
  selectedItems.value = []
}

function batchMoveDown() {
  if (!selectedItems.value.length) return
  // 收集所有需要移动的块（去重）
  const blocks = []
  const visited = new Set()
  const sorted = [...selectedItems.value].sort((a, b) => b - a)
  for (const idx of sorted) {
    if (visited.has(idx)) continue
    const block = getLinkedBlock(idx)
    block.forEach(i => visited.add(i))
    blocks.push(block)
  }
  // 从下到上逐块下移
  for (const block of blocks) {
    const blockEnd = Math.max(...block)
    if (blockEnd >= form.items.length - 1) continue
    // 下方的目标行
    const targetIdx = blockEnd + 1
    if (block.includes(targetIdx)) continue
    const targetBlock = getLinkedBlock(targetIdx)
    if (block.some(i => targetBlock.includes(i))) continue
    swapBlocks(block, targetBlock)
  }
  selectedItems.value = []
}

function rebuildSourceIdx() {
  form.items.forEach(it => { it.sourceIdx = null })
  for (let i = 0; i < form.items.length; i++) {
    if (form.items[i].productType !== '推拉门') continue
    for (let j = i + 1; j < form.items.length; j++) {
      const it = form.items[j]
      const name = (it.productName || '').toLowerCase()
      const ptype = (it.productType || '').toLowerCase()
      // 包边行的商品名通常包含"包边"，或 productType 为单包/双包
      const isEdge = name.includes('包边') || ptype === '单包' || ptype === '双包'
      if (isEdge && it.sourceIdx === null) {
        it.sourceIdx = i
        break
      }
    }
  }
}

// ===== Save =====
async function doSave() {
  if (saving.value) return false
  commitAllPendingPicks() // 商品下拉未确认的输入先提交，避免别名丢失
  if (!formRef.value) return false
  try {
    await formRef.value.validate()
  } catch {
    return false
  }

  if (!form.items.length) {
    ElMessage.warning('请至少添加一个商品')
    return false
  }

  for (let i = 0; i < form.items.length; i++) {
    const it = form.items[i]
    if (!it.productName && !it.commodityId) {
      ElMessage.warning(`第 ${i + 1} 行商品信息不完整，请选择商品或填写名称`)
      return false
    }
    if (it.fangPending || it.doorPending) {
      // 待定行：数量可为 0，金额按 0
    } else if (it.quantity == null || it.quantity <= 0) {
      ElMessage.warning(`第 ${i + 1} 行数量必须大于 0`)
      return false
    }
    if (it.unitPrice == null || it.unitPrice < 0) {
      ElMessage.warning(`第 ${i + 1} 行单价不能为负数`)
      return false
    }
  }

  saving.value = true
  try {
    const data = {
      orderType: form.orderType,
      customerName: form.customerName.trim(),
      customerPhone: form.customerPhone.trim(),
      customerAddress: form.customerAddress.trim(),
      orderDate: form.orderDate,
      deposit: form.deposit || 0,
      remark: form.remark.trim(),
      notice: form.notice.trim(),
      hiddenProductTypes: JSON.stringify([...editHiddenTypes.value]),
      items: form.items.map((it) => ({
        commodityId: it.commodityId,
        productName: it.productName,
        series: it.series,
        color: it.color,
        productType: it.productType,
        unit: it.unit,
        width: it.width,
        height: it.height,
        wallThickness: it.wallThickness,
        glassType: it.glassType,
        lockPosition: it.lockPosition,
        doorCount: it.doorCount,
        fangCount: it.fangCount,
        diaojiao: it.diaojiao,
        quantity: it.quantity,
        unitPrice: it.unitPrice,
        cost: it.cost,
        materialCost: it.materialCost,
        laborCost: it.laborCost,
        accessoryCost: it.accessoryCost,
        extraFee: it.extraFee ?? null,
        amount: it._manualAmount != null ? it._manualAmount : null,
        manualArea: !!(it._fangManual && it.fangCount != null && Number(it.fangCount) > 0),
        fangPending: it.fangPending ?? false,
        doorPending: it.doorPending ?? false,
        formulaId: it.formulaId ?? null,
        formulaSnapshot: it.formulaSnapshot ?? null,
        remark: it.remark,
      })),
    }

    if (editId.value) {
      const isDirty = formSnapshotJson.value !== JSON.stringify({ ...form, hiddenProductTypes: [...editHiddenTypes.value] })
      if (isDirty) {
        await updateSaleOrder(editId.value, data)
        afterEditSave({ id: editId.value, ...data }, true)
      }
    } else {
      const res = await addSaleOrder(data)
      ElMessage.success(form.orderType === 'presale' ? '预算单已创建' : '订单已创建')
    }
    formDirty.value = false
    dialogVisible.value = false
    loadList()
    loadSummary()
    return true
  } catch (e) {
    console.error('保存订单失败:', e)
    ElMessage.error(editId.value ? '更新失败，请稍后重试' : '创建失败，请稍后重试')
    return false
  } finally {
    saving.value = false
  }
}

async function handleSave() {
  await doSave()
}

// 关闭前确认未保存的修改
function handleDialogBeforeClose(done) {
  if (formDirty.value) {
    ElMessageBox.confirm('有未保存的修改，确定关闭吗？', '提示', {
      confirmButtonText: '确定关闭',
      cancelButtonText: '继续编辑',
      type: 'warning',
    }).then(() => {
      formDirty.value = false
      done()
    }).catch(() => {})
  } else {
    done()
  }
}

// Ctrl+Enter 快捷保存（弹窗打开时注册全局监听）
function dialogKeyHandler(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSave()
  }
}
function onDialogOpened() {
  document.addEventListener('keydown', dialogKeyHandler)
}
function onDialogClosed() {
  document.removeEventListener('keydown', dialogKeyHandler)
}

// 页面离开提醒
function beforeUnloadHandler(e) {
  if (formDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnloadHandler))
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnloadHandler))

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

.btn-outline {
  padding: 9px 18px;
  background: transparent;
  color: #C5A265;
  border: 1px solid #C5A265;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-outline:hover {
  background: rgba(197, 162, 101, 0.08);
}
.btn-outline.danger {
  color: #e74c3c;
  border-color: #e74c3c;
}
.btn-outline.danger:hover {
  background: rgba(231, 76, 60, 0.06);
}
.btn-outline:disabled {
  opacity: 0.5;
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
  color: #666;
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

/* Date picker */
.date-picker {
  max-width: 280px;
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
  color: #333;
  width: 200px;
  font-family: inherit;
  background: transparent;
}
.search-input::placeholder {
  color: #ccc;
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
.summary-card.red .sc-value   { color: #ff6b6b; }
.summary-card.blue .sc-value  { color: #5b9bd5; }
.summary-card.teal .sc-value  { color: #56ccf2; }

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
  min-width: 900px;
}
.data-table th {
  background: #243447;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  font-size: 12px;
  padding: 10px 10px;
  text-align: left;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
  user-select: none;
}
.data-table td {
  padding: 10px 10px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
  vertical-align: middle;
}
.data-table tbody tr {
  transition: background 0.12s;
}
.data-table tbody tr:hover {
  background: #fafbfc;
}
.data-table tbody tr.row-clickable { cursor: pointer; }

.col-order-no {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}
.col-customer {
  font-weight: 600;
  color: #1a1a1a;
}
.col-date {
  font-size: 12px;
  color: #888;
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
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.type-sale {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.type-presale {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}

/* Confirm button */
.confirm-btn {
  color: #52c41a !important;
}
.confirm-btn:hover {
  background: rgba(82, 196, 26, 0.08) !important;
}

.money-red {
  color: #e74c3c;
}

.col-lock {
  width: 60px;
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
.status-processing {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
.status-completed {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.status-cancelled {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
}
.clear-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.clear-badge.cleared {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.clear-badge.uncleared {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
.clear-badge.cancelled {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
}

/* Action buttons */
.btn-text {
  background: none;
  border: none;
  color: #C5A265;
  font-size: 12px;
  cursor: pointer;
  padding: 3px 6px;
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

/* ===== Batch Bar ===== */
.batch-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; margin-top: 12px;
  background: #1a2332; border-radius: 10px;
  border: 1px solid rgba(197,162,101,.3);
}
.batch-count { color: #C5A265; font-weight: 600; font-size: 13px; }
.batch-hint { color: rgba(255,255,255,0.4); font-size: 13px; }
.batch-btn { padding: 6px 14px; font-size: 12px; }
.selected-row { background: rgba(197,162,101,.06) !important; }

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
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.section-dash {
  display: inline-block;
  width: 20px;
  height: 3px;
  background: #C5A265;
  border-radius: 2px;
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
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.field-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}
.field-value.mono {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  color: #666;
}
.field-value.money {
  font-weight: 700;
  font-size: 14px;
  color: #333;
}
.field-value.money.gold {
  color: #C5A265;
}

.clear-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  background: #fff7e6;
  color: #d46b08;
}
.clear-tag.cleared {
  background: #f6ffed;
  color: #389e0d;
}
.clear-tag.cancelled {
  background: #f5f5f5;
  color: #999;
}

/* Detail items table */
.items-table-wrap {
  overflow-x: auto;
}
.items-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 480px;
}
.items-table th {
  background: #fafbfc;
  font-weight: 600;
  color: #666;
  font-size: 12px;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}
.items-table td {
  padding: 10px 12px;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  color: #444;
}
.items-table tfoot td {
  border-top: 2px solid #eee;
  font-weight: 700;
  padding: 12px;
}
.items-table .col-num {
  text-align: center;
  min-width: 60px;
}
.items-table .col-money {
  text-align: right;
  white-space: nowrap;
}
.items-table .col-money.gold {
  color: #C5A265;
}
.items-table .total-label {
  text-align: right;
  color: #666;
  font-size: 13px;
}
.items-table .total-value {
  font-size: 15px;
  color: #C5A265;
  font-weight: 700;
}
.empty-items {
  text-align: center;
  padding: 30px 0;
  color: #ccc;
  font-size: 13px;
}

/* Product Type Summary */
.type-summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.type-summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fafbfc;
  border: 1px solid #f0ebe1;
  border-radius: 8px;
}
.type-summary-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}
.type-summary-val {
  font-size: 13px;
  font-weight: 700;
  color: #C5A265;
}
.type-hidden {
  opacity: 0.35;
}
.type-hidden .type-summary-val {
  color: #ccc;
  text-decoration: line-through;
}
.type-toggle-icon {
  font-size: 12px;
  margin-left: 4px;
}

/* ===== Add/Edit Dialog ===== */
.dialog-section {
  margin-bottom: 32px;
}
.dialog-section:last-of-type {
  margin-bottom: 8px;
}

.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0 24px;
  margin-bottom: 8px;
}
.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
  margin-bottom: 8px;
}
.span-2 {
  grid-column: span 2;
}

/* Items edit table */
.items-edit-wrap {
  overflow-x: auto;
  margin-top: 8px;
}
.items-edit-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1200px;
}
.items-edit-table th {
  background: #fafbfc;
  font-weight: 600;
  color: #666;
  font-size: 14px;
  padding: 12px 10px;
  text-align: center;
  border-bottom: 1px solid #eee;
  white-space: nowrap;
}
.items-edit-table td {
  padding: 10px 8px;
  vertical-align: top;
  border-bottom: 1px solid #f5f5f5;
}
.items-edit-table .col-check {
  width: 44px;
  text-align: center;
}
.items-edit-table .col-check input[type="checkbox"] {
  width: 19px;
  height: 19px;
  cursor: pointer;
  accent-color: #C5A265;
}
.items-edit-table .col-commodity {
  width: 200px;
  min-width: 140px;
}
/* 商品单元格：下拉（allow-create 直接输入自定义名称） */
.items-edit-table .col-commodity .el-select { width: 100%; }
.alias-option { padding-left: 20px; }
.items-edit-table .col-color {
  width: 110px;
}
.items-edit-table .col-type {
  width: 110px;
}
.items-edit-table .col-lock {
  width: 100px;
}
.items-edit-table .col-formula {
  width: 130px;
}
.formula-err {
  margin-top: 2px; font-size: 11px; color: #e74c3c; line-height: 1.2; white-space: nowrap;
}
.items-edit-table .col-unit {
  width: 100px;
}
.items-edit-table .col-remark {
  width: 130px;
}
.remark-input {
  width: 100%;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 8px;
  font-size: 13px;
}
.remark-input:focus {
  position: absolute;
  left: 0;
  top: 0;
  min-width: 140px;
  height: 100%;
  z-index: 100;
  box-shadow: 0 0 0 3px rgba(197,162,101,0.3), 0 4px 16px rgba(0,0,0,0.15);
  border-color: #C5A265;
  outline: none;
  background: #fff;
}
.items-edit-table .col-size {
  width: 110px;
}
.num-input {
  width: 100%;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 8px;
  font-size: 14px;
  box-sizing: border-box;
  text-align: center;
}
/* 面积/樘数"待定"：按钮在输入框聚焦展开(200px)后出现在其右侧内部，平时不占宽 */
.pending-in-btn {
  display: none;
  position: absolute;
  left: 138px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 11px;
  line-height: 1;
  padding: 3px 5px;
  border-radius: 3px;
  border: 1px solid #e6a23c;
  color: #e6a23c;
  background: #fff;
  cursor: pointer;
  white-space: nowrap;
  z-index: 210;
}
.items-edit-table td:focus-within .pending-in-btn {
  display: block;
}
.pending-in-btn:hover {
  background: #e6a23c;
  color: #fff;
}
.pending-text {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  color: #e6a23c;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
}
.num-input:focus {
  position: absolute;
  left: 0;
  top: 0;
  min-width: 120px;
  height: 100%;
  z-index: 100;
  box-shadow: 0 0 0 3px rgba(197,162,101,0.3), 0 4px 16px rgba(0,0,0,0.15);
  border-color: #C5A265;
  outline: none;
  background: #fff;
}
.items-edit-table .col-num {
  width: 95px;
}
.items-edit-table .col-price {
  width: 120px;
}
.items-edit-table .col-amount {
  white-space: nowrap;
  font-weight: 600;
  color: #C5A265;
  text-align: right;
  padding-right: 10px;
  font-size: 15px;
  vertical-align: middle;
}
/* 金额自由编辑（砍价） */
.amount-display {
  cursor: pointer;
  padding: 3px 6px;
  border-radius: 4px;
  transition: background 0.15s, outline 0.15s;
}
.amount-display:hover {
  background: rgba(197, 162, 101, 0.12);
  outline: 1px dashed #C5A265;
}
.amount-display.amount-edited {
  color: #e6a23c;
}
.amount-display.amount-edited::after {
  content: '✎';
  font-size: 11px;
  margin-left: 4px;
  color: #C5A265;
  font-weight: 400;
}
.amount-input {
  width: 90px;
  text-align: right;
  font-weight: 600;
  color: #C5A265;
}
.items-edit-table .col-act {
  width: 100px;
  text-align: center;
  white-space: nowrap;
}
.items-edit-table tfoot td {
  border-top: 2px solid #eee;
  font-weight: 700;
  padding: 14px 10px;
}
.items-edit-table .total-label {
  text-align: right;
  color: #666;
  font-size: 14px;
}
.items-edit-table .total-value {
  font-size: 18px;
  color: #C5A265;
  text-align: right;
  padding-right: 10px;
}

.item-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-batch-move {
  padding: 8px 12px;
  background: #fff;
  color: #666;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-batch-move:hover {
  border-color: #C5A265;
  color: #C5A265;
  background: rgba(197, 162, 101, 0.06);
}
.btn-edge-wrap {
  padding: 8px 12px;
  background: #C5A265;
  color: #fff;
  border: 1px solid #C5A265;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.btn-edge-wrap:hover {
  background: #b8944e;
  border-color: #b8944e;
}
.edge-wrap-prompt td {
  background: #fffbe6;
  border-bottom: 1px solid #f0e6c0;
}
.edge-wrap-prompt-cell {
  padding: 6px 12px !important;
  text-align: center;
}
.edge-wrap-label {
  font-size: 13px;
  color: #666;
  margin-right: 8px;
}
.edge-wrap-btn {
  padding: 3px 14px;
  margin: 0 4px;
  background: #C5A265;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}
.edge-wrap-btn:hover { background: #b8944e; }
.edge-wrap-skip {
  padding: 3px 10px;
  margin-left: 8px;
  background: none;
  color: #999;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}
.edge-wrap-skip:hover { color: #666; border-color: #bbb; }
.btn-batch-item {
  padding: 8px 16px;
  background: rgba(197, 162, 101, 0.08);
  color: #C5A265;
  border: 1px solid rgba(197, 162, 101, 0.2);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-batch-item:hover {
  background: rgba(197, 162, 101, 0.18);
  border-color: #C5A265;
}
.btn-batch-del {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.06);
  border-color: rgba(231, 76, 60, 0.2);
}
.btn-batch-del:hover {
  background: rgba(231, 76, 60, 0.12);
  border-color: #e74c3c;
}
.btn-add-item {
  padding: 8px 18px;
  background: rgba(197, 162, 101, 0.1);
  color: #C5A265;
  border: 1px solid rgba(197, 162, 101, 0.3);
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.btn-add-item:hover {
  background: rgba(197, 162, 101, 0.2);
  border-color: #C5A265;
}

.btn-dup-item {
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  color: #bbb;
  cursor: pointer;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.btn-dup-item:hover {
  background: rgba(197, 162, 101, 0.08);
  color: #C5A265;
}
.btn-remove-item {
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  color: #ccc;
  cursor: pointer;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.btn-remove-item:hover {
  background: rgba(231, 76, 60, 0.08);
  color: #e74c3c;
}

.empty-items-edit {
  text-align: center;
  padding: 44px 0;
  color: #ccc;
  font-size: 15px;
  border: 1px dashed #e8e8e8;
  border-radius: 8px;
  margin-top: 8px;
}

/* ===== Formula Parameters Sub-row ===== */
.formula-params-row td {
  padding: 0 !important;
  border: none !important;
}
.formula-params-cell {
  background: #fafbfc;
}
.formula-params-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px 8px 44px;
  flex-wrap: wrap;
}
.formula-label {
  font-size: 12px;
  color: #888;
  font-weight: 500;
  white-space: nowrap;
}
.formula-param-items {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.formula-param-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.formula-param-item label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}
.formula-result {
  font-size: 13px;
  font-weight: 700;
  color: #C5A265;
  margin-left: auto;
  white-space: nowrap;
}

/* ===== Element Plus Overrides ===== */
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
  padding: 20px 32px;
  margin-right: 0;
}
:deep(.el-dialog__title) {
  font-size: 19px;
  font-weight: 600;
  color: #1a1a1a;
}
:deep(.el-dialog__body) {
  padding: 32px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}
:deep(.el-dialog__footer) {
  border-top: 1px solid #f0f0f0;
  padding: 18px 32px;
}
:deep(.el-form-item__label) {
  font-size: 15px;
  color: #555;
}
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 6px;
}
:deep(.el-input__inner) {
  font-size: 14px;
}
:deep(.el-textarea__inner) {
  font-size: 14px;
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
  display: flex;
  flex-direction: column;
}
:deep(.el-date-editor) {
  --el-date-editor-width: 280px;
}
:deep(.el-dropdown-menu__item.is-disabled) {
  opacity: 0.4;
}

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
    flex: 1 1 33%;
    min-width: 120px;
  }
  .form-row-3,
  .form-row-2 {
    grid-template-columns: 1fr;
  }
  .span-2 {
    grid-column: span 1;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* Edit dialog fullscreen */
.edit-dialog {
  max-width: none;
  margin-top: 2vh !important;
}
.edit-dialog .el-dialog__header {
  padding: 12px 24px 8px;
}
.edit-dialog .el-dialog__body {
  padding: 8px 24px 16px;
  max-height: calc(98vh - 60px);
  overflow-y: auto;
}

/* Hide dropdown arrow on productType/lockPosition/color/formula selects */
.items-edit-table .col-type .el-input__suffix,
.items-edit-table .col-lock .el-input__suffix,
.items-edit-table .col-color .el-input__suffix,
.items-edit-table .col-formula .el-input__suffix,
.items-edit-table .col-type .el-select__caret,
.items-edit-table .col-lock .el-select__caret,
.items-edit-table .col-color .el-select__caret,
.items-edit-table .col-formula .el-select__caret,
.items-edit-table .col-type .el-select__suffix,
.items-edit-table .col-lock .el-select__suffix,
.items-edit-table .col-color .el-select__suffix,
.items-edit-table .col-formula .el-select__suffix {
  display: none !important;
  width: 0 !important;
  overflow: hidden !important;
}
.items-edit-table .col-type .el-input__wrapper,
.items-edit-table .col-lock .el-input__wrapper,
.items-edit-table .col-color .el-input__wrapper,
.items-edit-table .col-formula .el-input__wrapper {
  padding-right: 12px !important;
}
.items-edit-table td {
  position: relative;
  overflow: visible;
  height: 36px;
  box-sizing: border-box;
}
/* expand on focus: el-select columns (color, type, lock, commodity) */
.items-edit-table td:focus-within {
  position: relative;
  z-index: 200;
  overflow: visible !important;
}
.items-edit-table td:focus-within .el-select {
  position: absolute;
  left: 0;
  top: 0;
  width: 200px;
  z-index: 200;
}
.items-edit-table td:focus-within .el-select .el-input__wra
pper {
  flex: none !important;
  width: 100% !important;
}
.items-edit-table td:focus-within .el-select .el-input__inner {
  width: 100% !important;
}
.items-edit-table td:focus-within .el-select .el-input__suffix {
  display: flex;
}
/* expand on focus: plain inputs (num-input, remark-input) */
.items-edit-table td:focus-within .num-input,
.items-edit-table td:focus-within .remark-input {
  position: absolute;
  left: 0;
  top: 0;
  min-width: 200px;
  width: 200px;
  z-index: 200;
}

/* ===== Sortable columns ===== */
.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}
.data-table th.sortable:hover {
  color: #C5A265;
}

/* Clickable cost display in items edit table */
.items-edit-table .cost-clickable {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  color: #C5A265;
  font-weight: 600;
  font-size: 14px;
  min-height: 28px;
  width: 100%;
  box-sizing: border-box;
}
.items-edit-table .cost-clickable:hover {
  background: #f5f0e8;
}
.items-edit-table .cost-clickable:focus-visible {
  outline: 2px solid #C5A265;
  outline-offset: 1px;
}
</style>

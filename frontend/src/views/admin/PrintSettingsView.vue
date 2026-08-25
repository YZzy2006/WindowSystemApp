<template>
  <div class="print-settings-page">
    <div class="page-header">
      <h2>打印设置</h2>
      <el-button type="primary" :loading="saving" @click="handleSaveAll">{{ activeTab.startsWith('afterSale') ? '保存售后单设置' : '保存订单设置' }}</el-button>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- Tab 1: 订单打印设置 -->
      <el-tab-pane label="订单打印" name="settings">
        <!-- Preset Bar -->
        <div class="preset-bar">
          <span class="preset-label">快速预设</span>
          <el-select v-model="currentPreset" size="small" placeholder="选择预设" @change="applyPreset" style="width: 160px;">
            <el-option v-for="p in allPresets" :key="p.name" :label="p.name" :value="p.name">
              <span>{{ p.name }}</span>
              <el-tag v-if="p.builtIn" size="small" type="info" style="margin-left:6px;transform:scale(.85);">内置</el-tag>
            </el-option>
          </el-select>
          <el-button size="small" @click="showSavePreset = true">
            <el-icon style="margin-right:4px;"><Plus /></el-icon>保存为预设
          </el-button>
          <el-button size="small" :disabled="isCurrentBuiltIn" @click="deletePreset" type="danger" plain>
            <el-icon style="margin-right:4px;"><Delete /></el-icon>删除预设
          </el-button>
          <el-button size="small" v-if="isCurrentBuiltIn" @click="applyPreset(currentPreset)" type="warning" plain>
            重置
          </el-button>
        </div>

        <!-- 针式/复写纸使用提示 -->
        <div v-if="config.pageHeightMm < 297 || (config.pageWidthMm || 210) !== 210" class="pin-hint">
          💡 针式打印机 + 两联复写纸：① 量一下复写纸实际长宽，填到「纸张宽度」和「页面高度」；② 在 Windows「打印机和扫描仪 → 打印服务器属性」新建自定义纸张（宽 = 纸张宽度、高 = 页面高度）；③ 打印时选这台针式打印机，缩放选「实际大小 / 100%」；④ 「上边距」用于对齐复写纸起印位置，调一次即可。
        </div>

        <div class="settings-grid">
          <!-- Page Setup -->
          <div class="section">
            <div class="section-title">页面设置</div>
            <div class="form-row">
              <label>纸张方向</label>
              <el-select v-model="config.orientation" size="small">
                <el-option label="纵向" value="portrait" />
                <el-option label="横向" value="landscape" />
              </el-select>
            </div>
            <div class="form-row">
              <label>纸张宽度(mm)</label>
              <el-input-number v-model="config.pageWidthMm" :min="50" :max="500" size="small" />
              <span class="mm-hint">A4=210；复写纸按实际量</span>
            </div>
            <div class="form-row">
              <label>页面高度(mm)</label>
              <el-input-number v-model="config.pageHeightMm" :min="60" :max="500" size="small" />
              <span class="mm-hint">A4=297；复写纸按实际量</span>
            </div>
            <div class="form-row">
              <label>上边距(mm)</label>
              <el-input-number v-model="config.marginTop" :min="0" :max="80" size="small" />
            </div>
            <div class="form-row">
              <label>下边距(mm)</label>
              <el-input-number v-model="config.marginBottom" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>左边距(mm)</label>
              <el-input-number v-model="config.marginLeft" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>右边距(mm)</label>
              <el-input-number v-model="config.marginRight" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>正文字号(px)</label>
              <el-input-number v-model="config.fontSize" :min="8" :max="18" size="small" />
            </div>
            <div class="form-row line-width-row">
              <label>线条粗细</label>
              <div class="lw-group">
                <span v-for="(label, key) in LINE_WIDTH_LABELS" :key="key" class="lw-item">
                  <span class="lw-label">{{ label }}</span>
                  <el-select v-model="config.lineWidths[key]" size="small" style="width:70px">
                    <el-option label="细" :value="1" />
                    <el-option label="中" :value="2" />
                    <el-option label="粗" :value="3" />
                  </el-select>
                </span>
              </div>
            </div>
          </div>

          <!-- Section Font Styles -->
          <div class="section">
            <div class="section-title">字体样式</div>
            <div v-for="(label, key) in SECTION_LABELS" :key="key" class="form-row font-style-row">
              <label>{{ label }}</label>
              <el-switch v-model="config.sectionStyles[key].bold" size="small" active-text="粗" inactive-text="" />
              <div class="size-controls">
                <button class="size-btn" :disabled="config.sectionStyles[key].sizeOffset <= -7" @click="config.sectionStyles[key].sizeOffset--">−</button>
                <span class="size-display">{{ config.fontSize + config.sectionStyles[key].sizeOffset }}px</span>
                <button class="size-btn" :disabled="config.sectionStyles[key].sizeOffset >= 7" @click="config.sectionStyles[key].sizeOffset++">+</button>
              </div>
              <el-radio-group v-if="key === 'info'" v-model="config.sectionStyles[key].align" size="small">
                <el-radio-button value="left">左</el-radio-button>
                <el-radio-button value="center">中</el-radio-button>
                <el-radio-button value="right">右</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- Company Info -->
          <div class="section">
            <div class="section-title">公司信息</div>
            <div class="form-row">
              <label>公司名称</label>
              <el-input v-model="config.companyName" size="small" placeholder="顺居门业" />
            </div>
            <div class="form-row">
              <label>制单人</label>
              <el-input v-model="config.creatorName" size="small" placeholder="冯伟明" />
            </div>
            <div class="form-row">
              <label>联系电话</label>
              <el-input v-model="config.creatorPhone" size="small" placeholder="18948846839" />
            </div>
            <div class="form-row">
              <label>公司地址</label>
              <el-input v-model="config.creatorAddress" size="small" placeholder="乳源县二九一物流仓库" />
            </div>
          </div>

          <!-- Column Visibility -->
          <div class="section">
            <div class="section-title">显示列</div>
            <div class="col-list">
              <div class="col-item" v-for="col in config.columns" :key="col.key">
                <el-checkbox v-model="col.visible">{{ col.label }}</el-checkbox>
              </div>
            </div>
          </div>

          <!-- Summary & Footer -->
          <div class="section">
            <div class="section-title">其他选项</div>
            <div class="form-row">
              <el-checkbox v-model="config.showSummary">显示细目总结</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="config.showPaymentTerms">显示付款条款</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="config.showTotalRow">显示合计行</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="config.showFooter">显示页脚签名</el-checkbox>
            </div>
            <div class="form-row">
              <label>付款条款文本</label>
              <el-input v-model="config.paymentTermsText" type="textarea" :rows="3" size="small" />
            </div>
            <div class="form-row">
              <el-checkbox v-model="config.showInstallationTerms">显示安装条款</el-checkbox>
            </div>
            <div class="form-row">
              <label>安装条款文本</label>
              <el-input v-model="config.installationTermsText" type="textarea" :rows="3" size="small" placeholder="留空则使用系统设置中的内容" />
            </div>
            <div class="form-row">
              <el-checkbox v-model="config.showWarrantyTerms">显示保修条款</el-checkbox>
            </div>
            <div class="form-row">
              <label>保修条款文本</label>
              <el-input v-model="config.warrantyTermsText" type="textarea" :rows="3" size="small" placeholder="留空则使用系统设置中的内容" />
            </div>
            <div class="form-row">
              <label>页脚附加文字</label>
              <el-input v-model="config.footerText" size="small" placeholder="备注信息" />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 2: Preview -->
      <el-tab-pane label="打印预览" name="preview">
        <div v-if="activeTab === 'preview'" class="preview-toolbar">
          <span class="preview-toolbar-hint">💡 点击预览中的区域可选中，直接调字号/粗体；调整完可保存为预设</span>
          <el-button size="small" type="primary" plain @click="showSavePreset = true">
            <el-icon style="margin-right:4px;"><Plus /></el-icon>保存为预设
          </el-button>
        </div>
        <div v-if="activeTab === 'preview'" class="preview-container" :style="previewContainerStyle">
          <div class="preview-page" :style="previewPageStyle">
            <!-- Header -->
            <div class="p-header section-clickable" :class="{ 'section-selected': selectedSection === 'header' }" :style="previewSectionBold('header')" @click.stop="selectSection('header')">
              <div class="p-title" :style="previewSectionFontSize('header', 20)">{{ previewTitle }}</div>
              <div class="p-company" :style="previewSectionFontSize('header', 12)">{{ config.companyName || '顺居门业' }}</div>
            </div>

            <!-- Order Info -->
            <div class="p-info section-clickable" :class="{ 'section-selected': selectedSection === 'info' }" :style="previewSectionBold('info')" @click.stop="selectSection('info')">
              <div class="p-info-left" :style="{ textAlign: config.sectionStyles.info.align || 'left' }">
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">订单编号</span><span class="p-value mono" :style="previewSectionFontSize('info', 10)">SJ2026050001</span></div>
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">开单日期</span><span class="p-value" :style="previewSectionFontSize('info', 11)">2026/05/19</span></div>
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">客户</span><span class="p-value" :style="previewSectionFontSize('info', 11)">张三</span></div>
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">联系电话</span><span class="p-value" :style="previewSectionFontSize('info', 11)">13800138000</span></div>
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">安装地址</span><span class="p-value" :style="previewSectionFontSize('info', 11)">乳源县某小区</span></div>
              </div>
              <div class="p-info-right" :style="{ textAlign: config.sectionStyles.info.align || 'left' }">
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">制单人</span><span class="p-value" :style="previewSectionFontSize('info', 11)">{{ config.creatorName || '冯伟明' }}</span></div>
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">联系电话</span><span class="p-value" :style="previewSectionFontSize('info', 11)">{{ config.creatorPhone || '18948846839' }}</span></div>
                <div class="p-info-row"><span class="p-label" :style="previewSectionFontSize('info', 10)">公司地址</span><span class="p-value" :style="previewSectionFontSize('info', 11)">{{ config.creatorAddress || '乳源县二九一物流仓库' }}</span></div>
              </div>
            </div>

            <!-- Table -->
            <div class="p-table-title section-clickable" :class="{ 'section-selected': selectedSection === 'tableHead' }" :style="[previewSectionBold('tableHead'), previewSectionFontSize('tableHead', 11)]" @click.stop="selectSection('tableHead')">商品明细</div>
            <table class="p-table">
              <thead class="section-clickable" :class="{ 'section-selected': selectedSection === 'tableHead' }" :style="previewSectionBold('tableHead')" @click.stop="selectSection('tableHead')">
                <tr>
                  <th rowspan="2" class="c-no" :style="[previewSectionFontSize('tableHead', 9), { width: '36px' }]">序号</th>
                  <th v-for="col in columnsBeforeSize" :key="col.key" rowspan="2" :class="'c-' + col.key" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth(col.key) + 'px' }]">
                    {{ col.label }}
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, col.key)"></div>
                  </th>
                  <th v-if="sizeGroupVisible" :colspan="sizeGroupCount" class="c-size-group" :style="previewSectionFontSize('tableHead', 9)">尺寸 <span class="unit-hint">mm</span></th>
                  <th v-for="col in columnsAfterSizeNonRemark" :key="col.key" rowspan="2" :class="'c-' + col.key" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth(col.key) + 'px' }]">
                    {{ col.label }}
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, col.key)"></div>
                  </th>
                  <th rowspan="2" class="c-amount" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth('amount') + 'px' }]">
                    金额
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, 'amount')"></div>
                  </th>
                  <th v-if="remarkColVisible" rowspan="2" class="c-remark" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth('remark') + 'px' }]">
                    备注
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, 'remark')"></div>
                  </th>
                </tr>
                <tr v-if="sizeGroupVisible">
                  <th v-if="sizeColVisible('width')" class="c-width" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth('width') + 'px' }]">
                    宽
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, 'width')"></div>
                  </th>
                  <th v-if="sizeColVisible('height')" class="c-height" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth('height') + 'px' }]">
                    高
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, 'height')"></div>
                  </th>
                  <th v-if="sizeColVisible('wallThickness')" class="c-wallThickness" :style="[previewSectionFontSize('tableHead', 9), { width: colWidth('wallThickness') + 'px' }]">
                    墙厚
                    <div class="col-resize-handle" @mousedown.prevent="startResize($event, 'wallThickness')"></div>
                  </th>
                </tr>
              </thead>
              <tbody class="section-clickable" :class="{ 'section-selected': selectedSection === 'tableBody' }" :style="[previewSectionBold('tableBody'), previewSectionFontSize('tableBody', 10)]" @click.stop="selectSection('tableBody')">
                <tr v-for="(item, idx) in previewItems" :key="idx">
                  <td class="c-no">{{ idx + 1 }}</td>
                  <td v-for="col in columnsBeforeSize" :key="col.key" :class="'c-' + col.key">{{ item[col.key] ?? '' }}</td>
                  <td v-if="sizeColVisible('width')" class="c-width">{{ item.width }}</td>
                  <td v-if="sizeColVisible('height')" class="c-height">{{ item.height }}</td>
                  <td v-if="sizeColVisible('wallThickness')" class="c-wallThickness">{{ item.wallThickness }}</td>
                  <td v-for="col in columnsAfterSizeNonRemark" :key="col.key" :class="'c-' + col.key">{{ item[col.key] ?? '' }}</td>
                  <td class="c-amount">{{ item.amount }}</td>
                  <td v-if="remarkColVisible" class="c-remark">{{ item.remark ?? '' }}</td>
                </tr>
              </tbody>
              <tfoot>
                <template v-if="config.showTotalRow">
                  <tr class="type-summary-row">
                    <td :colspan="previewTotalColspan" class="type-summary-cell">
                      <div class="type-summary-grid">
                        <div class="type-summary-item">断桥铝窗：2 樘 / 9.0 ㎡</div>
                        <div class="type-summary-item">防盗门：2 樘</div>
                      </div>
                    </td>
                  </tr>
                </template>
                <tr>
                  <td :colspan="previewTotalColspan" class="remark-cell">备注：请于月底前完成安装</td>
                </tr>
              </tfoot>
            </table>

            <!-- Summary (financial) -->
            <div class="p-summary section-clickable" :class="{ 'section-selected': selectedSection === 'summary' }" v-if="config.showSummary" :style="previewSectionBold('summary')" @click.stop="selectSection('summary')">
              <div class="p-summary-grid">
                <div class="p-summary-group">
                  <div class="p-summary-item" :style="previewSectionFontSize('summary', 12)">
                    <span class="p-summary-label">合计金额</span>
                    <span class="p-summary-value strong">¥35,800.00</span>
                  </div>
                  <div class="p-summary-item" :style="previewSectionFontSize('summary', 12)">
                    <span class="p-summary-label">大写</span>
                    <span class="p-summary-value chinese">叁万伍仟捌佰元整</span>
                  </div>
                </div>
                <div class="p-summary-group">
                  <div class="p-summary-item" :style="previewSectionFontSize('summary', 12)">
                    <span class="p-summary-label">已收定金</span>
                    <span class="p-summary-value">¥10,000.00</span>
                  </div>
                  <div class="p-summary-item" :style="previewSectionFontSize('summary', 12)">
                    <span class="p-summary-label">大写</span>
                    <span class="p-summary-value chinese">壹万元整</span>
                  </div>
                </div>
                <div class="p-summary-group">
                  <div class="p-summary-item" :style="previewSectionFontSize('summary', 12)">
                    <span class="p-summary-label">未收余款</span>
                    <span class="p-summary-value strong red">¥25,800.00</span>
                  </div>
                  <div class="p-summary-item" :style="previewSectionFontSize('summary', 12)">
                    <span class="p-summary-label">大写</span>
                    <span class="p-summary-value chinese">贰万伍仟捌佰元整</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Payment Terms -->
            <div class="p-terms section-clickable" :class="{ 'section-selected': selectedSection === 'terms' }" v-if="config.showPaymentTerms" :style="previewSectionBold('terms')" @click.stop="selectSection('terms')">
              <div class="p-terms-title" :style="previewSectionFontSize('terms', 10)">付款条款</div>
              <div class="p-terms-body" :style="previewSectionFontSize('terms', 9)">{{ config.paymentTermsText || '客户订货需支付总金额百分之30，安装完成后需付尾款百分之70，剩余货款需在一个月内付清。' }}</div>
            </div>

            <!-- Installation Terms -->
            <div class="p-terms section-clickable" :class="{ 'section-selected': selectedSection === 'terms' }" v-if="config.showInstallationTerms && (config.installationTermsText || sysConfigTerms.installation)" :style="previewSectionBold('terms')" @click.stop="selectSection('terms')">
              <div class="p-terms-title" :style="previewSectionFontSize('terms', 10)">安装条款</div>
              <div class="p-terms-body" :style="previewSectionFontSize('terms', 9)">{{ config.installationTermsText || sysConfigTerms.installation }}</div>
            </div>

            <!-- Warranty Terms -->
            <div class="p-terms section-clickable" :class="{ 'section-selected': selectedSection === 'terms' }" v-if="config.showWarrantyTerms && (config.warrantyTermsText || sysConfigTerms.warranty)" :style="previewSectionBold('terms')" @click.stop="selectSection('terms')">
              <div class="p-terms-title" :style="previewSectionFontSize('terms', 10)">保修条款</div>
              <div class="p-terms-body" :style="previewSectionFontSize('terms', 9)">{{ config.warrantyTermsText || sysConfigTerms.warranty }}</div>
            </div>

            <!-- Footer -->
            <div class="p-footer section-clickable" :class="{ 'section-selected': selectedSection === 'footer' }" v-if="config.showFooter" :style="previewSectionBold('footer')" @click.stop="selectSection('footer')">
              <div class="p-footer-row" :style="previewSectionFontSize('footer', 11)">
                <span>客户签名：______________</span>
                <span>制单人：______________</span>
                <span>审核人：______________</span>
              </div>
              <div class="p-footer-date" v-if="config.footerText" :style="previewSectionFontSize('footer', 9)">{{ config.footerText }}</div>
              <div class="p-footer-date" :style="previewSectionFontSize('footer', 9)">打印日期：2026/05/19</div>
            </div>
          </div>
        </div>

        <!-- 选中区域的浮动调节条：固定视口右下角，始终可见 -->
        <div v-if="selectedSection" class="section-adjust-bar">
          <span class="bar-name">{{ SECTION_LABELS[selectedSection] }}</span>
          <el-switch v-model="config.sectionStyles[selectedSection].bold" size="small" active-text="粗" inactive-text="" />
          <div class="bar-size">
            <button class="size-btn" :disabled="config.sectionStyles[selectedSection].sizeOffset <= -7" @click="config.sectionStyles[selectedSection].sizeOffset--">−</button>
            <span class="size-display">{{ config.fontSize + config.sectionStyles[selectedSection].sizeOffset }}px</span>
            <button class="size-btn" :disabled="config.sectionStyles[selectedSection].sizeOffset >= 7" @click="config.sectionStyles[selectedSection].sizeOffset++">+</button>
          </div>
          <span class="lw-inline">
            <span class="lw-mini">线</span>
            <el-select v-model="config.lineWidths[sectionLwKey]" size="small" style="width:64px">
              <el-option label="细" :value="1" />
              <el-option label="中" :value="2" />
              <el-option label="粗" :value="3" />
            </el-select>
          </span>
          <el-radio-group v-if="selectedSection === 'info'" v-model="config.sectionStyles.info.align" size="small">
            <el-radio-button value="left">左</el-radio-button>
            <el-radio-button value="center">中</el-radio-button>
            <el-radio-button value="right">右</el-radio-button>
          </el-radio-group>
          <el-button v-if="currentEditFields.length" size="small" type="warning" plain @click="openEditText">✏️ 编辑文字</el-button>
          <button class="bar-close" @click="selectedSection = ''">✕</button>
        </div>
      </el-tab-pane>

      <!-- Tab 3: 售后单打印设置 -->
      <el-tab-pane label="售后单" name="afterSaleSettings">
        <!-- AS Preset Bar -->
        <div class="preset-bar">
          <span class="preset-label">快速预设</span>
          <el-select v-model="asCurrentPreset" size="small" placeholder="选择预设" @change="applyAsPreset" style="width: 160px;">
            <el-option v-for="p in allAsPresets" :key="p.name" :label="p.name" :value="p.name">
              <span>{{ p.name }}</span>
              <el-tag v-if="p.builtIn" size="small" type="info" style="margin-left:6px;transform:scale(.85);">内置</el-tag>
            </el-option>
          </el-select>
          <el-button size="small" @click="showAsSavePreset = true">
            <el-icon style="margin-right:4px;"><Plus /></el-icon>保存为预设
          </el-button>
          <el-button size="small" :disabled="isAsCurrentBuiltIn" @click="deleteAsPreset" type="danger" plain>
            <el-icon style="margin-right:4px;"><Delete /></el-icon>删除预设
          </el-button>
        </div>

        <!-- AS Save Preset Dialog -->
        <div class="save-preset-dialog" v-if="showAsSavePreset">
          <div class="save-preset-inner">
            <label>预设名称</label>
            <input class="preset-name-input" v-model="newAsPresetName" placeholder="如：标准售后单" @keyup.enter="saveAsPreset" />
            <el-button type="primary" size="small" @click="saveAsPreset" :disabled="!newAsPresetName.trim()">保存</el-button>
            <el-button size="small" @click="showAsSavePreset = false">取消</el-button>
          </div>
        </div>

        <!-- AS 针式/复写纸使用提示 -->
        <div v-if="asConfig.pageHeightMm < 297 || (asConfig.pageWidthMm || 210) !== 210" class="pin-hint">
          💡 针式打印机 + 两联复写纸：① 量一下复写纸实际长宽，填到「纸张宽度」和「页面高度」；② 在 Windows「打印机和扫描仪 → 打印服务器属性」新建自定义纸张（宽 = 纸张宽度、高 = 页面高度）；③ 打印时选这台针式打印机，缩放选「实际大小 / 100%」；④ 「上边距」用于对齐复写纸起印位置，调一次即可。
        </div>

        <div class="settings-grid">
          <!-- AS Page Setup -->
          <div class="section">
            <div class="section-title">页面设置</div>
            <div class="form-row">
              <label>纸张方向</label>
              <el-select v-model="asConfig.orientation" size="small">
                <el-option label="纵向" value="portrait" />
                <el-option label="横向" value="landscape" />
              </el-select>
            </div>
            <div class="form-row">
              <label>纸张宽度(mm)</label>
              <el-input-number v-model="asConfig.pageWidthMm" :min="50" :max="500" size="small" />
              <span class="mm-hint">A4=210；复写纸按实际量</span>
            </div>
            <div class="form-row">
              <label>页面高度(mm)</label>
              <el-input-number v-model="asConfig.pageHeightMm" :min="60" :max="500" size="small" />
              <span class="mm-hint">A4=297；复写纸按实际量</span>
            </div>
            <div class="form-row">
              <label>上边距(mm)</label>
              <el-input-number v-model="asConfig.marginTop" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>下边距(mm)</label>
              <el-input-number v-model="asConfig.marginBottom" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>左边距(mm)</label>
              <el-input-number v-model="asConfig.marginLeft" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>右边距(mm)</label>
              <el-input-number v-model="asConfig.marginRight" :min="0" :max="50" size="small" />
            </div>
            <div class="form-row">
              <label>正文字号(px)</label>
              <el-input-number v-model="asConfig.fontSize" :min="8" :max="18" size="small" />
            </div>
          </div>

          <!-- AS Font Styles -->
          <div class="section">
            <div class="section-title">字体样式</div>
            <div v-for="(label, key) in AS_SECTION_LABELS" :key="key" class="form-row font-style-row">
              <label>{{ label }}</label>
              <el-switch v-model="asConfig.sectionStyles[key].bold" size="small" active-text="粗" inactive-text="" />
              <div class="size-controls">
                <button class="size-btn" :disabled="asConfig.sectionStyles[key].sizeOffset <= -7" @click="asConfig.sectionStyles[key].sizeOffset--">−</button>
                <span class="size-display">{{ asConfig.fontSize + asConfig.sectionStyles[key].sizeOffset }}px</span>
                <button class="size-btn" :disabled="asConfig.sectionStyles[key].sizeOffset >= 7" @click="asConfig.sectionStyles[key].sizeOffset++">+</button>
              </div>
            </div>
          </div>

          <!-- AS Company Info -->
          <div class="section">
            <div class="section-title">公司信息</div>
            <div class="form-row">
              <label>公司名称</label>
              <el-input v-model="asConfig.companyName" size="small" placeholder="顺居门业" />
            </div>
            <div class="form-row">
              <label>联系电话</label>
              <el-input v-model="asConfig.creatorPhone" size="small" placeholder="18948846839" />
            </div>
            <div class="form-row">
              <label>公司地址</label>
              <el-input v-model="asConfig.creatorAddress" size="small" placeholder="乳源县二九一物流仓库" />
            </div>
          </div>

          <!-- AS Display Options -->
          <div class="section">
            <div class="section-title">显示选项</div>
            <div class="form-row">
              <el-checkbox v-model="asConfig.showSignature">显示签名区</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="asConfig.showTechnician">显示师傅信息</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="asConfig.showSaleOrderNo">显示关联订单</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="asConfig.showProcessingInfo">显示处理信息</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="asConfig.showWarranty">显示质保信息</el-checkbox>
            </div>
            <div class="form-row">
              <el-checkbox v-model="asConfig.showSource">显示来源</el-checkbox>
            </div>
            <div class="form-row">
              <label>页脚附加文字</label>
              <el-input v-model="asConfig.footerText" size="small" placeholder="备注信息" />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 4: 售后单预览 -->
      <el-tab-pane label="售后单预览" name="afterSalePreview">
        <div v-if="activeTab === 'afterSalePreview'" class="preview-container" :style="asPreviewContainerStyle">
          <div class="as-preview-page" :style="asPreviewPageStyle">
            <!-- Header -->
            <div class="as-p-header" :style="asPreviewSectionBold('header')">
              <div class="as-p-title" :style="asPreviewSectionFontSize('header', 20)">顺居门业售后单</div>
              <div class="as-p-company" :style="asPreviewSectionFontSize('header', 12)">{{ asConfig.companyName || '顺居门业' }}</div>
            </div>

            <!-- Order Info -->
            <div class="as-p-info-section">
              <div class="as-p-info-row">
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">售后单号</span>
                  <span class="as-p-value mono" :style="asPreviewSectionFontSize('info', 10)">SH20260609001</span>
                </div>
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">创建日期</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">2026/06/09</span>
                </div>
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">状态</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">处理中</span>
                </div>
              </div>
              <div class="as-p-info-row">
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">客户</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">张三</span>
                </div>
                <div class="as-p-info-item" v-if="asConfig.showSaleOrderNo">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">关联订单</span>
                  <span class="as-p-value mono" :style="asPreviewSectionFontSize('info', 10)">XS2026050001</span>
                </div>
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">类型</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">质保维修</span>
                </div>
              </div>
              <div class="as-p-info-row">
                <div class="as-p-info-item" v-if="asConfig.showSource">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">来源</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">电话</span>
                </div>
                <div class="as-p-info-item" v-if="asConfig.showTechnician">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">师傅</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">李师傅</span>
                </div>
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">预约日期</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">2026/06/10</span>
                </div>
              </div>
            </div>

            <!-- Problem Description -->
            <div class="as-p-section">
              <div class="as-p-section-title" :style="[asPreviewSectionBold('section'), asPreviewSectionFontSize('section', 13)]">问题描述</div>
              <div class="as-p-section-body" :style="asPreviewSectionFontSize('section', 12)">阳台推拉窗滑轮损坏，窗户无法正常推拉，需要更换滑轮。</div>
            </div>

            <!-- Processing Info -->
            <div class="as-p-section" v-if="asConfig.showProcessingInfo">
              <div class="as-p-section-title" :style="[asPreviewSectionBold('section'), asPreviewSectionFontSize('section', 13)]">处理信息</div>
              <div class="as-p-info-row">
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">处理方式</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">更换部件</span>
                </div>
                <div class="as-p-info-item">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">费用</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">¥120.00</span>
                </div>
                <div class="as-p-info-item" v-if="asConfig.showWarranty">
                  <span class="as-p-label" :style="asPreviewSectionFontSize('info', 10)">质保期内</span>
                  <span class="as-p-value" :style="asPreviewSectionFontSize('info', 11)">是</span>
                </div>
              </div>
            </div>

            <!-- Signature Area -->
            <div class="as-p-signature" v-if="asConfig.showSignature">
              <div class="as-p-sig-item">
                <span class="as-p-sig-label">客户签名：</span>
                <span class="as-p-sig-line"></span>
              </div>
              <div class="as-p-sig-item">
                <span class="as-p-sig-label">师傅签名：</span>
                <span class="as-p-sig-line"></span>
              </div>
              <div class="as-p-sig-item">
                <span class="as-p-sig-label">日期：</span>
                <span class="as-p-sig-line"></span>
              </div>
            </div>

            <!-- Footer -->
            <div class="as-p-footer" :style="asPreviewSectionBold('footer')">
              <span :style="asPreviewSectionFontSize('footer', 10)">顺居门业 · 售后服务单</span>
              <span :style="asPreviewSectionFontSize('footer', 10)">打印日期：2026/06/09</span>
            </div>
            <div class="as-p-footer-text" v-if="asConfig.footerText" :style="asPreviewSectionFontSize('footer', 9)">{{ asConfig.footerText }}</div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Save Preset Dialog（浮层，任意 Tab 可用：预览页调整后直接保存预设） -->
    <div class="save-preset-dialog" v-if="showSavePreset">
      <div class="save-preset-inner">
        <label>预设名称</label>
        <input class="preset-name-input" v-model="newPresetName" placeholder="如：A4简洁版" @keyup.enter="savePreset" />
        <el-button type="primary" size="small" @click="savePreset" :disabled="!newPresetName.trim()">保存</el-button>
        <el-button size="small" @click="showSavePreset = false">取消</el-button>
      </div>
    </div>

    <!-- 编辑文字浮层（预览页选中区域后直接改文案，同步到配置） -->
    <el-dialog v-model="editTextVisible" title="编辑文字" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item v-for="f in currentEditFields" :key="f.key" :label="f.label">
          <el-input v-model="editTextForm[f.key]" :type="f.long ? 'textarea' : 'text'" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editTextVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEditText">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getPrintSetting, savePrintSetting, getSystemSettings } from '@/api/admin'
import { usePrintConfig, invalidatePrintConfig } from '@/composables/usePrintConfig'
import {
  ALL_COLUMNS, COMPACT_COLUMNS, DEFAULT_SECTION_STYLES, DEFAULT_COLUMN_WIDTHS,
  DEFAULT_CONFIG, makePresetConfig, BUILT_IN_PRESETS,
} from '@/composables/printPresets'
const { reloadPrintConfig } = usePrintConfig()

const saving = ref(false)
const activeTab = ref('settings')
const showSavePreset = ref(false)
const newPresetName = ref('')
const currentPreset = ref('标准')

// All columns default (visible: true)

const customPresets = ref([])

const allPresets = computed(() => [...BUILT_IN_PRESETS, ...customPresets.value])

const isCurrentBuiltIn = computed(() => BUILT_IN_PRESETS.some(p => p.name === currentPreset.value))

const config = reactive({
  ...DEFAULT_CONFIG,
  sectionStyles: JSON.parse(JSON.stringify(DEFAULT_SECTION_STYLES)),
  columnWidths: { ...DEFAULT_COLUMN_WIDTHS },
  lineWidths: { header: 2, info: 2, table: 2, summary: 2, terms: 2, footer: 2 },
})

const LINE_WIDTH_LABELS = { header: '标题', info: '信息', table: '表格', summary: '汇总', terms: '条款', footer: '页脚' }

const SECTION_LABELS = {
  header: '标题',
  info: '订单信息',
  tableHead: '表头',
  tableBody: '表体',
  summary: '汇总',
  terms: '条款',
  footer: '页脚',
}

// Load terms from sys_config for preview fallback (same source as PrintOrder)
const sysConfigTerms = ref({ installation: '', warranty: '' })

const visibleColumns = computed(() => config.columns.filter(c => c.visible))

// Split columns: size group vs others
const SIZE_KEYS = ['width', 'height', 'wallThickness']
const SIZE_INSERT_AFTER = 'color' // size group appears after this column
const nonSizeColumns = computed(() => visibleColumns.value.filter(c => !SIZE_KEYS.includes(c.key)))
const columnsBeforeSize = computed(() => {
  const idx = nonSizeColumns.value.findIndex(c => c.key === SIZE_INSERT_AFTER)
  return idx >= 0 ? nonSizeColumns.value.slice(0, idx + 1) : nonSizeColumns.value
})
const columnsAfterSize = computed(() => {
  const idx = nonSizeColumns.value.findIndex(c => c.key === SIZE_INSERT_AFTER)
  return idx >= 0 ? nonSizeColumns.value.slice(idx + 1) : []
})
const columnsAfterSizeNonRemark = computed(() => columnsAfterSize.value.filter(c => c.key !== 'remark'))
const remarkColVisible = computed(() => config.columns.find(c => c.key === 'remark')?.visible ?? false)
const sizeColVisible = (key) => config.columns.find(c => c.key === key)?.visible ?? false
const sizeGroupVisible = computed(() => sizeColVisible('width') || sizeColVisible('height') || sizeColVisible('wallThickness'))
const sizeGroupCount = computed(() => {
  let n = 0
  if (sizeColVisible('width')) n++
  if (sizeColVisible('height')) n++
  if (sizeColVisible('wallThickness')) n++
  return n
})
const previewTotalColspan = computed(() => {
  let cols = nonSizeColumns.value.length + 1 // non-size cols + 序号
  if (sizeGroupVisible.value) cols += sizeGroupCount.value
  cols++ // 金额
  return cols
})

const previewItems = [
  { productName: '断桥铝推拉窗', color: '氟碳灰', width: 1500, height: 1800, wallThickness: 108, glassType: '中空钢化', lockPosition: '中', diaojiao: 30, doorCount: 2, fangCount: 5.4, productType: '断桥铝窗', unitPrice: 680, amount: 13600 },
  { productName: '断桥铝平开窗', color: '氟碳灰', width: 1200, height: 1500, wallThickness: 108, glassType: '中空钢化', lockPosition: '右', diaojiao: 25, doorCount: 2, fangCount: 3.6, productType: '断桥铝窗', unitPrice: 580, amount: 11600 },
  { productName: '甲级防盗门', color: '红木纹', width: 960, height: 2050, wallThickness: '', glassType: '', lockPosition: '', diaojiao: '', doorCount: 2, fangCount: '', productType: '防盗门', unitPrice: 5300, amount: 10600 },
]

const previewTitle = computed(() => '顺居门窗客户销售单')

function previewSectionBold(key) {
  const s = config.sectionStyles?.[key]
  return s?.bold ? { fontWeight: 'bold' } : {}
}
function previewSectionFontSize(key, basePx) {
  const s = config.sectionStyles?.[key]
  const offset = s?.sizeOffset || 0
  if (offset === 0) return {}
  return { fontSize: (basePx + offset) + 'px' }
}

// 预览区域点击选中 → 浮动条调节该部分字体/线条（复用 sectionStyles / lineWidths）
const selectedSection = ref('')
function selectSection(key) {
  selectedSection.value = selectedSection.value === key ? '' : key
}
// 预览分区 → lineWidths 键（表头/表体共用「表格」线）
const SECTION_TO_LW = { header: 'header', info: 'info', tableHead: 'table', tableBody: 'table', summary: 'summary', terms: 'terms', footer: 'footer' }
const sectionLwKey = computed(() => SECTION_TO_LW[selectedSection.value] || selectedSection.value)

// 各区域可编辑的文字字段（预览页直接改文案，同步到 config）
const SECTION_EDIT_FIELDS = {
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
const currentEditFields = computed(() => SECTION_EDIT_FIELDS[selectedSection.value] || [])
const editTextVisible = ref(false)
const editTextForm = ref({})
function openEditText() {
  editTextForm.value = {}
  for (const f of currentEditFields.value) editTextForm.value[f.key] = config[f.key] || ''
  editTextVisible.value = true
}
function confirmEditText() {
  for (const f of currentEditFields.value) {
    config[f.key] = (editTextForm.value[f.key] || '').trim()
  }
  editTextVisible.value = false
  ElMessage.success('文字已更新')
}

// Column width helper
function colWidth(key) {
  return config.columnWidths?.[key] ?? DEFAULT_COLUMN_WIDTHS[key] ?? 60
}

// Column resize drag logic
let resizeState = null
function startResize(e, colKey) {
  resizeState = { colKey, startX: e.clientX, startWidth: colWidth(colKey) }
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
function onResize(e) {
  if (!resizeState) return
  const delta = e.clientX - resizeState.startX
  const newWidth = Math.max(20, resizeState.startWidth + delta)
  config.columnWidths[resizeState.colKey] = newWidth
}
function stopResize() {
  resizeState = null
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

onBeforeUnmount(() => {
  if (resizeState) stopResize()
})


const previewContainerStyle = computed(() => ({
  background: '#e8e8e8',
  padding: '20px',
  borderRadius: '6px',
  overflow: 'auto',
  maxHeight: 'calc(100vh - 140px)',
}))

const previewPageStyle = computed(() => {
  const portrait = config.orientation === 'portrait'
  const w = config.pageWidthMm || 210
  const h = (config.pageHeightMm || 297)
  const lw = config.lineWidths || {}
  const lwGet = (k) => (lw[k] ?? 2) + 'px'
  return {
    background: '#fff',
    width: portrait ? `${w}mm` : '297mm',
    minHeight: portrait ? `${h}mm` : '210mm',
    margin: '0 auto',
    padding: `${config.marginTop}mm ${config.marginRight}mm ${config.marginBottom}mm ${config.marginLeft}mm`,
    fontSize: config.fontSize + 'px',
    '--lw-header': lwGet('header'),
    '--lw-info': lwGet('info'),
    '--lw-table': lwGet('table'),
    '--lw-summary': lwGet('summary'),
    '--lw-terms': lwGet('terms'),
    '--lw-footer': lwGet('footer'),
    boxShadow: '0 2px 12px rgba(0,0,0,.15)',
    boxSizing: 'border-box',
    fontFamily: "'SimHei', 'SimSun', sans-serif",
    color: '#000',
    lineHeight: 1.5,
  }
})

// Extract preset fields from a config object
function extractPresetFields(cfg) {
  const fields = [
    'orientation', 'pageWidthMm', 'pageHeightMm', 'marginTop', 'marginBottom', 'marginLeft', 'marginRight',
    'fontSize', 'lineWidths', 'columns', 'showSummary', 'showPaymentTerms', 'showInstallationTerms', 'showWarrantyTerms', 'showTotalRow',
    'showFooter', 'paymentTermsText', 'installationTermsText', 'warrantyTermsText', 'footerText', 'sectionStyles', 'columnWidths',
  ]
  const result = {}
  for (const f of fields) {
    if ((f === 'sectionStyles' || f === 'columnWidths') && cfg[f]) {
      result[f] = JSON.parse(JSON.stringify(cfg[f]))
    } else {
      result[f] = Array.isArray(cfg[f]) ? cfg[f].map(c => ({ ...c })) : cfg[f]
    }
  }
  return result
}

function applyPreset(name) {
  const preset = allPresets.value.find(p => p.name === name)
  if (!preset) return
  const src = preset.config
  // Apply layout/style fields
  const fields = [
    'orientation', 'pageWidthMm', 'pageHeightMm', 'marginTop', 'marginBottom', 'marginLeft', 'marginRight',
    'fontSize', 'lineWidths', 'showSummary', 'showPaymentTerms', 'showInstallationTerms', 'showWarrantyTerms', 'showTotalRow',
    'showFooter', 'paymentTermsText', 'installationTermsText', 'warrantyTermsText', 'footerText',
  ]
  for (const f of fields) config[f] = src[f]
  // Deep copy columns
  config.columns = src.columns.map(c => ({ ...c }))
  // Apply sectionStyles
  if (src.sectionStyles) {
    for (const key of Object.keys(DEFAULT_SECTION_STYLES)) {
      config.sectionStyles[key] = { ...DEFAULT_SECTION_STYLES[key], ...(src.sectionStyles[key] || {}) }
    }
  }
  // Apply columnWidths
  if (src.columnWidths) {
    for (const key of Object.keys(DEFAULT_COLUMN_WIDTHS)) {
      config.columnWidths[key] = src.columnWidths[key] ?? DEFAULT_COLUMN_WIDTHS[key]
    }
  }
}

function savePreset() {
  const name = newPresetName.value.trim()
  if (!name) return
  // Check duplicate
  if (allPresets.value.some(p => p.name === name)) {
    ElMessage.warning('预设名称已存在')
    return
  }
  customPresets.value.push({
    name,
    builtIn: false,
    config: extractPresetFields(config),
  })
  currentPreset.value = name
  showSavePreset.value = false
  newPresetName.value = ''
  ElMessage.success(`预设「${name}」已保存`)
}

async function deletePreset() {
  if (isCurrentBuiltIn.value) return
  await ElMessageBox.confirm(`确定删除预设「${currentPreset.value}」？`, '提示', { type: 'warning' })
  customPresets.value = customPresets.value.filter(p => p.name !== currentPreset.value)
  currentPreset.value = '标准'
  applyPreset('标准')
  ElMessage.success('已删除')
}

function findMatchingPreset(cfg) {
  for (const p of allPresets.value) {
    const pc = p.config
    if (
      pc.orientation === cfg.orientation &&
      (pc.pageWidthMm || 210) === (cfg.pageWidthMm || 210) &&
      (pc.pageHeightMm || 297) === (cfg.pageHeightMm || 297) &&
      pc.fontSize === cfg.fontSize &&
      JSON.stringify(pc.lineWidths) === JSON.stringify(cfg.lineWidths) &&
      pc.marginTop === cfg.marginTop &&
      pc.marginBottom === cfg.marginBottom &&
      pc.marginLeft === cfg.marginLeft &&
      pc.marginRight === cfg.marginRight &&
      pc.showSummary === cfg.showSummary &&
      pc.showPaymentTerms === cfg.showPaymentTerms &&
      pc.showInstallationTerms === cfg.showInstallationTerms &&
      pc.showWarrantyTerms === cfg.showWarrantyTerms &&
      pc.showTotalRow === cfg.showTotalRow &&
      pc.showFooter === cfg.showFooter &&
      JSON.stringify(pc.columns?.map(c => c.visible)) === JSON.stringify(cfg.columns?.map(c => c.visible)) &&
      JSON.stringify(pc.sectionStyles) === JSON.stringify(cfg.sectionStyles) &&
      JSON.stringify(pc.columnWidths) === JSON.stringify(cfg.columnWidths)
    ) {
      return p.name
    }
  }
  return ''
}

onMounted(async () => {
  try {
    const res = await getPrintSetting()
    const data = res?.data?.data
    if (data) {
      let saved = null
      try {
        saved = typeof data === 'string' ? JSON.parse(data) : data
      } catch {
        // 兼容历史数据双重转义 [{\"name\":...}] → [{"name":...}]
        try { saved = JSON.parse(data.replace(/\\"/g, '"')) } catch { saved = {} }
      }
      if (!saved) saved = {}
      // Restore custom presets
      if (saved.customPresets) {
        customPresets.value = saved.customPresets
        delete saved.customPresets
      }
      Object.assign(config, saved)
      // 旧数据兼容：paperSize 迁移为 pageWidthMm
      if (config.pageWidthMm == null) {
        config.pageWidthMm = saved.paperSize === '241' ? 241 : 210
      }
      // Ensure columns array is complete and ordered per ALL_COLUMNS
      const savedColMap = Object.fromEntries(config.columns.map(c => [c.key, c]))
      config.columns = ALL_COLUMNS.map(col => {
        const saved = savedColMap[col.key]
        return saved ? { ...col, visible: saved.visible } : { ...col }
      })
      // Ensure sectionStyles is complete (backward compat, 兼容字符串形式)
      if (typeof config.sectionStyles === 'string') {
        try { config.sectionStyles = JSON.parse(config.sectionStyles.replace(/\\"/g, '"')) } catch { config.sectionStyles = {} }
      }
      if (!config.sectionStyles) config.sectionStyles = {}
      for (const key of Object.keys(DEFAULT_SECTION_STYLES)) {
        if (config.sectionStyles[key] == null) {
          config.sectionStyles[key] = { ...DEFAULT_SECTION_STYLES[key] }
        } else {
          config.sectionStyles[key] = { ...DEFAULT_SECTION_STYLES[key], ...config.sectionStyles[key] }
        }
      }
      // Ensure lineWidths is complete (backward compat)
      if (!config.lineWidths) config.lineWidths = {}
      for (const k of ['header', 'info', 'table', 'summary', 'terms', 'footer']) {
        if (config.lineWidths[k] == null) config.lineWidths[k] = 2
      }
      // Ensure columnWidths is complete (backward compat)
      if (!config.columnWidths) config.columnWidths = {}
      for (const key of Object.keys(DEFAULT_COLUMN_WIDTHS)) {
        if (config.columnWidths[key] == null) config.columnWidths[key] = DEFAULT_COLUMN_WIDTHS[key]
      }
      // Detect which preset matches
      currentPreset.value = findMatchingPreset(config) || '自定义'
    }
  } catch { /* use defaults */ }
  // Load terms from sys_config for preview fallback
  try {
    const sysRes = await getSystemSettings()
    const sysData = sysRes.data?.data ?? sysRes.data ?? {}
    sysConfigTerms.value.installation = sysData.installation_terms || ''
    sysConfigTerms.value.warranty = sysData.warranty_terms || ''
  } catch { /* ignore */ }
})

async function handleSave() {
  saving.value = true
  try {
    // 如果当前选中的是自定义预设，同步更新该预设的配置
    const idx = customPresets.value.findIndex(p => p.name === currentPreset.value)
    if (idx >= 0) {
      customPresets.value[idx].config = extractPresetFields(config)
    }
    const saveData = { ...config, customPresets: customPresets.value }
    await savePrintSetting(JSON.stringify(saveData))
    reloadPrintConfig()
    ElMessage.success('保存成功')
  } catch (e) {
    if (!e._handled) ElMessage.error('保存失败：' + (e.message || e))
  } finally {
    saving.value = false
  }
}

// =====================================================================
// 售后单打印设置
// =====================================================================
const AS_SECTION_LABELS = {
  header: '标题',
  info: '基本信息',
  section: '内容段',
  footer: '页脚',
}

const AS_DEFAULT_SECTION_STYLES = {
  header: { bold: false, sizeOffset: 0 },
  info: { bold: false, sizeOffset: 0 },
  section: { bold: true, sizeOffset: 0 },
  footer: { bold: false, sizeOffset: 0 },
}

const AS_DEFAULT_CONFIG = {
  orientation: 'portrait',
  pageWidthMm: 210, // 纸张宽度(mm)，A4=210；复写纸按实际量
  pageHeightMm: 297, // 页面高度(mm)，A4=297；针式两联复写纸通常更矮，需按实际量
  marginTop: 10, marginBottom: 10, marginLeft: 8, marginRight: 8,
  fontSize: 12,
  companyName: '顺居门业',
  creatorPhone: '18948846839',
  creatorAddress: '乳源县二九一物流仓库',
  showSignature: true,
  showTechnician: true,
  showSaleOrderNo: true,
  showProcessingInfo: true,
  showWarranty: true,
  showSource: true,
  footerText: '',
  sectionStyles: JSON.parse(JSON.stringify(AS_DEFAULT_SECTION_STYLES)),
}

const AS_BUILT_IN_PRESETS = [
  { name: '标准', builtIn: true, config: { ...AS_DEFAULT_CONFIG } },
  { name: '简约', builtIn: true, config: { ...AS_DEFAULT_CONFIG, showSignature: false, showSource: false, showWarranty: false, fontSize: 11, marginTop: 8, marginBottom: 8, marginLeft: 6, marginRight: 6 } },
  {
    // 针式打印机 + 两联复写纸专用：A4 宽、页面高度较矮、上边距加大便于对齐复写纸
    name: '针式复写', builtIn: true,
    config: {
      ...AS_DEFAULT_CONFIG,
      pageWidthMm: 210,
      pageHeightMm: 150,
      marginTop: 15, marginBottom: 8, marginLeft: 6, marginRight: 6,
      fontSize: 13,
      sectionStyles: {
        header: { bold: false, sizeOffset: 0 },
        info: { bold: false, sizeOffset: 1 },
        section: { bold: true, sizeOffset: 1 },
        footer: { bold: false, sizeOffset: 0 },
      },
    },
  },
]

const asCustomPresets = ref([])
const allAsPresets = computed(() => [...AS_BUILT_IN_PRESETS, ...asCustomPresets.value])
const asCurrentPreset = ref('标准')
const isAsCurrentBuiltIn = computed(() => AS_BUILT_IN_PRESETS.some(p => p.name === asCurrentPreset.value))
const showAsSavePreset = ref(false)
const newAsPresetName = ref('')

const asConfig = reactive({ ...AS_DEFAULT_CONFIG, sectionStyles: JSON.parse(JSON.stringify(AS_DEFAULT_SECTION_STYLES)) })

function applyAsPreset(name) {
  const preset = allAsPresets.value.find(p => p.name === name)
  if (!preset) return
  const src = preset.config
  const fields = ['orientation', 'pageWidthMm', 'pageHeightMm', 'marginTop', 'marginBottom', 'marginLeft', 'marginRight', 'fontSize', 'companyName', 'creatorPhone', 'creatorAddress', 'showSignature', 'showTechnician', 'showSaleOrderNo', 'showProcessingInfo', 'showWarranty', 'showSource', 'footerText']
  for (const f of fields) asConfig[f] = src[f]
  if (src.sectionStyles) {
    for (const key of Object.keys(AS_DEFAULT_SECTION_STYLES)) {
      asConfig.sectionStyles[key] = { ...AS_DEFAULT_SECTION_STYLES[key], ...(src.sectionStyles[key] || {}) }
    }
  }
}

function saveAsPreset() {
  const name = newAsPresetName.value.trim()
  if (!name) return
  if (allAsPresets.value.some(p => p.name === name)) {
    ElMessage.warning('预设名称已存在')
    return
  }
  const fields = ['orientation', 'pageWidthMm', 'pageHeightMm', 'marginTop', 'marginBottom', 'marginLeft', 'marginRight', 'fontSize', 'companyName', 'creatorPhone', 'creatorAddress', 'showSignature', 'showTechnician', 'showSaleOrderNo', 'showProcessingInfo', 'showWarranty', 'showSource', 'footerText', 'sectionStyles']
  const cfg = {}
  for (const f of fields) {
    cfg[f] = f === 'sectionStyles' ? JSON.parse(JSON.stringify(asConfig.sectionStyles)) : asConfig[f]
  }
  asCustomPresets.value.push({ name, builtIn: false, config: cfg })
  asCurrentPreset.value = name
  showAsSavePreset.value = false
  newAsPresetName.value = ''
  ElMessage.success(`预设「${name}」已保存`)
}

async function deleteAsPreset() {
  if (isAsCurrentBuiltIn.value) return
  await ElMessageBox.confirm(`确定删除预设「${asCurrentPreset.value}」？`, '提示', { type: 'warning' })
  asCustomPresets.value = asCustomPresets.value.filter(p => p.name !== asCurrentPreset.value)
  asCurrentPreset.value = '标准'
  applyAsPreset('标准')
  ElMessage.success('已删除')
}

function findAsMatchingPreset(cfg) {
  for (const p of allAsPresets.value) {
    const pc = p.config
    if (
      pc.orientation === cfg.orientation && (pc.pageWidthMm || 210) === (cfg.pageWidthMm || 210) &&
      (pc.pageHeightMm || 297) === (cfg.pageHeightMm || 297) && pc.fontSize === cfg.fontSize &&
      pc.marginTop === cfg.marginTop && pc.marginBottom === cfg.marginBottom &&
      pc.marginLeft === cfg.marginLeft && pc.marginRight === cfg.marginRight &&
      pc.showSignature === cfg.showSignature && pc.showTechnician === cfg.showTechnician &&
      pc.showSaleOrderNo === cfg.showSaleOrderNo && pc.showProcessingInfo === cfg.showProcessingInfo &&
      pc.showWarranty === cfg.showWarranty && pc.showSource === cfg.showSource &&
      JSON.stringify(pc.sectionStyles) === JSON.stringify(cfg.sectionStyles)
    ) {
      return p.name
    }
  }
  return ''
}

function asPreviewSectionBold(key) {
  const s = asConfig.sectionStyles?.[key]
  return s?.bold ? { fontWeight: 'bold' } : {}
}
function asPreviewSectionFontSize(key, basePx) {
  const s = asConfig.sectionStyles?.[key]
  const offset = s?.sizeOffset || 0
  if (offset === 0) return {}
  return { fontSize: (basePx + offset) + 'px' }
}
const asPreviewContainerStyle = computed(() => ({
  background: '#e8e8e8', padding: '20px', borderRadius: '6px', overflow: 'auto', maxHeight: 'calc(100vh - 140px)',
}))
const asPreviewPageStyle = computed(() => {
  const portrait = asConfig.orientation !== 'landscape'
  const w = asConfig.pageWidthMm || 210
  const h = asConfig.pageHeightMm || 297
  return {
    background: '#fff',
    width: portrait ? `${w}mm` : '297mm',
    minHeight: portrait ? `${h}mm` : '210mm',
    margin: '0 auto',
    padding: `${asConfig.marginTop}mm ${asConfig.marginRight}mm ${asConfig.marginBottom}mm ${asConfig.marginLeft}mm`,
    fontSize: asConfig.fontSize + 'px',
    boxShadow: '0 2px 12px rgba(0,0,0,.15)',
    boxSizing: 'border-box',
    fontFamily: "'Microsoft YaHei', 'SimSun', sans-serif",
    color: '#000',
    lineHeight: 1.5,
  }
})

async function loadAsConfig() {
  try {
    const res = await getPrintSetting('after_sale')
    const data = res?.data?.data
    if (data) {
      const saved = typeof data === 'string' ? JSON.parse(data) : data
      if (saved.customPresets) {
        asCustomPresets.value = saved.customPresets
        delete saved.customPresets
      }
      Object.assign(asConfig, saved)
      // 旧数据兼容：paperSize 迁移为 pageWidthMm
      if (asConfig.pageWidthMm == null) {
        asConfig.pageWidthMm = saved.paperSize === '241' ? 241 : 210
      }
      if (!asConfig.sectionStyles) asConfig.sectionStyles = {}
      for (const key of Object.keys(AS_DEFAULT_SECTION_STYLES)) {
        if (asConfig.sectionStyles[key] == null) asConfig.sectionStyles[key] = { ...AS_DEFAULT_SECTION_STYLES[key] }
        else asConfig.sectionStyles[key] = { ...AS_DEFAULT_SECTION_STYLES[key], ...asConfig.sectionStyles[key] }
      }
      asCurrentPreset.value = findAsMatchingPreset(asConfig) || '自定义'
    }
  } catch { /* use defaults */ }
}

async function handleAsSave() {
  saving.value = true
  try {
    const idx = asCustomPresets.value.findIndex(p => p.name === asCurrentPreset.value)
    if (idx >= 0) {
      const fields = ['orientation', 'pageWidthMm', 'pageHeightMm', 'marginTop', 'marginBottom', 'marginLeft', 'marginRight', 'fontSize', 'companyName', 'creatorPhone', 'creatorAddress', 'showSignature', 'showTechnician', 'showSaleOrderNo', 'showProcessingInfo', 'showWarranty', 'showSource', 'footerText', 'sectionStyles']
      const cfg = {}
      for (const f of fields) cfg[f] = f === 'sectionStyles' ? JSON.parse(JSON.stringify(asConfig.sectionStyles)) : asConfig[f]
      asCustomPresets.value[idx].config = cfg
    }
    const saveData = { ...asConfig, customPresets: asCustomPresets.value }
    await savePrintSetting(JSON.stringify(saveData), 'after_sale')
    invalidatePrintConfig('after_sale')
    ElMessage.success('售后单打印设置已保存')
  } catch (e) {
    if (!e._handled) ElMessage.error('保存失败：' + (e.message || e))
  } finally {
    saving.value = false
  }
}

// Override handleSave to save both order and after-sale configs
const originalHandleSave = handleSave
async function handleSaveAll() {
  if (activeTab.value === 'afterSaleSettings' || activeTab.value === 'afterSalePreview') {
    await handleAsSave()
  } else {
    await originalHandleSave()
  }
}

onMounted(() => { loadAsConfig() })
</script>

<style scoped>
.print-settings-page {
  height: 100%;
}

/* 预览区域点击调节 */
.preview-page { position: relative; }
.section-clickable { cursor: pointer; }
.section-clickable:hover { outline: 2px solid rgba(197, 162, 101, .45); outline-offset: -1px; }
.section-selected {
  outline: 2px solid #C5A265 !important;
  outline-offset: -1px;
  background-color: rgba(197, 162, 101, .10) !important;
}
.section-adjust-bar {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #C5A265;
  box-shadow: 0 6px 24px rgba(0, 0, 0, .18);
  color: #333;
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 13px;
  white-space: nowrap;
}
.section-adjust-bar .bar-name { color: #C5A265; font-weight: 700; font-size: 14px; }
.section-adjust-bar .bar-size { display: flex; align-items: center; gap: 4px; }
.section-adjust-bar .lw-inline { display: inline-flex; align-items: center; gap: 4px; }
.section-adjust-bar .lw-mini { font-size: 12px; color: #C5A265; font-weight: 600; }
.section-adjust-bar .size-btn {
  background: #f5f5f5; color: #333; border: 1px solid #ddd;
  width: 24px; height: 24px; border-radius: 6px; cursor: pointer;
  font-size: 14px; line-height: 1;
}
.section-adjust-bar .size-btn:hover:not(:disabled) { border-color: #C5A265; color: #C5A265; }
.section-adjust-bar .size-btn:disabled { opacity: .4; cursor: not-allowed; }
.section-adjust-bar .size-display { color: #C5A265; font-weight: 600; min-width: 36px; text-align: center; }
.section-adjust-bar .bar-close {
  background: none; border: 1px solid #e74c3c; color: #e74c3c;
  cursor: pointer; font-size: 13px; font-weight: 700; padding: 1px 8px; border-radius: 4px;
  transition: all .15s;
}
.section-adjust-bar .bar-close:hover { background: #e74c3c; color: #fff; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.settings-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 16px 16px;
}

/* Preset Bar */
.preset-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f0f5ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
  margin-bottom: 16px;
}
.preset-label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}
.pin-hint {
  padding: 10px 14px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 12px;
  color: #8a6d1a;
  line-height: 1.7;
  margin-bottom: 14px;
}
.mm-hint { font-size: 11px; color: #999; white-space: nowrap; }

.save-preset-dialog {
  position: fixed;
  top: 64px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.preview-toolbar-hint {
  font-size: 13px;
  color: #666;
}
.save-preset-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
}
.save-preset-inner label {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
}
.preset-name-input {
  width: 180px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color .2s;
}
.preset-name-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64,158,255,.15);
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  max-width: 900px;
}

.section {
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #eee;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.form-row:last-child { margin-bottom: 0; }
.form-row > label {
  font-size: 13px;
  color: #555;
  min-width: 90px;
  flex-shrink: 0;
}
.lw-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
}
.lw-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.lw-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}
.form-row .el-input-number {
  width: 120px;
}
.form-row .el-input {
  flex: 1;
}
.form-row .el-select {
  width: 120px;
}

.col-list {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 6px;
}
.col-item {
  font-size: 13px;
}

/* Font style controls */
.font-style-row {
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}
.font-style-row:last-child { border-bottom: none; }
.font-style-row > label {
  min-width: 70px;
}
.size-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}
.size-btn {
  width: 24px;
  height: 24px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all .15s;
  padding: 0;
}
.size-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
}
.size-btn:disabled {
  opacity: .4;
  cursor: not-allowed;
}
.size-display {
  min-width: 36px;
  text-align: center;
  font-size: 12px;
  color: #333;
  font-weight: 500;
}

/* Column resize handle */
.p-table th {
  position: relative;
}
.col-resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 5px;
  cursor: col-resize;
  z-index: 1;
}
.col-resize-handle:hover {
  background: #409eff;
}
/* === Preview Styles === */
.p-header {
  text-align: center;
  padding-bottom: 8px;
  border-bottom: var(--lw-header) solid #222;
  margin-bottom: 10px;
}
.p-title {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 6px;
  display: inline-block;
  background: #000;
  color: #fff;
  padding: 6px 28px;
}
.p-company {
  font-size: 12px;
  color: #000;
  margin-top: 2px;
  letter-spacing: 2px;
}

.p-info {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: var(--lw-info) solid #000;
}
.p-info-left, .p-info-right {
  flex: 1;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 1px 8px;
  align-items: baseline;
}
.p-info-row { display: contents; }
.p-label {
  font-size: 10px;
  color: #333;
  white-space: nowrap;
}
.p-value {
  font-size: 11px;
  color: #000;
  font-weight: 500;
}
.p-value.mono {
  font-family: 'Courier New', monospace;
  font-size: 10px;
  letter-spacing: 0.5px;
}

.p-table-title {
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 4px;
}
.p-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;
  font-size: 10px;
}
.p-table th, .p-table td {
  border: var(--lw-table) solid #000;
  padding: 4px 5px;
  text-align: center;
  vertical-align: middle;
}
.p-table th {
  background: #fff;
  font-weight: 700;
  font-size: 9px;
  text-align: center;
  white-space: nowrap;
  padding: 1px 5px;
  line-height: 1.6;
}
.p-table tfoot td {
  font-weight: 700;
  background: #fff;
  text-align: right;
  font-size: 11px;
}
.c-no { width: 28px; text-align: center; }
.c-productName { min-width: 90px; }
.c-glassType { min-width: 50px; }
.c-color { width: 50px; }
.c-width, .c-height, .c-wallThickness { width: 36px; text-align: center; }
.c-size-group { text-align: center; }
.unit-hint { font-size: 9px; font-weight: 400; color: #888; }
.c-lockPosition { width: 36px; text-align: center; }
.c-diaojiao, .c-doorCount, .c-fangCount { width: 36px; text-align: center; }
.c-productType { width: 60px; }
.c-unitPrice { width: 60px; text-align: right; }
.c-amount { width: 60px; text-align: right; font-variant-numeric: tabular-nums; }
.c-remark { width: 70px; font-size: 9px; color: #666; }

.total-cell { text-align: right; font-size: 11px; }
.remark-cell { text-align: left !important; font-size: 10px; font-weight: 400; color: #444; padding: 4px 6px; background: #fff; }
.p-table tfoot tr .remark-cell { text-align: left !important; }

.p-summary {
  margin-bottom: 12px;
  padding: 8px 10px;
  border: var(--lw-summary) solid #000;
  border-radius: 0;
  background: #fff;
}
.p-summary-grid {
  display: flex;
  justify-content: center;
  gap: 2em;
}
.p-summary-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.p-summary-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
  line-height: 2;
}
.p-summary-label {
  color: #333;
  white-space: nowrap;
  min-width: 60px;
  font-size: 12px;
}
.p-summary-value { color: #000; font-weight: 600; font-size: 16px; }
.p-summary-value.strong { font-weight: 700; font-size: 18px; }
.p-summary-value.chinese { font-size: 12px; color: #000; font-weight: 400; }
.p-summary-value.red { color: #000; font-weight: 700; }

.type-summary-row .type-summary-cell {
  font-weight: 400;
  font-size: 10px;
  padding: 2px 5px;
  border-top: none;
  border-bottom: var(--lw-table) solid #000;
  background: #fff;
}
.type-summary-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2px 2em;
}
.type-summary-item {
  white-space: nowrap;
}

.p-terms {
  margin-bottom: 10px;
  padding: 6px 8px;
  border: var(--lw-terms) solid #000;
  border-radius: 0;
  background: #fff;
}
.p-terms-title { font-size: 10px; font-weight: 700; margin-bottom: 3px; color: #000; }
.p-terms-body { font-size: 9px; color: #000; line-height: 1.5; white-space: pre-line; }

.p-footer {
  margin-top: 12px;
  padding-top: 8px;
  border-top: var(--lw-footer) solid #000;
}
.p-footer-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  gap: 12px;
}
.p-footer-date {
  text-align: right;
  font-size: 9px;
  color: #999;
  margin-top: 4px;
}

/* === After-Sale Preview Styles === */
.as-preview-page {
  background: #fff;
  margin: 0 auto;
  box-shadow: 0 2px 12px rgba(0,0,0,.15);
  box-sizing: border-box;
  line-height: 1.5;
}
.as-p-header {
  text-align: center;
  padding-bottom: 12px;
  border-bottom: 2px solid #333;
  margin-bottom: 16px;
}
.as-p-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 6px;
  display: inline-block;
  background: #000;
  color: #fff;
  padding: 6px 28px;
}
.as-p-company {
  font-size: 12px;
  color: #666;
  letter-spacing: 2px;
}
.as-p-info-section {
  margin-bottom: 16px;
}
.as-p-info-row {
  display: flex;
  gap: 20px;
  margin-bottom: 8px;
}
.as-p-info-item {
  flex: 1;
  display: flex;
  gap: 6px;
  align-items: baseline;
}
.as-p-label {
  font-size: 10px;
  color: #888;
  white-space: nowrap;
  min-width: 52px;
}
.as-p-value {
  font-size: 11px;
  color: #222;
  font-weight: 500;
}
.as-p-value.mono {
  font-family: 'Consolas', monospace;
  font-size: 10px;
  letter-spacing: 0.5px;
}
.as-p-section {
  margin-bottom: 14px;
}
.as-p-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  padding-bottom: 6px;
  border-bottom: 1px solid #ddd;
  margin-bottom: 8px;
}
.as-p-section-body {
  font-size: 12px;
  line-height: 1.8;
  color: #444;
}
.as-p-signature {
  display: flex;
  gap: 40px;
  margin-top: 36px;
  padding-top: 16px;
}
.as-p-sig-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.as-p-sig-label {
  white-space: nowrap;
  color: #666;
  font-size: 11px;
}
.as-p-sig-line {
  display: inline-block;
  width: 100px;
  border-bottom: 1px solid #333;
}
.as-p-footer {
  margin-top: 24px;
  padding-top: 8px;
  border-top: 1px solid #ddd;
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #999;
}
.as-p-footer-text {
  text-align: right;
  font-size: 9px;
  color: #999;
  margin-top: 2px;
}
</style>

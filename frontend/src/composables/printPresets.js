// 打印预设共享定义：打印设置页与销售单预览/打印共用同一份预设列表

export const ALL_COLUMNS = [
  { key: 'productName', label: '商品名称', visible: true },
  { key: 'color', label: '颜色', visible: true },
  { key: 'width', label: '宽(mm)', visible: true },
  { key: 'height', label: '高(mm)', visible: true },
  { key: 'wallThickness', label: '墙厚(mm)', visible: true },
  { key: 'glassType', label: '玻璃款式', visible: true },
  { key: 'lockPosition', label: '锁位', visible: true },
  { key: 'diaojiao', label: '吊脚', visible: true },
  { key: 'doorCount', label: '樘数', visible: true },
  { key: 'fangCount', label: '面积(㎡)', visible: true },
  { key: 'productType', label: '细目', visible: true },
  { key: 'unitPrice', label: '单价', visible: true },
  { key: 'remark', label: '备注', visible: true },
]

export const COMPACT_COLUMNS = ALL_COLUMNS.map(c => ({
  ...c,
  visible: ['productName', 'width', 'height', 'doorCount', 'fangCount', 'unitPrice', 'remark'].includes(c.key),
}))

export const DEFAULT_SECTION_STYLES = {
  header: { bold: false, sizeOffset: 0 },
  info: { bold: false, sizeOffset: 0, align: 'left' },
  tableHead: { bold: true, sizeOffset: 0 },
  tableBody: { bold: false, sizeOffset: 0 },
  summary: { bold: false, sizeOffset: 0 },
  terms: { bold: false, sizeOffset: 0 },
  footer: { bold: false, sizeOffset: 0 },
}

export const DEFAULT_COLUMN_WIDTHS = {
  productName: 100, color: 50,
  width: 40, height: 40, wallThickness: 40, glassType: 50,
  lockPosition: 36, diaojiao: 40, doorCount: 40, fangCount: 48,
  productType: 60, unitPrice: 56, amount: 60, remark: 70,
}

export const DEFAULT_CONFIG = {
  orientation: 'portrait',
  pageWidthMm: 210,
  pageHeightMm: 297,
  marginTop: 10, marginBottom: 10, marginLeft: 8, marginRight: 8,
  fontSize: 11,
  lineWidths: { header: 2, info: 2, table: 2, summary: 2, terms: 2, footer: 2 },
  companyName: '顺居门业',
  creatorName: '冯伟明',
  creatorPhone: '18948846839',
  creatorAddress: '乳源县二九一物流仓库',
  columns: ALL_COLUMNS.map(c => ({ ...c })),
  showSummary: true,
  showPaymentTerms: true,
  showInstallationTerms: false,
  showWarrantyTerms: false,
  showTotalRow: true,
  showFooter: true,
  paymentTermsText: '客户订货需支付总金额百分之30，安装完成后需付尾款百分之70，剩余货款需在一个月内付清。',
  installationTermsText: '',
  warrantyTermsText: '',
  footerText: '',
  sectionStyles: JSON.parse(JSON.stringify(DEFAULT_SECTION_STYLES)),
  columnWidths: { ...DEFAULT_COLUMN_WIDTHS },
}

export function makePresetConfig(overrides) {
  const mergedSections = {}
  for (const [k, v] of Object.entries(DEFAULT_SECTION_STYLES)) {
    mergedSections[k] = { ...v, ...(overrides.sectionStyles?.[k] || {}) }
  }
  return {
    ...DEFAULT_CONFIG,
    ...overrides,
    columns: (overrides.columns || DEFAULT_CONFIG.columns).map(c => ({ ...c })),
    sectionStyles: mergedSections,
    columnWidths: { ...DEFAULT_COLUMN_WIDTHS },
  }
}

export const BUILT_IN_PRESETS = [
  { name: '标准', builtIn: true, config: makePresetConfig({}) },
  {
    name: '简洁', builtIn: true,
    config: makePresetConfig({
      marginTop: 8, marginBottom: 8, marginLeft: 6, marginRight: 6,
      fontSize: 10,
      columns: COMPACT_COLUMNS,
      showSummary: false,
      showPaymentTerms: false,
    }),
  },
  {
    name: '详细', builtIn: true,
    config: makePresetConfig({
      marginTop: 12, marginBottom: 12, marginLeft: 10, marginRight: 10,
      fontSize: 12,
    }),
  },
  {
    // 针式打印机 + 两联复写纸专用：A4 宽、页面高度较矮、纯黑加粗、上边距加大便于对齐复写纸
    name: '针式复写', builtIn: true,
    config: makePresetConfig({
      pageWidthMm: 210,
      pageHeightMm: 200,
      marginTop: 18, marginBottom: 8, marginLeft: 6, marginRight: 6,
      fontSize: 13,
      columns: COMPACT_COLUMNS,
      showSummary: true,
      showPaymentTerms: false,
      sectionStyles: {
        info: { sizeOffset: 1 },
        tableHead: { sizeOffset: 1 },
        tableBody: { sizeOffset: 2 },
      },
    }),
  },
]

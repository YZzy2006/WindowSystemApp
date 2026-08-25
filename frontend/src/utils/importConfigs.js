/**
 * Import column configurations for all order system modules.
 * `label` must match the export column labels exactly.
 * `itemField: true` marks fields belonging to order items (for groupOrderRows).
 */
export const importConfigs = {
  // ===== 销售订单 =====
  saleOrders: {
    groupBy: 'orderNo',
    columns: [
      { label: '订单编号', key: 'orderNo', type: 'string' },
      { label: '订单类型', key: 'orderType', type: 'string', reverseMap: { '销售单': 'sale', '预算单': 'presale' } },
      { label: '客户名称', key: 'customerName', type: 'string', required: true },
      { label: '联系电话', key: 'customerPhone', type: 'string' },
      { label: '客户地址', key: 'customerAddress', type: 'string' },
      { label: '订单日期', key: 'orderDate', type: 'date', required: true },
      { label: '定金', key: 'deposit', type: 'number' },
      { label: '状态', key: 'status', type: 'string', reverseMap: { '待处理': 'pending', '进行中': 'processing', '已完成': 'completed', '已取消': 'cancelled' } },
      { label: '备注', key: 'remark', type: 'string' },
      { label: '发货通知', key: 'notice', type: 'string' },
      // 明细字段
      { label: '商品名称', key: 'productName', type: 'string', itemField: true },
      { label: '系列', key: 'series', type: 'string', itemField: true },
      { label: '颜色', key: 'color', type: 'string', itemField: true },
      { label: '细目', key: 'productType', type: 'string', itemField: true },
      { label: '宽(mm)', key: 'width', type: 'number', itemField: true },
      { label: '高(mm)', key: 'height', type: 'number', itemField: true },
      { label: '墙厚(mm)', key: 'wallThickness', type: 'number', itemField: true },
      { label: '玻璃款式', key: 'glassType', type: 'string', itemField: true },
      { label: '锁位', key: 'lockPosition', type: 'string', itemField: true },
      { label: '吊脚', key: 'diaojiao', type: 'number', itemField: true },
      { label: '樘数', key: 'doorCount', type: 'number', itemField: true },
      { label: '面积(㎡)', key: 'fangCount', type: 'number', itemField: true },
      { label: '单位', key: 'unit', type: 'string', itemField: true },
      { label: '数量', key: 'quantity', type: 'number', itemField: true },
      { label: '单价', key: 'unitPrice', type: 'number', itemField: true },
      { label: '材料成本', key: 'materialCost', type: 'number', itemField: true },
      { label: '人工成本', key: 'laborCost', type: 'number', itemField: true },
      { label: '配件成本', key: 'accessoryCost', type: 'number', itemField: true },
      { label: '附加费', key: 'extraFee', type: 'number', itemField: true },
      { label: '明细备注', key: 'itemRemark', type: 'string', itemField: true },
    ],
    sample: {
      orderNo: 'XS2025001', orderType: '销售单', customerName: '张三', customerPhone: '13800138000',
      customerAddress: '广东省佛山市南海区', orderDate: '2025-01-15', deposit: 5000,
      status: '待处理', remark: '', notice: '',
      productName: '断桥铝推拉门', series: '80系列', color: '砂灰色', productType: '推拉门',
      width: 2000, height: 2400, wallThickness: 80, glassType: '双层中空', lockPosition: '右锁',
      diaojiao: 50, doorCount: 1, fangCount: 4.8, unit: '㎡', quantity: 1, unitPrice: 2800,
      materialCost: 500, laborCost: 300, accessoryCost: 80, extraFee: 0, itemRemark: '',
    },
  },

  // ===== 采购订单 =====
  purchaseOrders: {
    groupBy: 'orderNo',
    columns: [
      { label: '订单编号', key: 'orderNo', type: 'string' },
      { label: '供应商', key: 'supplierName', type: 'string', required: true },
      { label: '联系人', key: 'supplierContact', type: 'string' },
      { label: '电话', key: 'supplierPhone', type: 'string' },
      { label: '地址', key: 'supplierAddress', type: 'string' },
      { label: '订单日期', key: 'orderDate', type: 'date', required: true },
      { label: '总金额', key: 'totalAmount', type: 'number', skip: true },
      { label: '已付', key: 'paidAmount', type: 'number', skip: true },
      { label: '状态', key: 'status', type: 'string', reverseMap: { '待处理': 'pending', '进行中': 'processing', '已完成': 'completed', '已取消': 'cancelled' } },
      { label: '备注', key: 'remark', type: 'string' },
      // 明细字段
      { label: '商品名称', key: 'productName', type: 'string', itemField: true },
      { label: '分类', key: 'productCategory', type: 'string', itemField: true },
      { label: '规格', key: 'spec', type: 'string', itemField: true },
      { label: '单位', key: 'unit', type: 'string', itemField: true },
      { label: '库位', key: 'warehouseLoc', type: 'string', itemField: true },
      { label: '数量', key: 'quantity', type: 'number', itemField: true },
      { label: '单价', key: 'unitPrice', type: 'number', itemField: true },
      { label: '金额', key: 'amount', type: 'number', itemField: true, skip: true },
      { label: '明细备注', key: 'itemRemark', type: 'string', itemField: true },
    ],
    sample: {
      orderNo: 'CG2025001', supplierName: '佛山铝材厂', supplierContact: '李四', supplierPhone: '13900139000',
      supplierAddress: '广东省佛山市三水区', orderDate: '2025-01-16',
      status: '待处理', remark: '',
      productName: '铝型材80系列', productCategory: '型材', spec: '80×80×1.4mm', unit: '支',
      warehouseLoc: 'A-01', quantity: 100, unitPrice: 80, itemRemark: '',
    },
  },

  // ===== 销售退货 =====
  saleReturns: {
    groupBy: 'orderNo',
    columns: [
      { label: '退货单号', key: 'orderNo', type: 'string' },
      { label: '原订单号', key: 'originalOrderNo', type: 'string' },
      { label: '客户名称', key: 'customerName', type: 'string', required: true },
      { label: '退货日期', key: 'returnDate', type: 'date', required: true },
      { label: '总金额', key: 'totalAmount', type: 'number', skip: true },
      { label: '已退金额', key: 'paidAmount', type: 'number', skip: true },
      { label: '状态', key: 'status', type: 'string', reverseMap: { '待处理': 'pending', '进行中': 'processing', '已完成': 'completed', '已取消': 'cancelled' } },
      { label: '备注', key: 'remark', type: 'string' },
      // 明细字段
      { label: '商品名称', key: 'productName', type: 'string', itemField: true },
      { label: '分类', key: 'productCategory', type: 'string', itemField: true },
      { label: '规格', key: 'spec', type: 'string', itemField: true },
      { label: '单位', key: 'unit', type: 'string', itemField: true },
      { label: '数量', key: 'quantity', type: 'number', itemField: true },
      { label: '单价', key: 'unitPrice', type: 'number', itemField: true },
      { label: '金额', key: 'amount', type: 'number', itemField: true, skip: true },
      { label: '明细备注', key: 'itemRemark', type: 'string', itemField: true },
    ],
    sample: {
      orderNo: 'XT2025001', originalOrderNo: 'XS2025001', customerName: '张三', returnDate: '2025-02-01',
      status: '待处理', remark: '尺寸不合',
      productName: '断桥铝推拉门', productCategory: '门窗', spec: '2000×2400', unit: '㎡',
      quantity: 1, unitPrice: 2000, itemRemark: '',
    },
  },

  // ===== 采购退货 =====
  purchaseReturns: {
    groupBy: 'orderNo',
    columns: [
      { label: '退货单号', key: 'orderNo', type: 'string' },
      { label: '原订单号', key: 'originalOrderNo', type: 'string' },
      { label: '供应商', key: 'supplierName', type: 'string', required: true },
      { label: '退货日期', key: 'returnDate', type: 'date', required: true },
      { label: '总金额', key: 'totalAmount', type: 'number', skip: true },
      { label: '已退金额', key: 'paidAmount', type: 'number', skip: true },
      { label: '状态', key: 'status', type: 'string', reverseMap: { '待处理': 'pending', '进行中': 'processing', '已完成': 'completed', '已取消': 'cancelled' } },
      { label: '备注', key: 'remark', type: 'string' },
      // 明细字段
      { label: '商品名称', key: 'productName', type: 'string', itemField: true },
      { label: '分类', key: 'productCategory', type: 'string', itemField: true },
      { label: '规格', key: 'spec', type: 'string', itemField: true },
      { label: '单位', key: 'unit', type: 'string', itemField: true },
      { label: '数量', key: 'quantity', type: 'number', itemField: true },
      { label: '单价', key: 'unitPrice', type: 'number', itemField: true },
      { label: '金额', key: 'amount', type: 'number', itemField: true, skip: true },
      { label: '明细备注', key: 'itemRemark', type: 'string', itemField: true },
    ],
    sample: {
      orderNo: 'CT2025001', originalOrderNo: 'CG2025001', supplierName: '佛山铝材厂', returnDate: '2025-02-05',
      status: '待处理', remark: '质量问题',
      productName: '铝型材80系列', productCategory: '型材', spec: '80×80×1.4mm', unit: '支',
      quantity: 10, unitPrice: 80, itemRemark: '',
    },
  },

  // ===== 入库管理 =====
  stockIn: {
    columns: [
      { label: '单号', key: 'orderNo', type: 'string' },
      { label: '日期', key: 'orderDate', type: 'date' },
      { label: '申请人', key: 'applicant', type: 'string', required: true },
      { label: '仓管员', key: 'warehouseKeeper', type: 'string', required: true },
      { label: '经办人', key: 'operator', type: 'string', required: true },
      { label: '备注', key: 'remark', type: 'string' },
      { label: '创建时间', key: 'createTime', type: 'date', skip: true },
    ],
    sample: { orderNo: 'RK2025001', orderDate: '2025-01-20', applicant: '王五', warehouseKeeper: '赵六', operator: '钱七', remark: '' },
  },

  // ===== 出库管理 =====
  stockOut: {
    columns: [
      { label: '单号', key: 'orderNo', type: 'string' },
      { label: '日期', key: 'orderDate', type: 'date' },
      { label: '申请人', key: 'applicant', type: 'string', required: true },
      { label: '仓管员', key: 'warehouseKeeper', type: 'string', required: true },
      { label: '经办人', key: 'operator', type: 'string', required: true },
      { label: '备注', key: 'remark', type: 'string' },
      { label: '创建时间', key: 'createTime', type: 'date', skip: true },
    ],
    sample: { orderNo: 'CK2025001', orderDate: '2025-01-20', applicant: '王五', warehouseKeeper: '赵六', operator: '钱七', remark: '' },
  },

  // ===== 收付款 =====
  payments: {
    columns: [
      { label: '单号', key: 'id', type: 'number', skip: true },
      { label: '关联订单号', key: 'orderNo', type: 'string' },
      { label: '日期', key: 'paymentDate', type: 'date', required: true },
      { label: '类型', key: 'type', type: 'string', required: true, reverseMap: { '收款': 'receipt', '付款': 'payment' } },
      { label: '金额', key: 'amount', type: 'number', required: true },
      { label: '对方名称', key: 'partyName', type: 'string' },
      { label: '备注', key: 'remark', type: 'string' },
    ],
    sample: { orderNo: 'FK2025001', paymentDate: '2025-01-25', type: '收款', amount: 5000, partyName: '张三', remark: '' },
  },

  // ===== 客户管理 =====
  customers: {
    columns: [
      { label: '客户名称', key: 'name', type: 'string', required: true },
      { label: '联系人', key: 'contact', type: 'string' },
      { label: '电话', key: 'phone', type: 'string' },
      { label: '地址', key: 'address', type: 'string' },
      { label: '备注', key: 'remark', type: 'string' },
    ],
    sample: { name: '张三', contact: '张三', phone: '13800138000', address: '广东省佛山市南海区', remark: '' },
  },

  // ===== 供应商管理 =====
  suppliers: {
    columns: [
      { label: '供应商名称', key: 'name', type: 'string', required: true },
      { label: '联系人', key: 'contact', type: 'string' },
      { label: '电话', key: 'phone', type: 'string' },
      { label: '地址', key: 'address', type: 'string' },
      { label: '备注', key: 'remark', type: 'string' },
    ],
    sample: { name: '佛山铝材厂', contact: '李四', phone: '13900139000', address: '广东省佛山市三水区', remark: '' },
  },

  // ===== 商品库存 =====
  commodities: {
    columns: [
      { label: '编码', key: 'code', type: 'string', required: true },
      { label: '商品名称', key: 'name', type: 'string', required: true },
      { label: '单位', key: 'unit', type: 'string' },
      { label: '单价', key: 'defaultPrice', type: 'number' },
      { label: '成本', key: 'costPrice', type: 'number' },
      { label: '材料成本', key: 'materialCost', type: 'number' },
      { label: '人工成本', key: 'laborCost', type: 'number' },
      { label: '配件成本', key: 'accessoryCost', type: 'number' },
      { label: '计算公式', key: 'formulaName', type: 'string' },
    ],
    sample: { code: 'SP001', name: '断桥铝推拉门80系列', unit: '㎡', defaultPrice: 2800, costPrice: 1800, materialCost: 1000, laborCost: 500, accessoryCost: 300, formulaName: '面积计价' },
  },
}

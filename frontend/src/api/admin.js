import request from './index'

// ==================== 认证 ====================
export const login = (data) => request.post('/admin/login', data)
export const logout = () => request.post('/admin/logout')
export const refreshToken = () => request.post('/admin/refresh-token')
export const changePassword = (data) => request.put('/admin/password', data)
export const forgotPassword = (data) => request.post('/admin/forgot-password', data)

// ==================== 看板 ====================
export const getDashboardOverview = (params) => request.get('/admin/dashboard/overview', { params })
export const getCustomerRank = (params) => request.get('/admin/dashboard/customer-rank', { params })
export const getProductRank = (params) => request.get('/admin/dashboard/product-rank', { params })
export const getReceivableAging = (params) => request.get('/admin/dashboard/receivable-aging', { params })
export const getReceivableAgingDetails = (params) => request.get('/admin/dashboard/receivable-aging/details', { params })
export const getCustomerOrders = (params) => request.get('/admin/dashboard/customer-orders', { params })
export const getDashboardReminders = () => request.get('/admin/dashboard/reminders')

// ==================== 上传 ====================
export const upload = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/upload', form)
}

// ==================== 分类 ====================
export const getCategories = () => request.get('/admin/categories')
export const addCategory = (data) => request.post('/admin/categories', data)
export const updateCategory = (id, data) => request.put(`/admin/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/admin/categories/${id}`)

// ==================== 产品 ====================
export const getProducts = () => request.get('/admin/products')
export const addProduct = (data) => request.post('/admin/products', data)
export const updateProduct = (id, data) => request.put(`/admin/products/${id}`, data)
export const deleteProduct = (id) => request.delete(`/admin/products/${id}`)

// ==================== 案例 ====================
export const getCases = () => request.get('/admin/cases')
export const addCase = (data) => request.post('/admin/cases', data)
export const updateCase = (id, data) => request.put(`/admin/cases/${id}`, data)
export const deleteCase = (id) => request.delete(`/admin/cases/${id}`)

// ==================== 站点配置 ====================
export const getSiteConfig = () => request.get('/admin/site-config')
export const updateSiteConfig = (configJson) => request.put('/admin/site-config', { configJson })

// ==================== 询价 ====================
export const getEnquiries = () => request.get('/admin/enquiries')
export const markRead = (id) => request.patch(`/admin/enquiries/${id}`, { isRead: true })
export const markUnread = (id) => request.patch(`/admin/enquiries/${id}`, { isRead: false })
export const markMeasured = (id, measured) => request.patch(`/admin/enquiries/${id}`, { isMeasured: measured ? 1 : 0 })
export const markStarred = (id, starred) => request.patch(`/admin/enquiries/${id}`, { isStarred: starred ? 1 : 0 })
export const markCompleted = (id, completed) => request.patch(`/admin/enquiries/${id}`, { isCompleted: completed ? 1 : 0 })
export const markAllRead = () => request.post('/admin/enquiries/read-all')
export const updateRemark = (id, remark) => request.patch(`/admin/enquiries/${id}`, { remark })
export const deleteEnquiry = (id) => request.delete(`/admin/enquiries/${id}`)

// ==================== 账号与安全 ====================
export const getSystemInfo = () => request.get('/admin/system-info')
export const getLoginLogs = () => request.get('/admin/login-logs')
export const updateLoginLogRemark = (id, remark) => request.patch(`/admin/login-logs/${id}`, { remark })

// ==================== 客户管理 ====================
export const getCustomers = (params) => request.get('/admin/customers', { params })
export const getCustomerById = (id) => request.get(`/admin/customers/${id}`)
export const addCustomer = (data) => request.post('/admin/customers', data)
export const updateCustomer = (id, data) => request.put(`/admin/customers/${id}`, data)
export const deleteCustomer = (id) => request.delete(`/admin/customers/${id}`)
export const toggleCustomerStar = (id) => request.put(`/admin/customers/${id}/star`)

// ==================== 供应商管理 ====================
export const getSuppliers = (params) => request.get('/admin/suppliers', { params })
export const getSupplierById = (id) => request.get(`/admin/suppliers/${id}`)
export const addSupplier = (data) => request.post('/admin/suppliers', data)
export const updateSupplier = (id, data) => request.put(`/admin/suppliers/${id}`, data)
export const deleteSupplier = (id) => request.delete(`/admin/suppliers/${id}`)
export const toggleSupplierStar = (id) => request.put(`/admin/suppliers/${id}/star`)

// ==================== 商品库存 ====================
export const getCommodities = (params) => request.get('/admin/commodities', { params })
export const getCommodityById = (id) => request.get(`/admin/commodities/${id}`)
export const addCommodity = (data) => request.post('/admin/commodities', data)
export const updateCommodity = (id, data) => request.put(`/admin/commodities/${id}`, data)
export const deleteCommodity = (id) => request.delete(`/admin/commodities/${id}`)
export const toggleCommodityShow = (id) => request.put(`/admin/commodities/${id}/toggle`)
export const getCommodityAlerts = () => request.get('/admin/commodities/alerts')
export const getCommodityInventory = (params) => request.get('/admin/commodities/inventory', { params })
export const getCommodityCategories = () => request.get('/admin/commodities/categories')
export const addCommodityCategory = (data) => request.post('/admin/commodities/categories', data)
export const updateCommodityCategory = (id, data) => request.put(`/admin/commodities/categories/${id}`, data)
export const deleteCommodityCategory = (id) => request.delete(`/admin/commodities/categories/${id}`)

// ==================== 商品细目 ====================
export const getProductTypes = () => request.get('/admin/commodities/product-types')
export const addProductType = (data) => request.post('/admin/commodities/product-types', data)
export const updateProductType = (id, data) => request.put(`/admin/commodities/product-types/${id}`, data)
export const deleteProductType = (id) => request.delete(`/admin/commodities/product-types/${id}`)

// ==================== 计算公式 ====================
export const getFormulas = () => request.get('/admin/pricing-formulas')
export const getFormulaById = (id) => request.get(`/admin/pricing-formulas/${id}`)
export const addFormula = (data) => request.post('/admin/pricing-formulas', data)
export const updateFormula = (id, data) => request.put(`/admin/pricing-formulas/${id}`, data)
export const deleteFormula = (id) => request.delete(`/admin/pricing-formulas/${id}`)

// ==================== 销售订单 ====================
export const getSaleOrders = (params) => request.get('/admin/sale-orders', { params })
export const getSaleOrderDetail = (id) => request.get(`/admin/sale-orders/${id}`)
export const addSaleOrder = (data) => request.post('/admin/sale-orders', data)
export const updateSaleOrder = (id, data) => request.put(`/admin/sale-orders/${id}`, data)
export const deleteSaleOrder = (id) => request.delete(`/admin/sale-orders/${id}`)
export const updateSaleOrderStatus = (id, status) => request.put(`/admin/sale-orders/${id}/status`, { status })
export const toggleSaleOrderCleared = (id) => request.put(`/admin/sale-orders/${id}/toggle-cleared`)
export const getSaleOrderSummary = (params) => request.get('/admin/sale-orders/summary', { params })
export const getSaleOrderItemsBatch = (orderIds) => request.get('/admin/sale-orders/items/batch', { params: { orderIds } })
export const getSaleOrderReport = (params) => request.get('/admin/sale-orders/report', { params })
export const batchDeleteSaleOrders = (ids) => request.post('/admin/sale-orders/batch/delete', { ids })
export const batchUpdateSaleOrderStatus = (ids, status) => request.put('/admin/sale-orders/batch/status', { ids, status })

// ==================== 采购订单 ====================
export const getPurchaseOrders = (params) => request.get('/admin/purchase-orders', { params })
export const getPurchaseOrderDetail = (id) => request.get(`/admin/purchase-orders/${id}`)
export const addPurchaseOrder = (data) => request.post('/admin/purchase-orders', data)
export const updatePurchaseOrder = (id, data) => request.put(`/admin/purchase-orders/${id}`, data)
export const deletePurchaseOrder = (id) => request.delete(`/admin/purchase-orders/${id}`)
export const updatePurchaseOrderStatus = (id, status) => request.put(`/admin/purchase-orders/${id}/status`, { status })
export const togglePurchaseOrderCleared = (id) => request.put(`/admin/purchase-orders/${id}/toggle-cleared`)
export const getPurchaseOrderSummary = (params) => request.get('/admin/purchase-orders/summary', { params })
export const getPurchaseOrderItemsBatch = (orderIds) => request.get('/admin/purchase-orders/items/batch', { params: { orderIds } })
export const getPurchaseOrderReport = (params) => request.get('/admin/purchase-orders/report', { params })

// ==================== 入库管理 ====================
export const getStockInList = (params) => request.get('/admin/stock/in', { params })
export const getStockInDetail = (id) => request.get(`/admin/stock/in/${id}`)
export const addStockIn = (data) => request.post('/admin/stock/in', data)
export const updateStockIn = (id, data) => request.put(`/admin/stock/in/${id}`, data)
export const deleteStockIn = (id) => request.delete(`/admin/stock/in/${id}`)
export const getStockInSummary = (params) => request.get('/admin/stock/in/summary', { params })

// ==================== 出库管理 ====================
export const getStockOutList = (params) => request.get('/admin/stock/out', { params })
export const getStockOutDetail = (id) => request.get(`/admin/stock/out/${id}`)
export const addStockOut = (data) => request.post('/admin/stock/out', data)
export const updateStockOut = (id, data) => request.put(`/admin/stock/out/${id}`, data)
export const deleteStockOut = (id) => request.delete(`/admin/stock/out/${id}`)
export const getStockOutSummary = (params) => request.get('/admin/stock/out/summary', { params })

// ==================== 销售退货 ====================
export const getSaleReturnAnalysis = (params) => request.get('/admin/returns/sale/analysis', { params })
export const getSaleReturns = (params) => request.get('/admin/returns/sale', { params })
export const getSaleReturnDetail = (id) => request.get(`/admin/returns/sale/${id}`)
export const addSaleReturn = (data) => request.post('/admin/returns/sale', data)
export const updateSaleReturn = (id, data) => request.put(`/admin/returns/sale/${id}`, data)
export const deleteSaleReturn = (id) => request.delete(`/admin/returns/sale/${id}`)
export const updateSaleReturnStatus = (id, status) => request.put(`/admin/returns/sale/${id}/status`, { status })
export const getSaleReturnSummary = (params) => request.get('/admin/returns/sale/summary', { params })
export const getSaleReturnItemsBatch = (orderIds) => request.get('/admin/returns/sale/items/batch', { params: { orderIds } })

// ==================== 采购退货 ====================
export const getPurchaseReturnAnalysis = (params) => request.get('/admin/returns/purchase/analysis', { params })
export const getPurchaseReturns = (params) => request.get('/admin/returns/purchase', { params })
export const getPurchaseReturnDetail = (id) => request.get(`/admin/returns/purchase/${id}`)
export const addPurchaseReturn = (data) => request.post('/admin/returns/purchase', data)
export const updatePurchaseReturn = (id, data) => request.put(`/admin/returns/purchase/${id}`, data)
export const deletePurchaseReturn = (id) => request.delete(`/admin/returns/purchase/${id}`)
export const updatePurchaseReturnStatus = (id, status) => request.put(`/admin/returns/purchase/${id}/status`, { status })
export const togglePurchaseReturnCleared = (id) => request.put(`/admin/returns/purchase/${id}/toggle-cleared`)
export const getPurchaseReturnSummary = (params) => request.get('/admin/returns/purchase/summary', { params })
export const getPurchaseReturnItemsBatch = (orderIds) => request.get('/admin/returns/purchase/items/batch', { params: { orderIds } })

// ==================== 收付款 ====================
export const getPayments = (params) => request.get('/admin/finance/payments', { params })
export const getPaymentById = (id) => request.get(`/admin/finance/payments/${id}`)
export const addPayment = (data) => request.post('/admin/finance/payments', data)
export const updatePayment = (id, data) => request.put(`/admin/finance/payments/${id}`, data)
export const deletePayment = (id) => request.delete(`/admin/finance/payments/${id}`)
export const getPaymentSummary = (params) => request.get('/admin/finance/payments/summary', { params })
export const getUnpaidOrders = (params) => request.get('/admin/finance/payments/unpaid', { params })
export const getPaymentReport = (params) => request.get('/admin/finance/payments/report', { params })

// ==================== 售后管理 ====================
export const getAfterSaleOrders = (params) => request.get('/admin/after-sale', { params })
export const getAfterSaleOrderById = (id) => request.get(`/admin/after-sale/${id}`)
export const addAfterSaleOrder = (data) => request.post('/admin/after-sale', data)
export const updateAfterSaleOrder = (id, data) => request.put(`/admin/after-sale/${id}`, data)
export const deleteAfterSaleOrder = (id) => request.delete(`/admin/after-sale/${id}`)
export const updateAfterSaleOrderStatus = (id, status) => request.put(`/admin/after-sale/${id}/status`, { status })
export const getAfterSaleOrderSummary = (params) => request.get('/admin/after-sale/summary', { params })

// ==================== 打印设置 ====================
export const getPrintSetting = (type = 'order') => request.get('/admin/print-setting', { params: { type } })
export const savePrintSetting = (config, type = 'order') => request.put('/admin/print-setting', { config, type })

// ==================== 系统设置 ====================
export const getSystemSettings = () => request.get('/admin/config')
export const saveSystemSettings = (data) => request.put('/admin/config', data)

// ==================== 操作日志 ====================
export const getOperationLogs = (params) => request.get('/admin/operation-logs', { params })

// ==================== 订单编辑锁 ====================
export const acquireOrderLock = (orderType, orderId) => request.post('/admin/order-lock/acquire', null, { params: { orderType, orderId } })
export const releaseOrderLock = (orderType, orderId) => request.post('/admin/order-lock/release', null, { params: { orderType, orderId } })
export const checkOrderLock = (orderType, orderId) => request.get('/admin/order-lock/check', { params: { orderType, orderId } })

// ==================== 数据备份 ====================
export const createBackup = () => request.post('/admin/backup/create', null, { timeout: 300000 })
export const getBackupList = () => request.get('/admin/backup/list')
export const downloadBackup = (filename) => request.get(`/admin/backup/download/${filename}`, { responseType: 'blob', timeout: 300000 })
export const deleteBackup = (filename) => request.delete(`/admin/backup/delete/${filename}`)
export const restoreBackup = (file) => {
  const form = new FormData()
  form.append('file', file)
  // 不要手动设 Content-Type: multipart/form-data —— 手动指定后浏览器不再附加 boundary，
  // 服务端解析失败报 "no multipart boundary" → 服务器内部错误。交给 axios/浏览器自动带 boundary。
  return request.post('/admin/backup/restore', form, { timeout: 300000 })
}

// 恢复已删除的实体（软删除撤回）
export const restoreEntity = (entityType, id) => request.post(`/admin/restore/${entityType}/${id}`)

// ==================== 客户跟进记录 ====================
export const getCustomerFollowups = (params) => request.get('/admin/customer-followups', { params })
export const addCustomerFollowup = (data) => request.post('/admin/customer-followups', data)
export const deleteCustomerFollowup = (id) => request.delete(`/admin/customer-followups/${id}`)

// ==================== 账号管理 ====================
export const getAdminAccounts = () => request.get('/admin/accounts')
export const createAdminAccount = (data) => request.post('/admin/accounts', data)
export const freezeAdminAccount = (id) => request.put(`/admin/accounts/${id}/freeze`)
export const unfreezeAdminAccount = (id) => request.put(`/admin/accounts/${id}/unfreeze`)
export const resetAdminPassword = (id, newPassword) => request.put(`/admin/accounts/${id}/reset-password`, { newPassword })
export const deleteAdminAccount = (id) => request.delete(`/admin/accounts/${id}`)

// ==================== 统一批量导入 ====================
export const importPreview = (data) => request.post('/admin/import/preview', data)
export const importCommit = (data) => request.post('/admin/import/commit', data)

# ADR-001: Excel 导入行号追踪方案

## 状态
已采纳

## 背景
Excel 导入功能中，导入失败时需要向用户显示准确的 Excel 行号以便定位问题。原始实现使用 `i + 1`（mappedRecords 数组索引），但分组订单模式下数组索引是订单序号而非 Excel 行号。

## 决策
在 `mapRows` 解析阶段给每条记录附加 `_excelRow` 元数据（`ri + 2`，即 Excel 行号），分组时通过 `orderFirstRow` Map 追踪每个订单的首行行号，在 API 调用前删除元数据。

## 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. mapRows 阶段附加 `_excelRow` | 行号准确，生命周期完整 | 需要修改共享工具函数 |
| B. ImportExcelDialog 中自行计算 | 不改工具函数 | 无法准确追踪跳过空行后的行号 |
| C. 返回原始行号数组 | 数据干净 | 需要改 mapRows 返回值结构 |

## 实现
```javascript
// exportExcel.js - mapRows
if (hasData) {
  record._excelRow = ri + 2  // Excel 行号（1-indexed，+1 for header）
  records.push(record)
}

// ImportExcelDialog.vue - startImport
const excelRow = record._excelRow || (i + 2)
delete record._excelRow  // 防止泄漏到 API
```

## 影响
- `_excelRow` 是内部元数据，约定以下划线开头
- 在 `transformRecord` 调用前删除，不影响后端
- 分组订单显示首行行号，非分组记录显示精确行号

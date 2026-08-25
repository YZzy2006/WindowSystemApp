# ADR-002: 排序状态持久化使用独立 sessionStorage key

## 状态
已采纳

## 背景
`useTableSort` 组合式函数需要将排序状态持久化到 sessionStorage。各视图已有 `_filters` 后缀的 key 用于筛选器持久化（如 `sale_orders_filters`）。排序状态应存在哪里？

## 决策
使用独立的 sessionStorage key，格式为 `${baseKey}_sort`（如 `sale_orders_filters_sort`），与筛选器数据隔离。

## 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. 共用 `_filters` key | 一个 key 存所有状态 | 视图的 `syncFiltersToUrl` 写入 `{}` 新对象会覆盖排序数据 |
| B. 独立 `_sort` key | 互不干扰，composable 自管理 | 多一个 sessionStorage 条目 |
| C. 存入 URL query | 刷新不丢失 | 排序不是用户显式筛选，污染 URL |

## 实现
```javascript
// useTableSort.js
const sortKey = opts.storageKey ? `${opts.storageKey}_sort` : null
if (sortKey) {
  // 读取
  const saved = JSON.parse(sessionStorage.getItem(sortKey) || '{}')
  // 写入
  watch([sortField, sortDir], () => {
    sessionStorage.setItem(sortKey, JSON.stringify({
      sortField: sortField.value || undefined,
      sortDir: sortDir.value !== 'asc' ? sortDir.value : undefined,
    }))
  })
}
```

## 影响
- 11 个视图各自传递 `storageKey` 参数
- sessionStorage 中每个视图有 2 个 key（`_filters` + `_sort`）
- 排序状态在页面关闭后保留，重新打开时恢复

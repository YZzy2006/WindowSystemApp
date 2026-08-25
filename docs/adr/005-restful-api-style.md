# ADR-005: API 风格决策 — RESTful

## 状态
已采纳

## 背景
前后端分离架构需要统一的 API 设计风格。

## 决策
采用 RESTful 风格，统一响应格式 `ApiResponse.success(data)`。

## 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. RESTful | 简单直观，HTTP 语义化，缓存友好 | 复杂查询需要多个端点 |
| B. GraphQL | 按需查询，无过度获取 | 学习曲线，缓存复杂，需要额外服务端库 |
| C. RPC | 灵活，适合内部调用 | 缺乏标准，文档依赖工具 |

## 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 路径规范
- `GET /api/admin/sale-orders` — 列表（分页参数 `page`, `size`）
- `GET /api/admin/sale-orders/{id}` — 详情
- `POST /api/admin/sale-orders` — 新增
- `PUT /api/admin/sale-orders/{id}` — 修改
- `DELETE /api/admin/sale-orders/{id}` — 删除

## 影响
- Controller 统一返回 `ResponseEntity.ok(ApiResponse.success(data))`
- 异常统一由 `@ExceptionHandler` 捕获并格式化
- 分页参数统一：`page`（从1开始）、`size`（默认20）

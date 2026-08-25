# ADR-003: JWT 双 Token 认证方案

## 状态
已采纳

## 背景
管理系统需要认证方案，需在无状态、安全性、用户体验之间平衡。

## 决策
采用 JWT 双 Token 方案：Access Token（2h）+ Refresh Token（7d）。

## 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. Session | 服务端可控，即时失效 | 需要共享存储，水平扩展复杂 |
| B. JWT 单 Token | 简单无状态 | 过期前无法续期，用户体验差 |
| C. JWT 双 Token | 无状态 + 自动续期 | 实现复杂度中等 |
| D. OAuth2 | 标准化，支持第三方 | 过重，内部系统不需要 |

## 实现
- Access Token: 2h 过期，放 `Authorization: Bearer xxx`
- Refresh Token: 7d 过期，放 HttpOnly Cookie
- 401 时自动用 Refresh Token 换新 Access Token
- Refresh Token 失效时强制登出

## 影响
- 前端拦截器需处理 401 自动续期
- 需要防并发刷新（队列机制）
- 登出需同时清除两个 Token

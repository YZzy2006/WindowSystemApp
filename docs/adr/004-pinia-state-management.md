# ADR-004: 前端状态管理选型 — Pinia

## 状态
已采纳

## 背景
Vue 3 项目需要全局状态管理（用户认证、主题、通知等），需在 Vuex 4 和 Pinia 之间选择。

## 决策
选择 Pinia。

## 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. Vuex 4 | 生态成熟，文档丰富 | 模块嵌套复杂，mutation 冗余，TypeScript 支持弱 |
| B. Pinia | Vue 官方推荐，Composition API 原生支持，无 mutation，TypeScript 友好 | 生态稍弱（已追平） |
| C. 纯 provide/inject | 无额外依赖 | 无 DevTools 支持，无持久化插件 |

## 关键优势
- 无 `mutation`，直接修改 state（`store.token = 'xxx'`）
- `defineStore` 返回普通对象，可 tree-shaking
- `storeToRefs` 解构保持响应式
- 插件生态：`pinia-plugin-persistedstate` 持久化

## 影响
- 所有 store 使用 Composition API 风格（`defineStore('id', () => {...})`）
- 持久化用 `localStorage` 手动实现或插件
- DevTools 集成开箱即用

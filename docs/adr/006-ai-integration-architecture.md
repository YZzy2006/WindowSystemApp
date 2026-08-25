# ADR-006: AI 集成架构决策

## 状态
已采纳

## 背景
系统集成 AI/LLM 能力（智能客服、内容生成等），需要选择调用方式和容错策略。

## 决策
使用 WebClient 异步调用 + CompletableFuture.orTimeout 超时控制 + 降级策略。

## 方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| A. RestTemplate 同步 | 简单直观 | 阻塞线程，不适合高并发 |
| B. WebClient 异步 | 非阻塞，支持流式 | 学习曲线 |
| C. Spring AI 框架 | 标准化抽象 | 框架成熟度不够 |

## 实现模式
```java
CompletableFuture.supplyAsync(() -> callLLM(prompt))
    .orTimeout(30, TimeUnit.SECONDS)
    .exceptionally(ex -> fallbackResponse(ex))
```

## 容错策略
- **超时**: 30 秒硬超时
- **重试**: 最多 2 次，指数退避
- **降级**: 返回预设模板或缓存结果
- **熔断**: 连续 5 次失败后熔断 5 分钟

## 影响
- LLM 响应 DTO 必须加 `@JsonIgnoreProperties(ignoreUnknown = true)`
- 异步调用中不能使用 ThreadLocal（通过参数传递上下文）
- 需要独立的线程池隔离 AI 调用和业务逻辑

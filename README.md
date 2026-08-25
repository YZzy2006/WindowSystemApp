<div align="center">

# 🚪 顺居门业管理系统 · Window System

**门窗门店一体化管理平台：销售订单 · 采购库存 · 财务应收 · 产品计价 · 公式引擎 · 打印报表 · 桌面客户端**

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.7-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.5-blue)
![Vue](https://img.shields.io/badge/Vue%203-4FC08D)
![Tauri](https://img.shields.io/badge/Tauri-2.0-24c8db)
![H2](https://img.shields.io/badge/Database-H2%2FMySQL-yellow)
![JWT](https://img.shields.io/badge/Auth-JWT-black)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**覆盖销售订单、采购进货、库存出入库、客户供应商、财务应收、产品计价、打印报表、数据备份等完整业务功能，前后端一体化部署，并提供 Windows 桌面安装包（MSI）。**

</div>

---

## ✨ 功能亮点

| | | |
|---|---|---|
| 🧾 **销售订单管理** | 📦 **采购与库存** | 💰 **财务与应收** |
| 下单 → 收款 → 发货 → 售后退换货 | 采购入库 / 出库 / 库存核算 | 收款流水 / 应收账龄 / 付款报表 |
| 📐 **产品与计价引擎** | 🖨️ **打印与导出** | 🔐 **数据备份恢复** |
| 多类门窗产品 + 可配置计价公式 | 订单打印预览 / Excel 导入导出 | SQL 一键备份与还原 |
| 🖥️ **桌面客户端** | 🛡️ **安全体系** | ⚡ **低配优化** |
| Tauri 2 打包 MSI 安装包 | JWT + AES 加密 + 操作审计 | 崩溃日志自动保存 / JVM 自适应 |

---

## 📑 目录

- [一、项目背景](#一项目背景)
- [二、系统架构](#二系统架构)
- [三、核心功能模块](#三核心功能模块)
- [四、关键技术点与难点解决](#四关键技术点与难点解决)
- [五、数据模型设计](#五数据模型设计)
- [六、工程结构一览](#六工程结构一览)
- [七、快速运行指南](#七快速运行指南)
- [八、环境变量配置](#八环境变量配置)
- [九、常见问题 FAQ](#九常见问题-faq)
- [十、项目亮点总结](#十项目亮点总结)
- [十一、项目收获与反思](#十一项目收获与反思)

---

## 一、项目背景

门窗定制行业业务链条长、单据流转频繁：从**客户询价、产品报价、下单收款，到采购进货、库存出入、财务对账、售后处理**，每个环节都依赖大量手工作业与纸质单据。

本项目以此为切入点，构建了一套覆盖**门窗定制企业核心业务全流程**的一体化管理平台，并打包为 **Windows 桌面应用**（Tauri 生成 MSI 安装包），单机离线即可使用，数据安全可控。

业务闭环如下：

- 🧾 客户询价 → 产品报价 → 销售下单 → 收款 → 发货 → 售后退换
- 📦 采购进货 → 库存出入库 → 库存核算
- 💰 收款 / 支出 → 应收账龄 → 对账与财务报表
- 🖨️ 订单打印预览 / 标签打印 / Excel 导入导出

---

## 二、系统架构

### 1. 技术选型总览

| 层面 | 技术 | 选型理由 |
|------|------|----------|
| 外壳 | **Tauri 2** | 轻量桌面壳，打包 MSI，WebView2 渲染，内存占用低 |
| 语言 | Java 17 | 稳定、生态成熟，适合企业级后端 |
| 框架 | Spring Boot 3.3.7 | 自动装配 + 约定优于配置，快速构建 |
| 持久层 | MyBatis-Plus 3.5.5 | 通用 CRUD + 逻辑删除 + 分页插件 |
| 数据库 | **H2 文件库**（默认）/ MySQL 8.0 | 单机离线可用；生产可切换 MySQL |
| 认证 | JWT (jjwt 0.12.6) | 无状态令牌，拦截器校验 |
| 密码加密 | BCrypt | 加盐哈希，抗彩虹表攻击 |
| 字段加密 | AES-256-GCM | 敏感配置加密存储 |
| 前端 | Vue 3.4 + Element Plus + Pinia + ECharts | 组件化 + 状态管理 + 数据可视化 |
| 打印 | html-to-image / jsPDF / ExcelJS | 打印预览 / 图片 / Excel |

### 2. 部署形态

- **桌面应用**：Tauri 壳内嵌 Spring Boot JAR + WebView2 前端，启动后访问 `localhost:8080`。单机可离线运行（H2 文件库），数据保存在安装目录/APPDATA。
- **前后端一体化**：Vue 构建产物输出到 `src/main/resources/static/`，与后端**同源部署**，单 JAR 打包。

### 3. 分层架构

采用经典**四层架构**，职责清晰、易于测试与扩展：

```
Controller（接口层） → Service（业务层） → Mapper（MyBatis-Plus 数据访问） → Entity → H2/MySQL
                          ↕
   JWT 拦截器 / AOP 操作日志 / 全局异常 / 安全响应头
```

- **Controller 层**：27 个 REST 控制器、190+ 接口，统一返回 `Result`（code / msg / data）
- **Service 层**：核心业务逻辑与事务边界（`@Transactional`）
- **Mapper 层**：MyBatis-Plus BaseMapper + 自定义 SQL，逻辑删除全局配置
- **横切关注点**：JWT Token 拦截器、通用接口限流、AOP 操作日志审计、全局异常处理、安全响应头

---

## 三、核心功能模块

### 1️⃣ 销售订单与售后

- 销售订单全流程：下单 → 收款 → 发货 → 完成，状态机驱动
- 订单明细（产品 / 数量 / 单价 / 金额）、收款记录关联
- **销售退货**：售后退货入库、退款登记、退换货分析
- **售后订单**：独立售后模块，含退换货原因与处理进度
- **金额自由编辑（砍价）**：自动计价后可点金额手动修改，预览 / 打印一致
- **单据锁 `OrderLock`**：并发编辑单据时锁冲突检测，防止多人同时修改互相覆盖

### 2️⃣ 采购与库存

- 采购订单 / 采购退货 / 采购入库单，供应商往来对账
- 库存管理：入库单、出库单、库存核算，商品级库存
- 商品与商品分类管理

### 3️⃣ 客户、供应商与跟进

- 客户档案（联系方式 / 地址 / 来源），供应商档案
- **客户跟进记录**：跟进时间、方式、内容、下次跟进提醒，销售过程留痕

### 4️⃣ 财务与报表

- 收款流水 / 支出流水统一管理
- **应收账龄分析**：按账期拆分应收余额，逾期预警
- 付款报表 / 订单营收分析 / 客户贡献排名 / 产品销量排名
- 运营仪表盘：核心经营指标实时看板

### 5️⃣ 产品、计价公式与打印

- 门窗产品管理：断桥铝门窗 / 推拉门 / 平开门等多品类，规格参数结构化存储
- **计价公式引擎**：公式可配置（`宽*高/1000000`、`(宽+高)/1000`、`高*2/1000` 等），支持加减乘除 / 括号 / 取模 / 多变量，**中文括号与乘除号输入自动转英文**
- **打印系统**：订单打印预览（标题 / 表体 / 汇总分区自定义字号与线条）、Excel 导入导出、图片导出
- **包边联动**：选推拉门自动弹出单包 / 双包，包边尺寸 / 米数 / 樘数联动

### 6️⃣ 系统安全与运维

- **JWT 认证**：Token 拦截器校验，同账号单会话（tokenId 轮换，旧会话自动失效）
- **通用接口限流**：滑动窗口 + IP 维度
- **操作日志审计**：AOP 自动埋点，记录操作人、时间、URI、参数与结果
- **登录日志**：登录成功 / 失败留痕
- **数据备份与恢复**：SQL 一键备份、一键还原、备份文件管理；**恢复前自动安全备份**，失败中止保护
- **崩溃日志**：应用异常退出后自动保存崩溃日志到桌面，便于远程定位
- **低配优化**：JVM 堆按机器内存自适应，避免低配机卡死

---

## 四、关键技术点与难点解决

### 1. 单据并发编辑冲突（OrderLock）

多员工同时编辑同一单据是门窗工厂的典型场景。采用**单据锁**机制：编辑前锁定单据，他人操作时提示冲突并携带锁持有者信息，保存时校验锁归属，杜绝互相覆盖。

### 2. 自定义计价公式引擎

支持任意四则运算 + 括号 + 取模 + 多变量的公式表达式，前后端**双解析器独立实现且行为一致**：

- 前端 `safeEval`（递归下降解析器）与后端 `SafeMathEvaluator`（Java 递归下降）逐字符对齐
- **面积列 = 实际公式结果**，与金额完全一致
- 中文输入法符号（`（）×÷－`）**实时自动转英文**，杜绝解析失败
- 公式参数默认值可配置，前后端默认值回填一致

### 3. 崩溃恢复与数据安全

- 崩溃日志通过"会话标记"检测异常退出，下次启动自动保存日志到桌面
- 备份 / 恢复全链路：`H2 SCRIPT` 导出 SQL，恢复前自动安全备份，失败中止
- JVM 内存按机器实际内存自适应（256 / 384 / 512MB），低配机不再集体卡死

### 4. 安全设计（可放心开源）

- ✅ BCrypt 加盐加密存储密码，不存明文
- ✅ **数据库密码、JWT 密钥、AES 密钥全部环境变量注入，代码零明文**
- ✅ 本地含密钥文件（`application-local/dev.yml`）、SQL 数据、构建产物均通过 `.gitignore` 排除，仓库可安全公开
- ✅ 通用接口限流防刷；安全响应头

---

## 五、数据模型设计

核心业务表（H2/MySQL）：

| 域 | 表 / 实体 | 职责 |
|----|-----------|------|
| 用户权限 | `admin` | 管理员账号（super_admin / admin）、角色与冻结状态 |
| 销售域 | `sale_order` / `sale_order_item` / `sale_return` / `sale_return_item` / `after_sale_order` | 销售订单、明细、退货、售后 |
| 采购域 | `purchase_order` / `purchase_order_item` / `purchase_return` / `purchase_return_item` / `supplier` | 采购订单、明细、退货、供应商 |
| 库存域 | `commodity` / `commodity_category` / `stock_in` / `stock_in_item` / `stock_out` / `stock_out_item` | 商品、分类、出入库 |
| 产品域 | `product` / `product_type` / `category` / `pricing_formula` / `case_info` | 门窗产品、型号、分类、计价公式、案例 |
| 客户域 | `customer` / `customer_followup` / `enquiry` | 客户档案、跟进记录、询价单 |
| 财务域 | `payment` | 收款 / 支出统一流水 |
| 系统域 | `login_log` / `operation_log` / `sys_config` / `site_config` / `print_setting` / `order_lock` / `order_sequence` | 登录日志、操作日志、系统配置、站点配置、打印设置、单据锁、单号序列 |

**核心 ER 关系：**

```
Customer 1──N SaleOrder 1──N SaleOrderItem
SaleOrder 1──N Payment          SaleOrder 1──1 OrderLock（单据锁）
Commodity 1──N StockIn / StockOut（出入库）
Product 1──N ProductType / PricingFormula
```

---

## 六、工程结构一览

```
window-system
├── src/main/java/com/window/
│   ├── controller/    # 27 个 REST 控制器
│   ├── service/       # 60 个 Service 类（业务逻辑与事务）
│   ├── mapper/        # MyBatis-Plus Mapper
│   ├── entity/        # 34 个实体
│   ├── dto/           # 请求 / 响应 DTO
│   ├── common/        # JWT / SafeMathEvaluator / 工具
│   ├── config/        # 拦截器 / 全局异常 / AOP审计 / Web
│   ├── util/          # AES 加解密 / SSRF 防护
│   └── WindowApplication.java
├── frontend/          # Vue 3 SPA（36 个视图 + 组件 + 打印）
├── src-tauri/         # Tauri 2 桌面壳（MSI 打包）
├── src/main/resources/db/   # 数据库初始化与迁移脚本 migration-*.sql
├── docs/adr/          # 架构决策记录（MADR）
└── README.md
```

---

## 七、快速运行指南

### 环境要求

JDK 17+ · Maven 3.6+ · Node 18+（前端开发模式）· Rust（Tauri 打包）

### 1. 后端（开发模式）

```bash
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
# 编辑该文件，替换示例密钥为你自己的密钥
mvn spring-boot:run     # http://localhost:8080
```

### 2. 前端开发模式（可选）

```bash
cd frontend
npm install
npm run dev             # http://localhost:3000（Vite 代理 /api 到 8080）
```

### 3. 构建 Windows MSI（桌面安装包）

```bash
build-msi.bat           # 一键：前端 build → mvn package → cargo tauri build
# 或手动：
cd frontend && npm run build
mvn package -DskipTests
cp target/*.jar src-tauri/resources/
cd src-tauri && cargo tauri build   # 产物在 target/release/bundle/msi/
```

---

## 八、环境变量配置

| 变量 | 必填 | 默认值 | 说明 |
|------|:---:|--------|------|
| `SPRING_PROFILES_ACTIVE` | ❌ | `local` | 激活的配置文件（local / dev / prod） |
| `JWT_SECRET` | ✅ | — | JWT 签名密钥（≥32 字节） |
| `AES_KEY` | ✅ | — | AES-256-GCM 密钥，Base64 编码 32 字节，用于敏感字段加密 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | ❌ | localhost / 3306 / window_db | 生产数据库连接（prod profile） |
| `DB_USERNAME` / `DB_PASSWORD` | ✅(prod) | — | 生产数据库账号密码 |

> 🔒 所有密钥均通过环境变量注入，代码零明文；本地开发参考 `application-local.example.yml`。

---

## 九、常见问题 FAQ

**Q1：启动报 `app.encryption.secret 未配置`？**
未配置 `AES_KEY` 环境变量（或本地 `application-local.yml` 未设置）。请参考 `application-local.example.yml` 配置后重启。

**Q2：登录报 JWT 相关错误？**
`jwt.secret` 未配置或密钥长度不足。请设置 ≥32 字节的签名密钥。

**Q3：桌面应用装在哪、数据在哪？**
MSI 默认装 Program Files（数据自动存 `%APPDATA%\com.shunjumc.window-system`）；装 D 盘则数据在安装目录 `data/`。数据都是确定性的，不会随启动方式变化。

**Q4：桌面卡死 / 异常退出？**
新版本已做低配机优化（JVM 内存自适应）。若异常退出，重启后桌面上会出现「崩溃日志」文件，可发送给开发者定位。

**Q5：自定义公式为什么算不对？**
确认公式使用英文括号和运算符（中文括号/乘除号会自动转英文）；公式变量名要与参数名称一致（宽/高/墙厚/樘数等）。

**Q6：如何备份数据？**
「数据备份」页点击创建备份 → 下载 .sql 文件保存；恢复前系统会自动生成安全备份。

---

## 十、项目亮点总结

| 维度 | 亮点 |
|------|------|
| 🏗️ 业务完整度 | 询价 → 报价 → 订单 → 收款 → 发货 → 售后 → 财务对账，全链路闭环 |
| 📦 进销存一体化 | 采购 / 库存 / 商品核算 |
| 🖨️ 打印与导出 | 打印预览分区自定义 / Excel 导入导出 / 图片 |
| 📐 计价公式引擎 | 自定义公式 + 包边联动 + 前后端一致 + 中文符号自动转换 |
| 🖥️ 桌面交付 | Tauri 打包 MSI，单机离线可用，数据可控 |
| 🛡️ 安全性 | JWT + 单会话 + BCrypt + AES-256-GCM + 操作审计 + 密钥环境变量化 |
| 🔐 运维能力 | SQL 备份恢复 + 崩溃日志 + 低配优化 + 单据并发锁 |
| 📐 工程规范性 | 四层架构、统一返回、全局异常、AOP 日志审计、数据库迁移脚本、ADR |

---

## 十一、项目收获与反思

> 这个项目让我将 **Spring Boot 分层架构、MyBatis-Plus 数据访问、JWT 认证、并发单据锁、Tauri 桌面打包、打印系统、自定义计价公式引擎、前端 Vue 3 状态管理** 完整串联成一条线。

也让我深刻认识到：

> **"能用"的系统 与 "安全、规范"的系统之间，差的正是那些看不见的细节——密钥环境变量化、数据库迁移、操作审计、单据并发控制、崩溃恢复。**

---

## 🙏 致谢

- **前端框架**：[Vue 3](https://vuejs.org/) · [Element Plus](https://element-plus.org/) · [ECharts](https://echarts.apache.org/)
- **后端框架**：[Spring Boot](https://spring.io/projects/spring-boot) · [MyBatis-Plus](https://baomidou.com/)
- **桌面壳**：[Tauri](https://tauri.app/) · [WebView2](https://developer.microsoft.com/microsoft-edge/webview2/)
- **数据库**：H2 · MySQL

> 📌 本仓库遵循 MIT 协议，未包含任何真实业务数据、客户隐私与密钥配置（已通过 `.gitignore` 排除）。
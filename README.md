# 顺居门业管理系统 (Window System)

门窗门店一体化管理桌面应用：进销存、订货、收付款、售后、打印报表全套流程。

## 技术栈

- **后端**：Spring Boot 3.3.7 (Java 17) + MyBatis-Plus + H2/MySQL
- **前端**：Vue 3.4 (Composition API) + Element Plus + ECharts
- **桌面壳**：Tauri 2 (生成 Windows MSI 安装包)
- **数据库**：默认 H2 文件库（单机离线可用），可切换 MySQL

## 目录结构

```
├── src/                  # Spring Boot 后端（Java 17）
│   └── main/
│       ├── java/com/window/   # 控制器/服务/实体/配置
│       └── resources/
│           ├── application.yml            # 主配置（密钥用环境变量占位）
│           ├── application-local.example.yml  # 本地配置模板（复制为 application-local.yml 使用）
│           ├── application-dev.example.yml   # 开发配置模板
│           └── db/                        # H2 初始化 SQL + 增量迁移
├── frontend/            # Vue 3 前端源码
├── src-tauri/           # Tauri 桌面壳（MSI 打包）
├── docs/adr/            # 架构决策记录 (MADR)
├── pom.xml              # Maven 构建
├── build-msi.bat        # 一键构建 MSI
└── build-local.bat      # 本地开发构建
```

## 快速开始

### 后端
```bash
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
# 编辑该文件，替换示例密钥为你自己的密钥
mvn spring-boot:run
```

### 前端
```bash
cd frontend
npm install
npm run dev   # 开发模式（代理到 http://localhost:8080）
```

### 构建 Windows MSI（需 Rust + Tauri 2）
```bash
build-msi.bat
# 或手动：
cd frontend && npm run build
mvn package -DskipTests
cd ../ && cp target/*.jar src-tauri/resources/
cd src-tauri && cargo tauri build
```

## 配置说明

- **密钥**：`jwt.secret` 与 `app.encryption.secret` 通过环境变量注入（prod），本地开发请复制 `application-local.example.yml` 并替换示例密钥。真实密钥环境变量：`JWT_SECRET`、`AES_KEY`。
- **数据库**：默认 `application-local.yml` 使用 H2 文件库（`./data/window_db`）；`application-prod.yml` 使用 MySQL（环境变量 `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`）。

## 许可证

[MIT](LICENSE)
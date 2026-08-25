@echo off
chcp 65001 >/dev/null
echo ========================================
echo  顺居门业管理系统 - MSI 安装包构建
echo ========================================
echo.

echo [1/4] 清理 WiX 缓存（保证每次 ProductCode 不同，无需改版本号即可升级）...
del /q "%~dp0src-tauri\target\release\wix\x64\main.wxs" 2>nul
del /q "%~dp0src-tauri\target\release\wix\x64\main.wixobj" 2>nul

echo [2/4] 构建前端...
cd /d "%~dp0frontend"
call npm run build
if errorlevel 1 (
    echo frontend build failed
    pause
    exit /b 1
)

echo [3/4] 打包 JAR...
cd /d "%~dp0"
call mvn clean package -DskipTests
if errorlevel 1 (
    echo JAR build failed
    pause
    exit /b 1
)

echo [4/4] 复制 JAR 到 Tauri 资源目录并构建 MSI...
copy /Y "target\window-system-1.0.0.jar" "src-tauri\resources\"
if errorlevel 1 (
    echo JAR copy failed
    pause
    exit /b 1
)
cd /d "%~dp0src-tauri"
call cargo tauri build
if errorlevel 1 (
    echo MSI build failed
    pause
    exit /b 1
)
echo.

echo ========================================
echo  build complete!
echo  output: src-tauri\target\release\bundle\msi\
echo  新 MSI 每次自动生成不同 ProductCode，可直接覆盖升级，无需改版本号
echo ========================================
pause

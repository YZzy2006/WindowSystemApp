@echo off
chcp 65001 >nul
echo ========================================
echo  顺居门业管理系统 - 本地版构建
echo ========================================
echo.

echo [1/2] 构建前端...
cd /d "%~dp0frontend"
call npm run build
if errorlevel 1 (
    echo 前端构建失败！
    pause
    exit /b 1
)
echo 前端构建完成。
echo.

echo [2/2] 打包 JAR...
cd /d "%~dp0"
call mvn clean package -DskipTests
if errorlevel 1 (
    echo JAR 打包失败！
    pause
    exit /b 1
)
echo.

echo ========================================
echo  构建完成！
echo  产出: target\window-system-1.0.0.jar
echo.
echo  启动命令:
echo  java -jar target\window-system-1.0.0.jar --spring.profiles.active=local
echo.
echo  然后浏览器访问: http://localhost:8080
echo  登录账号: super_admin / admin123
echo ========================================
pause

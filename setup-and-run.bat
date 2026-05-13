@echo off
setlocal enabledelayedexpansion
title FileNameTagTool 一键部署

:: ============================================
:: FileNameTagTool 一键部署脚本
:: 功能：注册右键菜单、配置图标路径、启动工具
:: ============================================

:: ============================================
:: 步骤1：检测管理员权限
:: ============================================
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo ========================================
    echo  需要管理员权限才能配置右键菜单
    echo ========================================
    echo.
    echo  请右键点击此脚本，选择「以管理员身份运行」
    echo.
    pause
    exit /b 1
)

cd /d "%~dp0"

:: ============================================
:: 步骤2：设置路径变量
:: ============================================
set "SCRIPT_DIR=%~dp0"
set "JAR_PATH=%SCRIPT_DIR%target\filename-tagtool-1.0.0.jar"
set "ICO_PATH=%SCRIPT_DIR%ico"
set "REG_FILE=%SCRIPT_DIR%temp_register.reg"
set "PS1_FILE=%SCRIPT_DIR%gen_reg.ps1"

set "ICO_PATH_SLASH=%ICO_PATH:\=/%"

:: ============================================
:: 步骤3：检测Java环境
:: ============================================
echo [1/4] 检测Java环境...

set "JAVAW="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" (
        set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
    )
)

if not defined JAVAW (
    where javaw >nul 2>&1
    if !errorLevel! equ 0 (
        for /f "delims=" %%i in ('where javaw') do (
            set "JAVAW=%%i"
            goto :found_javaw
        )
    )
)

:found_javaw
if not defined JAVAW (
    echo.
    echo [错误] 未找到 javaw.exe
    echo 请安装 JDK 8 或更高版本。
    echo.
    pause
    exit /b 1
)

echo       Java路径: %JAVAW%

:: ============================================
:: 步骤4：生成并导入注册表
:: ============================================
echo [2/4] 配置右键菜单注册表...

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1_FILE%" -TemplateReg "%SCRIPT_DIR%addRightMenu.reg" -OutputReg "%REG_FILE%" -JavawPath "%JAVAW%" -JarPath "%JAR_PATH%" -IcoPath "%ICO_PATH%"
if %errorLevel% neq 0 (
    echo.
    echo [错误] 生成注册表文件失败
    del "%REG_FILE%" >nul 2>&1
    pause
    exit /b 1
)

reg import "%REG_FILE%" >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo [错误] 注册表导入失败
    del "%REG_FILE%" >nul 2>&1
    pause
    exit /b 1
)

del "%REG_FILE%" >nul 2>&1
echo       注册表配置完成

:: ============================================
:: 步骤5：创建/更新配置文件
:: ============================================
echo [3/4] 配置图标路径...

set "CONFIG_DIR=%USERPROFILE%\.filenametagtool"
set "CONFIG_FILE=%CONFIG_DIR%\filename-tagtool.conf"

if not exist "%CONFIG_DIR%" (
    mkdir "%CONFIG_DIR%"
)

if exist "%CONFIG_FILE%" (
    findstr /c:"iconPath=" "%CONFIG_FILE%" >nul 2>&1
    if !errorLevel! equ 0 (
        set "TEMP_FILE=%CONFIG_DIR%\temp_conf.tmp"
        (
            for /f "usebackq delims=" %%a in ("%CONFIG_FILE%") do (
                set "line=%%a"
                echo !line! | findstr /b /c:"iconPath=" >nul 2>&1
                if !errorLevel! equ 0 (
                    echo iconPath=%ICO_PATH_SLASH%
                ) else (
                    echo %%a
                )
            )
        ) > "!TEMP_FILE!"
        move /y "!TEMP_FILE!" "%CONFIG_FILE%" >nul 2>&1
    ) else (
        echo. >> "%CONFIG_FILE%"
        echo iconPath=%ICO_PATH_SLASH% >> "%CONFIG_FILE%"
    )
    echo       配置文件已更新
) else (
    (
        echo # FileNameTagTool 配置文件
        echo iconPath=%ICO_PATH_SLASH%
    ) > "%CONFIG_FILE%"
    echo       配置文件已创建
)

:: ============================================
:: 步骤6：启动工具
:: ============================================
echo [4/4] 启动标签管理工具...
echo.

start "" javaw -jar "%JAR_PATH%" manage %SCRIPT_DIR%

echo ============================================
echo  部署完成！
echo ============================================
echo.
echo  已完成：
echo    [√] 右键菜单注册
echo    [√] 图标路径配置
echo    [√] 标签管理工具已启动
echo.
echo  使用说明：
echo    - 右键点击文件：可进行标签操作
echo    - 右键文件夹：管理该目录的文件标签
echo.
echo  如需更新注册表，请重新运行此脚本。
echo.

timeout /t 3 >nul

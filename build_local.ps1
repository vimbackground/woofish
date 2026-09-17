param(
    [ValidateSet("all", "apk", "android", "desktop", "windows")]
    [string]$Target = "all"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 自动检测与配置 JDK 17 环境 (确保拥有 javac.exe)
$knownJdks = @(
    "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot",
    [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine"),
    [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
)
$validJdk = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
    $validJdk = $env:JAVA_HOME
} else {
    foreach ($candidate in $knownJdks) {
        if ($candidate -and (Test-Path (Join-Path $candidate "bin\javac.exe"))) {
            $validJdk = $candidate
            break
        }
    }
}
if ($validJdk) {
    $env:JAVA_HOME = $validJdk
    $env:PATH = "$validJdk\bin;$env:PATH"
}

# 自动检测并补齐 Android SDK 环境变量
if (-not $env:ANDROID_HOME) {
    if (Test-Path "D:\Android\Sdk") {
        $env:ANDROID_HOME = "D:\Android\Sdk"
    } elseif (Test-Path (Join-Path $PSScriptRoot "local.properties")) {
        $lp = Get-Content (Join-Path $PSScriptRoot "local.properties") -Raw
        if ($lp -match "sdk\.dir\s*=\s*(.+)") {
            $env:ANDROID_HOME = $matches[1].Trim().Replace("\\", "\")
        }
    }
}

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   正念木鱼 (woofish) 本地自动化构建系统" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "目标构建模式: $Target" -ForegroundColor Gray
Write-Host "Java 环境: $env:JAVA_HOME" -ForegroundColor Gray
if ($env:ANDROID_HOME) {
    Write-Host "Android SDK: $env:ANDROID_HOME" -ForegroundColor Gray
}
Write-Host ""

# 1. 提取版本号
$gradlePath = Join-Path $PSScriptRoot "modules\windows\build.gradle.kts"
$APP_VER = "1.2.0"
if (Test-Path $gradlePath) {
    $vLine = Get-Content $gradlePath | Where-Object { $_ -match "packageVersion" } | Select-Object -First 1
    if ($vLine -and $vLine.Contains('"')) {
        $APP_VER = ($vLine -split '"')[1].Trim()
    }
}
Write-Host "[1/4] 当前目标构建版本: $APP_VER" -ForegroundColor Green
Write-Host ""

# 2. 准备 _Dist 目录
Write-Host "[2/4] 初始化 _Dist 目录..." -ForegroundColor Green
$DistDir = Join-Path $PSScriptRoot "_Dist"
$WinDist = Join-Path $DistDir "windows\woofish"
$AndroidDist = Join-Path $DistDir "android"

New-Item -ItemType Directory -Force -Path (Join-Path $DistDir "windows") | Out-Null
New-Item -ItemType Directory -Force -Path $AndroidDist | Out-Null

$gradlew = Join-Path $PSScriptRoot "gradlew.bat"

# 3. 编译 Windows 桌面便携版
if ($Target -in @("all", "desktop", "windows")) {
    Write-Host "[3/4] 编译 Windows 桌面 Release 便携版 (启用 ProGuard 压缩)..." -ForegroundColor Green
    & $gradlew ":desktop:packageReleaseAppImage"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] Windows 桌面端构建失败，退出码: $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }

    $WinSrc = Join-Path $PSScriptRoot "build\windows\compose\binaries\main-release\app\woofish"
    if (-not (Test-Path $WinSrc)) {
        Write-Host "[错误] 未找到构建输出产物: $WinSrc" -ForegroundColor Red
        exit 1
    }

    Write-Host "正在归档 Windows 便携版至: $WinDist ..." -ForegroundColor Gray
    if (Test-Path $WinDist) {
        Remove-Item -Recurse -Force $WinDist
    }
    Copy-Item -Recurse -Path $WinSrc -Destination $WinDist

    # 创建专用的 data 目录
    $DataDir = Join-Path $WinDist "data"
    if (-not (Test-Path $DataDir)) {
        New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
    }

    $exePath = Join-Path $WinDist "woofish.exe"
    if (Test-Path $exePath) {
        $folderSize = (Get-ChildItem $WinDist -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
        $sizeStr = "{0:N1} MB" -f $folderSize
        Write-Host "[成功] Windows 便携版已归档: $exePath" -ForegroundColor Green
        Write-Host "       便携包总大小: $sizeStr (含内置极简运行环境)" -ForegroundColor Gray
    }
} else {
    Write-Host "[3/4] 跳过 Windows 桌面端构建 (目标为 $Target)" -ForegroundColor Gray
}

# 4. 检查并编译 Android APK
if ($Target -in @("all", "apk", "android")) {
    Write-Host ""
    Write-Host "[4/4] 检查 Android 构建环境..." -ForegroundColor Green
    $hasSdk = $false
    if ($env:ANDROID_HOME -or $env:ANDROID_SDK_ROOT) {
        $hasSdk = $true
    } elseif (Test-Path (Join-Path $PSScriptRoot "local.properties")) {
        $localProp = Get-Content (Join-Path $PSScriptRoot "local.properties") -Raw
        if ($localProp -match "sdk\.dir") {
            $hasSdk = $true
        }
    }

    if ($hasSdk) {
        Write-Host "检测到 Android SDK，正在编译 Android Release APK..." -ForegroundColor Green
        & $gradlew ":app:assembleRelease"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[错误] Android APK 构建失败，退出码: $LASTEXITCODE" -ForegroundColor Red
            exit $LASTEXITCODE
        }
        $apkDir = Join-Path $PSScriptRoot "build\android\outputs\apk\release"
        if (Test-Path $apkDir) {
            $apkFiles = Get-ChildItem -Path $apkDir -Filter "*.apk"
            foreach ($apk in $apkFiles) {
                $destApk = Join-Path $AndroidDist "woofish-$APP_VER.apk"
                Copy-Item -Path $apk.FullName -Destination $destApk -Force
                Write-Host "[成功] Android APK 已归档: $destApk" -ForegroundColor Green
            }
        }
    } else {
        Write-Host "[提示] 本地未检测到 Android SDK 环境，已跳过本地 Android APK 编译。" -ForegroundColor Yellow
        Write-Host "       如需本地编译 APK，请在 local.properties 中添加 sdk.dir 路径。" -ForegroundColor Gray
    }
} else {
    Write-Host "[4/4] 跳过 Android APK 构建 (目标为 $Target)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   本地构建完成！" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "[可用软件包清单]" -ForegroundColor White
Write-Host "  1. Windows 便携版: $WinDist\woofish.exe" -ForegroundColor Green
Write-Host "     - 预置资源目录: $WinDist\data\" -ForegroundColor Gray
$apkFinal = Join-Path $AndroidDist "woofish-$APP_VER.apk"
if (Test-Path $apkFinal) {
    Write-Host "  2. Android 安装包: $apkFinal" -ForegroundColor Green
}
Write-Host ""
Write-Host "🚨 重要守则 (本地测试前置):" -ForegroundColor Red
Write-Host "   上传到 GitHub 前，请由开发者在本地直接测试运行上述产物！" -ForegroundColor Red
Write-Host "   测试无误后，再决定是否进行版本发布并推送到远程仓库。" -ForegroundColor Red
Write-Host "========================================================" -ForegroundColor Cyan

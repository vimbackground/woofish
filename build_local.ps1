param(
    [ValidateSet("all", "apk", "android")]
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
Write-Host "   正念 (woofish) 本地 Android 构建系统" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "目标构建模式: $Target" -ForegroundColor Gray
Write-Host "Java 环境: $env:JAVA_HOME" -ForegroundColor Gray
if ($env:ANDROID_HOME) {
    Write-Host "Android SDK: $env:ANDROID_HOME" -ForegroundColor Gray
}
Write-Host ""

# 1. 提取版本号
$androidGradlePath = Join-Path $PSScriptRoot "modules\android\build.gradle.kts"
$APP_VER = "1.5.0"
if (Test-Path $androidGradlePath) {
    $content = Get-Content $androidGradlePath -Raw
    if ($content -match 'versionName\s*=\s*"([^"]+)"') {
        $APP_VER = $matches[1].Trim()
    }
}
Write-Host "[1/3] 当前目标构建版本: $APP_VER" -ForegroundColor Green
Write-Host ""

# 2. 准备 _Dist 目录
Write-Host "[2/3] 初始化 _Dist/android 目录..." -ForegroundColor Green
$DistDir = Join-Path $PSScriptRoot "_Dist"
$AndroidDist = Join-Path $DistDir "android"
New-Item -ItemType Directory -Force -Path $AndroidDist | Out-Null

$gradlew = Join-Path $PSScriptRoot "gradlew.bat"

# 3. 检查并编译 Android APK
Write-Host ""
Write-Host "[3/3] 检查 Android 构建环境并开始编译..." -ForegroundColor Green
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
            $apkSize = (Get-Item $destApk).Length / 1MB
            $sizeStr = "{0:N2} MB" -f $apkSize
            Write-Host "[成功] Android APK 已归档: $destApk ($sizeStr)" -ForegroundColor Green
        }
    }
} else {
    Write-Host "[提示] 本地未检测到 Android SDK 环境，已跳过本地 Android APK 编译。" -ForegroundColor Yellow
    Write-Host "       如需本地编译 APK，请在 local.properties 中添加 sdk.dir 路径。" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   本地构建完成！" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
$apkFinal = Join-Path $AndroidDist "woofish-$APP_VER.apk"
if (Test-Path $apkFinal) {
    Write-Host "[可用软件包]" -ForegroundColor White
    Write-Host "  Android 安装包: $apkFinal" -ForegroundColor Green
}
Write-Host ""
Write-Host '🚨 重要守则 (本地测试前置):' -ForegroundColor Red
Write-Host '   上传到 GitHub 前，请由开发者在本地真机/模拟器测试运行上述产物！' -ForegroundColor Red
Write-Host '   测试无误后，再决定是否进行版本发布并推送到远程仓库。' -ForegroundColor Red
Write-Host "========================================================" -ForegroundColor Cyan

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
}

// 避免因网络连接 GitHub 超时阻断无安装器要求的便携包创建
tasks.matching { it.name.contains("Wix", ignoreCase = true) }.configureEach {
    enabled = false
}

// 统一所有子模块的编译中间产物到根目录下的 build/ 中
subprojects {
    val moduleBuildName = when (name) {
        "app" -> "android"
        "desktop" -> "windows"
        "shared" -> "shared"
        else -> name
    }
    layout.buildDirectory.set(rootProject.layout.buildDirectory.dir(moduleBuildName))
}


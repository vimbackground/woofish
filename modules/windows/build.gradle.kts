plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
}

compose.desktop {
    application {
        mainClass = "com.woofish.desktop.MainKt"
        buildTypes.release.proguard {
            isEnabled = true
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage)
            packageName = "woofish"
            packageVersion = "1.2.2"
            description = "woofish - mindfulness wooden fish and rhythm"
            copyright = "Copyright (c) 2026 woofish"
            modules("java.base", "java.desktop", "java.logging", "jdk.unsupported")
            windows {
                menu = true
                upgradeUuid = "8b52f1e2-9721-4cf1-8a9d-162e8412b10a"
                shortcut = true
                dirChooser = true
            }
        }
    }
}

tasks.matching { it.name == "packageReleaseAppImage" }.configureEach {
    doLast {
        val appImageRoot = file("${layout.buildDirectory.get()}/compose/binaries/main-release/app/woofish")
        val dataDir = File(appImageRoot, "data")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
            println("Portable data directory created at: ${dataDir.absolutePath}")
        }
        val distDir = rootProject.file("_Dist/windows/woofish")
        if (distDir.exists()) {
            distDir.deleteRecursively()
        }
        distDir.mkdirs()
        copy {
            from(appImageRoot)
            into(distDir)
        }
        println("Windows portable AppImage archived to: ${distDir.absolutePath}")
    }
}


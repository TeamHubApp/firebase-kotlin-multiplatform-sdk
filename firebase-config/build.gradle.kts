/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

version = project.property("firebase-config.version") as String

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    //id("com.quittle.android-emulator") version "0.2.0"
}

android {
    compileSdk = property("targetSdkVersion") as Int
    defaultConfig {
        minSdk = property("minSdkVersion") as Int
        targetSdk = property("targetSdkVersion") as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
        getByName("androidTest"){
            java.srcDir(file("src/androidAndroidTest/kotlin"))
        }
    }
    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }
    packagingOptions {
        resources.pickFirsts.add("META-INF/kotlinx-serialization-core.kotlin_module")
        resources.pickFirsts.add("META-INF/AL2.0")
        resources.pickFirsts.add("META-INF/LGPL2.1")
    }
    lint {
        abortOnError = false
    }
}

// Optional configuration
//androidEmulator {
//    emulator {
//        name("givlive_emulator")
//        sdkVersion(28)
//        abi("x86_64")
//        includeGoogleApis(true) // Defaults to false
//
//    }
//    headless(false)
//    logEmulatorOutput(false)
//}

val skipIosTarget: Boolean = project.property("skipIosTarget") == "true"
val skipMacOsTarget: Boolean = project.property("skipMacOsTarget") == "true"
val skipTvOsTarget: Boolean = project.property("skipTvOsTarget") == "true"

kotlin {

    android {
        publishAllLibraryVariants()
    }

    fun nativeTargetConfig(): KotlinNativeTarget.() -> Unit = {
        val nativeFrameworkPaths = konanTarget.firebaseCoreFrameworksPaths(rootProject.project("firebase-app").projectDir).plus(
            konanTarget.carthageXcFrameworksPaths(
                projectDir,
                listOf(
                    "FirebaseABTesting",
                    "FirebaseRemoteConfig"
                )
            )
        )

        binaries {
            getTest("DEBUG").apply {
                linkerOpts(nativeFrameworkPaths.map { "-F$it" })
                linkerOpts("-ObjC")
            }
        }

        compilations.getByName("main") {
            cinterops.create("FirebaseRemoteConfig") {
                compilerOpts(nativeFrameworkPaths.map { "-F$it" })
                extraOpts = listOf("-compiler-option", "-DNS_FORMAT_ARGUMENT(A)=", "-verbose")
            }
        }
    }

    if (!skipIosTarget) {
        ios(configure = nativeTargetConfig())
        iosSimulatorArm64(configure = nativeTargetConfig())
    }

    if (!skipMacOsTarget) {
        macosArm64(configure = nativeTargetConfig())
        macosX64(configure = nativeTargetConfig())
    }

    if (!skipTvOsTarget) {
        tvos(configure = nativeTargetConfig())
        tvosSimulatorArm64(configure = nativeTargetConfig())
    }

    js {
        useCommonJs()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "1.6"
                languageVersion = "1.6"
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        val commonMain by getting {
            dependencies {
                api(project(":firebase-app"))
                implementation(project(":firebase-common"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("com.google.firebase:firebase-config")
            }
        }

        val iosMain by getting
        val iosSimulatorArm64Main by getting
        val macosArm64Main by getting
        val macosX64Main by getting
        val tvosMain by getting
        val tvosSimulatorArm64Main by getting

        val nativeMain by creating {
            dependsOn(commonMain)

            iosMain.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
            macosX64Main.dependsOn(this)
            tvosMain.dependsOn(this)
            tvosSimulatorArm64Main.dependsOn(this)
        }

        val iosTest by getting
        val iosSimulatorArm64Test by getting
        val macosArm64Test by getting
        val macosX64Test by getting
        val tvosTest by getting
        val tvosSimulatorArm64Test by getting

        val nativeTest by creating {
            dependsOn(commonMain)

            iosTest.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
            macosArm64Test.dependsOn(this)
            macosX64Test.dependsOn(this)
            tvosTest.dependsOn(this)
            tvosSimulatorArm64Test.dependsOn(this)
        }

        val jsMain by getting
    }
}

if (project.property("firebase-config.skipIosTests") == "true") {
    tasks.forEach {
        if (it.name.contains("ios", true) && it.name.contains("test", true)) { it.enabled = false }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

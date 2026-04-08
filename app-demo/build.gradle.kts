import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.elveum.container.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.elveum.container.demo"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xcontext-parameters")
    }
    composeCompiler {
        stabilityConfigurationFiles.add(
            project.layout.projectDirectory.file("stability.txt")
        )
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.bundles.lifecycle.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.bundles.navigation3)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.javafaker)
    implementation(libs.coil.compose)

    implementation(project(":container"))
}

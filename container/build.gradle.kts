import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    // linuxX64 is the only Kotlin/Native target whose tests can execute on a
    // Linux CI runner, so it carries the native test coverage for iOS.
    linuxX64()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(libs.compose.runtime.annotation)
        }
        nativeMain.dependencies {
            implementation(libs.atomicfu)
        }
        nativeTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.flowtest)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

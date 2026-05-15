import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            compileOnly(libs.compose.stable.marker)
        }

        iosMain.dependencies {
            api(libs.compose.stable.marker)
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.flowtest)
            }
        }

    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }
    explicitApi()
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

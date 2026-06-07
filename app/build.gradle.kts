plugins {
    alias(libs.plugins.android.application)
    // NOTE: kotlin-android (org.jetbrains.kotlin.android) is REMOVED.
    // AGP 9.0+ bundles Kotlin compilation natively ("built-in Kotlin").
    // Applying kotlin-android alongside AGP 9.x causes a hard build failure.
    alias(libs.plugins.kotlin.compose)   // Compose compiler extension — still required
}

android {
    namespace  = "com.example.weardomgr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.weardomgr"
        minSdk        = 30
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0"
    }

    buildTypes {
        debug {
            // Keep debuggable for local ADB installs during development
            isDebuggable    = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled  = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign with the default debug key so CI can produce an installable APK
            // without managing a separate signing keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    // Java source/target kept for non-Kotlin Java interop paths
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // kotlinOptions{} was removed in AGP 9.x — use the top-level kotlin{} block below
}

// AGP 9.x + Kotlin 2.4: configure Kotlin compiler via the top-level kotlin{} block.
// jvmToolchain(17) covers both the JVM target and the Java toolchain.
kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=androidx.wear.compose.material3.ExperimentalWearMaterial3Api",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            )
        )
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.coroutines.android)

    // Compose BOM 2026.05.01
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)

    // Wear Compose 1.6.2
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.wear.compose.tooling)
}

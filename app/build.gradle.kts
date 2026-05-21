plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kyly.picking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kyly.picking"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val localProps = java.util.Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) load(f.inputStream())
        }
        buildConfigField(
            "String", "API_BASE_URL",
            "\"${localProps.getProperty("API_BASE_URL", "http://localhost:3000")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    repositories {
        flatDir { dirs("libs") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ── Compose BOM ───────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.prev)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // ── Core / Activity ───────────────────────────────────────────
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel)

    // ── Navigation ────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt ──────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ── Retrofit + Moshi ──────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    // ── Room ──────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Segurança / Auth ──────────────────────────────────────────
    implementation(libs.security.crypto)

    // ── WorkManager ───────────────────────────────────────────────
    implementation(libs.work.runtime)

    // ── Datalogic SDK ─────────────────────────────────────────────
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "be.arte.mathforkids"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "be.arte.mathforkids"
        minSdk = 26
        targetSdk = 35
        // Android only installs an update whose versionCode is strictly higher.
        // The release workflow derives it from the android-vMAJOR.MINOR.PATCH tag
        // (MAJOR*10000 + MINOR*100 + PATCH, see scripts/android_release_version.py).
        versionCode = System.getenv("VERSION_CODE")?.toInt() ?: 100
        versionName = System.getenv("VERSION_NAME") ?: "0.1.0"
    }

    // A fixed, dedicated release key: Android refuses an update signed with another one.
    // The keystore is never committed; CI decodes it from the SIGNING_* secrets, locally it
    // lives in ~/agent-managed/keys/android/math-for-kids/.
    signingConfigs {
        System.getenv("SIGNING_STORE_FILE")?.takeIf { it.isNotBlank() }?.let { keyPath ->
            create("release") {
                storeFile = file(keyPath)
                storeType = "PKCS12"
                storePassword = requireNotNull(System.getenv("SIGNING_STORE_PASSWORD")).also { require(it.isNotBlank()) }
                keyAlias = requireNotNull(System.getenv("SIGNING_KEY_ALIAS")).also { require(it.isNotBlank()) }
                keyPassword = requireNotNull(System.getenv("SIGNING_KEY_PASSWORD")).also { require(it.isNotBlank()) }
            }
        }
    }

    buildTypes {
        // Distinct id so a debug build can sit next to the release app.
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Math contre Zombies (debug)")
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            resValue("string", "app_name", "Math contre Zombies")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)

    testImplementation(libs.junit)
}

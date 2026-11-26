plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hidden.devices.detector.bugs.finder"
    // Latest androidx libraries need compileSdk 37.1. targetSdk stays 36 (the Play requirement).
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        // Same ID as the app already on Google Play, so this ships as an update.
        applicationId = "com.hidden.devices.detector.bugs.finder"
        minSdk = 26
        targetSdk = 36
        // IMPORTANT: set this higher than the versionCode of the version that is live on Play now.
        versionCode = 100
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // Brand words and the privacy text stay English on purpose.
    lint { disable += "MissingTranslation" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.play.review.ktx)
    implementation(libs.billing.ktx)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.ump)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

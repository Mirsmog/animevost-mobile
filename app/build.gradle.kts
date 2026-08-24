plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.animevost.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.animevost.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 43
        versionName = "1.7.19"
    }

    val releaseStoreFile = System.getenv("SIGNING_STORE_FILE")

    signingConfigs {
        if (!releaseStoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = requireNotNull(System.getenv("SIGNING_STORE_PASSWORD")) {
                    "SIGNING_STORE_PASSWORD is required when SIGNING_STORE_FILE is set"
                }
                keyAlias = requireNotNull(System.getenv("SIGNING_KEY_ALIAS")) {
                    "SIGNING_KEY_ALIAS is required when SIGNING_STORE_FILE is set"
                }
                keyPassword = requireNotNull(System.getenv("SIGNING_KEY_PASSWORD")) {
                    "SIGNING_KEY_PASSWORD is required when SIGNING_STORE_FILE is set"
                }
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (!releaseStoreFile.isNullOrBlank()) {
                signingConfigs.getByName("release")
                    .also { signingConfig = it }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("prod") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_DEVELOPMENT", "false")
            buildConfigField("boolean", "ENABLE_IN_APP_UPDATES", "true")
            buildConfigField("boolean", "ENABLE_BACKGROUND_NOTIFICATIONS", "true")
        }
        create("dev") {
            dimension = "distribution"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("boolean", "IS_DEVELOPMENT", "true")
            buildConfigField("boolean", "ENABLE_IN_APP_UPDATES", "false")
            buildConfigField("boolean", "ENABLE_BACKGROUND_NOTIFICATIONS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    beforeVariants { variant ->
        val distribution = variant.productFlavors
            .firstOrNull { (dimension, _) -> dimension == "distribution" }
            ?.second
        variant.enable = when (distribution) {
            "dev" -> variant.buildType == "debug"
            "prod" -> variant.buildType == "release"
            else -> false
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:catalog"))
    implementation(project(":feature:schedule"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:player"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:profile"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)

    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.exoplayer.hls)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
}

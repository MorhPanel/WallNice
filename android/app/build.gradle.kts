plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.morhpanel.nekowall"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.morhpanel.nekowall"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "0.0.12-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("Long", "BUILD_TIME_MILLIS", "${System.currentTimeMillis()}L")
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
            val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
            val keyAlias = System.getenv("RELEASE_KEY_ALIAS")?.takeIf { it.isNotBlank() }
            val keyPassword = System.getenv("RELEASE_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
            val keystoreFile = keystorePath?.let { file(it) }

            if (keystoreFile?.isFile == true && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    val releaseSigningConfigured = listOf(
        System.getenv("RELEASE_KEYSTORE_PATH"),
        System.getenv("RELEASE_KEYSTORE_PASSWORD"),
        System.getenv("RELEASE_KEY_ALIAS"),
        System.getenv("RELEASE_KEY_PASSWORD")
    ).all { !it.isNullOrBlank() } &&
        System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it).isFile } == true

    gradle.taskGraph.whenReady {
        if (allTasks.any { it.name.contains("Release") } && !releaseSigningConfigured) {
            throw GradleException("Release signing credentials are required.")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = releaseSigning
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)

    debugImplementation(libs.androidx.ui.tooling)
}

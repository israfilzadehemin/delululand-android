import java.util.Properties

plugins {
    alias(libs.plugins.delulutopia.android.application)
    alias(libs.plugins.delulutopia.android.compose)
    alias(libs.plugins.delulutopia.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

/*
 * Release signing (C3). CI writes the upload keystore from the DL_KEYSTORE_BASE64 secret; locally a
 * keystore.properties file (git-ignored) may point at one. Without either, release builds are signed
 * with the debug key so the APK still installs, and the CI run says so.
 */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.delulutopia.app"

    defaultConfig {
        applicationId = "com.delulutopia.app"
        versionCode = (System.getenv("DL_VERSION_CODE") ?: "1").toInt()
        versionName = "0.1.0"
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "DL_KEYSTORE_FILE")
        if (storeFile != null) {
            create("upload") {
                this.storeFile = file(storeFile)
                storePassword = signingValue("storePassword", "DL_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "DL_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "DL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // C2: the server each build talks to; both install side by side. The test-server flavour is called
    // "staging" because AGP reserves flavour names starting with "test".
    flavorDimensions += "server"
    productFlavors {
        create("staging") {
            dimension = "server"
            applicationIdSuffix = ".test"
            buildConfigField("String", "BASE_URL", "\"https://test.delulutopia.com/\"")
            resValue("string", "app_name", "Delulutopia Test")
        }
        create("prod") {
            dimension = "server"
            buildConfigField("String", "BASE_URL", "\"https://delulutopia.com/\"")
            resValue("string", "app_name", "Delulutopia")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.feature.start)
    implementation(projects.feature.home)
    implementation(projects.feature.letters)
    implementation(projects.core.ui)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

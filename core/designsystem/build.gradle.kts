plugins {
    alias(libs.plugins.delulutopia.android.library)
    alias(libs.plugins.delulutopia.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.delulutopia.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

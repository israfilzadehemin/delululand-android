plugins {
    alias(libs.plugins.delulutopia.android.library)
    alias(libs.plugins.delulutopia.android.compose)
    alias(libs.plugins.delulutopia.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.delulutopia.feature.discussions"
}

dependencies {
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.emoji2.emojipicker)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

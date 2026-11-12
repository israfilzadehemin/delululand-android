plugins {
    alias(libs.plugins.delulutopia.android.library)
}

android {
    namespace = "com.delulutopia.core.i18n"
}

dependencies {
    // The language is snapshot state, so a switch recomposes every screen that reads a text.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

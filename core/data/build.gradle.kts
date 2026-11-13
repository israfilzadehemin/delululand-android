plugins {
    alias(libs.plugins.delulutopia.android.library)
    alias(libs.plugins.delulutopia.hilt)
}

android {
    namespace = "com.delulutopia.core.data"
}

dependencies {
    api(projects.core.network)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
}

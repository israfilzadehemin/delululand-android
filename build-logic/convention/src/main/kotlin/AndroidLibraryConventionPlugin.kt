import com.android.build.api.dsl.LibraryExtension
import com.delulutopia.buildlogic.intVersion
import com.delulutopia.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android library module (core:*, feature:*): same SDK floor and bytecode level as the app. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        extensions.configure<LibraryExtension> {
            compileSdk = libs.intVersion("compileSdk")
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
            }
            lint {
                abortOnError = true
                targetSdk = libs.intVersion("targetSdk")
            }
        }
    }
}

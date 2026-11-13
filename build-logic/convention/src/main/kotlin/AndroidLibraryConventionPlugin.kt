import com.android.build.api.dsl.LibraryExtension
import com.delulutopia.buildlogic.intVersion
import com.delulutopia.buildlogic.library
import com.delulutopia.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

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
                // java.time on API 24–25 (instants, zones, durations).
                isCoreLibraryDesugaringEnabled = true
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
            }
            lint {
                abortOnError = true
                targetSdk = libs.intVersion("targetSdk")
            }
        }
        dependencies { add("coreLibraryDesugaring", libs.library("desugar-jdk-libs")) }
        tasks.withType<Test>().configureEach {
            // Robolectric's FileDescriptor interceptor needs JDK internals on JDK 21+.
            jvmArgs(
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
            )
            testLogging {
                exceptionFormat = TestExceptionFormat.FULL
                showStackTraces = true
                showCauses = true
            }
        }
    }
}

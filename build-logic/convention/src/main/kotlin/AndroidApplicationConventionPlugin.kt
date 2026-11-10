import com.android.build.api.dsl.ApplicationExtension
import com.delulutopia.buildlogic.intVersion
import com.delulutopia.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/** Android app module: SDK levels, Java 17 bytecode, tests on Robolectric. Kotlin is AGP 9's built-in support. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        extensions.configure<ApplicationExtension> {
            compileSdk = libs.intVersion("compileSdk")
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                targetSdk = libs.intVersion("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true
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
                warningsAsErrors = false
                checkDependencies = true
            }
        }
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

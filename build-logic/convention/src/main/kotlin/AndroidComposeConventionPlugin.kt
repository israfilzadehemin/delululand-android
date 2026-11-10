import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.delulutopia.buildlogic.library
import com.delulutopia.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Jetpack Compose for an app or library module: the Compose compiler plugin, the BOM and tooling. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.withPlugin("com.android.application") {
            extensions.getByType(ApplicationExtension::class.java).buildFeatures.compose = true
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.getByType(LibraryExtension::class.java).buildFeatures.compose = true
        }
        // The M3 top app bar and bottom sheet APIs are still marked experimental in Material 3 1.4.
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        }
        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            add("implementation", bom)
            add("testImplementation", bom)
            add("androidTestImplementation", bom)
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}

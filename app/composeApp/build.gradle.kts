import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

abstract class GenerateAppConfig : DefaultTask() {
    @get:Input
    abstract val supabaseUrl: Property<String>

    @get:Input
    abstract val supabaseAnonKey: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val directory = outputDir.get().asFile.resolve("app/cozitopia/config")
        directory.mkdirs()
        directory.resolve("AppConfig.kt").writeText(
            """
            |package app.cozitopia.config
            |
            |object AppConfig {
            |    const val SUPABASE_URL = "${supabaseUrl.get()}"
            |    const val SUPABASE_ANON_KEY = "${supabaseAnonKey.get()}"
            |}
            |
            """.trimMargin(),
        )
    }
}

fun configValue(property: String, variable: String): Provider<String> =
    providers.gradleProperty(property)
        .orElse(providers.environmentVariable(variable))
        .orElse("")

val generateAppConfig = tasks.register<GenerateAppConfig>("generateAppConfig") {
    supabaseUrl.set(configValue("supabaseUrl", "SUPABASE_URL"))
    supabaseAnonKey.set(configValue("supabaseAnonKey", "SUPABASE_ANON_KEY"))
    outputDir.set(layout.buildDirectory.dir("generated/appConfig/kotlin"))
}

kotlin {
    android {
        namespace = "app.cozitopia.shared"
        compileSdk = 36
        minSdk = 26
    }
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppConfig)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.material.icons.extended)
                implementation(project.dependencies.platform(libs.supabase.bom))
                implementation(libs.supabase.auth)
                implementation(libs.supabase.postgrest)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.lifecycle.viewmodel.compose)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

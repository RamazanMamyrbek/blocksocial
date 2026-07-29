import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

abstract class CopySupportedAppCatalog : DefaultTask() {

    @get:InputFile
    abstract val catalog: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyCatalog() {
        val target = outputDirectory.get().asFile
        target.mkdirs()
        catalog.get().asFile.copyTo(target.resolve("catalog.json"), overwrite = true)
    }
}

val copySupportedAppCatalog by tasks.registering(CopySupportedAppCatalog::class) {
    catalog.set(layout.projectDirectory.file("../../shared/supported-app-catalog/catalog.json"))
    outputDirectory.set(layout.buildDirectory.dir("generated/catalog/assets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            copySupportedAppCatalog,
            CopySupportedAppCatalog::outputDirectory,
        )
    }
}

android {
    namespace = "com.blocksocial"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.blocksocial"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


dependencies {
    implementation(project(":core-ui"))
    implementation(project(":core-data"))
    implementation(project(":feature-onboarding"))
    implementation(project(":feature-dashboard"))
    implementation(project(":feature-app-selection"))
    implementation(project(":feature-rules"))
    implementation(project(":feature-history"))
    implementation(project(":feature-settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.junit)
}

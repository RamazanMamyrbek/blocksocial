plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

val moduleLayers = mapOf(
    ":core-model" to 0,
    ":core-domain" to 1,
    ":core-data" to 2,
    ":core-ui" to 2,
    ":feature-onboarding" to 3,
    ":feature-dashboard" to 3,
    ":feature-app-selection" to 3,
    ":feature-rules" to 3,
    ":feature-history" to 3,
    ":feature-settings" to 3,
    ":app" to 4,
)

val checkModuleGraph by tasks.registering {
    group = "verification"
    description = "Fails when a module depends on its own layer or on a layer above it."
    doLast {
        val violations = mutableListOf<String>()
        subprojects.forEach { module ->
            val moduleLayer = moduleLayers[module.path]
            if (moduleLayer != null) {
                module.configurations.forEach { configuration ->
                    configuration.dependencies
                        .filterIsInstance<ProjectDependency>()
                        .forEach { dependency ->
                            val dependencyLayer = moduleLayers[dependency.path]
                            val isSelf = dependency.path == module.path
                            if (dependencyLayer != null && !isSelf && dependencyLayer >= moduleLayer) {
                                violations += "${module.path} depends on ${dependency.path}"
                            }
                        }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module dependencies must point downwards only:\n" + violations.distinct().joinToString("\n"),
            )
        }
    }
}

val frameworkFreeModules = listOf(":core-model", ":core-domain")

val androidArtifactGroups = listOf("androidx.", "com.android", "com.google.android")

val checkDomainIsFrameworkFree by tasks.registering {
    group = "verification"
    description = "Fails when the domain modules gain an Android dependency."
    doLast {
        val violations = mutableListOf<String>()
        frameworkFreeModules.forEach { path ->
            val module = project(path)
            if (module.plugins.hasPlugin("com.android.library") ||
                module.plugins.hasPlugin("com.android.application")
            ) {
                violations += "$path applies an Android Gradle plugin"
            }
            listOf("compileClasspath", "runtimeClasspath", "testRuntimeClasspath").forEach { name ->
                module.configurations.findByName(name)
                    ?.resolvedConfiguration
                    ?.resolvedArtifacts
                    ?.map { it.moduleVersion.id }
                    ?.filter { id -> androidArtifactGroups.any { id.group.startsWith(it) } }
                    ?.forEach { violations += "$path:$name pulls in $it" }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "The domain must stay free of the Android framework:\n" +
                    violations.distinct().joinToString("\n"),
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkModuleGraph, checkDomainIsFrameworkFree)
}

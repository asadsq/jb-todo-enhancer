/*
 * Gradle settings script that names the root project and pins the versions of the Kotlin,
 * changelog, foojay toolchain resolver, and IntelliJ Platform settings plugins. Also
 * configures dependency resolution to use Maven Central plus the default JetBrains
 * IntelliJ Platform repositories.
 */
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "jb-todo-enhancer"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.4.10"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Configure all projects' repositories
    repositories {
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("jvm") version "1.9.21"
        kotlin("plugin.serialization") version "1.9.21"
        id("com.google.devtools.ksp") version "1.9.21-1.0.15"
        id("org.jetbrains.compose") version "1.6.0"
        id("com.diffplug.spotless") version "6.22.0"
        id("com.github.gmazzo.buildconfig") version "4.1.2"
        id("io.sentry.jvm.gradle") version "3.12.0"
        id("dev.hydraulic.conveyor") version "1.5"
    }
}

rootProject.name = "rift"

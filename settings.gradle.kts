pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("jvm") version "2.0.0"
        kotlin("plugin.serialization") version "2.0.0"
        id("com.google.devtools.ksp") version "2.0.0-1.0.22"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        id("org.jetbrains.compose") version "1.7.0-alpha03"
        id("com.diffplug.spotless") version "6.25.0"
        id("com.github.gmazzo.buildconfig") version "5.3.5"
        id("io.sentry.jvm.gradle") version "4.8.0"
        id("dev.hydraulic.conveyor") version "1.10"
    }
}

rootProject.name = "rift"

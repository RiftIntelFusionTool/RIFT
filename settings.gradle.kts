pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("jvm") version "1.9.23"
        kotlin("plugin.serialization") version "1.9.23"
        id("com.google.devtools.ksp") version "1.9.23-1.0.20"
        id("org.jetbrains.compose") version "1.6.2"
        id("com.diffplug.spotless") version "6.22.0"
        id("com.github.gmazzo.buildconfig") version "5.3.5"
        id("io.sentry.jvm.gradle") version "4.4.1"
        id("dev.hydraulic.conveyor") version "1.6"
    }
}

rootProject.name = "rift"

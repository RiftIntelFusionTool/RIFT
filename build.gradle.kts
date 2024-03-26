import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.diffplug.spotless")
    id("com.google.devtools.ksp")
    id("com.github.gmazzo.buildconfig")
    id("dev.hydraulic.conveyor")
}

val riftVersion = properties["rift.version"] as String
group = "dev.nohus"
version = riftVersion

buildConfig {
    val environment = (properties["rift.environment"] as? String) ?: "dev"
    buildConfigField("String", "environment", "\"$environment\"")
    buildConfigField("String", "version", "\"${properties["rift.version"]}\"")
    buildConfigField("long", "buildTimestamp", "${Instant.now().toEpochMilli()}")
    buildConfigField("String", "buildUuid", "\"${properties["rift.buildUuid"]}\"")
    buildConfigField("String", "sentryDsn", "${properties["rift.sentryDsn"]}")
    buildConfigField("String", "focusedLoggers", "${properties["rift.focusedLoggers"]}")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    google()
}

dependencies {
    // Compose
    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.desktop.components.animatedImage)
    implementation(compose.components.resources)
    implementation("media.kamel:kamel-image:0.7.2")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Koin
    implementation("io.insert-koin:koin-core:3.4.3")
    implementation("io.insert-koin:koin-logger-slf4j:3.4.3")
    implementation("io.insert-koin:koin-annotations:1.2.2")
    ksp("io.insert-koin:koin-ksp-compiler:1.2.2")

    // Other
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.commons:commons-exec:1.3")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    implementation("org.bitbucket.b_c:jose4j:0.9.3")
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.nibor.autolink:autolink:0.11.0")

    // OpenAL Audio
    implementation("org.jogamp.joal:joal-main:2.3.2")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.3.2")

    // Smack (XMPP)
    implementation("org.igniterealtime.smack:smack-java8:4.4.0")
    implementation("org.igniterealtime.smack:smack-tcp:4.4.0")
    implementation("org.igniterealtime.smack:smack-im:4.4.0")
    implementation("org.igniterealtime.smack:smack-extensions:4.4.0")

    // SystemTray
    implementation("com.dorkbox:Collections:2.4")
    implementation("com.dorkbox:Executor:3.13")
    implementation("com.dorkbox:Desktop:1.1")
    implementation("com.dorkbox:JNA:1.2")
    implementation(files("libs/OS.jar"))
    implementation("com.dorkbox:Updates:1.1")
    implementation("com.dorkbox:Utilities:1.46")
    implementation("org.javassist:javassist:3.29.2-GA")
    val jnaVersion = "5.13.0"
    implementation("net.java.dev.jna:jna-jpms:${jnaVersion}")
    implementation("net.java.dev.jna:jna-platform-jpms:${jnaVersion}")
    implementation("org.slf4j:slf4j-api:1.8.0-beta4")  // java 8
    implementation(files("libs/SystemTray.jar"))

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:2.3.3")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.3")
    implementation("io.ktor:ktor-client-core:2.3.3")
    implementation("io.ktor:ktor-client-cio:2.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")

    // Sentry
    implementation(platform("io.sentry:sentry-bom:7.0.0"))
    implementation("io.sentry:sentry")
    implementation("io.sentry:sentry-logback")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("io.mockk:mockk:1.13.7")
}

compose.desktop {
    application {
        mainClass = "dev.nohus.rift.MainKt"

        nativeDistributions {
            modules("java.sql", "java.naming", "jdk.naming.dns")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    kotlinOptions.freeCompilerArgs += "-opt-in=org.jetbrains.compose.resources.ExperimentalResourceApi"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations.all {
    attributes {
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint("0.50.0")
        targetExclude("**/generated/**")
    }
}

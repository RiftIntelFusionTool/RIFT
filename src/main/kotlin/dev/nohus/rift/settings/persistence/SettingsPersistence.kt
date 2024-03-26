package dev.nohus.rift.settings.persistence

import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createParentDirectories

private val logger = KotlinLogging.logger {}

@Single
class SettingsPersistence(
    appDirectories: AppDirectories,
    @Named("settings") private val json: Json,
) {

    private val configFile = File(appDirectories.getAppDataDirectory(), "settings.json")
    private val scope = CoroutineScope(Job())
    private val mutex = Mutex()

    init {
        configFile.toPath().createParentDirectories()
    }

    fun load(): SettingsModel {
        return try {
            val serialized = configFile.readText()
            json.decodeFromString<SettingsModel>(serialized)
        } catch (e: FileNotFoundException) {
            logger.info { "Settings file not found" }
            SettingsModel()
        } catch (e: SerializationException) {
            logger.error(e) { "Could not deserialize settings" }
            Sentry.captureException(e)
            backupSettingsFile()
            SettingsModel(
                isSettingsReadFailure = true,
            )
        }
    }

    fun save(model: SettingsModel) = scope.launch(Dispatchers.IO) {
        val serialized = json.encodeToString(model)
        mutex.withLock {
            try {
                configFile.writeText(serialized)
            } catch (e: IOException) {
                logger.error { "Could not write settings: $e" }
            }
        }
    }

    private fun backupSettingsFile() {
        val source = configFile.toPath()
        val target = File(configFile.parentFile, "settingsBackup-${Instant.now().toEpochMilli()}.json").toPath()
        Files.move(source, target)
    }
}

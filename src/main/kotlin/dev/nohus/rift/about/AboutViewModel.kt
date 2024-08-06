package dev.nohus.rift.about

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.ViewModel
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.AsyncResource.Error
import dev.nohus.rift.network.AsyncResource.Loading
import dev.nohus.rift.network.AsyncResource.Ready
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.openFileManager
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Factory
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.jvm.optionals.getOrDefault

private val logger = KotlinLogging.logger {}

@Factory
class AboutViewModel(
    private val okHttpClient: OkHttpClient,
    operatingSystem: OperatingSystem,
    private val appDirectories: AppDirectories,
    private val windowManager: WindowManager,
    getVersionUseCase: GetVersionUseCase,
) : ViewModel() {

    data class UiState(
        val version: String,
        val buildTime: String,
        val latestVersion: String?,
        val isUpdateAvailable: AsyncResource<Boolean>,
        val isUpdateDialogShown: Boolean,
        val isLegalDialogShown: Boolean,
        val isCreditsDialogShown: Boolean,
        val operatingSystem: OperatingSystem,
        val executablePath: String,
    )

    data class Version(val major: Int, val minor: Int, val patch: Int) {
        override fun toString(): String = "$major.$minor.$patch"
    }

    private val _state = MutableStateFlow(
        UiState(
            version = getVersionUseCase(),
            buildTime = getBuildTime(),
            latestVersion = null,
            isUpdateAvailable = Loading,
            isUpdateDialogShown = false,
            isLegalDialogShown = false,
            isCreditsDialogShown = false,
            operatingSystem = operatingSystem,
            executablePath = getExecutable(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        checkForUpdate()
    }

    fun onUpdateClick() {
        _state.update { it.copy(isUpdateDialogShown = true) }
    }

    fun onDebugClick() {
        windowManager.onWindowOpen(RiftWindow.Debug)
    }

    fun onAppDataClick() {
        appDirectories.getAppDataDirectory().openFileManager()
    }

    fun onLegalClick() {
        _state.update { it.copy(isLegalDialogShown = true) }
    }

    fun onCreditsClick() {
        _state.update { it.copy(isCreditsDialogShown = true) }
    }

    fun onWhatsNewClick() {
        windowManager.onWindowOpen(RiftWindow.WhatsNew)
    }

    fun onDialogDismissed() {
        _state.update {
            it.copy(
                isUpdateDialogShown = false,
                isLegalDialogShown = false,
                isCreditsDialogShown = false,
            )
        }
    }

    private fun checkForUpdate() = viewModelScope.launch {
        val current = getVersion(BuildConfig.version)
        val latest = getLatestVersion()
        if (latest != null) {
            _state.update { it.copy(latestVersion = latest.toString()) }
        }
        if (latest != null && current != null) {
            _state.update { it.copy(isUpdateAvailable = Ready(current != latest)) }
        } else {
            _state.update { it.copy(isUpdateAvailable = Error(null)) }
        }
    }

    private suspend fun getLatestVersion(): Version? {
        @Suppress("KotlinConstantConditions")
        val baseUrl = System.getProperty("app.repositoryUrl") ?: "https://riftforeve.online/download"
        val url = "$baseUrl/metadata.properties?timestamp=${Instant.now().toEpochMilli()}"
        try {
            val body = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().body?.string() ?: ""
            }
            val line = body.lines().firstOrNull { it.startsWith("app.version") } ?: return null
            return getVersion(line.substringAfter("="))
        } catch (e: IOException) {
            logger.error(e) { "Could not check for updates" }
            return null
        }
    }

    private fun getVersion(string: String): Version? {
        try {
            val (major, minor, patch) = string.split(".").map { it.toInt() }
            return Version(major, minor, patch)
        } catch (e: Exception) {
            return null
        }
    }

    private fun getBuildTime(): String {
        val instant = Instant.ofEpochMilli(BuildConfig.buildTimestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        return "Built: ${formatter.format(dateTime)}"
    }

    private fun getExecutable(): String {
        return ProcessHandle.current().info().command().getOrDefault("")
    }
}

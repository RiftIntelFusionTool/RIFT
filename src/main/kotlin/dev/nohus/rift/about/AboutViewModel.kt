package dev.nohus.rift.about

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.ViewModel
import dev.nohus.rift.about.GetPatronsUseCase.Patron
import dev.nohus.rift.about.UpdateController.UpdateAvailability
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.AsyncResource.Loading
import dev.nohus.rift.network.AsyncResource.Ready
import dev.nohus.rift.network.HttpGetUseCase.CacheBehavior
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.openFileManager
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.jvm.optionals.getOrDefault

@Factory
class AboutViewModel(
    operatingSystem: OperatingSystem,
    private val appDirectories: AppDirectories,
    private val windowManager: WindowManager,
    getVersionUseCase: GetVersionUseCase,
    private val updateController: UpdateController,
    private val getPatrons: GetPatronsUseCase,
) : ViewModel() {

    data class UiState(
        val version: String,
        val buildTime: String,
        val updateAvailability: AsyncResource<UpdateAvailability>,
        val isUpdateDialogShown: Boolean,
        val isLegalDialogShown: Boolean,
        val isCreditsDialogShown: Boolean,
        val operatingSystem: OperatingSystem,
        val executablePath: String,
        val patrons: List<Patron>,
    )

    private val _state = MutableStateFlow(
        UiState(
            version = getVersionUseCase(),
            buildTime = getBuildTime(),
            updateAvailability = Loading,
            isUpdateDialogShown = false,
            isLegalDialogShown = false,
            isCreditsDialogShown = false,
            operatingSystem = operatingSystem,
            executablePath = getExecutable(),
            patrons = emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        checkForUpdate()
        checkPatrons()
    }

    private fun checkPatrons() {
        viewModelScope.launch {
            listOf(CacheBehavior.CacheOnly, CacheBehavior.NetworkOnly).forEach { cachedOnly ->
                getPatrons(cachedOnly).success?.let { patrons ->
                    _state.update { it.copy(patrons = patrons) }
                }
            }
        }
    }

    fun onUpdateClick() {
        _state.update { it.copy(isUpdateDialogShown = true) }
    }

    fun onTriggerUpdateClick() {
        updateController.triggerUpdate()
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
        val status = updateController.isUpdateAvailable()
        _state.update { it.copy(updateAvailability = Ready(status)) }
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

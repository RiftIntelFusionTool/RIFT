package dev.nohus.rift.startupwarning

import dev.nohus.rift.Event
import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.startupwarning.GetStartupWarningsUseCase.StartupWarning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class StartupWarningViewModel(
    @InjectedParam private val inputModel: StartupWarningInputModel,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val warnings: List<StartupWarning>,
        val closeEvent: Event? = null,
    )

    private val _state = MutableStateFlow(
        UiState(
            warnings = inputModel.warnings,
        ),
    )
    val state = _state.asStateFlow()

    fun onDoneClick(dismissedIds: List<String>) {
        settings.dismissedWarnings += dismissedIds
        _state.update { it.copy(closeEvent = Event()) }
    }
}

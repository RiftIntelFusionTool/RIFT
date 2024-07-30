package dev.nohus.rift.gamelogs

import dev.nohus.rift.Event
import dev.nohus.rift.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class NonEnglishEveClientWarningViewModel : ViewModel() {

    data class UiState(
        val closeEvent: Event? = null,
    )

    private val _state = MutableStateFlow(dev.nohus.rift.configurationpack.ConfigurationPackReminderViewModel.UiState())
    val state = _state.asStateFlow()

    fun onDoneClick() {
        _state.update { it.copy(closeEvent = Event()) }
    }
}

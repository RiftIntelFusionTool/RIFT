package dev.nohus.rift.configurationpack

import dev.nohus.rift.Event
import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class ConfigurationPackReminderViewModel(
    private val configurationPackRepository: ConfigurationPackRepository,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val suggestedConfigurationPack: ConfigurationPack? = null,
        val isSuccessful: Boolean = false,
        val closeEvent: Event? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        val suggested = configurationPackRepository.getSuggestedPack()
        if (suggested != null) {
            _state.update { it.copy(suggestedConfigurationPack = suggested) }
        } else {
            _state.update { it.copy(closeEvent = Event()) }
        }
    }

    fun onConfirmClick() {
        val pack = _state.value.suggestedConfigurationPack ?: return
        configurationPackRepository.set(pack)
        _state.update { it.copy(isSuccessful = true) }
    }

    fun onCancelClick() {
        settings.isConfigurationPackReminderDismissed = true
        _state.update { it.copy(closeEvent = Event()) }
    }

    fun onDoneClick() {
        _state.update { it.copy(closeEvent = Event()) }
    }
}

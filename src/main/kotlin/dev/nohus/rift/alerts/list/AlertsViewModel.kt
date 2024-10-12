package dev.nohus.rift.alerts.list

import dev.nohus.rift.ViewModel
import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.alerts.AlertAction
import dev.nohus.rift.alerts.AlertTrigger
import dev.nohus.rift.alerts.AlertsRepository
import dev.nohus.rift.alerts.JabberPingType
import dev.nohus.rift.alerts.create.CreateAlertInputModel
import dev.nohus.rift.alerts.creategroup.CreateGroupInputModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.sound.SoundPlayer
import dev.nohus.rift.utils.sound.SoundsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class AlertsViewModel(
    private val alertsRepository: AlertsRepository,
    private val settings: Settings,
    private val localCharactersRepository: LocalCharactersRepository,
    private val soundsRepository: SoundsRepository,
    private val soundPlayer: SoundPlayer,
    private val planetaryIndustryRepository: PlanetaryIndustryRepository,
) : ViewModel() {

    data class UiState(
        val alerts: List<Alert> = emptyList(),
        val expandedAlert: String? = null,
        val characters: List<LocalCharacter> = emptyList(),
        val sounds: List<Sound> = emptyList(),
        val isCreateAlertDialogOpen: CreateAlertInputModel? = null,
        val isCreateGroupDialogOpen: CreateGroupInputModel? = null,
        val groups: Set<String> = emptySet(),
        val colonies: List<ColonyItem> = emptyList(),
    )

    private val _state = MutableStateFlow(
        UiState(
            sounds = soundsRepository.getSounds(),
            groups = settings.alertGroups,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    alerts = settings.alerts.filterDeprecatedAlerts().sortedWith(AlertsComparator),
                )
            }
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        alerts = settings.alerts.filterDeprecatedAlerts().sortedWith(AlertsComparator),
                        groups = settings.alertGroups,
                    )
                }
            }
        }
        viewModelScope.launch {
            localCharactersRepository.characters.collect { items ->
                _state.update { it.copy(characters = items) }
            }
        }
        viewModelScope.launch {
            planetaryIndustryRepository.colonies.collect { resource ->
                resource.success?.values?.let { colonies ->
                    _state.update { it.copy(colonies = colonies.toList()) }
                }
            }
        }
    }

    fun onAlertClick(id: String) {
        val alert = _state.value.alerts.firstOrNull { it.id == id } ?: return
        val expandedAlert = if (_state.value.expandedAlert != alert.id) alert.id else null
        _state.update { it.copy(expandedAlert = expandedAlert) }
    }

    fun onToggleAlert(id: String, isEnabled: Boolean) {
        val alert = _state.value.alerts.firstOrNull { it.id == id } ?: return
        alertsRepository.add(alert.copy(isEnabled = isEnabled))
    }

    fun onGroupChange(id: String, group: String?) {
        val alert = _state.value.alerts.firstOrNull { it.id == id } ?: return
        settings.alerts = settings.alerts.map {
            if (it.id == alert.id) it.copy(group = group) else it
        }
    }

    fun onTestAlertSound(id: String) {
        val alert = _state.value.alerts.firstOrNull { it.id == id } ?: return
        val soundId = alert.actions.filterIsInstance<AlertAction.Sound>().firstOrNull()?.id
        if (soundId != null) {
            val sound = soundsRepository.getSounds().firstOrNull { it.id == soundId } ?: return
            soundPlayer.play(sound.resource)
        }
        val soundPath = alert.actions.filterIsInstance<AlertAction.CustomSound>().firstOrNull()?.path
        if (soundPath != null) {
            soundPlayer.playFile(soundPath)
        }
    }

    fun onEditAlertAction(id: String) {
        val alert = _state.value.alerts.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(isCreateAlertDialogOpen = CreateAlertInputModel.EditAction(alert)) }
    }

    fun onDeleteAlert(id: String) {
        alertsRepository.delete(id)
    }

    fun onCreateAlertClick() {
        _state.update { it.copy(isCreateAlertDialogOpen = CreateAlertInputModel.New) }
    }

    fun onCloseCreateAlert() {
        _state.update { it.copy(isCreateAlertDialogOpen = null) }
    }

    fun onCreateGroupClick() {
        _state.update { it.copy(isCreateGroupDialogOpen = CreateGroupInputModel.New) }
    }

    fun onCloseCreateGroup() {
        _state.update { it.copy(isCreateGroupDialogOpen = null) }
    }

    fun onCreateGroupConfirm(name: String) {
        when (val inputModel = _state.value.isCreateGroupDialogOpen) {
            CreateGroupInputModel.New -> {
                if (name.isNotBlank()) {
                    settings.alertGroups = (settings.alertGroups + name).toSet()
                }
            }
            is CreateGroupInputModel.Rename -> {
                if (name.isNotBlank()) {
                    settings.alertGroups = settings.alertGroups.map {
                        if (it == inputModel.name) name else it
                    }.toSet()
                    settings.alerts = settings.alerts.map { alert ->
                        if (alert.group == inputModel.name) alert.copy(group = name) else alert
                    }
                }
            }
            null -> {}
        }
        _state.update { it.copy(isCreateGroupDialogOpen = null) }
    }

    fun onGroupRenameClick(group: String) {
        _state.update { it.copy(isCreateGroupDialogOpen = CreateGroupInputModel.Rename(group)) }
    }

    fun onGroupDeleteClick(group: String) {
        settings.alerts = settings.alerts.map { alert ->
            if (alert.group == group) alert.copy(group = null) else alert
        }
        settings.alertGroups = settings.alertGroups.filterNot { it == group }.toSet()
    }

    fun onGroupToggleAlerts(group: String?) {
        val hasEnabledAlerts = settings.alerts.any { it.group == group && it.isEnabled }
        settings.alerts = settings.alerts.map {
            if (it.group == group) it.copy(isEnabled = !hasEnabledAlerts) else it
        }
    }

    private fun List<Alert>.filterDeprecatedAlerts(): List<Alert> {
        return filter { alert ->
            (alert.trigger as? AlertTrigger.JabberPing)?.pingType !is JabberPingType.Message
        }
    }
}

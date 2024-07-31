package dev.nohus.rift.pings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.RiftOpportunityBoxCategory
import dev.nohus.rift.compose.RiftOpportunityBoxCharacter
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.repositories.GetSystemDistanceFromCharacterUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.ZoneId

@Single
class PingsViewModel(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val getSystemDistanceFromCharacterUseCase: GetSystemDistanceFromCharacterUseCase,
    private val settings: Settings,
    private val jabberClient: JabberClient,
    private val windowManager: WindowManager,
    private val pingsRepository: PingsRepository,
    private val openMumbleUseCase: OpenMumbleUseCase,
) : ViewModel() {

    data class UiState(
        val pings: List<PingUiModel> = emptyList(),
        val displayTimezone: ZoneId,
        val isJabberConnected: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
            isJabberConnected = jabberClient.state.value.isConnected,
        ),
    )
    val state = _state.asStateFlow()

    private val pingUiModels = mutableMapOf<PingModel, PingUiModel>()

    init {
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update { it.copy(displayTimezone = settings.displayTimeZone) }
            }
        }
        viewModelScope.launch {
            jabberClient.state.map { it.isConnected }.collect { isConnected ->
                _state.update { it.copy(isJabberConnected = isConnected) }
            }
        }
        viewModelScope.launch {
            pingsRepository.pings.collect { pings ->
                _state.update { it.copy(pings = pings.map { it.toUiModel() }) }
            }
        }
    }

    fun onOpenJabberClick() {
        windowManager.onWindowOpen(WindowManager.RiftWindow.Jabber)
    }

    fun onMumbleClick(link: String) {
        viewModelScope.launch {
            openMumbleUseCase(link)
        }
    }

    private fun PingModel.toUiModel(): PingUiModel {
        pingUiModels[this]?.let { return it }
        return when (this) {
            is PingModel.PlainText -> PingUiModel.PlainText(
                timestamp = timestamp,
                sourceText = sourceText,
                text = text,
                sender = sender,
                target = target,
            )
            is PingModel.FleetPing -> PingUiModel.FleetPing(
                timestamp = timestamp,
                sourceText = sourceText,
                opportunityCategory = getOpportunityCategory(),
                description = description,
                fleetCommander = fleetCommander.toUiModel(),
                fleet = fleet,
                formupLocations = formupLocations.map { it.toUiModel() },
                papType = papType,
                comms = comms,
                doctrine = doctrine,
                target = target,
            )
        }.also { pingUiModels[this] = it }
    }

    private fun FormupLocation.toUiModel(): FormupLocationUiModel {
        return when (this) {
            is FormupLocation.Text -> FormupLocationUiModel.Text(text)
            is FormupLocation.System -> {
                val id = solarSystemsRepository.getSystemId(name) ?: return FormupLocationUiModel.Text(name)
                val security = solarSystemsRepository.getSystemSecurity(id) ?: return FormupLocationUiModel.Text(name)
                val distance = getSystemDistanceFromCharacterUseCase(id, maxDistance = 9, withJumpBridges = true)
                FormupLocationUiModel.System(name, security, distance)
            }
        }
    }

    private fun FleetCommander.toUiModel(): RiftOpportunityBoxCharacter {
        return RiftOpportunityBoxCharacter(
            name = name,
            id = id,
        )
    }

    private fun PingModel.getOpportunityCategory(): RiftOpportunityBoxCategory {
        return when (this) {
            is PingModel.PlainText -> RiftOpportunityBoxCategory.Unclassified
            is PingModel.FleetPing -> {
                if (listOf("wormhole", "[^a-z]wh[^a-z]").any { it.toRegex() in description.lowercase() }) {
                    RiftOpportunityBoxCategory.Explorer
                } else if (listOf("ess defense", "ess hack ", "hostiles in Delve", "structure defense").any { it in description.lowercase() }) {
                    RiftOpportunityBoxCategory.Enforcer
                } else if (listOf("pirating", "hunting").any { it in description.lowercase() }) {
                    RiftOpportunityBoxCategory.SoldierOfFortune
                } else {
                    if (papType == PapType.Strategic) {
                        RiftOpportunityBoxCategory.Enforcer
                    } else {
                        if (broadcastSource == "skirmishbot") {
                            RiftOpportunityBoxCategory.SoldierOfFortune
                        } else {
                            RiftOpportunityBoxCategory.Unclassified
                        }
                    }
                }
            }
        }
    }
}

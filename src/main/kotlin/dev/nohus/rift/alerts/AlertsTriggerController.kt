package dev.nohus.rift.alerts

import dev.nohus.rift.alerts.AlertTrigger.ChatMessage
import dev.nohus.rift.alerts.AlertTrigger.GameAction
import dev.nohus.rift.alerts.AlertTrigger.IntelReported
import dev.nohus.rift.alerts.AlertTrigger.JabberMessage
import dev.nohus.rift.alerts.AlertTrigger.JabberPing
import dev.nohus.rift.alerts.AlertTrigger.NoChannelActivity
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.gamelogs.GameLogAction
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.state.AlertTriggeringMessagesRepository
import dev.nohus.rift.intel.state.IntelUnderstanding
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.pings.FormupLocation
import dev.nohus.rift.pings.PingModel
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.repositories.GetSystemDistanceUseCase
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

/**
 * Responsible for checking events against alert conditions
 */
@Single
class AlertsTriggerController(
    private val settings: Settings,
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
    private val characterLocationRepository: CharacterLocationRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val alertsActionController: AlertsActionController,
    private val shipTypesRepository: ShipTypesRepository,
    private val alertTriggeringMessagesRepository: AlertTriggeringMessagesRepository,
) {

    private val enabledAlerts: List<Alert> get() = settings.alerts.filter { it.isEnabled }
    private val triggerTimestamps: MutableMap<String, Instant> = mutableMapOf()
    private val lastSeenMessagePerChannel = mutableMapOf<String, Instant>()
    private val alertedInactiveChannels = mutableSetOf<String>()
    private var loggedInTimestamp: Instant? = null
    private val combatStoppedTriggerController = CombatStoppedTriggerController { alert: Alert, action: GameLogAction, characterId: Int ->
        withCooldown(alert) {
            alertsActionController.triggerGameActionAlert(alert, action, characterId)
        }
    }

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                checkChannelActivity()
                combatStoppedTriggerController.checkPendingAlerts()
                delay(1000)
            }
        }
    }

    private data class TriggeredIntelAlert(
        val alert: Alert,
        val matchingEntities: List<Pair<IntelReportType, List<SystemEntity>>>,
        val entities: List<SystemEntity>,
        val locationMatch: AlertLocationMatch,
        val solarSystem: String,
    )

    fun onNewIntel(message: ParsedChannelChatMessage, understanding: IntelUnderstanding) {
        val triggeredIntelAlerts = enabledAlerts.mapNotNull { alert ->
            if (alert.trigger is IntelReported) {
                val matchingEntities = getMatchingEntities(alert.trigger.reportTypes, understanding)
                if (matchingEntities.isNotEmpty() && understanding.systems.isNotEmpty()) {
                    val reportSystem = understanding.systems.first()
                    val reportSystemId = solarSystemsRepository.getSystemId(reportSystem) ?: throw IllegalArgumentException("No system $reportSystem")
                    getMatchingAlertLocation(alert.trigger.reportLocation, reportSystemId)?.let { alertLocationMatch ->
                        withCooldown(alert) {
                            return@mapNotNull TriggeredIntelAlert(
                                alert = alert,
                                matchingEntities = matchingEntities,
                                entities = understanding.entities,
                                locationMatch = alertLocationMatch,
                                solarSystem = reportSystem,
                            )
                        }
                    }
                }
            }
            null
        }.sortedBy { triggeredIntelAlert ->
            when (val locationMatch = triggeredIntelAlert.locationMatch) {
                is AlertLocationMatch.Character -> locationMatch.distance
                is AlertLocationMatch.System -> locationMatch.distance
            }
        }
        if (triggeredIntelAlerts.isNotEmpty()) {
            alertTriggeringMessagesRepository.add(message)
        }
        triggeredIntelAlerts.forEach {
            alertsActionController.triggerIntelAlert(
                alert = it.alert,
                matchingEntities = it.matchingEntities,
                entities = it.entities,
                locationMatch = it.locationMatch,
                solarSystem = it.solarSystem,
            )
        }
    }

    fun onNewIntelMessage(message: ParsedChannelChatMessage) {
        val channel = message.metadata.channelName
        val timestamp = message.chatMessage.timestamp
        lastSeenMessagePerChannel[channel] = maxOf(timestamp, lastSeenMessagePerChannel[channel] ?: Instant.EPOCH)
        alertedInactiveChannels -= channel
    }

    fun onNewGameLogAction(action: GameLogAction, characterId: Int) {
        enabledAlerts.forEach { alert ->
            if (alert.trigger is GameAction) {
                val hasTriggered = alert.trigger.actionTypes.any { trigger ->
                    when (trigger) {
                        is GameActionType.InCombat -> {
                            action is GameLogAction.UnderAttack && action.target.containsNonNull(trigger.nameContaining) ||
                                action is GameLogAction.Attacking && action.target.containsNonNull(trigger.nameContaining) ||
                                action is GameLogAction.BeingWarpScrambled && action.target.containsNonNull(trigger.nameContaining)
                        }
                        is GameActionType.UnderAttack -> {
                            action is GameLogAction.UnderAttack && action.target.containsNonNull(trigger.nameContaining) ||
                                action is GameLogAction.BeingWarpScrambled && action.target.containsNonNull(trigger.nameContaining)
                        }
                        is GameActionType.Attacking -> {
                            action is GameLogAction.Attacking && action.target.containsNonNull(trigger.nameContaining)
                        }
                        GameActionType.BeingWarpScrambled -> action is GameLogAction.BeingWarpScrambled
                        is GameActionType.Decloaked -> {
                            action is GameLogAction.Decloaked && trigger.ignoredKeywords.none { it.lowercase() in action.by.lowercase() }
                        }
                        is GameActionType.CombatStopped -> {
                            if (action is GameLogAction.UnderAttack && action.target.containsNonNull(trigger.nameContaining)) {
                                combatStoppedTriggerController.onCombatAction(alert, trigger.durationSeconds, action.target, characterId)
                            } else if (action is GameLogAction.Attacking && action.target.containsNonNull(trigger.nameContaining)) {
                                combatStoppedTriggerController.onCombatAction(alert, trigger.durationSeconds, action.target, characterId)
                            } else if (action is GameLogAction.BeingWarpScrambled && action.target.containsNonNull(trigger.nameContaining)) {
                                combatStoppedTriggerController.onCombatAction(alert, trigger.durationSeconds, action.target, characterId)
                            }
                            false // Alert will be triggered after duration
                        }
                    }
                }
                if (hasTriggered) {
                    withCooldown(alert) {
                        alertsActionController.triggerGameActionAlert(alert, action, characterId)
                    }
                }
            }
        }
    }

    fun onNewChatMessage(channelChatMessage: ChannelChatMessage) {
        enabledAlerts.forEach { alert ->
            if (alert.trigger is ChatMessage) {
                val isChannelMatching = when (val channel = alert.trigger.channel) {
                    ChatMessageChannel.Any -> true
                    is ChatMessageChannel.Channel -> channel.name == channelChatMessage.metadata.channelName
                }
                if (isChannelMatching) {
                    val triggerSender = alert.trigger.sender
                    val isEveSystem = channelChatMessage.metadata.channelName == "Local" && channelChatMessage.chatMessage.author == "EVE System"
                    val isSenderMatching = triggerSender == null && !isEveSystem || channelChatMessage.chatMessage.author == triggerSender
                    if (isSenderMatching) {
                        val containing = alert.trigger.messageContaining
                        val isMessageMatching = channelChatMessage.chatMessage.message.lowercase().containsNonNull(containing?.lowercase())
                        if (isMessageMatching) {
                            withCooldown(alert) {
                                alertsActionController.triggerChatMessageAlert(alert, channelChatMessage)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onNewJabberMessage(chat: String, sender: String, message: String) {
        enabledAlerts.forEach { alert ->
            if (alert.trigger is JabberMessage) {
                val isChannelMatching = when (val channel = alert.trigger.channel) {
                    JabberMessageChannel.Any -> true
                    is JabberMessageChannel.Channel -> channel.name == chat
                    JabberMessageChannel.DirectMessage -> chat == sender
                }
                if (isChannelMatching) {
                    val triggerSender = alert.trigger.sender
                    val isDirectorbot = sender == "directorbot"
                    val isSenderMatching = triggerSender == null && !isDirectorbot || sender == triggerSender
                    if (isSenderMatching) {
                        val containing = alert.trigger.messageContaining
                        val isMessageMatching = message.lowercase().containsNonNull(containing?.lowercase())
                        if (isMessageMatching) {
                            withCooldown(alert) {
                                alertsActionController.triggerJabberMessageAlert(alert, chat, sender, message)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onNewJabberPing(ping: PingModel) {
        enabledAlerts.forEach { alert ->
            if (alert.trigger is JabberPing) {
                when (alert.trigger.pingType) {
                    is JabberPingType.Fleet -> {
                        if (ping is PingModel.FleetPing) {
                            val fleetPingAlert = alert.trigger.pingType
                            val isFleetCommanderMatching = if (fleetPingAlert.fleetCommanders.isEmpty()) {
                                true
                            } else {
                                ping.fleetCommander.name in fleetPingAlert.fleetCommanders
                            }
                            val isFormupSystemMatching = if (fleetPingAlert.formupSystem == null) {
                                true
                            } else {
                                ping.formupLocations.any { location ->
                                    when (location) {
                                        is FormupLocation.System -> location.name == fleetPingAlert.formupSystem
                                        is FormupLocation.Text -> false
                                    }
                                }
                            }
                            val isPapTypeMatching = when (fleetPingAlert.papType) {
                                PapType.Any -> true
                                PapType.Peacetime -> ping.papType == dev.nohus.rift.pings.PapType.Peacetime
                                PapType.Strategic -> ping.papType == dev.nohus.rift.pings.PapType.Strategic
                            }
                            val isDoctrineMatching = if (fleetPingAlert.doctrineContaining == null) {
                                true
                            } else {
                                ping.doctrine?.text?.contains(fleetPingAlert.doctrineContaining) == true
                            }
                            val isTargetMatching = if (fleetPingAlert.target == null) {
                                true
                            } else {
                                ping.target?.lowercase()?.contains(fleetPingAlert.target.lowercase()) == true
                            }
                            if (isFleetCommanderMatching && isFormupSystemMatching && isPapTypeMatching && isDoctrineMatching && isTargetMatching) {
                                alertsActionController.triggerJabberPingAlert(alert)
                            }
                        }
                    }
                    JabberPingType.Message -> {}
                    is JabberPingType.Message2 -> {
                        if (ping is PingModel.PlainText) {
                            val messagePingAlert = alert.trigger.pingType
                            val isTargetMatching = if (messagePingAlert.target == null) {
                                true
                            } else {
                                ping.target?.lowercase()?.contains(messagePingAlert.target.lowercase()) == true
                            }
                            if (isTargetMatching) {
                                alertsActionController.triggerJabberPingAlert(alert)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onNewPlanetaryIndustryAlert(alert: Alert, colonyItem: ColonyItem) {
        withCooldown(alert) {
            alertsActionController.triggerPlanetaryIndustryAlert(alert, colonyItem)
        }
    }

    private fun String.containsNonNull(needle: String?): Boolean {
        return needle == null || needle in this
    }

    private fun checkChannelActivity() {
        updateLoggedInTimestamp()
        val loggedInTimestamp = loggedInTimestamp ?: return

        enabledAlerts.forEach { alert ->
            if (alert.trigger is NoChannelActivity) {
                val maxInactivity = Duration.ofSeconds(alert.trigger.durationSeconds.toLong())
                if (maxInactivity > Duration.between(loggedInTimestamp, Instant.now())) return@forEach // Weren't logged in for enough time to pass for this alert

                val triggeredInactiveChannels: List<String> = when (alert.trigger.channel) {
                    IntelChannel.All -> {
                        val channels = settings.intelChannels.map { it.name }
                        val areAllInactive = channels.all { channel ->
                            isChannelInactive(channel, maxInactivity)
                        }
                        if (areAllInactive) channels else emptyList()
                    }
                    IntelChannel.Any -> {
                        val inactiveChannels = settings.intelChannels.filter { channel ->
                            isChannelInactive(channel.name, maxInactivity)
                        }
                        if (inactiveChannels.isNotEmpty()) {
                            inactiveChannels.map { it.name }
                        } else {
                            emptyList()
                        }
                    }
                    is IntelChannel.Channel -> {
                        if (isChannelInactive(alert.trigger.channel.name, maxInactivity)) {
                            listOf(alert.trigger.channel.name)
                        } else {
                            emptyList()
                        }
                    }
                }
                if (triggeredInactiveChannels.isNotEmpty()) {
                    triggeredInactiveChannels.forEach { channel ->
                        alertedInactiveChannels += channel
                    }
                    withCooldown(alert) {
                        alertsActionController.triggerInactiveChannelAlert(
                            alert = alert,
                            triggeredInactiveChannels = triggeredInactiveChannels,
                        )
                    }
                }
            }
        }
    }

    private fun updateLoggedInTimestamp() {
        if (loggedInTimestamp == null) { // We were offline
            if (onlineCharactersRepository.onlineCharacters.value.isNotEmpty()) {
                loggedInTimestamp = Instant.now() // We got online now
            }
        } else { // We were online
            if (onlineCharactersRepository.onlineCharacters.value.isEmpty()) {
                loggedInTimestamp = null // We got offline now
            }
        }
    }

    private fun isChannelInactive(channel: String, maxInactivity: Duration): Boolean {
        if (channel in alertedInactiveChannels) return false // Considered active because already alerted
        val timestamp = lastSeenMessagePerChannel[channel] ?: Instant.EPOCH
        return Duration.between(timestamp, Instant.now()) > maxInactivity
    }

    private fun getMatchingEntities(
        types: List<IntelReportType>,
        understanding: IntelUnderstanding,
    ): List<Pair<IntelReportType, List<SystemEntity>>> {
        return types.map { type ->
            type to when (type) {
                IntelReportType.AnyCharacter -> understanding.entities.filter {
                    it is SystemEntity.Character ||
                        it is SystemEntity.UnspecifiedCharacter ||
                        it is SystemEntity.Spike
                }
                is IntelReportType.SpecificCharacters ->
                    understanding.entities
                        .filterIsInstance<SystemEntity.Character>()
                        .filter { it.name in type.characters }
                IntelReportType.AnyShip -> understanding.entities.filterIsInstance<SystemEntity.Ship>()
                is IntelReportType.SpecificShipClasses ->
                    understanding.entities
                        .filterIsInstance<SystemEntity.Ship>()
                        .filter { shipTypesRepository.getShipClass(it.name) in type.classes }
                IntelReportType.Bubbles -> understanding.entities.filterIsInstance<SystemEntity.Bubbles>()
                IntelReportType.GateCamp -> understanding.entities.filterIsInstance<SystemEntity.GateCamp>()
                IntelReportType.Wormhole -> understanding.entities.filterIsInstance<SystemEntity.Wormhole>()
            }
        }.filter { it.second.isNotEmpty() }
    }

    sealed interface AlertLocationMatch {
        data class System(val systemId: Int, val distance: Int) : AlertLocationMatch
        data class Character(val characterId: Int, val distance: Int) : AlertLocationMatch
    }

    private fun getMatchingAlertLocation(location: IntelReportLocation, reportSystemId: Int): AlertLocationMatch? {
        return when (location) {
            is IntelReportLocation.System -> {
                val systemId = solarSystemsRepository.getSystemId(location.systemName)
                    ?: throw IllegalArgumentException("No system ${location.systemName}")
                val distance = getSystemDistanceUseCase(systemId, reportSystemId, location.jumpsRange.max, withJumpBridges = false) ?: Int.MAX_VALUE
                if (distance in location.jumpsRange) AlertLocationMatch.System(systemId, distance) else null
            }
            is IntelReportLocation.AnyOwnedCharacter -> {
                onlineCharactersRepository.onlineCharacters.value.firstNotNullOfOrNull { characterId ->
                    isCharacterWithinDistance(characterId, reportSystemId, location.jumpsRange)?.let {
                        AlertLocationMatch.Character(characterId, it)
                    }
                }
            }
            is IntelReportLocation.OwnedCharacter -> {
                if (location.characterId in onlineCharactersRepository.onlineCharacters.value) {
                    isCharacterWithinDistance(location.characterId, reportSystemId, location.jumpsRange)?.let {
                        AlertLocationMatch.Character(location.characterId, it)
                    }
                } else {
                    null
                }
            }
        }
    }

    private fun isCharacterWithinDistance(characterId: Int, systemId: Int, range: JumpRange): Int? {
        val characterSystemId = characterLocationRepository.locations.value[characterId]?.solarSystemId
        return if (characterSystemId != null) {
            val distance = getSystemDistanceUseCase(characterSystemId, systemId, range.max, withJumpBridges = settings.isUsingJumpBridgesForDistance) ?: Int.MAX_VALUE
            if (distance in range) distance else null
        } else {
            null
        }
    }

    private operator fun JumpRange.contains(distance: Int) = distance in min..max

    /**
     * Execute block if alert cooldown allows, and update the cooldown
     */
    private inline fun withCooldown(alert: Alert, block: () -> Unit) {
        val lastTriggered = triggerTimestamps[alert.id] ?: Instant.EPOCH
        val duration = Duration.between(lastTriggered, Instant.now())
        val cooldown = Duration.ofSeconds(alert.cooldownSeconds.toLong())
        if (duration > cooldown) {
            triggerTimestamps[alert.id] = Instant.now()
            block()
        }
    }
}

package dev.nohus.rift.intel.state

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.state.SystemEntity.Character
import dev.nohus.rift.intel.state.SystemEntity.Gate
import dev.nohus.rift.intel.state.SystemEntity.Killmail
import dev.nohus.rift.intel.state.SystemEntity.NoVisual
import dev.nohus.rift.intel.state.SystemEntity.Ship
import dev.nohus.rift.intel.state.SystemEntity.UnspecifiedCharacter
import dev.nohus.rift.killboard.KillmailProcessor.ProcessedKillmail
import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class IntelStateController(
    private val understandMessageUseCase: UnderstandMessageUseCase,
    private val settings: Settings,
    private val alertsTriggerController: AlertsTriggerController,
) {

    data class Dated<T>(
        val timestamp: Instant,
        val item: T,
    )

    private val systemContents = mutableMapOf<String, List<Dated<SystemEntity>>>()
    private val mutex = Mutex()
    private val _state = MutableStateFlow<Map<String, List<Dated<SystemEntity>>>>(emptyMap())
    val state = _state.asStateFlow()

    private fun updateState() {
        val expiryMinTimestamp = Instant.now() - Duration.ofSeconds(settings.intelExpireSeconds.toLong())
        _state.value = systemContents
            .mapValues { (_, datedEntities) ->
                datedEntities.filter { it.timestamp >= expiryMinTimestamp } // Filter out expired intel
            }
            .filterValues { it.isNotEmpty() }
            .toMap()
    }
    suspend fun submitKillmail(
        killmail: ProcessedKillmail,
    ) = mutex.withLock {
        if (killmail.timestamp.isAfter(Instant.now() - Duration.ofMinutes(15))) {
            killmail.victim?.let {
                removeKilledCharacters(listOf(it.name))
            }

            val entities: List<SystemEntity> = killmail.attackers + killmail.ships + killmail.killmail
            updateSystemEntities(killmail.timestamp, killmail.system, removeExisting = false, entities)

            updateState()
        }
    }

    suspend fun submitMessage(
        message: ParsedChannelChatMessage,
        context: List<ParsedChannelChatMessage>,
    ) = mutex.withLock {
        if (!isIntelChannel(message.metadata.channelName)) return

        val timestamp = message.chatMessage.timestamp
        val understanding = understandMessageUseCase(message.parsed)
        if (Duration.between(timestamp, Instant.now()) < Duration.ofSeconds(30)) {
            alertsTriggerController.onNewIntel(message, understanding)
            alertsTriggerController.onNewIntelMessage(message)
        }

        val system = if (understanding.systems.isNotEmpty()) {
            understanding.systems.first()
        } else { // No system in message, try to get from context
            val characters = understanding.entities.filterIsInstance<Character>().map { it.characterId }
            if (characters.isNotEmpty()) {
                val previousMessagesWithTheseCharacters = findMessagesWithCharacterIds(context, characters)
                val system = findSystemInMessages(previousMessagesWithTheseCharacters)
                system
            } else {
                null
            }
        }
        val isSystemExplicit = understanding.systems.isNotEmpty()

        if (
            system != null &&
            understanding.questions.isEmpty() // Don't use information if this is a question
        ) {
            var entities = understanding.entities
            if (understanding.movement != null) {
                val moved = entities.filterIsInstance<CharacterBound>()
                val toSystem = understanding.movement.toSystem
                removeSystemEntities(system, moved)
                updateSystemEntities(timestamp, toSystem, removeExisting = true, moved)
                removeMovedCharacters(toSystem, moved.filterIsInstance<Character>())
            }
            if (understanding.reportedNoVisual) {
                entities = entities + NoVisual
            }
            if (understanding.reportedClear) {
                updateSystemToClear(system)
                entities = entities.filter { it !is Clearable }
            }
            if (understanding.movement == null) {
                updateSystemEntities(timestamp, system, removeExisting = isSystemExplicit, entities)
                removeMovedCharacters(system, entities.filterIsInstance<Character>())
            }
        } else { // No system in message or context
            // TODO: Could be a continuation of a previous message or answer to a question
        }
        if (understanding.kills.isNotEmpty()) {
            removeKilledCharacters(understanding.kills.map { it.name })
        }
        if (understanding.questions.isNotEmpty()) {
            // TODO: Remember questions
        }
        removeEmptyEntities()
        removeEmptySystems()

        updateState()
    }

    private fun findMessagesWithCharacterIds(
        context: List<ParsedChannelChatMessage>,
        characterIds: List<Int>,
    ): List<ParsedChannelChatMessage> {
        return context.filter { previousMessage ->
            val characterIdsInPreviousMessage = previousMessage.parsed
                .flatMap { it.types }
                .filterIsInstance<ChatMessageParser.TokenType.Player>()
                .map { it.characterId }
            characterIds.all { it in characterIdsInPreviousMessage }
        }
    }

    private fun findSystemInMessages(
        messages: List<ParsedChannelChatMessage>,
    ): String? {
        return messages.firstNotNullOfOrNull { message ->
            message.parsed.flatMap { it.types }.filterIsInstance<ChatMessageParser.TokenType.System>().lastOrNull()?.name
        }
    }

    private fun updateSystemEntities(
        timestamp: Instant,
        system: String,
        removeExisting: Boolean,
        entities: List<SystemEntity>,
    ) {
        val existingContents = systemContents[system] ?: emptyList()
        val datedEntities = entities.map { Dated(timestamp, it) }
        val hasCharactersOrShips = entities.any { it is Character || it is UnspecifiedCharacter || it is Ship }
        val newContents = if (hasCharactersOrShips && removeExisting) {
            // We only want to remove existing characters if the system was explicitly in the message and not understood from context
            val remainingContents = existingContents.filter { it.item !is CharacterBound }
            remainingContents + datedEntities
        } else {
            existingContents + datedEntities
        }
        val deduplicated = deduplicateEntities(newContents)
        systemContents[system] = deduplicated
    }

    private fun removeSystemEntities(
        system: String,
        entities: List<SystemEntity>,
    ) {
        val existingContents = systemContents[system] ?: emptyList()
        val newContents = existingContents.filter { it.item !in entities }
        systemContents[system] = newContents
    }

    private fun updateSystemToClear(system: String) {
        val existingContents = systemContents[system] ?: emptyList()
        val remainingContents = existingContents.filter { it.item !is Clearable }
        systemContents[system] = remainingContents
    }

    /**
     * Remove these characters from anywhere except this system, and move their ships if needed
     */
    private fun removeMovedCharacters(
        systemTo: String,
        characters: List<Character>,
    ) {
        if (characters.isEmpty()) return
        systemContents.keys.forEach { system ->
            if (system == systemTo) return@forEach
            val charactersInSystem = systemContents[system]?.filter { it.item is Character } ?: emptyList()
            var filtered = systemContents[system]
                ?.filter { it.item !in characters }
                ?: emptyList()

            if (charactersInSystem.isNotEmpty() && charactersInSystem.none { it in filtered }) {
                val ships = systemContents[system]?.filter { it.item is Ship } ?: emptyList()
                if (ships.isNotEmpty() && ships.size <= charactersInSystem.size) {
                    // All characters moved from this system, and there were not more ships than characters, so the ships probably moved as well
                    filtered = filtered.filter { it !in ships }
                    addMovedShips(systemTo, ships)
                }
            }

            systemContents[system] = filtered
        }
    }

    /**
     * Add these ships that moved into a system with their characters, unless they are already in that system
     */
    private fun addMovedShips(
        systemTo: String,
        ships: List<Dated<SystemEntity>>,
    ) {
        val current = systemContents[systemTo] ?: emptyList()
        val currentShips = current.filter { it.item is Ship }.map { (it.item as Ship).name }
        val missingShips = ships.filter { ship ->
            (ship.item as Ship).name !in currentShips
        }
        systemContents[systemTo] = current + missingShips
    }

    /**
     * Remove systems with no entities from the entities map
     */
    private fun removeEmptySystems() {
        systemContents.keys.toList().forEach { key ->
            if (systemContents[key]?.isEmpty() == true) systemContents.remove(key)
        }
    }

    /**
     * Remove "no visual" entities from systems where there are no longer any characters
     * Remove "gate" entities from systems where they are the only entity
     */
    private fun removeEmptyEntities() {
        systemContents.keys.forEach { system ->
            val hasNoCharacters = systemContents[system]?.none { it.item is Character || it.item is UnspecifiedCharacter } ?: true
            if (hasNoCharacters) {
                val filtered = systemContents[system]?.filter { it.item !is NoVisual } ?: emptyList()
                systemContents[system] = filtered
            }
            val hasOnlyGate = systemContents[system]?.all { it.item is Gate } ?: false
            if (hasOnlyGate) {
                systemContents[system] = emptyList()
            }
        }
    }

    /**
     * Remove these characters from anywhere
     */
    private fun removeKilledCharacters(
        characterNames: List<String>,
    ) {
        systemContents.keys.forEach { system ->
            val filtered = systemContents[system]
                ?.filter { it.item !is Character || it.item.name !in characterNames }
                ?: emptyList()
            systemContents[system] = filtered
        }
    }

    /**
     * If the list contains multiple entities of the same type, only return the newest of each type
     */
    private fun deduplicateEntities(entities: List<Dated<SystemEntity>>): List<Dated<SystemEntity>> {
        // Characters, ships, and killmails don't get deduplicated, they stay in the list
        val characters = entities
            .filter { it.item is Character }
            .groupBy { (it.item as Character).characterId }
            .map { (id, list) -> list.maxBy { it.timestamp } }
        val ships = entities
            .filter { it.item is Ship }
            .groupBy { (it.item as Ship).name }
            .map { (id, list) -> list.maxBy { it.timestamp } }
        val killmails = entities
            .filter { it.item is Killmail }
        // Other entities get deduplicated, and only the newest instance remains
        val deduplicated = entities
            .groupBy { it.item::class }
            .map { (_, list) ->
                list.maxBy { it.timestamp }
            }
        return deduplicated
            .filter { it.item !is Character }
            .filter { it.item !is Ship }
            .filter { it.item !is Killmail } + characters + ships + killmails
    }

    private fun isIntelChannel(channelName: String): Boolean {
        return channelName in settings.intelChannels.map { it.name }
    }
}

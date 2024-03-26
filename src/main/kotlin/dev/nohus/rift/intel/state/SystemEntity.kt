package dev.nohus.rift.intel.state

import dev.nohus.rift.repositories.CharacterDetailsRepository.CharacterDetails

/**
 * An entity or event present in a system
 */
sealed interface SystemEntity {
    data class Character(
        val name: String,
        val characterId: Int,
        val details: CharacterDetails,
    ) : SystemEntity, CharacterBound

    data class UnspecifiedCharacter(
        val count: Int,
    ) : SystemEntity, CharacterBound

    data class Ship(
        val name: String,
        val count: Int,
        val isFriendly: Boolean? = null,
    ) : SystemEntity, CharacterBound

    data class Gate(
        val system: String,
        val isAnsiblex: Boolean,
    ) : SystemEntity, CharacterBound

    data class Killmail(
        val url: String,
        val ship: String?,
    ) : SystemEntity, Clearable

    data object Wormhole : SystemEntity
    data object Spike : SystemEntity, Clearable
    data object Ess : SystemEntity
    data object GateCamp : SystemEntity, Clearable
    data object CombatProbes : SystemEntity
    data object NoVisual : SystemEntity, CharacterBound
    data object Bubbles : SystemEntity
}

// Marker for system entities that go away if the character situation changes
interface CharacterBound : SystemEntity, Clearable

// Marker for entities that go away when a system is reported clear
interface Clearable : SystemEntity

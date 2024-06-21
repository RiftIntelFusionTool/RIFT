package dev.nohus.rift.network.zkillboard

import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.repositories.CharacterDetailsRepository
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class KillmailProcessor(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val intelStateController: IntelStateController,
    private val shipTypesRepository: ShipTypesRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
) {

    data class ProcessedKillmail(
        val system: String,
        val ships: List<SystemEntity.Ship>,
        val victim: SystemEntity.Character?,
        val attackers: List<SystemEntity.Character>,
        val killmail: SystemEntity.Killmail,
        val timestamp: Instant,
    )

    fun submit(message: KillboardMessage) {
        runBlocking {
            val ago = Duration.between(message.killmailTime, Instant.now())
            val system = solarSystemsRepository.getSystemName(message.solarSystemId)
                ?: return@runBlocking // Not in K-space

            val killmail = SystemEntity.Killmail(
                url = message.zkb.url,
                ship = shipTypesRepository.getShipName(message.victim.shipTypeId),
            )
            val deferredVictim = message.victim.characterId
                ?.let { async { characterDetailsRepository.getCharacterDetails(it) } }
            val deferredAttackers = message.attackers
                .mapNotNull { it.characterId }
                .map { async { characterDetailsRepository.getCharacterDetails(it) } }

            val victim = deferredVictim?.await()?.let {
                SystemEntity.Character(it.name, it.characterId, it)
            }
            val attackers = deferredAttackers.awaitAll().filterNotNull().map {
                SystemEntity.Character(it.name, it.characterId, it)
            }
            val ships = message.attackers
                .mapNotNull { attacker ->
                    val shipName = shipTypesRepository.getShipName(attacker.shipTypeId) ?: return@mapNotNull null
                    val isFriendly = attackers.firstOrNull { it.characterId == attacker.characterId }?.details?.isFriendly ?: false
                    isFriendly to shipName
                }
                .groupBy { it.first }
                .mapValues { (isFriendly, ships) ->
                    ships
                        .map { it.second }
                        .groupBy { it }
                        .map { (name, ships) -> SystemEntity.Ship(name, ships.size, isFriendly = isFriendly) }
                }
                .flatMap { it.value }

            val processedKillmail = ProcessedKillmail(
                system = system,
                ships = ships,
                victim = victim,
                attackers = attackers,
                killmail = killmail,
                timestamp = message.killmailTime,
            )

            logger.debug { "Kill: ${killmail.ship} in ${processedKillmail.system}, ${ago.toSeconds()}s ago" }
            intelStateController.submitKillmail(processedKillmail)
        }
    }
}

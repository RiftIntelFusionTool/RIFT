package dev.nohus.rift.killboard

import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.repositories.CharacterDetailsRepository
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class KillmailProcessor(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val intelStateController: IntelStateController,
    private val typeRepository: TypesRepository,
    private val shipTypesRepository: ShipTypesRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val standingsRepository: StandingsRepository,
) {

    data class ProcessedKillmail(
        val system: String,
        val ships: List<SystemEntity.Ship>,
        val victim: SystemEntity.Character?,
        val attackers: List<SystemEntity.Character>,
        val killmail: SystemEntity.Killmail,
        val timestamp: Instant,
    )

    private val seenKillmails = mutableMapOf<Int, Killboard>()
    private val mutex = Mutex()

    fun submit(message: Killmail) {
        runBlocking(Dispatchers.Default) {
            val ago = Duration.between(message.killmailTime, Instant.now())
            val system = solarSystemsRepository.getSystemName(message.solarSystemId)
                ?: return@runBlocking // Not in K-space

            val deferredVictim = message.victim.characterId
                ?.let { async { characterDetailsRepository.getCharacterDetails(it) } }

            // Corporation and alliance is only loaded if there is no character, otherwise they are included with the character
            val deferredVictimCorporation = if (message.victim.characterId == null) {
                message.victim.corporationId?.let {
                    async { characterDetailsRepository.getCorporationName(it).success }
                }
            } else {
                null
            }
            val deferredVictimAlliance = if (message.victim.characterId == null) {
                message.victim.allianceId?.let {
                    async { characterDetailsRepository.getAllianceName(it).success }
                }
            } else {
                null
            }

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
                    val standing = attackers.firstOrNull { it.characterId == attacker.characterId }?.details?.standing ?: Standing.Neutral
                    standing to shipName
                }
                .groupBy { it.first }
                .mapValues { (standing, ships) ->
                    ships
                        .map { it.second }
                        .groupBy { it }
                        .map { (name, ships) -> SystemEntity.Ship(name, ships.size, standing = standing) }
                }
                .flatMap { it.value }
            val standing = standingsRepository.getStanding(message.victim.allianceId, message.victim.corporationId, message.victim.characterId)

            val killmailVictim = SystemEntity.KillmailVictim(
                characterId = message.victim.characterId,
                details = victim?.details,
                corporationId = message.victim.corporationId ?: victim?.details?.corporationId,
                corporationName = victim?.details?.corporationName ?: deferredVictimCorporation?.await()?.name,
                corporationTicker = victim?.details?.corporationTicker ?: deferredVictimCorporation?.await()?.ticker,
                allianceId = message.victim.allianceId ?: victim?.details?.allianceId,
                allianceName = victim?.details?.allianceName ?: deferredVictimAlliance?.await()?.name,
                allianceTicker = victim?.details?.allianceTicker ?: deferredVictimAlliance?.await()?.ticker,
                standing = standing,
            )
            val killmail = SystemEntity.Killmail(
                url = message.url,
                ship = shipTypesRepository.getShipName(message.victim.shipTypeId),
                typeName = message.victim.shipTypeId?.let { typeRepository.getTypeName(it) },
                victim = killmailVictim,
            )

            val processedKillmail = ProcessedKillmail(
                system = system,
                ships = ships,
                victim = victim,
                attackers = attackers,
                killmail = killmail,
                timestamp = message.killmailTime,
            )

            mutex.withLock {
                if (message.killmailId !in seenKillmails) {
                    seenKillmails[message.killmailId] = message.killboard
                    logger.debug { "Kill from ${message.killboard}: ${killmail.ship} killed by ${ships.joinToString { it.name }} in ${processedKillmail.system}, ${ago.toSeconds()}s ago" }
                    intelStateController.submitKillmail(processedKillmail)
                } else {
                    val killboard = seenKillmails[message.killmailId]
                    logger.debug { "Kill from ${message.killboard}, ignoring, already seen from $killboard" }
                }
            }
        }
    }
}

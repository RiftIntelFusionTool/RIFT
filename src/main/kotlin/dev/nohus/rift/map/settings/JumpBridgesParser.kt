package dev.nohus.rift.map.settings

import dev.nohus.rift.repositories.JumpBridgesRepository.JumpBridgeConnection
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class JumpBridgesParser(
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    data class JumpBridgeNetwork(
        val connections: List<JumpBridgeConnection>,
    )

    suspend fun parse(text: String): JumpBridgeNetwork? = withContext(Dispatchers.Default) {
        if ("The Webway" in text) {
            parseGoonswarmWikiJumpBridges(text)
        } else {
            parseAnyJumpBridges(text)
        }
    }

    private fun parseGoonswarmWikiJumpBridges(text: String): JumpBridgeNetwork? {
        val regex = """^[^\t]*\t(?<from>[A-z0-9-]+) @[^\t]*\t(?<to>[A-z0-9-]+) @[^\t]*\t(?<online>[^\t]+)\t.*$""".toRegex()
        val connections = text.lines().mapNotNull { regex.find(it) }.mapNotNull { match ->
            val from = match["from"]
            val to = match["to"]
            val online = match["online"]
            if (online == "Online") {
                val fromSystem = solarSystemsRepository.getSystem(from) ?: return@mapNotNull null
                val toSystem = solarSystemsRepository.getSystem(to) ?: return@mapNotNull null
                JumpBridgeConnection(fromSystem, toSystem)
            } else {
                null
            }
        }
        return if (connections.size > 100) {
            JumpBridgeNetwork(connections)
        } else {
            null
        }
    }

    private fun parseAnyJumpBridges(text: String): JumpBridgeNetwork? {
        val systems = solarSystemsRepository.mapSolarSystems.map { it.name }
        val connections = text.lines().mapNotNull { line ->
            systems
                .filter { it in line }
                .sortedBy { line.indexOf(it) }
                .takeIf { it.size == 2 }
        }.mapNotNull {
            val fromSystem = solarSystemsRepository.getSystem(it[0]) ?: return@mapNotNull null
            val toSystem = solarSystemsRepository.getSystem(it[1]) ?: return@mapNotNull null
            JumpBridgeConnection(fromSystem, toSystem)
        }.distinct()
        return if (connections.size >= 2) {
            JumpBridgeNetwork(connections)
        } else {
            null
        }
    }
}

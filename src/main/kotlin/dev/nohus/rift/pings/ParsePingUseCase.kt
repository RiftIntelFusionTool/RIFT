package dev.nohus.rift.pings

import dev.nohus.rift.repositories.CharactersRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class ParsePingUseCase(
    private val charactersRepository: CharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
) {
    suspend operator fun invoke(
        timestamp: Instant,
        text: String,
    ): PingModel? {
        val cleanText = text
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            .replace("PAP \nType:", "\nPAP Type:")
            .replace("Doctrine:", "\nDoctrine:")

        if (!cleanText.contains("~~~ This was")) return null // Not a ping

        val fleetCommanderKeys = listOf("FC Name", "FC")
        val fleetKeys = listOf("Fleet name", "Fleet")
        val formupKeys = listOf("Formup Location", "Formup")
        val papKeys = listOf("PAP Type")
        val commsKeys = listOf("Comms")
        val doctrineKeys = listOf("Doctrine")
        val allKeys = fleetCommanderKeys + fleetKeys + formupKeys + papKeys + commsKeys + doctrineKeys

        val fleetCommander = getValue(cleanText, fleetCommanderKeys)?.let { parseFleetCommander(it) }
        val fleet = getValue(cleanText, fleetKeys)
        val formupLocation = getValue(cleanText, formupKeys)?.let { parseFormupLocation(it) }
        val papType = getValue(cleanText, papKeys)?.let { parsePapType(it) }
        val comms = getValue(cleanText, commsKeys)?.let { parseComms(it) }
        val doctrine = getValue(cleanText, doctrineKeys)?.let { parseDoctrine(it) }

        val description = cleanText.lines()
            .asSequence()
            .map { it.trim() }
            .filterNot { it.startsWith("~~~ This was") }
            .let { lines ->
                var filteredLines = lines.toList()
                while (true) {
                    val indices = getValueIndices(filteredLines, allKeys) ?: break
                    filteredLines = filteredLines.withIndex().filter { it.index !in indices }.map { it.value }
                }
                filteredLines
            }
            .windowed(2) { (a, b) -> if (a.isEmpty() && b.isEmpty()) listOf() else listOf(a) }
            .flatten()
            .joinToString("\n")
            .trim()

        val signature = cleanText.lines().lastOrNull()?.let { lastLine ->
            val regex = """~~~ This was a (?<source>.*) ?broadcast from (?<sender>.*) to (?<target>.*) at .* ~~~""".toRegex()
            regex.find(lastLine)
        }
        val broadcastSource = signature?.groups?.get("source")?.value?.trim()?.takeIf { it.isNotBlank() }
        val sender = signature?.groups?.get("sender")?.value?.trim()?.takeIf { it.isNotBlank() }
        val target = signature?.groups?.get("target")?.value?.trim()?.takeIf { it.isNotBlank() }

        return if (fleetCommander != null) {
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = text,
                description = description,
                fleetCommander = fleetCommander,
                fleet = fleet,
                formupSystem = formupLocation,
                papType = papType,
                comms = comms,
                doctrine = doctrine,
                broadcastSource = broadcastSource,
                target = target,
            )
        } else {
            val plainText = cleanText.lines()
                .takeWhile { !it.startsWith("~~~ This was") }
                .joinToString("\n")
                .trim()
            PingModel.PlainText(
                timestamp = timestamp,
                sourceText = text,
                text = plainText,
                sender = sender,
                target = target,
            )
        }
    }

    private suspend fun parseFleetCommander(text: String): FleetCommander {
        val characterId = charactersRepository.getCharacterId(text)
        return FleetCommander(text, characterId)
    }

    private fun parseFormupLocation(text: String): FormupLocation {
        val system = solarSystemsRepository.getSystemName(text, regionHint = null)
        return if (system != null) FormupLocation.System(system) else FormupLocation.Text(text)
    }

    private fun parsePapType(text: String): PapType {
        return when {
            text.lowercase().startsWith("strat") -> PapType.Strategic
            text.lowercase().startsWith("peace") -> PapType.Peacetime
            else -> PapType.Text(text)
        }
    }

    private fun parseComms(text: String): Comms {
        val regex = """(?<channel>.*) (?<link>https://gnf\.lt/.*\.html)""".toRegex()
        regex.find(text)?.let { match ->
            return Comms.Mumble(
                channel = match.groups["channel"]!!.value,
                link = match.groups["link"]!!.value,
            )
        }
        return Comms.Text(text)
    }

    private fun parseDoctrine(text: String): Doctrine {
        return Doctrine(
            text = text,
            link = getDoctrineLink(text),
        )
    }

    private fun getDoctrineLink(text: String): String? {
        val doctrines = mapOf(
            "CFI" to "https://goonfleet.com/index.php/topic/353938-active-strat-cyclone-fleet-issue/",
            "SuperTrains" to "https://goonfleet.com/index.php/topic/342568-active-strat-supertrains-mainfleet-editionrokh/",
            "Techfleet" to "https://goonfleet.com/index.php/topic/327228-active%E2%80%94strat%E2%80%94techfleet/",
            "OSPREY NAVY ISSUE" to "https://goonfleet.com/index.php/topic/341744-active-peacetime-osprey-navy-issues/",
            "thrasher fleet issue" to "https://goonfleet.com/index.php/topic/349435-active-peacetime-tfis/",
            "ENI" to "https://goonfleet.com/index.php/topic/349390-active-peacetime-eni-fleet/",
            "Void Rays" to "https://goonfleet.com/index.php/topic/345055-active-strat-void-rays-mwd-kikis/",
            "FNI" to "https://goonfleet.com/index.php/topic/357801-active-strat-hammerfleet-fni/",
            "Mallet" to "https://goonfleet.com/index.php/topic/295651-active%E2%80%94strat%E2%80%94malletfleet-feroxes/",
            "Ferox" to "https://goonfleet.com/index.php/topic/295651-active%E2%80%94strat%E2%80%94malletfleet-feroxes/",
            "Leshaks" to "https://goonfleet.com/index.php/topic/337953-active-strat-leshaks-the-electric-heat-gun/",
            "dakka" to "https://goonfleet.com/index.php/topic/349471-active-strat-dakka-fleet-20-return-of-the-dakka/",
            "Sacs" to "https://goonfleet.com/index.php/topic/334896-active-strat-sacfleet-20-sacrilege/",
            "Stormbringers" to "https://goonfleet.com/index.php/topic/335865-active-strat-stormbringers-your-electric-gimmick-here/",
            "Harpy Fleet" to "https://goonfleet.com/index.php/topic/346057-active-strat-harpyfleet/",
            "Torp Bombers" to "https://goonfleet.com/index.php/topic/344458-active-strat-siegefleet-40-torp-bombers/",
            "Flycatchers" to "https://goonfleet.com/index.php/topic/326958-active-strat-flycatchers/",
            "BOMB BOMBERS" to "https://goonfleet.com/index.php/topic/312491-active-strat-bomb-bombers/",
            "hurricane" to "https://goonfleet.com/index.php/topic/295648-active-strat-mosh-masters-hurricanes/",
            "Kestrel" to "https://goonfleet.com/index.php/topic/314644-active-peacetime-kestrels/",
            "Caracal" to "https://goonfleet.com/index.php/topic/293358-active-peacetime-caracals/",
            "Cormorant" to "https://goonfleet.com/index.php/topic/299033-active-peacetime-cormorants/",
            "Tornado" to "https://goonfleet.com/index.php/topic/296788-active-peacetime-windrunners-tornados/",
        )
        if (text.contains("(")) {
            val name = text.substringBefore("(")
            doctrines.entries
                .firstOrNull { it.key.lowercase() in name.lowercase() }
                ?.value
                ?.let { return it }
        }
        return doctrines.entries
            .firstOrNull { it.key.lowercase() in text.lowercase() }
            ?.value
    }

    private fun getValue(text: String, keys: List<String>): String? {
        val lines = text.lines()
        val start = lines.indexOfFirst { line -> keys.any { "$it:" in line } }
        if (start < 0) return null
        val key = keys.firstOrNull { lines[start].startsWith(it) } ?: return null
        return lines.drop(start).withIndex()
            .takeWhile { (index, line) -> index == 0 || (":" !in line && line.isNotBlank()) }
            .joinToString("\n") { it.value }
            .removePrefix("$key:")
            .trim()
    }

    private fun getValueIndices(lines: List<String>, keys: List<String>): List<Int>? {
        val start = lines.indexOfFirst { line -> keys.any { "$it:" in line } }
        if (start < 0) return null
        return lines.drop(start).withIndex()
            .takeWhile { (index, line) -> index == 0 || (":" !in line && line.isNotBlank()) }
            .map { start + it.index }
    }
}

package dev.nohus.rift.network.evescout

import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormStrength.Strong
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormStrength.Weak
import dev.nohus.rift.repositories.GetSystemsInRangeUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class GetMetaliminalStormsUseCase(
    private val eveScoutRescueApi: EveScoutRescueApi,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val getSystemsInRangeUseCase: GetSystemsInRangeUseCase,
) {

    enum class StormType {
        Gamma, Electric, Plasma, Exotic
    }

    enum class StormStrength {
        Strong, Weak
    }

    data class Storm(
        val type: StormType,
        val strength: StormStrength,
    )

    suspend operator fun invoke(): Map<Int, List<Storm>> {
        val storms = getStormCenters()
        return storms.flatMap { storm ->
            val core = getSystemsInRangeUseCase(storm.key, 1)
            val periphery = getSystemsInRangeUseCase(storm.key, 3) - core
            core.map { it to Storm(storm.value, Strong) } + periphery.map { it to Storm(storm.value, Weak) }
        }.groupBy({ it.first }, { it.second })
    }

    private suspend fun getStormCenters(): Map<Int, StormType> {
        return when (val response = eveScoutRescueApi.getStormTrack()) {
            is Success -> {
                val document = Jsoup.parse(response.data)
                val rows = document.select("table > tbody > tr")
                rows.mapNotNull { row ->
                    val cells = row.getElementsByTag("td").map { it.text() }
                    if (cells.size == 7) {
                        val system = solarSystemsRepository.getSystemId(cells[1]) ?: run {
                            logger.warn { "Unknown storm system: ${cells[1]}" }
                            return@mapNotNull null
                        }
                        val type = when (cells[3]) {
                            "Gamma" -> StormType.Gamma
                            "Electric" -> StormType.Electric
                            "Plasma" -> StormType.Plasma
                            "Exotic" -> StormType.Exotic
                            else -> {
                                logger.warn { "Unknown storm type: ${cells[3]}" }
                                return@mapNotNull null
                            }
                        }
                        system to type
                    } else {
                        null
                    }
                }.toMap()
            }
            is Failure -> {
                logger.error { "Could not get storms from EvE-Scout: ${response.cause}" }
                emptyMap()
            }
        }
    }
}

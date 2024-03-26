package dev.nohus.rift.intel.state

import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.adashboardinfo.AdashboardInfoApi
import dev.nohus.rift.network.dscaninfo.DscanInfoApi
import dev.nohus.rift.repositories.ShipTypesRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class UnderstandRemoteDscanUseCase(
    private val dscanInfoApi: DscanInfoApi,
    private val adashboardInfoApi: AdashboardInfoApi,
    private val shipTypesRepository: ShipTypesRepository,
) {

    /**
     * If tokens contain a link to dscan.info or adashboard.info,
     * returns the list of system entities in that scan.
     * Returns an empty list otherwise.
     */
    suspend operator fun invoke(tokens: List<ChatMessageParser.Token>): List<SystemEntity> {
        val urls = tokens
            .filter { it.types.any { it is ChatMessageParser.TokenType.Url } }
            .mapNotNull { it.words.singleOrNull() }

        val aDashboardId = urls.firstNotNullOfOrNull {
            val regex = """https://adashboard\.info/intel/dscan/view/(?<id>[0-9A-z]{8})""".toRegex()
            regex.find(it)?.groups?.get("id")?.value
        }
        val dScanInfoId = urls.firstNotNullOfOrNull {
            val regex = """https://dscan\.info/v/(?<id>[0-9a-f]{12})""".toRegex()
            regex.find(it)?.groups?.get("id")?.value
        }

        return if (aDashboardId != null) {
            getAdashboardInfoScan(aDashboardId)
        } else if (dScanInfoId != null) {
            getDscanInfoScan(dScanInfoId)
        } else {
            emptyList()
        }
    }

    private suspend fun getDscanInfoScan(id: String): List<SystemEntity> {
        return when (val response = dscanInfoApi.getScan(id)) {
            is Success -> {
                response.data.ships?.mapNotNull { ship ->
                    shipTypesRepository.getShip(ship.name)?.let { name ->
                        SystemEntity.Ship(
                            name = name,
                            count = ship.count,
                        )
                    }
                } ?: emptyList()
            }
            is Failure -> {
                logger.error { "Could not get scan from dscan.info: ${response.cause}" }
                emptyList()
            }
        }
    }

    private suspend fun getAdashboardInfoScan(id: String): List<SystemEntity> {
        return when (val response = adashboardInfoApi.getScan(id)) {
            is Success -> {
                val document = Jsoup.parse(response.data)
                val allShipsTitle = document.getElementsContainingOwnText("All ships").singleOrNull()
                val table = allShipsTitle?.siblingElements()?.singleOrNull()
                return table?.getElementsByTag("tr")?.mapNotNull { row ->
                    val elements = row.getElementsByTag("td")
                    if (elements.size == 2) {
                        val shipName = elements[0].text()
                        val count = elements[1].text().toIntOrNull() ?: return@mapNotNull null
                        shipTypesRepository.getShip(shipName)?.let { name ->
                            SystemEntity.Ship(name, count)
                        }
                    } else {
                        null
                    }
                } ?: run {
                    logger.error { "Could not parse adashboard.info scan: $id" }
                    emptyList()
                }
            }
            is Failure -> {
                logger.error { "Could not get scan from adashboard.info: ${response.cause}" }
                emptyList()
            }
        }
    }
}

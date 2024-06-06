package dev.nohus.rift.repositories

import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.MarketsPrice
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class PricesRepository(
    private val esiApi: EsiApi,
) {

    private var prices: Map<Int, MarketsPrice> = emptyMap()
    private var loadedTimestamp = Instant.EPOCH
    private val mutex = Mutex()

    suspend fun refreshPrices() {
        loadPricesIfNeeded()
    }

    fun getPrice(typeId: Int): Double? {
        return prices[typeId]?.averagePrice
    }

    private suspend fun loadPricesIfNeeded() {
        mutex.withLock {
            if (Duration.between(loadedTimestamp, Instant.now()) > Duration.ofMinutes(10)) {
                loadPrices()?.let {
                    prices = it
                    loadedTimestamp = Instant.now()
                }
            }
        }
    }

    private suspend fun loadPrices(): Map<Int, MarketsPrice>? {
        return when (val result = esiApi.getMarketsPrices()) {
            is Success -> result.data.associateBy { it.typeId }
            is Failure -> null
        }
    }
}

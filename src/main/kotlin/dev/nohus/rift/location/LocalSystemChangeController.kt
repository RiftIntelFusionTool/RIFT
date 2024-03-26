package dev.nohus.rift.location

import dev.nohus.rift.repositories.SolarSystemsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class LocalSystemChangeController(
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    data class SystemChange(
        val characterId: Int,
        val systemId: Int,
        val timestamp: Instant,
    )

    private val maxAge = Duration.ofSeconds(10)
    private val scope = CoroutineScope(Job())
    private val _characterSystemChanges = MutableSharedFlow<SystemChange>()
    val characterSystemChanges = _characterSystemChanges.asSharedFlow()

    fun onSystemChangeMessage(
        system: String,
        timestamp: Instant,
        characterId: Int,
    ) {
        if (Duration.between(timestamp, Instant.now()) > maxAge) return
        val systemId = solarSystemsRepository.getSystemId(system) ?: return
        scope.launch {
            _characterSystemChanges.emit(SystemChange(characterId, systemId, timestamp))
        }
    }
}

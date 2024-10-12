package dev.nohus.rift.splash

import dev.nohus.rift.ViewModel
import dev.nohus.rift.about.GetPatronsUseCase
import dev.nohus.rift.about.GetPatronsUseCase.Patron
import dev.nohus.rift.about.GetVersionUseCase
import dev.nohus.rift.network.HttpGetUseCase.CacheBehavior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class SplashViewModel(
    getVersion: GetVersionUseCase,
    private val getPatrons: GetPatronsUseCase,
) : ViewModel() {

    data class UiState(
        val version: String,
        val patrons: List<Patron>,
    )

    private val _state = MutableStateFlow(
        UiState(
            version = getVersion(),
            patrons = emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        checkPatrons()
    }

    private fun checkPatrons() {
        viewModelScope.launch {
            listOf(CacheBehavior.CacheOnly, CacheBehavior.NetworkOnly).forEach { cachedOnly ->
                getPatrons(cachedOnly).success?.let { patrons ->
                    _state.update { it.copy(patrons = patrons) }
                }
            }
        }
    }
}

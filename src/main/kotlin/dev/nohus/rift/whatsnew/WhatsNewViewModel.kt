package dev.nohus.rift.whatsnew

import dev.nohus.rift.ViewModel
import dev.nohus.rift.about.GetPatronsUseCase
import dev.nohus.rift.about.GetPatronsUseCase.Patron
import dev.nohus.rift.network.HttpGetUseCase.CacheBehavior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class WhatsNewViewModel(
    private val getPatrons: GetPatronsUseCase,
) : ViewModel() {

    data class Point(
        val text: String,
        val isHighlighted: Boolean,
    )

    data class Version(
        val version: String,
        val points: List<Point>,
    )

    data class UiState(
        val versions: List<Version> = emptyList(),
        val patrons: List<Patron> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState(WhatsNew.getVersions()))
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

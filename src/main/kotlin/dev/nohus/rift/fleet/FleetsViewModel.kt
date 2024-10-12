package dev.nohus.rift.fleet

import dev.nohus.rift.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class FleetsViewModel(
    private val fleetsRepository: FleetsRepository,
) : ViewModel() {

    data class UiState(
        val fleets: List<Fleet> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fleetsRepository.fleets.collect { fleets ->
                _state.update { it.copy(fleets = fleets.values.toList()) }
            }
        }
    }

    fun onCheckNowClick() {
        viewModelScope.launch {
            fleetsRepository.updateNow()
        }
    }
}

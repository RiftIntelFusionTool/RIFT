package dev.nohus.rift.whatsnew

import dev.nohus.rift.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class WhatsNewViewModel : ViewModel() {

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
    )

    private val _state = MutableStateFlow(UiState(WhatsNew.getVersions()))
    val state = _state.asStateFlow()
}

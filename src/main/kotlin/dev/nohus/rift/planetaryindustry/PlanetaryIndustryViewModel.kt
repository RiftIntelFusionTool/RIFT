package dev.nohus.rift.planetaryindustry

import dev.nohus.rift.ViewModel
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.DetailsView
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.GridView
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.ListView
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryViewModel.View.RowsView
import dev.nohus.rift.settings.persistence.ColonySortingFilter
import dev.nohus.rift.settings.persistence.ColonyView
import dev.nohus.rift.settings.persistence.PlanetaryIndustry
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class PlanetaryIndustryViewModel(
    private val planetaryIndustryRepository: PlanetaryIndustryRepository,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val colonies: AsyncResource<List<ColonyItem>> = AsyncResource.Loading,
        val sortingFilter: ColonySortingFilter,
        val view: View,
    )

    sealed interface View {
        data object ListView : View
        data object GridView : View
        data object RowsView : View
        data class DetailsView(val item: ColonyItem) : View
    }

    private var featureSettings: PlanetaryIndustry
        get() = settings.planetaryIndustry
        set(value) {
            settings.planetaryIndustry = value
        }

    private val _state = MutableStateFlow(
        UiState(
            sortingFilter = featureSettings.sortingFilter,
            view = featureSettings.view.asView(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            planetaryIndustryRepository.colonies.collect { resource ->
                val items = resource.map { it.values.toList().sort(_state.value.sortingFilter) }
                _state.update { it.copy(colonies = items) }
            }
        }
    }

    fun onReloadClick() {
        viewModelScope.launch {
            planetaryIndustryRepository.reload(true)
        }
    }

    fun onRequestSimulation() {
        viewModelScope.launch {
            planetaryIndustryRepository.requestSimulation()
        }
    }

    fun onVisibilityChange(visible: Boolean) {
        planetaryIndustryRepository.setNeedsRealtimeUpdates(visible)
        if (visible) {
            viewModelScope.launch {
                planetaryIndustryRepository.requestSimulation()
                planetaryIndustryRepository.reload(false)
            }
        }
    }

    fun onViewChange(colonyView: ColonyView) {
        val view = colonyView.asView()
        featureSettings = featureSettings.copy(view = colonyView)
        _state.update { it.copy(view = view) }
    }

    fun onDetailsClick(id: String) {
        val item = _state.value.colonies.success?.firstOrNull { it.colony.id == id } ?: return
        _state.update { it.copy(view = DetailsView(item)) }
    }

    fun onBackClick() {
        val view = featureSettings.view.asView()
        _state.update { it.copy(view = view) }
    }

    private fun ColonyView.asView(): View = when (this) {
        ColonyView.List -> ListView
        ColonyView.Grid -> GridView
        ColonyView.Rows -> RowsView
    }

    fun onSortingFilterChange(sorting: ColonySortingFilter) {
        featureSettings = featureSettings.copy(sortingFilter = sorting)
        _state.update { it.copy(sortingFilter = sorting, colonies = it.colonies.map { it.sort(sorting) }) }
    }

    private fun List<ColonyItem>.sort(sorting: ColonySortingFilter): List<ColonyItem> {
        val characterSelector: (ColonyItem) -> Comparable<*> = { it.colony.characterId }
        val statusSelector: (ColonyItem) -> Comparable<*> = { it.colony.status.order }
        val expiryTimeSelector: (ColonyItem) -> Comparable<*> = { item ->
            item.ffwdColony.currentSimTime.takeIf { it.isAfter(item.colony.currentSimTime) } ?: Instant.MAX
        }
        return when (sorting) {
            ColonySortingFilter.Character -> {
                sortedWith(compareBy(characterSelector, statusSelector, expiryTimeSelector))
            }

            ColonySortingFilter.Status -> {
                sortedWith(compareBy(statusSelector, expiryTimeSelector, characterSelector))
            }

            ColonySortingFilter.ExpiryTime -> {
                sortedWith(compareBy(expiryTimeSelector, statusSelector, characterSelector))
            }
        }
    }
}

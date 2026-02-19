package paulify.baeumeinwien.ui.screens.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.local.AgeRangeCount
import paulify.baeumeinwien.data.local.DistrictCount
import paulify.baeumeinwien.data.local.SpeciesCount
import paulify.baeumeinwien.data.repository.TreeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InfoViewModel(
    private val repository: TreeRepository
) : ViewModel() {

    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val totalCount = repository.getTreeCount()
                val topSpeciesRaw = repository.getTopSpecies()
                val districtCounts = repository.getTreesByDistrict()
                val ageRanges = repository.getTreesByAgeRange()

                val topSpecies = topSpeciesRaw
                    .filter { it.speciesGerman != "Jungbaum wird gepflanzt" }
                    .take(5)

                _statistics.value = Statistics(
                    totalTrees = if (totalCount > 0) totalCount else 600000,
                    topSpecies = if (topSpecies.isNotEmpty()) topSpecies else getStaticTopSpecies(),
                    districtCounts = if (districtCounts.isNotEmpty()) {
                        (1..23).map { districtId ->
                            districtCounts.find { it.district == districtId }
                                ?: DistrictCount(districtId, 0)
                        }
                    } else getStaticDistrictCounts(),
                    ageRanges = if (ageRanges.isNotEmpty()) ageRanges else getStaticAgeRanges()
                )
            } catch (e: Exception) {
                _statistics.value = Statistics(
                    totalTrees = 600000,
                    topSpecies = getStaticTopSpecies(),
                    districtCounts = getStaticDistrictCounts(),
                    ageRanges = getStaticAgeRanges()
                )
            }
        }
    }

    private fun getStaticTopSpecies() = listOf(
        SpeciesCount("Ahorn", 120000),
        SpeciesCount("Linde", 95000),
        SpeciesCount("Esche", 75000),
        SpeciesCount("Kastanie", 60000),
        SpeciesCount("Platane", 45000)
    )

    private fun getStaticDistrictCounts() = (1..23).map { 
        DistrictCount(it, 25000 + (it * 1000)) 
    }

    private fun getStaticAgeRanges() = listOf(
        AgeRangeCount("0-9 Jahre", 50000),
        AgeRangeCount("10-19 Jahre", 120000),
        AgeRangeCount("20-49 Jahre", 250000),
        AgeRangeCount("50-99 Jahre", 150000),
        AgeRangeCount("100+ Jahre", 30000)
    )
}

data class Statistics(
    val totalTrees: Int,
    val topSpecies: List<SpeciesCount>,
    val districtCounts: List<DistrictCount>,
    val ageRanges: List<AgeRangeCount>
)
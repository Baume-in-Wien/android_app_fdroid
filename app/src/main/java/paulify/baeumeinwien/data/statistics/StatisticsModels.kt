package paulify.baeumeinwien.data.statistics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SpeciesStatistic(
    val species: String,
    @SerialName("total_count") val totalCount: Long,
    val percentage: Double
)

@Serializable
data class SpeciesByDistrict(
    val district: Int,
    val species: String,
    val count: Long,
    @SerialName("percentage_in_district") val percentageInDistrict: Double
)

@Serializable
data class DistrictStatistic(
    val district: Int,
    @SerialName("total_trees") val totalTrees: Long,
    @SerialName("unique_species") val uniqueSpecies: Long,
    @SerialName("avg_trunk_circumference") val avgTrunkCircumference: Double?,
    @SerialName("avg_height") val avgHeight: Double?,
    @SerialName("oldest_tree_year") val oldestTreeYear: Int?,
    @SerialName("newest_tree_year") val newestTreeYear: Int?
)

@Serializable
data class SpeciesAgeStatistic(
    val species: String,
    @SerialName("total_count") val totalCount: Long,
    @SerialName("oldest_year") val oldestYear: Int?,
    @SerialName("newest_year") val newestYear: Int?,
    @SerialName("avg_age") val avgAge: Double?
)

@Serializable
data class TotalTreeCount(
    val total: Long
)

data class TreeStatistics(
    val topSpecies: List<SpeciesStatistic>,
    val districtStats: List<DistrictStatistic>,
    val totalTrees: Long,
    val uniqueSpecies: Int
)

data class DistrictTreeStatistics(
    val district: Int,
    val topSpecies: List<SpeciesByDistrict>,
    val totalTrees: Long,
    val uniqueSpecies: Long
)

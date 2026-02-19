package paulify.baeumeinwien.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityTreeInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("user_display_name") val userDisplayName: String?,
    @SerialName("species_german") val speciesGerman: String,
    @SerialName("species_scientific") val speciesScientific: String?,
    val latitude: Double,
    val longitude: Double,
    val district: Int? = null,
    val street: String? = null,
    @SerialName("estimated_height") val estimatedHeight: Double? = null,
    @SerialName("estimated_trunk_circumference") val estimatedTrunkCircumference: Int? = null,
    @SerialName("gps_accuracy_meters") val gpsAccuracyMeters: Double? = null,
    @SerialName("location_method") val locationMethod: String = "gps"
)

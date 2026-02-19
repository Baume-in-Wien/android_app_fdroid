package paulify.baeumeinwien.data.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityTree(
    val id: String,
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
    @SerialName("location_method") val locationMethod: String = "gps",
    val status: String = "approved",
    @SerialName("confirmation_count") val confirmationCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_role") val userRole: String? = null,
    @SerialName("user_is_verified") val userIsVerified: Boolean? = null,
    @SerialName("show_creator_name") val showCreatorName: Boolean? = null
) {
    val displayName: String
        get() = speciesScientific?.let { "$speciesGerman ($it)" } ?: speciesGerman

    val creatorDisplayText: String
        get() = when {
            userRole == "official" && userIsVerified == true -> "Bäume in Wien Official Team"
            showCreatorName == false -> "Community-Mitglied"
            else -> userDisplayName ?: "Community-Mitglied"
        }

    val officialStatusText: String?
        get() = when {
            isOfficialTree -> "Überprüft vom offiziellen Team"
            status == "verified" -> "Vom offiziellen Bäume in Wien Team verifiziert"
            else -> null
        }

    val isOfficialTree: Boolean
        get() = userRole == "official" && userIsVerified == true

    val isVerified: Boolean
        get() = status == "verified" || isOfficialTree
}

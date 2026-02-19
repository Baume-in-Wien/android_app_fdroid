package paulify.baeumeinwien.data.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TreeSpecies(
    val id: Int,
    @SerialName("name_german") val nameGerman: String,
    @SerialName("name_scientific") val nameScientific: String?,
    val category: String?
) {
    val displayName: String
        get() = nameScientific?.let { "$nameGerman ($it)" } ?: nameGerman
}

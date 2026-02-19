package paulify.baeumeinwien.data.domain

import kotlinx.serialization.Serializable

@Serializable
data class Tree(
    val id: String,
    val speciesGerman: String,
    val speciesScientific: String?,
    val plantYear: Int?,
    val trunkCircumference: Int?,
    val height: Double?,
    val crownDiameter: Double?,
    val district: Int?,
    val area: String?,
    val street: String?,
    val latitude: Double,
    val longitude: Double,
    val fid: String?,
    val objectId: String?,
    val baumId: String?,
    val datenfuehrung: String?,
    val isFavorite: Boolean = false
) {
    val estimatedAge: Int?
        get() = plantYear?.let { 
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            currentYear - it 
        }
    
    val displayName: String
        get() = speciesScientific?.let { "$speciesGerman ($it)" } ?: speciesGerman
}

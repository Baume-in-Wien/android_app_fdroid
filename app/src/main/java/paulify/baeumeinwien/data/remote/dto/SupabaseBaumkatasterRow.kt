package paulify.baeumeinwien.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBaumkatasterRow(
    @SerialName("BAUM_ID")
    val baumId: String,
    
    @SerialName("OBJEKT_STRASSE")
    val objektStrasse: String? = null,
    
    @SerialName("GATTUNG_ART")
    val gattungArt: String? = null,
    
    @SerialName("PFLANZJAHR")
    val pflanzjahr: Int? = null,
    
    @SerialName("BEZIRK")
    val bezirk: Int? = null,
    
    @SerialName("SHAPE")
    val shape: String? = null
)

package paulify.baeumeinwien.data.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("display_name") val displayName: String?,
    @SerialName("show_name_on_trees") val showNameOnTrees: Boolean = true,
    val role: String = "user",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    val isAdmin: Boolean get() = role == "admin"
    val isOfficial: Boolean get() = role == "official"
}

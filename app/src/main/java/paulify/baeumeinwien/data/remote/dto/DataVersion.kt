package paulify.baeumeinwien.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DataVersion(
    val version: String,
    val dataUrl: String,
    val treeCount: Int,
    val lastUpdated: String,
    val changelog: String
)

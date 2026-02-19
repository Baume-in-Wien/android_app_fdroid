package paulify.baeumeinwien.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeoJsonResponse(
    val type: String,
    val features: List<Feature>,
    val crs: Crs?
)

@JsonClass(generateAdapter = true)
data class Feature(
    val type: String,
    val id: String?,
    val geometry: Geometry,
    val properties: BaumProperties
)

@JsonClass(generateAdapter = true)
data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

@JsonClass(generateAdapter = true)
data class BaumProperties(
    @Json(name = "FID") val fid: String?,
    @Json(name = "OBJECTID") val objectId: String?,
    @Json(name = "BAUM_ID") val baumId: String?,
    @Json(name = "DATENFUEHRUNG") val datenfuehrung: String?,
    @Json(name = "BAUMNUMMER") val treeNumber: String?,
    @Json(name = "GATTUNG_ART") val speciesName: String?,
    @Json(name = "PFLANZJAHR") val plantYear: Int?,
    @Json(name = "PFLANZJAHR_TXT") val plantYearText: String?,
    @Json(name = "STAMMUMFANG") val trunkCircumference: Int?,
    @Json(name = "STAMMUMFANG_TXT") val trunkCircumferenceText: String?,
    @Json(name = "BAUMHOEHE") val heightCategory: Int?,
    @Json(name = "BAUMHOEHE_TXT") val heightText: String?,
    @Json(name = "KRONENDURCHMESSER") val crownDiameterCategory: Int?,
    @Json(name = "KRONENDURCHMESSER_TXT") val crownDiameterText: String?,
    @Json(name = "BEZIRK") val district: Int?,
    @Json(name = "OBJEKT_STRASSE") val objectStreet: String?,
    @Json(name = "GEBIETSGRUPPE") val areaGroup: String?
)

@JsonClass(generateAdapter = true)
data class Crs(
    val type: String,
    val properties: CrsProperties?
)

@JsonClass(generateAdapter = true)
data class CrsProperties(
    val name: String?
)

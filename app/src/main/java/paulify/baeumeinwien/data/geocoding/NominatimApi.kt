package paulify.baeumeinwien.data.geocoding

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countryCode: String = "at",
        @Query("bounded") bounded: Int = 1,
        @Query("viewbox") viewbox: String = "16.18,48.11,16.58,48.33"
    ): List<NominatimResult>
}

data class NominatimResult(
    val place_id: Long,
    val display_name: String,
    val lat: String,
    val lon: String,
    val address: NominatimAddress?
)

data class NominatimAddress(
    val road: String?,
    val house_number: String?,
    val postcode: String?,
    val city: String?,
    val suburb: String?
)

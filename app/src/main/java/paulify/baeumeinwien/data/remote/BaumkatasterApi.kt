package paulify.baeumeinwien.data.remote

import paulify.baeumeinwien.data.remote.dto.GeoJsonResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BaumkatasterApi {

    @GET("BAUMKATOGD.json")
    suspend fun getTrees(
        @Query("bbox") bbox: String? = null,
        @Query("count") count: Int? = null,
        @Query("startIndex") startIndex: Int? = null
    ): GeoJsonResponse
    
    @GET("BAUMKATOGD_{district}.json")
    suspend fun getTreesByDistrict(
        @Path("district") district: Int
    ): GeoJsonResponse
    
    @GET("version.json")
    suspend fun getVersion(): paulify.baeumeinwien.data.remote.dto.DataVersion

    @GET
    suspend fun getTreesWfs(
        @retrofit2.http.Url url: String,
        @retrofit2.http.QueryMap options: Map<String, String>
    ): GeoJsonResponse
}


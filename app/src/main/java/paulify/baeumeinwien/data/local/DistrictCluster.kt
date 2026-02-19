package paulify.baeumeinwien.data.local

import androidx.room.DatabaseView

@DatabaseView(
    "SELECT district, " +
    "COUNT(*) as treeCount, " +
    "AVG(latitude) as centerLat, " +
    "AVG(longitude) as centerLon " +
    "FROM trees " +
    "WHERE district IS NOT NULL " +
    "GROUP BY district"
)
data class DistrictCluster(
    val district: Int,
    val treeCount: Int,
    val centerLat: Double,
    val centerLon: Double
)

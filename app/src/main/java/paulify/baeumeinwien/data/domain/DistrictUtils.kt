package paulify.baeumeinwien.data.domain

object DistrictUtils {
    private val centers = mapOf(
        1 to Pair(48.2082, 16.3738),
        2 to Pair(48.2167, 16.4000),
        3 to Pair(48.1983, 16.3967),
        4 to Pair(48.1917, 16.3667),
        5 to Pair(48.1883, 16.3567),
        6 to Pair(48.1950, 16.3500),
        7 to Pair(48.2017, 16.3500),
        8 to Pair(48.2117, 16.3483),
        9 to Pair(48.2217, 16.3567),
        10 to Pair(48.1500, 16.3833),
        11 to Pair(48.1667, 16.4500),
        12 to Pair(48.1667, 16.3167),
        13 to Pair(48.1833, 16.2667),
        14 to Pair(48.2000, 16.2667),
        15 to Pair(48.1967, 16.3333),
        16 to Pair(48.2133, 16.3167),
        17 to Pair(48.2250, 16.3167),
        18 to Pair(48.2333, 16.3333),
        19 to Pair(48.2500, 16.3333),
        20 to Pair(48.2333, 16.3667),
        21 to Pair(48.2667, 16.4000),
        22 to Pair(48.2333, 16.4667),
        23 to Pair(48.1500, 16.2833)
    )

    fun getClosestDistrict(lat: Double, lng: Double): Int {
        return centers.minByOrNull { (_, coords) ->
            val (dLat, dLng) = coords
            val latDiff = lat - dLat
            val lngDiff = lng - dLng
            latDiff * latDiff + lngDiff * lngDiff
        }?.key ?: 1
    }
}

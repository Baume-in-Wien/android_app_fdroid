package paulify.baeumeinwien.ui.screens.ar

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.data.repository.TreeRepository
import kotlinx.coroutines.flow.*
import kotlin.math.*

data class ArTreeOverlay(
    val tree: Tree,
    val distance: Float,
    val bearing: Float,
    val isVisible: Boolean
)

data class ArUiState(
    val nearbyTrees: List<ArTreeOverlay> = emptyList(),
    val userLocation: Location? = null,
    val deviceBearing: Float = 0f,
    val devicePitch: Float = 0f,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)


class ArViewModel(
    private val repository: TreeRepository
) : ViewModel() {
    
    private val _userLocation = MutableStateFlow<Location?>(null)
    private val _deviceBearing = MutableStateFlow(0f)
    private val _devicePitch = MutableStateFlow(0f)
    private val _errorMessage = MutableStateFlow<String?>(null)
    
    private val nearbyTreesFlow = _userLocation
        .filterNotNull()
        .flatMapLatest { location ->
            repository.getNearbyTrees(
                centerLat = location.latitude,
                centerLon = location.longitude,
                radiusMeters = 100.0
            )
        }
    
    private data class ArCombined(
        val trees: List<Tree>,
        val location: Location?,
        val bearing: Float,
        val pitch: Float,
        val error: String?
    )

    private val basicCombined = combine(
        nearbyTreesFlow,
        _userLocation,
        _deviceBearing,
        _devicePitch,
        _errorMessage
    ) { trees, location, bearing, pitch, error ->
        ArCombined(
            trees = trees,
            location = location,
            bearing = bearing,
            pitch = pitch,
            error = error
        )
    }

    val uiState: StateFlow<ArUiState> = basicCombined.map { combined ->
        val nearbyTrees = if (combined.location != null) {
            calculateNearbyTrees(combined.trees, combined.bearing)
        } else {
            emptyList()
        }

        ArUiState(
            nearbyTrees = nearbyTrees,
            userLocation = combined.location,
            deviceBearing = combined.bearing,
            devicePitch = combined.pitch,
            isLoading = combined.location == null,
            errorMessage = combined.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArUiState()
    )
    
    fun updateLocation(location: Location) {
        _userLocation.value = location
    }
    
    fun updateBearing(bearing: Float) {
        _deviceBearing.value = bearing
    }
    
    fun updatePitch(pitch: Float) {
        _devicePitch.value = pitch
    }
    
    private fun calculateNearbyTrees(
        nearbyTrees: List<Tree>,
        deviceBearing: Float
    ): List<ArTreeOverlay> {
        val viewAngle = 60f
        val location = _userLocation.value ?: return emptyList()
        
        return nearbyTrees.mapNotNull { tree ->
            val distance = calculateDistance(
                location.latitude,
                location.longitude,
                tree.latitude,
                tree.longitude
            )
            
            val bearing = calculateBearing(
                location.latitude,
                location.longitude,
                tree.latitude,
                tree.longitude
            )
            
            var relativeBearing = bearing - deviceBearing
            while (relativeBearing > 180) relativeBearing -= 360
            while (relativeBearing < -180) relativeBearing += 360
            
            val isVisible = abs(relativeBearing) <= viewAngle / 2
            
            ArTreeOverlay(
                tree = tree,
                distance = distance,
                bearing = relativeBearing,
                isVisible = isVisible
            )
        }
        .sortedBy { it.distance }
        .take(50)
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000f
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return (earthRadius * c).toFloat()
    }
    
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        
        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        
        bearing = (bearing + 360) % 360
        
        return bearing
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}


package paulify.baeumeinwien.ui.screens.community

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import paulify.baeumeinwien.data.domain.TreeSpecies
import paulify.baeumeinwien.data.remote.dto.CommunityTreeInsert
import paulify.baeumeinwien.data.repository.AuthRepository
import paulify.baeumeinwien.data.repository.CommunityTreeRepository
import paulify.baeumeinwien.data.repository.Result

class AddTreeViewModel(
    private val communityTreeRepository: CommunityTreeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    enum class AddTreeStep {
        GPS_ACQUISITION,
        MAP_PLACEMENT,
        SPECIES_SELECTION,
        OPTIONAL_DETAILS,
        CONFIRMATION
    }

    enum class GpsStatus {
        WAITING, POOR, MODERATE, GOOD, EXCELLENT;

        companion object {
            fun fromAccuracy(meters: Float): GpsStatus = when {
                meters > 25f -> POOR
                meters > 10f -> MODERATE
                meters > 5f -> GOOD
                else -> EXCELLENT
            }
        }
    }

    data class AddTreeUiState(
        val step: AddTreeStep = AddTreeStep.GPS_ACQUISITION,
        val selectedSpecies: TreeSpecies? = null,
        val speciesSearchQuery: String = "",
        val speciesResults: List<TreeSpecies> = emptyList(),
        val isSearchingSpecies: Boolean = false,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val gpsAccuracy: Float? = null,
        val gpsStatus: GpsStatus = GpsStatus.WAITING,
        val locationMethod: String = "gps",
        val estimatedHeight: String = "",
        val estimatedTrunkCircumference: String = "",
        val isSubmitting: Boolean = false,
        val submitSuccess: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(AddTreeUiState())
    val uiState: StateFlow<AddTreeUiState> = _uiState.asStateFlow()

    private var locationListener: LocationListener? = null
    private var locationManager: LocationManager? = null
    private var speciesSearchJob: Job? = null


    @SuppressLint("MissingPermission")
    fun startGpsAcquisition(context: Context) {
        _uiState.value = _uiState.value.copy(
            step = AddTreeStep.GPS_ACQUISITION,
            gpsStatus = GpsStatus.WAITING
        )

        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = mgr

        val listener = LocationListener { location ->
            val accuracy = location.accuracy
            _uiState.value = _uiState.value.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                gpsAccuracy = accuracy,
                gpsStatus = GpsStatus.fromAccuracy(accuracy)
            )
        }
        locationListener = listener

        val provider = if (mgr.isProviderEnabled(LocationManager.GPS_PROVIDER))
            LocationManager.GPS_PROVIDER
        else
            LocationManager.NETWORK_PROVIDER

        try {
            mgr.requestLocationUpdates(provider, 500L, 0f, listener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    fun stopGpsUpdates() {
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        locationListener = null
        locationManager = null
    }

    fun useCurrentGpsLocation() {
        stopGpsUpdates()
        _uiState.value = _uiState.value.copy(
            step = AddTreeStep.SPECIES_SELECTION,
            locationMethod = "gps"
        )
    }

    fun switchToMapPlacement() {
        stopGpsUpdates()
        _uiState.value = _uiState.value.copy(step = AddTreeStep.MAP_PLACEMENT)
    }

    fun onMapLocationSelected(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lon,
            locationMethod = "map_placement",
            gpsAccuracy = null
        )
    }

    fun confirmMapLocation() {
        _uiState.value = _uiState.value.copy(step = AddTreeStep.SPECIES_SELECTION)
    }


    fun searchSpecies(query: String) {
        _uiState.value = _uiState.value.copy(speciesSearchQuery = query)
        speciesSearchJob?.cancel()

        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(speciesResults = emptyList(), isSearchingSpecies = false)
            return
        }

        speciesSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearchingSpecies = true)
            val results = communityTreeRepository.searchSpecies(query)
            _uiState.value = _uiState.value.copy(
                speciesResults = results,
                isSearchingSpecies = false
            )
        }
    }

    fun selectSpecies(species: TreeSpecies) {
        _uiState.value = _uiState.value.copy(
            selectedSpecies = species,
            speciesSearchQuery = species.nameGerman,
            speciesResults = emptyList()
        )
    }


    fun updateHeight(height: String) {
        _uiState.value = _uiState.value.copy(estimatedHeight = height)
    }

    fun updateTrunkCircumference(circumference: String) {
        _uiState.value = _uiState.value.copy(estimatedTrunkCircumference = circumference)
    }


    fun goToNextStep() {
        val current = _uiState.value
        val nextStep = when (current.step) {
            AddTreeStep.GPS_ACQUISITION -> AddTreeStep.SPECIES_SELECTION
            AddTreeStep.MAP_PLACEMENT -> AddTreeStep.SPECIES_SELECTION
            AddTreeStep.SPECIES_SELECTION -> AddTreeStep.OPTIONAL_DETAILS
            AddTreeStep.OPTIONAL_DETAILS -> AddTreeStep.CONFIRMATION
            AddTreeStep.CONFIRMATION -> AddTreeStep.CONFIRMATION
        }
        _uiState.value = current.copy(step = nextStep)
    }

    fun goToPreviousStep() {
        val current = _uiState.value
        val prevStep = when (current.step) {
            AddTreeStep.GPS_ACQUISITION -> AddTreeStep.GPS_ACQUISITION
            AddTreeStep.MAP_PLACEMENT -> AddTreeStep.GPS_ACQUISITION
            AddTreeStep.SPECIES_SELECTION -> AddTreeStep.GPS_ACQUISITION
            AddTreeStep.OPTIONAL_DETAILS -> AddTreeStep.SPECIES_SELECTION
            AddTreeStep.CONFIRMATION -> AddTreeStep.OPTIONAL_DETAILS
        }
        _uiState.value = current.copy(step = prevStep)
    }


    fun submitTree() {
        val state = _uiState.value
        val userId = authRepository.getCurrentUserId()
        val displayName = authRepository.getCurrentDisplayName()

        if (userId == null) {
            _uiState.value = state.copy(error = "Nicht angemeldet")
            return
        }
        if (state.selectedSpecies == null) {
            _uiState.value = state.copy(error = "Bitte Baumart wählen")
            return
        }
        if (state.latitude == null || state.longitude == null) {
            _uiState.value = state.copy(error = "Kein Standort")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)

            val dto = CommunityTreeInsert(
                userId = userId,
                userDisplayName = displayName,
                speciesGerman = state.selectedSpecies.nameGerman,
                speciesScientific = state.selectedSpecies.nameScientific,
                latitude = state.latitude,
                longitude = state.longitude,
                estimatedHeight = state.estimatedHeight.toDoubleOrNull(),
                estimatedTrunkCircumference = state.estimatedTrunkCircumference.toIntOrNull(),
                gpsAccuracyMeters = state.gpsAccuracy?.toDouble(),
                locationMethod = state.locationMethod
            )

            val result = communityTreeRepository.addCommunityTree(dto)
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Tree submitted successfully: ${result.data.id}")
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "Tree submission failed", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message ?: "Fehler beim Speichern"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopGpsUpdates()
    }

    companion object {
        private const val TAG = "AddTreeViewModel"
    }
}

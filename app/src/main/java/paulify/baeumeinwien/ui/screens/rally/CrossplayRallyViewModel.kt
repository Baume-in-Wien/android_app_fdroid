package paulify.baeumeinwien.ui.screens.rally

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.domain.*
import paulify.baeumeinwien.data.realtime.RallyRealtimeManager
import paulify.baeumeinwien.data.repository.CrossplayRallyRepository
import paulify.baeumeinwien.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.provider.Settings
import android.util.Log

import paulify.baeumeinwien.data.repository.TreeRepository

class CrossplayRallyViewModel(
    context: Context,
    private val repository: CrossplayRallyRepository,
    private val realtimeManager: RallyRealtimeManager,
    private val treeRepository: TreeRepository
) : ViewModel() {

    private val TAG = "CrossplayRallyVM"
    
    private val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    private val _uiState = MutableStateFlow(RallyUiStateData())
    val uiState: StateFlow<RallyUiStateData> = _uiState.asStateFlow()
    
    val favoriteTrees = treeRepository.getFavoriteTrees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentParticipantId: String? = null

    fun createRally(
        name: String,
        description: String,
        mode: RallyMode,
        districtFilter: List<Int>? = null,
        targetSpeciesCount: Int? = null,
        timeLimitMinutes: Int? = null,
        centerLat: Double? = null,
        centerLng: Double? = null,
        radiusMeters: Int? = null,
        targetTreeIds: List<String>? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.createRally(
                name = name,
                description = description,
                mode = mode,
                districtFilter = districtFilter,
                targetSpeciesCount = targetSpeciesCount,
                timeLimitMinutes = timeLimitMinutes,
                centerLat = centerLat,
                centerLng = centerLng,
                radiusMeters = radiusMeters,
                targetTreeIds = targetTreeIds
            )
            
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    Log.d(TAG, "Rally created with code: ${data.join_code}")
                    joinRally(data.join_code, "Gamemaster")
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to create rally", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Fehler beim Erstellen: ${result.message ?: result.exception.message}"
                    )
                }
                else -> {}
            }
        }
    }

    fun joinRally(code: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.joinRally(code, displayName)
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    Log.d(TAG, "Joined rally: ${data.rally_id}")
                    currentParticipantId = data.participant_id
                    loadRally(data.rally_id)
                    subscribeToRealtime(data.rally_id)
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to join rally", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Fehler beim Beitreten: ${result.message ?: result.exception.message}"
                    )
                }
                else -> {}
            }
        }
    }

    fun collectTree(
        treeId: String,
        species: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val rallyId = _uiState.value.rally?.id ?: return@launch
            val participantId = currentParticipantId ?: return@launch
            
            val result = repository.collectTree(
                rallyId = rallyId,
                participantId = participantId,
                treeId = treeId,
                species = species,
                latitude = latitude,
                longitude = longitude,
                photoUrl = photoUrl,
                notes = notes
            )
            
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    Log.d(TAG, "Tree collected: ${data.collection_id}, new: ${data.is_new_species}")
                    
                    if (data.is_new_species) {
                        showAchievement("Neue Baumart entdeckt!", species)
                    }
                    
                    loadCollections(rallyId)
                    loadStatistics(rallyId)
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to collect tree", result.exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Fehler beim Sammeln: ${result.message ?: result.exception.message}"
                    )
                }
                else -> {}
            }
        }
    }

    private fun loadRally(rallyId: String) {
        viewModelScope.launch {
            val result = repository.getRally(rallyId)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        rally = result.data,
                        isLoading = false
                    )
                    loadParticipants(rallyId)
                    loadCollections(rallyId)
                    loadStatistics(rallyId)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: result.exception.message
                    )
                }
                else -> {}
            }
        }
    }

    private fun loadParticipants(rallyId: String) {
        viewModelScope.launch {
            val result = repository.getRallyParticipants(rallyId)
            if (result is Result.Success) {
                _uiState.value = _uiState.value.copy(participants = result.data)
            } else if (result is Result.Error) {
                Log.e(TAG, "Failed to load participants", result.exception)
            }
        }
    }

    private fun loadCollections(rallyId: String) {
        viewModelScope.launch {
            val result = repository.getRallyCollections(rallyId)
            if (result is Result.Success) {
                _uiState.value = _uiState.value.copy(collections = result.data)
            } else if (result is Result.Error) {
                Log.e(TAG, "Failed to load collections", result.exception)
            }
        }
    }

    private fun loadStatistics(rallyId: String) {
        viewModelScope.launch {
            val result = repository.getRallyStatistics(rallyId)
            if (result is Result.Success) {
                _uiState.value = _uiState.value.copy(statistics = result.data)
            } else if (result is Result.Error) {
                Log.e(TAG, "Failed to load statistics", result.exception)
            }
        }
    }

    private fun subscribeToRealtime(rallyId: String) {
        viewModelScope.launch {
            realtimeManager.subscribeToRally(rallyId)
                .collect { event ->
                    handleRealtimeEvent(event)
                }
        }
    }

    private fun handleRealtimeEvent(event: RallyRealtimeEvent) {
        when (event) {
            is RallyRealtimeEvent.ParticipantJoined -> {
                Log.d(TAG, "Realtime: Participant joined - ${event.participant.display_name}")
                
                val updatedParticipants = _uiState.value.participants + event.participant
                _uiState.value = _uiState.value.copy(participants = updatedParticipants)
                
                showNotification("${event.participant.display_name} ist beigetreten (${event.participant.platform})")
            }
            
            is RallyRealtimeEvent.TreeCollected -> {
                Log.d(TAG, "Realtime: Tree collected - ${event.collection.species}")
                
                val updatedCollections = _uiState.value.collections + event.collection
                _uiState.value = _uiState.value.copy(collections = updatedCollections)
                
                _uiState.value.rally?.id?.let { loadStatistics(it) }
                
                showNotification("Baum gesammelt: ${event.collection.species}")
            }
            
            is RallyRealtimeEvent.ParticipantLeft -> {
                Log.d(TAG, "Realtime: Participant left - ${event.participantId}")
                
                val updatedParticipants = _uiState.value.participants.filter { it.id != event.participantId }
                _uiState.value = _uiState.value.copy(participants = updatedParticipants)
            }
            
            is RallyRealtimeEvent.RallyUpdated -> {
                Log.d(TAG, "Realtime: Rally updated - ${event.rally.status}")
                _uiState.value = _uiState.value.copy(rally = event.rally)
            }
            
            is RallyRealtimeEvent.RallyFinished -> {
                Log.d(TAG, "Realtime: Rally finished - ${event.rallyId}")
                showNotification("Rallye beendet!")
                
                _uiState.value.rally?.let { rally ->
                    _uiState.value = _uiState.value.copy(
                        rally = rally.copy(status = "finished")
                    )
                }
            }
        }
    }

    fun leaveRally() {
        viewModelScope.launch {
            currentParticipantId?.let { participantId ->
                val result = repository.leaveRally(participantId)
                if (result is Result.Success) {
                    Log.d(TAG, "Left rally")
                    realtimeManager.unsubscribe()
                    _uiState.value = RallyUiStateData()
                } else if (result is Result.Error) {
                    Log.e(TAG, "Failed to leave rally", result.exception)
                }
            }
        }
    }

    fun finishRally() {
        viewModelScope.launch {
            _uiState.value.rally?.id?.let { rallyId ->
                val result = repository.finishRally(rallyId)
                if (result is Result.Success) {
                    Log.d(TAG, "Rally finished")
                    showNotification("Rallye erfolgreich beendet!")
                } else if (result is Result.Error) {
                    Log.e(TAG, "Failed to finish rally", result.exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Nur der Ersteller kann die Rallye beenden"
                    )
                }
            }
        }
    }

    fun searchPublicRallies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = repository.searchPublicRallies()
            _uiState.value = _uiState.value.copy(isLoading = false)
            
            if (result is Result.Success) {
                Log.d(TAG, "Found ${result.data.size} public rallies")
            } else if (result is Result.Error) {
                Log.e(TAG, "Failed to search rallies", result.exception)
                _uiState.value = _uiState.value.copy(
                    error = result.message ?: result.exception.message
                )
            }
        }
    }

    fun updateActivity() {
        viewModelScope.launch {
            currentParticipantId?.let { participantId ->
                repository.updateParticipantActivity(participantId)
            }
        }
    }

    private fun showAchievement(title: String, message: String) {
        Log.d(TAG, "Achievement unlocked: $title - $message")
    }

    private fun showNotification(message: String) {
        Log.d(TAG, "Notification: $message")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeManager.unsubscribe()
        }
    }
}

package paulify.baeumeinwien.ui.screens.rally

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.domain.Achievement
import paulify.baeumeinwien.data.domain.Rally
import paulify.baeumeinwien.data.domain.RallyProgress
import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.data.repository.RallyRepository
import paulify.baeumeinwien.data.repository.TreeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.location.Location
import paulify.baeumeinwien.data.domain.RallyMode
import paulify.baeumeinwien.data.domain.TaskType
import org.maplibre.android.geometry.LatLng

enum class RallyNavigationMode {
    MAP, RADAR, AR, TASKS
}

data class RallyUiState(
    val currentRally: Rally? = null,
    val progress: RallyProgress? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val targetTrees: List<Tree> = emptyList(),
    val allProgress: List<RallyProgress> = emptyList(),
    val isAdmin: Boolean = false,
    val userLocation: LatLng? = null,
    val deviceBearing: Float = 0f,
    val activeTreeId: String? = null,
    val navigationMode: RallyNavigationMode = RallyNavigationMode.MAP,
    val selectedTargetTreeId: String? = null,
    val achievements: List<Achievement> = emptyList(),
    val newAchievement: Achievement? = null,
    val leafPhotos: Map<String, android.graphics.Bitmap> = emptyMap(),
    val nearbyTree: Tree? = null
)

class RallyViewModel(
    private val rallyRepository: RallyRepository,
    private val treeRepository: TreeRepository,
    private val crossplayRepository: paulify.baeumeinwien.data.repository.CrossplayRallyRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RallyUiState())
    val uiState: StateFlow<RallyUiState> = _uiState.asStateFlow()

    fun storeLeafPhoto(treeId: String, bitmap: android.graphics.Bitmap) {
        val currentPhotos = _uiState.value.leafPhotos.toMutableMap()
        currentPhotos[treeId] = bitmap
        _uiState.value = _uiState.value.copy(leafPhotos = currentPhotos)
        
        viewModelScope.launch {
            try {
                val context = paulify.baeumeinwien.BaeumeinwienApplication.instance.applicationContext
                val filename = "leaf_${treeId}_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(context.filesDir, filename)
                
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                android.util.Log.d("RallyViewModel", "Saved leaf photo to ${file.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("RallyViewModel", "Failed to save leaf photo", e)
            }
        }
    }

    fun createRally(name: String, treeIds: List<String>, radius: Double, creator: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            if (crossplayRepository != null) {
                try {
                    val result = crossplayRepository.createRally(
                        name = name,
                        description = "Erstellt von $creator",
                        mode = RallyMode.STUDENT,
                        targetTreeIds = treeIds,
                        radiusMeters = radius.toInt()
                    )
                    
                    when (result) {
                        is paulify.baeumeinwien.data.repository.Result.Success -> {
                            android.util.Log.d("RallyViewModel", "Rally via Supabase erstellt: ${result.data.join_code}")
                            val rally = Rally(
                                id = result.data.rally_id,
                                code = result.data.join_code,
                                name = name,
                                mode = RallyMode.STUDENT,
                                targetTreeIds = treeIds,
                                radiusMeters = radius,
                                creatorName = creator,
                                timestamp = System.currentTimeMillis(),
                                tasks = emptyList()
                            )
                            _uiState.value = _uiState.value.copy(currentRally = rally, isLoading = false, isAdmin = true)
                        }
                        is paulify.baeumeinwien.data.repository.Result.Error -> {
                            android.util.Log.e("RallyViewModel", "Supabase Fehler, fallback local: ${result.message}")
                            createRallyLocal(name, treeIds, radius, creator)
                        }
                        else -> {
                           android.util.Log.d("RallyViewModel", "Unexpected result state: $result")
                           createRallyLocal(name, treeIds, radius, creator)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RallyViewModel", "Exception, fallback local: ${e.message}")
                    createRallyLocal(name, treeIds, radius, creator)
                }
            } else {
                createRallyLocal(name, treeIds, radius, creator)
            }
        }
    }
    
    private suspend fun createRallyLocal(name: String, treeIds: List<String>, radius: Double, creator: String) {
        try {
            val tasks = treeIds.flatMap { treeId ->
                listOf(
                    paulify.baeumeinwien.data.domain.RallyTask(treeId, TaskType.LEAF_PHOTO, "Mache ein Foto eines Blattes"),
                    paulify.baeumeinwien.data.domain.RallyTask(treeId, TaskType.SPECIES_ID, "Welche Baumart ist das?")
                )
            }
            val rally = rallyRepository.createRally(name, treeIds, radius, creator, RallyMode.SCHOOL, tasks)
            _uiState.value = _uiState.value.copy(currentRally = rally, isLoading = false, isAdmin = true)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
        }
    }

    fun startSoloRally(trees: List<Tree>) {
         viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val treeIds = trees.map { it.id }
                val tasks = treeIds.map { treeId -> 
                    paulify.baeumeinwien.data.domain.RallyTask(treeId, TaskType.OBSERVATION, "Siehst du Nester oder Flechten?")
                }
                val rally = rallyRepository.createRally("Solo Entdeckung", treeIds, 100.0, "Ich", RallyMode.SOLO, tasks)
                val progress = RallyProgress(rally.id, "Entdecker", emptyList())
                _uiState.value = _uiState.value.copy(
                    currentRally = rally,
                    progress = progress,
                    targetTrees = trees,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun joinRally(context: android.content.Context, code: String, studentName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            if (crossplayRepository != null) {
                try {
                    val result = crossplayRepository.joinRally(code.uppercase(), studentName)
                    
                    when (result) {
                        is paulify.baeumeinwien.data.repository.Result.Success -> {
                            android.util.Log.d("RallyViewModel", "Via Supabase beigetreten: ${result.data.rally_id}")
                            
                            val rallyResult = crossplayRepository.getRally(result.data.rally_id)
                            if (rallyResult is paulify.baeumeinwien.data.repository.Result.Success) {
                                val crossplayRally = rallyResult.data
                                
                                val treeIds = crossplayRally.target_tree_ids ?: emptyList()
                                
                                val districtHint = crossplayRally.district_filter ?: 
                                    if (crossplayRally.center_lat != null && crossplayRally.center_lng != null) {
                                        val inferred = paulify.baeumeinwien.data.domain.DistrictUtils.getClosestDistrict(
                                            crossplayRally.center_lat, crossplayRally.center_lng
                                        )
                                        android.util.Log.d("RallyViewModel", "Inferred district from center: $inferred")
                                        listOf(inferred)
                                    } else null

                                android.util.Log.d("RallyViewModel", "Crossplay Rally hat ${treeIds.size} Bäume, Districts: $districtHint")
                                
                                var targets: List<Tree> = emptyList()
                                if (treeIds.isNotEmpty()) {
                                    try {
                                        targets = treeRepository.getTreesByIdsOnline(treeIds, districtHint)
                                        android.util.Log.d("RallyViewModel", "Bäume geladen: ${targets.size}")
                                    } catch (e: Exception) {
                                        android.util.Log.e("RallyViewModel", "Fehler beim Laden der Bäume: ${e.message}")
                                    }
                                }
                                
                                val rally = Rally(
                                    id = crossplayRally.id,
                                    code = crossplayRally.code,
                                    name = crossplayRally.name,
                                    mode = RallyMode.fromServerValue(crossplayRally.mode),
                                    targetTreeIds = treeIds,
                                    radiusMeters = crossplayRally.radius_meters?.toDouble() ?: 500.0,
                                    creatorName = crossplayRally.creator_platform,
                                    timestamp = System.currentTimeMillis(),
                                    tasks = emptyList()
                                )
                                
                                rallyRepository.saveSession(context, code, studentName)
                                _uiState.value = _uiState.value.copy(
                                    currentRally = rally,
                                    targetTrees = targets,
                                    progress = RallyProgress(rally.id, studentName, emptyList()),
                                    isLoading = false
                                )
                                return@launch
                            }
                        }
                        is paulify.baeumeinwien.data.repository.Result.Error -> {
                            android.util.Log.w("RallyViewModel", "Supabase join failed, trying local: ${result.message}")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RallyViewModel", "Supabase exception, trying local: ${e.message}")
                }
            }
            
            try {
                val rally = rallyRepository.getRallyByCode(code)
                if (rally != null) {
                    var targets = rally.targetTreeIds.mapNotNull { treeRepository.getTreeById(it) }
                    
                    if (targets.size < rally.targetTreeIds.size) {
                        android.util.Log.d("RallyViewModel", "Lade ${rally.targetTreeIds.size - targets.size} Bäume online...")
                        
                        val missingIds = rally.targetTreeIds.filter { id -> 
                            targets.none { it.id == id } 
                        }
                        
                        if (missingIds.isNotEmpty()) {
                            try {
                                val newTrees = treeRepository.getTreesByIdsOnline(missingIds)
                                targets = targets + newTrees
                            } catch (e: Exception) {
                                android.util.Log.e("RallyViewModel", "Fehler beim Batch-Laden: ${e.message}")
                            }
                        }
                    }
                    
                    rallyRepository.saveSession(context, code, studentName)
                    _uiState.value = _uiState.value.copy(
                        currentRally = rally, 
                        targetTrees = targets,
                        progress = RallyProgress(rally.id, studentName, emptyList()),
                        isLoading = false,
                        error = if (targets.isEmpty()) "Keine Bäume gefunden" else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "Code nicht gefunden", isLoading = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("RallyViewModel", "Fehler beim Beitreten: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun checkSession(context: android.content.Context) {
        val (code, name) = rallyRepository.getSession(context)
        if (code != null && name != null) {
            joinRally(context, code, name)
        }
    }

    fun updateLocation(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(userLocation = latLng)
        checkForNearbyTree(latLng)
    }

    private fun checkForNearbyTree(userLoc: LatLng) {
        val state = _uiState.value
        val foundIds = state.progress?.foundTreeIds ?: emptyList()
        val targetTrees = state.targetTrees
        
        val nearby = targetTrees.filter { !foundIds.contains(it.id) }
            .map { tree -> 
                val dist = FloatArray(1)
                Location.distanceBetween(userLoc.latitude, userLoc.longitude, tree.latitude, tree.longitude, dist)
                Pair(tree, dist[0])
            }
            .filter { it.second <= 50f }
            .minByOrNull { it.second }
            
        _uiState.value = _uiState.value.copy(nearbyTree = nearby?.first)
    }

        fun completeTask(treeId: String, answer: String, points: Int) {
        val state = _uiState.value
        val progress = state.progress ?: return
        val rally = state.currentRally ?: return
        val tree = state.targetTrees.find { it.id == treeId } ?: return

        val newAnswers = progress.taskAnswers + (treeId to answer)
        val newFound = if (!progress.foundTreeIds.contains(treeId)) {
            progress.foundTreeIds + treeId
        } else progress.foundTreeIds
        
        val newScore = progress.score + points
        val completed = newFound.distinct().size == rally.targetTreeIds.size

        viewModelScope.launch {
            rallyRepository.updateProgress(rally.id, progress.studentName, newFound, newAnswers, newScore, completed)
            _uiState.value = _uiState.value.copy(
                progress = progress.copy(
                    foundTreeIds = newFound,
                    taskAnswers = newAnswers,
                    score = newScore,
                    completed = completed
                )
            )

            if (rally.mode == RallyMode.SOLO) {
                val species = tree.speciesGerman
                if (!treeRepository.hasAchievement(species)) {
                    val imageUrl = getImageUrlForSpecies(species)
                    treeRepository.unlockAchievement(species, imageUrl)
                    val newAchievement = Achievement(species, imageUrl)
                    _uiState.value = _uiState.value.copy(newAchievement = newAchievement)
                }
            }
        }
    }

    fun calculateDistanceTo(tree: Tree): Float {
        val userLoc = _uiState.value.userLocation ?: return Float.MAX_VALUE
        val results = FloatArray(1)
        Location.distanceBetween(userLoc.latitude, userLoc.longitude, tree.latitude, tree.longitude, results)
        return results[0]
    }

    fun calculateBearingTo(tree: Tree): Float {
        val userLoc = _uiState.value.userLocation ?: return 0f
        val userLocation = Location("").apply {
            latitude = userLoc.latitude
            longitude = userLoc.longitude
        }
        val targetLocation = Location("").apply {
            latitude = tree.latitude
            longitude = tree.longitude
        }
        return userLocation.bearingTo(targetLocation)
    }

    fun updateNavigationMode(mode: RallyNavigationMode) {
        _uiState.value = _uiState.value.copy(navigationMode = mode)
    }

    fun updateDeviceBearing(bearing: Float) {
        _uiState.value = _uiState.value.copy(deviceBearing = bearing)
    }

    fun selectTargetTree(treeId: String?) {
        _uiState.value = _uiState.value.copy(selectedTargetTreeId = treeId)
    }

    fun refreshAllProgress() {
        val rallyId = _uiState.value.currentRally?.id ?: return
        viewModelScope.launch {
            val remote = rallyRepository.fetchRemoteProgress(rallyId)
            _uiState.value = _uiState.value.copy(allProgress = remote)
        }
    }

        fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearNewAchievement() {
        _uiState.value = _uiState.value.copy(newAchievement = null)
    }

    suspend fun searchTrees(query: String): List<Tree> {
        if (query.isBlank()) return emptyList()
        
        return try {
            val onlineResults = treeRepository.searchTrees(query)
            if (onlineResults.isNotEmpty()) {
                onlineResults
            } else {
                _uiState.value.targetTrees.filter { tree ->
                    tree.speciesGerman.contains(query, ignoreCase = true) ||
                    tree.speciesScientific?.contains(query, ignoreCase = true) == true ||
                    tree.id.contains(query, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            _uiState.value.targetTrees.filter { tree ->
                tree.speciesGerman.contains(query, ignoreCase = true) ||
                tree.speciesScientific?.contains(query, ignoreCase = true) == true ||
                tree.id.contains(query, ignoreCase = true)
            }
        }
    }

    fun markTreeFound(treeId: String) {
        val state = _uiState.value
        val progress = state.progress ?: return
        val rally = state.currentRally ?: return

        val newFoundIds = if (!progress.foundTreeIds.contains(treeId)) {
            progress.foundTreeIds + treeId
        } else {
            progress.foundTreeIds
        }

        val completed = newFoundIds.size == rally.targetTreeIds.size

        viewModelScope.launch {
            rallyRepository.updateProgress(
                rallyId = rally.id,
                studentName = progress.studentName,
                foundTreeIds = newFoundIds,
                taskAnswers = progress.taskAnswers,
                score = progress.score + 10,
                completed = completed
            )

            _uiState.value = _uiState.value.copy(
                progress = progress.copy(
                    foundTreeIds = newFoundIds,
                    score = progress.score + 10,
                    completed = completed
                )
            )
        }
    }

    private fun getImageUrlForSpecies(species: String): String {
        return when (species) {
            "Acer campestre" -> "https://upload.wikimedia.org/wikipedia/commons/a/a4/Acer_campestre_leaf.jpg"
            "Tilia cordata" -> "https://upload.wikimedia.org/wikipedia/commons/e/e1/Tilia_cordata_leaf.jpg"
            else -> "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Ei-leaf.svg/1200px-Ei-leaf.svg.png"
        }
    }

    fun loadTreesNearby(latitude: Double, longitude: Double, radiusMeters: Double) {
        viewModelScope.launch {
            try {
                val trees = treeRepository.getTreesNearby(latitude, longitude, radiusMeters)
                _uiState.value = _uiState.value.copy(targetTrees = trees)
            } catch (e: Exception) {
                android.util.Log.e("RallyViewModel", "Error loading nearby trees: ${e.message}")
            }
        }
    }

    suspend fun hasAchievement(species: String): Boolean {
        return treeRepository.hasAchievement(species)
    }

    fun unlockAchievement(species: String, imageUrl: String) {
        viewModelScope.launch {
            treeRepository.unlockAchievement(species, imageUrl)
            val achievement = Achievement(species, imageUrl)
            _uiState.value = _uiState.value.copy(newAchievement = achievement)
            
            loadAchievements()
        }
    }

    fun loadAchievements() {
        viewModelScope.launch {
            treeRepository.getAllAchievements().collect { achievements ->
                _uiState.value = _uiState.value.copy(achievements = achievements)
            }
        }
    }
    
    fun leaveRally(context: android.content.Context) {
        android.util.Log.d("RallyViewModel", "leaveRally called - clearing session synchronously")
        
        rallyRepository.clearSession(context)
        
        clearRally()
        
        android.util.Log.d("RallyViewModel", "leaveRally complete - session and state cleared")
    }
    
    fun clearRally() {
        _uiState.value = RallyUiState()
    }
    
    fun isTeacherMode(): Boolean {
        return _uiState.value.currentRally?.mode == RallyMode.STUDENT && _uiState.value.isAdmin
    }
}

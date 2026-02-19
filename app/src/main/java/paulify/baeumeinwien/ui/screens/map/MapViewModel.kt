package paulify.baeumeinwien.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.domain.CommunityTree
import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.data.local.DistrictCluster
import paulify.baeumeinwien.data.domain.AuthState
import paulify.baeumeinwien.data.repository.AuthRepository
import paulify.baeumeinwien.data.repository.CommunityTreeRepository
import paulify.baeumeinwien.data.repository.DownloadProgress
import paulify.baeumeinwien.data.repository.Result
import paulify.baeumeinwien.data.repository.TreeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLngBounds

import org.maplibre.android.geometry.LatLng

data class MapUiState(
    val trees: List<Tree> = emptyList(),
    val districtClusters: List<DistrictCluster> = emptyList(),
    val selectedTree: Tree? = null,
    val highlightedTree: Tree? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val showDownloadDialog: Boolean = false,
    val currentZoom: Double = 11.5,
    val currentBounds: LatLngBounds? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isSearchActive: Boolean = false,
    val cameraPosition: LatLng? = null,
    val savedCameraPosition: LatLng? = null,
    val savedZoom: Double = 14.0,
    val searchResults: List<Tree> = emptyList(),
    val showFavoritesOnly: Boolean = false,
    val show3DBuildings: Boolean = true,
    val updateAvailable: paulify.baeumeinwien.data.remote.dto.DataVersion? = null,
    val currentVersion: String? = null,
    val communityTrees: List<CommunityTree> = emptyList(),
    val selectedCommunityTree: CommunityTree? = null
)

class MapViewModel(
    private val repository: TreeRepository,
    private val achievementManager: paulify.baeumeinwien.data.repository.AchievementManager,
    private val communityTreeRepository: CommunityTreeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedTree = MutableStateFlow<Tree?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _visibleTrees = MutableStateFlow<List<Tree>>(emptyList())
    private val _districtClusters = MutableStateFlow<List<DistrictCluster>>(emptyList())
    private val _showDownloadDialog = MutableStateFlow(false)
    private val _currentZoom = MutableStateFlow(11.5)
    private val _currentBounds = MutableStateFlow<LatLngBounds?>(null)

    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _isSearchActive = MutableStateFlow(false)
    private val _cameraPosition = MutableStateFlow<LatLng?>(null)
    private val _savedCameraPosition = MutableStateFlow<LatLng?>(LatLng(48.2082, 16.3738))
    private val _savedZoom = MutableStateFlow(14.0)
    private val _searchResults = MutableStateFlow<List<Tree>>(emptyList())
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _show3DBuildings = MutableStateFlow(true)
    private val _updateAvailable = MutableStateFlow<paulify.baeumeinwien.data.remote.dto.DataVersion?>(null)
    private val _currentVersion = MutableStateFlow<String?>(null)
    private val _communityTrees = MutableStateFlow<List<CommunityTree>>(emptyList())
    private val _selectedCommunityTree = MutableStateFlow<CommunityTree?>(null)
    
    private val _highlightedTree = MutableStateFlow<Tree?>(null)
    val highlightedTree: StateFlow<Tree?> = _highlightedTree.asStateFlow()


    @OptIn(ExperimentalStdlibApi::class)
    val uiState: StateFlow<MapUiState> = combine(
        _visibleTrees,
        _districtClusters,
        _selectedTree,
        _isLoading,
        _errorMessage,
        _isRefreshing,
        _currentZoom,
        _currentBounds,
        _showDownloadDialog,
        repository.downloadProgress,
        _searchQuery,
        _isSearching,
        _isSearchActive,
        _cameraPosition,
        _savedCameraPosition,
        _savedZoom,
        _searchResults,
        _showFavoritesOnly,
        _show3DBuildings,
        _updateAvailable,
        _currentVersion,
        _communityTrees,
        _selectedCommunityTree
    ) { latestValues ->
        @Suppress("UNCHECKED_CAST")
        val rawTrees = latestValues[0] as List<Tree>
        val showFavoritesOnly = latestValues[17] as Boolean
        val show3DBuildings = latestValues[18] as Boolean
        val updateAvailable = latestValues[19] as paulify.baeumeinwien.data.remote.dto.DataVersion?
        val currentVersion = latestValues[20] as String?
        
        val filteredTrees = if (showFavoritesOnly) {
            rawTrees.filter { it.isFavorite }
        } else {
            rawTrees
        }
        
        MapUiState(
            trees = filteredTrees,
            districtClusters = latestValues[1] as List<DistrictCluster>,
            selectedTree = latestValues[2] as Tree?,
            isLoading = latestValues[3] as Boolean,
            errorMessage = latestValues[4] as String?,
            isRefreshing = latestValues[5] as Boolean,
            currentZoom = latestValues[6] as Double,
            currentBounds = latestValues[7] as LatLngBounds?,
            showDownloadDialog = latestValues[8] as Boolean,
            downloadProgress = latestValues[9] as DownloadProgress,
            searchQuery = latestValues[10] as String,
            isSearching = latestValues[11] as Boolean,
            isSearchActive = latestValues[12] as Boolean,
            cameraPosition = latestValues[13] as LatLng?,
            savedCameraPosition = latestValues[14] as LatLng?,
            savedZoom = latestValues[15] as Double,
            searchResults = latestValues[16] as List<Tree>,
            showFavoritesOnly = latestValues[17] as Boolean,
            show3DBuildings = latestValues[18] as Boolean,
            updateAvailable = latestValues[19] as paulify.baeumeinwien.data.remote.dto.DataVersion?,
            currentVersion = latestValues[20] as String?,
            communityTrees = latestValues[21] as List<CommunityTree>,
            selectedCommunityTree = latestValues[22] as CommunityTree?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        viewModelScope.launch {
            val treeCount = repository.getTreeCount()
            _currentVersion.value = repository.getCurrentVersion()
            
            if (treeCount == 0) {
                _isOnlineMode = true
                android.util.Log.d("MapViewModel", "Online-Modus aktiviert - keine Offline-Daten")
            } else {
                _updateAvailable.value = repository.checkForUpdates()
            }
        }
    }

    private var _isOnlineMode = false

    fun onCameraIdle(bounds: LatLngBounds, zoom: Double) {
        _currentZoom.value = zoom
        _currentBounds.value = bounds
        
        val center = LatLng(
            (bounds.latitudeSouth + bounds.latitudeNorth) / 2,
            (bounds.longitudeWest + bounds.longitudeEast) / 2
        )
        _savedCameraPosition.value = center
        _savedZoom.value = zoom

        android.util.Log.d("MapViewModel", "Camera idle at zoom: $zoom, Bounds: (S:${bounds.latitudeSouth}, N:${bounds.latitudeNorth}, W:${bounds.longitudeWest}, E:${bounds.longitudeEast})")

        viewModelScope.launch {
            if (zoom < 11) {
                _visibleTrees.value = emptyList()
                if (_districtClusters.value.isEmpty()) { 
                    _districtClusters.value = repository.getDistrictClusters()
                }
            } else {
                _districtClusters.value = emptyList()
                _isLoading.value = true
                
                val result = repository.loadTreesForViewport(
                    minLat = bounds.latitudeSouth,
                    maxLat = bounds.latitudeNorth,
                    minLon = bounds.longitudeWest,
                    maxLon = bounds.longitudeEast,
                    forceRefresh = false
                )
                
                when (result) {
                    is Result.Success -> {
                        val trees = result.data
                        val visibleInViewport = trees.filter { tree ->
                            tree.latitude in bounds.latitudeSouth..bounds.latitudeNorth &&
                            tree.longitude in bounds.longitudeWest..bounds.longitudeEast
                        }.take(1000)
                        
                        _visibleTrees.value = visibleInViewport
                        android.util.Log.d("MapViewModel", "Loaded ${trees.size} trees, showing ${visibleInViewport.size} in viewport")
                    }
                    is Result.Error -> {
                        android.util.Log.e("MapViewModel", "Error loading trees: ${result.message}")
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }
                
                _isLoading.value = false
            }

            loadCommunityTreesForBounds(bounds)
        }
    }
    
    fun refreshViewport(bounds: LatLngBounds) {
        viewModelScope.launch {
            _isRefreshing.value = true
            android.util.Log.d("MapViewModel", "Force refreshing viewport...")
            
            val result = repository.loadTreesForViewport(
                minLat = bounds.latitudeSouth,
                maxLat = bounds.latitudeNorth,
                minLon = bounds.longitudeWest,
                maxLon = bounds.longitudeEast,
                forceRefresh = true
            )
            
            when (result) {
                is Result.Success -> {
                    val trees = result.data
                    val visibleInViewport = trees.filter { tree ->
                        tree.latitude in bounds.latitudeSouth..bounds.latitudeNorth &&
                        tree.longitude in bounds.longitudeWest..bounds.longitudeEast
                    }.take(1000)
                    
                    _visibleTrees.value = visibleInViewport
                    android.util.Log.d("MapViewModel", "Refresh complete: ${visibleInViewport.size} trees")
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                }
                else -> {}
            }
            
            _isRefreshing.value = false
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        android.util.Log.d("MapViewModel", "Search query changed: '$query'")
        
        if (query.length >= 2) {
            viewModelScope.launch {
                _isSearching.value = true
                _isSearchActive.value = true
                
                android.util.Log.d("MapViewModel", "Executing search for: '$query'")
                
                val localResults = repository.searchTrees(query)
                android.util.Log.d("MapViewModel", "Local search found ${localResults.size} results")
                _searchResults.value = localResults
                
                if (_isOnlineMode && localResults.size < 5 && query.length >= 3) {
                    android.util.Log.d("MapViewModel", "Triggering online search...")
                    val onlineResults = repository.searchTreesSupabase(query, limit = 50)
                    android.util.Log.d("MapViewModel", "Online search found ${onlineResults.size} results")
                    val combined = (localResults + onlineResults).distinctBy { it.id }.take(50)
                    _searchResults.value = combined
                }
                
                _isSearching.value = false
                android.util.Log.d("MapViewModel", "Search completed. Total results: ${_searchResults.value.size}")
            }
        } else {
            android.util.Log.d("MapViewModel", "Query too short, clearing results")
            _searchResults.value = emptyList()
            _isSearchActive.value = false
        }
    }

    fun executeSearch() {
        val query = _searchQuery.value
        if (query.isBlank() || query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _isSearchActive.value = true
            
            val localResults = repository.searchTrees(query)
            _searchResults.value = localResults
            
            if (localResults.size < 10 || _isOnlineMode) {
                val onlineResults = repository.searchTreesSupabase(query, limit = 50)
                val combined = (localResults + onlineResults).distinctBy { it.id }.take(50)
                _searchResults.value = combined
            }
            
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        android.util.Log.d("MapViewModel", "Clearing search")
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearchActive.value = false
        _isSearching.value = false
    }
    
    fun onSearchResultClick(tree: Tree) {
        _cameraPosition.value = LatLng(tree.latitude, tree.longitude)
        _isSearchActive.value = false
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
    
    fun onMapMoved() {
        _cameraPosition.value = null
    }

    fun confirmDownload() {
        _showDownloadDialog.value = false
        loadAllTrees()
    }

    fun dismissDownloadDialog() {
        _showDownloadDialog.value = false
        _isLoading.value = false
        _isOnlineMode = true
    }
    
    fun selectTreeAndZoom(tree: Tree) {
        _selectedTree.value = tree
        _cameraPosition.value = LatLng(tree.latitude, tree.longitude)
    }
    
    fun highlightTree(tree: Tree) {
        _highlightedTree.value = tree
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000L)
            if (_highlightedTree.value?.id == tree.id) {
                _highlightedTree.value = null
            }
        }
    }
    
    fun clearHighlight() {
        _highlightedTree.value = null
    }

    private fun loadCommunityTreesForBounds(bounds: LatLngBounds) {
        viewModelScope.launch {
            try {
                val trees = communityTreeRepository.getCommunityTreesInBounds(
                    minLat = bounds.latitudeSouth,
                    maxLat = bounds.latitudeNorth,
                    minLon = bounds.longitudeWest,
                    maxLon = bounds.longitudeEast
                )
                _communityTrees.value = trees
                android.util.Log.d("MapViewModel", "Loaded ${trees.size} community trees")
            } catch (e: Exception) {
                android.util.Log.e("MapViewModel", "Failed to load community trees", e)
            }
        }
    }

    fun selectCommunityTree(tree: CommunityTree) {
        _selectedCommunityTree.value = tree
    }

    fun dismissUpdateDialog() {
        _updateAvailable.value = null
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggle3DBuildings() {
        _show3DBuildings.value = !_show3DBuildings.value
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateAvailable.value = repository.checkForUpdates()
            if (_updateAvailable.value == null) {
                _errorMessage.value = "Daten sind auf dem neuesten Stand"
            }
        }
    }

    fun clearCacheAndReload() {
        viewModelScope.launch {
            repository.clearCache()
            _visibleTrees.value = emptyList()
            _showDownloadDialog.value = true
        }
    }
    
    fun loadAllTreesFirstTime() {
        android.util.Log.d("MapViewModel", "Starting first-time download of ALL trees")
        loadAllTrees()
    }
    
    fun enableOnlineMode() {
        android.util.Log.d("MapViewModel", "Enabling online mode")
        _isOnlineMode = true
        _isLoading.value = false
    }

    private fun loadAllTrees() {
        viewModelScope.launch {
            _isLoading.value = true
            val viennaBbox = "16.18,48.11,16.58,48.33,EPSG:4326"

            when (val result = repository.downloadAllTrees(viennaBbox)) {
                is Result.Success -> {
                    _isLoading.value = false
                    _currentVersion.value = repository.getCurrentVersion()
                }
                is Result.Error -> {
                    if (result.exception?.message == "OOM_ERROR") {
                        android.util.Log.w("MapViewModel", "OOM during download, switching to Online Mode")
                        _errorMessage.value = "Gerätespeicher voll - Online Modus aktiviert"
                        enableOnlineMode()
                    } else {
                        _errorMessage.value = result.message ?: "Fehler beim Laden"
                    }
                    _isLoading.value = false
                }
                is Result.Loading -> {}
            }
        }
    }

    fun refreshTrees() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.clearCache()
            _visibleTrees.value = emptyList()

            val viennaBbox = "16.18,48.11,16.58,48.33,EPSG:4326"
            when (val result = repository.downloadAllTrees(viennaBbox)) {
                is Result.Success -> _errorMessage.value = null
                is Result.Error -> _errorMessage.value = result.message
                is Result.Loading -> {}
            }
            _isRefreshing.value = false
        }
    }

    private val _selectedTreeNotes = MutableStateFlow<List<paulify.baeumeinwien.data.local.TreeNote>>(emptyList())
    
    val selectedTreeNotes: StateFlow<List<paulify.baeumeinwien.data.local.TreeNote>> = _selectedTreeNotes.asStateFlow()

    fun selectTree(tree: Tree) {
        _selectedTree.value = tree
        loadNotesForTree(tree.id)
        
        viewModelScope.launch {
            val species = tree.speciesGerman
            val district = tree.district
            
            val unlockedAchievements = achievementManager.onTreeDiscovered(species, district)
            
            val milestoneAchievements = listOf(
                "first_tree",
                "species_10",
                "species_25",
                "species_50",
                "linde_lover",
                "kastanie_king",
                "ahorn_ace",
                "eiche_expert",
                "district_all",
                "first_favorite",
                "favorites_10"
            )
            
            val milestoneUnlocked = unlockedAchievements.filter { it.id in milestoneAchievements }
            
            if (milestoneUnlocked.isNotEmpty()) {
                val achievement = milestoneUnlocked.first()
                android.util.Log.d("MapViewModel", "Meilenstein-Achievement freigeschaltet: ${achievement.title}")
                _newGameAchievement.value = achievement
            }
        }
    }
    
    private val _newGameAchievement = MutableStateFlow<paulify.baeumeinwien.data.domain.GameAchievement?>(null)
    val newGameAchievement: StateFlow<paulify.baeumeinwien.data.domain.GameAchievement?> = _newGameAchievement.asStateFlow()
    
    private val _newAchievement = MutableStateFlow<paulify.baeumeinwien.data.domain.Achievement?>(null)
    val newAchievement: StateFlow<paulify.baeumeinwien.data.domain.Achievement?> = _newAchievement.asStateFlow()
    
    fun clearNewAchievement() {
        _newAchievement.value = null
        _newGameAchievement.value = null
    }

    fun clearSelection() {
        _selectedTree.value = null
        _selectedCommunityTree.value = null
    }

    fun toggleFavorite(treeId: String) {
        viewModelScope.launch {
            val wasAlreadyFavorite = _selectedTree.value?.isFavorite == true
            
            repository.toggleFavorite(treeId)
            
            _selectedTree.value?.let { tree ->
                if (tree.id == treeId) {
                    _selectedTree.value = tree.copy(isFavorite = !tree.isFavorite)
                }
            }
            
            val updatedTrees = _visibleTrees.value.map { tree ->
                if (tree.id == treeId) {
                    tree.copy(isFavorite = !tree.isFavorite)
                } else {
                    tree
                }
            }
            _visibleTrees.value = updatedTrees
            
            if (!wasAlreadyFavorite) {
                val unlockedAchievements = achievementManager.onFavoriteAdded()
                if (unlockedAchievements.isNotEmpty()) {
                    val achievement = unlockedAchievements.first()
                    android.util.Log.d("MapViewModel", "Favoriten-Achievement freigeschaltet: ${achievement.title}")
                    _newGameAchievement.value = achievement
                }
            }
            
            android.util.Log.d("MapViewModel", "Toggled favorite for tree $treeId")
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    private fun loadNotesForTree(treeId: String) {
        viewModelScope.launch {
            repository.getNotesForTree(treeId).collect { notes ->
                _selectedTreeNotes.value = notes
            }
        }
        loadPhotosForTree(treeId)
    }

    private val _selectedTreePhotos = MutableStateFlow<List<paulify.baeumeinwien.data.local.TreePhoto>>(emptyList())
    val selectedTreePhotos: StateFlow<List<paulify.baeumeinwien.data.local.TreePhoto>> = _selectedTreePhotos.asStateFlow()

    private fun loadPhotosForTree(treeId: String) {
        viewModelScope.launch {
            repository.getPhotosForTree(treeId).collect { photos ->
                _selectedTreePhotos.value = photos
            }
        }
    }

    fun addNote(treeId: String, content: String) {
        viewModelScope.launch {
            repository.addNote(treeId, content)
        }
    }

    fun addPhoto(treeId: String, uri: String) {
        viewModelScope.launch {
            repository.addPhoto(treeId, uri)
        }
    }

    fun deleteNote(note: paulify.baeumeinwien.data.local.TreeNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deletePhoto(photo: paulify.baeumeinwien.data.local.TreePhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }


    val authState: StateFlow<AuthState> = authRepository.authState

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()

    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        val profile = communityTreeRepository.getUserProfile(userId)
                        _currentUserRole.value = profile?.role
                    }
                } else {
                    _currentUserRole.value = null
                }
            }
        }
    }

    init {
        loadCurrentUserRole()
    }

    fun reportCommunityTree(treeId: String, reason: String, comment: String?) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            when (val result = communityTreeRepository.reportTree(treeId, userId, reason, comment)) {
                is Result.Success -> {
                    _errorMessage.value = "Meldung gesendet"
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Meldung fehlgeschlagen"
                }
                else -> {}
            }
        }
    }

    fun confirmCommunityTree(treeId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            when (val result = communityTreeRepository.confirmTree(treeId, userId)) {
                is Result.Success -> {
                    _selectedCommunityTree.value?.let { tree ->
                        if (tree.id == treeId) {
                            _selectedCommunityTree.value = tree.copy(
                                confirmationCount = tree.confirmationCount + 1
                            )
                        }
                    }
                    _errorMessage.value = "Baum bestätigt"
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Bestätigung fehlgeschlagen"
                }
                else -> {}
            }
        }
    }

    fun deleteCommunityTree(treeId: String) {
        viewModelScope.launch {
            when (val result = communityTreeRepository.deleteCommunityTree(treeId)) {
                is Result.Success -> {
                    _communityTrees.value = _communityTrees.value.filter { it.id != treeId }
                    _selectedCommunityTree.value = null
                    _errorMessage.value = "Baum gelöscht"
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Löschen fehlgeschlagen"
                }
                else -> {}
            }
        }
    }

    fun adminVerifyCommunityTree(treeId: String) {
        viewModelScope.launch {
            when (val result = communityTreeRepository.adminVerifyTree(treeId)) {
                is Result.Success -> {
                    _selectedCommunityTree.value?.let { tree ->
                        if (tree.id == treeId) {
                            _selectedCommunityTree.value = tree.copy(status = "verified")
                        }
                    }
                    _communityTrees.value = _communityTrees.value.map { tree ->
                        if (tree.id == treeId) tree.copy(status = "verified") else tree
                    }
                    _errorMessage.value = "Baum offiziell verifiziert"
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Verifizierung fehlgeschlagen"
                }
                else -> {}
            }
        }
    }

    fun updateShowNamePreference(show: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            communityTreeRepository.updateShowNamePreference(userId, show)
        }
    }
}

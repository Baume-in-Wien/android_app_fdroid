package paulify.baeumeinwien.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.domain.GameAchievement
import paulify.baeumeinwien.data.repository.AchievementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AchievementsUiState(
    val achievements: List<GameAchievement> = emptyList(),
    val unlockedAchievementIds: Set<String> = emptySet(),
    val uniqueSpeciesCount: Int = 0,
    val isLoading: Boolean = true
)

class AchievementsViewModel(
    private val achievementManager: AchievementManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()
    
    init {
        loadAchievements()
    }
    
    fun loadAchievements() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val unlockedIds = achievementManager.getUnlockedAchievementIds()
            val uniqueSpeciesCount = achievementManager.getUniqueSpeciesCount()
            val allAchievements = achievementManager.getAllAchievementsWithStatus()
            
            _uiState.value = AchievementsUiState(
                achievements = allAchievements,
                unlockedAchievementIds = unlockedIds,
                uniqueSpeciesCount = uniqueSpeciesCount,
                isLoading = false
            )
        }
    }
    
    fun refresh() {
        loadAchievements()
    }
}

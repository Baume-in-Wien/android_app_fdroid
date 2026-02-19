package paulify.baeumeinwien.ui

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import paulify.baeumeinwien.data.realtime.RallyRealtimeManager
import paulify.baeumeinwien.data.repository.CrossplayRallyRepository
import paulify.baeumeinwien.data.repository.TreeRepository
import paulify.baeumeinwien.data.repository.AuthRepository
import paulify.baeumeinwien.data.repository.CommunityTreeRepository
import paulify.baeumeinwien.ui.screens.ar.ArViewModel
import paulify.baeumeinwien.ui.screens.auth.AuthViewModel
import paulify.baeumeinwien.ui.screens.community.AddTreeViewModel
import paulify.baeumeinwien.ui.screens.favorites.FavoritesViewModel
import paulify.baeumeinwien.ui.screens.info.InfoViewModel
import paulify.baeumeinwien.ui.screens.map.MapViewModel
import paulify.baeumeinwien.ui.screens.rally.CrossplayRallyViewModel

class ViewModelFactory(
    private val context: Context,
    private val repository: TreeRepository,
    private val rallyRepository: paulify.baeumeinwien.data.repository.RallyRepository,
    private val achievementManager: paulify.baeumeinwien.data.repository.AchievementManager,
    private val authRepository: AuthRepository,
    private val communityTreeRepository: CommunityTreeRepository
) : ViewModelProvider.Factory {
    
    private val androidId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    
    private val crossplayRallyRepository: CrossplayRallyRepository by lazy {
        CrossplayRallyRepository(androidId)
    }
    
    private val rallyRealtimeManager: RallyRealtimeManager by lazy {
        RallyRealtimeManager()
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                MapViewModel(repository, achievementManager, communityTreeRepository, authRepository) as T
            }
            modelClass.isAssignableFrom(ArViewModel::class.java) -> {
                ArViewModel(repository) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(InfoViewModel::class.java) -> {
                InfoViewModel(repository) as T
            }
            modelClass.isAssignableFrom(paulify.baeumeinwien.ui.screens.rally.RallyViewModel::class.java) -> {
                paulify.baeumeinwien.ui.screens.rally.RallyViewModel(rallyRepository, repository, crossplayRallyRepository) as T
            }
            modelClass.isAssignableFrom(CrossplayRallyViewModel::class.java) -> {
                CrossplayRallyViewModel(context, crossplayRallyRepository, rallyRealtimeManager, repository) as T
            }
            modelClass.isAssignableFrom(paulify.baeumeinwien.ui.screens.achievements.AchievementsViewModel::class.java) -> {
                paulify.baeumeinwien.ui.screens.achievements.AchievementsViewModel(achievementManager) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(AddTreeViewModel::class.java) -> {
                AddTreeViewModel(communityTreeRepository, authRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

package paulify.baeumeinwien.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.data.repository.TreeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteTrees: List<Tree> = emptyList(),
    val isEmpty: Boolean = true
)

class FavoritesViewModel(
    private val repository: TreeRepository
) : ViewModel() {
    
    val uiState: StateFlow<FavoritesUiState> = repository.getFavoriteTrees()
        .map { trees ->
            FavoritesUiState(
                favoriteTrees = trees,
                isEmpty = trees.isEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FavoritesUiState()
        )
    
    fun removeFavorite(treeId: String) {
        viewModelScope.launch {
            repository.setFavorite(treeId, false)
        }
    }
}

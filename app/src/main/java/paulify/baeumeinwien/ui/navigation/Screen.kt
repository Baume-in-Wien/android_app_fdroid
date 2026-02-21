package paulify.baeumeinwien.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Eco
import androidx.compose.ui.graphics.vector.ImageVector
import paulify.baeumeinwien.R

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    
    data object Map : Screen(
        route = "map",
        titleResId = R.string.nav_map,
        icon = Icons.Default.Map
    )
    
    data object Explorer : Screen(
        route = "solo_explorer",
        titleResId = R.string.nav_explorer,
        icon = Icons.Default.DirectionsWalk
    )
    
    data object AR : Screen(
        route = "ar",
        titleResId = R.string.nav_ar,
        icon = Icons.Default.CameraAlt
    )
    
    data object Rally : Screen(
        route = "rally",
        titleResId = R.string.rally_title,
        icon = Icons.Default.Flag
    )
    
    data object More : Screen(
        route = "more",
        titleResId = R.string.nav_more,
        icon = Icons.Default.MoreHoriz
    )
    
    
    data object Favorites : Screen(
        route = "favorites",
        titleResId = R.string.favorites_title,
        icon = Icons.Default.Favorite
    )
    
    data object Achievements : Screen(
        route = "achievements_tab",
        titleResId = R.string.achievements_title,
        icon = Icons.Default.EmojiEvents
    )
    
    data object Statistics : Screen(
        route = "statistics",
        titleResId = R.string.settings_version,
        icon = Icons.Default.BarChart
    )
    
    data object Info : Screen(
        route = "info",
        titleResId = R.string.nav_info,
        icon = Icons.Default.Info
    )


    data object Login : Screen(
        route = "login",
        titleResId = R.string.nav_info,
        icon = Icons.Default.Person
    )

    data object AddTree : Screen(
        route = "add_tree",
        titleResId = R.string.nav_map,
        icon = Icons.Default.AddLocationAlt
    )

    data object LeafScanner : Screen(
        route = "leaf_scanner",
        titleResId = R.string.nav_leaf_scanner,
        icon = Icons.Default.Eco
    )
}

val bottomNavItems = listOf(
    Screen.Map,
    Screen.Explorer,
    Screen.AR,
    Screen.Rally,
    Screen.More
)

val moreMenuItems = listOf(
    Screen.LeafScanner,
    Screen.Favorites,
    Screen.Achievements,
    Screen.Statistics,
    Screen.Info
)

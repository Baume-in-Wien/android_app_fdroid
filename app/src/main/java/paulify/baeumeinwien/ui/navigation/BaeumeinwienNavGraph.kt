package paulify.baeumeinwien.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import paulify.baeumeinwien.ui.ViewModelFactory
import paulify.baeumeinwien.ui.screens.ar.ArScreen
import paulify.baeumeinwien.ui.screens.ar.ArViewModel
import paulify.baeumeinwien.ui.screens.favorites.FavoritesScreen
import paulify.baeumeinwien.ui.screens.favorites.FavoritesViewModel
import paulify.baeumeinwien.ui.screens.info.InfoScreen
import paulify.baeumeinwien.ui.screens.info.InfoViewModel
import paulify.baeumeinwien.ui.screens.map.MapScreen
import paulify.baeumeinwien.ui.screens.map.MapViewModel
import paulify.baeumeinwien.ui.screens.map.MapUiState
import paulify.baeumeinwien.ui.screens.auth.AuthViewModel
import paulify.baeumeinwien.ui.screens.auth.LoginScreen
import paulify.baeumeinwien.ui.screens.community.AddTreeScreen
import paulify.baeumeinwien.ui.screens.community.AddTreeViewModel
import paulify.baeumeinwien.data.domain.AuthState
import paulify.baeumeinwien.ui.screens.settings.SettingsScreen
import paulify.baeumeinwien.ui.screens.statistics.StatisticsScreen
import paulify.baeumeinwien.ui.screens.more.MoreScreen
import paulify.baeumeinwien.ui.screens.leafscanner.LeafScannerScreen
import paulify.baeumeinwien.ui.screens.leafscanner.LeafScannerViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun BaeumeinwienNavGraph(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            val mapViewModel: MapViewModel = viewModel(factory = viewModelFactory)
            MapScreen(
                viewModel = mapViewModel,
                onAddTree = {
                    navController.navigate(Screen.AddTree.route)
                }
            )
        }
        
        composable(Screen.AR.route) {
            val viewModel: ArViewModel = viewModel(factory = viewModelFactory)
            ArScreen(viewModel = viewModel)
        }
        
        composable(Screen.Favorites.route) {
            val viewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)
            val mapEntry = navController.getBackStackEntry(Screen.Map.route)
            val mapViewModel: MapViewModel = viewModel(mapEntry, factory = viewModelFactory)
            
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateToTree = { tree ->
                    mapViewModel.selectTreeAndZoom(tree)
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(Screen.Info.route) {
            val viewModel: InfoViewModel = viewModel(factory = viewModelFactory)
            InfoScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }
        
        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }
        
        composable(Screen.More.route) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            val mapEntry = navController.getBackStackEntry(Screen.Map.route)
            val mapViewModel: MapViewModel = viewModel(mapEntry, factory = viewModelFactory)
            MoreScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                },
                authViewModel = authViewModel,
                onLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onToggleShowName = { show ->
                    mapViewModel.updateShowNamePreference(show)
                }
            )
        }
        
        composable(Screen.Explorer.route) {
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            paulify.baeumeinwien.ui.screens.rally.SoloExplorerScreen(
                viewModel = rallyViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements.route)
                }
            )
        }
        
        composable(Screen.Achievements.route) {
            val achievementsViewModel: paulify.baeumeinwien.ui.screens.achievements.AchievementsViewModel = viewModel(factory = viewModelFactory)
            val uiState by achievementsViewModel.uiState.collectAsState()
            
            paulify.baeumeinwien.ui.screens.achievements.AchievementsGalleryScreen(
                unlockedAchievementIds = uiState.unlockedAchievementIds,
                uniqueSpeciesCount = uiState.uniqueSpeciesCount
            )
        }
        
        composable(Screen.Rally.route) {
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            paulify.baeumeinwien.ui.screens.rally.RallyScreen(
                viewModel = rallyViewModel,
                onNavigateToPlay = { code, name ->
                    navController.navigate("rally_play/$code/$name")
                },
                onNavigateToCreate = {
                    navController.navigate("rally_create")
                },
                onNavigateToSoloExplorer = {
                    navController.navigate("solo_explorer")
                }
            )
        }
        
        composable("rally_create") {
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            val favViewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)
            val favoriteUiState by favViewModel.uiState.collectAsState()
            
            paulify.baeumeinwien.ui.screens.rally.RallyCreationScreen(
                viewModel = rallyViewModel,
                favoriteTrees = favoriteUiState.favoriteTrees,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("rally_play/{code}/{name}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            
            val mapEntry = navController.getBackStackEntry(Screen.Map.route)
            val mapViewModel: MapViewModel = viewModel(mapEntry, factory = viewModelFactory)
            
            paulify.baeumeinwien.ui.screens.rally.RallyPlayScreen(
                viewModel = rallyViewModel,
                code = code,
                studentName = name,
                onBack = { navController.popBackStack() },
                onLeaveRally = { 
                    navController.popBackStack(Screen.Map.route, inclusive = false)
                },
                onNavigateToTree = { /* TODO */ },
                onViewCertificate = {
                    navController.navigate("rally_certificate")
                },
                onNavigateToTeacherDashboard = {
                    navController.navigate("teacher_dashboard/$code")
                }
            )
        }
        
        composable("teacher_dashboard/{code}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: ""
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            val uiState by rallyViewModel.uiState.collectAsState()
            
            paulify.baeumeinwien.ui.screens.rally.TeacherDashboardScreen(
                rallyName = uiState.currentRally?.name ?: "Rally",
                rallyCode = code,
                participants = emptyList(),
                collections = emptyList(),
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("solo_explorer") {
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            paulify.baeumeinwien.ui.screens.rally.SoloExplorerScreen(
                viewModel = rallyViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAchievements = {
                    navController.navigate("achievements")
                }
            )
        }
        
        composable("achievements") {
            val achievementsViewModel: paulify.baeumeinwien.ui.screens.achievements.AchievementsViewModel = viewModel(factory = viewModelFactory)
            val uiState by achievementsViewModel.uiState.collectAsState()
            
            paulify.baeumeinwien.ui.screens.achievements.AchievementsGalleryScreen(
                unlockedAchievementIds = uiState.unlockedAchievementIds,
                uniqueSpeciesCount = uiState.uniqueSpeciesCount
            )
        }

        composable("rally_certificate") {
            val rallyViewModel: paulify.baeumeinwien.ui.screens.rally.RallyViewModel = viewModel(factory = viewModelFactory)
            val uiState by rallyViewModel.uiState.collectAsState()
            
            if (uiState.currentRally != null && uiState.progress != null) {
                paulify.baeumeinwien.ui.screens.rally.RallyCertificateScreen(
                    rally = uiState.currentRally!!,
                    progress = uiState.progress!!,
                    onClose = { navController.popBackStack() }
                )
            }
        }

        
        composable("settings") {
            val mapEntry = navController.getBackStackEntry(Screen.Map.route)
            val mapViewModel: MapViewModel = viewModel(mapEntry, factory = viewModelFactory)
            val uiState by mapViewModel.uiState.collectAsState()

            SettingsScreen(
                currentVersion = uiState.currentVersion,
                showOnlyFavorites = uiState.showFavoritesOnly,
                enable3DBuildings = uiState.show3DBuildings,
                onToggleFavorites = { mapViewModel.toggleFavoritesOnly() },
                onToggle3DBuildings = { mapViewModel.toggle3DBuildings() },
                onCheckForUpdates = { mapViewModel.checkForUpdates() },
                onClearCache = {
                    mapViewModel.clearCacheAndReload()
                    navController.navigate(Screen.Map.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }


        composable(Screen.LeafScanner.route) {
            val viewModel: LeafScannerViewModel = viewModel(factory = viewModelFactory)
            LeafScannerScreen(
                viewModel = viewModel,
                onSearchSpecies = { species ->
                    navController.navigate(Screen.Map.route)
                }
            )
        }

        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddTree.route) {
            val addTreeViewModel: AddTreeViewModel = viewModel(factory = viewModelFactory)
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            val authState by authViewModel.authState.collectAsState()

            AddTreeScreen(
                viewModel = addTreeViewModel,
                authState = authState,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack(Screen.Map.route, inclusive = false)
                },
                onNeedLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.AddTree.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

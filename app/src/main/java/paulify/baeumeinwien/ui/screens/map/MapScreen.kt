package paulify.baeumeinwien.ui.screens.map

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.AuthState
import paulify.baeumeinwien.data.domain.CommunityTree
import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.ui.screens.map.components.TreeDetailBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onAddTree: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val highlightedTree by viewModel.highlightedTree.collectAsState()
    val newAchievement by viewModel.newAchievement.collectAsState()
    val newGameAchievement by viewModel.newGameAchievement.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    var isSatelliteView by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(true) }
    var mapLibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            mapLibreMapInstance?.let { map ->
                try {
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                            as android.location.LocationManager
                    val provider = if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
                        android.location.LocationManager.GPS_PROVIDER
                    else
                        android.location.LocationManager.NETWORK_PROVIDER
                    val location = locationManager.getLastKnownLocation(provider)
                    location?.let {
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                17.0
                            ),
                            1000
                        )
                    }
                } catch (e: SecurityException) {
                }
            }
        }
    }
    
    val prefs = remember { context.getSharedPreferences("baumkatastar_prefs", android.content.Context.MODE_PRIVATE) }
    
    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("has_chosen_download_mode", false)) {
            prefs.edit().putBoolean("has_chosen_download_mode", true).apply()
            prefs.edit().putBoolean("offline_mode", false).apply()
            viewModel.enableOnlineMode()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapContent(
                context = context,
                trees = uiState.trees,
                communityTrees = uiState.communityTrees,
                highlightedTree = highlightedTree,
                isSatelliteView = isSatelliteView,
                initialPosition = uiState.savedCameraPosition,
                initialZoom = uiState.savedZoom,
                onCameraIdle = { bounds, zoom ->
                    viewModel.onCameraIdle(bounds, zoom)
                },
                onTreeClick = { tree ->
                    viewModel.selectTree(tree)
                },
                onCommunityTreeClick = { tree ->
                    viewModel.selectCommunityTree(tree)
                },
                onMapReady = { map ->
                    mapLibreMapInstance = map
                }
            )

        paulify.baeumeinwien.ui.screens.map.components.ExpressiveTreeSearchBar(
            query = searchQuery,
            onQueryChange = { newValue ->
                searchQuery = newValue
                if (newValue.length >= 2) {
                    viewModel.onSearchQueryChanged(newValue)
                } else {
                    viewModel.clearSearch()
                }
            },
            onSearch = { query ->
                if (query.length >= 2) {
                    viewModel.onSearchQueryChanged(query)
                }
            },
            searchResults = uiState.searchResults,
            onTreeClick = { tree ->
                mapLibreMapInstance?.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                        LatLng(tree.latitude, tree.longitude),
                        18.0
                    ),
                    1000
                )
                viewModel.highlightTree(tree)
                viewModel.selectTree(tree)
                searchQuery = ""
                viewModel.clearSearch()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp, start = 16.dp, end = 16.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 90.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledIconButton(
                onClick = { isSatelliteView = !isSatelliteView }
            ) {
                Icon(
                    imageVector = if (isSatelliteView) Icons.Default.LayersClear else Icons.Default.Satellite,
                    contentDescription = if (isSatelliteView) "Straßenansicht" else "Satellitenansicht"
                )
            }

            FilledIconButton(
                onClick = {
                    is3DMode = !is3DMode
                    mapLibreMapInstance?.let { map ->
                        val currentPos = map.cameraPosition
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(currentPos.target)
                                    .zoom(currentPos.zoom)
                                    .tilt(if (is3DMode) 60.0 else 0.0)
                                    .bearing(currentPos.bearing)
                                    .build()
                            ),
                            1000
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Terrain,
                    contentDescription = if (is3DMode) "2D-Ansicht" else "3D-Ansicht",
                    tint = if (is3DMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            ExtendedFloatingActionButton(
                onClick = onAddTree,
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddLocationAlt,
                        contentDescription = "Baum hinzufügen"
                    )
                },
                text = { Text("Baum melden") },
                containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800),
                contentColor = androidx.compose.ui.graphics.Color.White
            )

            ExtendedFloatingActionButton(
                onClick = { 
                    uiState.currentBounds?.let { bounds ->
                        viewModel.refreshViewport(bounds)
                    }
                },
                icon = {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Neu laden"
                        )
                    }
                },
                text = { Text("Aktualisieren") }
            )
            
            ExtendedFloatingActionButton(
                onClick = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        mapLibreMapInstance?.let { map ->
                            try {
                                val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                                        as android.location.LocationManager
                                val provider = if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
                                    android.location.LocationManager.GPS_PROVIDER
                                else
                                    android.location.LocationManager.NETWORK_PROVIDER
                                val location = locationManager.getLastKnownLocation(provider)
                                location?.let {
                                    map.animateCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            LatLng(it.latitude, it.longitude),
                                            17.0
                                        ),
                                        1000
                                    )
                                }
                            } catch (e: SecurityException) {
                                android.util.Log.e("MapScreen", "Location permission not granted", e)
                            }
                        }
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Mein Standort"
                    )
                },
                text = { Text("Standort") }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        uiState.selectedTree?.let { tree ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearSelection() },
                sheetState = sheetState
            ) {
                TreeDetailBottomSheet(
                    tree = tree,
                    onToggleFavorite = { viewModel.toggleFavorite(tree.id) },
                    onDismiss = { viewModel.clearSelection() }
                )
            }
        }

        uiState.selectedCommunityTree?.let { communityTree ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearSelection() },
                sheetState = sheetState
            ) {
                CommunityTreeDetailSheet(
                    tree = communityTree,
                    authState = authState,
                    currentUserId = viewModel.getCurrentUserId(),
                    currentUserRole = currentUserRole,
                    onDismiss = { viewModel.clearSelection() },
                    onConfirm = { viewModel.confirmCommunityTree(communityTree.id) },
                    onReport = { reason, comment ->
                        viewModel.reportCommunityTree(communityTree.id, reason, comment)
                    },
                    onDelete = { viewModel.deleteCommunityTree(communityTree.id) },
                    onVerify = { viewModel.adminVerifyCommunityTree(communityTree.id) }
                )
            }
        }
        
        newGameAchievement?.let { achievement ->
            paulify.baeumeinwien.ui.screens.rally.components.GameAchievementUnlockedDialog(
                achievement = achievement,
                onDismiss = { viewModel.clearNewAchievement() }
            )
        }
        
        if (newGameAchievement == null) {
            newAchievement?.let { achievement ->
                paulify.baeumeinwien.ui.screens.rally.components.AchievementUnlockedDialog(
                    achievement = achievement,
                    onDismiss = { viewModel.clearNewAchievement() }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    downloadProgress: paulify.baeumeinwien.data.repository.DownloadProgress
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forest,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            if (downloadProgress.totalTrees > 0) {
                val animatedProgress by animateFloatAsState(
                    targetValue = downloadProgress.progressPercent / 100f,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(12.dp)
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.map_loading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (downloadProgress.totalTrees > 0) {
                Text(
                    text = stringResource(R.string.map_trees_loaded, downloadProgress.downloadedTrees, downloadProgress.totalTrees),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.map_percent_downloaded, downloadProgress.progressPercent),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(R.string.map_fetching_data),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MapContent(
    context: Context,
    trees: List<Tree>,
    communityTrees: List<CommunityTree> = emptyList(),
    highlightedTree: Tree?,
    isSatelliteView: Boolean,
    initialPosition: LatLng?,
    initialZoom: Double,
    onCameraIdle: (LatLngBounds, Double) -> Unit,
    onTreeClick: (Tree) -> Unit,
    onCommunityTreeClick: (CommunityTree) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit
) {
    var mapLibreMap: MapLibreMap? by remember { mutableStateOf(null) }
    var lastBounds: LatLngBounds? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    var isInitialLoad by remember { mutableStateOf(true) }
    
    val currentTrees = remember { mutableStateOf(trees) }
    val currentCommunityTrees = remember { mutableStateOf(communityTrees) }
    LaunchedEffect(trees) {
        currentTrees.value = trees
    }
    LaunchedEffect(communityTrees) {
        currentCommunityTrees.value = communityTrees
    }

    DisposableEffect(Unit) {
        MapLibre.getInstance(context)
        onDispose { }
    }

    LaunchedEffect(trees) {
        android.util.Log.d("MapScreen", "Trees changed: ${trees.size} trees")
        mapLibreMap?.let { map ->
            map.getStyle { style ->
                updateTreeMarkers(style, trees)
                android.util.Log.d("MapScreen", "Tree markers updated")
                
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        if (!map.locationComponent.isLocationComponentActivated) {
                            map.locationComponent.apply {
                                activateLocationComponent(
                                    org.maplibre.android.location.LocationComponentActivationOptions
                                        .builder(context, style)
                                        .build()
                                )
                                isLocationComponentEnabled = true
                                renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                            }
                        } else {
                            map.locationComponent.isLocationComponentEnabled = true
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MapScreen", "Location activation error", e)
                    }
                }
            }
        }
    }

    LaunchedEffect(communityTrees) {
        android.util.Log.d("MapScreen", "Community trees changed: ${communityTrees.size}")
        mapLibreMap?.let { map ->
            map.getStyle { style ->
                updateCommunityTreeMarkers(style, communityTrees)
            }
        }
    }

    LaunchedEffect(highlightedTree) {
        mapLibreMap?.let { map ->
            map.getStyle { style ->
                style.getLayer("highlight-pulse-layer")?.let { style.removeLayer(it) }
                style.getLayer("highlight-glow-layer")?.let { style.removeLayer(it) }
                style.getSource("highlight-source")?.let { style.removeSource(it) }
                
                highlightedTree?.let { tree ->
                    android.util.Log.d("MapScreen", "Highlighting tree: ${tree.speciesGerman}")
                    
                    val highlightFeature = Feature.fromGeometry(
                        Point.fromLngLat(tree.longitude, tree.latitude)
                    )
                    val highlightSource = GeoJsonSource(
                        "highlight-source",
                        FeatureCollection.fromFeatures(listOf(highlightFeature))
                    )
                    style.addSource(highlightSource)
                    
                    val glowLayer = CircleLayer("highlight-glow-layer", "highlight-source").apply {
                        setProperties(
                            circleRadius(35f),
                            circleColor(0xFFFFD700.toInt()),
                            circleOpacity(0.3f),
                            circleStrokeWidth(0f)
                        )
                    }
                    style.addLayerAbove(glowLayer, "trees-layer")
                    
                    val pulseLayer = CircleLayer("highlight-pulse-layer", "highlight-source").apply {
                        setProperties(
                            circleRadius(20f),
                            circleColor(0xFFFF6B00.toInt()),
                            circleOpacity(0.8f),
                            circleStrokeColor(0xFFFFFFFF.toInt()),
                            circleStrokeWidth(4f)
                        )
                    }
                    style.addLayerAbove(pulseLayer, "highlight-glow-layer")
                    
                    scope.launch {
                        repeat(25) { i ->
                            val phase = (i % 10) / 10f
                            val pulseRadius = 20f + (phase * 15f)
                            val glowRadius = 35f + (phase * 20f)
                            val pulseOpacity = 0.8f - (phase * 0.3f)
                            val glowOpacity = 0.3f - (phase * 0.15f)
                            
                            mapLibreMap?.getStyle { s ->
                                (s.getLayer("highlight-pulse-layer") as? CircleLayer)?.setProperties(
                                    circleRadius(pulseRadius),
                                    circleOpacity(pulseOpacity)
                                )
                                (s.getLayer("highlight-glow-layer") as? CircleLayer)?.setProperties(
                                    circleRadius(glowRadius),
                                    circleOpacity(glowOpacity)
                                )
                            }
                            delay(200L)
                        }
                        
                        mapLibreMap?.getStyle { s ->
                            s.getLayer("highlight-pulse-layer")?.let { s.removeLayer(it) }
                            s.getLayer("highlight-glow-layer")?.let { s.removeLayer(it) }
                            s.getSource("highlight-source")?.let { s.removeSource(it) }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isSatelliteView) {
        mapLibreMap?.let { map ->
            val currentPosition = map.cameraPosition

            val styleUrl = if (isSatelliteView) "asset://satellite_style.json" else "https://tiles.openfreemap.org/styles/bright"

            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                map.cameraPosition = currentPosition
                add3DBuildingsLayer(style)
                updateTreeMarkers(style, trees)
                updateCommunityTreeMarkers(style, communityTrees)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                onCreate(null)
                onStart()
                onResume()

                getMapAsync { map ->
                    mapLibreMap = map
                    onMapReady(map)

                    val startPosition = initialPosition ?: LatLng(48.2082, 16.3738)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(startPosition)
                        .zoom(initialZoom)
                        .tilt(45.0)
                        .bearing(-17.6)
                        .build()

                    map.setStyle("https://tiles.openfreemap.org/styles/bright") { style ->
                        add3DBuildingsLayer(style)
                        
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            try {
                                map.locationComponent.apply {
                                    activateLocationComponent(
                                        org.maplibre.android.location.LocationComponentActivationOptions
                                            .builder(context, style)
                                            .build()
                                    )
                                    isLocationComponentEnabled = true
                                    renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                                }
                                android.util.Log.d("MapScreen", "Location component activated")
                            } catch (e: Exception) {
                                android.util.Log.e("MapScreen", "Location init error", e)
                            }
                        }
                        
                        updateTreeMarkers(style, trees)
                        updateCommunityTreeMarkers(style, communityTrees)
                        isInitialLoad = false

                        val initialBounds = map.projection.visibleRegion.latLngBounds
                        val initialZoom = map.cameraPosition.zoom
                        onCameraIdle(initialBounds, initialZoom)
                    }

                    map.addOnMapClickListener { clickedPoint ->
                        val treesToCheck = currentTrees.value
                        val communityTreesToCheck = currentCommunityTrees.value
                        android.util.Log.d("MapScreen", "=== MAP CLICK ===")
                        android.util.Log.d("MapScreen", "Clicked: ${clickedPoint.latitude}, ${clickedPoint.longitude}")
                        android.util.Log.d("MapScreen", "Trees available: ${treesToCheck.size}, Community: ${communityTreesToCheck.size}")

                        if (treesToCheck.isEmpty() && communityTreesToCheck.isEmpty()) {
                            android.util.Log.w("MapScreen", "No trees loaded!")
                            return@addOnMapClickListener false
                        }

                        var nearestTree: Tree? = null
                        var nearestDistance = Float.MAX_VALUE

                        for (tree in treesToCheck) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                clickedPoint.latitude, clickedPoint.longitude,
                                tree.latitude, tree.longitude,
                                results
                            )
                            val distance = results[0]

                            if (distance < nearestDistance) {
                                nearestDistance = distance
                                nearestTree = tree
                            }
                        }

                        var nearestCommunityTree: CommunityTree? = null
                        var nearestCommunityDistance = Float.MAX_VALUE

                        for (tree in communityTreesToCheck) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                clickedPoint.latitude, clickedPoint.longitude,
                                tree.latitude, tree.longitude,
                                results
                            )
                            val distance = results[0]

                            if (distance < nearestCommunityDistance) {
                                nearestCommunityDistance = distance
                                nearestCommunityTree = tree
                            }
                        }

                        android.util.Log.d("MapScreen", "Nearest official: ${nearestTree?.speciesGerman} at ${nearestDistance}m")
                        android.util.Log.d("MapScreen", "Nearest community: ${nearestCommunityTree?.speciesGerman} at ${nearestCommunityDistance}m")

                        val zoom = map.cameraPosition.zoom
                        val maxDistance = when {
                            zoom >= 18 -> 20f
                            zoom >= 16 -> 50f
                            zoom >= 14 -> 100f
                            else -> 200f
                        }

                        val useOfficial = nearestDistance <= nearestCommunityDistance

                        if (useOfficial && nearestTree != null && nearestDistance <= maxDistance) {
                            android.util.Log.d("MapScreen", "Opening official tree detail!")
                            onTreeClick(nearestTree)
                            true
                        } else if (!useOfficial && nearestCommunityTree != null && nearestCommunityDistance <= maxDistance) {
                            android.util.Log.d("MapScreen", "Opening community tree detail!")
                            onCommunityTreeClick(nearestCommunityTree)
                            true
                        } else if (nearestTree != null && nearestDistance <= maxDistance) {
                            onTreeClick(nearestTree)
                            true
                        } else if (nearestCommunityTree != null && nearestCommunityDistance <= maxDistance) {
                            onCommunityTreeClick(nearestCommunityTree)
                            true
                        } else {
                            android.util.Log.d("MapScreen", "No tree close enough (need ${maxDistance}m)")
                            false
                        }
                    }

                    map.addOnCameraIdleListener {
                        if (!isInitialLoad) {
                            val bounds = map.projection.visibleRegion.latLngBounds
                            val zoom = map.cameraPosition.zoom

                            if (lastBounds == null || !boundsAreSimilar(lastBounds!!, bounds)) {
                                lastBounds = bounds
                                onCameraIdle(bounds, zoom)
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = {
        },
        onRelease = { view ->
            view.onPause()
            view.onStop()
            view.onDestroy()
        }
    )
}

private fun add3DBuildingsLayer(style: Style) {
    if (style.getSource("openfreemap") == null) {
        style.addSource(
            VectorSource("openfreemap", "https://tiles.openfreemap.org/planet")
        )
    }

    style.getLayer("3d-buildings")?.let { style.removeLayer(it) }

    val layers = style.layers
    var labelLayerId: String? = null
    for (i in layers.indices) {
        if (layers[i].id.contains("label") || layers[i].id.contains("text")) {
            labelLayerId = layers[i].id
            break
        }
    }

    val buildingsLayer = FillExtrusionLayer("3d-buildings", "openfreemap").apply {
        sourceLayer = "building"
        minZoom = 14f

        setFilter(
            neq(get("hide_3d"), literal(true))
        )

        setProperties(
            fillExtrusionColor(
                interpolate(
                    linear(),
                    zoom(),
                    literal(15), color(0xFFB0B0B0.toInt()),
                    literal(18), color(0xFFD3D3D3.toInt())
                )
            ),
            fillExtrusionHeight(get("render_height")),
            fillExtrusionBase(get("render_min_height")),
            fillExtrusionOpacity(
                interpolate(
                    linear(),
                    zoom(),
                    literal(14), literal(0.6),
                    literal(16), literal(0.85)
                )
            )
        )
    }

    if (labelLayerId != null) {
        style.addLayerBelow(buildingsLayer, labelLayerId)
    } else {
        style.addLayer(buildingsLayer)
    }

    android.util.Log.d("MapScreen", "3D buildings layer added successfully")
}

private fun updateTreeMarkers(style: Style, trees: List<Tree>) {
    android.util.Log.d("MapScreen", "updateTreeMarkers called with ${trees.size} trees")
    
    style.getLayer("trees-layer")?.let { style.removeLayer(it) }
    style.getSource("trees-source")?.let { style.removeSource(it) }

    val features = trees.mapNotNull { tree ->
        try {
            val properties = com.google.gson.JsonObject().apply {
                addProperty("id", tree.id)
                addProperty("species", tree.speciesGerman)
                addProperty("address", tree.street ?: "")
                addProperty("isFavorite", tree.isFavorite)
            }
            
            Feature.fromGeometry(
                Point.fromLngLat(tree.longitude, tree.latitude),
                properties,
                tree.id
            )
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "Error creating marker for tree ${tree.id}", e)
            null
        }
    }
    
    android.util.Log.d("MapScreen", "Created ${features.size} features")
    
    val geoJsonSource = GeoJsonSource("trees-source", FeatureCollection.fromFeatures(features))
    style.addSource(geoJsonSource)

    val circleLayer = CircleLayer("trees-layer", "trees-source").apply {
        setProperties(
            circleRadius(12f),
            circleColor(
                switchCase(
                    get("isFavorite"),
                    color(0xFF4CAF50.toInt()),
                    color(0xFF2196F3.toInt())
                )
            ),
            circleStrokeColor(0xFFFFFFFF.toInt()),
            circleStrokeWidth(3f),
            circleOpacity(0.95f)
        )
    }
    style.addLayer(circleLayer)
    
    android.util.Log.d("MapScreen", "Trees layer added to map")
}


private fun boundsAreSimilar(bounds1: LatLngBounds, bounds2: LatLngBounds): Boolean {
    val threshold = 0.001
    return abs(bounds1.latitudeNorth - bounds2.latitudeNorth) < threshold &&
           abs(bounds1.longitudeEast - bounds2.longitudeEast) < threshold &&
           abs(bounds1.latitudeSouth - bounds2.latitudeSouth) < threshold &&
           abs(bounds1.longitudeWest - bounds2.longitudeWest) < threshold
}

private fun updateCommunityTreeMarkers(style: Style, communityTrees: List<CommunityTree>) {
    android.util.Log.d("MapScreen", "updateCommunityTreeMarkers called with ${communityTrees.size} trees")

    style.getLayer("community-trees-layer")?.let { style.removeLayer(it) }
    style.getSource("community-trees-source")?.let { style.removeSource(it) }

    if (communityTrees.isEmpty()) return

    val features = communityTrees.mapNotNull { tree ->
        try {
            val properties = com.google.gson.JsonObject().apply {
                addProperty("id", tree.id)
                addProperty("species", tree.speciesGerman)
                addProperty("user", tree.userDisplayName ?: "Community")
            }

            Feature.fromGeometry(
                Point.fromLngLat(tree.longitude, tree.latitude),
                properties,
                tree.id
            )
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "Error creating community marker for tree ${tree.id}", e)
            null
        }
    }

    val geoJsonSource = GeoJsonSource("community-trees-source", FeatureCollection.fromFeatures(features))
    style.addSource(geoJsonSource)

    val circleLayer = CircleLayer("community-trees-layer", "community-trees-source").apply {
        setProperties(
            circleRadius(12f),
            circleColor(0xFFFF9800.toInt()),
            circleStrokeColor(0xFFFFFFFF.toInt()),
            circleStrokeWidth(3f),
            circleOpacity(0.95f)
        )
    }
    style.addLayer(circleLayer)

    android.util.Log.d("MapScreen", "Community trees layer added to map")
}

@Composable
private fun CommunityTreeDetailSheet(
    tree: CommunityTree,
    authState: AuthState,
    currentUserId: String?,
    currentUserRole: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReport: (reason: String, comment: String?) -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit = {}
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isLoggedIn = authState is AuthState.Authenticated
    val isAdmin = currentUserRole in listOf("admin", "official")
    val isOwnTree = currentUserId != null && currentUserId == tree.userId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = androidx.compose.ui.graphics.Color(0xFFFF9800).copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocationAlt,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Community-Baum",
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = if (tree.isOfficialTree) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (tree.creatorDisplayText.firstOrNull() ?: 'C').uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (tree.isOfficialTree) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tree.creatorDisplayText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (tree.userIsVerified == true || tree.isOfficialTree) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_verified_badge),
                            contentDescription = "Verifiziert",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
                tree.createdAt?.let {
                    Text(
                        text = "Hinzugefügt am ${it.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                tree.officialStatusText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF78A75A)
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (tree.isVerified) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tree.isVerified) Icons.Default.CheckCircle
                        else Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (tree.isVerified) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${tree.confirmationCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tree.isVerified) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tree.speciesGerman,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        tree.speciesScientific?.let { scientific ->
            Text(
                text = scientific,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                CommunityDetailRow("Koordinaten", "%.5f, %.5f".format(tree.latitude, tree.longitude))
                CommunityDetailRow("Standortmethode", if (tree.locationMethod == "gps") "GPS" else "Karte")
                tree.gpsAccuracyMeters?.let {
                    CommunityDetailRow("GPS-Genauigkeit", "%.1f m".format(it))
                }
                tree.estimatedHeight?.let {
                    CommunityDetailRow("Höhe", "%.1f m".format(it))
                }
                tree.estimatedTrunkCircumference?.let {
                    CommunityDetailRow("Stammumfang", "$it cm")
                }
                tree.district?.let {
                    CommunityDetailRow("Bezirk", it.toString())
                }
                if (tree.status != "approved") {
                    CommunityDetailRow("Status", when (tree.status) {
                        "verified" -> "Verifiziert"
                        "pending" -> "Ausstehend"
                        "rejected" -> "Abgelehnt"
                        else -> tree.status
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoggedIn && !isOwnTree) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Baum bestätigen", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoggedIn && !isOwnTree) {
            OutlinedButton(
                onClick = { showReportDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Melden")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isOwnTree || isAdmin) {
            if (isAdmin && !tree.isVerified) {
                Button(
                    onClick = onVerify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78A75A),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Offiziell bestätigen", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Baum löschen")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Schließen")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showReportDialog) {
        ReportTreeDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, comment ->
                onReport(reason, comment)
                showReportDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Baum löschen?", fontWeight = FontWeight.Bold) },
            text = { Text("Dieser Baum wird dauerhaft von der Community-Karte entfernt.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun ReportTreeDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, comment: String?) -> Unit
) {
    val reasons = listOf(
        "wrong_location" to "Falscher Standort",
        "wrong_species" to "Falsche Baumart",
        "does_not_exist" to "Baum existiert nicht",
        "spam" to "Spam",
        "other" to "Sonstiges"
    )
    var selectedReason by remember { mutableStateOf(reasons.first().first) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Baum melden", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Warum möchtest du diesen Baum melden?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                reasons.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == key,
                                onClick = { selectedReason = key },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == key,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Kommentar (optional)") },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, comment.ifBlank { null }) },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Melden")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun CommunityDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

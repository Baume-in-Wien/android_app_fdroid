package paulify.baeumeinwien.ui.screens.rally

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.Achievement
import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.ui.screens.rally.components.AchievementUnlockedDialog
import android.location.LocationListener
import android.location.LocationManager
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import paulify.baeumeinwien.ui.screens.achievements.AchievementsGalleryScreen
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import paulify.baeumeinwien.ui.screens.map.MapContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloExplorerScreen(
    viewModel: RallyViewModel,
    onBack: () -> Unit,
    onNavigateToAchievements: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var nearbyTrees by remember { mutableStateOf<List<Pair<Tree, Float>>>(emptyList()) }
    var discoveredTrees by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedRadius by remember { mutableStateOf(500.0) }
    var missionTrees by remember { mutableStateOf<List<Tree>>(emptyList()) }
    var completedMissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMap by remember { mutableStateOf(true) }
    var capturedPhotos by remember { mutableStateOf<Map<String, android.graphics.Bitmap>>(emptyMap()) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showGpsTips by remember { mutableStateOf(false) }
    
    val locationManager = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager }
    
    LaunchedEffect(Unit) {
        viewModel.loadAchievements()
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLocationUpdates(locationManager, context) { location ->
                currentLocation = location
            }
        }
    }

    uiState.newAchievement?.let { achievement ->
        AchievementUnlockedDialog(
            achievement = achievement,
            onDismiss = { viewModel.clearNewAchievement() }
        )
    }

    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val listener = LocationListener { location ->
                currentLocation = location
            }

            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                LocationManager.GPS_PROVIDER
            else
                LocationManager.NETWORK_PROVIDER

            try {
                locationManager.requestLocationUpdates(provider, 2000L, 0f, listener)
            } catch (e: SecurityException) {
                android.util.Log.e("SoloExplorer", "Location permission missing")
            }

            onDispose {
                locationManager.removeUpdates(listener)
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            onDispose { }
        }
    }

    LaunchedEffect(currentLocation, selectedRadius) {
        currentLocation?.let { loc ->
            viewModel.loadTreesNearby(loc.latitude, loc.longitude, radiusMeters = selectedRadius)
        }
    }

    LaunchedEffect(currentLocation, uiState.targetTrees) {
        currentLocation?.let { loc ->
            nearbyTrees = uiState.targetTrees.map { tree ->
                val distance = calculateDistance(
                    loc.latitude, loc.longitude,
                    tree.latitude, tree.longitude
                )
                tree to distance
            }.sortedBy { it.second }
        }
    }

    LaunchedEffect(nearbyTrees, selectedRadius) {
        if (nearbyTrees.isNotEmpty() && missionTrees.isEmpty()) {
            val shuffled = nearbyTrees.shuffled()
            missionTrees = shuffled.take(5).map { it.first }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            android.util.Log.e("SoloExplorer", "Camera permission denied")
        }
    }

    var currentPhotoTreeId by remember { mutableStateOf<String?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            currentPhotoTreeId?.let { treeId ->
                capturedPhotos = capturedPhotos + (treeId to bitmap)
                completedMissions = completedMissions + treeId
                
                val tree = missionTrees.find { it.id == treeId }
                tree?.let { t ->
                    val species = t.speciesGerman
                    val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Ei-leaf.svg/1200px-Ei-leaf.svg.png"
                    viewModel.unlockAchievement(species, imageUrl)
                }
                currentPhotoTreeId = null
            }
        }
    }

    LaunchedEffect(nearbyTrees, missionTrees, completedMissions) {
        nearbyTrees.forEach { (tree, distance) ->
            if (distance < 20f && 
                missionTrees.any { it.id == tree.id } && 
                !completedMissions.contains(tree.id)) {
                currentPhotoTreeId = tree.id
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(28.dp))
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.nav_explorer).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "${completedMissions.size}/${missionTrees.size} ${stringResource(R.string.rally_missions)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    val achievementCount = uiState.achievements.size
                    Surface(
                        onClick = { showAchievementsDialog = true },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Text(
                                "$achievementCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = showMap,
                        onClick = { showMap = true },
                        icon = { Icon(Icons.Default.Map, stringResource(R.string.rally_map), modifier = Modifier.size(28.dp)) },
                        label = { Text(stringResource(R.string.rally_map).uppercase(), fontWeight = if(showMap) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = !showMap,
                        onClick = { showMap = false },
                        icon = { Icon(Icons.Default.List, stringResource(R.string.rally_missions), modifier = Modifier.size(28.dp)) },
                        label = { Text(stringResource(R.string.rally_missions).uppercase(), fontWeight = if(!showMap) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentLocation == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            stringResource(R.string.gps_waiting),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val accuracy = currentLocation!!.accuracy.toInt()
                val accuracyText = when {
                    accuracy <= 5 -> stringResource(R.string.gps_accuracy_excellent)
                    accuracy <= 10 -> stringResource(R.string.gps_accuracy_good)
                    accuracy <= 20 -> stringResource(R.string.gps_accuracy_moderate)
                    else -> stringResource(R.string.gps_accuracy_poor)
                }
                val accuracyColor = when {
                    accuracy <= 5 -> Color(0xFF4CAF50)
                    accuracy <= 10 -> Color(0xFF8BC34A)
                    accuracy <= 20 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accuracyColor.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { showGpsTips = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = accuracyColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${stringResource(R.string.gps_accuracy)}: $accuracyText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.gps_accuracy_meters, accuracy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        if (accuracy > 10) {
                            TextButton(
                                onClick = { showGpsTips = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    stringResource(R.string.gps_tips_button),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            RadiusSelector(
                selectedRadius = selectedRadius,
                onRadiusChange = { newRadius ->
                    selectedRadius = newRadius
                    missionTrees = emptyList()
                    completedMissions = emptySet()
                }
            )

            if (showMap) {
                currentLocation?.let { location ->
                    MissionMapView(
                        currentLocation = location,
                        missionTrees = missionTrees,
                        completedMissions = completedMissions,
                        radius = selectedRadius,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            } else {
                MissionListView(
                    missionTrees = missionTrees,
                    completedMissions = completedMissions,
                    currentLocation = currentLocation,
                    capturedPhotos = capturedPhotos,
                    onTakePhoto = { treeId ->
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            currentPhotoTreeId = treeId
                            takePictureLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    currentPhotoTreeId?.let { treeId ->
        val tree = missionTrees.find { it.id == treeId }
        tree?.let {
            PhotoPromptDialog(
                tree = it,
                onTakePhoto = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        takePictureLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onDismiss = { currentPhotoTreeId = null }
            )
        }
    }
    
    if (showGpsTips) {
        AlertDialog(
            onDismissRequest = { showGpsTips = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.gps_tips_title))
                }
            },
            text = {
                Text(stringResource(R.string.gps_tips_content))
            },
            confirmButton = {
                TextButton(onClick = { showGpsTips = false }) {
                    Text(stringResource(R.string.ar_understood))
                }
            }
        )
    }
    
    if (showAchievementsDialog) {
        Dialog(
            onDismissRequest = { showAchievementsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val uniqueCount = uiState.achievements.size
            val unlockedIds = remember(uniqueCount) {
                paulify.baeumeinwien.data.domain.GameAchievement.allAchievements
                    .filter { 
                        it.category == paulify.baeumeinwien.data.domain.AchievementCategory.SPECIES && 
                        (it.targetCount ?: Int.MAX_VALUE) <= uniqueCount
                    }
                    .map { it.id }
                    .toSet()
            }
            
            AchievementsGalleryScreen(
                unlockedAchievementIds = unlockedIds,
                uniqueSpeciesCount = uniqueCount,
                onBack = { showAchievementsDialog = false }
            )
        }
    }
}

@Composable
fun RadiusSelector(
    selectedRadius: Double,
    onRadiusChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                stringResource(R.string.explorer_radius).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(500.0 to "500m", 1000.0 to "1km", 2000.0 to "2km", 5000.0 to "5km").forEach { (radius, label) ->
                    FilterChip(
                        selected = selectedRadius == radius,
                        onClick = { onRadiusChange(radius) },
                        label = { 
                            Text(
                                label, 
                                fontWeight = if(selectedRadius == radius) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MissionMapView(
    currentLocation: Location,
    missionTrees: List<Tree>,
    completedMissions: Set<String>,
    radius: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(modifier = modifier.fillMaxSize()) {
        MapContent(
            context = context,
            trees = missionTrees,
            highlightedTree = null,
            isSatelliteView = false,
            initialPosition = LatLng(currentLocation.latitude, currentLocation.longitude),
            initialZoom = 15.0,
            onCameraIdle = { _, _ -> },
            onTreeClick = { /* Optional: Show detail */ },
            onMapReady = { map -> 
            }
        )
        
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Text(
                stringResource(R.string.explorer_trees_in_radius, missionTrees.size),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MissionListView(
    missionTrees: List<Tree>,
    completedMissions: Set<String>,
    currentLocation: Location?,
    capturedPhotos: Map<String, android.graphics.Bitmap>,
    onTakePhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (missionTrees.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.missions_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.missions_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(missionTrees) { tree ->
            val isCompleted = completedMissions.contains(tree.id)
            val distance = currentLocation?.let {
                calculateDistance(it.latitude, it.longitude, tree.latitude, tree.longitude)
            }
            val photo = capturedPhotos[tree.id]
            
            MissionTreeCard(
                tree = tree,
                distance = distance,
                isCompleted = isCompleted,
                photo = photo,
                onTakePhoto = { onTakePhoto(tree.id) }
            )
        }
    }
}

@Composable
fun MissionTreeCard(
    tree: Tree,
    distance: Float?,
    isCompleted: Boolean,
    photo: android.graphics.Bitmap?,
    onTakePhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCompleted) Color(0xFF4CAF50) 
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (photo != null) {
                    androidx.compose.foundation.Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = stringResource(R.string.photo_of, tree.speciesGerman),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    tree.speciesGerman,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    tree.street ?: stringResource(R.string.search_no_street),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                distance?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.rally_meters_away, it.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isCompleted && distance != null && distance < 20f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onTakePhoto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.rally_photo_made))
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoPromptDialog(
    tree: Tree,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.explorer_tree_found))
        },
        text = {
            Column {
                Text(stringResource(R.string.rally_you_are_at))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    tree.speciesGerman,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.rally_take_photo_to_complete))
            }
        },
        confirmButton = {
            Button(onClick = onTakePhoto) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.rally_photo_made))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.rally_later))
            }
        }
    )
}

private fun startLocationUpdates(
    locationManager: LocationManager,
    context: android.content.Context,
    onLocationUpdate: (Location) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        LocationManager.GPS_PROVIDER
    else
        LocationManager.NETWORK_PROVIDER

    try {
        val location = locationManager.getLastKnownLocation(provider)
        location?.let { onLocationUpdate(it) }
    } catch (e: SecurityException) {
        android.util.Log.e("SoloExplorer", "Location permission missing in startLocationUpdates")
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadius * c).toFloat()
}

package paulify.baeumeinwien.ui.screens.rally

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import paulify.baeumeinwien.data.domain.Tree
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RallyPlayScreen(
    viewModel: RallyViewModel,
    code: String,
    studentName: String,
    onBack: () -> Unit,
    onLeaveRally: () -> Unit = {},
    onNavigateToTree: (Tree) -> Unit,
    onViewCertificate: () -> Unit,
    onNavigateToTeacherDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCameraForTree by remember { mutableStateOf<Tree?>(null) }

    LaunchedEffect(code, studentName) {
        viewModel.joinRally(context, code, studentName)
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Rally verlassen?") },
            text = { Text("Bist du sicher, dass du die Rally verlassen möchtest? Dein Fortschritt wird gespeichert.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveRally(context)
                        showLeaveDialog = false
                        onLeaveRally()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Verlassen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.currentRally?.name ?: "Baum-Rallye",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${uiState.progress?.foundTreeIds?.size ?: 0} von ${uiState.targetTrees.size} gefunden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { 
                                    if (uiState.targetTrees.isNotEmpty()) 
                                        (uiState.progress?.foundTreeIds?.size ?: 0).toFloat() / uiState.targetTrees.size 
                                    else 0f 
                                },
                                modifier = Modifier.width(60.dp),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    val foundCount = uiState.progress?.foundTreeIds?.size ?: 0
                    val totalCount = uiState.targetTrees.size
                    
                    if (foundCount == totalCount && totalCount > 0) {
                        IconButton(onClick = onViewCertificate) {
                            Icon(Icons.Default.EmojiEvents, "Urkunde", tint = Color(0xFFFFD700))
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menü")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (uiState.isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Lehrer:innen-Dashboard") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToTeacherDashboard()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SupervisorAccount, null)
                                    }
                                )
                                Divider()
                            }
                            
                            DropdownMenuItem(
                                text = { Text("Aktualisieren") },
                                onClick = {
                                    showMenu = false
                                    viewModel.joinRally(context, code, studentName)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, null)
                                }
                            )
                            
                            Divider()
                            
                            DropdownMenuItem(
                                text = { Text("Rally verlassen", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showLeaveDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ExitToApp, 
                                        null, 
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, "Karte") },
                    label = { Text("Karte") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FormatListBulleted, "Liste") },
                    label = { Text("Bäume") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                showCameraForTree != null -> {
                    TreeCameraScreen(
                        tree = showCameraForTree!!,
                        onBack = { showCameraForTree = null },
                        onPhotoCaptured = { bitmap ->
                            viewModel.storeLeafPhoto(showCameraForTree!!.id, bitmap)
                            viewModel.completeTask(showCameraForTree!!.id, "Foto", 10)
                            viewModel.markTreeFound(showCameraForTree!!.id)
                            showCameraForTree = null
                        }
                    )
                }
                uiState.isLoading -> LoadingScreen()
                uiState.error != null -> ErrorScreen(uiState.error!!, onBack)
                selectedTab == 0 -> MapTab(
                    viewModel = viewModel, 
                    uiState = uiState, 
                    onOpenCamera = { tree -> showCameraForTree = tree }
                )
                selectedTab == 1 -> TreeListTab(viewModel, uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RallyTopBar(
    title: String,
    foundCount: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onViewCertificate: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "$foundCount von $totalCount gefunden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Zurück")
            }
        },
        actions = {
            if (foundCount == totalCount && totalCount > 0) {
                IconButton(onClick = onViewCertificate) {
                    Icon(Icons.Default.EmojiEvents, "Urkunde", tint = Color(0xFFFFD700))
                }
            }
            LinearProgressIndicator(
                progress = { if (totalCount > 0) foundCount.toFloat() / totalCount else 0f },
                modifier = Modifier
                    .width(60.dp)
                    .padding(end = 16.dp),
            )
        }
    )
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Text(
                "Rallye wird geladen...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorScreen(error: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Fehler",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Zurück")
            }
        }
    }
}

@Composable
fun MapTab(
    viewModel: RallyViewModel, 
    uiState: RallyUiState,
    onOpenCamera: (Tree) -> Unit
) {
    val context = LocalContext.current
    var mapInstance by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var showTreeDetail by remember { mutableStateOf<Tree?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            mapInstance?.getStyle { style ->
                try {
                    mapInstance?.locationComponent?.apply {
                        activateLocationComponent(
                            org.maplibre.android.location.LocationComponentActivationOptions
                                .builder(context, style)
                                .build()
                        )
                        isLocationComponentEnabled = true
                        cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING
                        renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Rally", "Location error: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)

        val sensorEventListener = object : android.hardware.SensorEventListener {
            var gravity: FloatArray? = null
            var geomagnetic: FloatArray? = null

            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                if (event.sensor.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                    gravity = event.values
                }
                if (event.sensor.type == android.hardware.Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values
                }
                if (gravity != null && geomagnetic != null) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    if (android.hardware.SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        android.hardware.SensorManager.getOrientation(R, orientation)
                        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        viewModel.updateDeviceBearing(azimuth)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnetometer, android.hardware.SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    LaunchedEffect(uiState.targetTrees, uiState.progress?.foundTreeIds, mapInstance) {
        android.util.Log.d("Rally", "LaunchedEffect: Trees changed, count=${uiState.targetTrees.size}")
        mapInstance?.getStyle { style ->
            android.util.Log.d("Rally", "Style ready, updating markers")
            updateTreeMarkers(
                style,
                uiState.targetTrees,
                uiState.progress?.foundTreeIds ?: emptyList(),
                uiState.nearbyTree?.id
            )
        } ?: android.util.Log.w("Rally", "Map instance or style not ready yet")
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.d("Rally", "Camera permission granted")
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        android.util.Log.d("Rally", "Camera result: bitmap=${bitmap != null}, tree=${showTreeDetail?.id}")
        if (bitmap != null && showTreeDetail != null) {
            viewModel.storeLeafPhoto(showTreeDetail!!.id, bitmap)
            android.util.Log.d("Rally", "Photo stored for tree ${showTreeDetail!!.id}")
        }
    }
    
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                org.maplibre.android.maps.MapView(ctx).apply {
                    onCreate(null)
                    onStart()
                    onResume()

                    getMapAsync { map ->
                        mapInstance = map

                        val vienna = LatLng(48.2082, 16.3738)
                        map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(vienna)
                            .zoom(14.0)
                            .build()

                        map.setStyle("https://tiles.openfreemap.org/styles/bright") { style ->
                            android.util.Log.d("Rally", "Style loaded, initial trees: ${uiState.targetTrees.size}")
                            
                            if (uiState.targetTrees.isNotEmpty()) {
                                updateTreeMarkers(
                                    style,
                                    uiState.targetTrees,
                                    uiState.progress?.foundTreeIds ?: emptyList(),
                                    uiState.nearbyTree?.id
                                )
                            } else {
                                android.util.Log.w("Rally", "No trees loaded yet, waiting for LaunchedEffect")
                            }

                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
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
                                        cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING
                                        renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
                                        
                                        addOnLocationClickListener {
                                            lastKnownLocation?.let { loc ->
                                                viewModel.updateLocation(LatLng(loc.latitude, loc.longitude))
                                            }
                                        }
                                    }
                                    
                                    map.locationComponent.lastKnownLocation?.let { loc ->
                                        viewModel.updateLocation(LatLng(loc.latitude, loc.longitude))
                                    }
                                    
                                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                                            as android.location.LocationManager
                                    val provider = if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
                                        android.location.LocationManager.GPS_PROVIDER
                                    else
                                        android.location.LocationManager.NETWORK_PROVIDER

                                    val locationListener = android.location.LocationListener { loc ->
                                        viewModel.updateLocation(LatLng(loc.latitude, loc.longitude))
                                    }

                                    try {
                                        locationManager.requestLocationUpdates(provider, 1000L, 0f, locationListener)
                                    } catch (e: SecurityException) {
                                        android.util.Log.e("Rally", "Location permission missing", e)
                                    }

                                    if (uiState.targetTrees.isNotEmpty()) {
                                        val bounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                                        uiState.targetTrees.forEach { tree ->
                                            bounds.include(LatLng(tree.latitude, tree.longitude))
                                        }
                                        map.animateCamera(
                                            org.maplibre.android.camera.CameraUpdateFactory
                                                .newLatLngBounds(bounds.build(), 100)
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("Rally", "Location init error", e)
                                }
                            }
                            
                            map.addOnMapClickListener { point ->
                                android.util.Log.d("Rally", "Map clicked at: $point")
                                val screenPoint = map.projection.toScreenLocation(point)
                                
                                android.util.Log.d("Rally", "Screen Point: x=${screenPoint.x}, y=${screenPoint.y}")

                                val rectF = android.graphics.RectF(
                                    screenPoint.x - 200,
                                    screenPoint.y - 200,
                                    screenPoint.x + 200,
                                    screenPoint.y + 200
                                )
                                
                                android.util.Log.d("Rally", "Querying features in Rect: $rectF")
                                
                                val features = map.queryRenderedFeatures(rectF, "rally-trees")
                                
                                android.util.Log.d("Rally", "Found ${features.size} features near click")

                                if (features.isNotEmpty()) {
                                    val feature = features[0]
                                    
                                    android.util.Log.d("Rally", "Feature found: ID=${feature.id()}")
                                    feature.properties()?.entrySet()?.forEach { 
                                        android.util.Log.d("Rally", "Property: ${it.key} = ${it.value}")
                                    }

                                    val treeId = feature.getStringProperty("id") ?: feature.id()
                                    android.util.Log.d("Rally", "Resolved Tree ID: $treeId")
                                    
                                    android.util.Log.d("Rally", "Target Trees Count: ${uiState.targetTrees.size}")

                                    val tree = uiState.targetTrees.find { it.id == treeId }
                                    if (tree != null) {
                                        android.util.Log.d("Rally", "Tree match found: ${tree.speciesGerman}")
                                        showTreeDetail = tree
                                        viewModel.selectTargetTree(tree.id)
                                    } else {
                                        android.util.Log.e("Rally", "Tree with ID $treeId not found in targetTrees list!")
                                    }
                                    true
                                } else {
                                    android.util.Log.d("Rally", "No tree found at click location")
                                    false
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                view.onPause()
                view.onStop()
                view.onDestroy()
            }
        )
        
        if (uiState.selectedTargetTreeId != null && uiState.userLocation != null) {
            val targetTree = uiState.targetTrees.find { it.id == uiState.selectedTargetTreeId }
            if (targetTree != null) {
                val bearingToTree = viewModel.calculateBearingTo(targetTree)
                val distance = viewModel.calculateDistanceTo(targetTree)
                val deviceBearing = uiState.deviceBearing
                
                val relativeDirection = (bearingToTree - deviceBearing + 360f) % 360f
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Richtung zum Baum",
                                modifier = Modifier
                                    .size(56.dp)
                                    .rotate(relativeDirection),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "${distance.toInt()} m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = targetTree.speciesGerman,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        val directionHint = when {
                            relativeDirection < 22.5f || relativeDirection >= 337.5f -> "Geradeaus!"
                            relativeDirection < 67.5f -> "Leicht rechts"
                            relativeDirection < 112.5f -> "Rechts"
                            relativeDirection < 157.5f -> "Hinter dir rechts"
                            relativeDirection < 202.5f -> "Umdrehen!"
                            relativeDirection < 247.5f -> "Hinter dir links"
                            relativeDirection < 292.5f -> "Links"
                            else -> "Leicht links"
                        }
                        Text(
                            text = directionHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    mapInstance?.let { map ->
                        map.locationComponent.lastKnownLocation?.let { loc ->
                            map.animateCamera(
                                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(loc.latitude, loc.longitude),
                                    16.0
                                )
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, "Mein Standort")
        }
    }

    if (uiState.nearbyTree != null && !uiState.progress?.foundTreeIds?.contains(uiState.nearbyTree!!.id)!!) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            NearbyTreeCard(
                tree = uiState.nearbyTree!!,
                distance = viewModel.calculateDistanceTo(uiState.nearbyTree!!).toInt(),
                onOpenCamera = { onOpenCamera(uiState.nearbyTree!!) }
            )
        }
    }
    
    showTreeDetail?.let { tree ->
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showTreeDetail = null }
        ) {
            TreeDetailSheet(
                tree = tree,
                isFound = uiState.progress?.foundTreeIds?.contains(tree.id) == true,
                userLocation = uiState.userLocation,
                leafPhoto = uiState.leafPhotos[tree.id],
                onMarkFound = {
                    viewModel.markTreeFound(tree.id)
                },
                onTakePhoto = {
                    showTreeDetail = null
                    onOpenCamera(tree)
                },
                onDismiss = { showTreeDetail = null }
            )
        }
    }
}

private fun updateTreeMarkers(
    style: org.maplibre.android.maps.Style,
    trees: List<Tree>,
    foundTreeIds: List<String>,
    nearbyTreeId: String? = null
) {
    android.util.Log.d("Rally", "Updating tree markers: ${trees.size} trees, ${foundTreeIds.size} found")
    
    style.getLayer("rally-trees")?.let { 
        android.util.Log.d("Rally", "Removing old rally-trees layer")
        style.removeLayer(it) 
    }
    style.getSource("rally-trees-source")?.let { 
        android.util.Log.d("Rally", "Removing old rally-trees-source")
        style.removeSource(it) 
    }
    
    if (trees.isEmpty()) {
        android.util.Log.w("Rally", "No trees to display!")
        return
    }

    val features = trees.map { tree ->
        val point = org.maplibre.geojson.Point.fromLngLat(tree.longitude, tree.latitude)
        val feature = org.maplibre.geojson.Feature.fromGeometry(point, null, tree.id)
        feature.addStringProperty("id", tree.id)
        feature.addBooleanProperty("found", foundTreeIds.contains(tree.id))
        feature.addBooleanProperty("nearby", tree.id == nearbyTreeId)
        android.util.Log.d("Rally", "Adding marker for tree ${tree.id} at (${tree.latitude}, ${tree.longitude})")
        feature
    }

    val source = org.maplibre.android.style.sources.GeoJsonSource(
        "rally-trees-source",
        org.maplibre.geojson.FeatureCollection.fromFeatures(features)
    )
    style.addSource(source)
    android.util.Log.d("Rally", "Added source with ${features.size} features")

    val layer = org.maplibre.android.style.layers.CircleLayer("rally-trees", "rally-trees-source")
    layer.setProperties(
        org.maplibre.android.style.layers.PropertyFactory.circleRadius(20f),
        org.maplibre.android.style.layers.PropertyFactory.circleColor(
            org.maplibre.android.style.expressions.Expression.switchCase(
                org.maplibre.android.style.expressions.Expression.get("found"),
                org.maplibre.android.style.expressions.Expression.color(Color(0xFF4CAF50).toArgb()),
                org.maplibre.android.style.expressions.Expression.switchCase(
                    org.maplibre.android.style.expressions.Expression.get("nearby"),
                    org.maplibre.android.style.expressions.Expression.color(Color(0xFFFF9800).toArgb()),
                    org.maplibre.android.style.expressions.Expression.color(Color(0xFF2196F3).toArgb())
                )
            )
        ),
        org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(4f),
        org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(Color.White.toArgb())
    )
    style.addLayer(layer)
    android.util.Log.d("Rally", "Layer added successfully with ${features.size} markers")
}

@Composable
fun TreeDetailSheet(
    tree: Tree,
    isFound: Boolean,
    userLocation: LatLng?,
    leafPhoto: android.graphics.Bitmap?,
    onMarkFound: () -> Unit,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val distance = remember(userLocation, tree) {
        if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                tree.latitude, tree.longitude,
                results
            )
            results[0].roundToInt()
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            tree.speciesGerman,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        tree.speciesScientific?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        distance?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$it Meter entfernt")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isFound) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Baum gefunden! ✓",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (leafPhoto != null) {
                        Text("Dein Blatt-Foto:", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = leafPhoto.asImageBitmap(),
                            contentDescription = "Blatt Foto",
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                        )
                    } else {
                        Button(
                            onClick = onTakePhoto,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Blatt fotografieren")
                        }
                    }
                }
            }
        } else {
            val canMarkFound = distance != null && distance < 50
            
            Button(
                onClick = onMarkFound,
                modifier = Modifier.fillMaxWidth(),
                enabled = canMarkFound
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (canMarkFound) "Als gefunden markieren"
                    else "Komm näher (${distance ?: "?"}m)"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Schließen")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TreeListTab(viewModel: RallyViewModel, uiState: RallyUiState) {
    val foundIds = uiState.progress?.foundTreeIds ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.targetTrees) { tree ->
            TreeListItem(
                tree = tree,
                isFound = foundIds.contains(tree.id),
                distance = calculateDistance(uiState.userLocation, tree),
                onClick = { viewModel.selectTargetTree(tree.id) }
            )
        }
    }
}

@Composable
fun TreeListItem(
    tree: Tree,
    isFound: Boolean,
    distance: Int?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isFound)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFound) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFound) Icons.Default.CheckCircle else Icons.Default.Place,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tree.speciesGerman,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                tree.speciesScientific?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                distance?.let {
                    Text(
                        "$it m entfernt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isFound) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

private fun calculateDistance(userLocation: LatLng?, tree: Tree): Int? {
    if (userLocation == null) return null
    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude, userLocation.longitude,
        tree.latitude, tree.longitude,
        results
    )
    return results[0].roundToInt()
}


@Composable
fun NearbyTreeCard(
    tree: Tree,
    distance: Int,
    onOpenCamera: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, null, tint = Color(0xFFFF9800))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Baum in der Nähe!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Park, 
                    null, 
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tree.speciesGerman,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$distance m entfernt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onOpenCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fotografieren")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeCameraScreen(
    tree: Tree,
    onBack: () -> Unit,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit
) {
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Baum fotografieren") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Abbrechen")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Park,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
                Text(
                    tree.speciesGerman,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                tree.speciesScientific?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider()
            
            if (capturedBitmap != null) {
                Text(
                    "Wie sieht dein Foto aus?",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { capturedBitmap = null; cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Neu aufnehmen")
                    }
                    Button(
                        onClick = { onPhotoCaptured(capturedBitmap!!) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Bestätigen")
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CameraEnhance,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Fotografiere den Baum oder ein Blatt,\num ihn für dein Herbarium zu sammeln.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kamera öffnen", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

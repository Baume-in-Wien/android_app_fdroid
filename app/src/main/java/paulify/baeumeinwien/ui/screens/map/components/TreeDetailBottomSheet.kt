package paulify.baeumeinwien.ui.screens.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.Tree
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun TreeDetailBottomSheet(
    tree: Tree,
    notes: List<paulify.baeumeinwien.data.local.TreeNote> = emptyList(),
    photos: List<paulify.baeumeinwien.data.local.TreePhoto> = emptyList(),
    showWikipedia: Boolean = true,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onAddNote: (String) -> Unit = {},
    onAddPhoto: (String) -> Unit = {},
    onDeleteNote: (paulify.baeumeinwien.data.local.TreeNote) -> Unit = {},
    onDeletePhoto: (paulify.baeumeinwien.data.local.TreePhoto) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.8f else 1f,
        animationSpec = tween(durationMillis = 100)
    )
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (showWikipedia) {
        listOf(stringResource(R.string.nav_info), "Wiki", stringResource(R.string.tree_notes), stringResource(R.string.tree_take_photo).split(" ").last())
    } else {
        listOf(stringResource(R.string.nav_info), stringResource(R.string.tree_notes), stringResource(R.string.tree_take_photo).split(" ").last())
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tree.speciesGerman,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { 
                    isPressed.value = true
                    onToggleFavorite()
                    coroutineScope.launch {
                        delay(100)
                        isPressed.value = false
                    }
                }
            ) {
                Icon(
                    imageVector = if (tree.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (tree.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(scale)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { 
                        Text(
                            text = title,
                            maxLines = 1
                        ) 
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (showWikipedia) {
                when (selectedTab) {
                    0 -> TreeInfoTab(tree)
                    1 -> WikipediaTab(tree = tree)
                    2 -> TreeNotesTab(notes = notes, onAddNote = onAddNote, onDeleteNote = onDeleteNote)
                    3 -> TreePhotosTab(photos = photos, onAddPhoto = onAddPhoto, onDeletePhoto = onDeletePhoto)
                }
            } else {
                when (selectedTab) {
                    0 -> TreeInfoTab(tree)
                    1 -> TreeNotesTab(notes = notes, onAddNote = onAddNote, onDeleteNote = onDeleteNote)
                    2 -> TreePhotosTab(photos = photos, onAddPhoto = onAddPhoto, onDeletePhoto = onDeletePhoto)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
fun TreeInfoTab(tree: Tree) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    tree.baumId?.let { TreeDetailRow(stringResource(R.string.tree_number), it) }
                    tree.street?.let { TreeDetailRow(stringResource(R.string.tree_street), it) }
                    tree.district?.let { TreeDetailRow(stringResource(R.string.tree_district), it.toString()) }
                    tree.plantYear?.let { TreeDetailRow(stringResource(R.string.tree_plant_year), it.toString()) }
                    tree.height?.let { TreeDetailRow(stringResource(R.string.tree_height), "${it}m") }
                    tree.trunkCircumference?.let { TreeDetailRow(stringResource(R.string.tree_trunk_circumference), "${it}cm") }
                    tree.crownDiameter?.let { TreeDetailRow(stringResource(R.string.tree_crown_diameter), "${it}m") }
                }
            }
        }
    }
}

@Composable
fun TreeNotesTab(
    notes: List<paulify.baeumeinwien.data.local.TreeNote>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (paulify.baeumeinwien.data.local.TreeNote) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    
    Column {
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text(stringResource(R.string.tree_add_note)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (noteText.isNotEmpty()) {
                    IconButton(onClick = {
                        onAddNote(noteText)
                        noteText = ""
                    }) {
                        Icon(Icons.Default.Send, contentDescription = stringResource(R.string.save))
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(note.content, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(note.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteNote(note) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        if (notes.isEmpty()) {
            Text(
                stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun TreeDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun TreePhotosTab(
    photos: List<paulify.baeumeinwien.data.local.TreePhoto>,
    onAddPhoto: (String) -> Unit,
    onDeletePhoto: (paulify.baeumeinwien.data.local.TreePhoto) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cameraPermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                android.util.Log.w("TreePhotosTab", "Could not take persistable permission", e)
            }
            onAddPhoto(it.toString())
        }
    }
    
    val cameraUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { onAddPhoto(it.toString()) }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Galerie")
            }
            
            FilledTonalButton(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        val photoFile = java.io.File(
                            context.cacheDir,
                            "tree_photo_${System.currentTimeMillis()}.jpg"
                        )
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        cameraUri.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kamera")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos, key = { it.id }) { photo ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = android.net.Uri.parse(photo.uri),
                            contentDescription = "Foto",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Foto",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(photo.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(onClick = { onDeletePhoto(photo) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Löschen",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        if (photos.isEmpty()) {
            Text(
                "Keine Fotos vorhanden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

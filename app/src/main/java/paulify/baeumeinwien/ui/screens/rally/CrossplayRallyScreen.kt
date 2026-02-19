package paulify.baeumeinwien.ui.screens.rally

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.data.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossplayRallyScreen(
    viewModel: CrossplayRallyViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(uiState.rally?.name ?: "Crossplay Rallye") 
                },
                actions = {
                    IconButton(onClick = { viewModel.finishRally() }) {
                        Icon(Icons.Default.Stop, "Beenden")
                    }
                    IconButton(onClick = { viewModel.leaveRally() }) {
                        Icon(Icons.Default.ExitToApp, "Verlassen")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.rally == null) {
            JoinOrCreateRallyScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        } else {
            RallyActiveScreen(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
        
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { /* Dismiss */ }) {
                        Text("OK")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun JoinOrCreateRallyScreen(
    viewModel: CrossplayRallyViewModel,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            "Crossplay Rallye",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Spiele mit Android & iOS Freunden",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null, Modifier.padding(end = 8.dp))
            Text("Neue Rallye erstellen")
        }
        
        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Login, null, Modifier.padding(end = 8.dp))
            Text("Rallye beitreten")
        }
        
        OutlinedButton(
            onClick = { viewModel.searchPublicRallies() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, null, Modifier.padding(end = 8.dp))
            Text("Öffentliche Rallyes")
        }
        
        val uiState by viewModel.uiState.collectAsState()
        uiState.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
    
    val favoriteTrees by viewModel.favoriteTrees.collectAsState()

    if (showCreateDialog) {
        CreateRallyDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, mode, treeIds ->
                viewModel.createRally(
                    name = name,
                    description = description,
                    mode = mode,
                    targetTreeIds = treeIds
                )
                showCreateDialog = false
            },
            favoriteTrees = favoriteTrees
        )
    }
    
    if (showJoinDialog) {
        JoinRallyDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { code, displayName ->
                viewModel.joinRally(code, displayName)
                showJoinDialog = false
            }
        )
    }
}

@Composable
fun RallyActiveScreen(
    uiState: RallyUiStateData,
    viewModel: CrossplayRallyViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = modifier.fillMaxSize()) {
        RallyInfoCard(uiState.rally!!)
        
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Teilnehmer (${uiState.participants.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Bäume (${uiState.collections.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Statistik") }
            )
        }
        
        when (selectedTab) {
            0 -> ParticipantsTab(uiState.participants)
            1 -> CollectionsTab(uiState.collections)
            2 -> StatisticsTab(uiState.statistics)
        }
    }
}

@Composable
fun RallyInfoCard(rally: CrossplayRally) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        rally.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        rally.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        rally.code,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip("Mode: ${rally.mode}")
                StatusChip("Status: ${rally.status}")
                if (rally.creator_platform.isNotEmpty()) {
                    StatusChip("Creator: ${rally.creator_platform}")
                }
            }
        }
    }
}

@Composable
fun ParticipantsTab(participants: List<RallyParticipant>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(participants) { participant ->
            ParticipantCard(participant)
        }
    }
}

@Composable
fun ParticipantCard(participant: RallyParticipant) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (participant.platform == "android") 
                        Icons.Default.Android 
                    else 
                        Icons.Default.Devices,
                    contentDescription = participant.platform,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        participant.display_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${participant.species_collected} Arten · ${participant.trees_scanned} Bäume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (participant.is_active) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                ) {}
            }
        }
    }
}

@Composable
fun CollectionsTab(collections: List<TreeCollection>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(collections) { collection ->
            CollectionCard(collection)
        }
    }
}

@Composable
fun CollectionCard(collection: TreeCollection) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        collection.species,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Baum-ID: ${collection.tree_id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    collection.notes?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun StatisticsTab(statistics: RallyStatistics?) {
    if (statistics == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatCard(
                title = "Gesamt-Statistik",
                items = listOf(
                    "Teilnehmer" to statistics.total_participants.toString(),
                    "Gesammelte Bäume" to statistics.total_trees_collected.toString(),
                    "Einzigartige Arten" to statistics.total_unique_species.toString()
                )
            )
        }
        
        item {
            Text(
                "🏆 Top Sammler",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(statistics.top_collectors) { collector ->
            TopCollectorCard(collector)
        }
    }
}

@Composable
fun StatCard(title: String, items: List<Pair<String, String>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TopCollectorCard(collector: TopCollector) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    if (collector.platform == "android") 
                        Icons.Default.Android 
                    else 
                        Icons.Default.Devices,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        collector.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${collector.species_count} Arten · ${collector.tree_count} Bäume",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun CreateRallyDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, mode: RallyMode, treeIds: List<String>) -> Unit,
    favoriteTrees: List<Tree>
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(RallyMode.STUDENT) }
    var selectedTreeIds by remember { mutableStateOf(setOf<String>()) }
    var showModeSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Rallye erstellen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 450.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    "Modus",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(RallyMode.STUDENT, RallyMode.TEACHER, RallyMode.SOLO).forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = {
                                Text(
                                    when (mode) {
                                        RallyMode.STUDENT -> "Schüler:innen"
                                        RallyMode.TEACHER -> "Lehrer:innen"
                                        RallyMode.SOLO -> "Solo"
                                        else -> mode.name
                                    }
                                )
                            },
                            leadingIcon = if (selectedMode == mode) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                
                if (favoriteTrees.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Bäume auswählen (${selectedTreeIds.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(favoriteTrees) { tree ->
                            val isSelected = selectedTreeIds.contains(tree.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedTreeIds = if (checked) {
                                            selectedTreeIds + tree.id
                                        } else {
                                            selectedTreeIds - tree.id
                                        }
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(tree.speciesGerman, style = MaterialTheme.typography.bodyMedium)
                                    Text(tree.street ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Keine Favoriten markiert. Markiere Bäume auf der Karte, um sie hier auszuwählen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, selectedMode, selectedTreeIds.toList()) },
                enabled = name.isNotBlank()
            ) {
                Text("Erstellen")
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
fun JoinRallyDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, displayName: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rallye beitreten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { input -> 
                        code = input.filter { it.isLetterOrDigit() }.uppercase().take(6) 
                    },
                    label = { Text("6-stelliger Code") },
                    singleLine = true,
                    placeholder = { Text("AB12CD") }
                )
                
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Dein Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code, displayName) },
                enabled = code.length == 6 && displayName.isNotBlank()
            ) {
                Text("Beitreten")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

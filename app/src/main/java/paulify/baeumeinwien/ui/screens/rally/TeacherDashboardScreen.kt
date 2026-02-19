package paulify.baeumeinwien.ui.screens.rally

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import paulify.baeumeinwien.data.domain.*
import paulify.baeumeinwien.data.repository.CrossplayRallyRepository
import paulify.baeumeinwien.data.repository.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    rallyName: String,
    rallyCode: String,
    participants: List<RallyParticipant>,
    collections: List<TreeCollection>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Lehrer:innen-Dashboard")
                        Text(
                            "$rallyName • Code: $rallyCode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${participants.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Teilnehmer:innen")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${collections.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text("Bäume")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${collections.map { it.species }.distinct().size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00BCD4)
                                )
                                Text("Arten")
                            }
                        }
                    }
                }
            }
            
            if (participants.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Noch keine Teilnehmer:innen",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Teile den Code $rallyCode mit deiner Gruppe",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(participants) { participant ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (participant.platform == "ios") Icons.Default.PhoneIphone else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    participant.display_name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${participant.species_collected} Arten • ${participant.trees_scanned} Bäume",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (participant.is_active) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    rally: CrossplayRally,
    repository: CrossplayRallyRepository,
    onBack: () -> Unit
) {
    var participants by remember { mutableStateOf<List<RallyParticipant>>(emptyList()) }
    var collections by remember { mutableStateOf<List<TreeCollection>>(emptyList()) }
    var statistics by remember { mutableStateOf<RallyStatistics?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showEndRallyDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    LaunchedEffect(rally.id) {
        while (true) {
            when (val result = repository.getRallyParticipants(rally.id)) {
                is Result.Success -> participants = result.data
                else -> {}
            }
            
            when (val result = repository.getRallyCollections(rally.id)) {
                is Result.Success -> collections = result.data
                else -> {}
            }
            
            when (val result = repository.getRallyStatistics(rally.id)) {
                is Result.Success -> statistics = result.data
                else -> {}
            }
            
            isLoading = false
            delay(10_000)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lehrer:innen-Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            when (val result = repository.getRallyParticipants(rally.id)) {
                                is Result.Success -> participants = result.data
                                else -> {}
                            }
                            when (val result = repository.getRallyCollections(rally.id)) {
                                is Result.Success -> collections = result.data
                                else -> {}
                            }
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                    
                    IconButton(onClick = { showEndRallyDialog = true }) {
                        Icon(Icons.Default.Stop, contentDescription = "Rallye beenden")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            RallyInfoHeader(
                rally = rally,
                participantCount = participants.size,
                collectionCount = collections.count(),
                uniqueSpeciesCount = statistics?.total_unique_species ?: collections.map { it.species }.distinct().size
            )
            
            TabRow(
                selectedTabIndex = pagerState.currentPage
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Teilnehmer:innen (${participants.size})") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Bäume (${collections.size})") }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text("Statistik") }
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> ParticipantsTabContent(
                            participants = participants.sortedByDescending { it.species_collected },
                            collections = collections,
                            rallyCode = rally.code
                        )
                        1 -> CollectionsTabContent(collections = collections)
                        2 -> StatisticsTabContent(
                            statistics = statistics,
                            participants = participants,
                            collections = collections
                        )
                    }
                }
            }
        }
    }
    
    if (showEndRallyDialog) {
        AlertDialog(
            onDismissRequest = { showEndRallyDialog = false },
            title = { Text("Rallye beenden?") },
            text = { Text("Alle Teilnehmer:innen werden benachrichtigt und können keine Bäume mehr sammeln.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.finishRally(rally.id)
                            showEndRallyDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Beenden")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndRallyDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun RallyInfoHeader(
    rally: CrossplayRally,
    participantCount: Int,
    collectionCount: Int,
    uniqueSpeciesCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = rally.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (rally.description.isNotEmpty()) {
                        Text(
                            text = rally.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CODE",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = rally.code,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatBadge(
                    value = participantCount.toString(),
                    label = "Teilnehmer:innen",
                    icon = Icons.Default.Group,
                    color = MaterialTheme.colorScheme.primary
                )
                QuickStatBadge(
                    value = collectionCount.toString(),
                    label = "Bäume",
                    icon = Icons.Default.Forest,
                    color = Color(0xFF4CAF50)
                )
                QuickStatBadge(
                    value = uniqueSpeciesCount.toString(),
                    label = "Arten",
                    icon = Icons.Default.Eco,
                    color = Color(0xFF00BCD4)
                )
            }
        }
    }
}

@Composable
private fun QuickStatBadge(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ParticipantsTabContent(
    participants: List<RallyParticipant>,
    collections: List<TreeCollection>,
    rallyCode: String
) {
    if (participants.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Text(
                    text = "Noch keine Teilnehmer:innen",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Teile den Code $rallyCode,\num Schüler:innen einzuladen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(participants.withIndex().toList()) { (index, participant) ->
                ParticipantCard(
                    participant = participant,
                    rank = index + 1,
                    collectionsCount = collections.count { it.participant_id == participant.id }
                )
            }
        }
    }
}

@Composable
private fun ParticipantCard(
    participant: RallyParticipant,
    rank: Int,
    collectionsCount: Int
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(rankColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                imageVector = if (participant.platform == "ios") Icons.Default.PhoneIphone else Icons.Default.PhoneAndroid,
                contentDescription = participant.platform,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.display_name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "${participant.species_collected} Arten",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${participant.trees_scanned} Bäume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (participant.is_active) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        text = "Aktiv",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionsTabContent(collections: List<TreeCollection>) {
    if (collections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forest,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50).copy(alpha = 0.5f)
                )
                Text(
                    text = "Noch keine Bäume gesammelt",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Die Teilnehmer:innen sammeln gerade...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val grouped = collections.groupBy { it.species }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(grouped.keys.toList().sortedByDescending { grouped[it]?.size ?: 0 }) { species ->
                val speciesCollections = grouped[species] ?: emptyList()
                SpeciesGroupCard(species = species, count = speciesCollections.size)
            }
        }
    }
}

@Composable
private fun SpeciesGroupCard(species: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Forest,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = species,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${count}×",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatisticsTabContent(
    statistics: RallyStatistics?,
    participants: List<RallyParticipant>,
    collections: List<TreeCollection>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 Gesamt-Statistik",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatRow("Teilnehmer:innen", "${statistics?.total_participants ?: participants.size}")
                    StatRow("Gesammelte Bäume", "${statistics?.total_trees_collected ?: collections.size}")
                    StatRow("Einzigartige Arten", "${statistics?.total_unique_species ?: collections.map { it.species }.distinct().size}")
                    
                    val iosCount = participants.count { it.platform == "ios" }
                    val androidCount = participants.count { it.platform == "android" }
                    if (iosCount > 0 || androidCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRow("iOS Geräte", "$iosCount")
                        StatRow("Android Geräte", "$androidCount")
                    }
                }
            }
        }
        
        if (statistics?.top_collectors?.isNotEmpty() == true) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏆 Top Sammler:innen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        statistics.top_collectors.forEachIndexed { index, collector ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> "🥇"
                                        1 -> "🥈"
                                        2 -> "🥉"
                                        else -> "${index + 1}."
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = collector.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${collector.species_count} Arten",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (statistics?.most_collected_species?.isNotEmpty() == true) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🌳 Häufigste Arten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        statistics.most_collected_species.take(5).forEach { speciesCount ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = speciesCount.species,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${speciesCount.count}×",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

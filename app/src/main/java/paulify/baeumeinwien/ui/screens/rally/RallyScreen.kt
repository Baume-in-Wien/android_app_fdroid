package paulify.baeumeinwien.ui.screens.rally

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import paulify.baeumeinwien.R
import paulify.baeumeinwien.ui.screens.rally.components.AchievementUnlockedDialog
import paulify.baeumeinwien.data.domain.Tree

@Composable
fun RallyScreen(
    viewModel: RallyViewModel,
    onNavigateToPlay: (String, String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSoloExplorer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.checkSession(context)
    }

    LaunchedEffect(uiState.currentRally) {
        uiState.currentRally?.let { rally ->
            if (uiState.progress?.studentName != null && !uiState.isLoading) {
                onNavigateToPlay(rally.code, uiState.progress!!.studentName)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.rally_title).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.explorer_nearby_trees),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.userLocation != null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.userLocation != null) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                contentDescription = stringResource(R.string.map_location),
                                tint = if (uiState.userLocation != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (uiState.userLocation != null) "GPS" else stringResource(R.string.error_location),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.userLocation != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            
            item {
                ExpressiveCard(
                    title = stringResource(R.string.rally_solo_discovery),
                    subtitle = stringResource(R.string.explorer_start),
                    icon = Icons.Default.Public,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToSoloExplorer,
                    modifier = Modifier.height(160.dp)
                )
            }

            item {
                Row(modifier = Modifier.height(180.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                     ExpressiveCard(
                        title = stringResource(R.string.rally_join),
                        subtitle = stringResource(R.string.rally_enter_code),
                        icon = Icons.Default.School,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    
                    ExpressiveCard(
                        title = stringResource(R.string.rally_create),
                        subtitle = stringResource(R.string.rally_start),
                        icon = Icons.Default.Edit,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onNavigateToCreate,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            
            if (uiState.isAdmin && uiState.currentRally != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.rally_ranking).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                
                items(uiState.allProgress) { progress ->
                     Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                     ) {
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier.padding(16.dp)
                         ) {
                             Text(
                                 progress.studentName, 
                                 style = MaterialTheme.typography.titleLarge, 
                                 fontWeight = FontWeight.Bold,
                                 modifier = Modifier.weight(1f)
                             )
                             Text(
                                 "${progress.score}", 
                                 style = MaterialTheme.typography.displaySmall, 
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colorScheme.primary
                             )
                         }
                     }
                }
            }
        }
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text(stringResource(R.string.rally_join).uppercase(), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = joinCode.uppercase(),
                        onValueChange = { joinCode = it.take(6) },
                        label = { Text(stringResource(R.string.rally_code).uppercase()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text(stringResource(R.string.rally_your_name).uppercase()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCode.length == 6 && studentName.isNotBlank()) {
                            viewModel.joinRally(context, joinCode.uppercase(), studentName)
                            showJoinDialog = false
                        }
                    },
                    enabled = joinCode.length == 6 && studentName.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.rally_start).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun ExpressiveCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                 Surface(
                     shape = androidx.compose.foundation.shape.CircleShape,
                     color = contentColor.copy(alpha = 0.15f),
                     modifier = Modifier.size(48.dp)
                 ) {
                     Box(
                         modifier = Modifier.fillMaxSize(),
                         contentAlignment = Alignment.Center
                     ) {
                         Icon(
                             imageVector = icon, 
                             contentDescription = null, 
                             tint = contentColor,
                             modifier = Modifier.size(24.dp)
                         )
                     }
                 }
                 
                 Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                     contentDescription = null, 
                     tint = contentColor.copy(alpha = 0.5f),
                     modifier = Modifier.size(24.dp)
                 )
            }
            
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    lineHeight = 24.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}

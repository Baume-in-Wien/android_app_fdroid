package paulify.baeumeinwien.ui.screens.rally

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.Tree

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RallyCreationScreen(
    viewModel: RallyViewModel,
    favoriteTrees: List<Tree>,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    var rallyName by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("") }
    var selectedTreeIds by remember { mutableStateOf(setOf<String>()) }
    var radius by remember { mutableStateOf(100f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rally_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.currentRally != null && uiState.isAdmin) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.rally_create_success),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.rally_your_code),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = uiState.currentRally?.code ?: "",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = { 
                            uiState.currentRally?.code?.let { 
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(R.string.rally_share_instruction),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.rally_done))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = rallyName,
                        onValueChange = { rallyName = it },
                        label = { Text(stringResource(R.string.rally_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = creatorName,
                        onValueChange = { creatorName = it },
                        label = { Text(stringResource(R.string.rally_creator_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Text(
                        stringResource(R.string.rally_radius_label, radius.toInt()),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 50f..500f,
                        steps = 9
                    )
                }
                
                item {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.rally_select_trees), style = MaterialTheme.typography.titleMedium)
                    if (favoriteTrees.isEmpty()) {
                        Text(
                            stringResource(R.string.rally_no_favorites_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                items(favoriteTrees) { tree ->
                    val isSelected = selectedTreeIds.contains(tree.id)
                    ListItem(
                        headlineContent = { Text(tree.speciesGerman) },
                        supportingContent = { Text(tree.street ?: stringResource(R.string.rally_unknown_street)) },
                        trailingContent = {
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
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.createRally(
                                name = rallyName,
                                treeIds = selectedTreeIds.toList(),
                                radius = radius.toDouble(),
                                creator = creatorName
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rallyName.isNotBlank() && creatorName.isNotBlank() && selectedTreeIds.isNotEmpty()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(R.string.rally_create_button))
                        }
                    }
                }
            }
        }
    }
}

package paulify.baeumeinwien.ui.screens.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.Tree

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveTreeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<Tree>,
    onTreeClick: (Tree) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    val popularSpecies = remember {
        listOf(
            Triple("Linde", "Linde", "Beliebter Stadtbaum"),
            Triple("Ahorn", "Ahorn", "Wunderschöne Herbstfärbung"), 
            Triple("Eiche", "Eiche", "Majestätisch & langlebig"),
            Triple("Kastanie", "Kastanie", "Schöne Blüten im Frühling"),
            Triple("Platane", "Platane", "Typischer Alleebaum")
        )
    }
    val cornerRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (expanded) 28.dp else 28.dp,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "cornerRadius"
    )
    
    Surface(
        modifier = modifier
            .semantics { isTraversalGroup = true; traversalIndex = 0f }
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        shadowElevation = if (expanded) 6.dp else 0.dp
    ) {
        SearchBar(
            modifier = Modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { 
                    onSearch(query)
                    expanded = false 
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = { 
                    Text(
                        stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyLarge
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = stringResource(R.string.search_hint),
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                }
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (searchResults.isNotEmpty() && query.length >= 2) {
                item {
                    Text(
                        text = stringResource(R.string.search_results_count, searchResults.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                items(searchResults.take(15)) { tree ->
                    TreeSearchResultItem(
                        tree = tree,
                        onClick = {
                            onTreeClick(tree)
                            expanded = false
                        }
                    )
                }
            } else if (query.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.search_popular_species),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                items(popularSpecies) { (searchTerm, displayName, description) ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        supportingContent = { 
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Forest,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable { 
                                onQueryChange(searchTerm)
                                onSearch(searchTerm)
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.search_popular_places),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                item {
                    ListItem(
                        headlineContent = { Text("Ringstraße", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Historische Prachtstraße") },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable { 
                                onQueryChange("Ringstraße")
                                onSearch("Ringstraße")
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
                
                item {
                    ListItem(
                        headlineContent = { Text("Prater", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Grüne Oase der Stadt") },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable { 
                                onQueryChange("Prater")
                                onSearch("Prater")
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            } else if (query.length >= 2 && searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text(
                                text = stringResource(R.string.search_no_results_for, query),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.search_min_chars),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TreeSearchResultItem(
    tree: Tree,
    onClick: () -> Unit
) {
    val noStreetText = stringResource(R.string.search_no_street)
    
    ListItem(
        headlineContent = { 
            Text(
                text = tree.speciesGerman,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) 
        },
        supportingContent = {
            Column {
                tree.speciesScientific?.let { scientific ->
                    Text(
                        text = scientific,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tree.street ?: noStreetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (tree.isFavorite) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (tree.isFavorite) Icons.Default.Star else Icons.Default.Forest,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (tree.isFavorite) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        trailingContent = {
            tree.district?.let { district ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = district.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

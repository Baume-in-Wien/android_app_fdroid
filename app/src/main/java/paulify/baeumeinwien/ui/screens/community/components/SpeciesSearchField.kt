package paulify.baeumeinwien.ui.screens.community.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.data.domain.TreeSpecies

@Composable
fun SpeciesSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<TreeSpecies>,
    isSearching: Boolean,
    onSpeciesSelected: (TreeSpecies) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Baumart suchen...") },
            placeholder = { Text("z.B. Ahorn, Linde, Quercus...") },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(results) { species ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = species.nameGerman,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = species.nameScientific?.let { scientific ->
                            {
                                Text(
                                    text = scientific,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Park,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onSpeciesSelected(species) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

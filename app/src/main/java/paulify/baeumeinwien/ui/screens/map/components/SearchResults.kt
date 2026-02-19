package paulify.baeumeinwien.ui.screens.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.data.domain.Tree

@Composable
fun SearchResults(
    results: List<Tree>,
    onResultClick: (Tree) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(results) { tree ->
            ListItem(
                headlineContent = { Text(tree.speciesGerman) },
                supportingContent = { Text(tree.street ?: "Keine Straße") },
                modifier = Modifier.clickable { onResultClick(tree) }
            )
        }
    }
}

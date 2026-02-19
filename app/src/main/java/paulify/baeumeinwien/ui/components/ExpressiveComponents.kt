package paulify.baeumeinwien.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp


@Composable
fun FilterSplitButton(
    leadingText: String,
    modifier: Modifier = Modifier,
    onClickLeading: () -> Unit,
    onClickTrailing: () -> Unit,
    isExpanded: Boolean
) {
    Row(
        modifier = modifier.height(56.dp)
    ) {
        Button(
            onClick = onClickLeading,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(leadingText, style = MaterialTheme.typography.labelLarge)
        }
        
        Spacer(modifier = Modifier.width(2.dp))
        
        Button(
            onClick = onClickTrailing,
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp),
            contentPadding = PaddingValues(0.dp),
            shape = MaterialTheme.shapes.extraLarge,
             colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "Arrow Rotation"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Optionen",
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

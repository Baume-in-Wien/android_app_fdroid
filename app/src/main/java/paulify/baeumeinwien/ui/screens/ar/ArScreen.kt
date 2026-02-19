package paulify.baeumeinwien.ui.screens.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * F-Droid-Variante: AR ist in dieser Version nicht verfuegbar.
 *
 * ARCore (com.google.ar:core) ist eine propriataere Google-Bibliothek und
 * darf laut F-Droid-Richtlinien nicht enthalten sein.
 *
 * Alle anderen Features sind vollstaendig verfuegbar:
 *   - Interaktive Karte (MapLibre, 100% Open Source)
 *   - Baum-Suche und Detailansichten
 *   - Blattscan mit ONNX-Modell
 *   - Favoriten, Notizen, Fotos
 *   - Rally-Spiel
 *   - Community-Baeume hinzufuegen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArScreen(
    viewModel: ArViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR-Ansicht") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "AR nicht verfuegbar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Die Augmented-Reality-Ansicht ist in der F-Droid-Version " +
                            "nicht enthalten, da sie ARCore von Google benoetigt.\n\n" +
                            "Alle anderen Funktionen sind vollstaendig verfuegbar: " +
                            "Karte, Baum-Suche, Blattscan, Favoriten und Rally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

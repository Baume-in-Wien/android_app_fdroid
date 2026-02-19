package paulify.baeumeinwien.ui.screens.community.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.ui.screens.community.AddTreeViewModel.GpsStatus

@Composable
fun GpsAccuracyIndicator(
    gpsStatus: GpsStatus,
    accuracy: Float?,
    modifier: Modifier = Modifier
) {
    val color = when (gpsStatus) {
        GpsStatus.WAITING -> Color.Gray
        GpsStatus.POOR -> Color(0xFFF44336)
        GpsStatus.MODERATE -> Color(0xFFFF9800)
        GpsStatus.GOOD -> Color(0xFF8BC34A)
        GpsStatus.EXCELLENT -> Color(0xFF4CAF50)
    }

    val statusText = when (gpsStatus) {
        GpsStatus.WAITING -> "Warte auf GPS-Signal..."
        GpsStatus.POOR -> "Schlechte Genauigkeit"
        GpsStatus.MODERATE -> "Mittlere Genauigkeit"
        GpsStatus.GOOD -> "Gute Genauigkeit"
        GpsStatus.EXCELLENT -> "Ausgezeichnete Genauigkeit!"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (gpsStatus == GpsStatus.WAITING) 1500 else 1000
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = color.copy(alpha = pulseAlpha),
                    radius = size.minDimension / 2 * pulseScale
                )
            }
            Canvas(modifier = Modifier.size(60.dp)) {
                drawCircle(color = color)
            }

            accuracy?.let {
                Text(
                    text = "${it.toInt()}m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )

        accuracy?.let {
            Text(
                text = "GPS-Genauigkeit: %.1f m".format(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

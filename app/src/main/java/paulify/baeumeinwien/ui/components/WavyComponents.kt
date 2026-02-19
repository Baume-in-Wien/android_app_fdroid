package paulify.baeumeinwien.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LinearWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 4.dp,
    frequency: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.height(amplitude * 4).fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val midHeight = height / 2
        
        val trackPath = Path()
        val waveLen = 20.dp.toPx()
        
        for (x in 0..width.toInt() step 5) {
            val y = midHeight + sin(x * (2 * PI / waveLen)).toFloat() * amplitude.toPx()
            if (x == 0) trackPath.moveTo(x.toFloat(), y) else trackPath.lineTo(x.toFloat(), y)
        }
        
        drawPath(
            path = trackPath,
            color = trackColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )

        
        val progressWidth = width * progress
        val progressPath = Path()
        
        
        var firstPoint = true
        for (x in 0..progressWidth.toInt() step 2) {
            val currentPhase = phase
            val y = midHeight + sin(x * (2 * PI / waveLen)).toFloat() * amplitude.toPx()
            if (firstPoint) {
                progressPath.moveTo(x.toFloat(), y)
                firstPoint = false
            } else {
                progressPath.lineTo(x.toFloat(), y)
            }
        }
        
        drawPath(
            path = progressPath,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

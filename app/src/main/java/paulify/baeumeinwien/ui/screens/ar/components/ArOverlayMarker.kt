package paulify.baeumeinwien.ui.screens.ar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import paulify.baeumeinwien.data.domain.Tree
import kotlin.math.roundToInt

@Composable
fun ArOverlayMarker(
    tree: Tree,
    distance: Float,
    screenX: Float,
    screenY: Float,
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceScale = when {
        distance < 10f -> 1.2f
        distance < 25f -> 1.0f
        distance < 50f -> 0.85f
        distance < 75f -> 0.7f
        else -> 0.6f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) distanceScale else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "markerScale"
    )
    
    val markerColor = getTreeColor(tree)
    
    val alpha = when {
        distance < 20f -> 1f
        distance < 50f -> 0.9f
        distance < 80f -> 0.75f
        else -> 0.6f
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 0.5f)
    ) {
        Box(
            modifier = modifier
                .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                .scale(animatedScale)
                .alpha(alpha)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = markerColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.background(
                            Brush.radialGradient(
                                colors = listOf(
                                    markerColor.copy(alpha = 0.9f),
                                    markerColor
                                )
                            )
                        )
                    ) {
                        Icon(
                            imageVector = if (tree.isFavorite) Icons.Default.Favorite else Icons.Default.Forest,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(markerColor, markerColor.copy(alpha = 0.3f))
                            )
                        )
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tree.speciesGerman,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = markerColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDistance(distance),
                                style = MaterialTheme.typography.bodySmall,
                                color = markerColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArOverlayMarkerCompact(
    tree: Tree,
    distance: Float,
    screenX: Float,
    screenY: Float,
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val markerColor = getTreeColor(tree)
    val size = when {
        distance < 30f -> 24.dp
        distance < 60f -> 18.dp
        else -> 14.dp
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = modifier
                .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(size + 8.dp)
            ) {}
            
            Surface(
                shape = CircleShape,
                color = markerColor,
                shadowElevation = 4.dp,
                modifier = Modifier.size(size)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (size >= 18.dp) {
                        Icon(
                            imageVector = Icons.Default.Forest,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size - 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getTreeColor(tree: Tree): Color {
    if (tree.isFavorite) {
        return Color(0xFF4CAF50)
    }
    
    val hash = tree.speciesGerman.hashCode()
    return when (kotlin.math.abs(hash) % 8) {
        0 -> Color(0xFF66BB6A)
        1 -> Color(0xFFFF7043)
        2 -> Color(0xFF42A5F5)
        3 -> Color(0xFFAB47BC)
        4 -> Color(0xFFFFCA28)
        5 -> Color(0xFF26A69A)
        6 -> Color(0xFFEF5350)
        else -> Color(0xFF8D6E63)
    }
}

private fun formatDistance(meters: Float): String {
    return when {
        meters < 10 -> "${meters.roundToInt()}m"
        meters < 100 -> "${(meters / 5).roundToInt() * 5}m"
        else -> "${(meters / 10).roundToInt() * 10}m"
    }
}

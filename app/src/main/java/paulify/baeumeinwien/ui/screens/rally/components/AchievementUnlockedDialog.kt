package paulify.baeumeinwien.ui.screens.rally.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.Achievement
import paulify.baeumeinwien.data.domain.AchievementStrings
import paulify.baeumeinwien.data.domain.GameAchievement
import kotlin.random.Random

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float
)

@Composable
fun AchievementUnlockedDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    GameAchievementUnlockedDialog(
        title = achievement.speciesGerman,
        description = stringResource(R.string.achievement_added_to_collection),
        iconName = "leaf",
        onDismiss = onDismiss
    )
}

@Composable
fun GameAchievementUnlockedDialog(
    achievement: GameAchievement,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    val localizedTitle = AchievementStrings.getTitle(context, achievement.id)
    val localizedDescription = AchievementStrings.getDescription(context, achievement.id)
    
    GameAchievementUnlockedDialog(
        title = localizedTitle,
        description = localizedDescription,
        iconName = achievement.iconName,
        onDismiss = onDismiss
    )
}

@Composable
private fun GameAchievementUnlockedDialog(
    title: String,
    description: String,
    iconName: String,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val confettiColors = listOf(
        Color(0xFFFFD700),
        Color(0xFF4CAF50),
        Color(0xFFFF6B6B),
        Color(0xFF64B5F6),
        Color(0xFFFFB74D),
        Color(0xFFBA68C8),
        Color(0xFFFF8A80)
    )
    
    var confettiParticles by remember { 
        mutableStateOf(
            List(60) {
                ConfettiParticle(
                    x = Random.nextFloat() * 1000f,
                    y = -Random.nextFloat() * 500f - 100f,
                    color = confettiColors[Random.nextInt(confettiColors.size)],
                    size = Random.nextFloat() * 10f + 5f,
                    speedX = Random.nextFloat() * 4f - 2f,
                    speedY = Random.nextFloat() * 8f + 4f,
                    rotation = Random.nextFloat() * 360f
                )
            }
        )
    }
    
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )
    
    LaunchedEffect(confettiProgress) {
        confettiParticles = confettiParticles.map { particle ->
            val newY = particle.y + particle.speedY
            val newX = particle.x + particle.speedX
            if (newY > 1500f) {
                particle.copy(
                    y = -50f,
                    x = Random.nextFloat() * 1000f
                )
            } else {
                particle.copy(y = newY, x = newX)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                confettiParticles.forEach { particle ->
                    drawCircle(
                        color = particle.color,
                        radius = particle.size,
                        center = Offset(particle.x, particle.y)
                    )
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.achievement_unlocked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Color(0xFFFFD700).copy(alpha = glowAlpha * 0.3f)
                                )
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                getIconForName(iconName),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700)
                        )
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.achievement_awesome),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun getIconForName(name: String): ImageVector {
    return when (name) {
        "leaf" -> Icons.Default.Eco
        "leaf_circle" -> Icons.Default.Eco
        "star_circle" -> Icons.Default.Star
        "tree" -> Icons.Default.Park
        "sparkles" -> Icons.Default.AutoAwesome
        "graduationcap" -> Icons.Default.School
        "crown" -> Icons.Default.EmojiEvents
        "flag" -> Icons.Default.Flag
        "walk" -> Icons.Default.DirectionsWalk
        "hiking" -> Icons.Default.Hiking
        "map" -> Icons.Default.Map
        "footprints" -> Icons.Default.DirectionsWalk
        "medal" -> Icons.Default.MilitaryTech
        "group" -> Icons.Default.Groups
        "trophy" -> Icons.Default.EmojiEvents
        "qrcode" -> Icons.Default.QrCode
        "star" -> Icons.Default.Star
        "heart" -> Icons.Default.Favorite
        "heart_circle" -> Icons.Default.FavoriteBorder
        "camera" -> Icons.Default.CameraAlt
        else -> Icons.Default.EmojiEvents
    }
}

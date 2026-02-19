package paulify.baeumeinwien.ui.screens.achievements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import paulify.baeumeinwien.R
import paulify.baeumeinwien.data.domain.AchievementCategory
import paulify.baeumeinwien.data.domain.AchievementStrings
import paulify.baeumeinwien.data.domain.GameAchievement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsGalleryScreen(
    unlockedAchievementIds: Set<String> = emptySet(),
    uniqueSpeciesCount: Int = 0,
    onBack: (() -> Unit)? = null
) {
    val achievements = remember(unlockedAchievementIds, uniqueSpeciesCount) {
        GameAchievement.allAchievements.map { achievement ->
            var isUnlocked = unlockedAchievementIds.contains(achievement.id)
            
            if (!isUnlocked && achievement.category == AchievementCategory.SPECIES) {
                when (achievement.id) {
                    "first_tree" -> if (uniqueSpeciesCount >= 1) isUnlocked = true
                    "species_10" -> if (uniqueSpeciesCount >= 10) isUnlocked = true
                    "species_25" -> if (uniqueSpeciesCount >= 25) isUnlocked = true
                    "species_50" -> if (uniqueSpeciesCount >= 50) isUnlocked = true
                }
            }

            if (isUnlocked) {
                achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
            } else {
                achievement
            }
        }
    }
    
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    
    val filteredAchievements = if (selectedCategory != null) {
        achievements.filter { it.category == selectedCategory }
    } else {
        achievements
    }
    
    val unlockedCount = achievements.count { it.isUnlocked }
    val progress = if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size else 0f

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.achievements_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.achievements_unlocked, unlockedCount, achievements.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProgressCard(
                    progress = progress,
                    unlockedCount = unlockedCount,
                    totalCount = achievements.size,
                    speciesCount = uniqueSpeciesCount,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            item {
                CategoryFilter(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            
            if (selectedCategory == null) {
                AchievementCategory.entries.forEach { category ->
                    val categoryAchievements = achievements.filter { it.category == category }
                    if (categoryAchievements.isNotEmpty()) {
                        item {
                            CategoryHeader(category = category)
                        }
                        items(categoryAchievements) { achievement ->
                            AchievementListItem(
                                achievement = achievement,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredAchievements) { achievement ->
                    AchievementListItem(
                        achievement = achievement,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    progress: Float,
    unlockedCount: Int,
    totalCount: Int,
    speciesCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
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
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$speciesCount Baumarten entdeckt",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFFFFD700),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: AchievementCategory?,
    onCategorySelected: (AchievementCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Alle", fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = if (selectedCategory == null) {
                    { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                shape = RoundedCornerShape(16.dp)
            )
        }
        
        items(AchievementCategory.entries.toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { 
                    Text(
                        category.displayName,
                        fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun CategoryHeader(category: AchievementCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AchievementListItem(
    achievement: GameAchievement,
    modifier: Modifier = Modifier
) {
    val isUnlocked = achievement.isUnlocked
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val cardColor by animateColorAsState(
        targetValue = if (isUnlocked) 
            MaterialTheme.colorScheme.secondaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceContainerLow,
        label = "cardColor"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (isUnlocked) pulseScale else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isUnlocked) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA500)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForName(achievement.iconName),
                    contentDescription = null,
                    tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) 
                        MaterialTheme.colorScheme.onSecondaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnlocked)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isUnlocked) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Freigeschaltet",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Gesperrt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: AchievementCategory): ImageVector {
    return when (category) {
        AchievementCategory.SPECIES -> Icons.Default.Eco
        AchievementCategory.EXPLORER -> Icons.Default.Explore
        AchievementCategory.RALLY -> Icons.Default.Groups
        AchievementCategory.SOCIAL -> Icons.Default.Favorite
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

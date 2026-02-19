package paulify.baeumeinwien.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_progress")
data class AchievementProgressEntity(
    @PrimaryKey
    val achievementId: String,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "species_discovery_count")
data class SpeciesDiscoveryCount(
    @PrimaryKey
    val speciesName: String,
    val count: Int = 0
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalTreesDiscovered: Int = 0,
    val uniqueSpeciesCount: Int = 0,
    val totalDistanceWalkedMeters: Double = 0.0,
    val explorerSessionsCompleted: Int = 0,
    val ralliesParticipated: Int = 0,
    val ralliesWon: Int = 0,
    val ralliesCreated: Int = 0,
    val favoritesCount: Int = 0,
    val photosCount: Int = 0,
    val districtsVisited: Set<Int> = emptySet()
)

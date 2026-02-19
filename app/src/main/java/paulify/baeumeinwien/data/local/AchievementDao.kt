package paulify.baeumeinwien.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import paulify.baeumeinwien.data.domain.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: Achievement)

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE speciesGerman = :species")
    suspend fun getAchievementBySpecies(species: String): Achievement?
    
    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getUniqueSpeciesCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: AchievementProgressEntity)
    
    @Query("SELECT * FROM achievement_progress WHERE achievementId = :id")
    suspend fun getProgress(id: String): AchievementProgressEntity?
    
    @Query("SELECT * FROM achievement_progress WHERE isUnlocked = 1")
    fun getUnlockedAchievements(): Flow<List<AchievementProgressEntity>>
    
    @Query("SELECT * FROM achievement_progress")
    fun getAllProgress(): Flow<List<AchievementProgressEntity>>
    
    @Query("SELECT achievementId FROM achievement_progress WHERE isUnlocked = 1")
    suspend fun getUnlockedAchievementIds(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpeciesCount(count: SpeciesDiscoveryCount)
    
    @Query("SELECT * FROM species_discovery_count WHERE speciesName = :species")
    suspend fun getSpeciesCount(species: String): SpeciesDiscoveryCount?
    
    @Query("SELECT SUM(count) FROM species_discovery_count")
    suspend fun getTotalTreesDiscovered(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: UserStatsEntity)
    
    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getStats(): UserStatsEntity?
    
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observeStats(): Flow<UserStatsEntity?>
}

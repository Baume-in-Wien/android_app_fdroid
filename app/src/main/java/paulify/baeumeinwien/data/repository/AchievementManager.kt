package paulify.baeumeinwien.data.repository

import paulify.baeumeinwien.data.domain.Achievement
import paulify.baeumeinwien.data.domain.GameAchievement
import paulify.baeumeinwien.data.local.AchievementDao
import paulify.baeumeinwien.data.local.AchievementProgressEntity
import paulify.baeumeinwien.data.local.SpeciesDiscoveryCount
import paulify.baeumeinwien.data.local.UserStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AchievementManager(
    private val achievementDao: AchievementDao
) {
    
    suspend fun onTreeDiscovered(speciesGerman: String, district: Int?): List<GameAchievement> {
        val unlockedAchievements = mutableListOf<GameAchievement>()
        
        if (speciesGerman.isBlank() || speciesGerman == "Unbekannt") {
            return emptyList()
        }
        
        val existingSpecies = achievementDao.getAchievementBySpecies(speciesGerman)
        val isNewSpecies = existingSpecies == null
        
        if (isNewSpecies) {
            val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Ei-leaf.svg/1200px-Ei-leaf.svg.png"
            achievementDao.insert(Achievement(speciesGerman, imageUrl))
        }
        
        val speciesKey = getSpeciesKey(speciesGerman)
        if (speciesKey != null) {
            val currentCount = achievementDao.getSpeciesCount(speciesKey)?.count ?: 0
            achievementDao.upsertSpeciesCount(SpeciesDiscoveryCount(speciesKey, currentCount + 1))
        }
        
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        val newUniqueSpeciesCount = achievementDao.getUniqueSpeciesCount()
        val newDistrictsVisited = if (district != null) {
            stats.districtsVisited + district
        } else {
            stats.districtsVisited
        }
        
        achievementDao.upsertStats(stats.copy(
            totalTreesDiscovered = stats.totalTreesDiscovered + 1,
            uniqueSpeciesCount = newUniqueSpeciesCount,
            districtsVisited = newDistrictsVisited
        ))
        
        unlockedAchievements.addAll(checkSpeciesAchievements(newUniqueSpeciesCount, speciesKey))
        unlockedAchievements.addAll(checkDistrictAchievements(newDistrictsVisited))
        
        return unlockedAchievements
    }
    
    private fun getSpeciesKey(speciesGerman: String): String? {
        val lowerCase = speciesGerman.lowercase()
        return when {
            lowerCase.contains("linde") -> "Linde"
            lowerCase.contains("eiche") -> "Eiche"
            lowerCase.contains("ahorn") -> "Ahorn"
            lowerCase.contains("kastanie") -> "Kastanie"
            else -> null
        }
    }
    
    private suspend fun checkSpeciesAchievements(uniqueCount: Int, speciesKey: String?): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        
        if (uniqueCount >= 1) {
            unlocked.addAll(tryUnlockAchievement("first_tree"))
        }
        
        if (uniqueCount >= 10) {
            unlocked.addAll(tryUnlockAchievement("species_10"))
        }
        if (uniqueCount >= 25) {
            unlocked.addAll(tryUnlockAchievement("species_25"))
        }
        if (uniqueCount >= 50) {
            unlocked.addAll(tryUnlockAchievement("species_50"))
        }
        
        if (speciesKey != null) {
            val count = achievementDao.getSpeciesCount(speciesKey)?.count ?: 0
            if (count >= 10) {
                val achievementId = when (speciesKey) {
                    "Linde" -> "linde_lover"
                    "Eiche" -> "eiche_expert"
                    "Ahorn" -> "ahorn_ace"
                    "Kastanie" -> "kastanie_king"
                    else -> null
                }
                achievementId?.let { unlocked.addAll(tryUnlockAchievement(it)) }
            }
        }
        
        return unlocked
    }
    
    private suspend fun checkDistrictAchievements(visitedDistricts: Set<Int>): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        
        if (visitedDistricts.size >= 23) {
            unlocked.addAll(tryUnlockAchievement("district_all"))
        }
        
        return unlocked
    }
    
    suspend fun onFavoriteAdded(): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        val newCount = stats.favoritesCount + 1
        
        achievementDao.upsertStats(stats.copy(favoritesCount = newCount))
        
        if (newCount >= 1) {
            unlocked.addAll(tryUnlockAchievement("first_favorite"))
        }
        
        if (newCount >= 10) {
            unlocked.addAll(tryUnlockAchievement("favorites_10"))
        }
        
        return unlocked
    }
    
    suspend fun onPhotoTaken(): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        val newCount = stats.photosCount + 1
        
        achievementDao.upsertStats(stats.copy(photosCount = newCount))
        
        if (newCount >= 10) {
            unlocked.addAll(tryUnlockAchievement("photo_10"))
        }
        
        return unlocked
    }
    
    suspend fun onExplorerSessionCompleted(): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        val newCount = stats.explorerSessionsCompleted + 1
        
        achievementDao.upsertStats(stats.copy(explorerSessionsCompleted = newCount))
        
        if (newCount >= 1) {
            unlocked.addAll(tryUnlockAchievement("first_mission"))
        }
        
        if (newCount >= 5) {
            unlocked.addAll(tryUnlockAchievement("explorer_5"))
        }
        
        if (newCount >= 25) {
            unlocked.addAll(tryUnlockAchievement("explorer_25"))
        }
        
        return unlocked
    }
    
    suspend fun onDistanceWalked(distanceMeters: Double): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        val newDistance = stats.totalDistanceWalkedMeters + distanceMeters
        
        achievementDao.upsertStats(stats.copy(totalDistanceWalkedMeters = newDistance))
        
        if (newDistance >= 5000) {
            unlocked.addAll(tryUnlockAchievement("walker_5km"))
        }
        
        if (newDistance >= 50000) {
            unlocked.addAll(tryUnlockAchievement("walker_50km"))
        }
        
        return unlocked
    }
    
    suspend fun onRallyParticipated(): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        val newCount = stats.ralliesParticipated + 1
        
        achievementDao.upsertStats(stats.copy(ralliesParticipated = newCount))
        
        if (newCount >= 1) {
            unlocked.addAll(tryUnlockAchievement("first_rally"))
        }
        
        if (newCount >= 10) {
            unlocked.addAll(tryUnlockAchievement("rally_10"))
        }
        
        return unlocked
    }
    
    suspend fun onRallyWon(): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        
        achievementDao.upsertStats(stats.copy(ralliesWon = stats.ralliesWon + 1))
        unlocked.addAll(tryUnlockAchievement("rally_winner"))
        
        return unlocked
    }
    
    suspend fun onRallyCreated(): List<GameAchievement> {
        val unlocked = mutableListOf<GameAchievement>()
        val stats = achievementDao.getStats() ?: UserStatsEntity()
        
        achievementDao.upsertStats(stats.copy(ralliesCreated = stats.ralliesCreated + 1))
        unlocked.addAll(tryUnlockAchievement("rally_host"))
        
        return unlocked
    }
    
    private suspend fun tryUnlockAchievement(achievementId: String): List<GameAchievement> {
        val existing = achievementDao.getProgress(achievementId)
        
        if (existing?.isUnlocked == true) {
            return emptyList()
        }
        
        achievementDao.upsertProgress(
            AchievementProgressEntity(
                achievementId = achievementId,
                currentProgress = 1,
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis()
            )
        )
        
        val achievement = GameAchievement.allAchievements.find { it.id == achievementId }
        return if (achievement != null) {
            listOf(achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
        } else {
            emptyList()
        }
    }
    
    suspend fun getUnlockedAchievementIds(): Set<String> {
        return achievementDao.getUnlockedAchievementIds().toSet()
    }
    
    suspend fun getUniqueSpeciesCount(): Int {
        return achievementDao.getUniqueSpeciesCount()
    }
    
    fun observeStats(): Flow<UserStatsEntity?> {
        return achievementDao.observeStats()
    }
    
    suspend fun getAllAchievementsWithStatus(): List<GameAchievement> {
        val unlockedIds = getUnlockedAchievementIds()
        return GameAchievement.allAchievements.map { achievement ->
            if (unlockedIds.contains(achievement.id)) {
                achievement.copy(isUnlocked = true)
            } else {
                achievement
            }
        }
    }
}

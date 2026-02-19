package paulify.baeumeinwien.data.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AchievementCategory(val displayName: String, val icon: String) {
    SPECIES("Arten", "🌿"),
    EXPLORER("Entdecker", "🚶"),
    RALLY("Rallye", "👥"),
    SOCIAL("Social", "❤️")
}

@Entity(tableName = "game_achievements")
data class GameAchievement(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val targetCount: Int? = null
) {
    companion object {
        val allAchievements: List<GameAchievement> = listOf(
            GameAchievement(
                id = "first_tree",
                title = "Erster Baum",
                description = "Entdecke deinen ersten Baum",
                category = AchievementCategory.SPECIES,
                iconName = "leaf",
                targetCount = 1
            ),
            GameAchievement(
                id = "linde_lover",
                title = "Lindenliebhaber:in",
                description = "Entdecke 10 Linden",
                category = AchievementCategory.SPECIES,
                iconName = "leaf_circle",
                targetCount = 10
            ),
            GameAchievement(
                id = "kastanie_king",
                title = "Kastanienkönig:in",
                description = "Entdecke 10 Kastanien",
                category = AchievementCategory.SPECIES,
                iconName = "star_circle",
                targetCount = 10
            ),
            GameAchievement(
                id = "ahorn_ace",
                title = "Ahorn-Ass",
                description = "Entdecke 10 Ahornbäume",
                category = AchievementCategory.SPECIES,
                iconName = "leaf",
                targetCount = 10
            ),
            GameAchievement(
                id = "eiche_expert",
                title = "Eichen-Expert:in",
                description = "Entdecke 10 Eichen",
                category = AchievementCategory.SPECIES,
                iconName = "tree",
                targetCount = 10
            ),
            GameAchievement(
                id = "species_10",
                title = "Artenkenner:in",
                description = "Entdecke 10 verschiedene Arten",
                category = AchievementCategory.SPECIES,
                iconName = "sparkles",
                targetCount = 10
            ),
            GameAchievement(
                id = "species_25",
                title = "Botaniker:in",
                description = "Entdecke 25 verschiedene Arten",
                category = AchievementCategory.SPECIES,
                iconName = "graduationcap",
                targetCount = 25
            ),
            GameAchievement(
                id = "species_50",
                title = "Dendrologe:in",
                description = "Entdecke 50 verschiedene Arten",
                category = AchievementCategory.SPECIES,
                iconName = "crown",
                targetCount = 50
            ),
            
            GameAchievement(
                id = "first_mission",
                title = "Erste Mission",
                description = "Schließe deine erste Mission ab",
                category = AchievementCategory.EXPLORER,
                iconName = "flag"
            ),
            GameAchievement(
                id = "explorer_5",
                title = "Entdecker:in",
                description = "Schließe 5 Explorer-Sessions ab",
                category = AchievementCategory.EXPLORER,
                iconName = "walk"
            ),
            GameAchievement(
                id = "explorer_25",
                title = "Wanderer:in",
                description = "Schließe 25 Explorer-Sessions ab",
                category = AchievementCategory.EXPLORER,
                iconName = "hiking"
            ),
            GameAchievement(
                id = "district_all",
                title = "Wien-Kenner:in",
                description = "Besuche Bäume in allen 23 Bezirken",
                category = AchievementCategory.EXPLORER,
                iconName = "map"
            ),
            GameAchievement(
                id = "walker_5km",
                title = "5km Wanderer:in",
                description = "Lege 5km zu Fuß zurück",
                category = AchievementCategory.EXPLORER,
                iconName = "footprints"
            ),
            GameAchievement(
                id = "walker_50km",
                title = "Marathoni",
                description = "Lege 50km zu Fuß zurück",
                category = AchievementCategory.EXPLORER,
                iconName = "medal"
            ),
            
            GameAchievement(
                id = "first_rally",
                title = "Erste Rallye",
                description = "Nimm an deiner ersten Rallye teil",
                category = AchievementCategory.RALLY,
                iconName = "group"
            ),
            GameAchievement(
                id = "rally_winner",
                title = "Rallye-Sieger:in",
                description = "Gewinne eine Rallye",
                category = AchievementCategory.RALLY,
                iconName = "trophy"
            ),
            GameAchievement(
                id = "rally_host",
                title = "Rallye-Leiter:in",
                description = "Erstelle deine erste Rallye",
                category = AchievementCategory.RALLY,
                iconName = "qrcode"
            ),
            GameAchievement(
                id = "rally_10",
                title = "Rallye-Veteran:in",
                description = "Nimm an 10 Rallyes teil",
                category = AchievementCategory.RALLY,
                iconName = "star"
            ),
            
            GameAchievement(
                id = "first_favorite",
                title = "Erster Favorit",
                description = "Füge deinen ersten Favoriten hinzu",
                category = AchievementCategory.SOCIAL,
                iconName = "heart"
            ),
            GameAchievement(
                id = "favorites_10",
                title = "Sammler:in",
                description = "Speichere 10 Lieblingsbäume",
                category = AchievementCategory.SOCIAL,
                iconName = "heart_circle"
            ),
            GameAchievement(
                id = "photo_10",
                title = "Fotograf:in",
                description = "Mache 10 Baum-Fotos",
                category = AchievementCategory.SOCIAL,
                iconName = "camera"
            )
        )
    }
}

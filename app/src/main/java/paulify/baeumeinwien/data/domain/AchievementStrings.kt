package paulify.baeumeinwien.data.domain

import android.content.Context
import paulify.baeumeinwien.R

object AchievementStrings {
    
    fun getTitle(context: Context, achievementId: String): String {
        return when (achievementId) {
            "first_tree" -> context.getString(R.string.achievement_first_tree)
            "species_10" -> context.getString(R.string.achievement_species_10)
            "species_25" -> context.getString(R.string.achievement_species_25)
            "species_50" -> context.getString(R.string.achievement_species_50)
            "linde_lover" -> context.getString(R.string.achievement_linde_lover)
            "kastanie_king" -> context.getString(R.string.achievement_kastanie_king)
            "ahorn_ace" -> context.getString(R.string.achievement_ahorn_ace)
            "eiche_expert" -> context.getString(R.string.achievement_eiche_expert)
            "first_mission" -> context.getString(R.string.achievement_first_mission)
            "explorer_5" -> context.getString(R.string.achievement_explorer_5)
            "explorer_25" -> context.getString(R.string.achievement_explorer_25)
            "district_all" -> context.getString(R.string.achievement_district_all)
            "walker_5km" -> context.getString(R.string.achievement_walker_5km)
            "walker_50km" -> context.getString(R.string.achievement_walker_50km)
            "first_rally" -> context.getString(R.string.achievement_first_rally)
            "rally_winner" -> context.getString(R.string.achievement_rally_winner)
            "rally_host" -> context.getString(R.string.achievement_rally_host)
            "rally_10" -> context.getString(R.string.achievement_rally_10)
            "first_favorite" -> context.getString(R.string.achievement_first_favorite)
            "favorites_10" -> context.getString(R.string.achievement_favorites_10)
            "photo_10" -> context.getString(R.string.achievement_photo_10)
            else -> achievementId
        }
    }
    
    fun getDescription(context: Context, achievementId: String): String {
        return when (achievementId) {
            "first_tree" -> context.getString(R.string.achievement_first_tree_desc)
            "species_10" -> context.getString(R.string.achievement_species_10_desc)
            "species_25" -> context.getString(R.string.achievement_species_25_desc)
            "species_50" -> context.getString(R.string.achievement_species_50_desc)
            "linde_lover" -> context.getString(R.string.achievement_linde_lover_desc)
            "kastanie_king" -> context.getString(R.string.achievement_kastanie_king_desc)
            "ahorn_ace" -> context.getString(R.string.achievement_ahorn_ace_desc)
            "eiche_expert" -> context.getString(R.string.achievement_eiche_expert_desc)
            "first_mission" -> context.getString(R.string.achievement_first_mission_desc)
            "explorer_5" -> context.getString(R.string.achievement_explorer_5_desc)
            "explorer_25" -> context.getString(R.string.achievement_explorer_25_desc)
            "district_all" -> context.getString(R.string.achievement_district_all_desc)
            "walker_5km" -> context.getString(R.string.achievement_walker_5km_desc)
            "walker_50km" -> context.getString(R.string.achievement_walker_50km_desc)
            "first_rally" -> context.getString(R.string.achievement_first_rally_desc)
            "rally_winner" -> context.getString(R.string.achievement_rally_winner_desc)
            "rally_host" -> context.getString(R.string.achievement_rally_host_desc)
            "rally_10" -> context.getString(R.string.achievement_rally_10_desc)
            "first_favorite" -> context.getString(R.string.achievement_first_favorite_desc)
            "favorites_10" -> context.getString(R.string.achievement_favorites_10_desc)
            "photo_10" -> context.getString(R.string.achievement_photo_10_desc)
            else -> ""
        }
    }
    
    fun getCategoryName(context: Context, category: AchievementCategory): String {
        return when (category) {
            AchievementCategory.SPECIES -> context.getString(R.string.achievements_category_species)
            AchievementCategory.EXPLORER -> context.getString(R.string.achievements_category_explorer)
            AchievementCategory.RALLY -> context.getString(R.string.achievements_category_rally)
            AchievementCategory.SOCIAL -> context.getString(R.string.achievements_category_social)
        }
    }
}

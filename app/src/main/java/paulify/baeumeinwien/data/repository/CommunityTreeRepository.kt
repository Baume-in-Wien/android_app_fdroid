package paulify.baeumeinwien.data.repository

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import paulify.baeumeinwien.data.domain.CommunityTree
import paulify.baeumeinwien.data.domain.TreeSpecies
import paulify.baeumeinwien.data.domain.UserProfile
import paulify.baeumeinwien.data.remote.SupabaseInstance
import paulify.baeumeinwien.data.remote.dto.CommunityTreeInsert

class CommunityTreeRepository {

    private val client = SupabaseInstance.client

    suspend fun searchSpecies(query: String): List<TreeSpecies> {
        if (query.length < 2) return emptyList()

        try {
            val result = client.postgrest.rpc(
                "search_tree_species",
                buildJsonObject {
                    put("p_query", query)
                    put("p_limit", 20)
                }
            )
            val species = result.decodeList<TreeSpecies>()
            if (species.isNotEmpty()) return species
        } catch (e: Exception) {
            Log.d(TAG, "RPC search unavailable, using local fallback", e)
        }

        val lowerQuery = query.lowercase()
        return COMMON_VIENNA_SPECIES.filter { species ->
            species.nameGerman.lowercase().contains(lowerQuery) ||
                    (species.nameScientific?.lowercase()?.contains(lowerQuery) == true)
        }.take(20)
    }

    companion object {
        private const val TAG = "CommunityTreeRepo"

        private val COMMON_VIENNA_SPECIES = listOf(
            TreeSpecies(1, "Spitzahorn", "Acer platanoides", "Laubbaum"),
            TreeSpecies(2, "Bergahorn", "Acer pseudoplatanus", "Laubbaum"),
            TreeSpecies(3, "Feldahorn", "Acer campestre", "Laubbaum"),
            TreeSpecies(4, "Silberahorn", "Acer saccharinum", "Laubbaum"),
            TreeSpecies(5, "Rosskastanie", "Aesculus hippocastanum", "Laubbaum"),
            TreeSpecies(6, "Rotbuche", "Fagus sylvatica", "Laubbaum"),
            TreeSpecies(7, "Hainbuche", "Carpinus betulus", "Laubbaum"),
            TreeSpecies(8, "Winterlinde", "Tilia cordata", "Laubbaum"),
            TreeSpecies(9, "Sommerlinde", "Tilia platyphyllos", "Laubbaum"),
            TreeSpecies(10, "Silberlinde", "Tilia tomentosa", "Laubbaum"),
            TreeSpecies(11, "Stieleiche", "Quercus robur", "Laubbaum"),
            TreeSpecies(12, "Traubeneiche", "Quercus petraea", "Laubbaum"),
            TreeSpecies(13, "Roteiche", "Quercus rubra", "Laubbaum"),
            TreeSpecies(14, "Gemeine Esche", "Fraxinus excelsior", "Laubbaum"),
            TreeSpecies(15, "Schmalbl. Esche", "Fraxinus angustifolia", "Laubbaum"),
            TreeSpecies(16, "Gemeine Birke", "Betula pendula", "Laubbaum"),
            TreeSpecies(17, "Schwarzerle", "Alnus glutinosa", "Laubbaum"),
            TreeSpecies(18, "Grauerle", "Alnus incana", "Laubbaum"),
            TreeSpecies(19, "Platane", "Platanus x acerifolia", "Laubbaum"),
            TreeSpecies(20, "Robinie", "Robinia pseudoacacia", "Laubbaum"),
            TreeSpecies(21, "Schwarzpappel", "Populus nigra", "Laubbaum"),
            TreeSpecies(22, "Silberpappel", "Populus alba", "Laubbaum"),
            TreeSpecies(23, "Zitterpappel", "Populus tremula", "Laubbaum"),
            TreeSpecies(24, "Bruchweide", "Salix fragilis", "Laubbaum"),
            TreeSpecies(25, "Silberweide", "Salix alba", "Laubbaum"),
            TreeSpecies(26, "Trauerweide", "Salix babylonica", "Laubbaum"),
            TreeSpecies(27, "Vogelkirsche", "Prunus avium", "Laubbaum"),
            TreeSpecies(28, "Japanische Kirsche", "Prunus serrulata", "Laubbaum"),
            TreeSpecies(29, "Eberesche", "Sorbus aucuparia", "Laubbaum"),
            TreeSpecies(30, "Mehlbeere", "Sorbus aria", "Laubbaum"),
            TreeSpecies(31, "Walnuss", "Juglans regia", "Laubbaum"),
            TreeSpecies(32, "Götterbaum", "Ailanthus altissima", "Laubbaum"),
            TreeSpecies(33, "Tulpenbaum", "Liriodendron tulipifera", "Laubbaum"),
            TreeSpecies(34, "Ginkgo", "Ginkgo biloba", "Laubbaum"),
            TreeSpecies(35, "Gemeine Fichte", "Picea abies", "Nadelbaum"),
            TreeSpecies(36, "Schwarzkiefer", "Pinus nigra", "Nadelbaum"),
            TreeSpecies(37, "Waldkiefer", "Pinus sylvestris", "Nadelbaum"),
            TreeSpecies(38, "Europäische Lärche", "Larix decidua", "Nadelbaum"),
            TreeSpecies(39, "Weißtanne", "Abies alba", "Nadelbaum"),
            TreeSpecies(40, "Eibe", "Taxus baccata", "Nadelbaum"),
            TreeSpecies(41, "Lebensbaum", "Thuja occidentalis", "Nadelbaum"),
            TreeSpecies(42, "Blauglockenbaum", "Paulownia tomentosa", "Laubbaum"),
            TreeSpecies(43, "Trompetenbaum", "Catalpa bignonioides", "Laubbaum"),
            TreeSpecies(44, "Zürgelbaum", "Celtis australis", "Laubbaum"),
            TreeSpecies(45, "Amberbaum", "Liquidambar styraciflua", "Laubbaum"),
            TreeSpecies(46, "Schnurbaum", "Styphnolobium japonicum", "Laubbaum"),
            TreeSpecies(47, "Gleditschie", "Gleditsia triacanthos", "Laubbaum"),
            TreeSpecies(48, "Hasel", "Corylus avellana", "Strauch"),
            TreeSpecies(49, "Holunder", "Sambucus nigra", "Strauch"),
            TreeSpecies(50, "Apfelbaum", "Malus domestica", "Obstbaum"),
            TreeSpecies(51, "Birnbaum", "Pyrus communis", "Obstbaum"),
            TreeSpecies(52, "Zwetschke", "Prunus domestica", "Obstbaum"),
            TreeSpecies(53, "Marille", "Prunus armeniaca", "Obstbaum"),
            TreeSpecies(54, "Edelkastanie", "Castanea sativa", "Laubbaum"),
            TreeSpecies(55, "Mammutbaum", "Sequoiadendron giganteum", "Nadelbaum"),
        )
    }

    suspend fun addCommunityTree(tree: CommunityTreeInsert): Result<CommunityTree> {
        return try {
            val result = client.postgrest
                .from("community_trees")
                .insert(tree) {
                    select()
                }
                .decodeSingle<CommunityTree>()
            Log.d(TAG, "Community tree added: ${result.id}")
            Result.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add community tree", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun getCommunityTreesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<CommunityTree> {
        return try {
            val result = client.postgrest.rpc(
                "get_community_trees_in_bounds",
                buildJsonObject {
                    put("p_min_lat", minLat)
                    put("p_max_lat", maxLat)
                    put("p_min_lon", minLon)
                    put("p_max_lon", maxLon)
                    put("p_limit", 500)
                }
            )
            result.decodeList<CommunityTree>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load community trees", e)
            emptyList()
        }
    }

    suspend fun getMyTrees(userId: String): List<CommunityTree> {
        return try {
            client.postgrest
                .from("community_trees")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<CommunityTree>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user trees", e)
            emptyList()
        }
    }

    suspend fun deleteCommunityTree(treeId: String): Result<Unit> {
        return try {
            client.postgrest
                .from("community_trees")
                .delete {
                    filter {
                        eq("id", treeId)
                    }
                }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete community tree", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun reportTree(treeId: String, reporterId: String, reason: String, comment: String?): Result<Unit> {
        return try {
            client.postgrest
                .from("community_tree_reports")
                .insert(buildJsonObject {
                    put("tree_id", treeId)
                    put("reporter_id", reporterId)
                    put("reason", reason)
                    comment?.let { put("comment", it) }
                })
            Log.d(TAG, "Tree $treeId reported: $reason")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report tree", e)
            val message = if (e.message?.contains("duplicate") == true || e.message?.contains("unique") == true) {
                "Du hast diesen Baum bereits gemeldet"
            } else {
                e.message ?: "Meldung fehlgeschlagen"
            }
            Result.Error(e, message)
        }
    }

    suspend fun confirmTree(treeId: String, userId: String): Result<Unit> {
        return try {
            client.postgrest
                .from("community_tree_confirmations")
                .insert(buildJsonObject {
                    put("tree_id", treeId)
                    put("user_id", userId)
                })
            Log.d(TAG, "Tree $treeId confirmed by $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to confirm tree", e)
            val message = if (e.message?.contains("duplicate") == true || e.message?.contains("unique") == true) {
                "Du hast diesen Baum bereits bestätigt"
            } else {
                e.message ?: "Bestätigung fehlgeschlagen"
            }
            Result.Error(e, message)
        }
    }

    suspend fun removeConfirmation(treeId: String, userId: String): Result<Unit> {
        return try {
            client.postgrest
                .from("community_tree_confirmations")
                .delete {
                    filter {
                        eq("tree_id", treeId)
                        eq("user_id", userId)
                    }
                }
            Log.d(TAG, "Confirmation removed for tree $treeId by $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove confirmation", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            client.postgrest
                .from("user_profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user profile", e)
            null
        }
    }

    suspend fun updateShowNamePreference(userId: String, show: Boolean): Result<Unit> {
        return try {
            client.postgrest
                .from("user_profiles")
                .update(buildJsonObject {
                    put("show_name_on_trees", show)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
            Log.d(TAG, "Updated show_name_on_trees to $show for $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update show name preference", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun adminVerifyTree(treeId: String): Result<Unit> {
        return try {
            client.postgrest.rpc(
                "admin_verify_tree",
                buildJsonObject {
                    put("p_tree_id", treeId)
                }
            )
            Log.d(TAG, "Tree $treeId officially verified by admin")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify tree", e)
            Result.Error(e, e.message ?: "Verifizierung fehlgeschlagen")
        }
    }
}

package paulify.baeumeinwien.data.rally

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

class RallyRepository(
    private val supabase: SupabaseClient
) {

    
    private fun getCurrentUserId(): String {
        return "anonymous-user-${android.os.Build.ID}"
    }


    @Serializable
    data class Rally(
        val id: String,
        val code: String,
        val title: String,
        val description: String? = null,
        @SerialName("creator_user_id") val creatorUserId: String,
        @SerialName("center_lat") val centerLat: Double,
        @SerialName("center_lon") val centerLon: Double,
        @SerialName("radius_meters") val radiusMeters: Int,
        @SerialName("target_tree_count") val targetTreeCount: Int,
        @SerialName("is_active") val isActive: Boolean = true,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class RallyParticipant(
        val id: String,
        @SerialName("rally_id") val rallyId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("joined_at") val joinedAt: String? = null
    )

    @Serializable
    data class RallyTask(
        val id: String? = null,
        @SerialName("rally_id") val rallyId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("tree_id") val treeId: String,
        @SerialName("photo_url") val photoUrl: String? = null,
        val note: String? = null,
        @SerialName("tree_lat") val treeLat: Double,
        @SerialName("tree_lon") val treeLon: Double,
        @SerialName("completed_at") val completedAt: String? = null
    )

    @Serializable
    data class RallyProgress(
        @SerialName("rally_id") val rallyId: String,
        val code: String,
        val title: String,
        @SerialName("target_tree_count") val targetTreeCount: Int,
        @SerialName("user_id") val userId: String,
        @SerialName("trees_found") val treesFound: Int,
        @SerialName("progress_percent") val progressPercent: Double,
        @SerialName("is_completed") val isCompleted: Boolean
    )


    sealed class RallyResult<out T> {
        data class Success<T>(val data: T) : RallyResult<T>()
        data class Error(val message: String, val exception: Throwable? = null) : RallyResult<Nothing>()
    }


    suspend fun createRally(
        title: String,
        description: String?,
        radiusMeters: Int,
        targetTreeCount: Int,
        centerLat: Double,
        centerLon: Double
    ): RallyResult<Rally> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val rally = Rally(
                id = UUID.randomUUID().toString(),
                code = "",
                title = title.trim(),
                description = description?.trim(),
                creatorUserId = userId,
                centerLat = centerLat,
                centerLon = centerLon,
                radiusMeters = radiusMeters,
                targetTreeCount = targetTreeCount,
                isActive = true
            )

            val createdRally = supabase.from("rallies")
                .insert(rally) {
                    select()
                }
                .decodeSingle<Rally>()

            val participant = RallyParticipant(
                id = UUID.randomUUID().toString(),
                rallyId = createdRally.id,
                userId = userId
            )

            supabase.from("rally_participants")
                .insert(participant)

            RallyResult.Success(createdRally)
        } catch (e: Exception) {
            RallyResult.Error("Failed to create rally: ${e.message}", e)
        }
    }

    suspend fun joinRally(code: String): RallyResult<Rally> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val rallies = supabase.from("rallies")
                .select {
                    filter {
                        eq("code", code.uppercase().trim())
                        eq("is_active", true)
                    }
                }
                .decodeList<Rally>()

            if (rallies.isEmpty()) {
                return@withContext RallyResult.Error("Rally not found with code: $code")
            }

            val rally = rallies.first()

            val existingParticipants = supabase.from("rally_participants")
                .select {
                    filter {
                        eq("rally_id", rally.id)
                        eq("user_id", userId)
                    }
                }
                .decodeList<RallyParticipant>()

            if (existingParticipants.isNotEmpty()) {
                return@withContext RallyResult.Success(rally)
            }

            val participant = RallyParticipant(
                id = UUID.randomUUID().toString(),
                rallyId = rally.id,
                userId = userId
            )

            supabase.from("rally_participants")
                .insert(participant)

            RallyResult.Success(rally)
        } catch (e: Exception) {
            RallyResult.Error("Failed to join rally: ${e.message}", e)
        }
    }

    suspend fun getActiveRally(): RallyResult<Rally?> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val participants = supabase.from("rally_participants")
                .select(Columns.raw("rally_id, joined_at")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<RallyParticipant>()

            if (participants.isEmpty()) {
                return@withContext RallyResult.Success(null)
            }

            val rallyId = participants
                .maxByOrNull { it.joinedAt ?: "" }
                ?.rallyId
                ?: return@withContext RallyResult.Success(null)

            val rally = supabase.from("rallies")
                .select {
                    filter {
                        eq("id", rallyId)
                        eq("is_active", true)
                    }
                }
                .decodeSingleOrNull<Rally>()

            RallyResult.Success(rally)
        } catch (e: Exception) {
            RallyResult.Error("Failed to get active rally: ${e.message}", e)
        }
    }

    suspend fun getUserRallies(): RallyResult<List<Rally>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val participants = supabase.from("rally_participants")
                .select(Columns.raw("rally_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<RallyParticipant>()

            if (participants.isEmpty()) {
                return@withContext RallyResult.Success(emptyList())
            }

            val rallyIds = participants.map { it.rallyId }

            val rallies = supabase.from("rallies")
                .select {
                    filter {
                        isIn("id", rallyIds)
                        eq("is_active", true)
                    }
                }
                .decodeList<Rally>()

            RallyResult.Success(rallies)
        } catch (e: Exception) {
            RallyResult.Error("Failed to get user rallies: ${e.message}", e)
        }
    }

    suspend fun getRallyTasks(rallyId: String): RallyResult<List<RallyTask>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val tasks = supabase.from("rally_tasks")
                .select {
                    filter {
                        eq("rally_id", rallyId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<RallyTask>()

            RallyResult.Success(tasks)
        } catch (e: Exception) {
            RallyResult.Error("Failed to get rally tasks: ${e.message}", e)
        }
    }

    suspend fun completeTask(
        rallyId: String,
        treeId: String,
        treeLat: Double,
        treeLon: Double,
        photoUrl: String? = null,
        note: String? = null
    ): RallyResult<RallyTask> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val participants = supabase.from("rally_participants")
                .select {
                    filter {
                        eq("rally_id", rallyId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<RallyParticipant>()

            if (participants.isEmpty()) {
                return@withContext RallyResult.Error("User is not a participant of this rally")
            }

            val rally = supabase.from("rallies")
                .select {
                    filter {
                        eq("id", rallyId)
                    }
                }
                .decodeSingleOrNull<Rally>()
                ?: return@withContext RallyResult.Error("Rally not found")

            val distance = calculateDistance(
                rally.centerLat, rally.centerLon,
                treeLat, treeLon
            )

            if (distance > rally.radiusMeters) {
                return@withContext RallyResult.Error(
                    "Tree is outside rally radius (${distance.toInt()}m > ${rally.radiusMeters}m)"
                )
            }

            val existingTasks = supabase.from("rally_tasks")
                .select {
                    filter {
                        eq("rally_id", rallyId)
                        eq("user_id", userId)
                        eq("tree_id", treeId)
                    }
                }
                .decodeList<RallyTask>()

            if (existingTasks.isNotEmpty()) {
                return@withContext RallyResult.Error("Tree already discovered in this rally")
            }

            val task = RallyTask(
                rallyId = rallyId,
                userId = userId,
                treeId = treeId,
                treeLat = treeLat,
                treeLon = treeLon,
                photoUrl = photoUrl,
                note = note?.trim()
            )

            val createdTask = supabase.from("rally_tasks")
                .insert(task) {
                    select()
                }
                .decodeSingle<RallyTask>()

            RallyResult.Success(createdTask)
        } catch (e: Exception) {
            RallyResult.Error("Failed to complete task: ${e.message}", e)
        }
    }

    suspend fun getRallyProgress(rallyId: String): RallyResult<RallyProgress?> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val progress = supabase.from("rally_progress")
                .select {
                    filter {
                        eq("rally_id", rallyId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<RallyProgress>()

            RallyResult.Success(progress)
        } catch (e: Exception) {
            RallyResult.Error("Failed to get rally progress: ${e.message}", e)
        }
    }

    suspend fun leaveRally(rallyId: String): RallyResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            supabase.from("rally_participants")
                .delete {
                    filter {
                        eq("rally_id", rallyId)
                        eq("user_id", userId)
                    }
                }

            RallyResult.Success(Unit)
        } catch (e: Exception) {
            RallyResult.Error("Failed to leave rally: ${e.message}", e)
        }
    }

    suspend fun updateRally(
        rallyId: String,
        title: String? = null,
        description: String? = null,
        isActive: Boolean? = null
    ): RallyResult<Rally> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            val updates = mutableMapOf<String, Any>()
            title?.let { updates["title"] = it.trim() }
            description?.let { updates["description"] = it.trim() }
            isActive?.let { updates["is_active"] = it }

            if (updates.isEmpty()) {
                return@withContext RallyResult.Error("No updates provided")
            }

            val updatedRally = supabase.from("rallies")
                .update(updates) {
                    filter {
                        eq("id", rallyId)
                        eq("creator_user_id", userId)
                    }
                    select()
                }
                .decodeSingle<Rally>()

            RallyResult.Success(updatedRally)
        } catch (e: Exception) {
            RallyResult.Error("Failed to update rally: ${e.message}", e)
        }
    }


    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadiusMeters * c
    }
}

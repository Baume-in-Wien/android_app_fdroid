package paulify.baeumeinwien.data.repository

import android.provider.Settings
import android.util.Log
import paulify.baeumeinwien.data.domain.*
import paulify.baeumeinwien.data.remote.SupabaseInstance
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put

class CrossplayRallyRepository(private val androidId: String) {

    private val supabase = SupabaseInstance.client
    private val TAG = "CrossplayRallyRepo"

    suspend fun createRally(
        name: String,
        description: String,
        mode: RallyMode,
        districtFilter: List<Int>? = null,
        targetSpeciesCount: Int? = null,
        timeLimitMinutes: Int? = null,
        centerLat: Double? = null,
        centerLng: Double? = null,
        radiusMeters: Int? = null,
        targetTreeIds: List<String>? = null
    ): Result<CreateRallyResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating rally: $name")
            
            val params = buildJsonObject {
                put("p_name", name)
                put("p_description", description)
                put("p_creator_id", androidId)
                put("p_creator_platform", "android")
                put("p_mode", mode.toServerValue())
                districtFilter?.let { filters ->
                    putJsonArray("p_district_filter") {
                        filters.forEach { add(it) }
                    }
                }
                targetTreeIds?.let { ids ->
                    putJsonArray("p_target_tree_ids") {
                        ids.forEach { add(it) }
                    }
                }
                targetSpeciesCount?.let { put("p_target_species_count", it) }
                timeLimitMinutes?.let { put("p_time_limit_minutes", it) }
                radiusMeters?.let { put("p_radius_meters", it) }
                centerLat?.let { put("p_center_lat", it) }
                centerLng?.let { put("p_center_lng", it) }
            }
            
            val result = supabase.postgrest.rpc(
                function = "create_rally",
                parameters = params
            ).decodeSingle<CreateRallyResult>()
            
            Log.d(TAG, "Rally created: ${result.join_code}")
            Result.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create rally", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun joinRally(
        code: String,
        displayName: String
    ): Result<JoinRallyResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Joining rally with code: $code")
            
            val params = buildJsonObject {
                put("p_code", code.uppercase())
                put("p_device_id", androidId)
                put("p_platform", "android")
                put("p_display_name", displayName)
            }
            
            val result = supabase.postgrest.rpc(
                function = "join_rally",
                parameters = params
            ).decodeSingle<JoinRallyResult>()
            
            Log.d(TAG, "Joined rally: ${result.rally_id}")
            Result.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join rally", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun collectTree(
        rallyId: String,
        participantId: String,
        treeId: String,
        species: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String? = null,
        notes: String? = null
    ): Result<CollectTreeResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Collecting tree: $treeId in rally: $rallyId")
            
            val params = buildJsonObject {
                put("p_rally_id", rallyId)
                put("p_participant_id", participantId)
                put("p_tree_id", treeId)
                put("p_species", species)
                put("p_latitude", latitude)
                put("p_longitude", longitude)
                photoUrl?.let { put("p_photo_url", it) }
                notes?.let { put("p_notes", it) }
            }
            
            val result = supabase.postgrest.rpc(
                function = "collect_tree",
                parameters = params
            ).decodeSingle<CollectTreeResult>()
            
            Log.d(TAG, "Tree collected: ${result.collection_id}, new species: ${result.is_new_species}")
            Result.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect tree", e)
            Result.Error(e, e.message)
        }
    }
    
    suspend fun getRally(rallyId: String): Result<CrossplayRally> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching rally: $rallyId")
            
            val rally = supabase.from("rallies")
                .select(Columns.ALL) {
                    filter {
                        eq("id", rallyId)
                    }
                }
                .decodeSingle<CrossplayRally>()
            
            Log.d(TAG, "Rally fetched successfully:")
            Log.d(TAG, "  - ID: ${rally.id}")
            Log.d(TAG, "  - Code: ${rally.code}")
            Log.d(TAG, "  - Name: ${rally.name}")
            Log.d(TAG, "  - Creator Platform: ${rally.creator_platform}")
            Log.d(TAG, "  - Target Tree IDs: ${rally.target_tree_ids?.size ?: 0} trees")
            Log.d(TAG, "  - Target Tree IDs content: ${rally.target_tree_ids?.take(5)?.joinToString() ?: "null"}")
            
            Result.Success(rally)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch rally: ${e.message}", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun getRallyParticipants(rallyId: String): Result<List<RallyParticipant>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching participants for rally: $rallyId")
            
            val participants = supabase.from("rally_participants")
                .select(Columns.ALL) {
                    filter {
                        eq("rally_id", rallyId)
                        eq("is_active", true)
                    }
                }
                .decodeList<RallyParticipant>()
            
            Log.d(TAG, "Found ${participants.size} participants")
            Result.Success(participants)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch participants", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun getRallyCollections(rallyId: String): Result<List<TreeCollection>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching collections for rally: $rallyId")
            
            val collections = supabase.from("rally_collections")
                .select(Columns.ALL) {
                    filter {
                        eq("rally_id", rallyId)
                    }
                }
                .decodeList<TreeCollection>()
            
            Log.d(TAG, "Found ${collections.size} collections")
            Result.Success(collections)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch collections", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun getRallyStatistics(rallyId: String): Result<RallyStatistics> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching statistics for rally: $rallyId")
            
            val params = buildJsonObject {
                put("p_rally_id", rallyId)
            }
            
            val stats = supabase.postgrest.rpc(
                function = "get_rally_stats",
                parameters = params
            ).decodeSingle<RallyStatistics>()
            
            Log.d(TAG, "Statistics: ${stats.total_participants} participants, ${stats.total_unique_species} species")
            Result.Success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch statistics", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun updateParticipantActivity(participantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("rally_participants")
                .update({
                    set("last_active_at", "NOW()")
                }) {
                    filter {
                        eq("id", participantId)
                    }
                }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update participant activity", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun leaveRally(participantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Leaving rally, participant: $participantId")
            
            supabase.from("rally_participants")
                .update({
                    set("is_active", false)
                }) {
                    filter {
                        eq("id", participantId)
                    }
                }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave rally", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun finishRally(rallyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Finishing rally: $rallyId")
            
            supabase.from("rallies")
                .update({
                    set("status", "finished")
                    set("ended_at", "NOW()")
                }) {
                    filter {
                        eq("id", rallyId)
                        eq("creator_id", androidId)
                    }
                }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finish rally", e)
            Result.Error(e, e.message)
        }
    }

    suspend fun searchPublicRallies(): Result<List<CrossplayRally>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching public rallies")
            
            val rallies = supabase.from("rallies")
                .select(Columns.ALL) {
                    filter {
                        eq("is_public", true)
                        eq("status", "active")
                    }
                }
                .decodeList<CrossplayRally>()
            
            Log.d(TAG, "Found ${rallies.size} public rallies")
            Result.Success(rallies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search rallies", e)
            Result.Error(e, e.message)
        }
    }
}

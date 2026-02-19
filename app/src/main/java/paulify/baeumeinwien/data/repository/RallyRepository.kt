package paulify.baeumeinwien.data.repository

import paulify.baeumeinwien.data.local.RallyDao
import paulify.baeumeinwien.data.local.RallyEntity
import paulify.baeumeinwien.data.local.RallyProgressEntity
import paulify.baeumeinwien.data.domain.Rally
import paulify.baeumeinwien.data.domain.RallyProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

import paulify.baeumeinwien.data.remote.SupabaseInstance
import paulify.baeumeinwien.data.domain.RallyMode
import paulify.baeumeinwien.data.domain.RallyTask
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RallyRepository(
    private val rallyDao: RallyDao
) {
    private val gson = Gson()

    suspend fun createRally(
        name: String,
        targetTreeIds: List<String>,
        radiusMeters: Double,
        creatorName: String,
        mode: RallyMode = RallyMode.SCHOOL,
        tasks: List<RallyTask> = emptyList()
    ): Rally {
        val id = UUID.randomUUID().toString()
        val code = generateSimpleCode()
        val rally = Rally(
            id = id,
            code = if (mode == RallyMode.SOLO) "SOLO" else code,
            name = name,
            mode = mode,
            targetTreeIds = targetTreeIds,
            radiusMeters = radiusMeters,
            creatorName = creatorName,
            timestamp = System.currentTimeMillis(),
            tasks = tasks
        )
        
        val entity = RallyEntity(
            id = rally.id,
            code = rally.code,
            name = rally.name,
            mode = rally.mode.name,
            targetTreeIds = rally.targetTreeIds,
            radiusMeters = rally.radiusMeters,
            creatorName = rally.creatorName,
            timestamp = rally.timestamp,
            tasksJson = gson.toJson(rally.tasks)
        )
        rallyDao.insertRally(entity)
        
        return rally
    }

    suspend fun getRallyByCode(code: String): Rally? {
        try {
            val rally = SupabaseInstance.client.from("rallies")
                .select(Columns.ALL) { 
                    filter {
                        eq("code", code)
                    } 
                }.decodeSingleOrNull<Rally>()
            
            if (rally != null) {
                val entity = RallyEntity(
                    id = rally.id,
                    code = rally.code,
                    name = rally.name,
                    mode = rally.mode.name,
                    targetTreeIds = rally.targetTreeIds,
                    radiusMeters = rally.radiusMeters,
                    creatorName = rally.creatorName,
                    timestamp = rally.timestamp,
                    tasksJson = gson.toJson(rally.tasks)
                )
                rallyDao.insertRally(entity)
                return rally
            }
        } catch (e: Exception) {
            android.util.Log.e("RallyRepository", "Supabase rally fetch failed: ${e.message}", e)
        }
        
        return rallyDao.getRallyByCode(code)?.toDomain()
    }

    suspend fun updateProgress(
        rallyId: String,
        studentName: String,
        foundTreeIds: List<String>,
        taskAnswers: Map<String, String>,
        score: Int,
        completed: Boolean = false
    ) {
        val existing = rallyDao.getProgress(rallyId, studentName)
        val progress = RallyProgress(
            rallyId = rallyId,
            studentName = studentName,
            foundTreeIds = foundTreeIds,
            taskAnswers = taskAnswers,
            score = score,
            completed = completed,
            startTime = existing?.startTime ?: System.currentTimeMillis(),
            endTime = if (completed) System.currentTimeMillis() else null
        )
        
        val entity = RallyProgressEntity(
            id = existing?.id ?: 0,
            rallyId = progress.rallyId,
            studentName = progress.studentName,
            foundTreeIds = progress.foundTreeIds,
            taskAnswersJson = gson.toJson(progress.taskAnswers),
            score = progress.score,
            completed = progress.completed,
            startTime = progress.startTime,
            endTime = progress.endTime
        )
        rallyDao.insertProgress(entity)
        
        try {
            SupabaseInstance.client.from("rally_progress").upsert(progress)
        } catch (e: Exception) {
            android.util.Log.e("RallyRepository", "Failed to sync progress to Supabase", e)
        }
    }

    suspend fun deleteExpiredRallies() {
        val expiryTime = System.currentTimeMillis() - (72 * 60 * 60 * 1000)
        try {
            rallyDao.deleteOldRallies(expiryTime)
            
            SupabaseInstance.client.from("rallies").delete {
                filter {
                    lt("timestamp", expiryTime)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RallyRepository", "Failed to cleanup old rallies", e)
        }
    }

    fun saveSession(context: android.content.Context, code: String, name: String) {
        val prefs = context.getSharedPreferences("rally_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("current_code", code)
            putString("current_name", name)
            apply()
        }
    }

    fun getSession(context: android.content.Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences("rally_prefs", android.content.Context.MODE_PRIVATE)
        return Pair(prefs.getString("current_code", null), prefs.getString("current_name", null))
    }

    fun clearSession(context: android.content.Context) {
        val prefs = context.getSharedPreferences("rally_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        android.util.Log.d("RallyRepository", "Session cleared")
    }

    suspend fun fetchRemoteProgress(rallyId: String): List<RallyProgress> {
        return try {
            val results = SupabaseInstance.client.from("rally_progress")
                .select(Columns.ALL) {
                    filter {
                        eq("rallyId", rallyId)
                    }
                }.decodeList<RallyProgress>()
            
            results.forEach { progress ->
                val entity = RallyProgressEntity(
                    id = 0,
                    rallyId = progress.rallyId,
                    studentName = progress.studentName,
                    foundTreeIds = progress.foundTreeIds,
                    taskAnswersJson = gson.toJson(progress.taskAnswers),
                    score = progress.score,
                    completed = progress.completed,
                    startTime = progress.startTime,
                    endTime = progress.endTime
                )
                rallyDao.insertProgress(entity)
            }
            results
        } catch (e: Exception) {
            android.util.Log.e("RallyRepository", "Failed to fetch remote progress", e)
            emptyList()
        }
    }

    fun getAllProgressForRally(rallyId: String): Flow<List<RallyProgress>> {
        return rallyDao.getAllProgressForRally(rallyId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun generateSimpleCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun RallyEntity.toDomain(): Rally {
        val taskType = object : TypeToken<List<RallyTask>>() {}.type
        val tasks: List<RallyTask> = gson.fromJson(tasksJson, taskType) ?: emptyList()
        return Rally(
            id = id,
            code = code,
            name = name,
            mode = RallyMode.valueOf(mode),
            targetTreeIds = targetTreeIds,
            radiusMeters = radiusMeters,
            creatorName = creatorName,
            timestamp = timestamp,
            tasks = tasks
        )
    }

    private fun RallyProgressEntity.toDomain(): RallyProgress {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        val taskAnswers: Map<String, String> = gson.fromJson(taskAnswersJson, mapType) ?: emptyMap()
        return RallyProgress(
            rallyId = rallyId,
            studentName = studentName,
            foundTreeIds = foundTreeIds,
            taskAnswers = taskAnswers,
            score = score,
            completed = completed,
            startTime = startTime,
            endTime = endTime
        )
    }
}

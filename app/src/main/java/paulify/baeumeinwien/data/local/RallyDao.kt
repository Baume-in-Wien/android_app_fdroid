package paulify.baeumeinwien.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RallyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRally(rally: RallyEntity)

    @Query("SELECT * FROM rallies WHERE code = :code")
    suspend fun getRallyByCode(code: String): RallyEntity?

    @Query("SELECT * FROM rallies WHERE id = :id")
    suspend fun getRallyById(id: String): RallyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: RallyProgressEntity)

    @Query("SELECT * FROM rally_progress WHERE rallyId = :rallyId AND studentName = :studentName")
    suspend fun getProgress(rallyId: String, studentName: String): RallyProgressEntity?

    @Query("SELECT * FROM rally_progress WHERE rallyId = :rallyId")
    fun getAllProgressForRally(rallyId: String): Flow<List<RallyProgressEntity>>

    @Query("DELETE FROM rallies WHERE timestamp < :expiryTime")
    suspend fun deleteOldRallies(expiryTime: Long)

    @Query("DELETE FROM rally_progress WHERE startTime < :expiryTime")
    suspend fun deleteOldProgress(expiryTime: Long)
}

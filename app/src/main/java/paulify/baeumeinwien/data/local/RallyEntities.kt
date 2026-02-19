package paulify.baeumeinwien.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "rallies")
data class RallyEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val mode: String, // from RallyMode enum
    val targetTreeIds: List<String>,
    val radiusMeters: Double,
    val creatorName: String,
    val timestamp: Long,
    val tasksJson: String // Serialized List<RallyTask>
)

@Entity(tableName = "rally_progress")
data class RallyProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rallyId: String,
    val studentName: String,
    val foundTreeIds: List<String>,
    val taskAnswersJson: String, // Serialized Map<String, String>
    val score: Int,
    val completed: Boolean,
    val startTime: Long,
    val endTime: Long?
)

class RallyConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromIntSet(value: Set<Int>): String = gson.toJson(value.toList())
    
    @TypeConverter
    fun toIntSet(value: String): Set<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return (gson.fromJson<List<Int>>(value, type) ?: emptyList()).toSet()
    }
}

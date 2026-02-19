package paulify.baeumeinwien.data.domain

import paulify.baeumeinwien.data.local.TreeEntity
import kotlinx.serialization.Serializable

@Serializable
enum class RallyMode {
    SOLO,
    SCHOOL,
    STUDENT,
    TEACHER;
    
    fun toServerValue(): String = when(this) {
        SOLO -> "solo"
        SCHOOL, STUDENT -> "student"
        TEACHER -> "teacher"
    }
    
    companion object {
        fun fromServerValue(value: String): RallyMode = when(value.lowercase()) {
            "solo" -> SOLO
            "student" -> STUDENT
            "teacher" -> TEACHER
            "school" -> SCHOOL
            else -> STUDENT
        }
    }
}

@Serializable
data class Rally(
    val id: String,
    val code: String,
    val name: String,
    val mode: RallyMode = RallyMode.SCHOOL,
    val targetTreeIds: List<String>,
    val radiusMeters: Double,
    val creatorName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tasks: List<RallyTask> = emptyList()
)

@Serializable
data class RallyProgress(
    val rallyId: String,
    val studentName: String,
    val foundTreeIds: List<String> = emptyList(),
    val taskAnswers: Map<String, String> = emptyMap(),
    val score: Int = 0,
    val completed: Boolean = false,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

@Serializable
data class RallyTask(
    val treeId: String,
    val type: TaskType,
    val question: String,
    val options: List<String>? = null,
    val points: Int = 10
)

@Serializable
enum class TaskType {
    LEAF_PHOTO,
    TREE_PHOTO,
    SPECIES_ID,
    INFO_GATHER,
    OBSERVATION
}

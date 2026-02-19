package paulify.baeumeinwien.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_notes")
data class TreeNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treeId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

package paulify.baeumeinwien.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_photos")
data class TreePhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treeId: String,
    val uri: String,
    val timestamp: Long = System.currentTimeMillis()
)

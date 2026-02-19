package paulify.baeumeinwien.data.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val speciesGerman: String,
    val imageUrl: String,
    val unlockedAt: Long = System.currentTimeMillis()
)

package paulify.baeumeinwien.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import paulify.baeumeinwien.data.domain.Achievement

@Database(
    entities = [
        TreeEntity::class, 
        TreeNote::class, 
        TreePhoto::class, 
        RallyEntity::class, 
        RallyProgressEntity::class, 
        Achievement::class,
        AchievementProgressEntity::class,
        SpeciesDiscoveryCount::class,
        UserStatsEntity::class
    ],
    version = 6,
    exportSchema = false
)
@androidx.room.TypeConverters(RallyConverters::class)
abstract class TreeDatabase : RoomDatabase() {
    
    abstract fun treeDao(): TreeDao
    abstract fun rallyDao(): RallyDao
    abstract fun achievementDao(): AchievementDao
    
    companion object {
        @Volatile
        private var INSTANCE: TreeDatabase? = null
        
        fun getDatabase(context: Context): TreeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TreeDatabase::class.java,
                    "tree_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

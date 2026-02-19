package paulify.baeumeinwien.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SpeciesCount(
    val speciesGerman: String,
    val count: Int
)

data class DistrictCount(
    val district: Int,
    val count: Int
)

data class AgeRangeCount(
    val ageRange: String,
    val count: Int
)

@Dao
interface TreeDao {
    
    @Query("""
        SELECT * FROM trees
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
        ORDER BY id
        LIMIT 5000
    """)
    fun getTreesInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<TreeEntity>>
    
    @Query("""
        SELECT * FROM trees 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
        ORDER BY id
        LIMIT :limit
    """)
    suspend fun getTreesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int = 5000
    ): List<TreeEntity>
    
    @Query("""
        SELECT district, 
        COUNT(*) as treeCount, 
        AVG(latitude) as centerLat, 
        AVG(longitude) as centerLon 
        FROM trees 
        WHERE district IS NOT NULL 
        GROUP BY district
    """)
    suspend fun getDistrictClusters(): List<DistrictCluster>
    
    @Query("""
        SELECT * FROM trees 
        WHERE street LIKE '%' || :query || '%' 
        OR speciesGerman LIKE '%' || :query || '%'
        OR speciesScientific LIKE '%' || :query || '%'
        OR baumId LIKE '%' || :query || '%'
        LIMIT 50
    """)
    suspend fun searchTrees(query: String): List<TreeEntity>
    
    @Query("""
        SELECT * FROM trees
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
        ORDER BY id
        LIMIT 200
    """)
    fun getTreesNearby(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<TreeEntity>>
    
    @Query("SELECT * FROM trees WHERE isFavorite = 1")
    fun getFavoriteTrees(): Flow<List<TreeEntity>>
    
    @Query("SELECT * FROM trees WHERE id = :treeId")
    suspend fun getTreeById(treeId: String): TreeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrees(trees: List<TreeEntity>)
    
    @Update
    suspend fun updateTree(tree: TreeEntity)
    
    @Query("UPDATE trees SET isFavorite = :isFavorite WHERE id = :treeId")
    suspend fun updateFavoriteStatus(treeId: String, isFavorite: Boolean)
    
    @Query("DELETE FROM trees")
    suspend fun deleteAllTrees()
    
    @Query("SELECT id FROM trees WHERE isFavorite = 1")
    suspend fun getFavoriteTreeIds(): List<String>
    
    @Query("UPDATE trees SET isFavorite = 1 WHERE id IN (:treeIds)")
    suspend fun restoreFavorites(treeIds: List<String>)
    
    @Query("SELECT * FROM trees WHERE district = :district")
    fun getTreesByDistrict(district: Int): Flow<List<TreeEntity>>
    
    @Query("SELECT * FROM trees WHERE speciesGerman LIKE '%' || :species || '%'")
    fun getTreesBySpecies(species: String): Flow<List<TreeEntity>>
    
    @Query("SELECT COUNT(*) FROM trees")
    suspend fun getTreeCount(): Int
    
    @Query("""
        SELECT speciesGerman, COUNT(*) as count 
        FROM trees 
        WHERE speciesGerman IS NOT NULL 
        GROUP BY speciesGerman 
        ORDER BY count DESC 
        LIMIT 10
    """)
    suspend fun getTopSpecies(): List<SpeciesCount>
    
    @Query("""
        SELECT district, COUNT(*) as count 
        FROM trees 
        WHERE district IS NOT NULL 
        GROUP BY district 
        ORDER BY district
    """)
    suspend fun getTreesByDistrict(): List<DistrictCount>
    
    @Query("""
        SELECT 
            CASE 
                WHEN (2025 - plantYear) < 10 THEN '0-9 Jahre'
                WHEN (2025 - plantYear) < 20 THEN '10-19 Jahre'
                WHEN (2025 - plantYear) < 50 THEN '20-49 Jahre'
                WHEN (2025 - plantYear) < 100 THEN '50-99 Jahre'
                ELSE '100+ Jahre'
            END as ageRange,
            COUNT(*) as count
        FROM trees 
        WHERE plantYear IS NOT NULL 
        GROUP BY 
            CASE 
                WHEN (2025 - plantYear) < 10 THEN '0-9 Jahre'
                WHEN (2025 - plantYear) < 20 THEN '10-19 Jahre'
                WHEN (2025 - plantYear) < 50 THEN '20-49 Jahre'
                WHEN (2025 - plantYear) < 100 THEN '50-99 Jahre'
                ELSE '100+ Jahre'
            END
        ORDER BY ageRange
    """)
    suspend fun getTreesByAgeRange(): List<AgeRangeCount>

    @androidx.room.Query("SELECT * FROM tree_notes WHERE treeId = :treeId ORDER BY timestamp DESC")
    fun getNotesForTree(treeId: String): kotlinx.coroutines.flow.Flow<List<TreeNote>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TreeNote)
    
    @androidx.room.Delete
    suspend fun deleteNote(note: TreeNote)

    @androidx.room.Query("SELECT * FROM tree_photos WHERE treeId = :treeId ORDER BY timestamp DESC")
    fun getPhotosForTree(treeId: String): kotlinx.coroutines.flow.Flow<List<TreePhoto>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: TreePhoto)
    
    @androidx.room.Delete
    suspend fun deletePhoto(photo: TreePhoto)
}

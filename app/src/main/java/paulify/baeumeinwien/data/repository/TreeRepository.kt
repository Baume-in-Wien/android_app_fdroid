package paulify.baeumeinwien.data.repository

import paulify.baeumeinwien.data.domain.Tree
import paulify.baeumeinwien.data.domain.Achievement
import paulify.baeumeinwien.data.local.AchievementDao
import paulify.baeumeinwien.data.local.AgeRangeCount
import paulify.baeumeinwien.data.local.TreeNote
import paulify.baeumeinwien.data.local.TreePhoto
import paulify.baeumeinwien.data.local.DistrictCluster
import paulify.baeumeinwien.data.local.DistrictCount
import paulify.baeumeinwien.data.local.SpeciesCount
import paulify.baeumeinwien.data.local.TreeDao
import paulify.baeumeinwien.data.local.toEntity
import paulify.baeumeinwien.data.local.toTree
import paulify.baeumeinwien.data.remote.BaumkatasterApi
import paulify.baeumeinwien.data.remote.SupabaseInstance
import paulify.baeumeinwien.data.remote.dto.SupabaseBaumkatasterRow
import paulify.baeumeinwien.data.remote.dto.toTrees
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.edit
import java.io.IOException

class TreeRepository(
    private val api: BaumkatasterApi,
    private val dao: TreeDao,
    private val achievementDao: AchievementDao,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) {
    
    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()
    
    private val loadedDistricts = mutableSetOf<Int>()
    private val loadingDistricts = mutableSetOf<Int>()
    
    companion object {
        private val DATA_VERSION_KEY = androidx.datastore.preferences.core.stringPreferencesKey("data_version")
        private val LAST_UPDATE_CHECK_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_update_check")
        private val LOADED_DISTRICTS_KEY = androidx.datastore.preferences.core.stringPreferencesKey("loaded_districts")
        
        val DISTRICT_BOUNDS = mapOf(
            1 to BoundingBoxData(48.199, 48.220, 16.355, 16.385),
            2 to BoundingBoxData(48.200, 48.245, 16.380, 16.450),
            3 to BoundingBoxData(48.185, 48.210, 16.375, 16.420),
            4 to BoundingBoxData(48.185, 48.200, 16.355, 16.380),
            5 to BoundingBoxData(48.178, 48.195, 16.345, 16.365),
            6 to BoundingBoxData(48.185, 48.200, 16.335, 16.360),
            7 to BoundingBoxData(48.195, 48.210, 16.335, 16.355),
            8 to BoundingBoxData(48.205, 48.215, 16.335, 16.355),
            9 to BoundingBoxData(48.215, 48.235, 16.345, 16.375),
            10 to BoundingBoxData(48.140, 48.185, 16.340, 16.430),
            11 to BoundingBoxData(48.150, 48.200, 16.400, 16.500),
            12 to BoundingBoxData(48.155, 48.190, 16.280, 16.350),
            13 to BoundingBoxData(48.155, 48.205, 16.220, 16.290),
            14 to BoundingBoxData(48.185, 48.225, 16.210, 16.290),
            15 to BoundingBoxData(48.185, 48.205, 16.310, 16.345),
            16 to BoundingBoxData(48.200, 48.230, 16.290, 16.340),
            17 to BoundingBoxData(48.220, 48.250, 16.300, 16.350),
            18 to BoundingBoxData(48.220, 48.255, 16.310, 16.365),
            19 to BoundingBoxData(48.235, 48.280, 16.320, 16.400),
            20 to BoundingBoxData(48.230, 48.250, 16.360, 16.400),
            21 to BoundingBoxData(48.250, 48.330, 16.370, 16.500),
            22 to BoundingBoxData(48.200, 48.290, 16.430, 16.580),
            23 to BoundingBoxData(48.110, 48.175, 16.220, 16.340)
        )
    }
    
    data class BoundingBoxData(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)
    
    init {
        kotlinx.coroutines.runBlocking {
            val savedDistricts = dataStore.data.first()[LOADED_DISTRICTS_KEY]
            savedDistricts?.split(",")?.mapNotNull { it.toIntOrNull() }?.let {
                loadedDistricts.addAll(it)
            }
        }
    }
    
    fun getDistrictsInViewport(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<Int> {
        return DISTRICT_BOUNDS.filter { (_, bounds) ->
            bounds.minLat <= maxLat && bounds.maxLat >= minLat &&
            bounds.minLon <= maxLon && bounds.maxLon >= minLon
        }.keys.toList()
    }
    
    suspend fun loadTreesForViewport(
        minLat: Double, 
        maxLat: Double, 
        minLon: Double, 
        maxLon: Double,
        forceRefresh: Boolean = false
    ): Result<List<Tree>> {
        val visibleDistricts = getDistrictsInViewport(minLat, maxLat, minLon, maxLon)
        android.util.Log.d("TreeRepository", "Visible districts: $visibleDistricts")
        
        val districtsToLoad = if (forceRefresh) {
            visibleDistricts
        } else {
            visibleDistricts.filter { it !in loadedDistricts && it !in loadingDistricts }
        }
        
        if (districtsToLoad.isNotEmpty()) {
            android.util.Log.d("TreeRepository", "Loading districts: $districtsToLoad")
            
            for (district in districtsToLoad) {
                try {
                    loadingDistricts.add(district)
                    val result = loadDistrictTrees(district)
                    if (result is Result.Success) {
                        loadedDistricts.add(district)
                        saveLoadedDistricts()
                    }
                    loadingDistricts.remove(district)
                } catch (e: Exception) {
                    android.util.Log.e("TreeRepository", "Failed to load district $district: ${e.message}")
                    loadingDistricts.remove(district)
                }
            }
        }
        
        val trees = getTreesInBounds(minLat, maxLat, minLon, maxLon, limit = 5000)
        return Result.Success(trees)
    }
    
    suspend fun loadDistrictTrees(district: Int): Result<Unit> {
        return try {
            android.util.Log.d("TreeRepository", "Downloading district $district from CDN...")
            val startTime = System.currentTimeMillis()
            
            val response = api.getTreesByDistrict(district)
            val trees = response.toTrees()
            
            android.util.Log.d("TreeRepository", "District $district: ${trees.size} trees downloaded in ${System.currentTimeMillis() - startTime}ms")
            
            if (trees.isNotEmpty()) {
                insertTreesPreservingFavorites(trees)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Error loading district $district", e)
            Result.Error(e, "Fehler beim Laden von Bezirk $district")
        }
    }
    
    private suspend fun saveLoadedDistricts() {
        dataStore.edit { prefs ->
            prefs[LOADED_DISTRICTS_KEY] = loadedDistricts.joinToString(",")
        }
    }
    
    suspend fun clearLoadedDistricts() {
        loadedDistricts.clear()
        dataStore.edit { prefs ->
            prefs.remove(LOADED_DISTRICTS_KEY)
        }
    }
    
    private suspend fun insertTreesPreservingFavorites(trees: List<Tree>) {
        if (trees.isEmpty()) return
        
        val treeIdsToInsert = trees.map { it.id }.toSet()
        
        val existingFavoriteIds = dao.getFavoriteTreeIds().filter { it in treeIdsToInsert }
        
        dao.insertTrees(trees.map { it.toEntity() })
        
        if (existingFavoriteIds.isNotEmpty()) {
            dao.restoreFavorites(existingFavoriteIds)
            android.util.Log.d("TreeRepository", "Restored ${existingFavoriteIds.size} favorites after insert")
        }
    }
    
    fun getAllAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements()
    }
    
    suspend fun unlockAchievement(species: String, imageUrl: String) {
        val achievement = Achievement(species, imageUrl)
        achievementDao.insert(achievement)
    }
    
    suspend fun hasAchievement(species: String): Boolean {
        return achievementDao.getAchievementBySpecies(species) != null
    }
    
    suspend fun getTreesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int = 5000
    ): List<Tree> {
        return dao.getTreesInBounds(minLat, maxLat, minLon, maxLon, limit).map { it.toTree() }
    }
    
    suspend fun getDistrictClusters(): List<DistrictCluster> {
        return dao.getDistrictClusters()
    }


    fun getNearbyTrees(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double = 100.0
    ): Flow<List<Tree>> {
        val bounds = calculateBoundingBox(centerLat, centerLon, radiusMeters)
        return dao.getTreesInBoundingBox(
            bounds.minLat, bounds.maxLat,
            bounds.minLon, bounds.maxLon
        ).map { entities -> entities.map { it.toTree() } }
    }
    
    fun getTreesForAr(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double = 100.0
    ): Flow<List<Tree>> {
        val bounds = calculateBoundingBox(centerLat, centerLon, radiusMeters)
        return dao.getTreesNearby(
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            minLon = bounds.minLon,
            maxLon = bounds.maxLon
        ).map { entities -> entities.map { it.toTree() } }
    }
    
    suspend fun getTreesNearby(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double = 100.0
    ): List<Tree> {
        val bounds = calculateBoundingBox(centerLat, centerLon, radiusMeters)
        return dao.getTreesNearby(
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            minLon = bounds.minLon,
            maxLon = bounds.maxLon
        ).first().map { it.toTree() }
    }
    
    fun getFavoriteTrees(): Flow<List<Tree>> {
        return dao.getFavoriteTrees().map { entities ->
            entities.map { it.toTree() }
        }
    }

    suspend fun searchTrees(query: String): List<Tree> {
        if (query.isBlank() || query.length < 2) return emptyList()
        
        android.util.Log.d("TreeRepository", "Searching for: '$query'")
        
        val localResults = dao.searchTrees(query).map { it.toTree() }
        android.util.Log.d("TreeRepository", "Local search found ${localResults.size} results")
        
        if (localResults.isNotEmpty()) {
            return localResults
        }
        
        return try {
            android.util.Log.d("TreeRepository", "No local results, trying Supabase search...")
            searchTreesSupabase(query)
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Supabase search failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchTreesSupabase(
        query: String,
        district: Int? = null,
        limit: Int = 50
    ): List<Tree> {
        return try {
            val supabase = SupabaseInstance.client
            
            android.util.Log.d("TreeRepository", "Supabase search starting for: '$query', district: $district")
            
            val result = supabase.from("Baumkataster")
                .select(Columns.raw("BAUM_ID, OBJEKT_STRASSE, GATTUNG_ART, PFLANZJAHR, BEZIRK, SHAPE")) {
                    filter {
                        or {
                            ilike("OBJEKT_STRASSE", "%$query%")
                            ilike("OBJEKT_STRASSE", "% $query%")
                            ilike("GATTUNG_ART", "%$query%")
                        }
                        district?.let {
                            eq("BEZIRK", it)
                        }
                    }
                    limit(limit.toLong())
                }
                .decodeList<SupabaseBaumkatasterRow>()
            
            android.util.Log.d("TreeRepository", "Supabase search returned ${result.size} results for '$query'")
            
            if (result.isEmpty()) {
                android.util.Log.w("TreeRepository", "No Supabase results - data might have district prefixes like '05 $query'")
            }
            
            result.mapNotNull { row ->
                try {
                    val coords = parseShapeCoordinates(row.shape)
                    Tree(
                        id = row.baumId,
                        speciesScientific = row.gattungArt,
                        speciesGerman = row.gattungArt ?: "Unbekannt",
                        street = row.objektStrasse,
                        latitude = coords?.first ?: 0.0,
                        longitude = coords?.second ?: 0.0,
                        plantYear = row.pflanzjahr,
                        district = row.bezirk,
                        trunkCircumference = null,
                        height = null,
                        crownDiameter = null,
                        area = null,
                        fid = null,
                        objectId = null,
                        baumId = row.baumId,
                        datenfuehrung = null,
                        isFavorite = false
                    )
                } catch (e: Exception) {
                    android.util.Log.e("TreeRepository", "Error parsing row: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Supabase search error: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun searchTreesInBounds(
        query: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int = 100
    ): List<Tree> {
        val allResults = searchTreesSupabase(query, limit = limit * 2)
        return allResults.filter { tree ->
            tree.latitude in minLat..maxLat && tree.longitude in minLon..maxLon
        }.take(limit)
    }
    
    private fun parseShapeCoordinates(shape: String?): Pair<Double, Double>? {
        if (shape.isNullOrBlank()) return null
        
        return try {
            val pointRegex = """POINT\s*\(\s*([0-9.]+)\s+([0-9.]+)\s*\)""".toRegex(RegexOption.IGNORE_CASE)
            val match = pointRegex.find(shape)
            if (match != null) {
                val lon = match.groupValues[1].toDouble()
                val lat = match.groupValues[2].toDouble()
                Pair(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchTreesOnline(query: String): List<Tree> {
        return try {
            val supabase = SupabaseInstance.client
            var trees = try {
                supabase.from("trees")
                    .select(Columns.ALL) {
                        filter {
                            or {
                                ilike("street", "%$query%")
                                ilike("art_dtsch", "%$query%")
                            }
                        }
                    }.decodeList<Tree>()
            } catch (e: Exception) {
                supabase.from("trees")
                    .select(Columns.ALL) {
                        filter {
                            or {
                                ilike("street", "%$query%")
                                ilike("art_dtsch", "%$query%")
                            }
                        }
                    }.decodeList<Tree>()
            }
            
            if (trees.isNotEmpty()) {
                dao.insertTrees(trees.map { it.toEntity() })
                trees
            } else {
                searchTreesWfs(query)
            }
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Online search failed: ${e.message}")
            searchTreesWfs(query)
        }
    }

    private suspend fun searchTreesWfs(query: String): List<Tree> {
        return try {
            val baseUrl = "https://data.wien.gv.at/daten/geo"
            val params = mapOf(
                "service" to "WFS",
                "version" to "1.1.0",
                "request" to "GetFeature",
                "typeName" to "ogdwien:BAUMKATOGD",
                "srsName" to "EPSG:4326",
                "outputFormat" to "json",
                "cql_filter" to "STRASSE ILIKE '%$query%' OR ART_DT ILIKE '%$query%'"
            )
            
            val response = api.getTreesWfs(baseUrl, params)
            val trees = response.toTrees()
            
            dao.insertTrees(trees.map { it.toEntity() })
            trees
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "WFS search failed", e)
            emptyList()
        }
    }
    
    suspend fun getTopSpecies(): List<SpeciesCount> {
        return dao.getTopSpecies()
    }
    
    suspend fun getTreesByDistrict(): List<DistrictCount> {
        return dao.getTreesByDistrict()
    }
    
    suspend fun getTreesByAgeRange(): List<AgeRangeCount> {
        return dao.getTreesByAgeRange()
    }
    
    suspend fun downloadAllTrees(bbox: String): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            
            android.util.Log.d("TreeRepository", "Starting CDN download from: https://pub-5061dbde1e5d428583b6722a65924e3c.r2.dev/BAUMKATOGD.json")
            
            _downloadProgress.value = DownloadProgress(
                downloadedTrees = 0,
                totalTrees = 600000,
                currentBatch = 0,
                totalBatches = 1,
                elapsedTimeMs = 0,
                estimatedRemainingMs = 0
            )
            
            android.util.Log.d("TreeRepository", "Downloading complete tree data from CDN...")
            
            val trees = try {
                val response = api.getTrees()
                android.util.Log.d("TreeRepository", "CDN response received with ${response.features.size} features")
                response.toTrees()
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("TreeRepository", "OOM during download/parsing", e)
                return Result.Error(Exception("OOM_ERROR"))
            } catch (e: Exception) {
                android.util.Log.e("TreeRepository", "Download failed", e)
                return Result.Error(e)
            }
            
            android.util.Log.d("TreeRepository", "Converted to ${trees.size} valid trees")

            if (trees.size >= 5) {
                android.util.Log.d("TreeRepository", "--- COORDINATE SANITY CHECK ---")
                for (i in 0..4) {
                    android.util.Log.d("TreeRepository", "Tree ${i + 1}: Lat=${trees[i].latitude}, Lon=${trees[i].longitude}")
                }
                android.util.Log.d("TreeRepository", "-------------------------------")
            }
            
            val batchSize = 2000
            val chunks = trees.chunked(batchSize)
            
            chunks.forEachIndexed { index, batch ->
                try {
                    dao.insertTrees(batch.map { it.toEntity() })
                    
                    val progress = (index + 1) * batchSize
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val progressPercent = (progress.toFloat() / trees.size * 100).toInt()
                    
                    _downloadProgress.value = DownloadProgress(
                        downloadedTrees = minOf(progress, trees.size),
                        totalTrees = trees.size,
                        currentBatch = index + 1,
                        totalBatches = chunks.size,
                        elapsedTimeMs = elapsedTime,
                        estimatedRemainingMs = if (progressPercent > 0) {
                            (elapsedTime * (100 - progressPercent)) / progressPercent
                        } else 0
                    )
                    
                    kotlinx.coroutines.yield()
                    
                } catch (e: Exception) {
                    android.util.Log.e("TreeRepository", "Error inserting batch ${index + 1}", e)
                }
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            android.util.Log.d("TreeRepository", "Download complete: ${trees.size} trees in ${totalTime}ms (${totalTime/1000}s)")

            val dbCount = dao.getTreeCount()
            android.util.Log.d("TreeRepository", "Verification: Total trees in database after download: $dbCount")
            
            try {
                val version = api.getVersion()
                saveVersion(version.version)
                android.util.Log.d("TreeRepository", "Saved data version: ${version.version}")
            } catch (e: Exception) {
                android.util.Log.w("TreeRepository", "Could not save version after download", e)
            }
            
            Result.Success(Unit)
        } catch (e: IOException) {
            android.util.Log.e("TreeRepository", "Network error during CDN download", e)
            Result.Error(
                exception = e,
                message = "Netzwerkfehler beim Download vom CDN"
            )
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Error during CDN download", e)
            Result.Error(
                exception = e,
                message = "Fehler beim Download: ${e.message}"
            )
        }
    }
    
    suspend fun refreshTrees(bbox: String? = null): Result<Unit> {
        return try {
            android.util.Log.d("TreeRepository", "Starting tree data refresh with bbox: $bbox")
            
            val response = if (bbox != null) {
                val wfsUrl = "https://data.wien.gv.at/daten/geo"
                val params = mapOf(
                    "service" to "WFS",
                    "version" to "1.0.0",
                    "request" to "GetFeature",
                    "typeName" to "ogdwien:BAUMKATOGD",
                    "srsName" to "EPSG:4326",
                    "outputFormat" to "json",
                    "bbox" to bbox
                )
                api.getTreesWfs(wfsUrl, params)
            } else {
                api.getTrees()
            }
            
            android.util.Log.d("TreeRepository", "API response received with ${response.features.size} features")
            
            val trees = response.toTrees()
            android.util.Log.d("TreeRepository", "Converted to ${trees.size} valid trees")
            
            if (trees.isEmpty()) {
                android.util.Log.w("TreeRepository", "No trees parsed from response")
                 return Result.Success(Unit) 
            }
            
            try {
                val existingFavorites = dao.getFavoriteTrees().first().map { it.id }.toSet()
                
                val treesToInsert = trees.map { tree ->
                    tree.copy(isFavorite = existingFavorites.contains(tree.id))
                }
                
                dao.insertTrees(treesToInsert.map { it.toEntity() })
                android.util.Log.d("TreeRepository", "Successfully cached ${trees.size} trees (${existingFavorites.size} favorites preserved)")
            } catch (e: Exception) {
                android.util.Log.e("TreeRepository", "DB Error during refresh insert", e)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Error during refresh", e)
            Result.Error(
                exception = e,
                message = "Aktualisierung fehlgeschlagen: ${e.message}"
            )
        }
    }
    
    suspend fun getTreeById(treeId: String): Tree? {
        return dao.getTreeById(treeId)?.toTree()
    }
    
    suspend fun getTreeByIdOnline(treeId: String): Tree? {
        return try {
            val supabase = SupabaseInstance.client
            val trees = supabase.from("trees")
                .select(Columns.ALL) {
                    filter {
                        eq("id", treeId)
                    }
                }.decodeList<Tree>()
            
            if (trees.isNotEmpty()) {
                val tree = trees.first()
                dao.insertTrees(listOf(tree.toEntity()))
                tree
            } else {
                getTreeByIdFromWfs(treeId)
            }
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Online tree fetch failed for $treeId: ${e.message}")
            getTreeByIdFromWfs(treeId)
        }
    }
    
    private suspend fun getTreeByIdFromWfs(treeId: String): Tree? {
        return try {
            val baseUrl = "https://data.wien.gv.at/daten/geo"
            val params = mapOf(
                "service" to "WFS",
                "version" to "1.1.0",
                "request" to "GetFeature",
                "typeName" to "ogdwien:BAUMKATOGD",
                "srsName" to "EPSG:4326",
                "outputFormat" to "json",
                "cql_filter" to "FID='$treeId'"
            )
            
            val response = api.getTreesWfs(baseUrl, params)
            val trees = response.toTrees()
            
            if (trees.isNotEmpty()) {
                val tree = trees.first()
                dao.insertTrees(listOf(tree.toEntity()))
                tree
            } else null
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "WFS tree fetch failed for $treeId", e)
            null
        }
    }
    
    suspend fun getTreesByIdsOnline(treeIds: List<String>, districtHint: List<Int>? = null): List<Tree> {
        if (treeIds.isEmpty()) return emptyList()
        
        return try {
            val normalizedIds = treeIds.flatMap { id ->
                if (id.startsWith("BAUM_")) {
                    listOf(id, id.removePrefix("BAUM_"))
                } else {
                    listOf(id, "BAUM_$id")
                }
            }.distinct()
            
            android.util.Log.d("TreeRepository", "Normalized ${treeIds.size} IDs to ${normalizedIds.size} variants")
            
        val foundTrees = mutableListOf<Tree>()
        
        for (id in normalizedIds) {
            val tree = dao.getTreeById(id)?.toTree()
            if (tree != null) {
                foundTrees.add(tree)
                android.util.Log.d("TreeRepository", "Found tree $id in local cache")
            }
        }
        
        fun getMissingOriginalIds(): List<String> {
            val foundSet = foundTrees.map { it.id }.toSet()
            return treeIds.filter { id ->
                val num = if (id.startsWith("BAUM_")) id.removePrefix("BAUM_") else id
                !foundSet.contains(num) && !foundSet.contains("BAUM_$num") && !foundSet.contains(id)
            }
        }
        
        var missingOriginals = getMissingOriginalIds()
        
        if (missingOriginals.isEmpty()) {
            android.util.Log.d("TreeRepository", "Found ALL ${treeIds.size} requested trees in local cache")
            return foundTrees.distinctBy { it.id }
        }
        
        android.util.Log.d("TreeRepository", "Found ${foundTrees.size} local, missing ${missingOriginals.size} trees: $missingOriginals")
        
        if (districtHint != null && districtHint.isNotEmpty()) {
            android.util.Log.d("TreeRepository", "Trying CDN download for districts: $districtHint")
            
            for (district in districtHint) {
                try {
                    val trees = fetchTreesFromCdn(district)
                    
                    val matching = trees.filter { tree -> 
                         normalizedIds.contains(tree.id) || normalizedIds.contains("BAUM_${tree.id}")
                    }
                    
                    if (matching.isNotEmpty()) {
                        android.util.Log.d("TreeRepository", "Found ${matching.size} trees in CDN data for district $district")
                        foundTrees.addAll(matching)
                        
                        dao.insertTrees(matching.map { it.toEntity() })
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TreeRepository", "Failed to load CDN data for district $district", e)
                }
            }
        }
        
        missingOriginals = getMissingOriginalIds()
        if (missingOriginals.isEmpty()) {
            android.util.Log.d("TreeRepository", "Found ALL missing trees via CDN")
            return foundTrees.distinctBy { it.id }
        }

        android.util.Log.d("TreeRepository", "Still missing ${missingOriginals.size} trees, trying WFS API...")
        
        val foundSet = foundTrees.map { it.id }.toSet()
        val remainingNumericIds = normalizedIds.filter { 
             !foundSet.contains(it) && it.all { c -> c.isDigit() }
        }
        
        var newTrees: List<Tree> = emptyList()
        
        if (remainingNumericIds.isNotEmpty()) {
            val idList = remainingNumericIds.joinToString(",")
            val cqlFilter = "OBJECTID IN ($idList)"
            
            android.util.Log.d("TreeRepository", "Trying OBJECTID filter: $cqlFilter")
            newTrees = fetchTreesFromWfs(cqlFilter)
            android.util.Log.d("TreeRepository", "OBJECTID query returned ${newTrees.size} trees")
            
            if (newTrees.isNotEmpty()) {
                 foundTrees.addAll(newTrees)
            }
        }
        
        if (newTrees.isEmpty() && remainingNumericIds.isNotEmpty()) {
            val idList = remainingNumericIds.joinToString(",") { "'$it'" } 
            val cqlFilter = "BAUM_ID IN ($idList)"
            android.util.Log.d("TreeRepository", "Trying BAUM_ID filter (quoted): $cqlFilter")
            newTrees = fetchTreesFromWfs(cqlFilter)
            
            if (newTrees.isNotEmpty()) {
                 foundTrees.addAll(newTrees)
            }
        }
        
        if (newTrees.isEmpty() && remainingNumericIds.isNotEmpty()) {
            val featureIds = remainingNumericIds.joinToString(",") { "BAUMKATOGD.$it" }
            android.util.Log.d("TreeRepository", "Trying featureID fetch: $featureIds")
            newTrees = fetchTreesFromWfs("FEATURE_ID_MAGIC:$featureIds")
            
            if (newTrees.isNotEmpty()) {
                 foundTrees.addAll(newTrees)
            }
        }
        
        if (newTrees.isNotEmpty()) {
            dao.insertTrees(newTrees.map { it.toEntity() })
        }
        
        return foundTrees.distinctBy { it.id }
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Batch online fetch failed", e)
            emptyList()
        }
    }
    
    private suspend fun fetchTreesFromCdn(district: Int): List<Tree> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = "https://pub-5061dbde1e5d428583b6722a65924e3c.r2.dev/BAUMKATOGD_$district.json"
            android.util.Log.d("TreeRepository", "Downloading CDN data: $url")
            
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                android.util.Log.e("TreeRepository", "CDN HTTP error: ${response.code}")
                return@withContext emptyList<Tree>()
            }
            
            val body = response.body?.string() ?: ""
            if (body.isEmpty()) return@withContext emptyList<Tree>()
            
            val json = org.json.JSONObject(body)
            val features = json.optJSONArray("features") ?: return@withContext emptyList<Tree>()
            
            val trees = mutableListOf<Tree>()
            for (i in 0 until features.length()) {
                try {
                    val feature = features.getJSONObject(i)
                    val props = feature.getJSONObject("properties")
                    val geometry = feature.optJSONObject("geometry")
                    val coords = geometry?.optJSONArray("coordinates")
                    
                    val lat = coords?.optDouble(1) ?: 0.0
                    val lon = coords?.optDouble(0) ?: 0.0
                    
                    val tree = Tree(
                        id = props.optString("OBJECTID", ""),
                        baumId = props.optString("BAUM_ID", ""),
                        latitude = lat,
                        longitude = lon,
                        speciesGerman = props.optString("GATTUNG_ART", "Unbekannt"),
                        speciesScientific = props.optString("GATTUNG_ART_BOTANISCH", null),
                        height = props.optDouble("BAUMHOEHE", 0.0),
                        crownDiameter = props.optDouble("KRONENDURCHMESSER", 0.0),
                        trunkCircumference = props.optInt("STAMMUMFANG", 0),
                        street = props.optString("STRASSE", null),
                        district = props.optInt("BEZIRK", 0),
                        plantYear = props.optInt("PFLANZJAHR", 0),
                        area = props.optString("KATEGORIE", null),
                        fid = props.optString("FID", null),
                        objectId = props.optString("OBJECTID", null),
                        datenfuehrung = props.optString("DATENFUEHRUNG", null)
                    )
                    
                    
                    if (tree.id.isNotEmpty()) trees.add(tree)
                } catch (e: Exception) {
                    continue
                }
            }
            
            android.util.Log.d("TreeRepository", "Parsed ${trees.size} trees from CDN for district $district")
            trees
        }
    }
    
    private suspend fun fetchTreesFromWfs(filter: String): List<Tree> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val baseUrl = "https://data.wien.gv.at/daten/geo"
                var url = ""
                
                if (filter.startsWith("FEATURE_ID_MAGIC:")) {
                    val fids = filter.removePrefix("FEATURE_ID_MAGIC:")
                    url = "$baseUrl?service=WFS&version=1.1.0&request=GetFeature&typeName=ogdwien:BAUMKATOGD&srsName=EPSG:4326&outputFormat=json&featureId=$fids"
                } else {
                    val encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8")
                    url = "$baseUrl?service=WFS&version=1.1.0&request=GetFeature&typeName=ogdwien:BAUMKATOGD&srsName=EPSG:4326&outputFormat=json&cql_filter=$encodedFilter"
                }
                
                android.util.Log.d("TreeRepository", "WFS URL: $url")
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    android.util.Log.e("TreeRepository", "WFS HTTP error: ${response.code}")
                    return@withContext emptyList<Tree>()
                }
                
                val body = response.body?.string() ?: ""
                
                if (body.isEmpty()) {
                    android.util.Log.w("TreeRepository", "WFS returned empty response")
                    return@withContext emptyList<Tree>()
                }
                
                if (body.trimStart().startsWith("<")) {
                    android.util.Log.e("TreeRepository", "WFS returned HTML error page instead of JSON")
                    return@withContext emptyList<Tree>()
                }
                
                val json = org.json.JSONObject(body)
                val features = json.optJSONArray("features") ?: return@withContext emptyList<Tree>()
                
                val trees = mutableListOf<Tree>()
                for (i in 0 until features.length()) {
                    try {
                        val feature = features.getJSONObject(i)
                        val props = feature.getJSONObject("properties")
                        val geometry = feature.optJSONObject("geometry")
                        
                        val coords = geometry?.optJSONArray("coordinates")
                        val lat = coords?.optDouble(1) ?: 0.0
                        val lon = coords?.optDouble(0) ?: 0.0
                        
                        val tree = Tree(
                            id = props.optString("OBJECTID", ""),
                            baumId = props.optString("BAUM_ID", ""),
                            latitude = lat,
                            longitude = lon,
                            speciesGerman = props.optString("GATTUNG_ART", "Unbekannt"),
                            speciesScientific = props.optString("GATTUNG_ART_BOTANISCH", null),
                            height = props.optDouble("BAUMHOEHE", 0.0),
                            crownDiameter = props.optDouble("KRONENDURCHMESSER", 0.0),
                            trunkCircumference = props.optInt("STAMMUMFANG", 0),
                            street = props.optString("STRASSE", null),
                            district = props.optInt("BEZIRK", 0),
                            plantYear = props.optInt("PFLANZJAHR", 0),
                            area = props.optString("KATEGORIE", null),
                            fid = props.optString("FID", null),
                            objectId = props.optString("OBJECTID", null),
                            datenfuehrung = props.optString("DATENFUEHRUNG", null)
                        )
                        if (tree.id.isNotEmpty()) trees.add(tree)
                    } catch (e: Exception) {
                        android.util.Log.w("TreeRepository", "Failed to parse tree feature", e)
                    }
                }
                
                android.util.Log.d("TreeRepository", "Parsed ${trees.size} trees from WFS response")
                trees
            } catch (e: Exception) {
                android.util.Log.e("TreeRepository", "WFS fetch failed", e)
                emptyList()
            }
        }
    }

    suspend fun toggleFavorite(treeId: String) {
        val tree = dao.getTreeById(treeId) ?: return
        dao.updateFavoriteStatus(treeId, !tree.isFavorite)
    }
    
    suspend fun setFavorite(treeId: String, isFavorite: Boolean) {
        dao.updateFavoriteStatus(treeId, isFavorite)
    }
    
    suspend fun getTreeCount(): Int {
        return dao.getTreeCount()
    }
    
    suspend fun clearCache() {
        dao.deleteAllTrees()
    }

    fun getNotesForTree(treeId: String) = dao.getNotesForTree(treeId)
    
    suspend fun addNote(treeId: String, content: String) {
        dao.insertNote(TreeNote(treeId = treeId, content = content))
    }

    fun getPhotosForTree(treeId: String) = dao.getPhotosForTree(treeId)

    suspend fun addPhoto(treeId: String, uri: String) {
        dao.insertPhoto(TreePhoto(treeId = treeId, uri = uri))
    }

    suspend fun deleteNote(note: TreeNote) {
        dao.deleteNote(note)
    }

    suspend fun deletePhoto(photo: TreePhoto) {
        dao.deletePhoto(photo)
    }
    
    suspend fun checkForUpdates(): paulify.baeumeinwien.data.remote.dto.DataVersion? {
        return try {
            val remoteVersion = api.getVersion()
            val currentVersion = getCurrentVersion()
            
            dataStore.edit { prefs ->
                prefs[LAST_UPDATE_CHECK_KEY] = System.currentTimeMillis()
            }
            
            if (currentVersion == null || remoteVersion.version != currentVersion) {
                remoteVersion
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("TreeRepository", "Error checking for updates", e)
            null
        }
    }
    
    suspend fun getCurrentVersion(): String? {
        return dataStore.data.map { prefs ->
            prefs[DATA_VERSION_KEY]
        }.first()
    }
    
    private suspend fun saveVersion(version: String) {
        dataStore.edit { prefs ->
            prefs[DATA_VERSION_KEY] = version
        }
    }
}

private data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

private fun calculateBoundingBox(
    centerLat: Double,
    centerLon: Double,
    radiusMeters: Double
): BoundingBox {
    val latDelta = radiusMeters / 111000.0
    val lonDelta = radiusMeters / (111000.0 * kotlin.math.cos(Math.toRadians(centerLat)))

    return BoundingBox(
        minLat = centerLat - latDelta,
        maxLat = centerLat + latDelta,
        minLon = centerLon - lonDelta,
        maxLon = centerLon + lonDelta
    )
}
package paulify.baeumeinwien.data.statistics

import android.util.Log
import paulify.baeumeinwien.data.remote.SupabaseInstance
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TreeStatisticsRepository {
    
    private val supabase = SupabaseInstance.client
    
    sealed class StatisticsResult<out T> {
        data class Success<T>(val data: T) : StatisticsResult<T>()
        data class Error(val message: String) : StatisticsResult<Nothing>()
    }
    
    suspend fun getTopSpecies(limit: Int = 10): StatisticsResult<List<SpeciesStatistic>> = 
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from("tree_species_statistics")
                    .select {
                        limit(limit.toLong())
                    }
                    .decodeList<SpeciesStatistic>()
                
                Log.d("TreeStatisticsRepository", "Loaded ${result.size} top species")
                StatisticsResult.Success(result)
            } catch (e: Exception) {
                Log.e("TreeStatisticsRepository", "Error loading top species", e)
                StatisticsResult.Error("Fehler beim Laden der Baumarten: ${e.message}")
            }
        }
    
    suspend fun getTopSpeciesInDistrict(
        district: Int, 
        limit: Int = 10
    ): StatisticsResult<List<SpeciesByDistrict>> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("tree_species_by_district")
                .select {
                    filter {
                        eq("district", district)
                    }
                    limit(limit.toLong())
                }
                .decodeList<SpeciesByDistrict>()
            
            Log.d("TreeStatisticsRepository", "Loaded ${result.size} species for district $district")
            StatisticsResult.Success(result)
        } catch (e: Exception) {
            Log.e("TreeStatisticsRepository", "Error loading district species", e)
            StatisticsResult.Error("Fehler beim Laden der Bezirks-Statistik: ${e.message}")
        }
    }
    
    suspend fun getAllDistrictStatistics(): StatisticsResult<List<DistrictStatistic>> = 
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from("district_statistics")
                    .select()
                    .decodeList<DistrictStatistic>()
                
                Log.d("TreeStatisticsRepository", "Loaded statistics for ${result.size} districts")
                StatisticsResult.Success(result)
            } catch (e: Exception) {
                Log.e("TreeStatisticsRepository", "Error loading district statistics", e)
                StatisticsResult.Error("Fehler beim Laden der Bezirks-Übersicht: ${e.message}")
            }
        }
    
    suspend fun getDistrictStatistic(district: Int): StatisticsResult<DistrictStatistic?> = 
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from("district_statistics")
                    .select {
                        filter {
                            eq("district", district)
                        }
                    }
                    .decodeSingleOrNull<DistrictStatistic>()
                
                Log.d("TreeStatisticsRepository", "Loaded statistic for district $district")
                StatisticsResult.Success(result)
            } catch (e: Exception) {
                Log.e("TreeStatisticsRepository", "Error loading district statistic", e)
                StatisticsResult.Error("Fehler beim Laden der Bezirks-Statistik: ${e.message}")
            }
        }
    
    suspend fun getSpeciesAgeStatistics(limit: Int = 20): StatisticsResult<List<SpeciesAgeStatistic>> = 
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from("species_age_statistics")
                    .select {
                        limit(limit.toLong())
                    }
                    .decodeList<SpeciesAgeStatistic>()
                
                Log.d("TreeStatisticsRepository", "Loaded age statistics for ${result.size} species")
                StatisticsResult.Success(result)
            } catch (e: Exception) {
                Log.e("TreeStatisticsRepository", "Error loading age statistics", e)
                StatisticsResult.Error("Fehler beim Laden der Alters-Statistik: ${e.message}")
            }
        }
    
    suspend fun getTotalTreeCount(): StatisticsResult<Long> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("total_tree_count")
                .select()
                .decodeSingle<TotalTreeCount>()
            
            Log.d("TreeStatisticsRepository", "Total trees in database: ${result.total}")
            StatisticsResult.Success(result.total)
        } catch (e: Exception) {
            Log.e("TreeStatisticsRepository", "Error loading total tree count", e)
            StatisticsResult.Error("Fehler beim Laden der Gesamt-Anzahl: ${e.message}")
        }
    }
    
    suspend fun getCompleteStatistics(): StatisticsResult<TreeStatistics> = 
        withContext(Dispatchers.IO) {
            try {
                val topSpecies = when (val result = getTopSpecies(10)) {
                    is StatisticsResult.Success -> result.data
                    is StatisticsResult.Error -> emptyList()
                }
                
                val districtStats = when (val result = getAllDistrictStatistics()) {
                    is StatisticsResult.Success -> result.data
                    is StatisticsResult.Error -> emptyList()
                }
                
                val totalTrees = when (val result = getTotalTreeCount()) {
                    is StatisticsResult.Success -> result.data
                    is StatisticsResult.Error -> districtStats.sumOf { it.totalTrees }
                }
                
                val uniqueSpecies = topSpecies.size
                
                val statistics = TreeStatistics(
                    topSpecies = topSpecies,
                    districtStats = districtStats,
                    totalTrees = totalTrees,
                    uniqueSpecies = uniqueSpecies
                )
                
                StatisticsResult.Success(statistics)
            } catch (e: Exception) {
                Log.e("TreeStatisticsRepository", "Error loading complete statistics", e)
                StatisticsResult.Error("Fehler beim Laden der Statistiken: ${e.message}")
            }
        }
    
    suspend fun getDistrictTreeStatistics(district: Int): StatisticsResult<DistrictTreeStatistics> = 
        withContext(Dispatchers.IO) {
            try {
                val topSpecies = when (val result = getTopSpeciesInDistrict(district, 10)) {
                    is StatisticsResult.Success -> result.data
                    is StatisticsResult.Error -> emptyList()
                }
                
                val districtStat = when (val result = getDistrictStatistic(district)) {
                    is StatisticsResult.Success -> result.data
                    is StatisticsResult.Error -> null
                }
                
                val statistics = DistrictTreeStatistics(
                    district = district,
                    topSpecies = topSpecies,
                    totalTrees = districtStat?.totalTrees ?: 0,
                    uniqueSpecies = districtStat?.uniqueSpecies ?: 0
                )
                
                StatisticsResult.Success(statistics)
            } catch (e: Exception) {
                Log.e("TreeStatisticsRepository", "Error loading district tree statistics", e)
                StatisticsResult.Error("Fehler beim Laden der Bezirks-Statistik: ${e.message}")
            }
        }
}

object TreeStatisticsProvider {
    val repository: TreeStatisticsRepository by lazy {
        TreeStatisticsRepository()
    }
}

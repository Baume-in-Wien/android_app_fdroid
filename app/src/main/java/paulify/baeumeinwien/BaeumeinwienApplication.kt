package paulify.baeumeinwien

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import paulify.baeumeinwien.data.local.TreeDatabase
import paulify.baeumeinwien.data.remote.RetrofitInstance
import paulify.baeumeinwien.data.repository.TreeRepository
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre

private val Context.dataStore by preferencesDataStore(name = "settings")

class BaeumeinwienApplication : Application(), ImageLoaderFactory {
    
    companion object {
        lateinit var instance: BaeumeinwienApplication
            private set
    }

    val database by lazy { TreeDatabase.getDatabase(this) }
    
        val repository by lazy {
        TreeRepository(
            api = RetrofitInstance.api,
            dao = database.treeDao(),
            achievementDao = database.achievementDao(),
            dataStore = this.dataStore
        )
    }

    val rallyRepository by lazy {
        paulify.baeumeinwien.data.repository.RallyRepository(
            rallyDao = database.rallyDao()
        )
    }
    
    val achievementManager by lazy {
        paulify.baeumeinwien.data.repository.AchievementManager(
            achievementDao = database.achievementDao()
        )
    }

    val authRepository by lazy {
        paulify.baeumeinwien.data.repository.AuthRepository()
    }

    val communityTreeRepository by lazy {
        paulify.baeumeinwien.data.repository.CommunityTreeRepository()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        MapLibre.getInstance(this)
    }
    
    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Baeumeinwien/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
        
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
}

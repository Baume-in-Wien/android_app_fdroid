package paulify.baeumeinwien

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.handleDeeplinks
import paulify.baeumeinwien.data.remote.SupabaseInstance
import paulify.baeumeinwien.ui.BaeumeinwienApp
import paulify.baeumeinwien.ui.ViewModelFactory
import paulify.baeumeinwien.ui.theme.BaeumeinwienTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SupabaseInstance.client.handleDeeplinks(intent) { session ->
            Log.d("MainActivity", "OAuth session imported successfully")
        }

        val app = application as BaeumeinwienApplication
        val repository = app.repository
        val rallyRepository = app.rallyRepository
        val achievementManager = app.achievementManager
        val authRepository = app.authRepository
        val communityTreeRepository = app.communityTreeRepository
        val viewModelFactory = ViewModelFactory(
            applicationContext, repository, rallyRepository, achievementManager,
            authRepository, communityTreeRepository
        )

        lifecycleScope.launchWhenStarted {
            rallyRepository.deleteExpiredRallies()
        }

        setContent {
            BaeumeinwienTheme {
                BaeumeinwienApp(viewModelFactory = viewModelFactory)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseInstance.client.handleDeeplinks(intent) { session ->
            Log.d("MainActivity", "OAuth session imported via onNewIntent")
        }
    }
}

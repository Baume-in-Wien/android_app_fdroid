package paulify.baeumeinwien.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SupabaseInstance {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = "https://awkwclebcnzgvpnmypwd.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF3a3djbGViY256Z3Zwbm15cHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjcxNTU2ODgsImV4cCI6MjA4MjczMTY4OH0.z29RXjO5wZxt0BKZsZINs_9bnpF25439fUbN3U3A-qc"
        ) {
            httpEngine = OkHttp.create()
            install(Auth) {
                scheme = "paulify.baeumeinwien"
                host = "login-callback"
                alwaysAutoRefresh = true
            }
            install(Postgrest)
            install(Realtime) {
            }
            install(Storage)
        }
    }
    
    suspend fun setDeviceId(deviceId: String) {
        try {
            client.postgrest.rpc("set_config", buildJsonObject {
                put("setting_name", "app.device_id")
                put("setting_value", deviceId)
            })
        } catch (e: Exception) {
            android.util.Log.e("SupabaseInstance", "Failed to set device_id", e)
        }
    }
}

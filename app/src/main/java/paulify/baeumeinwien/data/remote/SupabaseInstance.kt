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
            supabaseUrl = "https://login.treesinvienna.eu",
            supabaseKey = "7v40sIFSOAC7sYM53SBjhysrzlYmNwD45bV44I"
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

package paulify.baeumeinwien.data.repository

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserUpdateBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import paulify.baeumeinwien.data.domain.AuthState
import paulify.baeumeinwien.data.remote.SupabaseInstance

class AuthRepository {

    private val client = SupabaseInstance.client
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                Log.d(TAG, "Session status changed: $status")
                val newState = when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = client.auth.currentUserOrNull()
                        Log.d(TAG, "Authenticated user: ${user?.id}, email: ${user?.email}")
                        AuthState.Authenticated(
                            userId = user?.id ?: "",
                            email = user?.email,
                            displayName = user?.userMetadata?.get("full_name")?.toString()?.trim('"')
                                ?: user?.userMetadata?.get("name")?.toString()?.trim('"')
                                ?: user?.email?.substringBefore('@'),
                            avatarUrl = user?.userMetadata?.get("avatar_url")?.toString()?.trim('"')
                        )
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Log.d(TAG, "User not authenticated")
                        AuthState.NotAuthenticated
                    }
                    is SessionStatus.Initializing -> {
                        Log.d(TAG, "Session initializing...")
                        AuthState.Loading
                    }
                    else -> {
                        Log.d(TAG, "Unknown session status: $status")
                        AuthState.Loading
                    }
                }
                _authState.value = newState
            }
        }
    }

    suspend fun signInWithGoogle(): Result<Unit> {
        return try {
            client.auth.signInWith(Google)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            _authState.value = AuthState.Error(e.message ?: "Google-Anmeldung fehlgeschlagen")
            Result.Error(e, e.message)
        }
    }

    suspend fun signInWithGitHub(): Result<Unit> {
        return try {
            client.auth.signInWith(Github)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub sign-in failed", e)
            _authState.value = AuthState.Error(e.message ?: "GitHub-Anmeldung fehlgeschlagen")
            Result.Error(e, e.message)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed", e)
            val userMessage = when {
                e.message?.contains("Invalid login credentials") == true -> "E-Mail oder Passwort falsch"
                e.message?.contains("Email not confirmed") == true -> "E-Mail noch nicht bestätigt. Prüfe dein Postfach."
                e.message?.contains("network") == true -> "Netzwerkfehler. Prüfe deine Internetverbindung."
                else -> e.message ?: "Anmeldung fehlgeschlagen"
            }
            _authState.value = AuthState.Error(userMessage)
            Result.Error(e, userMessage)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up failed", e)
            val userMessage = when {
                e.message?.contains("already registered") == true -> "Diese E-Mail ist bereits registriert"
                e.message?.contains("password") == true -> "Passwort zu schwach (min. 6 Zeichen)"
                else -> e.message ?: "Registrierung fehlgeschlagen"
            }
            _authState.value = AuthState.Error(userMessage)
            Result.Error(e, userMessage)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            client.auth.updateUser {
                password = newPassword
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password change failed", e)
            Result.Error(e, e.message ?: "Passwort-Änderung fehlgeschlagen")
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    fun getCurrentDisplayName(): String? {
        val user = client.auth.currentUserOrNull() ?: return null
        return user.userMetadata?.get("full_name")?.toString()?.trim('"')
            ?: user.userMetadata?.get("name")?.toString()?.trim('"')
            ?: user.email?.substringBefore('@')
    }

    fun isEmailUser(): Boolean {
        val user = client.auth.currentUserOrNull() ?: return false
        return user.appMetadata?.get("provider")?.toString()?.trim('"') == "email"
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}

package paulify.baeumeinwien.data.domain

sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String?,
        val displayName: String?,
        val avatarUrl: String?
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

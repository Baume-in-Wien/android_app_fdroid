package paulify.baeumeinwien.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import paulify.baeumeinwien.data.domain.AuthState
import paulify.baeumeinwien.data.repository.AuthRepository
import paulify.baeumeinwien.data.repository.Result

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    data class LoginUiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSignUpMode: Boolean = false,
        val signUpSuccess: Boolean = false
    )

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Error) {
                    _loginUiState.value = _loginUiState.value.copy(
                        error = state.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _loginUiState.value = _loginUiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithGoogle()
            if (result is Result.Error) {
                _loginUiState.value = _loginUiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun signInWithGitHub() {
        viewModelScope.launch {
            _loginUiState.value = _loginUiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithGitHub()
            if (result is Result.Error) {
                _loginUiState.value = _loginUiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun signInWithEmail() {
        val state = _loginUiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _loginUiState.value = state.copy(error = "Bitte E-Mail und Passwort eingeben")
            return
        }
        viewModelScope.launch {
            _loginUiState.value = state.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmail(state.email, state.password)
            _loginUiState.value = _loginUiState.value.copy(
                isLoading = false,
                error = (result as? Result.Error)?.message
            )
        }
    }

    fun signUpWithEmail() {
        val state = _loginUiState.value
        if (state.email.isBlank() || state.password.length < 6) {
            _loginUiState.value = state.copy(error = "Passwort muss mindestens 6 Zeichen haben")
            return
        }
        viewModelScope.launch {
            _loginUiState.value = state.copy(isLoading = true, error = null)
            val result = authRepository.signUpWithEmail(state.email, state.password)
            if (result is Result.Success) {
                _loginUiState.value = _loginUiState.value.copy(
                    isLoading = false,
                    signUpSuccess = true,
                    error = null
                )
            } else {
                _loginUiState.value = _loginUiState.value.copy(
                    isLoading = false,
                    error = (result as? Result.Error)?.message
                )
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _loginUiState.value = _loginUiState.value.copy(isLoading = true, error = null)
            val result = authRepository.changePassword(newPassword)
            _loginUiState.value = _loginUiState.value.copy(
                isLoading = false,
                error = (result as? Result.Error)?.message
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun isEmailUser(): Boolean = authRepository.isEmailUser()

    fun updateEmail(email: String) {
        _loginUiState.value = _loginUiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _loginUiState.value = _loginUiState.value.copy(password = password, error = null)
    }

    fun toggleSignUpMode() {
        _loginUiState.value = _loginUiState.value.copy(
            isSignUpMode = !_loginUiState.value.isSignUpMode,
            error = null,
            signUpSuccess = false
        )
    }

    fun clearError() {
        _loginUiState.value = _loginUiState.value.copy(error = null)
    }
}

package paulify.baeumeinwien.data.repository

import paulify.baeumeinwien.data.domain.Tree
import kotlinx.coroutines.flow.Flow

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

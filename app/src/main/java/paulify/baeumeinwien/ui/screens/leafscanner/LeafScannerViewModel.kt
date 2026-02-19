package paulify.baeumeinwien.ui.screens.leafscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import paulify.baeumeinwien.util.LeafClassificationResult
import paulify.baeumeinwien.util.LeafClassificationService
import paulify.baeumeinwien.util.ModelDownloadState

class LeafScannerViewModel(
    val classificationService: LeafClassificationService
) : ViewModel() {

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    val modelState: StateFlow<ModelDownloadState> = classificationService.modelState
    val isClassifying: StateFlow<Boolean> = classificationService.isClassifying
    val lastResults: StateFlow<List<LeafClassificationResult>> = classificationService.lastResults
    val errorMessage: StateFlow<String?> = classificationService.errorMessage

    fun downloadModel() {
        viewModelScope.launch {
            classificationService.downloadModel()
        }
    }

    fun retryDownload() {
        viewModelScope.launch {
            classificationService.retryDownload()
        }
    }

    fun classifyImage(bitmap: Bitmap) {
        _capturedImage.value = bitmap
        viewModelScope.launch {
            classificationService.classify(bitmap)
        }
    }

    fun reset() {
        _capturedImage.value = null
        classificationService.reset()
    }

    override fun onCleared() {
        super.onCleared()
        classificationService.close()
    }
}

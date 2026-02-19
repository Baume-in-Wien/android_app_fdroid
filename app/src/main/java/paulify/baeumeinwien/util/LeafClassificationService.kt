package paulify.baeumeinwien.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.FloatBuffer

data class LeafClassificationResult(
    val speciesGerman: String,
    val speciesLatin: String,
    val confidence: Float,
    val label: String
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()

    val confidenceLevel: ConfidenceLevel
        get() = when {
            confidence >= 0.7f -> ConfidenceLevel.HIGH
            confidence >= 0.4f -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

    enum class ConfidenceLevel(val description: String, val icon: String) {
        HIGH("Hohe Sicherheit", "✓"),
        MEDIUM("Mittlere Sicherheit", "?"),
        LOW("Niedrige Sicherheit", "!")
    }
}

sealed class ModelDownloadState {
    data object NotDownloaded : ModelDownloadState()
    data class Downloading(val progress: Float) : ModelDownloadState()
    data object Downloaded : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

class LeafClassificationService(private val context: Context) {

    companion object {
        private const val MODEL_URL = "https://pub-5061dbde1e5d428583b6722a65924e3c.r2.dev/LeafClassifier/leaf_classifier.onnx"
        const val MODEL_SIZE_MB = 380
        private const val MODEL_FILENAME = "leaf_classifier.onnx"
        private const val IMAGE_SIZE = 224

        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    private val _modelState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val modelState: StateFlow<ModelDownloadState> = _modelState.asStateFlow()

    private val _isClassifying = MutableStateFlow(false)
    val isClassifying: StateFlow<Boolean> = _isClassifying.asStateFlow()

    private val _lastResults = MutableStateFlow<List<LeafClassificationResult>>(emptyList())
    val lastResults: StateFlow<List<LeafClassificationResult>> = _lastResults.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var ortSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var classNames: List<ClassInfo> = emptyList()
    private var isModelLoaded = false

    private val modelDir: File
        get() = File(context.filesDir, "leaf_classifier_model")

    private val modelFile: File
        get() = File(modelDir, MODEL_FILENAME)

    val isModelAvailable: Boolean
        get() = _modelState.value == ModelDownloadState.Downloaded

    data class ClassInfo(
        val german: String,
        val latin: String,
        val label: String
    )

    init {
        loadClassNames()
        checkModelAvailability()
    }

    private fun loadClassNames() {
        try {
            val json = context.assets.open("class_names.json").bufferedReader().readText()
            val jsonArray = JSONArray(json)
            val names = mutableListOf<ClassInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                names.add(
                    ClassInfo(
                        german = obj.getString("german"),
                        latin = obj.getString("latin"),
                        label = obj.getString("label")
                    )
                )
            }
            classNames = names
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkModelAvailability() {
        if (modelFile.exists() && modelFile.length() > 0) {
            _modelState.value = ModelDownloadState.Downloaded
            loadModel()
        } else {
            _modelState.value = ModelDownloadState.NotDownloaded
        }
    }

    suspend fun downloadModel() {
        if (_modelState.value is ModelDownloadState.Downloading) return

        _modelState.value = ModelDownloadState.Downloading(0f)

        withContext(Dispatchers.IO) {
            try {
                modelDir.mkdirs()

                val url = URL(MODEL_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.connect()

                val totalBytes = connection.contentLength.toLong()
                var downloadedBytes = 0L

                val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes
                                _modelState.value = ModelDownloadState.Downloading(progress)
                            }
                        }
                    }
                }

                if (modelFile.exists()) modelFile.delete()
                tempFile.renameTo(modelFile)

                _modelState.value = ModelDownloadState.Downloaded
                loadModel()

            } catch (e: Exception) {
                File(modelDir, "$MODEL_FILENAME.tmp").delete()
                _modelState.value = ModelDownloadState.Error(
                    "Download fehlgeschlagen: ${e.localizedMessage}"
                )
            }
        }
    }

    suspend fun retryDownload() {
        _modelState.value = ModelDownloadState.NotDownloaded
        downloadModel()
    }

    fun deleteModel() {
        ortSession?.close()
        ortSession = null
        isModelLoaded = false

        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }

        _modelState.value = ModelDownloadState.NotDownloaded
    }

    private fun loadModel() {
        try {
            val env = OrtEnvironment.getEnvironment()
            ortEnvironment = env

            val sessionOptions = OrtSession.SessionOptions().apply {
                try {
                    addNnapi()
                } catch (_: Exception) {
                }
            }

            ortSession = env.createSession(modelFile.absolutePath, sessionOptions)
            isModelLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Modell konnte nicht geladen werden: ${e.localizedMessage}"
        }
    }

    suspend fun classify(bitmap: Bitmap) {
        if (!isModelLoaded || ortSession == null) {
            _errorMessage.value = "Modell nicht geladen"
            return
        }

        _isClassifying.value = true
        _errorMessage.value = null

        withContext(Dispatchers.Default) {
            try {
                if (!passesLeafPreFilter(bitmap)) {
                    _isClassifying.value = false
                    _lastResults.value = emptyList()
                    _errorMessage.value = "Kein Blatt erkannt. Bitte fotografiere ein Blatt eines Baumes."
                    return@withContext
                }

                val inputTensor = preprocessImage(bitmap)

                val session = ortSession!!
                val inputName = session.inputNames.first()
                val env = ortEnvironment!!

                val onnxTensor = OnnxTensor.createTensor(env, inputTensor, longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong()))

                val output = session.run(mapOf(inputName to onnxTensor))
                val logits = (output[0].value as Array<FloatArray>)[0]

                val probabilities = softmax(logits)

                val topIndices = probabilities.indices
                    .sortedByDescending { probabilities[it] }
                    .take(5)

                val results = topIndices.mapNotNull { idx ->
                    if (idx < classNames.size) {
                        val cls = classNames[idx]
                        LeafClassificationResult(
                            speciesGerman = cls.german,
                            speciesLatin = cls.latin,
                            confidence = probabilities[idx],
                            label = cls.label
                        )
                    } else null
                }

                _lastResults.value = results
                _isClassifying.value = false

                onnxTensor.close()
                output.close()

            } catch (e: Exception) {
                e.printStackTrace()
                _isClassifying.value = false
                _errorMessage.value = "Klassifizierung fehlgeschlagen: ${e.localizedMessage}"
            }
        }
    }

    private fun passesLeafPreFilter(bitmap: Bitmap): Boolean {
        val scaled = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        var greenPixels = 0
        var brownPixels = 0
        val total = scaled.width * scaled.height

        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                val pixel = scaled[x, y]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (g > r * 0.8 && g > b * 0.8 && g > 50) {
                    greenPixels++
                }
                if (r > g && r > b && r > 80 && g > 40 && g < r) {
                    brownPixels++
                }
            }
        }

        if (scaled != bitmap) scaled.recycle()

        val greenRatio = greenPixels.toFloat() / total
        val brownRatio = brownPixels.toFloat() / total

        return greenRatio > 0.05f || brownRatio > 0.10f
    }

    private fun preprocessImage(bitmap: Bitmap): FloatBuffer {
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val scaled = Bitmap.createScaledBitmap(cropped, IMAGE_SIZE, IMAGE_SIZE, true)

        val buffer = FloatBuffer.allocate(3 * IMAGE_SIZE * IMAGE_SIZE)
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        scaled.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        for (c in 0..2) {
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val value = when (c) {
                    0 -> Color.red(pixel)
                    1 -> Color.green(pixel)
                    else -> Color.blue(pixel)
                }
                buffer.put((value / 255f - MEAN[c]) / STD[c])
            }
        }

        buffer.rewind()

        if (cropped != bitmap) cropped.recycle()
        if (scaled != cropped) scaled.recycle()

        return buffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.max()
        val exps = logits.map { Math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps = exps.sum()
        return exps.map { it / sumExps }.toFloatArray()
    }

    fun reset() {
        _lastResults.value = emptyList()
        _errorMessage.value = null
        _isClassifying.value = false
    }

    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
    }
}

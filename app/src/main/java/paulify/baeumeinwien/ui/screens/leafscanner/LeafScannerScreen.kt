package paulify.baeumeinwien.ui.screens.leafscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import paulify.baeumeinwien.util.LeafClassificationResult
import paulify.baeumeinwien.util.ModelDownloadState

private val LeafGreen = Color(0xFF4CAF50)
private val LeafGreenDark = Color(0xFF388E3C)
private val LeafGreenLight = Color(0xFF81C784)
private val LeafMint = Color(0xFF80CBC4)
private val ConfidenceHigh = Color(0xFF4CAF50)
private val ConfidenceMedium = Color(0xFFFF9800)
private val ConfidenceLow = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafScannerScreen(
    viewModel: LeafScannerViewModel,
    onSearchSpecies: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val modelState by viewModel.modelState.collectAsState()
    val isClassifying by viewModel.isClassifying.collectAsState()
    val results by viewModel.lastResults.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val capturedImage by viewModel.capturedImage.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.classifyImage(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { bmp -> viewModel.classifyImage(bmp) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blatt-Scanner", fontWeight = FontWeight.Bold) },
                actions = {
                    if (results.isNotEmpty()) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            LeafGreen.copy(alpha = 0.03f),
                            LeafGreen.copy(alpha = 0.07f),
                            LeafMint.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = when {
                    modelState !is ModelDownloadState.Downloaded -> "download"
                    results.isEmpty() && !isClassifying && errorMessage == null -> "empty"
                    else -> "results"
                },
                transitionSpec = {
                    fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                },
                label = "screen_state"
            ) { state ->
                when (state) {
                    "download" -> ModelDownloadView(
                        modelState = modelState,
                        onDownload = { viewModel.downloadModel() },
                        onRetry = { viewModel.retryDownload() }
                    )
                    "empty" -> EmptyStateView(
                        onCamera = { cameraLauncher.launch(null) },
                        onGallery = { galleryLauncher.launch("image/*") }
                    )
                    "results" -> ResultStateView(
                        capturedImage = capturedImage,
                        isClassifying = isClassifying,
                        results = results,
                        errorMessage = errorMessage,
                        onCamera = { cameraLauncher.launch(null) },
                        onGallery = { galleryLauncher.launch("image/*") },
                        onSearchSpecies = onSearchSpecies
                    )
                }
            }
        }
    }
}


@Composable
private fun ModelDownloadView(
    modelState: ModelDownloadState,
    onDownload: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "BLATT\nERKENNUNG",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "KI-Modell wird benötigt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(LeafGreen.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            val icon = when (modelState) {
                is ModelDownloadState.NotDownloaded -> Icons.Default.CloudDownload
                is ModelDownloadState.Downloading -> Icons.Default.CloudDownload
                is ModelDownloadState.Downloaded -> Icons.Default.CheckCircle
                is ModelDownloadState.Error -> Icons.Default.Warning
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = LeafGreen
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        val (title, description) = when (modelState) {
            is ModelDownloadState.NotDownloaded -> "KI-Modell benötigt" to
                    "Für die Blatterkennung muss einmalig ein KI-Modell heruntergeladen werden (~${LeafClassificationService.MODEL_SIZE_MB} MB). WLAN empfohlen."
            is ModelDownloadState.Downloading -> "Wird heruntergeladen..." to
                    "Das Modell wird heruntergeladen und für dein Gerät optimiert..."
            is ModelDownloadState.Downloaded -> "Bereit" to "Das Modell ist bereit."
            is ModelDownloadState.Error -> "Fehler" to (modelState as ModelDownloadState.Error).message
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 20.sp
        )

        if (modelState is ModelDownloadState.Downloading) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { (modelState as ModelDownloadState.Downloading).progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    color = LeafGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${((modelState as ModelDownloadState.Downloading).progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        when (modelState) {
            is ModelDownloadState.Downloading -> { /* show nothing */ }
            is ModelDownloadState.Error -> {
                GreenGradientButton(
                    text = "Erneut versuchen",
                    icon = Icons.Default.Refresh,
                    onClick = onRetry
                )
            }
            else -> {
                if (modelState !is ModelDownloadState.Downloaded) {
                    GreenGradientButton(
                        text = "Modell herunterladen (~${LeafClassificationService.MODEL_SIZE_MB} MB)",
                        icon = Icons.Default.CloudDownload,
                        onClick = onDownload
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}


@Composable
private fun EmptyStateView(
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    val appeared = remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    LaunchedEffect(Unit) {
        appeared.value = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        AnimatedVisibility(
            visible = appeared.value,
            enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 16 }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "BLATT\nERKENNUNG",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fotografiere ein Blatt und\nerkenne die Baumart",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = appeared.value,
            enter = fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.85f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .rotate(ringRotation)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    LeafGreen.copy(alpha = 0.5f),
                                    LeafGreen.copy(alpha = 0.05f),
                                    LeafMint.copy(alpha = 0.25f),
                                    LeafGreen.copy(alpha = 0.05f),
                                    LeafGreen.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(LeafGreen.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                Icon(
                    imageVector = Icons.Default.Yard,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = LeafGreen
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = appeared.value,
            enter = fadeIn(tween(700, delayMillis = 200)) + slideInVertically(tween(700, delayMillis = 200)) { 12 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StepRow(number = 1, icon = Icons.Default.CameraAlt, text = "Fotografiere ein einzelnes Blatt")
                StepRow(number = 2, icon = Icons.Default.AutoAwesome, text = "KI erkennt die Baumart")
                StepRow(number = 3, icon = Icons.Default.Park, text = "Finde passende Bäume auf der Karte")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        GreenGradientButton(
            text = "Foto aufnehmen",
            icon = Icons.Default.CameraAlt,
            onClick = onCamera
        )

        Spacer(modifier = Modifier.height(10.dp))

        GlassButton(
            text = "Aus Galerie wählen",
            icon = Icons.Default.PhotoLibrary,
            onClick = onGallery
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}


@Composable
private fun ResultStateView(
    capturedImage: Bitmap?,
    isClassifying: Boolean,
    results: List<LeafClassificationResult>,
    errorMessage: String?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSearchSpecies: ((String) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp, top = 4.dp)
    ) {
        capturedImage?.let { bitmap ->
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .shadow(20.dp, RoundedCornerShape(24.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Aufgenommenes Blatt",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (isClassifying) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = LeafGreen,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analysiere Blatt...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        errorMessage?.let { error ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (results.isNotEmpty()) {
            item {
                Text(
                    text = "Erkannte Baumarten",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            itemsIndexed(results) { index, result ->
                LeafResultCard(
                    result = result,
                    rank = index + 1,
                    isTop = index == 0,
                    onClick = {
                        if (index == 0) onSearchSpecies?.invoke(result.speciesGerman)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Einzelnes Blatt auf hellem Hintergrund ergibt bessere Ergebnisse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GreenGradientButton(
                        text = "Neues Foto",
                        icon = Icons.Default.CameraAlt,
                        onClick = onCamera
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassButton(
                        text = "Anderes Bild wählen",
                        icon = Icons.Default.PhotoLibrary,
                        onClick = onGallery
                    )
                }
            }
        }
    }
}


@Composable
private fun LeafResultCard(
    result: LeafClassificationResult,
    rank: Int,
    isTop: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confidenceColor = when (result.confidenceLevel) {
        LeafClassificationResult.ConfidenceLevel.HIGH -> ConfidenceHigh
        LeafClassificationResult.ConfidenceLevel.MEDIUM -> ConfidenceMedium
        LeafClassificationResult.ConfidenceLevel.LOW -> ConfidenceLow
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isTop) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            LeafGreen.copy(alpha = 0.5f),
                            LeafMint.copy(alpha = 0.2f),
                            LeafGreen.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (isTop) 0.8f else 0.5f
            )
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTop) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isTop) Brush.linearGradient(
                            colors = listOf(LeafGreen, LeafGreenDark)
                        ) else Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTop) {
                    Icon(
                        Icons.Default.Yard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.speciesGerman,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.speciesLatin.isNotEmpty()) {
                    Text(
                        text = result.speciesLatin,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${result.confidencePercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = confidenceColor
                )
                Text(
                    text = result.confidenceLevel.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = confidenceColor
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(LeafGreen.copy(alpha = 0.18f), LeafGreen.copy(alpha = 0.06f))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LeafGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = "Schritt $number",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = LeafGreen.copy(alpha = 0.8f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun GreenGradientButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LeafGreen
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlassButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private typealias LeafClassificationService = paulify.baeumeinwien.util.LeafClassificationService

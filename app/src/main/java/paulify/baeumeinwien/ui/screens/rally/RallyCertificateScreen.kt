package paulify.baeumeinwien.ui.screens.rally

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import paulify.baeumeinwien.data.domain.Rally
import paulify.baeumeinwien.data.domain.RallyProgress

@Composable
fun RallyCertificateScreen(
    rally: Rally,
    progress: RallyProgress,
    onClose: () -> Unit
) {
    val gold = Color(0xFFFFD700)
    val ribbonColor = Color(0xFFC41E3A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CertificateBackground()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .border(8.dp, gold, RoundedCornerShape(16.dp))
                .padding(4.dp)
                .border(2.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "URKUNDE",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.DarkGray,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Divider(color = gold, thickness = 2.dp, modifier = Modifier.width(120.dp))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Diese Auszeichnung wird verliehen an",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = progress.studentName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "für die erfolgreiche Teilnahme an der Rallye",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Text(
                    text = rally.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CertificateStat(label = "Bäume gefunden", value = "${progress.foundTreeIds.size}")
                    CertificateStat(label = "Punkte", value = "${progress.score}")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = gold
                    )
                    
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.3f, size.height * 0.8f)
                            lineTo(size.width * 0.3f, size.height)
                            lineTo(size.width * 0.4f, size.height * 0.9f)
                            lineTo(size.width * 0.5f, size.height)
                            close()
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(0.5f))
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ribbonColor)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Urkunde Speichern")
                }
            }
        }
    }
}

@Composable
fun CertificateStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun CertificateBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val color = Color.LightGray.copy(alpha = 0.1f)
        
        for (i in 0..size.width.toInt() step 50) {
            drawLine(color, Offset(i.toFloat(), 0f), Offset(size.width - i, size.height), strokeWidth)
        }
    }
}

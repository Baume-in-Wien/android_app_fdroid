package paulify.baeumeinwien.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = ExpressiveLime,
    secondary = ExpressivePink,
    tertiary = ExpressiveYellow,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = ExpressiveDeepPurple,
    onSecondary = ExpressiveDeepPurple,
    onTertiary = ExpressiveDeepPurple,
    onBackground = White,
    onSurface = White
)

private val LightColorScheme = lightColorScheme(
    primary = ExpressiveDeepPurple,
    secondary = ExpressivePurple,
    tertiary = ExpressivePink,
    background = ExpressiveLavender,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = ExpressiveDeepPurple,
    onBackground = ExpressiveDeepPurple,
    onSurface = ExpressiveDeepPurple,
    
    primaryContainer = ExpressiveLime,
    onPrimaryContainer = ExpressiveDeepPurple,
    secondaryContainer = ExpressivePink,
    onSecondaryContainer = ExpressiveDeepPurple,
    tertiaryContainer = ExpressiveYellow,
    onTertiaryContainer = ExpressiveDeepPurple

)

@Composable
fun BaeumeinwienTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
package app1.pkg.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app1.pkg.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFF636366),
    onSecondary = Color.White,
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color.LightGray,
    error = Color(0xFFFF453A),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = Color.White,
    surface = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color.DarkGray,
    error = Color(0xFFFF3B30),
    onError = Color.White
)

@Composable
fun App1Theme(
    theme: AppTheme = AppTheme.BLACK,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme.copy(
            primary = if (theme == AppTheme.BLACK) Color.White else theme.primaryColor,
            onPrimary = if (theme == AppTheme.BLACK) Color.Black else Color.White,
            secondary = if (theme == AppTheme.BLACK) Color(0xFF636366) else theme.primaryColor.copy(alpha = 0.7f),
            onSecondary = Color.White
        )
    } else {
        LightColorScheme.copy(
            primary = if (theme == AppTheme.BLACK) Color.Black else theme.primaryColor,
            onPrimary = Color.White,
            secondary = if (theme == AppTheme.BLACK) Color(0xFF8E8E93) else theme.primaryColor.copy(alpha = 0.7f),
            onSecondary = Color.White
        )
    }

    val localView = LocalView.current
    if (!localView.isInEditMode) {
        SideEffect {
            val window = (localView.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, localView).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

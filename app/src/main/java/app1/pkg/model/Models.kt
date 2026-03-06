package app1.pkg.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class FamilyMember(
    val id: String,
    val name: String,
    val color: Color
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val memberId: String? = null,
    val color: Color,
    val description: String = "",
    val time: String = "All Day"
)

enum class AppTheme(val displayName: String, val primaryColor: Color) {
    BLACK("Modern Black", Color.Black),
    PINK("Soft Pink", Color(0xFFE91E63)),
    BLUE("Ocean Blue", Color(0xFF2196F3)),
    GREEN("Forest Green", Color(0xFF4CAF50)),
    PURPLE("Royal Purple", Color(0xFF9C27B0))
}

data class CalendarSettings(
    val wallpaperUri: String? = "https://images.unsplash.com/photo-1557683311-eac922347aa1?q=80&w=2029&auto=format&fit=crop", // Solid dark gradient
    val bannerUri: String? = "https://images.unsplash.com/photo-1557683316-973673baf926?q=80&w=2029&auto=format&fit=crop", // Solid purple gradient
    val theme: AppTheme = AppTheme.BLACK,
    val isDarkMode: Boolean = false,
    val fontFamily: String = "Serif",
    val isMinimalist: Boolean = true
) {
    val themeColor: Color get() = theme.primaryColor
}

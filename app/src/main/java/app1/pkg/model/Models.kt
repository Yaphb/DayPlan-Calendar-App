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
    val time: String = "All Day",
    val isNotificationEnabled: Boolean = true
)

data class EventData(
    val id: String,
    val title: String,
    val date: String,
    val colorArgb: Int,
    val description: String,
    val time: String,
    val isNotificationEnabled: Boolean = true
)

enum class AppTheme(val displayName: String, val primaryColor: Color) {
    BLACK("Modern Black", Color.Black),
    PINK("Soft Pink", Color(0xFFE91E63)),
    BLUE("Ocean Blue", Color(0xFF2196F3)),
    GREEN("Forest Green", Color(0xFF4CAF50)),
    PURPLE("Royal Purple", Color(0xFF9C27B0))
}

data class CalendarSettings(
    val wallpaperUri: String? = null,
    val bannerUri: String? = null,
    val theme: AppTheme = AppTheme.BLACK,
    val isDarkMode: Boolean = false,
    val fontFamily: String = "Serif",
    val isMinimalist: Boolean = true
) {
    val themeColor: Color get() = theme.primaryColor
}

data class WeatherInfo(
    val temperature: Double,
    val description: String,
    val weatherCode: Int,
    val icon: String,
    val date: LocalDate,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val rainSum: Double? = null,
    val tempMax: Double? = null,
    val tempMin: Double? = null
)
